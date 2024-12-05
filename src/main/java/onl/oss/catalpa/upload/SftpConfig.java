package onl.oss.catalpa.upload;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import onl.oss.catalpa.model.Progress;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SftpConfig extends UploadConfig {
	
	private String host;
	private int    port = 22;
	private String path;
	private Path   privateKeyFilePath;
	private String passphrase;
	private String username;
	private String password;
	private UserInfo userInfo;

	public SftpConfig(Path configFilePath, List<Map.Entry<Path, Map<String, Object>>> data) {
		super(configFilePath, data);
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
		if (s == null) {
			throw new RuntimeException("host not found");
		}
		this.host = s;

		i = getValueAsInteger("port");
		if (i != null) {
			this.port = i;
		}

		s = getValueAsString("path");
		if (s == null) {
			throw new RuntimeException("path not found");
		}
		this.path = s;
		if (!this.path.endsWith("/")) {
			this.path += "/";
		}

		s = getValueAsString("privateKey");
		if (s != null) {
			String privatekey = s.replace('/', '\\');
			if (privatekey.length() >= 3 && privatekey.startsWith(":\\", 1)) {
				this.privateKeyFilePath = Paths.get(privatekey).toAbsolutePath();
			} else {
				Path p = getFolderPath("privateKey");
				this.privateKeyFilePath = p.resolve(privatekey).toAbsolutePath();
			}
			if (!Files.exists(this.privateKeyFilePath)) {
				throw new UncheckedIOException(new FileNotFoundException(this.privateKeyFilePath.toString()));
			}
		}

		s = getValueAsString("passphrase");
		if (s != null) {
			this.passphrase = s;
		}

		s = getValueAsString("username");
		if (s != null) {
			this.username = s;
		}

		s = getValueAsString("password");
		if (s != null) {
			this.password = s;
		}
	}

	@Override
	public int upload(Path uploadPath, Consumer<Progress> consumer) throws JSchException, SftpException, IOException {
		initialize();

		SftpUploader uploader = new SftpUploader(this);
		int count = 0;
		try {
			uploader.connect();
			count = uploader.upload(uploadPath, consumer);
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
			return true;
		}

		@Override
		public boolean promptPassword(String message) {
			return true;
		}

		@Override
		public boolean promptYesNo(String message) {
			return true;
		}

		@Override
		public void showMessage(String message) {
		}
	}
}
