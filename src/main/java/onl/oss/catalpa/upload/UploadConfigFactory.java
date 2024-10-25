package onl.oss.catalpa.upload;

import onl.oss.catalpa.model.Content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class UploadConfigFactory {

	public UploadConfig create(Path inputPath, Path mydataPath) throws IOException {
		UploadConfig uploadConfig = null;

		String folderName = inputPath.getFileName().toString();

		List<Entry<Path, Map<String, Object>>> data = new ArrayList<>();

		Path configFilePath = inputPath.resolve("config.yml");
		if (Files.isRegularFile(configFilePath)) {
			Content content = new Content(configFilePath);
			Map<String, Object> config = content.getYaml();
			Object obj = config.get("upload");
			if (obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>)obj;
				@SuppressWarnings({"unchecked", "rawtypes"})
				Entry<Path, Map<String, Object>> entry = new AbstractMap.SimpleEntry(inputPath, map);
				data.add(entry);
			}
		}

		Path globalConfigFilePath = mydataPath.resolve("config.yml");
		if (Files.isRegularFile(globalConfigFilePath)) {
			Content content = new Content(globalConfigFilePath);
			Map<String, Object> config = content.getYaml();
			Object obj = config.get(folderName);
			if (obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> mapByFolder = (Map<String, Object>)obj;
				obj = mapByFolder.get("upload");
				if (obj instanceof Map<?, ?>) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>)obj;
					@SuppressWarnings({"unchecked", "rawtypes"})
					Entry<Path, Map<String, Object>> entry = new AbstractMap.SimpleEntry(mydataPath, map);
					data.add(entry);
				}
			}
			obj = config.get("upload");
			if (obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>)obj;
				@SuppressWarnings({"unchecked", "rawtypes"})
				Entry<Path, Map<String, Object>> entry = new AbstractMap.SimpleEntry(mydataPath, map);
				data.add(entry);
			}
		}

		String type = UploadConfig.getConfigValueAsString(data, "type");
		if (type != null) {
            uploadConfig = switch (type) {
                case "firebase" -> new FirebaseConfig(configFilePath, data);
                case "netlify" -> new NetlifyConfig(configFilePath, data);
                case "sftp" -> new SftpConfig(configFilePath, data);
                case "smb" -> new SmbConfig(configFilePath, data);
                default -> null;
            };
		}

		return uploadConfig;
	}
}
