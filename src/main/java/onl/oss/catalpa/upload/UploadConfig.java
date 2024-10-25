package onl.oss.catalpa.upload;

import onl.oss.catalpa.model.Progress;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class UploadConfig {

	private final Path configFilePath;
	private final List<Map.Entry<Path, Map<String, Object>>> data;

	public abstract int upload(Path uploadPath, Consumer<Progress> consumer) throws Exception;

	public UploadConfig(Path configFilePath, List<Map.Entry<Path, Map<String, Object>>> data) {
		this.configFilePath = configFilePath;
		this.data = data;
	}

	public Path getConfigFilePath() {
		return configFilePath;
	}

	public Path getFolderPath(String name) {
		return getConfigFolderPath(data, name);
	}

	public String getValueAsString(String name) {
		return getConfigValueAsString(data, name);
	}

	public Integer getValueAsInteger(String name) {
		return getConfigValueAsInteger(data, name);
	}


	protected static Path getConfigFolderPath(List<Map.Entry<Path, Map<String, Object>>> data, String name) {
		for(Map.Entry<Path, Map<String, Object>> entry : data) {
			Map<String, Object> map = entry.getValue();
			Object obj = map.get(name);
			if(obj != null) {
				return entry.getKey();
			}
			// 大文字小文字が異なるキーも対象として検索します。
			if(name != null) {
				for(String key : map.keySet()) {
					if(name.equalsIgnoreCase(key)) {
						obj = map.get(key);
						if(obj != null) {
							return entry.getKey();
						}
					}
				}
			}
		}
		return null;
	}

	protected static Object getConfigValueAsObject(List<Map.Entry<Path, Map<String, Object>>> data, String name) {
		for(Map.Entry<Path, Map<String, Object>> entry : data) {
			Map<String, Object> map = entry.getValue();
			Object obj = map.get(name);
			if(obj != null) {
				return obj;
			}
			// 大文字小文字が異なるキーも対象として検索します。
			if(name != null) {
				for(String key : map.keySet()) {
					if(name.equalsIgnoreCase(key)) {
						obj = map.get(key);
						if(obj != null) {
							return obj;
						}
					}
				}
			}
		}
		return null;
	}

	protected static String getConfigValueAsString(List<Map.Entry<Path, Map<String, Object>>> data, String name) {
		Object value = getConfigValueAsObject(data, name);
		if(value != null) {
			if(value instanceof String) {
				return (String)value;
			}
			return value.toString();
		}
		return null;
	}

	protected static Integer getConfigValueAsInteger(List<Map.Entry<Path, Map<String, Object>>> data, String name) {
		Object value = getConfigValueAsObject(data, name);
		if(value instanceof Number) {
			return ((Number)value).intValue();
		}
		if(value != null) {
			try {
				return Integer.parseInt(value.toString());
			} catch(Exception ignore) {}
		}
		return null;
	}
}
