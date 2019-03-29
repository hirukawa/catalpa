package net.osdn.catalpa.flexmark.ext;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;

public class BasicNodeExtension implements HtmlRenderer.HtmlRendererExtension {

	public static Extension create() {
		return new BasicNodeExtension();
	}
	
	private BasicNodeExtension() {
	}
	
	@Override
	public void rendererOptions(MutableDataHolder options) {
	}

	@Override
	public void extend(Builder rendererBuilder, String rendererType) {
		rendererBuilder.nodeRendererFactory(new NodeRendererFactory() {
			@Override
			public NodeRenderer create(DataHolder options) {
				return new BasicNodeRenderer(options);
			}
		});
	}
}
