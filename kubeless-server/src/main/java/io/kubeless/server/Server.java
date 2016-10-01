package io.kubeless.server;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import rx.Observable;

@SpringBootApplication
public class Server {

    @Autowired
    private Vertx vertx;

    @Autowired
    private ModelWatcherVerticle watcher;

    @Autowired
    private ModelChangeDetectorVerticle changeDetector;

    @Autowired
    private ModelUpdaterVerticle updater;

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @PostConstruct
    public void deploy() throws Exception {
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(watcher);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(changeDetector);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(updater);


        EventBus eventBus = vertx.eventBus();

        Observable<KubelessModel> currentModel = eventBus.consumer("kubeless.model.current").toObservable()
                .map(m -> (KubelessModel) m.body());

        Observable.interval(8, 8, TimeUnit.SECONDS).withLatestFrom(currentModel, (t, model) -> model)
                .map(model -> model.withReplicas("hello", (int)(Math.random()*5)))
                .subscribe(model -> {
                    eventBus.publish("kubeless.model.desired", model);
                });

    }

}
