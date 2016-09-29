package io.kubeless.server;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.schedulers.Schedulers;

/**
 * Updates the model according to the desired state.
 */
@Component
public class ModelUpdaterVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private KubernetesAPI kubernetesAPI;

    @Autowired
    private Vertx vertx;

    /**
     * If your verticle does a simple, synchronous start-up then override this method and put your start-up
     * code in there.
     * @throws Exception
     */
    @Override
    public void start() throws Exception {

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer("kubeless.changes").toObservable()
                .map(m -> (ReplicaChangeRequest) m.body())
                .observeOn(Schedulers.io())
                .map(kubernetesAPI::scale)
                .flatMap(Future::setHandlerObservable)
                .subscribe();
    }
}
