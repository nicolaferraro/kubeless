package io.kubeless.server.integration.eventbus;

import javax.annotation.PostConstruct;

import io.kubeless.server.KubelessBusAPI;
import io.kubeless.server.model.KubelessReplicaChangeRequest;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 *
 */
@Component
public class KubelessBusAPIImpl implements KubelessBusAPI {

    @Autowired
    private Vertx vertx;

    private PublishSubject<KubelessReplicaChangeRequest> changes;

    private PublishSubject<String> requestedDomains;

    @PostConstruct
    public void init() {
        EventBus eventBus = vertx.eventBus();

        this.changes = PublishSubject.create();
        this.requestedDomains = PublishSubject.create();

        eventBus.consumer(KubelessAddress.DOMAIN_REQUESTED.get())
                .toObservable()
                .map(m -> (String) m.body())
                .subscribe(this.requestedDomains);
    }

    @Override
    public void pushRequestedDomain(String domain) {
        EventBus eventBus = vertx.eventBus();
        eventBus.publish(KubelessAddress.DOMAIN_REQUESTED.get(), domain);
    }

    @Override
    public Observable<String> requestedDomains() {
        return this.requestedDomains;
    }
}
