package net.osdn.catalpa.upload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.upload.sftp.SftpConfig;
import net.osdn.catalpa.upload.smb.SmbConfig;

public class UploadConfigFactory {

	public UploadConfig create(Path inputPath) throws IOException {
		Path configPath = null;
		if(Files.isDirectory(inputPath)) {
			configPath = inputPath.resolve(Catalpa.CONFIG_FILENAME);
		} else if(inputPath.getFileName().toString().equalsIgnoreCase(Catalpa.CONFIG_FILENAME)) {
			configPath = inputPath;
		}
		if(configPath == null || !Files.exists(configPath)) {
			return null;
		}

		Map<String, Object> config = new Context(null, null).load(configPath);
		UploadConfig uploadConfig = null;
		Object obj = config.get("upload");
		if(obj instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>)obj;
			obj = map.get("type");
			if(obj instanceof String) {
				String type = ((String)obj).toLowerCase().trim();
				if(type.equals("sftp")) {
					File dir = configPath.toFile().getParentFile();
					uploadConfig = new SftpConfig(dir, map);
				} else if(type.equals("smb")) {
					File dir = configPath.toFile().getParentFile();
					uploadConfig = new SmbConfig(dir, map);
				}
			}
		}
		return uploadConfig;
	}
}
