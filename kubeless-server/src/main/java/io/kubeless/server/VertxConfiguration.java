package io.kubeless.server;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import rx.plugins.RxJavaHooks;
import rx.plugins.RxJavaSchedulersHook;

/**
 *
 */
@Configuration
public class VertxConfiguration {

    @Bean
    public Vertx vertx() {
        Vertx vertx = Vertx.vertx();
        RxJavaSchedulersHook hook = RxHelper.schedulerHook(vertx);

        RxJavaHooks.onComputationScheduler(hook.getComputationScheduler());
        RxJavaHooks.onIOScheduler(hook.getIOScheduler());
        RxJavaHooks.onNewThreadScheduler(hook.getNewThreadScheduler());

        return vertx;
    }

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

}
