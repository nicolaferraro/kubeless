package io.kubeless.examples.basic.server;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/**
 *
 */
public class App {

    public static void main(String[] args) throws Exception {

        int port = 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", exchange -> {
            String message = "Hello World";
            exchange.sendResponseHeaders(200, message.length());
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(message.getBytes());
            }
        });

        server.start();

        System.out.println("Server listening on port " + port);
    }

}
