package net.osdn.catalpa.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.upload.netlify.NetlifyConfig;
import net.osdn.catalpa.upload.sftp.SftpConfig;
import net.osdn.catalpa.upload.smb.SmbConfig;

public class UploadConfigFactory {

	public UploadConfig create(Path inputPath, Path mydataPath) throws IOException {
		UploadConfig uploadConfig = null;

		String folderName = inputPath.getFileName().toString();

		List<Entry<Path, Map<String, Object>>> uploadConfigData = new ArrayList<Entry<Path, Map<String, Object>>>();

		Path configPath;
		configPath = inputPath.resolve(Catalpa.CONFIG_FILENAME);
		if(Files.isRegularFile(configPath)) {
			Map<String, Object> config = new Context(null, null).load(configPath);
			Object obj = config.get("upload");
			if(obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>)obj;
				@SuppressWarnings({"unchecked", "rawtypes"})
				Entry<Path, Map<String, Object>> entry = new AbstractMap.SimpleEntry(inputPath, map);
				uploadConfigData.add(entry);
			}
		}

		configPath = mydataPath.resolve(Catalpa.CONFIG_FILENAME);
		if(Files.isRegularFile(configPath)) {
			Map<String, Object> config = new Context(null, null).load(configPath);
			Object obj = config.get(folderName);
			if(obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> mapByFolder = (Map<String, Object>)obj;
				obj = mapByFolder.get("upload");
				if(obj instanceof Map<?, ?>) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>)obj;
					@SuppressWarnings({"unchecked", "rawtypes"})
					Entry<Path, Map<String, Object>> entry = new AbstractMap.SimpleEntry(mydataPath, map);
					uploadConfigData.add(entry);
				}
			}
			obj = config.get("upload");
			if(obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>)obj;
				@SuppressWarnings({"unchecked", "rawtypes"})
				Entry<Path, Map<String, Object>> entry = new AbstractMap.SimpleEntry(mydataPath, map);
				uploadConfigData.add(entry);
			}
		}

		String type = UploadConfig.getConfigValueAsString(uploadConfigData, "type");
		if(type != null) {
			if(type.equals("netlify")) {
				uploadConfig = new NetlifyConfig();
			} else if(type.equals("sftp")) {
				uploadConfig = new SftpConfig();
			} else if(type.equals("smb")) {
				uploadConfig = new SmbConfig();
			}
			if(uploadConfig != null) {
				uploadConfig.setUploadConfigData(uploadConfigData);
			}
		}

		return uploadConfig;
	}
}
