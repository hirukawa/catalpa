package net.osdn.catalpa.upload.sftp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.upload.UploadConfig;
import net.osdn.catalpa.upload.UploadType;

public class SftpConfig implements UploadConfig {
	
	private String host;
	private int port = 22;
	private String path;
	private String privateKeyFilePath;
	private String passphrase;
	private String username;
	private String password;
	private UserInfo userInfo;

	public SftpConfig(File dir, Map<?, ?> map, Path mydataPath) {
		Object object;
		
		object = map.get("host");
		if(object instanceof String) {
			this.host = (String)object;
		}
		
		object = map.get("port");
		if(object != null) {
			try {
				this.port = Integer.parseInt(object.toString());
			} catch(Exception e) {}
		}
		
		object = map.get("path");
		if(object instanceof String) {
			this.path = (String)object;
			if(!this.path.endsWith("/")) {
				this.path += "/";
			}
		}

		object = map.get("privatekey");
		if(object instanceof String) {
			String privatekey = ((String)object).replace('/', '\\');
			if(privatekey.length() >= 3 && privatekey.substring(1, 3).equals(":\\")) {
				this.privateKeyFilePath = privatekey;
			} else {
				this.privateKeyFilePath = dir.getAbsolutePath() + "\\" + privatekey;
				
				if(mydataPath != null) {
					if(!Files.exists(Paths.get(this.privateKeyFilePath))) {
						Path p = mydataPath.resolve(privatekey);
						if(Files.exists(p)) {
							this.privateKeyFilePath = p.toString();
						}
					}
				}
			}
		}
		
		object = map.get("passphrase");
		if(object instanceof String) {
			this.passphrase = (String)object;
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
	
	public UploadType getType() {
		return UploadType.Sftp;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public String getPrivateKeyFilePath() {
		return this.privateKeyFilePath;
	}
	
	public String getPassphrase() {
		return this.passphrase;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public UserInfo getUserInfo() {
		if(this.userInfo == null) {
			this.userInfo = new SftpUserInfo();
		}
		return this.userInfo;
	}

	@Override
	public int upload(File dir, ProgressObserver observer) throws JSchException, SftpException, IOException {
		System.out.println("upload: " + dir);
		
		int count = 0;
		SftpUploader uploader = new SftpUploader(this);
		try {
			uploader.connect();
			count = uploader.upload(dir, observer);
		} finally {
			uploader.disconnect();
		}
		
		return count;
	}
	
	@Override
	public String toString() {
		return "SftpConfig [host=" + host + ", port=" + port + ", path=" + path + ", privateKeyFilePath="
				+ privateKeyFilePath + ", passphrase=" + passphrase + ", username=" + username + ", password="
				+ password + ", userInfo=" + userInfo + "]";
	}

	private class SftpUserInfo implements UserInfo {
		@Override
		public String getPassphrase() {
			return passphrase;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public boolean promptPassphrase(String message) {
			System.out.println("promptPassphrase: " + message);
			return true;
		}

		@Override
		public boolean promptPassword(String message) {
			System.out.println("promptPassword: " + message);
			return true;
		}

		@Override
		public boolean promptYesNo(String message) {
			System.out.println("promptYesNo: " + message);
			return true;
		}

		@Override
		public void showMessage(String message) {
			System.out.println("showMessage: " + message);
		}
	}
}
