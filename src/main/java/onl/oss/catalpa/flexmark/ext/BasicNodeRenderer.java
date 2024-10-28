package onl.oss.catalpa.flexmark.ext;

import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.ext.definition.DefinitionList;
import com.vladsch.flexmark.ext.definition.DefinitionTerm;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import onl.oss.catalpa.freemarker.MarkdownDirective;
import onl.oss.catalpa.model.ImageSize;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static onl.oss.catalpa.Logger.ERROR;

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
		set.add(new NodeRenderingHandler<DefinitionList>(DefinitionList.class, new NodeRenderingHandler.CustomNodeRenderer<DefinitionList>() {
			@Override
			public void render(@NotNull DefinitionList node, @NotNull NodeRendererContext context, @NotNull HtmlWriter html) {
				BasicNodeRenderer.this.render(node, context, html);
			}
		}));
		set.add(new NodeRenderingHandler<Image>(Image.class, new NodeRenderingHandler.CustomNodeRenderer<Image>() {
			@Override
			public void render(@NotNull Image node, @NotNull NodeRendererContext context, @NotNull HtmlWriter html) {
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

	protected void render(DefinitionList node, NodeRendererContext context, HtmlWriter html) {
		// 定義リストの定義名を省略した場合は DefinitionTerm の子孫に Text ノードが存在しません。
		// 定義名が空ではない場合、DL に "has-term" クラス属性を追加します。
		if(node.hasChildren() && node.getFirstChild() instanceof DefinitionTerm) {
			String term = null;
			for(Node descendant : node.getFirstChild().getDescendants()) {
				if(descendant instanceof Text) {
					term = descendant.getChars().toString();
					break;
				}
			}
			if(term != null) {
				html.attr("class", "has-term");
			}
		}
		context.delegateRender();
	}

	protected void render(Image node, NodeRendererContext context, HtmlWriter html) {
		try {
			// 画像のサイズを取得して width 属性、height 属性を設定します。
			// 画像ファイル名に @2x などのスケール指定が含まれている場合は、スケールで割った値が width, height に設定されます。
			Path imgPath = null;
			String url = node.getUrl().toString();
			if (!url.isBlank()) {
				if (url.contains("://")) {
					// URL が http:// などの場合は何もしません。
				} else if (url.startsWith("/")) {
					// URL が / で始まっている場合はルートフォルダーからのパスとして解釈します。
					Path dir = getRootPath(context);
					if (dir != null) {
						imgPath = dir.resolve(url.substring(1));
					}
				} else {
					// URL が / で始まっていない場合はコンテンツ・ファイルからの相対パスとして解釈します。
					Path filepath = getFilePath(context);
					if (filepath != null) {
						Path dir = filepath.getParent();
						if (dir != null) {
							imgPath = dir.resolve(url);
						}
					}
				}
			}

			// 決定した画像ファイルパスが存在する場合は、画像サイズを取得して width 属性と height 属性を設定します。
			// ImageSize が返す width と height はファイル名に含まれるスケール指定を考慮した値になっています。
			if (imgPath != null && Files.exists(imgPath)) {
				ImageSize imageSize = ImageSize.get(imgPath);
				if (imageSize != null && imageSize.getWidth() >= 0 && imageSize.getHeight() >= 0) {
					html.attr("width", Integer.toString(imageSize.getWidth()));
					html.attr("height", Integer.toString(imageSize.getHeight()));
				}
			}
		} catch (Exception e) {
			ERROR(e);
		} finally {
			context.delegateRender();
		}
	}

	public static Path getRootPath(NodeRendererContext context) {
		Path rootpath = null;

		Object o = context.getDocument().getOrCompute(MarkdownDirective._ROOTPATH, dataHolder -> null);
		if (o instanceof String s) {
			if (!s.isBlank()) {
				rootpath = Path.of(s);
			}
		}

		return rootpath;
	}

	public static Path getFilePath(NodeRendererContext context) {
		Path filepath = null;

		Object o = context.getDocument().getOrCompute(MarkdownDirective._FILEPATH, dataHolder -> null);
		if (o instanceof String s) {
			if (!s.isBlank()) {
				filepath = Path.of(s);
			}
		}

		return filepath;
	}
}
