package io.kubeless.server.integration;

import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.kubeless.server.KubelessModel;

import javaslang.collection.List;

/**
 * Builds a Kubeless model from the data retrieved from Kubernetes.
 */
public class KubelessModelBuilder {

    private static final String KUBELESS_DOMAIN_LABEL_KEY = "io.kubeless.domain";

    private List<Service> services;

    private List<ReplicationController> controllers;

    public KubelessModelBuilder(List<Service> services, List<ReplicationController> controllers) {
        this.services = services;
        this.controllers = controllers;
    }

    public KubelessModel build() {
        KubelessModel model = new KubelessModel();

        Predicate<HasMetadata> kubelessObject = kubernetesObject -> kubernetesObject.getMetadata().getLabels().containsKey(KUBELESS_DOMAIN_LABEL_KEY);

        model.setServices(services.filter(kubelessObject)
                .groupBy(s -> s.getMetadata().getLabels().get(KUBELESS_DOMAIN_LABEL_KEY))
                .mapValues(serviceList -> serviceList.map(s -> s.getMetadata().getName())));

        model.setControllers(controllers.filter(kubelessObject)
                .groupBy(c -> c.getMetadata().getLabels().get(KUBELESS_DOMAIN_LABEL_KEY))
                .mapValues(controllerList -> controllerList.map(c -> c.getMetadata().getName())));

        model.setInstances(controllers.filter(kubelessObject)
                .groupBy(c -> c.getMetadata().getName())
                .mapValues(controllerList -> controllerList.get().getStatus().getReplicas()));

        return model;
    }


}
