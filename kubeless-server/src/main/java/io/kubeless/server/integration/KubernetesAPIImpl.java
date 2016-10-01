package io.kubeless.server.integration;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kubeless.server.KubelessModel;
import io.kubeless.server.KubernetesAPI;
import io.kubeless.server.KubelessReplicaChangeRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javaslang.collection.List;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 *
 */
@Component
public class KubernetesAPIImpl implements KubernetesAPI {

    @Autowired
    private KubernetesClient client;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Observable<KubelessModel> kubelessModel() {

        Observable<List<Service>> services = kubernetesServices();
        Observable<List<ReplicationController>> controllers = kubernetesControllers();

        return Observable.zip(services, controllers, (s,c) -> new KubelessModelBuilder(s, c).build());
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

    private Observable<List<Service>> kubernetesServices() {
        return Observable.just(0)
                .subscribeOn(Schedulers.io())
                .doOnEach(x -> logger.debug("Retrieving the list of services"))
                .map(t -> List.ofAll(client.services().list().getItems()));
    }

    private Observable<List<ReplicationController>> kubernetesControllers() {
        return Observable.just(0)
                .subscribeOn(Schedulers.io())
                .doOnNext(x -> logger.debug("Retrieving the list of controllers"))
                .map(t -> List.ofAll(client.replicationControllers().list().getItems()));
    }

}
