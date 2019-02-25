package net.osdn.catalpa.freemarker;

import java.util.List;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class BaseurlMethod implements TemplateMethodModelEx {
	
	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		Environment env = Environment.getCurrentEnvironment();
		if(env == null) {
			return null;
		}
		Template currentTemplate = env.getCurrentTemplate();
		if(currentTemplate == null) {
			return null;
		}
		String name = currentTemplate.getName();
		if(name == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		int fromIndex = 0;
		int i;
		while((i = name.indexOf('/', fromIndex)) != -1) {
			sb.append("../");
			fromIndex = i + 1;
		}
		return sb.toString();
	}

}
