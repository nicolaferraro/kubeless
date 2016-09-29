package io.kubeless.server;

import java.util.function.Function;

import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.control.Option;

/**
 *
 */
public class KubelessModel {

    private Map<String, List<String>> services;

    private Map<String, List<String>> controllers;

    private Map<String, Integer> replicas;

    public KubelessModel(Map<String, List<String>> services, Map<String, List<String>> controllers, Map<String, Integer> replicas) {
        this.services = services;
        this.controllers = controllers;
        this.replicas = replicas;
    }

    public Map<String, List<String>> getServices() {
        return services;
    }

    public Map<String, List<String>> getControllers() {
        return controllers;
    }

    public Map<String, Integer> getReplicas() {
        return replicas;
    }

    public Set<String> getDomains() {
        return services.keySet().union(controllers.keySet());
    }

    public List<String> getServices(String domain) {
        return services.get(domain).getOrElse(List.empty());
    }

    public List<String> getControllers(String domain) {
        return controllers.get(domain).getOrElse(List.empty());
    }

    public Option<Integer> getReplicas(String controller) {
        return replicas.get(controller);
    }

    public Map<String, Integer> computeChanges(KubelessModel model) {
        return this.replicas.toList().removeAll(model.replicas.toList()).toMap(Function.identity());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KubelessModel that = (KubelessModel) o;

        if (services != null ? !services.equals(that.services) : that.services != null) return false;
        if (controllers != null ? !controllers.equals(that.controllers) : that.controllers != null) return false;
        return replicas != null ? replicas.equals(that.replicas) : that.replicas == null;

    }

    @Override
    public int hashCode() {
        int result = services != null ? services.hashCode() : 0;
        result = 31 * result + (controllers != null ? controllers.hashCode() : 0);
        result = 31 * result + (replicas != null ? replicas.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KubelessModel{");
        sb.append("services=").append(services);
        sb.append(", controllers=").append(controllers);
        sb.append(", replicas=").append(replicas);
        sb.append('}');
        return sb.toString();
    }
}
