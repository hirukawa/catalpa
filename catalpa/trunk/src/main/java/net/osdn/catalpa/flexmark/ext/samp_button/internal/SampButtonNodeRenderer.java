package net.osdn.catalpa.flexmark.ext.samp_button.internal;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;

import com.vladsch.flexmark.util.data.DataHolder;
import net.osdn.catalpa.flexmark.ext.samp_button.SampButton;

public class SampButtonNodeRenderer implements NodeRenderer {

    public SampButtonNodeRenderer(DataHolder options) {
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
        set.add(new NodeRenderingHandler<SampButton>(SampButton.class, new NodeRenderingHandler.CustomNodeRenderer<SampButton>() {
        	@Override
        	public void render(SampButton node, NodeRendererContext context, HtmlWriter html) {
        		SampButtonNodeRenderer.this.render(node, context, html);
        	}
        }));
        return set;
    }

    private void render(SampButton node, NodeRendererContext context, HtmlWriter html) {
    	html.withAttr()
    	.attr("class", "button")
    	.attr("data-length", Integer.toString(node.getText().length()))
    	.tag("samp").text(node.getText()).tag("/samp");
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer apply(DataHolder options) {
            return new SampButtonNodeRenderer(options);
        }
    }
}
