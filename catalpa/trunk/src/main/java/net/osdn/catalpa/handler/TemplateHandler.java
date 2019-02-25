package net.osdn.catalpa.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.yamlbeans.YamlException;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.Handler;
import net.osdn.catalpa.Util;

public class TemplateHandler implements Handler {

	public static final String[] APPLICABLE_EXTENSIONS = new String[] {
		".markdown.txt",
		".markdown",
		".md.txt",
		".md",
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
		context.setOutputPath(Util.replaceFileExtension(context.getOutputPath(), APPLICABLE_EXTENSIONS, OUTPUT_EXTENSION));
		
		Entry<Template, FileTime> entry = getTemplate(context);
		if(entry != null) {
			Template template = entry.getKey();
			FileTime lastModifiedTime = entry.getValue();
			if(lastModifiedTime != null && lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
				context.setLastModifiedTime(lastModifiedTime);
			}
			if(template != null) {
				try {
					context.getDataModel().put("_INPUT_PATH", context.getInputPath());
					context.getDataModel().put("_OUTPUT_PATH", context.getOutputPath());
					template.process(context.getDataModel(), writer);
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
			if(tl instanceof FileTemplateLoader) {
				FileTemplateLoader ftl = (FileTemplateLoader)tl;
				Object templateSource = ftl.findTemplateSource(name);
				if(templateSource != null) {
					lastModifiedTime = FileTime.fromMillis(ftl.getLastModified(templateSource));
				}
			}
		}
		return new AbstractMap.SimpleEntry<Template, FileTime>(template, lastModifiedTime);
	}
	
	public static void main(String[] args) {
		
		Path relativeInputPath = Paths.get("dirA/index.html");
		Path relativeOutputPath = Paths.get("dirA/sub/index.html");
		
		Path inDir = relativeInputPath.getParent();
		if(inDir == null) {
			inDir = Paths.get("");
		}
		Path outDir = relativeOutputPath.getParent();
		if(outDir == null) {
			outDir = Paths.get("");
		}
		
		while(inDir.toString().length() > 0 && outDir.toString().length() > 0 && inDir.getName(0).equals(outDir.getName(0))) {
			System.out.println(">>" + outDir.getName(0));
			inDir = inDir.relativize(inDir.getName(0));
			outDir = outDir.relativize(outDir.getName(0));
			System.out.println("!inDir=" + inDir);
			System.out.println("!outDir=" + outDir);
		}
		System.out.println("#inDir=" + inDir);
		System.out.println("#outDir=" + outDir);
		
		
		
	}
}
