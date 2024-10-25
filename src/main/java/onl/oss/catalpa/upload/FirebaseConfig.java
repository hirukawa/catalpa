package onl.oss.catalpa.upload;

import onl.oss.catalpa.model.Progress;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseConfig extends UploadConfig {

    private String siteId;
    private Path serviceAccountKeyFilePath;
    private Path firebaseConfigFilePath;

    public FirebaseConfig(Path configFilePath, List<Map.Entry<Path, Map<String, Object>>> data) {
        super(configFilePath, data);
    }

    public String getSiteId() {
        return this.siteId;
    }

    public Path getServiceAccountKeyFilePath() {
        return this.serviceAccountKeyFilePath;
    }

    public Path getFirebaseConfigFilePath() {
        return this.firebaseConfigFilePath;
    }

    private void initialize() {
        String s;

        s = getValueAsString("siteId");
        if(s == null) {
            throw new RuntimeException("siteId not found");
        }
        this.siteId = s.trim();

        s = getValueAsString("serviceAccountKey");
        if(s != null) {
            String serviceAccountKey = s.replace('/', '\\');
            if(serviceAccountKey.length() >= 3 && serviceAccountKey.substring(1, 3).equals(":\\")) {
                this.serviceAccountKeyFilePath = Paths.get(serviceAccountKey).toAbsolutePath();
            } else {
                Path p = getFolderPath("serviceAccountKey");
                this.serviceAccountKeyFilePath = p.resolve(serviceAccountKey).toAbsolutePath();
            }
            if(!Files.exists(this.serviceAccountKeyFilePath)) {
                throw new UncheckedIOException(new FileNotFoundException(this.serviceAccountKeyFilePath.toString()));
            }
        }

        s = getValueAsString("config");
        if(s != null) {
            String path = s.replace('/', '\\');
            if(path.length() >= 3 && path.substring(1, 3).equals(":\\")) {
                this.firebaseConfigFilePath = Paths.get(path).toAbsolutePath();
            } else {
                Path folder = getFolderPath("config");
                this.firebaseConfigFilePath = folder.resolve(path).toAbsolutePath();
            }
            if(!Files.exists(this.firebaseConfigFilePath)) {
                throw new UncheckedIOException((new FileNotFoundException(this.firebaseConfigFilePath.toString())));
            }
        }
    }

    public int upload(Path uploadPath, Consumer<Progress> consumer) throws Exception {
        initialize();

        FirebaseUploader uploader = new FirebaseUploader(this);
        int count = 0;
        count = uploader.upload(uploadPath, consumer);

        return count;
    }
}
