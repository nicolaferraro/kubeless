package io.kubeless.server;

import io.vertx.core.shareddata.Shareable;

/**
 * Represents a single kubeless domain.
 */
public class KubelessDomain implements Shareable {

    private String domain;

    private String serviceName;

    private String serviceHost;

    private Integer servicePort;

    private String controllerName;

    private Integer controllerReplicas;

    public KubelessDomain(String domain, String serviceName, String serviceHost, Integer servicePort, String controllerName, Integer controllerReplicas) {
        this.domain = domain;
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        this.servicePort = servicePort;
        this.controllerName = controllerName;
        this.controllerReplicas = controllerReplicas;
    }

    public KubelessDomain withReplicas(int replicas) {
        return new KubelessDomain(domain, serviceName, serviceHost, servicePort, controllerName, replicas);
    }

    public String getDomain() {
        return domain;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public Integer getServicePort() {
        return servicePort;
    }

    public String getControllerName() {
        return controllerName;
    }

    public Integer getControllerReplicas() {
        return controllerReplicas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KubelessDomain that = (KubelessDomain) o;

        if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
        if (serviceHost != null ? !serviceHost.equals(that.serviceHost) : that.serviceHost != null) return false;
        if (servicePort != null ? !servicePort.equals(that.servicePort) : that.servicePort != null) return false;
        if (controllerName != null ? !controllerName.equals(that.controllerName) : that.controllerName != null) return false;
        return controllerReplicas != null ? controllerReplicas.equals(that.controllerReplicas) : that.controllerReplicas == null;

    }

    @Override
    public int hashCode() {
        int result = domain != null ? domain.hashCode() : 0;
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        result = 31 * result + (serviceHost != null ? serviceHost.hashCode() : 0);
        result = 31 * result + (servicePort != null ? servicePort.hashCode() : 0);
        result = 31 * result + (controllerName != null ? controllerName.hashCode() : 0);
        result = 31 * result + (controllerReplicas != null ? controllerReplicas.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KubelessDomain{");
        sb.append("domain='").append(domain).append('\'');
        sb.append(", serviceName='").append(serviceName).append('\'');
        sb.append(", serviceHost='").append(serviceHost).append('\'');
        sb.append(", servicePort=").append(servicePort);
        sb.append(", controllerName='").append(controllerName).append('\'');
        sb.append(", controllerReplicas='").append(controllerReplicas).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
