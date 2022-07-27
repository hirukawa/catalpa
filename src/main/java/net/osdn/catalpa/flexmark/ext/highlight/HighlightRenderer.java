package net.osdn.catalpa.flexmark.ext.highlight;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.codewaves.codehighlight.core.Highlighter;
import com.codewaves.codehighlight.core.Highlighter.HighlightResult;
import com.codewaves.codehighlight.core.StyleRenderer;
import com.codewaves.codehighlight.core.StyleRendererFactory;
import com.codewaves.codehighlight.renderer.HtmlRenderer;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.sequence.BasedSequence;

public class HighlightRenderer implements NodeRenderer {
	private static final AttributablePart CODE_CONTENT = new AttributablePart("FENCED_CODE_CONTENT");
	private static final String ADDITIONAL_CHARS = "";
	private static final String ATTRIBUTENAME = "[a-zA-Z" + ADDITIONAL_CHARS + "_:][a-zA-Z0-9" + ADDITIONAL_CHARS + ":._-]*";
	private static final String UNQUOTEDVALUE = "[^\"'=<>{}`\u0000-\u0020]+";
	private static final String SINGLEQUOTEDVALUE = "'[^']*'";
	private static final String DOUBLEQUOTEDVALUE = "\"[^\"]*\"";
	private static final String ATTRIBUTEVALUE = "(?:" + UNQUOTEDVALUE + "|" + SINGLEQUOTEDVALUE + "|" + DOUBLEQUOTEDVALUE + ")";
    private static final Pattern ATTRIBUTES_TAG = Pattern.compile(
            "\\{((?:[#.])|(?:" + "\\s*([#.]" + UNQUOTEDVALUE + "|" + ATTRIBUTENAME + ")\\s*(?:=\\s*(" + ATTRIBUTEVALUE + ")?" + ")?" + ")" +
                    "(?:" + "\\s+([#.]" + UNQUOTEDVALUE + "|" + ATTRIBUTENAME + ")\\s*(?:=\\s*(" + ATTRIBUTEVALUE + ")?" + ")?" + ")*" + "\\s*)\\}$");
    private static final Pattern ATTRIBUTE = Pattern.compile("\\s*([#.]" + UNQUOTEDVALUE + "|" + ATTRIBUTENAME + ")\\s*(?:=\\s*(" + ATTRIBUTEVALUE + ")?" + ")?");
    
    private static final String COPY_SCRIPT =
    		"document.getSelection().selectAllChildren(event.target.parentNode.nextSibling);" +
    		"document.execCommand('copy');" +
    		"document.getSelection().removeAllRanges();" + 
    		"return false;";

    private final boolean codeContentBlock;
    private final boolean isReplaceYenSign;
    
    private final Highlighter highlighter = new Highlighter(new StyleRendererFactory() {
		@Override
		public StyleRenderer create(String languageName) {
			return new HtmlRenderer("hljs-");
		}
	});
	
