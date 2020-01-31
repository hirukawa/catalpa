package net.osdn.catalpa.upload;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.osdn.catalpa.ProgressObserver;

public abstract class UploadConfig {

	private List<Map.Entry<Path, Map<String, Object>>> uploadConfigData;

	public abstract int upload(File dir, ProgressObserver observer) throws Exception;

	/* package private */ void setUploadConfigData(List<Map.Entry<Path, Map<String, Object>>> uploadConfigData) {
		this.uploadConfigData = uploadConfigData;
	}

	public Path getFolderPath(String name) {
		return getConfigFolderPath(uploadConfigData, name);
	}

	public String getValueAsString(String name) {
		return getConfigValueAsString(uploadConfigData, name);
	}

	public Integer getValueAsInteger(String name) {
		return getConfigValueAsInteger(uploadConfigData, name);
	}


	protected static Path getConfigFolderPath(List<Map.Entry<Path, Map<String, Object>>> uploadConfigData, String name) {
		for(Map.Entry<Path, Map<String, Object>> entry : uploadConfigData) {
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

	protected static Object getConfigValueAsObject(List<Map.Entry<Path, Map<String, Object>>> uploadConfigData, String name) {
		for(Map.Entry<Path, Map<String, Object>> entry : uploadConfigData) {
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

	protected static String getConfigValueAsString(List<Map.Entry<Path, Map<String, Object>>> uploadConfigData, String name) {
		Object value = getConfigValueAsObject(uploadConfigData, name);
		if(value != null) {
			if(value instanceof String) {
				return (String)value;
			}
			return value.toString();
		}
		return null;
	}

	protected static Integer getConfigValueAsInteger(List<Map.Entry<Path, Map<String, Object>>> uploadConfigData, String name) {
		Object value = getConfigValueAsObject(uploadConfigData, name);
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
