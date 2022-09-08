package net.osdn.catalpa.freemarker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;

import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataValueFactory;
import com.vladsch.flexmark.util.data.MutableDataSet;
import freemarker.core.Environment;
import freemarker.template.Template;
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
	private static final String PARAM_USE_RUBY = "use_ruby";
	private static final String PARAM_USE_CATALPA_FONT = "use_catalpa_font";

	private static final DataValueFactory<Object> NULL_VALUE_FACTORY = new DataValueFactory<Object>() {
		@Override
		public Object apply(DataHolder dataHolder) {
			return null;
		}
	};

	private MutableDataSet options;
	private Parser parser;
	private HtmlRenderer renderer;

	public MarkdownDirective(MutableDataSet options) {
		this.options = options;
		this.parser = Parser.builder(options).build();
	}

	@Override
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
		// Processing the parameters
		String relativeUrlPrefix = null;
		boolean isReplaceBackslashToYensign = false;
		boolean useRuby = false;
		boolean useCatalpaFont = false;
		
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
				if (!(param.getValue() instanceof TemplateBooleanModel)) {
					throw new TemplateModelException("The \"" + PARAM_REPLACE_BACKSLASH_TO_YENSIGN + "\" parameter must be a boolean.");
				}
				isReplaceBackslashToYensign = ((TemplateBooleanModel) param.getValue()).getAsBoolean();
			} else if(param.getKey().equals(PARAM_USE_RUBY)) {
				if(!((param.getValue()) instanceof TemplateBooleanModel)) {
					throw new TemplateModelException("The \"" + PARAM_USE_RUBY + "\" parameter must be a boolean.");
				}
				useRuby = ((TemplateBooleanModel)param.getValue()).getAsBoolean();
			} else if(param.getKey().equals(PARAM_USE_CATALPA_FONT)) {
				if(!((param.getValue()) instanceof TemplateBooleanModel)) {
					throw new TemplateModelException("The \"" + PARAM_USE_CATALPA_FONT + "\" parameter must be a boolean.");
				}
				useCatalpaFont = ((TemplateBooleanModel)param.getValue()).getAsBoolean();
			}
		}

		StringWriter sw = new StringWriter();
		body.render(sw);
		String input = sw.toString();

		// relativeUrlPrefix が指定されている場合、Freemarker の変数展開を適用します。
		// これはブログAddOnでページやカテゴリーで記事が表示されるときに該当します。
		if(relativeUrlPrefix != null) {
			StringWriter writer = new StringWriter();
			Template template = new Template("", input, env.getConfiguration());
			template.process(env.getDataModel(), writer);
			writer.toString();
			input = writer.toString();
		}

		// ブランク行（半角スペース・タブのみで構成されている行）を垂直スペース用の div に変換します。
		// 最初の半角スペースで構成される行は高さ 0 の垂直余白になります。（マージン相殺が無効になるのでこれでも高さが増えます。）
		// さらに半角スペースで構成される行が続くと半角スペース 1つごとに高さ 0.25em の垂直余白になります。
		try(BufferedReader reader = new BufferedReader(new StringReader(input))) {
			StringBuilder sb = new StringBuilder();
			String line;
			boolean isContinuousVerticalSpace = false;
			while((line = reader.readLine()) != null) {
				if(!line.isEmpty() && line.isBlank()) {
					int space = 0;
					for(int i = 0; i < line.length(); i++) {
						if(line.charAt(i) == '\u0020') { // 半角スペース
							space++;
						}
					}
					if(space > 0) {
						if(!isContinuousVerticalSpace) {
							sb.append("\n<div class=\"vspace\" data-length=\"0\" style=\"margin-block-start:-1px;height:1px\"></div>\n");
							isContinuousVerticalSpace = true;
						} else {
							String em = BigDecimal.valueOf(space).divide(BigDecimal.valueOf(4)).toPlainString() + "em";
							sb.append("\n<div class=\"vspace\" data-length=\"" + space + "\" style=\"height:" + em + "\"></div>\n");
						}
					}
				} else {
					sb.append(line);
					isContinuousVerticalSpace = false;
				}
				sb.append('\n');
			}
			input = sb.toString();
		}

		Document document = parser.parse(input);

		String previousRelativeUrlPrefix = null;
		if(options.contains(RelativeLinkExtension.RELATIVE_URL_PREFIX)) {
			previousRelativeUrlPrefix = (String)options.getOrCompute(RelativeLinkExtension.RELATIVE_URL_PREFIX, NULL_VALUE_FACTORY);
		}
		if(!Objects.equals(relativeUrlPrefix, previousRelativeUrlPrefix)) {
			if(options.contains(RelativeLinkExtension.RELATIVE_URL_PREFIX)) {
				options.remove(RelativeLinkExtension.RELATIVE_URL_PREFIX);
			}
			if(relativeUrlPrefix != null) {
				options.set(RelativeLinkExtension.RELATIVE_URL_PREFIX, relativeUrlPrefix);
			}
			renderer = null;
		}
		if(renderer == null) {
			renderer = HtmlRenderer.builder(options).build();
		}
		String output = renderer.render(document);
		String japaneseTextLayouted = JapaneseTextLayouter.layout(output, isReplaceBackslashToYensign, useRuby, useCatalpaFont);
		env.getOut().write(japaneseTextLayouted);
	}

}
