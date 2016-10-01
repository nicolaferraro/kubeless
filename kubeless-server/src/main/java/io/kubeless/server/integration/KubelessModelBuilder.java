package io.kubeless.server.integration;

import java.util.function.Function;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.kubeless.server.KubelessDomain;
import io.kubeless.server.KubelessModel;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javaslang.Tuple;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Set;

/**
 * Builds a Kubeless model from the data retrieved from Kubernetes.
 */
public class KubelessModelBuilder {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String KUBELESS_DOMAIN_ANNOTATION_KEY = "kubeless.io-domain";

    private List<Service> services;

    private List<ReplicationController> controllers;

    // Common predicates
    private Predicate<HasMetadata> kubelessObject = kubernetesObject -> kubernetesObject.getMetadata().getAnnotations().containsKey(KUBELESS_DOMAIN_ANNOTATION_KEY);
    private Predicate<Integer> allowedPort = port -> List.of(80, 8080, 9000).contains(port.intValue());
    private Predicate<Service> exposedService = service -> List.ofAll(service.getSpec().getExternalIPs()).nonEmpty();
    private Predicate<Service> allowedService = service -> exposedService.test(service) && List.ofAll(service.getSpec().getPorts()).map(sp -> sp.getPort()).filter(allowedPort).nonEmpty();

    public KubelessModelBuilder(List<Service> services, List<ReplicationController> controllers) {
        this.services = services;
        this.controllers = controllers;
    }

    public KubelessModel build() {

        Map<String, Service> modelServices = services.filter(allowedService.and(kubelessObject))
                .groupBy(s -> s.getMetadata().getAnnotations().get(KUBELESS_DOMAIN_ANNOTATION_KEY))
                .mapValues(serviceList -> serviceList.get());

        Map<String, ReplicationController> modelControllers = controllers.filter(kubelessObject)
                .groupBy(c -> c.getMetadata().getAnnotations().get(KUBELESS_DOMAIN_ANNOTATION_KEY))
                .mapValues(controllerList -> controllerList.get());

        Set<String> domains = modelServices.keySet().intersect(modelControllers.keySet());

        Map<String, KubelessDomain> modelDomains = domains.map(d -> Tuple.of(d, makeDomain(d, modelServices.apply(d), modelControllers.apply(d)))).toMap(Function.identity());

        return new KubelessModel(modelDomains);
    }


    private KubelessDomain makeDomain(String domain, Service service, ReplicationController controller) {

        String serviceName = service.getMetadata().getName();

        String serviceHost = service.getSpec().getExternalIPs().get(0);

        Integer servicePort = List.ofAll(service.getSpec().getPorts()).map(sp -> sp.getPort()).filter(allowedPort).get();

        String controllerName = controller.getMetadata().getName();

        Integer controllerReplicas = controller.getSpec().getReplicas();

        return new KubelessDomain(domain, serviceName, serviceHost, servicePort, controllerName, controllerReplicas);
    }


}
