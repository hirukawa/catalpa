package net.osdn.catalpa.flexmark.ext.highlight;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.misc.Extension;

public class HighlightExtension implements HtmlRenderer.HtmlRendererExtension {
	public static final DataKey<Boolean> REPLACE_YEN_SIGN = new DataKey<>("REPLACE_YEN_SIGN", true);
	
	private HighlightExtension() {
	}
	
	public static Extension create() {
		return new HighlightExtension();
	}

	@Override
	public void rendererOptions(MutableDataHolder options) {
	}

	@Override
	public void extend(Builder rendererBuilder, String rendererType) {
		rendererBuilder.nodeRendererFactory(new NodeRendererFactory() {
			@Override
			public NodeRenderer apply(DataHolder options) {
				return new HighlightRenderer(options);
			}
		});
	}
}
