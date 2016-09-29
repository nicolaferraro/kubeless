package io.kubeless.server;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.shareddata.LocalMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.Tuple;
import javaslang.collection.List;
import javaslang.control.Option;
import rx.Observable;

/**
 * Detects changes in the desired state of the model.
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

        LocalMap<String, KubelessModel> kubelessModels = vertx.sharedData().getLocalMap("kubeless.model");

        EventBus eventBus = vertx.eventBus();

        Observable<KubelessModel> currentModel = eventBus.consumer("kubeless.model.current").toObservable()
                .map(m -> (KubelessModel) m.body());

        Observable<KubelessModel> desiredModel = eventBus.consumer("kubeless.model.desired").toObservable()
                .map(m -> (KubelessModel) m.body());


        desiredModel.withLatestFrom(currentModel, Tuple::of)
                .subscribe(t -> {
                    KubelessModel desired = t._1;
                    KubelessModel comparison = Option.of(kubelessModels.get("pushed")).getOrElse(t._2);
                    List<ReplicaChangeRequest> changes = desired.computeChanges(comparison).toList().map(ReplicaChangeRequest::new);

                    kubelessModels.put("pushed", desired);
                    changes.forEach(change -> {
                        eventBus.send("kubeless.changes", change);
                    });
                });

    }
}
