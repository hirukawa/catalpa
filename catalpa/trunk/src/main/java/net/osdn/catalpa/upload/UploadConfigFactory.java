package net.osdn.catalpa.upload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.upload.sftp.SftpConfig;
import net.osdn.catalpa.upload.smb.SmbConfig;

public class UploadConfigFactory {

	public UploadConfig create(Path inputPath, Path mydataPath) throws IOException {
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
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>)obj;
			
			for(Entry<?, ?> mydataEntry : getMyDataUploadConfig(mydataPath).entrySet()) {
				if(!map.containsKey(mydataEntry.getKey())) {
					map.put(mydataEntry.getKey(), mydataEntry.getValue());
				}
			}
			
			obj = map.get("type");
			if(obj instanceof String) {
				String type = ((String)obj).toLowerCase().trim();
				if(type.equals("sftp")) {
					File dir = configPath.toFile().getParentFile();
					uploadConfig = new SftpConfig(dir, map, mydataPath);
				} else if(type.equals("smb")) {
					File dir = configPath.toFile().getParentFile();
					uploadConfig = new SmbConfig(dir, map, mydataPath);
				}
			}
		}
		return uploadConfig;
	}
	
	protected Map<?, ?> getMyDataUploadConfig(Path mydataPath) throws IOException {
		if(Files.isDirectory(mydataPath)) {
			Path mydataConfigPath = mydataPath.resolve(Catalpa.CONFIG_FILENAME);
			if(Files.isRegularFile(mydataConfigPath)) {
				Map<String, Object> mydataConfig = new Context(null, null).load(mydataConfigPath);
				Object obj = mydataConfig.get("upload");
				if(obj instanceof Map<?, ?>) {
					return (Map<?, ?>)obj;
				}
			}
		}
		return new HashMap<Object, Object>();
	}
}
