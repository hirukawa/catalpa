package net.osdn.catalpa.upload.firebase;

import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.upload.UploadConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirebaseConfig extends UploadConfig {

    private String siteId;
    private Path secretKeyFilePath;

    public FirebaseConfig() {
    }

    public String getSiteId() {
        return this.siteId;
    }

    public Path getSecretKeyFilePath() {
        return this.secretKeyFilePath;
    }

    private void initialize() {
        String s;

        s = getValueAsString("siteId");
        if(s == null) {
            throw new RuntimeException("siteId not found");
        }
        this.siteId = s.trim();

        s = getValueAsString("secretKey");
        if(s != null) {
            String secretKey = s.replace('/', '\\');
            if(secretKey.length() >= 3 && secretKey.substring(1, 3).equals(":\\")) {
                this.secretKeyFilePath = Paths.get(secretKey).toAbsolutePath();
            } else {
                Path p = getFolderPath("secretKey");
                this.secretKeyFilePath = p.resolve(secretKey).toAbsolutePath();
            }
            if(!Files.exists(this.secretKeyFilePath)) {
                throw new UncheckedIOException(new FileNotFoundException(this.secretKeyFilePath.toString()));
            }
        }
    }

    public int upload(File dir, ProgressObserver observer) throws Exception {
        initialize();

        FirebaseUploader uploader = new FirebaseUploader(this);
        int count = 0;
        count = uploader.upload(dir, observer);

        return count;
    }
}
