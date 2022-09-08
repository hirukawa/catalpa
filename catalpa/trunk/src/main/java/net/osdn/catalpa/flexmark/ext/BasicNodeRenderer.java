package net.osdn.catalpa.flexmark.ext;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;

public class BasicNodeRenderer implements NodeRenderer {

	public BasicNodeRenderer(DataHolder options) {
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
		set.add(new NodeRenderingHandler<Paragraph>(Paragraph.class, new NodeRenderingHandler.CustomNodeRenderer<Paragraph>() {
			@Override
			public void render(Paragraph node, NodeRendererContext context, HtmlWriter html) {
				BasicNodeRenderer.this.render(node, context, html);
			}
		}));
		set.add(new NodeRenderingHandler<Heading>(Heading.class, new NodeRenderingHandler.CustomNodeRenderer<Heading>() {
			@Override
			public void render(Heading node, NodeRendererContext context, HtmlWriter html) {
				BasicNodeRenderer.this.render(node, context, html);
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
			if(child instanceof ImageRef) {
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

	protected void render(Heading node, NodeRendererContext context, HtmlWriter html) {
		if(2 <= node.getLevel() && node.getLevel() <= 5) {
			// id と name に含まれているタグとルビ記法は取り除きます。
			String id = node.getAnchorRefId();
			id = id.replaceAll("<.+?>", "");
			id = id.replaceAll("｜", "");
			id = id.replaceAll("《.+?》", "");
			String name = node.getAnchorRefText().toString().trim().replace(' ', '+');
			name = name.replaceAll("<.+?>", "");
			name = name.replaceAll("｜", "");
			name = name.replaceAll("《.+?》", "");
			html.raw("<a class=\"h" + node.getLevel() + " anchor\" id=\"" + id + "\" name=\"" + name + "\"></a>");
		}
		context.delegateRender();
	}
}
