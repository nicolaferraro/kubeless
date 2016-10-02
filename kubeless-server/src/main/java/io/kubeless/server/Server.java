package io.kubeless.server;

import javax.annotation.PostConstruct;

import io.vertx.rxjava.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javaslang.Tuple;
import javaslang.control.Option;
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

    @Autowired
    private DispatcherVerticle dispatcher;


    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @PostConstruct
    public void deploy() throws Exception {
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(watcher);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(changeDetector);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(updater);
        ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(dispatcher);


        Observable<KubelessModel> currentModel = vertx.eventBus().consumer("kubeless.model.current").toObservable()
                .map(m -> (KubelessModel) m.body());


        vertx.eventBus().consumer("domain.requested").toObservable()
                .map(msg -> (String) msg.body())
                .withLatestFrom(currentModel, Tuple::of)
                .subscribe(t -> {
                    String domainName = t._1;
                    KubelessModel model = t._2;
                    Option<KubelessModel> desiredModel = model
                            .getDomain(domainName)
                            .filter(domain -> domain.getControllerReplicas() == 0)
                            .map(domain -> model.withReplicas(domainName, 1));
                    desiredModel.forEach(m -> vertx.eventBus().publish("kubeless.model.desired", m));
                });


//        HttpServer server = vertx.createHttpServer();
//
//        server.requestStream().toObservable().subscribe(res -> {
//            System.out.println("BBB");
//        }, err -> {
//            System.out.println("ERR");
//        }, () -> {
//            System.out.println("CPP");
//        });
//
//        server.listen(8008);


//        EventBus eventBus = vertx.eventBus();
//
//        Observable<KubelessModel> currentModel = eventBus.consumer("kubeless.model.current").toObservable()
//                .map(m -> (KubelessModel) m.body());
//
//        Observable.interval(8, 8, TimeUnit.SECONDS).withLatestFrom(currentModel, (t, model) -> model)
//                .map(model -> model.withReplicas("hello", (int)(Math.random()*5)))
//                .subscribe(model -> {
//                    eventBus.publish("kubeless.model.desired", model);
//                });

    }

}
