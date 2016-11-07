package io.kubeless.server;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Just logs
 */
@Component
public class ModelLoggerVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private KubernetesAPI kubernetesAPI;

    @Override
    public void start() throws Exception {
        kubernetesAPI.kubelessModel()
            .subscribe(model -> logger.info("Retrieved new model from Kubernetes: " + model));

    }

}
