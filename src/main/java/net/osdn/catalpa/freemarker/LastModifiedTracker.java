package net.osdn.catalpa.freemarker;

import java.io.File;
import java.io.IOException;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;

public class LastModifiedTracker {
	
	private static long lastModified;
	
	public static void reset(Configuration configuration) {
		configuration.clearTemplateCache();
		lastModified = 0;
	}
	
	public static long getLastModified() {
		return lastModified;
	}
	
	public static class TemplateLoader extends FileTemplateLoader {

		public TemplateLoader(File baseDir) throws IOException {
			super(baseDir);
		}
		
		@Override
		public Object findTemplateSource(String name) throws IOException {
			Object source = super.findTemplateSource(name);
			if(source != null) {
				long modified = getLastModified(source);
				if(modified > lastModified) {
					lastModified = modified;
				}
			}
			return source;
		}
	}
}
