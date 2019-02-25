package net.osdn.catalpa.flexmark.ext;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.DataHolder;

public class ParagraphRenderer implements NodeRenderer {

	public ParagraphRenderer(DataHolder options) {
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
		set.add(new NodeRenderingHandler<Paragraph>(Paragraph.class, new CustomNodeRenderer<Paragraph>() {
			@Override
			public void render(Paragraph node, NodeRendererContext context, HtmlWriter html) {
				ParagraphRenderer.this.render(node, context, html);
			}
		}));
		set.add(new NodeRenderingHandler<Code>(Code.class, new CustomNodeRenderer<Code>() {
			@Override
			public void render(Code node, NodeRendererContext context, HtmlWriter html) {
				ParagraphRenderer.this.render(node, context, html);
			}
		}));
		return set;
	}
	
	protected void render(Paragraph node, NodeRendererContext context, HtmlWriter html) {
		boolean isImagesOnly = true;
		int count = 0;
		for(Node child : node.getChildren()) {
			if(child instanceof Image) {
				count++;
				continue;
			}
			if(child instanceof SoftLineBreak) {
				continue;
			}
			if(child instanceof HardLineBreak) {
				continue;
			}
			if(child instanceof AttributesNode) {
				continue;
			}
			isImagesOnly = false;
			break;
		}
		if(isImagesOnly && count >= 1) {
			html.attr("class", "images-only");
		}
		context.delegateRender();
	}
	
	protected void render(Code node, NodeRendererContext context, HtmlWriter html) {
		html.attr("data-length", Integer.toString(node.getText().length()));
		context.delegateRender();
	}
}
