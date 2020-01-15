package net.osdn.catalpa.upload.netlify;

import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.ui.javafx.ToastMessage;
import net.osdn.catalpa.upload.UploadConfig;
import net.osdn.util.rest.client.HttpException;

import java.io.File;

public class NetlifyConfig extends UploadConfig  {

    private String siteName;
    private String personalAccessToken;

    public NetlifyConfig() {
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
            throw new ToastMessage("Netlify", "siteName が設定されていません。");
        }
        this.siteName = s;

        s = getValueAsString("personalAccessToken");
        if(s == null) {
            throw new ToastMessage("Netlify", "personalAccessToken が設定されていません。");
        }
        this.personalAccessToken = s;
    }

    @Override
    public int upload(File dir, ProgressObserver observer) throws Exception {
        initialize();

        NetlifyUploader uploader = new NetlifyUploader(this);
        int count = 0;
        try {
            count = uploader.upload(dir, observer);
        } catch(HttpException._401_UNAUTHORIZED e) {
            throw new ToastMessage("Netlify", "認証に失敗しました。\r\npersonalAccessToken が正しいか確認してください。");
        }

        return count;
    }
}
