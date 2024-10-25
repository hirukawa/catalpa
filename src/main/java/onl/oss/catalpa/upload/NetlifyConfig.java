package onl.oss.catalpa.upload;

import onl.oss.catalpa.model.Progress;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NetlifyConfig extends UploadConfig  {

    private String siteName;
    private String personalAccessToken;

    public NetlifyConfig(Path configFilePath, List<Map.Entry<Path, Map<String, Object>>> data) {
        super(configFilePath, data);
    }

    public String getSiteName() {
        return this.siteName;
    }

    public String getPersonalAccessToken() {
        return this.personalAccessToken;
    }

    private void initialize() {
        String s;

        s = getValueAsString("siteName");
        if(s == null) {
            throw new RuntimeException("Netlify: siteName が設定されていません");
        }
        this.siteName = s;

        s = getValueAsString("personalAccessToken");
        if(s == null) {
            throw new RuntimeException("Netlify: personalAccessToken が設定されていません");
        }
        this.personalAccessToken = s;
    }

    @Override
    public int upload(Path uploadPath, Consumer<Progress> consumer) throws Exception {
        initialize();

        NetlifyUploader uploader = new NetlifyUploader(this);
        int count = 0;
        count = uploader.upload(uploadPath, consumer);

        return count;
    }
}
