package io.kubeless.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.streams.Pump;

/**
 *
 */
public class DispatcherVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(DispatcherVerticle.class);

    /**
     * Start the verticle.<p>
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.<p>
     * If your verticle does things in it's startup which take some time then you can override this method
     * and call the startFuture some time later when start up is complete.
     * @param startFuture  a future which should be called when verticle start-up is complete.
     * @throws Exception
     */
    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        HttpServer server = getVertx().createHttpServer();

        HttpClient client = getVertx().createHttpClient();

        SharedData sharedData = getVertx().sharedData();

        LocalMap<String, String> serviceMap = sharedData.getLocalMap("io.kubeless.services");
        LocalMap<String, String> replicationControllerMap = sharedData.getLocalMap("io.kubeless.replication.controllers");

        server.requestHandler(req -> {
            try {
                logger.info("Request arrived host=" + req.host() + ", uri=" + req.uri());
                String hostPort = req.host();
                String host = hostPort;
                int port = 80;
                if (hostPort != null && hostPort.indexOf(":") > 0) {
                    host = hostPort.substring(0, hostPort.indexOf(":"));
                    port = Integer.parseInt(hostPort.substring(hostPort.indexOf(":") + 1));
                }

                // doscale

//                if(replicationControllerMap.get("datagrid")!=null) {
//                    ScaleOp.scale(replicationControllerMap.get("datagrid"), 1).setHandler(res -> {
//                        System.out.println("RES " + res.succeeded());
//                        if (!res.succeeded()) {
//                            res.cause().printStackTrace();
//                        }
//                    });
//                }

                HttpClientRequest proxiedRequest = client.get(port, host, req.uri(), res -> {
                    req.response().setStatusCode(res.statusCode());
                    req.response().headers().setAll(res.headers());


//                    res.handler(data -> {
//                        req.response().write(data);
//                    });

                    Pump.pump(res, req.response()).start();

                    res.endHandler(v -> req.response().end());
                });

                proxiedRequest.connectionHandler(conn -> {
                    conn.closeHandler((v) -> {
                        req.connection().close();
                    });
                });

                proxiedRequest.exceptionHandler(e -> {
                    e.printStackTrace();
                    req.connection().close();
                });

                proxiedRequest.headers().setAll(req.headers());
                req.handler(data -> {
                    proxiedRequest.write(data);
                });

                req.endHandler((v) -> proxiedRequest.end());
            } catch(Throwable t) {
                t.printStackTrace();
                req.response().setStatusCode(500).end();
            }
        });

        server.listen(8080, status -> {
           if(status.succeeded()) {
               startFuture.complete();
           } else {
               startFuture.fail(status.cause());
           }
        });

    }
}
