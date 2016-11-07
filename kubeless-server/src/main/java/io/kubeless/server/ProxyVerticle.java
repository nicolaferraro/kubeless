package io.kubeless.server;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.kubeless.server.model.KubelessDomain;
import io.kubeless.server.model.KubelessModel;
import io.kubeless.server.util.Vertexizer;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.control.Option;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 *
 */
@Component
public class ProxyVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

    @Autowired
    private Vertx vertx;

    @Autowired
    private KubernetesAPI kubernetesAPI;

    @Autowired
    private KubelessBusAPI kubelessBusAPI;


    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        HttpServer server = vertx.createHttpServer();

        Observable<KubelessModel> modelChanges = kubernetesAPI.kubelessModel();

        // A general purpose timeout observable
        Supplier<Observable<Boolean>> timeoutSupplier = () -> Vertexizer.cleanBehaviour(vertx, Observable.just(true).delay(7, TimeUnit.SECONDS).startWith(false));

        // All different channels for managing requests
        PublishSubject<RequestContext> allRequests = PublishSubject.create();
        PublishSubject<RequestContext> requestsWithUnknownDomain = PublishSubject.create();
        PublishSubject<RequestContext> requestsWithTimeout = PublishSubject.create();
        PublishSubject<RequestContext> requestsWithTargetReady = PublishSubject.create();
        PublishSubject<RequestContext> requestsWithTargetNotReady = PublishSubject.create();

        /**
         * Intermediate steps
         */

        // Prepare the allRequest subject
        server.requestStream().toObservable()
                .doOnNext(request -> logger.info("Received new request for uri: " + request.absoluteURI()))
                .map(RequestContext::new)
                .map(ctx -> ctx.withDomain(domain(ctx.getRequest())))
                .doOnNext(ctx -> logger.info("Request for uri " + ctx.getRequest().absoluteURI() + " related to domain: " + ctx.getDomain()))
                .subscribe(allRequests);

        // Push requests without domain into the 404 bucket
        allRequests.filter(RequestContext::domainRequestUndefined)
                .subscribe(requestsWithUnknownDomain);

        // Separate requests that can be served immediately
        allRequests.filter(RequestContext::domainRequestDefined)
                .flatMap(ctx -> modelChanges.first().map(ctx::withModel))
                .subscribe(ctx -> {
                    if (ctx.targetDomainInexistent()) {
                        requestsWithUnknownDomain.onNext(ctx);
                    } else {
                        // Notify that the domain has been requested to trigger a pod creation if needed
                        kubelessBusAPI.pushRequestedDomain(ctx.getDomain().get());

                        if (ctx.targetDomainReady()) {
                            requestsWithTargetReady.onNext(ctx);
                        } else {
                            logger.info("Target not ready");
                            requestsWithTargetNotReady.onNext(ctx);
                        }
                    }
                });


        // Wait for the pod to become ready or time out
        requestsWithTargetNotReady
                .doOnNext(ctx -> ctx.getRequest().pause()) // Pause the request
                .doOnNext(ctx -> logger.info("Waiting for the domain " + ctx.domain.get() + " to become ready"))
                .flatMap(ctx -> Observable.combineLatest(timeoutSupplier.get().map(ctx::withTimeout), modelChanges, RequestContext::withModel)
                        .filter(c -> c.targetDomainReady() || c.isTimedOut())
                        .first()
                )
                .subscribe(ctx -> {
                    if (ctx.isTimedOut()) {
                        logger.info("Pod for domain " + ctx.domain.get() + " not ready: timeout");
                        requestsWithTimeout.onNext(ctx);
                    } else {
                        logger.info("Pod for domain " + ctx.domain.get() + " ready to accept the request: " + ctx.getRequest().absoluteURI());
                        requestsWithTargetReady.onNext(ctx);
                    }
                });


        /**
         * Final States
         */

        // Handle requests without domain
        requestsWithUnknownDomain
                .doOnNext(ctx -> logger.info("Sending a 404 error to the request for uri " + ctx.getRequest().absoluteURI()))
                .subscribe(ctx -> {
                    ctx.getRequest()
                            .resume()
                            .response()
                            .setStatusMessage("Unknown domain or target container not found")
                            .setStatusCode(404)
                            .setChunked(true)
                            .write("<html>"
                                    + "<head>"
                                    + "<title>Kubeless - Not Found (404)</title>"
                                    + "</head>"
                                    + "<body>"
                                    + "<h1>Kubeless - Not Found (404)</h1>"
                                    + "<h2>Unknown domain or target container not found</h2>"
                                    + "</body>"
                                    + "</html>")
                            .end();
                });

        // Handle request that timed out
        requestsWithTimeout
                .doOnNext(ctx -> logger.info("Sending a 504 error to the request for uri " + ctx.getRequest().absoluteURI()))
                .subscribe(ctx -> {
                    ctx.getRequest()
                            .resume()
                            .response()
                            .setStatusMessage("Timeout while waiting for the container to scale up")
                            .setStatusCode(504)
                            .setChunked(true)
                            .write("<html>"
                                    + "<head>"
                                    + "<title>Kubeless - Gateway Timeout (504)</title>"
                                    + "</head>"
                                    + "<body>"
                                    + "<h1>Kubeless - Gateway Timeout (504)</h1>"
                                    + "<h2>Timeout while waiting for the container to scale up</h2>"
                                    + "</body>"
                                    + "</html>")
                            .end();
                });

        // Handle requests that can be forwarded
        requestsWithTargetReady
                .doOnNext(ctx -> logger.info("Forwarding request to the pod " + ctx.getRequest().absoluteURI()))
                .subscribe(this::proxyRequest);


        /**
         * Starting the server
         */

        server.listen(8080, status -> {
            if (status.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(status.cause());
            }
        });

    }


    private void proxyRequest(RequestContext ctx) {

        HttpClientOptions options = new HttpClientOptions().setKeepAlive(false);
        HttpClient client = vertx.createHttpClient(options);

        KubelessDomain domain = ctx.getDomainData().get();
        HttpServerRequest originalReq = ctx.getRequest();
        originalReq.resume();

        HttpClientRequest proxiedReq = client.get(domain.getServicePort(), domain.getServiceHost(), uri(originalReq));
        originalReq.handler(proxiedReq::write);
        originalReq.endHandler(end -> proxiedReq.end());

        proxiedReq.handler(res -> {
            originalReq.response().setStatusCode(res.statusCode());
            originalReq.response().headers().setAll(res.headers());

            res.handler(originalReq.response()::write);
            res.endHandler(end -> originalReq.response().end());
        });

        proxiedReq.exceptionHandler(ex -> {
            logger.warn("Error while forwarding connection to " + domain.getDomain(), ex);
            originalReq.response().setStatusCode(500).end();
        });

        proxiedReq.headers().addAll(originalReq.headers().remove("Host").add("Host", domain.getServiceHost()));
        proxiedReq.setChunked(true);
    }

    private String uri(HttpServerRequest req) {
        String path = req.uri();
        if (path != null && path.indexOf("/", 1) > 0) {
            path = path.substring(path.indexOf("/", 1));
        } else {
            path = "/";
        }
        return path;
    }

    private Option<String> domain(HttpServerRequest req) {
        String path = req.uri();
        if (path != null && path.indexOf("/", 1) > 0) {
            String domain = path.substring(0, path.indexOf("/", 1)).replace("/", "").trim();
            return Option.of(domain.length() > 0 ? domain : null);
        } else if (path != null) {
            String domain = path.replace("/", "").trim();
            return Option.of(domain.length() > 0 ? domain : null);
        }
        return Option.none();
    }

    private static class RequestContext {

        private HttpServerRequest request;

        private Option<String> domain;

        private KubelessModel model;

        private boolean timedOut;

        public RequestContext(HttpServerRequest request) {
            this.request = request;
        }

        public RequestContext(HttpServerRequest request, Option<String> domain, KubelessModel model, boolean timedOut) {
            this.request = request;
            this.domain = domain;
            this.model = model;
            this.timedOut = timedOut;
        }

        protected RequestContext copy() {
            RequestContext copy = new RequestContext(this.request, this.domain, this.model, this.timedOut);
            return copy;
        }

        public RequestContext withDomain(Option<String> domain) {
            RequestContext copy = copy();
            copy.domain = domain;
            return copy;
        }

        public RequestContext withModel(KubelessModel model) {
            RequestContext copy = copy();
            copy.model = model;
            return copy;
        }

        public RequestContext withTimeout(boolean timedOut) {
            RequestContext copy = copy();
            copy.timedOut = timedOut;
            return copy;
        }

        public HttpServerRequest getRequest() {
            return request;
        }

        public Option<String> getDomain() {
            return domain;
        }

        public Option<KubelessDomain> getDomainData() {
            return domain.flatMap(model::getDomain);
        }

        public KubelessModel getModel() {
            return model;
        }

        public boolean domainRequestUndefined() {
            return domain.isEmpty();
        }

        public boolean domainRequestDefined() {
            return !domainRequestUndefined();
        }

        public boolean targetDomainInexistent() {
            return model.getDomain(domain.get()).isEmpty();
        }

        public boolean targetDomainExists() {
            return !targetDomainInexistent();
        }

        public boolean targetDomainReady() {
            return model.getDomain(domain.get()).filter(d -> d.getControllerReplicas() > 0).isDefined();
        }

        public boolean targetDomainNotReady() {
            return !targetDomainReady();
        }

        public boolean isTimedOut() {
            return timedOut;
        }

    }

}
