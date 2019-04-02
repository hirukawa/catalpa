package net.osdn.catalpa.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.esotericsoftware.yamlbeans.YamlException;

import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.Handler;
import net.osdn.catalpa.Util;
import net.osdn.catalpa.freemarker.LastModifiedTracker;
import net.osdn.util.io.AutoDetectReader;

public class TemplateHandler implements Handler {
	
	private static final Pattern BLOCK_LAST_MODIFIED = Pattern.compile("<!--catalpa.block.lastModified=([-0-9T:.]+Z)-->");

	public static final String[] APPLICABLE_EXTENSIONS = new String[] {
		".markdown.txt",
		".markdown",
		".md.txt",
		".md",
		".ftl"
	};

	private static final String OUTPUT_EXTENSION = ".html";
	
	@Override
	public int getPriority() {
		return 500;
	}

	@Override
	public boolean isApplicable(Path path) {
		String filename = path.getFileName().toString().toLowerCase();
		for(String ext : APPLICABLE_EXTENSIONS) {
			if(filename.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void handle(Context context, Reader reader, Writer writer) throws YamlException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Template template = null;
		FileTime lastModifiedTime = context.getConfigLastModifiedTime();
		if(lastModifiedTime != null && lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
			context.setLastModifiedTime(lastModifiedTime);
		}
		
		if(context.getInputPath().getFileName().toString().toLowerCase().endsWith(".ftl")) {
			context.setOutputPath(Util.replaceFileExtension(context.getOutputPath(), new String[] { ".ftl" }, null));
			template = new Template(null, AutoDetectReader.readAll(context.getInputPath()), context.getFreeMarker());
			lastModifiedTime = Files.getLastModifiedTime(context.getInputPath());
		} else {
			context.setOutputPath(Util.replaceFileExtension(context.getOutputPath(), APPLICABLE_EXTENSIONS, OUTPUT_EXTENSION));
			Entry<Template, FileTime> entry = getTemplate(context);
			if(entry != null) {
				template = entry.getKey();
				lastModifiedTime = entry.getValue();
			}
		}
		
		if(template != null) {
			if(lastModifiedTime != null) {
				if(context.getContentLastModifiedTime() == null || lastModifiedTime.compareTo(context.getContentLastModifiedTime()) > 0) {
					context.setContentLastModifiedTime(lastModifiedTime);
				}
				if(lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
					context.setLastModifiedTime(lastModifiedTime);
				}
			}
			if(template != null) {
				try {
					context.getDataModel().put("_INPUT_PATH", context.getInputPath());
					context.getDataModel().put("_OUTPUT_PATH", context.getOutputPath());

					synchronized (context.getFreeMarker()) {
						LastModifiedTracker.reset(context.getFreeMarker());
						
						StringWriter out = new StringWriter();
						template.process(context.getDataModel(), out);
						
						lastModifiedTime = FileTime.fromMillis(LastModifiedTracker.getLastModified());
						if(lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
							context.setLastModifiedTime(lastModifiedTime);
						}
						Matcher m = BLOCK_LAST_MODIFIED.matcher(out.toString());
						int start = 0;
						while(m.find(start)) {
							lastModifiedTime = FileTime.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(m.group(1))));
							if(lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
								context.setLastModifiedTime(lastModifiedTime);
							}
							start = m.end();
						}
						writer.write(m.replaceAll(""));
					}
				} finally {
					context.getDataModel().remove("_OUTPUT_PATH");
					context.getDataModel().remove("_INPUT_PATH");
				}
				return;
			}
		}

		// template not found
		context.setOutputPath(null);
	}
	
	protected Entry<Template, FileTime> getTemplate(Context context) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
		Map<String, Object> dataModel = context.getDataModel();
		String name = "default";
		Object obj = dataModel.get("template");
		if(obj instanceof String) {
			name = (String)obj;
		}
		if(!name.toLowerCase().endsWith(".ftl")) {
			name += ".ftl";
		}
		FileTime lastModifiedTime = null;
		Template template = context.getFreeMarker().getTemplate(name);
	
		if(template != null) {
			TemplateLoader tl = context.getFreeMarker().getTemplateLoader();
			Object templateSource = tl.findTemplateSource(name);
			if(templateSource != null) {
				long time = tl.getLastModified(templateSource);
				if(time > -1) { // -1 if the time is not known.
					lastModifiedTime = FileTime.fromMillis(time);
				}
			}
		}
		return new AbstractMap.SimpleEntry<Template, FileTime>(template, lastModifiedTime);
	}
}
