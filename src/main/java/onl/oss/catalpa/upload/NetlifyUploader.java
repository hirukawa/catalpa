package onl.oss.catalpa.upload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import onl.oss.catalpa.GeneratorException;
import onl.oss.catalpa.Main;
import onl.oss.catalpa.model.Progress;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static onl.oss.catalpa.Logger.INFO;
import static onl.oss.catalpa.Logger.WARN;

public class NetlifyUploader {

    private NetlifyConfig    config;
    private MessageDigest    sha1;
    private Map<String, String> deployPathBySHA = new HashMap<String, String>();

    private Consumer<Progress> consumer;
    private long progressValue;
    private long progressMax;

    public NetlifyUploader(NetlifyConfig config) {
        this.config = config;
    }

    public int upload(Path uploadPath, Consumer<Progress> consumer) throws IOException, InterruptedException, NoSuchAlgorithmException, ExecutionException {
        this.consumer = consumer != null ? consumer : progress -> {};
        this.consumer.accept(new Progress(0.0, "アップロードの準備をしています..."));

        int uploadCount = 0;

        String siteName = config.getSiteName();
        if (siteName == null) {
            throw new GeneratorException(config.getConfigFilePath(), "Netlify: siteName が設定されていません");
        }

        String token = config.getPersonalAccessToken();
        if (token == null) {
            throw new GeneratorException(config.getConfigFilePath(), "Netlify: personalAccessToken が設定されていません");
        }

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            HttpClient client = HttpClient.newBuilder()
                    .executor(executor)
                    .build();

            String siteId = getSiteId(client, token, siteName);
            if (siteId == null) {
                WARN("getConfigFilePath()=" + config.getConfigFilePath());
                throw new GeneratorException(config.getConfigFilePath(), "Netlify: サイト名が見つかりません: " + siteName);
            }

            Map<Path, String> files = new LinkedHashMap<>();
            try (Stream<Path> stream = Files.walk(uploadPath)) {
                List<Path> list = stream.toList();
                for (Path path : list) {
                    if (!Files.isDirectory(path)) {
                        Path file = uploadPath.relativize(path);
                        String hash = getSHA1(sha1, path);
                        files.put(file, hash);
                    }
                }
            }

            CreateSiteDeployResult createSiteDeployResult = createSiteDeploy(client, token, siteId, files);
            String deployId = createSiteDeployResult.id;
            List<String> required = createSiteDeployResult.required;

            if (required != null && !required.isEmpty()) {
                progressValue    = 0;
                progressMax = required.size();

                Map<String, Path> map = new HashMap<>();
                for(Map.Entry<Path, String> entry : files.entrySet()) {
                    map.put(entry.getValue(), entry.getKey());
                }
                uploadCount = required.size();

                @SuppressWarnings({"rawtypes", "unchecked"})
                CompletableFuture<HttpResponse<String>>[] futures = new CompletableFuture[uploadCount];
                int i = 0;
                for (String hash : required) {
                    Path file = map.get(hash);
                    String url = "/" + file.toString().replace('\\', '/');

                    CompletableFuture<HttpResponse<String>> future = uploadDeployFile(client, token, deployId, url, uploadPath.resolve(file));
                    future.thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            double value = ++progressValue / (double)progressMax;
                            this.consumer.accept(new Progress(value, "アップロードしています", file));
                        }
                    });
                    futures[i++] = future;
                }
                CompletableFuture.allOf(futures).get();
            }
        }
        return uploadCount;
    }


    private static String getSiteId(HttpClient client, String token,  String siteName) throws IOException, InterruptedException {
        if(siteName == null) {
            return null;
        }

        String url = "https://api.netlify.com" + "/api/v1/sites";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            throw new IOException(response + "\n" + response.body());
        }

        List<ListSitesResult> results = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(response.body(), new TypeReference<List<ListSitesResult>>(){});

        for(ListSitesResult result : results) {
            if(siteName.equals(result.name)) {
                return result.site_id;
            }
        }
        return null;
    }


    private static CreateSiteDeployResult createSiteDeploy(HttpClient client, String token, String siteId, Map<Path, String> files) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"files\": {\n");
        for(Map.Entry<Path, String> entry : files.entrySet()) {
            Path file = entry.getKey();
            String hash = entry.getValue();
            sb.append("        \"/" + file.toString().replace('\\', '/') + "\": ");
            sb.append("\"" + hash + "\"");
            sb.append(",\n");
        }
        if(files.size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
            sb.append("\n");
        }
        sb.append("    }\n");
        sb.append("}\n");

        String url = "https://api.netlify.com" + "/api/v1/sites/" + siteId + "/deploys"
                + "?title=" + URLEncoder.encode("Uploaded from " + Main.APPLICATION_NAME + " " + Main.APPLICATION_VERSION, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            throw new IOException(response + "\n" + response.body());
        }

        CreateSiteDeployResult result = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(response.body(), CreateSiteDeployResult.class);

        return result;
    }


    private static CompletableFuture<HttpResponse<String>> uploadDeployFile(HttpClient client, String token, String deployId, String url, Path file) throws IOException {
        String _url = "https://api.netlify.com" + "/api/v1/deploys/" + deployId + "/files" + URLEncoder.encode(url, StandardCharsets.UTF_8)
                + "?size=" + Files.size(file);
        HttpRequest request = HttpRequest.newBuilder(URI.create(_url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofFile(file))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    private static String getSHA1(MessageDigest sha1, Path path) throws IOException {
        sha1.reset();

        try(InputStream in = Files.newInputStream(path)) {
            byte[] buf = new byte[65536];
            int size;
            while((size = in.read(buf)) != -1) {
                sha1.update(buf, 0, size);
            }
        }
        return String.format("%040x", new BigInteger(1, sha1.digest()));
    }


    static class ListSitesResult {
        public String site_id;
        public String name;
    }


    static class CreateSiteDeployResult {
        public String id;
        public List<String> required;
    }
}
