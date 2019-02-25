package net.osdn.catalpa.flexmark.ext;

import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.LinkStatus;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.ast.Node;

public class RelativeLinkResolver implements LinkResolver {
	
	private String relativeUrlPrefix;

	public RelativeLinkResolver(LinkResolverContext context) {
		relativeUrlPrefix = context.getOptions().get(RelativeLinkExtension.RELATIVE_URL_PREFIX);
	}
	
	@Override
	public ResolvedLink resolveLink(Node node, LinkResolverContext context, ResolvedLink link) {
		String url = link.getUrl();
		if(url.length() > 0) {
			if(url.charAt(0) == '/' || url.indexOf(":/") != -1) {
				return link.withStatus(LinkStatus.VALID).withUrl(url);
			} else {
				return link.withStatus(LinkStatus.VALID).withUrl(relativeUrlPrefix + url);
			}
		}
		
		return link;
	}
}
