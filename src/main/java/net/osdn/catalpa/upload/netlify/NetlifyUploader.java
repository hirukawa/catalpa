package net.osdn.catalpa.upload.netlify;

import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.URLEncoder;
import net.osdn.catalpa.ui.javafx.Main;
import net.osdn.catalpa.ui.javafx.ToastMessage;
import net.osdn.util.rest.client.HttpException;
import net.osdn.util.rest.client.RestClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetlifyUploader {

    private NetlifyConfig    config;
    private RestClient.Instance netlify;
    private MessageDigest    sha1;
    private Map<String, String> deployPathBySHA = new HashMap<String, String>();

    private ProgressObserver observer;
    private int progress;
    private int maxProgress;

    public NetlifyUploader(NetlifyConfig config) {
        this.config = config;
    }

    public int upload(File localDirectory, ProgressObserver observer) throws IOException, HttpException, NoSuchAlgorithmException {
        this.observer = (observer != null) ? observer : ProgressObserver.EMPTY;
        this.observer.setProgress(0.0);
        this.observer.setText("アップロードの準備をしています…");

        int uploadCount = 0;

        netlify = RestClient.newInstance();
        netlify.setUrl("https://api.netlify.com/api/v1");
        netlify.setAuthorization("Bearer " + config.getPersonalAccessToken());

        String siteId = getSiteId(config.getSiteName());
        if(siteId == null) {
            throw new ToastMessage("Netlify", "サイト名が見つかりません: " + config.getSiteName());
        }

        CreateSiteDeployResult createSiteDeployResult = createSiteDeploy(siteId, localDirectory);
        String deployId = createSiteDeployResult.id;
        List<String> required = createSiteDeployResult.required;

        progress = 0;
        maxProgress = required.size() + 1;

        for(String sha : required) {
            String relativePath = deployPathBySHA.get(sha);
            Path localFilePath = localDirectory.toPath().resolve(relativePath.substring(1));

            this.observer.setProgress(++progress / (double)maxProgress);
            this.observer.setText(relativePath.substring(1));

            uploadDeployFile(deployId, relativePath, localFilePath);
            uploadCount++;
        }

        return uploadCount;
    }

    public String getSiteId(String siteName) throws IOException, HttpException {
        if(siteName == null) {
            return null;
        }
        List<ListSitesResult> results = netlify.path("/sites").getList(ListSitesResult.class);
        for(ListSitesResult result : results) {
            if(siteName.equals(result.name)) {
                return result.site_id;
            }
        }
        return null;
    }

    public CreateSiteDeployResult createSiteDeploy(String siteId, File localDirectory) throws IOException, NoSuchAlgorithmException, HttpException {
        String json = createDeployFilesJson(localDirectory);

        CreateSiteDeployResult result = netlify.reset().path("/sites").path(siteId).path("/deploys")
                .rawParam("title", "Uploaded from " + Main.APPLICATION_NAME + " " + Main.APPLICATION_VERSION)
                .post("application/json", json.getBytes(StandardCharsets.UTF_8))
                .get(CreateSiteDeployResult.class);

        return result;
    }

    private String createDeployFilesJson(File localDirectory) throws IOException, NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\r\n");
        sb.append("\t\"files\": {\r\n");

        int localDirectoryLength = localDirectory.getAbsolutePath().length();
        List<File> files = listLocalFiles(localDirectory);
        for(int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String name = file.getAbsolutePath().substring(localDirectoryLength).replace('\\', '/');
            String sha = getSHA1(file);
            deployPathBySHA.put(sha, name);
            sb.append("\t\t\"");
            sb.append(URLEncoder.encode(name));
            sb.append("\": \"");
            sb.append(sha);
            sb.append("\"");
            if(i + 1 < files.size()) {
                sb.append(",");
            }
            sb.append("\r\n");
        }
        sb.append("\t}\r\n");
        sb.append("}\r\n");
        return sb.toString();
    }

    private String uploadDeployFile(String deployId, String relativePath, Path localFilePath) throws IOException, HttpException {
        String result = netlify.path("/deploys").path(deployId).path("/files")
                .path(URLEncoder.encode(relativePath)).param("size", Files.size(localFilePath))
                .put(RequestBody.create(MediaType.parse("application/octet-stream"), localFilePath.toFile()))
                .get();

        return result;
    }

    private static List<File> listLocalFiles(File dir) {
        List<File> list = new ArrayList<File>();

        for(File child : dir.listFiles()) {
            if(child.isDirectory()) {
                list.addAll(listLocalFiles(child));
            } else {
                list.add(child);
            }
        }
        return list;
    }


    private String getSHA1(File file) throws NoSuchAlgorithmException, IOException {
        if(sha1 == null) {
            sha1 = MessageDigest.getInstance("SHA-1");
        }
        sha1.reset();

        try(FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
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
