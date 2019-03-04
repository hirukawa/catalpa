package net.osdn.catalpa;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.yamlbeans.YamlReader;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.osdn.catalpa.handler.BlockHandler;
import net.osdn.catalpa.handler.YamlFrontMatterHandler;
import net.osdn.util.io.AutoDetectReader;

public class Context {
	
	private Configuration freeMarker;
	private Path rootInputPath;
	private Path rootOutputPath;
	private Path inputPath;
	private Path outputPath;
	private FileTime lastModifiedTime;
	
	private Map<String, Object> systemDataModel = new LinkedHashMap<String, Object>();
	private List<String> yamlFrontMatters = new ArrayList<String>();
	private Map<String, String> blocks = new LinkedHashMap<String, String>();
	private Map<String, Object> dataModel = null;
	private Map<Path, Map<String, Object>> configs = new HashMap<Path, Map<String, Object>>();

	public Context(Path rootInputPath, Path rootOutputPath) {
		this.rootInputPath = rootInputPath;
		this.rootOutputPath = rootOutputPath;
	}
	
	public Map<String, Object> getSystemDataModel() {
		return systemDataModel;
	}
	
	public Path getRootInputPath() {
		return rootInputPath;
	}
	
	public Path getRootOutputPath() {
		return rootOutputPath;
	}
	
	public void setInputPath(Path inputPath) {
		this.inputPath = inputPath;
	}
	
	public Path getInputPath() {
		return inputPath;
	}
	
	public Path getRelativeInputPath() {
		return (inputPath != null) ? rootInputPath.relativize(inputPath) : null;
	}
	
	public void setOutputPath(Path outputPath) {
		this.outputPath = outputPath;
	}
	
	public Path getOutputPath() {
		return outputPath;
	}
	
	public Path getRelativeOutputPath() {
		return (outputPath != null) ? rootOutputPath.relativize(outputPath) : null;
	}
	
	public void setLastModifiedTime(FileTime lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
		dataModel = null;
	}

	public FileTime getLastModifiedTime() {
		return lastModifiedTime;
	}
	
	public void addYamlFrontMatter(String yamlFrontMatter) {
		yamlFrontMatters.add(yamlFrontMatter);
		dataModel = null;
	}
	
	public void addBlock(String key, String value) {
		blocks.put(key, value);
		dataModel = null;
	}
	
	public void invalidateDataModel() {
		dataModel = null;
	}
	
	public Map<String, Object> getDataModel() throws IOException {
		if(dataModel == null) {
			Map<String, Object> dm = new HashMap<String, Object>();
			dm.put("baseurl", getBaseUrl());
			if(lastModifiedTime != null) {
				dm.put("dateModified", new Date(lastModifiedTime.toMillis()));
			}
			if(systemDataModel != null) {
				for(Entry<String, Object> entry : systemDataModel.entrySet()) {
					dm.put(entry.getKey(), entry.getValue());
				}
			}
			for(String yamlFrontMatter : yamlFrontMatters) {
				Object obj = new YamlReader(yamlFrontMatter).read();
				if(obj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>)obj;
					for(Entry<String, Object> entry : map.entrySet()) {
						dm.put(entry.getKey(), entry.getValue());
					}
				}
			}
			for(Entry<String, String> block : blocks.entrySet()) {
				Template template = new Template(null, block.getValue(), getFreeMarker());
				StringWriter out = new StringWriter();
				try {
					template.process(dm, out);
				} catch(TemplateException e) {
					throw new IOException(e);
				}
				dm.put(block.getKey(), out.toString());
			}
			dataModel = dm;
		}
		return dataModel;
	}
	
	public void setFreeMarker(Configuration freeMarker) {
		this.freeMarker = freeMarker;
	}
	
	public Configuration getFreeMarker() {
		return freeMarker;
	}
	
	public String getSiteUrl() throws IOException {
		String siteurl = null;
		Object obj = getDataModel().get("siteurl");
		if(obj instanceof String) {
			siteurl = ((String)obj).trim();
			if(siteurl.endsWith("/")) {
				siteurl = siteurl.substring(0, siteurl.length() - 1);
			}
		}
		return siteurl;
	}
	
	public String getBaseUrl() {
		Path relativeOutputPath = getRelativeOutputPath();
		if(relativeOutputPath == null || relativeOutputPath.getNameCount() <= 1) {
			return "";
		}
		return "../".repeat(relativeOutputPath.getNameCount() - 1);
	}
	
	public String getUrl() throws IOException {
		Path relativeOutputPath = getRelativeOutputPath();
		if(relativeOutputPath == null) {
			return null;
		}
		String siteurl = getSiteUrl();
		if(siteurl == null) {
			siteurl = "";
		}
		String url = siteurl + "/" + relativeOutputPath.toString().replace('\\', '/');
		return url;
	}
	
	public Map<String, Object> load(Path configPath) throws IOException {
		Map<String, Object> map = configs.get(configPath);
		if(map == null) {
			
			int yamlFrontMatterIndex = yamlFrontMatters.size();
			Reader reader = null;
			Writer writer;
			
			try {
				reader = new AutoDetectReader(configPath);
				writer = new StringWriter();
				new YamlFrontMatterHandler().handle(this, reader, writer);
			} finally {
				if(reader != null) {
					reader.close();
				}
			}
			
			reader = new StringReader(writer.toString());
			writer = new StringWriter();
			new BlockHandler().handle(this, reader, writer);
			
			map = new HashMap<String, Object>();
			if(yamlFrontMatters.size() > yamlFrontMatterIndex) {
				Object obj = new YamlReader(yamlFrontMatters.get(yamlFrontMatterIndex)).read();
				if(obj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> m = (Map<String, Object>)obj;
					for(Entry<String, Object> entry : m.entrySet()) {
						map.put(entry.getKey(), entry.getValue());
					}
				}
			}
			configs.put(configPath, map);
		}
		return map;
	}
	
	@Override
	public Context clone() {
		Context clone = new Context(this.rootInputPath, this.rootOutputPath);
		clone.setLastModifiedTime(this.lastModifiedTime);
		clone.setFreeMarker(this.freeMarker);
		clone.setInputPath(this.inputPath);
		clone.setOutputPath(this.outputPath);
		clone.systemDataModel = this.systemDataModel;
		for(String yamlFrontMatter : this.yamlFrontMatters) {
			clone.yamlFrontMatters.add(yamlFrontMatter);
		}
		for(Entry<String, String> block : this.blocks.entrySet()) {
			clone.blocks.put(block.getKey(), block.getValue());
		}
		for(Entry<Path, Map<String, Object>> config : this.configs.entrySet()) {
			clone.configs.put(config.getKey(), config.getValue());
		}
		return clone;
	}
}
