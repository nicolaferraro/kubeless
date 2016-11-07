package io.kubeless.server;

import io.kubeless.server.model.KubelessDomain;
import io.kubeless.server.model.KubelessModel;
import io.kubeless.server.model.KubelessReplicaChangeRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.Tuple;
import javaslang.control.Option;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Scales up the pods when they are requested.
 */
@Component
public class DeployerVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private KubernetesAPI kubernetesAPI;

    @Autowired
    private KubelessBusAPI kubelessBusAPI;

    @Autowired
    private Vertx vertx;

    /**
     * If your verticle does a simple, synchronous start-up then override this method and put your start-up
     * code in there.
     * @throws Exception
     */
    @Override
    public void start() throws Exception {

        Observable<KubelessModel> model = kubernetesAPI.kubelessModel();

        Observable<String> domains = kubelessBusAPI.requestedDomains();

        // When a domain with replicas=0 is requested, fire the scale up operation
        domains.withLatestFrom(model, DeployContext::new)
                .filter(ctx -> ctx.getDomainData().exists(dom -> dom.getControllerReplicas() == 0))
                .doOnNext(ctx -> logger.info("Scaling up domain " + ctx.getDomain()))
                .flatMapIterable(DeployContext::getDomainData)
                .map(dom -> new KubelessReplicaChangeRequest(dom.getControllerName(), 1))
                .doOnNext(chr -> logger.info("Scaling up controller " + chr))
                .observeOn(Schedulers.io())
                .map(kubernetesAPI::scale)
                .flatMap(Future::setHandlerObservable)
                .subscribe();
    }

    private static class DeployContext {

        private String domain;

        private KubelessModel model;

        public DeployContext(String domain, KubelessModel model) {
            this.domain = domain;
            this.model = model;
        }

        public String getDomain() {
            return domain;
        }

        public KubelessModel getModel() {
            return model;
        }

        public Option<KubelessDomain> getDomainData() {
            return model.getDomain(this.domain);
        }

    }
}
