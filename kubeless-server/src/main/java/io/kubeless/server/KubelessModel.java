package io.kubeless.server;

import java.util.function.Function;

import io.vertx.core.shareddata.Shareable;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.control.Option;

/**
 *
 */
public class KubelessModel implements Shareable {

    private Map<String, KubelessDomain> domains;

    public KubelessModel(Map<String, KubelessDomain> domains) {
        this.domains = domains;
    }

    public List<KubelessReplicaChangeRequest> computeReplicaChanges(KubelessModel model) {
        return replicaStatus(this).removeAll(replicaStatus(model)).map(KubelessReplicaChangeRequest::new);
    }

    public KubelessModel withReplicas(String domain, int replicas) {
        return new KubelessModel(domains.put(domain, domains.apply(domain).withReplicas(replicas)));
    }

    private static List<Tuple2<String, Integer>> replicaStatus(KubelessModel model) {
        return model.domains.values().map(d -> Tuple.of(d.getControllerName(), d.getControllerReplicas())).toList();
    }

    public Map<String, KubelessDomain> getDomains() {
        return domains;
    }

    public Option<KubelessDomain> getDomain(String domain) {
        return domains.get(domain);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KubelessModel that = (KubelessModel) o;

        return domains != null ? domains.equals(that.domains) : that.domains == null;
    }

    @Override
    public int hashCode() {
        return domains != null ? domains.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KubelessModel{");
        sb.append("domains=").append(domains);
        sb.append('}');
        return sb.toString();
    }
}