	public HighlightRenderer(DataHolder options) {
        codeContentBlock = Parser.FENCED_CODE_CONTENT_BLOCK.get(options);
        isReplaceYenSign = HighlightExtension.REPLACE_YEN_SIGN.get(options);
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
		set.add(new NodeRenderingHandler<FencedCodeBlock>(FencedCodeBlock.class, new NodeRenderingHandler.CustomNodeRenderer<FencedCodeBlock>() {
			@Override
			public void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
				HighlightRenderer.this.render(node, context, html);
			}
		}));
		return set;
	}
	
	protected void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
		String languageName = null;
        BasedSequence language = BasedSequence.NULL;
        BasedSequence title = BasedSequence.EMPTY;
        boolean isCopyable = false;
		
        html.line();

		BasedSequence attributes = node.getAttributes();
		BasedSequence info = node.getInfo();
		if(info.isNotNull() && !info.isBlank()) {
			Matcher matcher = ATTRIBUTES_TAG.matcher(info);
			if(matcher.find()) {
				attributes = info.subSequence(matcher.start()).trim();
				info = info.subSequence(0, matcher.start()).trim();
			}
		}
        if(attributes.isNotNull() && !attributes.isBlank()) {
			Matcher matcher = ATTRIBUTES_TAG.matcher(attributes);
			if (matcher.find()) {
				BasedSequence attributesText = attributes.subSequence(matcher.start(1), matcher.end(1)).trim();
				if (!attributesText.isEmpty()) {
					Matcher attributeMatcher = ATTRIBUTE.matcher(attributesText);
					while (attributeMatcher.find()) {
						BasedSequence attributeName = attributesText.subSequence(attributeMatcher.start(1), attributeMatcher.end(1));
						if (attributeName.isNotNull() && attributeName.length() > 0) {
							if (attributeName.charAt(0) == '.') {
								BasedSequence cls = attributeName.subSequence(1);
								html.attr("class", cls);
								if (cls.startsWith("cop")) {
									isCopyable = true;
								}
							} else if (attributeName.charAt(0) == '#') {
								html.attr("id", attributeName.subSequence(1));
							} else {
								BasedSequence attributeValue = attributeMatcher.groupCount() == 1 || attributeMatcher.start(2) == -1 ? BasedSequence.NULL : attributesText.subSequence(attributeMatcher.start(2), attributeMatcher.end(2));
								boolean isQuoted = attributeValue.length() >= 2 && (attributeValue.charAt(0) == '"' && attributeValue.endCharAt(1) == '"' || attributeValue.charAt(0) == '\'' && attributeValue.endCharAt(1) == '\'');
								if (isQuoted) {
									attributeValue = attributeValue.midSequence(1, -1);
								}
								if (attributeValue.isNotNull()) {
									html.attr(attributeName, attributeValue);
								}
							}
						}
					}
				}
			}
		}

        if (info.isNotNull() && !info.isBlank()) {
            int space = info.indexOfAny(CharPredicate.SPACE);
            if (space == -1) {
                language = info;
            } else {
                language = info.subSequence(0, space);
                title = info.subSequence(space).trim();
            }
            languageName = language.unescape();
        }
        html.srcPosWithTrailingEOL(node.getChars()).withAttr().tag("pre").openPre();

        if(!title.isEmpty() || isCopyable) {
        	html.attr("class", "title").withAttr().tag("div");
       		html.append(title.isEmpty() ? " " : title);
        	if(isCopyable) {
        		html.attr("class", "copy-button").attr("onclick", COPY_SCRIPT).withAttr().tag("span").tag("/span");
        	}
        	html.tag("/div");
        }

        if(language.isNotNull()) {
            html.attr("class", context.getHtmlOptions().languageClassPrefix + language.unescape());
        } else {
            String noLanguageClass = context.getHtmlOptions().noLanguageClass.trim();
            if (!noLanguageClass.isEmpty()) {
                html.attr("class", noLanguageClass);
            }
        }
        html.srcPosWithEOL(node.getContentChars()).withAttr(CODE_CONTENT).tag("code");
        if (codeContentBlock) {
            context.renderChildren(node);
        } else {
        	if(languageName == null) {
        		// nohighlight (html escape)
				html.text(node.getContentChars().normalizeEOL());
			} else if(languageName.equals("raw")) {
        		// raw (without html escape)
				html.append(node.getContentChars().normalizeEOL());
			} else {
				String code = node.getContentChars().normalizeEOL();
				CharSequence content;
				if(Highlighter.findLanguage(languageName) != null) {
					content = highlight(languageName, code);
				} else {
					content = code
							.replace("&", "&amp;")
							.replace("<", "&lt;")
							.replace(">", "&gt;");
				}
				if(isReplaceYenSign) {
					content = content.toString().replace("\\", "&yen;");
				}
				html.append(content);
			}
        }
        html.tag("/code");
        html.tag("/pre").closePre();
        html.lineIf(context.getHtmlOptions().htmlBlockCloseTagEol);
	}
	
	protected CharSequence highlight(String languageName, String code) {
		HighlightResult result = highlighter.highlight(languageName, code);
		return result.getResult();
	}
}
