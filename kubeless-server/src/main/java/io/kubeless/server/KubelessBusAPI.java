package io.kubeless.server;

import rx.Observable;


public interface KubelessBusAPI {

    void pushRequestedDomain(String domain);

    Observable<String> requestedDomains();

}
