package net.osdn.catalpa.flexmark.ext.kbd.internal;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;

import com.vladsch.flexmark.util.data.DataHolder;
import net.osdn.catalpa.flexmark.ext.kbd.Kbd;

public class KbdNodeRenderer implements NodeRenderer {

    public KbdNodeRenderer(DataHolder options) {
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
        set.add(new NodeRenderingHandler<Kbd>(Kbd.class, new NodeRenderingHandler.CustomNodeRenderer<Kbd>() {
        	@Override
        	public void render(Kbd node, NodeRendererContext context, HtmlWriter html) {
        		KbdNodeRenderer.this.render(node, context, html);
        	}
        }));
        return set;
    }

    private void render(Kbd node, NodeRendererContext context, HtmlWriter html) {
    	html.withAttr()
    	.attr("data-length", Integer.toString(node.getText().length()))
    	.tag("kbd").text(node.getText()).tag("/kbd");
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer apply(DataHolder options) {
            return new KbdNodeRenderer(options);
        }
    }
}
