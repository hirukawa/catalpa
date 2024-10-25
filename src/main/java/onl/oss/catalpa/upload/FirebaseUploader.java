package onl.oss.catalpa.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import onl.oss.catalpa.GeneratorException;
import onl.oss.catalpa.Util;
import onl.oss.catalpa.model.Progress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
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
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static onl.oss.catalpa.Logger.INFO;

/* Hosting REST API を使用してサイトにデプロイする
 * https://firebase.google.com/docs/hosting/api-deploy?hl=ja#raw-https-request_4
 *
 */
public class FirebaseUploader {

    private FirebaseConfig config;
    private Consumer<Progress> consumer;
    private long progressMax;
    private long progressValue;

    public FirebaseUploader(FirebaseConfig config) {
        this.config = config;
    }

    public int upload(Path uploadPath, Consumer<Progress> consumer) throws IOException, InterruptedException, NoSuchAlgorithmException, ExecutionException {
        this.consumer = consumer != null ? consumer : progress -> {};
        this.consumer.accept(new Progress(0.0, "アップロードの準備をしています..."));

        Path temporaryPath = Util.createTemporaryDirectory("upload-htdocs-gzipped", true);

        int uploadCount = 0;

        String siteId = config.getSiteId();
        if(siteId == null) {
            throw new GeneratorException(config.getConfigFilePath(), "Firebase Hosting: siteId が指定されていません");
        }

        Path serviceAccountKeyFilePath = config.getServiceAccountKeyFilePath();
        if(serviceAccountKeyFilePath == null) {
            throw new GeneratorException(config.getConfigFilePath(), "Firebase Hosting: serviceAccountKey が指定されていません");
        }

        String firebaseConfig;
        Path firebaseConfigFilePath = config.getFirebaseConfigFilePath();
        if(firebaseConfigFilePath != null && Files.exists(firebaseConfigFilePath)) {
            firebaseConfig = Files.readString(firebaseConfigFilePath);
        } else {
            firebaseConfig = """
            {
                "headers": [{
                    "glob": "**",
                    "headers": {
                        "Cache-Control": "max-age=1800"
                    }
                }]
            }
            """;
        }

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String token = getAccessToken(serviceAccountKeyFilePath);

        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            HttpClient client = HttpClient.newBuilder()
                    .executor(executor)
                    .build();

            String versionId = createVersionId(client, token, siteId, firebaseConfig);

            Map<Path, String> files = new LinkedHashMap<>();
            int len;
            byte[] buf = new byte[65536];
            try (Stream<Path> stream = Files.walk(uploadPath)) {
                List<Path> list = stream.toList();
                progressValue = 0;
                progressMax = list.size();
                for (Path path : list) {
                    if (Files.isDirectory(path)) {
                        Path dirname = uploadPath.relativize(path);
                        Files.createDirectories(temporaryPath.resolve(dirname));
                    } else {
                        Path file = uploadPath.relativize(path);
                        try (InputStream in = Files.newInputStream(path);
                            OutputStream out = Files.newOutputStream(temporaryPath.resolve(file));
                            GZIPOutputStream gzOut = new GZIPOutputStream(out) {{ def.setLevel(Deflater.BEST_SPEED); }}) {
                            while ((len = in.read(buf)) > 0) {
                                gzOut.write(buf, 0, len);
                            }
                            gzOut.finish();
                        }
                        String hash = getSHA256(sha256, temporaryPath.resolve(file));
                        files.put(file, hash);
                    }

                    double value = 0.4 * ++progressValue / progressMax;
                    this.consumer.accept(new Progress(value, "ファイルを圧縮しています", uploadPath.relativize(path)));
                }
            }

            PopulateFilesResult populateFilesResult = populateFiles(client, token, siteId, versionId, files);

            if (populateFilesResult.uploadRequiredHashes != null) {
                progressValue = 0;
                progressMax = populateFilesResult.uploadRequiredHashes.size();

                Map<String, Path> map = new HashMap<>();
                for (Map.Entry<Path, String> entry : files.entrySet()) {
                    map.put(entry.getValue(), entry.getKey());
                }
                uploadCount = populateFilesResult.uploadRequiredHashes.size();

                @SuppressWarnings({"rawtypes", "unchecked"})
                CompletableFuture<HttpResponse<String>>[] futures = new CompletableFuture[uploadCount];
                int i = 0;
                for (String hash : populateFilesResult.uploadRequiredHashes) {
                    Path file = map.get(hash);
                    String url = populateFilesResult.uploadUrl + "/" + hash;

                    CompletableFuture<HttpResponse<String>> future = upload(client, token, url, temporaryPath.resolve(file));
                    future.thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            double value = 0.4 + (0.6 * ++progressValue / progressMax);
                            this.consumer.accept(new Progress(value, "アップロードしています", file));
                        }
                    });
                    futures[i++] = future;
                }
                CompletableFuture.allOf(futures).get();
            }

            this.consumer.accept(new Progress(1.0, "最終処理を実行しています..."));
            finalizeVersion(client, token, siteId, versionId);
            releaseVersion(client, token, siteId, versionId);
        }
        return uploadCount;
    }


    private static String getAccessToken(Path secretKeyJsonFile) throws IOException {
        try(InputStream in = Files.newInputStream(secretKeyJsonFile)) {
            GoogleCredentials credential = GoogleCredentials
                    .fromStream(in)
                    .createScoped("https://www.googleapis.com/auth/firebase.hosting");

            AccessToken token = credential.refreshAccessToken();
            return token.getTokenValue().replaceFirst("\\.*$", "");
        }
    }


    private static String createVersionId(HttpClient client, String token, String siteId, String firebaseConfig) throws IOException, InterruptedException {
        String versionId = null;

        String url = "https://firebasehosting.googleapis.com" + "/v1beta1/sites/" + siteId + "/versions";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{ \"config\": " + firebaseConfig + " }", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            throw new IOException(response + "\n" + response.body());
        }

        CreateVersionResult result = new ObjectMapper().readValue(response.body(), CreateVersionResult.class);
        if(result.status.equals("CREATED")) {
            int i = result.name.indexOf("/versions/");
            versionId = result.name.substring(i + "/versions/".length());
        }
        return versionId;
    }


    private static PopulateFilesResult populateFiles(HttpClient client, String token, String siteId, String versionId, Map<Path, String> files) throws IOException, InterruptedException {
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

        String url = "https://firebasehosting.googleapis.com" + "/v1beta1/sites/" + siteId + "/versions/" + versionId + ":populateFiles";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            throw new IOException(response + "\n" + response.body());
        }

        PopulateFilesResult result = new ObjectMapper().readValue(response.body(), PopulateFilesResult.class);
        return result;
    }


    private static CompletableFuture<HttpResponse<String>> upload(HttpClient client, String token, String url, Path file) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofFile(file))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }


    private static void finalizeVersion(HttpClient client, String token, String siteId, String versionId) throws IOException, InterruptedException {
        String url = "https://firebasehosting.googleapis.com" + "/v1beta1/sites/" + siteId + "/versions/" + versionId + "?update_mask=status";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\": \"FINALIZED\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            throw new IOException(response + "\n" + response.body());
        }
    }


    private static void releaseVersion(HttpClient client, String token, String siteId, String versionId) throws IOException, InterruptedException {
        String url = "https://firebasehosting.googleapis.com" + "/v1beta1/sites/" + siteId + "/releases?versionName=sites/" + siteId + "/versions/" + versionId;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            throw new IOException(response + "\n" + response.body());
        }
    }


    private static String getSHA256(MessageDigest sha256, Path path) throws IOException {
        sha256.reset();

        try(InputStream in = Files.newInputStream(path)) {
            byte[] buf = new byte[65536];
            int len;
            while((len = in.read(buf)) != -1) {
                sha256.update(buf, 0, len);
            }
        }
        return String.format("%064x", new BigInteger(1, sha256.digest()));
    }


    static class CreateVersionResult {
        public String name;
        public String status;
        public Object config;
    }


    static class PopulateFilesResult {
        public List<String> uploadRequiredHashes;
        public String uploadUrl;
    }
}
