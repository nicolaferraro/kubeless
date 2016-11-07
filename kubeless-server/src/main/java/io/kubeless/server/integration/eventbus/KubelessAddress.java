package io.kubeless.server.integration.eventbus;

/**
 *
 */
public enum KubelessAddress {

    DOMAIN_REQUESTED("domain.requested");

    private String address;

    KubelessAddress(String address) {
        this.address = address;
    }

    public String get() {
        return address;
    }

}
