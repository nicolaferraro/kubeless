package io.kubeless.server;

import javax.annotation.PostConstruct;

import io.vertx.rxjava.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Server {

    @Autowired
    private Vertx vertx;

    @Autowired
    private ModelWatcherVerticle synchronizer;

    @Autowired
    private ModelChangeDetectorVerticle changeDetector;

    @Autowired
    private ModelUpdaterVerticle updater;

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @PostConstruct
    public void deploy() throws Exception {
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(synchronizer);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(changeDetector);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(updater);
    }

}
