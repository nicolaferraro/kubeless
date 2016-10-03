package io.kubeless.server;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Scales down the pods after a timeout.
 */
@Component
public class UndeployerVerticle extends AbstractVerticle {

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

        Observable<KubelessModel> currentModel = eventBus.consumer("kubeless.model.current").toObservable()
                .map(m -> (KubelessModel) m.body());

        Observable<Map<String, Long>> lastUsed = eventBus.consumer("domain.requested").toObservable()
                .map(m -> (String) m.body())
                .scan(HashMap.<String, Long>empty(), (map, domain) -> map.put(domain, System.currentTimeMillis()));

        Observable.interval(3, 1, TimeUnit.SECONDS)
                .withLatestFrom(lastUsed, (tick, usage) -> usage)
                .withLatestFrom(currentModel, (usage, model) ->
                        usage
                            .filter(t -> model.getDomains().keySet().contains(t._1))
                            .merge(model.getDomains()
                                .keySet()
                                .removeAll(usage.keySet())
                                .map(domain -> Tuple.of(domain, Long.MIN_VALUE))
                                .toMap(Function.identity()))
                            .removeAll(model.getDomains().filter(t -> t._2.getControllerReplicas()==0).keySet())
                            .map(t -> Tuple.of(model.getDomain(t._1).get().getControllerName(), t._2))
                            .toMap(Function.identity())
                )
                .map(usage -> usage.filter(controllerUsage -> controllerUsage._2 < System.currentTimeMillis() - 5000))
                .flatMapIterable(usage -> usage.keySet())
                .map(controller -> new KubelessReplicaChangeRequest(controller, 0))
                .map(kubernetesAPI::scale)
                .flatMap(Future::setHandlerObservable)
                .subscribe();

    }
}
