package io.kubeless.server;

import io.vertx.rxjava.core.Future;

import rx.Observable;


public interface KubernetesAPI {

    /**
     * Retrieves the Kubeless model from the current kubernetes namespace.
     *
     * @return the Kubeless model
     */
    Observable<KubelessModel> kubelessModel();

    /**
     * Scales the controller to the specified number of replicas.
     *
     * @param request the change request
     * @return an async future
     */
    Future<Void> scale(KubelessReplicaChangeRequest request);

}
