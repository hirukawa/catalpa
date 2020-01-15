package net.osdn.catalpa.upload.smb;

import java.io.File;

import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.upload.UploadConfig;

/*
 * config.yml には以下のような接続設定を記述します。
 * 
 * upload:
 *   type: smb
 *   path: \\server\share\path
 *   domain:   mydomain
 *   username: myname
 *   password: mypass
 *   
 * ドメイン、ユーザー名、パスワードは省略してもかまいません。(それで接続できる環境であれば)
 * 
 */

public class SmbConfig extends UploadConfig {
	
	private String path;
	private String domain;
	private String username;
	private String password;

	public SmbConfig() {
	}

	public String getPath() {
		return this.path;
	}
	
	public String getDomain() {
		return this.domain;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}

	private void initialize() {
		String s;

		s = getValueAsString("path");
		if(s == null) {
			throw new RuntimeException("path not found");
		}
		this.path = s;
		if(!this.path.endsWith("/")) {
			this.path = "/";
		}

		s = getValueAsString("domain");
		if(s != null) {
			this.domain = s;
		}

		s = getValueAsString("username");
		if(s != null) {
			this.username = s;
		}

		s = getValueAsString("password");
		if(s != null) {
			this.password = s;
		}
	}

	@Override
	public int upload(File dir, ProgressObserver observer) throws Exception {
		initialize();

		int count = 0;
		SmbUploader uploader = new SmbUploader(this);
		count = uploader.upload(dir, observer);
		
		return count;
	}

}
