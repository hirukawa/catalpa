package net.osdn.catalpa.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.options.MutableDataSet;

import freemarker.core.Environment;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import net.osdn.catalpa.flexmark.ext.RelativeLinkExtension;
import net.osdn.catalpa.html.JapaneseTextLayouter;

public class MarkdownDirective implements TemplateDirectiveModel {
	
	private static final String PARAM_RELATIVE_URL_PREFIX = "relative_url_prefix";
	private static final String PARAM_REPLACE_BACKSLASH_TO_YENSIGN = "replace_backslash_to_yensign";
	
	private Parser parser;
	private HtmlRenderer renderer;
	
	public MarkdownDirective(MutableDataSet options) {
		parser = Parser.builder(options).build();
		renderer = HtmlRenderer.builder(options).build();
	}

	@Override
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
		// Processing the parameters
		String relativeUrlPrefix = null;
		boolean isReplaceBackslashToYensign = false;
		
		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, TemplateModel>> it = params.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, TemplateModel> param = it.next();
			if(param.getKey().equals(PARAM_RELATIVE_URL_PREFIX)) {
				if(!(param.getValue() instanceof TemplateScalarModel)) {
					throw new TemplateModelException("The \"" + PARAM_RELATIVE_URL_PREFIX + "\" parameter must be a string.");
				}
				relativeUrlPrefix = ((TemplateScalarModel)param.getValue()).getAsString();
			} else if(param.getKey().equals(PARAM_REPLACE_BACKSLASH_TO_YENSIGN)) {
				if(!(param.getValue() instanceof TemplateBooleanModel)) {
					throw new TemplateModelException("The \"" + PARAM_REPLACE_BACKSLASH_TO_YENSIGN + "\" parameter must be a boolean.");
				}
				isReplaceBackslashToYensign = ((TemplateBooleanModel)param.getValue()).getAsBoolean();
			}
		}

		MutableDataSet options = null;
		if(relativeUrlPrefix != null) {
			if(options == null) {
				options = new MutableDataSet();
			}
			options.set(RelativeLinkExtension.RELATIVE_URL_PREFIX, relativeUrlPrefix);
		}
		
		StringWriter input = new StringWriter();
		body.render(input);
		Document document = parser.parse(input.toString());
		String output = renderer.withOptions(options).render(document);
		String japaneseTextLayouted = JapaneseTextLayouter.layout(output, isReplaceBackslashToYensign);
		env.getOut().write(japaneseTextLayouted);
	}
}
