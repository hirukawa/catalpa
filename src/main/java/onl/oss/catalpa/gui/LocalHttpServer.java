package onl.oss.catalpa.gui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalHttpServer {

    private static final int BACKLOG = 10;
    private static final int HTTP_SERVER_PORT = 4000;
    private static final int HTTP_SERVER_PORT_MAX = HTTP_SERVER_PORT + 5;

    private final ExecutorService executor ;
    private final HttpServer httpServer;
    private final Object update = new Object();

    public LocalHttpServer(Path rootDirectory) throws IOException {
        HttpServer hs;
        int port = HTTP_SERVER_PORT;
        for (;;) {
            try {
                hs = HttpServer.create(new InetSocketAddress("127.0.0.1", port), BACKLOG);
                break;
            } catch (BindException e) {
                if (++port > HTTP_SERVER_PORT_MAX) {
                    throw e;
                }
            }
        }
        httpServer = hs;

        httpServer.createContext("/", new FileHandler(rootDirectory));
        httpServer.createContext("/wait-for-update", this::waitForUpdate);

        executor = Executors.newCachedThreadPool();
        httpServer.setExecutor(executor);
    }

    public InetSocketAddress getAddress() {
        return httpServer.getAddress();
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        executor.shutdown();

        synchronized (update) {
            update.notifyAll();
        }

        httpServer.stop(0);
    }

    public void update() {
        synchronized (update) {
            update.notifyAll();
        }
    }

    private void waitForUpdate(HttpExchange exchange) throws IOException {
        synchronized (update) {
            try {
                update.wait();
            } catch (InterruptedException ignored) {}
        }
        byte[] response = "OK".getBytes();
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
