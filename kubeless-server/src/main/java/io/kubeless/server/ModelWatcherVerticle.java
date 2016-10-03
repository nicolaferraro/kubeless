package io.kubeless.server;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.shareddata.LocalMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;

/**
 * Maintains an updated version of the model.
 */
@Component
public class ModelWatcherVerticle extends AbstractVerticle {

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

        Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                .flatMap(t ->
                        kubernetesAPI.kubelessModel()
                                .doOnError(e -> logger.error("Error while retrieving the model", e))
                                .onErrorResumeNext(Observable.empty())
                        )
                .distinctUntilChanged()
                .doOnNext(model -> logger.info("Retrieved new model from Kubernetes: " + model))
                .subscribe(model -> eventBus.publish("kubeless.model.current", model));

    }
}
