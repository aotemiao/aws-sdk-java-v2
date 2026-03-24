package software.amazon.awssdk.learning.examples;

import java.util.Objects;

/**
 * Tiny SPI: high-level "client" depends only on {@link MiniHttpClient}.
 * Swap implementations without changing {@link MiniSdkClient} — same idea as http-client-spi.
 */
public final class HttpClientSpiDemo {

    private HttpClientSpiDemo() {
    }

    public static void run() {
        MiniSdkClient a = new MiniSdkClient(new LoggingHttpClient(new EchoHttpClient()));
        System.out.println(a.send("GET", "/hello"));

        MiniSdkClient b = new MiniSdkClient(new FixedResponseHttpClient(404, "not-found"));
        System.out.println(b.send("GET", "/missing"));
    }

    /** SPI: transport boundary. */
    public interface MiniHttpClient {
        MiniHttpResponse execute(MiniHttpRequest request);
    }

    public static final class MiniHttpRequest {
        private final String method;
        private final String path;

        public MiniHttpRequest(String method, String path) {
            this.method = Objects.requireNonNull(method, "method");
            this.path = Objects.requireNonNull(path, "path");
        }

        public String method() {
            return method;
        }

        public String path() {
            return path;
        }
    }

    public static final class MiniHttpResponse {
        private final int status;
        private final String body;

        public MiniHttpResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }

        @Override
        public String toString() {
            return status + " " + body;
        }
    }

    /** High-level client: policy / orchestration only. */
    public static final class MiniSdkClient {
        private final MiniHttpClient http;

        public MiniSdkClient(MiniHttpClient http) {
            this.http = Objects.requireNonNull(http, "http");
        }

        public String send(String method, String path) {
            return http.execute(new MiniHttpRequest(method, path)).toString();
        }
    }

    public static final class EchoHttpClient implements MiniHttpClient {
        @Override
        public MiniHttpResponse execute(MiniHttpRequest request) {
            return new MiniHttpResponse(200, "echo:" + request.method() + ":" + request.path());
        }
    }

    public static final class FixedResponseHttpClient implements MiniHttpClient {
        private final int status;
        private final String body;

        public FixedResponseHttpClient(int status, String body) {
            this.status = status;
            this.body = body;
        }

        @Override
        public MiniHttpResponse execute(MiniHttpRequest request) {
            return new MiniHttpResponse(status, body);
        }
    }

    public static final class LoggingHttpClient implements MiniHttpClient {
        private final MiniHttpClient delegate;

        public LoggingHttpClient(MiniHttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public MiniHttpResponse execute(MiniHttpRequest request) {
            System.out.println("[http] -> " + request.method() + " " + request.path());
            MiniHttpResponse r = delegate.execute(request);
            System.out.println("[http] <- " + r.status());
            return r;
        }
    }
}
