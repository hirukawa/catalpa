package onl.oss.catalpa.gui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalHttpServer {

    private static final int BACKLOG = 10;
    private static final int HTTP_SERVER_PORT = 4000;
    private static final int HTTP_SERVER_PORT_MAX = HTTP_SERVER_PORT + 5;

    private final ExecutorService executor ;
    private final HttpServer httpServer;
    private final Object update = new Object();

    private final AtomicInteger waitCount = new AtomicInteger(0);
    private volatile long sequence;
    private volatile Path inputPath;

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

    public void update(Path inputPath) {
        synchronized (update) {
            this.sequence++;
            this.inputPath = inputPath;
            update.notifyAll();
        }
    }

    private void waitForUpdate(HttpExchange exchange) throws IOException {
        try {
            // /waitForUpdate の同時接続数が 4 を超えた場合、notifyAll を呼び出して待機スレッドを復帰させます。
            // sequence が変化していない状態で復帰したスレッドは
            int i = waitCount.incrementAndGet();
            while (--i >= 4) {
                synchronized (update) {
                    update.notifyAll();
                }
            }

            boolean isTooManyRequests = false;
            boolean isInputPathChanged = false;

            synchronized (update) {
                long sequence = this.sequence;
                Path inputPath = this.inputPath;

                try {
                    update.wait();
                } catch (InterruptedException ignored) {}

                // sequence が変化していない場合は更新による復帰ではなく、同時接続数超過による復帰として扱います。
                if (sequence == this.sequence) {
                    isTooManyRequests = true;
                }

                if (!Objects.equals(inputPath, this.inputPath)) {
                    isInputPathChanged = true;
                }
            }

            int rCode = 200;
            byte[] response = "OK".getBytes();

            if (isTooManyRequests) {
                rCode = 429;
                response = "Too Many Requests".getBytes();
            }

            // 待機している間に inputPath が変わっていたら（別のフォルダーを開いたということなので）
            // ルートにリダイレクトするために レスポンスコード 205 を返します。
            // ブラウザー側の JavaScript で 205 を処理するようにしています。
            // 307 + Location を返すと、ブラウザーが勝手にリダイレクトを実行してしまうため、205 を使用しています。
            if (isInputPathChanged) {
                rCode = 205;
                response = "Reset Content".getBytes();
            }

            exchange.sendResponseHeaders(rCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        } finally {
            waitCount.decrementAndGet();
        }
    }
}
