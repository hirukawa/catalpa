package net.osdn.catalpa.upload.sftp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.upload.UploadConfig;

public class SftpConfig extends UploadConfig {
	
	private String host;
	private int    port = 22;
	private String path;
	private Path   privateKeyFilePath;
	private String passphrase;
	private String username;
	private String password;
	private UserInfo userInfo;

	public SftpConfig() {
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
	
	public Path getPrivateKeyFilePath() {
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

	private void initialize() {
		String s;
		Integer i;

		s = getValueAsString("host");
		if(s == null) {
			throw new RuntimeException("host not found");
		}
		this.host = s;

		i = getValueAsInteger("port");
		if(i != null) {
			this.port = i;
		}

		s = getValueAsString("path");
		if(s == null) {
			throw new RuntimeException("path not found");
		}
		this.path = s;
		if(!this.path.endsWith("/")) {
			this.path += "/";
		}

		s = getValueAsString("privateKey");
		if(s != null) {
			String privatekey = s.replace('/', '\\');
			if(privatekey.length() >= 3 && privatekey.substring(1, 3).equals(":\\")) {
				this.privateKeyFilePath = Paths.get(privatekey).toAbsolutePath();
			} else {
				Path p = getFolderPath("privateKey");
				this.privateKeyFilePath = p.resolve(privatekey).toAbsolutePath();
			}
			if(!Files.exists(this.privateKeyFilePath)) {
				throw new UncheckedIOException(new FileNotFoundException(this.privateKeyFilePath.toString()));
			}
		}

		s = getValueAsString("passphrase");
		if(s != null) {
			this.passphrase = s;
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
	public int upload(File dir, ProgressObserver observer) throws JSchException, SftpException, IOException {
		initialize();

		SftpUploader uploader = new SftpUploader(this);
		int count = 0;
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
