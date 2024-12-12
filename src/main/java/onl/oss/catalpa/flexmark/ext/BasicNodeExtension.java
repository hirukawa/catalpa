package onl.oss.catalpa.flexmark.ext;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.misc.Extension;
import onl.oss.catalpa.Generator;

public class BasicNodeExtension implements HtmlRenderer.HtmlRendererExtension {

	public static Extension create(Generator generator) {
		return new BasicNodeExtension(generator);
	}

	private final Generator generator;

	private BasicNodeExtension(Generator generator) {
		this.generator = generator;
	}
	
	@Override
	public void rendererOptions(MutableDataHolder options) {
	}

	@Override
	public void extend(Builder rendererBuilder, String rendererType) {
		rendererBuilder.nodeRendererFactory(new NodeRendererFactory() {
			@Override
			public NodeRenderer apply(DataHolder options) {
				return new BasicNodeRenderer(generator, options);
			}
		});
	}
}
