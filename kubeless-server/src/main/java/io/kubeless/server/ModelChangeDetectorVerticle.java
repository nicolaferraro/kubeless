package io.kubeless.server;

import io.kubeless.server.model.KubelessModel;
import io.kubeless.server.model.KubelessReplicaChangeRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.Tuple;
import javaslang.collection.List;
import rx.Observable;

/**
 * Detects changes in the desired state of the model and create corrective actions.
 */
@Component
public class ModelChangeDetectorVerticle extends AbstractVerticle {

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

        Observable<KubelessModel> desiredModel = eventBus.consumer("kubeless.model.desired").toObservable()
                .map(m -> (KubelessModel) m.body());


        desiredModel.withLatestFrom(currentModel, Tuple::of)
                .subscribe(t -> {
                    KubelessModel desired = t._1;
                    KubelessModel comparison = t._2;

                    List<KubelessReplicaChangeRequest> changes = desired.computeReplicaChanges(comparison);
                    changes.forEach(change -> {
                        eventBus.publish("kubeless.changes", change);
                    });

                    eventBus.publish("kubeless.model.current", desired);
                });

    }
}
