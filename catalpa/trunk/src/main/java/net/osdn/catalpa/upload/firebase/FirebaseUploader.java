package net.osdn.catalpa.upload.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.ui.javafx.MainApp;
import net.osdn.catalpa.ui.javafx.ToastMessage;

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
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/* Hosting REST API を使用してサイトにデプロイする
 * https://firebase.google.com/docs/hosting/api-deploy?hl=ja#raw-https-request_4
 *
 */
public class FirebaseUploader {

    private FirebaseConfig config;
    private ProgressObserver observer;
    private int progress;
    private int maxProgress;

    public FirebaseUploader(FirebaseConfig config) {
        this.config = config;
    }

    public int upload(File localDirectory, ProgressObserver observer) throws IOException, InterruptedException, NoSuchAlgorithmException, ExecutionException {
        this.observer = (observer != null) ? observer : ProgressObserver.EMPTY;
        this.observer.setProgress(0.0);
        this.observer.setText("アップロードの準備をしています…");

        int uploadCount = 0;

        String siteId = config.getSiteId();
        if(siteId == null) {
            throw new ToastMessage("Firebase Hosting", "siteId が指定されていません");
        }

        Path serviceAccountKeyFilePath = config.getServiceAccountKeyFilePath();
        if(serviceAccountKeyFilePath == null) {
            throw new ToastMessage("Firebase Hosting", "serviceAccountKey が指定されていません");
        }

        Path input = localDirectory.toPath();
        Path output = MainApp.createTemporaryDirectory("upload-htdocs-gzipped", true);

        String token = getAccessToken(serviceAccountKeyFilePath);

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        HttpClient client = HttpClient.newBuilder()
                .executor(Executors.newFixedThreadPool(6))
                .build();

        String versionId = createVersionId(client, token, siteId);

        Map<Path, String> files = new LinkedHashMap<>();
        int len;
        byte[] buf = new byte[65536];
        try(Stream<Path> stream = Files.walk(input)) {
            List<Path> list = stream.toList();
            for(Path path : list) {
                if(Files.isDirectory(path)) {
                    Path dirname = input.relativize(path);
                    Files.createDirectories(output.resolve(dirname));
                } else {
                    Path file = input.relativize(path);
                    try(InputStream in = Files.newInputStream(path);
                        OutputStream out = Files.newOutputStream(output.resolve(file));
                        GZIPOutputStream gzOut = new GZIPOutputStream(out) {{ def.setLevel(Deflater.BEST_SPEED); }}) {
                        while((len = in.read(buf)) > 0) {
                            gzOut.write(buf, 0, len);
                        }
                        gzOut.finish();
                    }
                    String hash = getSHA256(sha256, output.resolve(file));
                    files.put(file, hash);
                }
            }
        }

        PopulateFilesResult populateFilesResult = populateFiles(client, token, siteId, versionId, files);

        if(populateFilesResult.uploadRequiredHashes != null) {
            progress = 0;
            maxProgress = populateFilesResult.uploadRequiredHashes.size();

            Map<String, Path> map = new HashMap<>();
            for(Map.Entry<Path, String> entry : files.entrySet()) {
                map.put(entry.getValue(), entry.getKey());
            }
            uploadCount = populateFilesResult.uploadRequiredHashes.size();

            @SuppressWarnings({"rawtypes", "unchecked"})
            CompletableFuture<HttpResponse<String>>[] futures = new CompletableFuture[uploadCount];
            int i = 0;
            for(String uploadRequiredhash : populateFilesResult.uploadRequiredHashes) {
                Path file = map.get(uploadRequiredhash);
                String url = populateFilesResult.uploadUrl + "/" + uploadRequiredhash;

                CompletableFuture<HttpResponse<String>> future = upload(client, token, url, output.resolve(file));
                future.thenAccept(response -> {
                    if(response.statusCode() == 200) {
                        this.observer.setProgress(++progress / (double)maxProgress);
                        this.observer.setText("/" + file.toString().replace('\\', '/'));
                    }
                });
                futures[i++] = future;
            }
            CompletableFuture.allOf(futures).get();
        }

        finalizeVersion(client, token, siteId, versionId);
        releaseVersion(client, token, siteId, versionId);

        return uploadCount;
    }


    private static String getAccessToken(Path secretKeyJsonFile) throws IOException {
        try(InputStream in = Files.newInputStream(secretKeyJsonFile)) {
            GoogleCredentials credential = GoogleCredentials
                    .fromStream(in)
                    .createScoped("https://www.googleapis.com/auth/firebase.hosting");

            AccessToken token = credential.refreshAccessToken();
            return token.getTokenValue();
        }
    }


    private static String createVersionId(HttpClient client, String token, String siteId) throws IOException, InterruptedException {
        String versionId = null;

        String url = "https://firebasehosting.googleapis.com" + "/v1beta1/sites/" + siteId + "/versions";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {
                        "config": {
                            "headers": [{
                                "glob": "**",
                                "headers": {
                                    "Cache-Control": "max-age=1800"
                                }
                            }]
                        }
                    }
                    """, StandardCharsets.UTF_8))
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


    private static CompletableFuture<HttpResponse<String>> upload(HttpClient client, String token, String url, Path path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Bearer " + token)
                .setHeader("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofFile(path))
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


    private static String getSHA256(MessageDigest sha256, Path path) throws IOException, NoSuchAlgorithmException {
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
