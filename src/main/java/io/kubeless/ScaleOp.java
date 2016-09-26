package io.kubeless;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

/**
 *
 */
public class ScaleOp {

    private static KubernetesClient client = new DefaultKubernetesClient();

    public static Future<Void> scale(String replicationController, int replicas) {
        try {
            ReplicationController controller = client.replicationControllers().withName(replicationController).get();
            ReplicationControllerSpec specs = controller.getSpec();
            specs.setReplicas(replicas);

            client.replicationControllers().withName(replicationController).edit().withSpec(specs).done();

            return Future.succeededFuture();
        } catch (Throwable t) {
            return Future.failedFuture(t);
        }
    }

}
