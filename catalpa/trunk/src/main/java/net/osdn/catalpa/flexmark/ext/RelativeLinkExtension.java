package net.osdn.catalpa.flexmark.ext;

import java.util.Set;

import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.LinkResolverFactory;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;

public class RelativeLinkExtension implements HtmlRendererExtension {
    public static final DataKey<String> RELATIVE_URL_PREFIX = new DataKey<String>("RELATIVE_URL_PREFIX", "");
	
	public static Extension create() {
		return new RelativeLinkExtension();
	}

	private LinkResolverFactory linkResolverFactory = new RelativeLinkResolverFactory();

	@Override
	public void rendererOptions(MutableDataHolder options) {
	}

	@Override
	public void extend(Builder rendererBuilder, String rendererType) {
		rendererBuilder.linkResolverFactory(linkResolverFactory);
	}
	
	
	private static class RelativeLinkResolverFactory implements LinkResolverFactory {

		@Override
		public Set<Class<? extends LinkResolverFactory>> getAfterDependents() {
			return null;
		}

		@Override
		public Set<Class<? extends LinkResolverFactory>> getBeforeDependents() {
			return null;
		}

		@Override
		public boolean affectsGlobalScope() {
			return false;
		}

		@Override
		public LinkResolver create(LinkResolverContext context) {
			return new RelativeLinkResolver(context);
		}
	}
}
