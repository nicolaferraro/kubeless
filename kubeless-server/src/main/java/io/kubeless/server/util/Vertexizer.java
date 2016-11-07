package io.kubeless.server.util;

import io.vertx.rxjava.core.Vertx;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.SerializedSubject;

/**
 * A behaviour cleaner to overcome https://github.com/vert-x3/vertx-rx/issues/57
 */
public class Vertexizer {

    private Vertexizer() {
    }

    public static <T> Observable<T> cleanBehaviour(Vertx vertx, Observable<T> obs) {
        SerializedSubject<T, T> pub = BehaviorSubject.<T>create().toSerialized();
        obs.subscribe(t -> vertx.runOnContext(c -> pub.onNext(t)));
        return pub;
    }

}
