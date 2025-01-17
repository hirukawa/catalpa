package onl.oss.catalpa.gui;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.UnaryOperator;

public class FileHandler implements HttpHandler {

    private static final String NOT_FOUND_HTML =
            "<!DOCTYPE html>\r\n" +
            "<html lang=\"ja\">\r\n" +
            "<head>\r\n" +
            "   <style>\r\n" +
            "       h1 { font-size: 1.5rem; }\r\n" +
            "   </style>\r\n" +
            "</head>\r\n" +
            "<body>\r\n" +
            "<h1>Not Found</h1>\r\n" +
            "<p><a href=\"/\">トップページに戻る</a></p>\r\n" +
            "<script>\r\n" +
            "	function waitForUpdate() {\r\n" +
            "		var xhr = new XMLHttpRequest();\r\n" +
            "		xhr.onload = function (e) {\r\n" +
            "		    if (xhr.status === 205 && location.pathname !== \"/\" && location.pathname !== \"/index.html\") {\r\n" +
            "			    location.href = \"/\";\r\n" +
            "			} else if (xhr.status === 200 || xhr.status === 205) {\r\n" +
            "				location.reload();\r\n" +
            "			} else {\r\n" +
            "				waitForUpdate();\r\n" +
            "			}\r\n" +
            "		};\r\n" +
            "		xhr.onerror = function (e) {\r\n" +
            "		    waitForUpdate();\r\n" +
            "		};\r\n" +
            "		xhr.open(\"GET\", \"/wait-for-update?random=\" + Math.random(), true);\r\n" +
            "		xhr.send(null);\r\n" +
            "	}\r\n" +
            "	waitForUpdate();\r\n" +
            "</script>\r\n" +
            "</body>\r\n" +
            "</html>\r\n";

    private static final UnaryOperator<String> MIME_TABLE = URLConnection.getFileNameMap()::getContentTypeFor;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final Path root;

    public FileHandler(Path root) {
        this.root = root.normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        assert List.of("GET", "HEAD").contains(exchange.getRequestMethod());
        try (exchange) {
            discardRequestBody(exchange);
            Path path = mapToPath(exchange, root);
            if (path != null) {
                exchange.setAttribute("request-path", path.toString()); // store for OutputFilter
                if (!Files.exists(path) || !Files.isReadable(path) || Files.isHidden(path) || Files.isSymbolicLink(path)) {
                    handleNotFound(exchange);
                } else {
                    boolean writeBody = !exchange.getRequestMethod().equals("HEAD");
                    if (Files.isDirectory(path)) {
                        if (!exchange.getRequestURI().getPath().endsWith("/")) {
                            // missing slash
                            exchange.getResponseHeaders().set("Location", getRedirectURI(exchange.getRequestURI()));
                            exchange.sendResponseHeaders(301, -1);
                            return;
                        }

                        Path indexFile = indexFile(path);
                        if (indexFile != null) {
                            serveFile(exchange, indexFile, writeBody);
                        } else {
                            handleNotFound(exchange);
                        }
                    } else {
                        serveFile(exchange, path, writeBody);
                    }
                }
            } else {
                exchange.setAttribute("request-path", "could not resolve request URI path");
                handleNotFound(exchange);
            }
        }
    }

    private void serveFile(HttpExchange exchange, Path path, boolean writeBody) throws IOException {
        String mediaType = mediaType(path.toString());

        Headers headers = exchange.getResponseHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        if (mediaType.startsWith("text/")) {
            headers.set("Content-Type", mediaType + "; charset=UTF-8");
        } else {
            headers.set("Content-Type", mediaType);
        }
        headers.set("Last-Modified", getLastModified(path));

        if (writeBody) {
            exchange.sendResponseHeaders(200, Files.size(path));
            try (InputStream in = Files.newInputStream(path);
                 OutputStream out = exchange.getResponseBody()) {
                in.transferTo(out);
            }
        } else {
            headers.set("Content-Length", Long.toString(Files.size(path)));
            exchange.sendResponseHeaders(200, -1);
        }
    }

    private void handleNotFound(HttpExchange exchange) throws IOException {
        byte[] bytes = NOT_FOUND_HTML.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("Content-Type", "text/html; charset=UTF-8");

        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
            exchange.sendResponseHeaders(404, -1);
        } else {
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private Path mapToPath(HttpExchange exchange, Path root) {
        try {
            assert root.isAbsolute() && Files.isDirectory(root);
            String uriPath = relativeRequestPath(exchange);
            String[] pathSegment = uriPath.split("/");

            // resolve each path segment against the root
            Path path = root;
            for (var segment : pathSegment) {
                if (!isSupported(segment)) {
                    return null;  // stop resolution, null results in 404 response
                }
                path = path.resolve(segment);
                if (!Files.isReadable(path) || Files.isHidden(path) || Files.isSymbolicLink(path)) {
                    return null;  // stop resolution
                }
            }
            path = path.normalize();
            if (!path.startsWith(root)) {
                return null; // request not in root;
            }
            return path;
        } catch (Exception e) {
            return null; // could not resolve request URI path
        }
    }

    private static Path indexFile(Path path) {
        Path html = path.resolve("index.html");
        if (Files.exists(html)) {
            return html;
        }

        Path htm = path.resolve("index.htm");
        if (Files.exists(htm)) {
            return htm;
        }

        return null;
    }

    // Returns the request URI path relative to the context.
    private static String relativeRequestPath(HttpExchange exchange) {
        String context = contextPath(exchange);
        String request = requestPath(exchange);
        checkRequestWithinContext(request, context);
        return request.substring(context.length());
    }

    private static String contextPath(HttpExchange exchange) {
        String context = exchange.getHttpContext().getPath();
        if (!context.startsWith("/")) {
            throw new IllegalArgumentException("Context path invalid: " + context);
        }
        return context;
    }

    // Checks that the request does not escape context.
    private static void checkRequestWithinContext(String requestPath,
                                                  String contextPath) {
        if (requestPath.equals(contextPath)) {
            return;  // context path requested, e.g. context /foo, request /foo
        }
        String contextPathWithTrailingSlash = contextPath.endsWith("/")
                ? contextPath : contextPath + "/";
        if (!requestPath.startsWith(contextPathWithTrailingSlash)) {
            throw new IllegalArgumentException("Request not in context: " + contextPath);
        }
    }

    private static String requestPath(HttpExchange exchange) {
        String request = exchange.getRequestURI().getPath();
        if (!request.startsWith("/")) {
            throw new IllegalArgumentException("Request path invalid: " + request);
        }
        return request;
    }

    private static void discardRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            is.readAllBytes();
        }
    }

    private static String getRedirectURI(URI uri) {
        String query = uri.getRawQuery();
        String redirectPath = uri.getRawPath() + "/";
        return query == null ? redirectPath : redirectPath + "?" + query;
    }

    private static String getLastModified(Path path) throws IOException {
        FileTime fileTime = Files.getLastModifiedTime(path);
        return fileTime.toInstant().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private static String mediaType(String file) {
        String type = MIME_TABLE.apply(file);
        return type != null ? type : DEFAULT_CONTENT_TYPE;
    }

    private static boolean isSupported(String segment) {
        // apply same logic as WindowsPathParser
        if (segment.length() >= 2 && isLetter(segment.charAt(0)) && segment.charAt(1) == ':') {
            return false;
        }
        return true;
    }

    private static boolean isLetter(char c) {
        return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
    }
}
