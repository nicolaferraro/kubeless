package io.kubeless.server;

import javaslang.Tuple2;

/**
 *
 */
public class KubelessReplicaChangeRequest {

    private String controller;

    private int replicas;

    public KubelessReplicaChangeRequest(Tuple2<String, Integer> replicaInfo) {
        this.controller = replicaInfo._1;
        this.replicas = replicaInfo._2;
    }

    public KubelessReplicaChangeRequest(String controller, int replicas) {
        this.controller = controller;
        this.replicas = replicas;
    }

    public String getController() {
        return controller;
    }

    public int getReplicas() {
        return replicas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KubelessReplicaChangeRequest that = (KubelessReplicaChangeRequest) o;

        if (replicas != that.replicas) return false;
        return controller != null ? controller.equals(that.controller) : that.controller == null;

    }

    @Override
    public int hashCode() {
        int result = controller != null ? controller.hashCode() : 0;
        result = 31 * result + replicas;
        return result;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KubelessReplicaChangeRequest{");
        sb.append("controller='").append(controller).append('\'');
        sb.append(", replicas=").append(replicas);
        sb.append('}');
        return sb.toString();
    }
}
