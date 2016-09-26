package io.kubeless;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.streams.Pump;

/**
 *
 */
public class CloudModelVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(CloudModelVerticle.class);

    private static final String KUBELESS_DOMAIN_LABEL_KEY = "io.kubeless.domain";

    /**
     * Start the verticle.<p>
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.<p>
     * If your verticle does things in it's startup which take some time then you can override this method
     * and call the startFuture some time later when start up is complete.
     * @param startFuture  a future which should be called when verticle start-up is complete.
     * @throws Exception
     */
    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        KubernetesClient client = new DefaultKubernetesClient();

        SharedData data = getVertx().sharedData();

        LocalMap<String, String> serviceMap = data.getLocalMap("io.kubeless.services");
        LocalMap<String, String> replicationControllerMap = data.getLocalMap("io.kubeless.replication.controllers");

        getVertx().setPeriodic(3000, t -> {

            Future<ServiceList> f1 = Future.future();
            getVertx().executeBlocking(future -> {
                try {
                    future.complete(client.services().list());
                } catch (Throwable thr) {
                    f1.fail(thr);
                }
            }, false, res -> {
                if(res.succeeded()) {
                    f1.complete((ServiceList) res.result());
                } else {
                    f1.fail(res.cause());
                }
            });

            Future<ReplicationControllerList> f2 = Future.future();
            getVertx().executeBlocking(future -> {
                try {
                    future.complete(client.replicationControllers().list());
                } catch (Throwable thr) {
                    f2.fail(thr);
                }
            }, false, res -> {
                if(res.succeeded()) {
                    f2.complete((ReplicationControllerList) res.result());
                } else {
                    f2.fail(res.cause());
                }
            });

            CompositeFuture.all(f1, f2).setHandler(res -> {
                if(res.succeeded()) {
                    ServiceList services = res.result().resultAt(0);
                    ReplicationControllerList controllers = res.result().resultAt(1);

                    Map<String, String> newServiceMap = new HashMap<>();
                    for(Service s : services.getItems()) {
                        String domain = s.getMetadata().getLabels().get(KUBELESS_DOMAIN_LABEL_KEY);
                        if(domain!=null) {
                            newServiceMap.put(domain, s.getMetadata().getName());
                        }
                    }

                    Map<String, String> newReplicationControllerMap = new HashMap<>();
                    for(ReplicationController rc : controllers.getItems()) {
                        String domain = rc.getMetadata().getLabels().get(KUBELESS_DOMAIN_LABEL_KEY);
                        if(domain!=null) {
                            newReplicationControllerMap.put(domain, rc.getMetadata().getName());
                        }
                    }

                    Set<String> removedServices = new HashSet<>(serviceMap.keySet());
                    removedServices.removeAll(newServiceMap.keySet());
                    for(String removed : removedServices) {
                        serviceMap.remove(removed);
                    }
                    for(String service : newServiceMap.keySet()) {
                        serviceMap.put(service, newServiceMap.get(service));
                    }

                    Set<String> removedControllers = new HashSet<>(replicationControllerMap.keySet());
                    removedControllers.removeAll(newReplicationControllerMap.keySet());
                    for(String removed : removedControllers) {
                        replicationControllerMap.remove(removed);
                    }
                    for(String controller : newReplicationControllerMap.keySet()) {
                        replicationControllerMap.put(controller, newReplicationControllerMap.get(controller));
                    }

                    System.out.println(serviceMap.values());
                    System.out.println(replicationControllerMap.values());
                }
            });
        });

    }
}
