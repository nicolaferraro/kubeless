package io.kubeless.server;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerRequest;

import com.sun.org.apache.xpath.internal.operations.Bool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 *
 */
@Component
public class BugVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(BugVerticle.class);

    @Autowired
    private Vertx vertx;

    @Autowired
    private KubernetesAPI kubernetesAPI;

    @Autowired
    private KubelessBusAPI kubelessBusAPI;


    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        HttpServer server = vertx.createHttpServer();

//        io.vertx.core.Vertx v = (io.vertx.core.Vertx ) vertx.getDelegate();
//
//        server.requestStream().handler(req -> {
//            req.pause();
//            v.timerStream(1000).handler(t -> {
//                req.resume();
//                req.handler(data -> logger.info("RECEIVED " + data));
//                req.response().setStatusCode(200).setChunked(true).write("Hello World").end();
//            });
//        });




//        ObservableHandler<HttpServerRequest> requests = new ObservableHandler<>();



        Supplier<Observable<Boolean>> timerSupplier = () -> {
            SerializedSubject<Boolean, Boolean> ser = PublishSubject.<Boolean>create().toSerialized();
            vertx.timerStream(5000).handler(t -> ser.onNext(true));
            return ser.startWith(false);
        };

//        timerSupplier.get().filter(t -> t).subscribe(t -> logger.info("AAAAAA " + t));
//        timerSupplier.get().subscribe(t -> logger.info("BBB " + t));


//        server.requestStream()
//                .handler(req -> {
//                    req.pause();
//                    requests.toHandler().handle(req);
//                });

//        requests

        Observable<Integer> o2 = Observable.just(1).delay(3, TimeUnit.SECONDS);

        server.requestStream().toObservable()
                .doOnNext(req -> req.pause())
                .flatMap(req -> Observable.combineLatest(timerSupplier.get().map(t -> req), o2, (a, b) -> a))
                .doOnNext(req -> req.pause())
                .doOnNext(a-> logger.info("HEREEEEE"))
                .subscribe(req -> {
                    req.resume();
                    req.handler(data -> logger.info("RECEIVED " + data));

                    req.response().setStatusCode(200).setChunked(true).write("Hello World").end();
                });




        server.listen(9000);

    }


}

