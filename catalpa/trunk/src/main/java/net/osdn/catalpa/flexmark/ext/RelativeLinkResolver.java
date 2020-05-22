package net.osdn.catalpa.flexmark.ext;

import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.renderer.LinkResolverBasicContext;
import com.vladsch.flexmark.html.renderer.LinkStatus;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataValueFactory;

public class RelativeLinkResolver implements LinkResolver {

	private static final DataValueFactory<Object> NULL_VALUE_FACTORY = new DataValueFactory<Object>() {
		@Override
		public Object apply(DataHolder dataHolder) {
			return null;
		}
	};

	private String relativeUrlPrefix;

	public RelativeLinkResolver(LinkResolverBasicContext context) {
		relativeUrlPrefix = (String)context.getOptions().getOrCompute(RelativeLinkExtension.RELATIVE_URL_PREFIX, NULL_VALUE_FACTORY);
	}

	@Override
	public ResolvedLink resolveLink(Node node, LinkResolverBasicContext context, ResolvedLink link) {
		String url = link.getUrl();
		if(url.length() > 0) {
			if(url.charAt(0) == '/' || url.indexOf(":/") != -1 || url.startsWith("data:")) {
				return link.withStatus(LinkStatus.VALID).withUrl(url);
			} else if(relativeUrlPrefix != null) {
				return link.withStatus(LinkStatus.VALID).withUrl(relativeUrlPrefix + url);
			} else {
				return link.withStatus(LinkStatus.VALID).withUrl(url);
			}
		}
		return link;
	}


	/*
	@Override
	public ResolvedLink resolveLink(Node node, LinkResolverContext context, ResolvedLink link) {
		String url = link.getUrl();
		if(url.length() > 0) {
			if(url.charAt(0) == '/' || url.indexOf(":/") != -1 || url.startsWith("data:")) {
				return link.withStatus(LinkStatus.VALID).withUrl(url);
			} else {
				return link.withStatus(LinkStatus.VALID).withUrl(relativeUrlPrefix + url);
			}
		}
		
		return link;
	}
	 */
}
