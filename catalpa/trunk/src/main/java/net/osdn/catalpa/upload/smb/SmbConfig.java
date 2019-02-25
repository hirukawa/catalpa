package net.osdn.catalpa.upload.smb;

import java.io.File;
import java.util.Map;

import net.osdn.catalpa.upload.UploadConfig;
import net.osdn.catalpa.upload.UploadType;

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

public class SmbConfig implements UploadConfig {
	
	private String path;
	private String domain;
	private String username;
	private String password;

	public SmbConfig(File dir, Map<?, ?> map) {
		Object object;
		
		object = map.get("path");
		if(object instanceof String) {
			this.path = (String)object;
			this.path = this.path.replace('\\', '/');
			
			if(!this.path.endsWith("/")) {
				this.path += "/";
			}
		}
		
		object = map.get("domain");
		if(object instanceof String) {
			this.domain = (String)object;
		}
		
		object = map.get("username");
		if(object instanceof String) {
			this.username = (String)object;
		}
		
		object = map.get("password");
		if(object instanceof String) {
			this.password = (String)object;
		}
	}
	
	@Override
	public UploadType getType() {
		return UploadType.Smb;
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

	@Override
	public int upload(File dir) throws Exception {
		int count = 0;
		SmbUploader uploader = new SmbUploader(this);
		count = uploader.upload(dir);
		
		return count;
	}

}
