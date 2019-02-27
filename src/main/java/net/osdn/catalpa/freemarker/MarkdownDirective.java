package net.osdn.catalpa.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.options.MutableDataSet;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import net.osdn.blogs.flexmark.ext.highlight.HighlightExtension;
import net.osdn.blogs.flexmark.ext.kbd.KbdExtension;
import net.osdn.blogs.flexmark.ext.samp_button.SampButtonExtension;
import net.osdn.catalpa.flexmark.ext.LineDividableTableExtension;
import net.osdn.catalpa.flexmark.ext.ParagraphExtension;
import net.osdn.catalpa.flexmark.ext.RelativeLinkExtension;
import net.osdn.catalpa.html.JapaneseTextLayouter;

public class MarkdownDirective implements TemplateDirectiveModel {
	
	private static final String PARAM_RELATIVE_URL_PREFIX = "relative_url_prefix";
	
	private Parser parser;
	private HtmlRenderer renderer;
	
	public MarkdownDirective() {
		MutableDataSet options = new MutableDataSet();
		
		options.set(HtmlRenderer.FENCED_CODE_NO_LANGUAGE_CLASS, "nohighlight");
		
		options.set(HighlightExtension.REPLACE_YEN_SIGN, true);

		/*
		options.set(AnchorLinkExtension.ANCHORLINKS_SET_NAME, true);
		options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false);
		options.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "anchor");
		*/

		/*
		options.set(AttributesExtension.ASSIGN_TEXT_ATTRIBUTES, true);
		options.set(AttributesExtension.USE_EMPTY_IMPLICIT_AS_SPAN_DELIMITER, true);
		*/
		
		/*
		options.set(AttributesExtension.WRAP_NON_ATTRIBUTE_TEXT, false);
		*/

		options.set(Parser.EXTENSIONS, Arrays.asList(new Extension[] {
				AttributesExtension.create(),
				DefinitionExtension.create(),
				WikiLinkExtension.create(),
				StrikethroughExtension.create(),
				TaskListExtension.create(),
				TablesExtension.create(),
				TypographicExtension.create(),

				HighlightExtension.create(),
				KbdExtension.create(),
				SampButtonExtension.create(),
				
				LineDividableTableExtension.create(),
				ParagraphExtension.create(),
				RelativeLinkExtension.create()
		}));
		
		parser = Parser.builder(options).build();
		renderer = HtmlRenderer.builder(options).build();
	}

	@Override
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
		// Processing the parameters
		String relativeUrlPrefix = null;
		
		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, TemplateModel>> it = params.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, TemplateModel> param = it.next();
			if(param.getKey().equals(PARAM_RELATIVE_URL_PREFIX)) {
				if(!(param.getValue() instanceof TemplateScalarModel)) {
					throw new TemplateModelException("The \"" + PARAM_RELATIVE_URL_PREFIX + "\" parameter must be a string.");
				}
				relativeUrlPrefix = ((TemplateScalarModel)param.getValue()).getAsString();
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
		String japaneseTextLayouted = JapaneseTextLayouter.layout(output);
		env.getOut().write(japaneseTextLayouted);
	}
}
