package io.kubeless.server;

import io.kubeless.server.model.KubelessDomain;
import io.kubeless.server.model.KubelessModel;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.streams.Pump;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Option;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 *
 */
@Component
public class ProxyVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

    @Autowired
    private Vertx vertx;

    /**
     * Start the verticle.<p>
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.<p>
     * If your verticle does things in it's startup which take some time then you can override this method
     * and call the startFuture some time later when start up is complete.
     * @param startFuture  a future which should be called when verticle start-up is complete.
     * @throws Exception
     */
    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        EventBus eventBus = vertx.eventBus();
        HttpServer server = vertx.createHttpServer();
        HttpClient client = vertx.createHttpClient();

        Observable<KubelessModel> modelStream = eventBus.consumer("kubeless.model.current").toObservable()
                .map(m -> (KubelessModel) m.body());

        BehaviorSubject<KubelessModel> modelChanges = BehaviorSubject.create();
        modelStream.subscribe(modelChanges);

        server.requestStream().toObservable()
                .map(request -> {
                    logger.info("New request");
                    // Publish all requested domains to a topic
                    domain(request).forEach(domain -> {
                        logger.info("Publishing request to domain " + domain);
                        eventBus.publish("domain.requested", domain);
                    });
                    return request;
                })
                .flatMap(request -> readyDomain(request, domain(request), modelChanges))
                .subscribe(t -> {
                    HttpServerRequest request = t._1;
                    KubelessDomain domain = t._2;
                    logger.info("Ready to dispath request to host=" + domain.getServiceHost() + ", port=" + domain.getServicePort() + ", uri=" + request.uri());
                    serviceCall(client, domain.getServiceHost(), domain.getServicePort(), request.uri(), request.headers())
                            .subscribe(res -> {
                                logger.info("Got response from remote server");
                                request.response().setStatusCode(res.statusCode());
                                request.response().headers().setAll(res.headers());
                                Pump.pump(res, request.response()).start();
                                res.endHandler(end -> request.response().end());
                            }, err -> {
                                logger.warn("Got error from remote server", err);
                                request.response().setStatusCode(500);
                            });
                });

        server.listen(8080, status -> {
            if (status.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(status.cause());
            }
        });

    }

    private Observable<Tuple2<HttpServerRequest, KubelessDomain>> readyDomain(HttpServerRequest request, Option<String> domainName, BehaviorSubject<KubelessModel> modelChanges) {
        return modelChanges.map(model -> domainName.flatMap(dn -> model.getDomain(dn)))
                .filter(Option::isDefined)
                .map(Option::get)
                .filter(domain -> domain.getControllerReplicas() > 0)
                .map(domain -> Tuple.of(request, domain))
                .first();
    }

    private Observable<HttpClientResponse> serviceCall(HttpClient client, String host, int port, String uri, MultiMap headers) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        HttpClientRequest req = client.get(port, host, uri, responseHandler.toHandler());
        req.headers().addAll(headers.remove("Host").add("Host", host));
        req.end();

        return responseHandler;
    }

    private Option<String> domain(HttpServerRequest req) {
        String host = req.host();
        if (host != null && host.contains(".")) {
            String domain = host.substring(0, host.indexOf("."));
            return Option.of(domain);
        } else if (host != null && host.contains(":")) {
            String domain = host.substring(0, host.indexOf(":"));
            return Option.of(domain);
        }

        return Option.of(host);
    }

}
