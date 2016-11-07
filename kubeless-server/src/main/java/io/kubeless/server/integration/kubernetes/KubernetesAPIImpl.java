package io.kubeless.server.integration.kubernetes;

import javax.annotation.PostConstruct;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.kubeless.server.model.KubelessModel;
import io.kubeless.server.KubernetesAPI;
import io.kubeless.server.model.KubelessReplicaChangeRequest;
import io.kubeless.server.util.Vertexizer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.collection.List;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.SerializedSubject;

/**
 *
 */
@Component
public class KubernetesAPIImpl implements KubernetesAPI {

    @Autowired
    private KubernetesClient client;

    @Autowired
    private Vertx vertx;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Observable<KubelessModel> kubelessModel;

    @PostConstruct
    public void init() {
        KubelessModel initialModel = pollKubelessModel().toBlocking().single();

        BehaviorSubject<KubelessModel> kubelessModel = BehaviorSubject.create();
        kubelessModel.onNext(initialModel);

        SerializedSubject<KubelessModel, KubelessModel> subject = kubelessModel.toSerialized();

        client.services().watch((WatcherAdapter<Service>) (action, obj) -> pollKubelessModel().subscribe(subject::onNext));
        client.replicationControllers().watch((WatcherAdapter<ReplicationController>) (action, obj) -> pollKubelessModel().subscribe(subject::onNext));

        logger.info("Kubeless Model BehaviourSubject created");

        this.kubelessModel = Vertexizer.cleanBehaviour(vertx, subject.distinctUntilChanged());
    }

    @Override
    public Observable<KubelessModel> kubelessModel() {
        return this.kubelessModel;
    }

    /**
     * Scales the controller to the specified number of replicas.
     *
     * @param request the change request
     * @return an async future
     */
    @Override
    public Future<Void> scale(KubelessReplicaChangeRequest request) {
        try {
            ReplicationController controller = client.replicationControllers().withName(request.getController()).get();
            ReplicationControllerSpec specs = controller.getSpec();
            specs.setReplicas(request.getReplicas());

            client.replicationControllers().withName(request.getController()).edit().withSpec(specs).done();

            return Future.succeededFuture();
        } catch (Throwable t) {
            String message = "Error while scaling the resource " + request;
            logger.error(message, t);
            return Future.failedFuture(message);
        }
    }

    private Observable<KubelessModel> pollKubelessModel() {
        Observable<List<Service>> services = pollKubernetesServices();
        Observable<List<ReplicationController>> controllers = pollKubernetesControllers();

        return Observable.zip(services, controllers, (s,c) -> new KubelessModelBuilder(s, c).build())
                .doOnError(e -> logger.error("Error while retrieving the model", e))
                .onErrorResumeNext(Observable.empty());
    }

    private Observable<List<Service>> pollKubernetesServices() {
        return Observable.just(0)
                .subscribeOn(Schedulers.io())
                .doOnEach(x -> logger.debug("Retrieving the list of services"))
                .map(t -> List.ofAll(client.services().list().getItems()))
                .doOnNext(e -> logger.debug("Query result from Kubernetes API - Services: {}", e));
    }

    private Observable<List<ReplicationController>> pollKubernetesControllers() {
        return Observable.just(0)
                .subscribeOn(Schedulers.io())
                .doOnNext(x -> logger.debug("Retrieving the list of controllers"))
                .map(t -> List.ofAll(client.replicationControllers().list().getItems()))
                .doOnNext(e -> logger.debug("Query result from Kubernetes API - Replication controllers: {}", e));
    }

    @FunctionalInterface
    private interface WatcherAdapter<T> extends Watcher<T> {

        Logger logger = LoggerFactory.getLogger(WatcherAdapter.class);

        @Override
        default void onClose(KubernetesClientException cause) {
            if (cause != null) {
                logger.warn("Exception thrown while watching", cause);
            }
        }
    }

}
