package io.kubeless;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.Vertx;

/**
 *
 */
public class Server {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(DispatcherVerticle.class.getCanonicalName());
        vertx.deployVerticle(CloudModelVerticle.class.getCanonicalName());

    }

}
