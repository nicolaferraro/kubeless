package io.kubeless.server;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kubeless.server.model.KubelessModel;
import io.kubeless.server.model.KubelessReplicaChangeRequest;
import io.kubeless.server.util.GenericJsonCodec;
import io.vertx.core.eventbus.EventBus;
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

        // Hooks for RxJava schedulers
        RxJavaSchedulersHook hook = RxHelper.schedulerHook(vertx);
        RxJavaHooks.onComputationScheduler(hook.getComputationScheduler());
        RxJavaHooks.onIOScheduler(hook.getIOScheduler());
        RxJavaHooks.onNewThreadScheduler(hook.getNewThreadScheduler());

        // Default codecs
        EventBus eventBus = ((EventBus) vertx.eventBus().getDelegate());
        eventBus.registerDefaultCodec(KubelessModel.class, GenericJsonCodec.of(KubelessModel.class));
        eventBus.registerDefaultCodec(KubelessReplicaChangeRequest.class, GenericJsonCodec.of(KubelessReplicaChangeRequest.class));

        return vertx;
    }

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

}
