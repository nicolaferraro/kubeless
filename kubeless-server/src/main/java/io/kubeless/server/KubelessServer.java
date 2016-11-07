package io.kubeless.server;

import javax.annotation.PostConstruct;

import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KubelessServer {

    @Autowired
    private Vertx vertx;

    @Autowired
    private ModelLoggerVerticle logger;

    @Autowired
    private DeployerVerticle deployer;

    @Autowired
    private UndeployerVerticle undeployer;

    @Autowired
    private ProxyVerticle proxy;

    @Autowired
    private BugVerticle bug;


    public static void main(String[] args) {
        SpringApplication.run(KubelessServer.class, args);
    }

    @PostConstruct
    public void deploy() throws Exception {

        RxHelper.deployVerticle(vertx, logger);
        RxHelper.deployVerticle(vertx, deployer);
        RxHelper.deployVerticle(vertx, undeployer);
        RxHelper.deployVerticle(vertx, proxy);

        RxHelper.deployVerticle(vertx, bug);

    }

}
