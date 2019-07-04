package net.osdn.catalpa.addon.blog;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map.Entry;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.options.MutableDataSet;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import net.osdn.catalpa.AddOn;
import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.CatalpaException;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.SitemapItem;
import net.osdn.catalpa.SitemapItem.ChangeFreq;
import net.osdn.catalpa.URLEncoder;
import net.osdn.catalpa.Util;
import net.osdn.catalpa.handler.TemplateHandler;
import net.osdn.util.io.AutoDetectReader;

public class BlogAddOn implements AddOn {
	private static Pattern LEADING_SEPARATOR = Pattern.compile("(<!--+\\s*more\\s*-+->)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern CATEGORY_ID_PATTERN = Pattern.compile("(.+)\\(([-_.a-zA-Z0-9]*)\\)$");
	private static final String THUMBNAIL_FILENAME = "thumbnail.png";
	
	private static String DEFAULT_THUMBNAIL_DATA_URI = null;
	
	static {
	    try(InputStream in = BlogAddOn.class.getClassLoader().getResourceAsStream("img/blog-default-thumbnail.png")) {
			DEFAULT_THUMBNAIL_DATA_URI = "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
		} catch(Exception e) {
			System.err.println("NotFound: img/blog-default-thumbnail.png");
		}
	}
	
	private Catalpa catalpa;
	private Factory factory;
	private Map<String, Object> blogDataModel = new HashMap<String, Object>();
	private Map<String, Object> draftDataModel = null;
	private int paginate = 10;
	
	private Map<Post, Integer> characterCounts = new HashMap<Post, Integer>();
	
	@Override
	public void setCatalpa(Catalpa catalpa) {
		this.catalpa = catalpa;
	}
	
	@Override
	public boolean isApplicable(String type) {
		return type != null && type.equals("blog");
	}

	/** 各ファイルを処理する前に呼ばれます。
	 * inputPathは入力フォルダー、outputPathは出力フォルダーです。
	 * 
	 */
	@Override
	public void prepare(Path inputPath, Path outputPath, Map<String, Object> config, Map<String, Object> options, Context context) throws IOException {
		if(options == null) {
			options = new HashMap<String, Object>();
		}
		Object obj;

		//config
		obj = config.get("paginate");
		if(obj != null) {
			try {
				int i = Integer.parseInt(obj.toString().trim());
				if(i >= 1) {
					paginate = i;
				}
			} catch(NumberFormatException e) {
				throw new NumberFormatException("paginate: " + e.getMessage());
			}
		}
		
		factory = createFactory(inputPath, outputPath);
		if(factory.hasDraft) {
			//draft template
			context.getSystemDataModel().put("template", "draft.ftl");
			options.put("_DRAFT", true);
		} else {
			//default template
			context.getSystemDataModel().put("template", "post.ftl");
			options.remove("_DRAFT");
		}
		options.remove("_DEFAULT_URL");
		
		List<Category> categories = factory.getCategories();
		categories.sort(Comparator.comparing(Category::getDate).thenComparing(c -> c.getPosts().size()).reversed());

		List<Post> posts = factory.getPosts();
		posts.sort(Comparator.comparing(Post::getDate).reversed());
		for(int i = 0; i < posts.size(); i++) {
			Post post = posts.get(i);
			if(factory.hasDraft && i == 0) {
				options.put("_DEFAULT_URL", post.getUrl());
			}
			if(Files.exists(post.getPath().getParent().resolve(THUMBNAIL_FILENAME))) {
				Path thumbnail = inputPath.relativize(post.getPath().getParent()).resolve(THUMBNAIL_FILENAME);
				post.setThumbnail(thumbnail.toString().replace('\\', '/'));
			} else if(DEFAULT_THUMBNAIL_DATA_URI != null) {
				post.setThumbnail(DEFAULT_THUMBNAIL_DATA_URI);
			}
		}
		
		obj = context.getDataModel().get("title");
		if(obj instanceof String) {
			blogDataModel.put("title", obj);
		}
		obj = context.getDataModel().get("description");
		if(obj instanceof String) {
			blogDataModel.put("description", obj);
		}
		blogDataModel.put("categories", categories);
		blogDataModel.put("posts", posts);
		blogDataModel.put("pager", new Pager(inputPath, outputPath, posts));
		context.getSystemDataModel().put("blog", blogDataModel);
		context.invalidateDataModel();
	}

	/** 各ファイルごとに呼ばれます。
	 * @throws IOException 
	 * @throws TemplateException 
	 * 
	 */
	@Override
	public void execute(Context context) throws IOException, TemplateException {
		if(!factory.containsPost(context.getInputPath())) {
			if(Post.isApplicable(context.getInputPath())) {
				context.setOutputPath(null);
			}
			return;
		}
		
		Post post = factory.getPostBy(context.getInputPath());

		blogDataModel.put("post", post);

		if(draftDataModel != null) {
			draftDataModel.put("characterCount", characterCounts.get(post));
		}
	}
	
	@Override
	public void postExecute(Path inputPath, Path outputPath, Map<String, Object> options, Context context, List<SitemapItem> sitemap) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Map<String, Object> dataModel = context.getDataModel();
		
		{ // create page html
			@SuppressWarnings("unchecked")
			List<Post> posts = (List<Post>)blogDataModel.get("posts");
			
			Template pageTemplate = context.getFreeMarker().getTemplate("page.ftl");
			int pages = (posts.size() - 1) / paginate + 1;
			int page = pages;
			int fromIndex = 0;
			int toIndex;
			while(fromIndex < posts.size()) {
				catalpa.getProgressObserver().setText(String.format("リストページ（%d）を作成しています…", (pages - page + 1)));
				
				toIndex = Math.min(fromIndex + paginate, posts.size());
				Map<String, Object> pageModel = new HashMap<String, Object>();
				pageModel.put("posts", posts.subList(fromIndex, toIndex));
				blogDataModel.put("page", pageModel);
				blogDataModel.put("pager", new Pager(
					new Link("前のページ", getPageUrl(page, page - 1, pages)),
					new Link("次のページ", getPageUrl(page, page + 1, pages))
				));

				String baseUrl = "";
				Path path;
				if(page == pages) {
					path = context.getOutputPath().resolve("index.html");
					SitemapItem item = new SitemapItem(
							URLEncoder.encode(context.getUrl()),
							FileTime.from(ZonedDateTime.now().toInstant()),
							ChangeFreq.Daily,
							1.0);
					sitemap.add(item);
				} else {
					baseUrl = "../";
					path = context.getOutputPath().resolve("page").resolve(page + ".html");
					Files.createDirectories(path.getParent());
				}
				for(int i = fromIndex; i < toIndex; i++) {
					Post post = posts.get(i);
					String relativePath = inputPath.relativize(post.getPath().getParent()).toString();
					String relativeUrlPrefix = baseUrl + relativePath.replace('\\', '/');
					if(relativeUrlPrefix.length() > 0) {
						relativeUrlPrefix += '/';
					}
					post.setRelativeUrlPrefix(relativeUrlPrefix);
				}
				dataModel.put("baseurl", baseUrl);
				
				try(ByteArrayOutputStream out = new ByteArrayOutputStream();
						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
					synchronized (context.getFreeMarker()) {
						pageTemplate.process(dataModel, writer);
					}
					writer.flush();
					catalpa.write(path, out.toByteArray());
				}
				
				// next page
				page--;
				fromIndex = toIndex;
			}
		}
		
		{ // create category html
			@SuppressWarnings("unchecked")
			List<Category> categories = (List<Category>)blogDataModel.get("categories");

			Template categoryTemplate = context.getFreeMarker().getTemplate("category.ftl");
			for(Category category : categories) {
				catalpa.getProgressObserver().setText(String.format("カテゴリーページ（%s）を作成しています…", category.getName()));
				
				Files.createDirectories(context.getOutputPath().resolve("category").resolve(category.getId()));
				blogDataModel.put("category", category);

				int pages = (category.getPosts().size() - 1) / paginate + 1;
				int page = pages;
				int fromIndex = 0;
				int toIndex;
				while(fromIndex < category.getPosts().size()) {
					toIndex = Math.min(fromIndex + paginate, category.getPosts().size());
					Map<String, Object> pageModel = new HashMap<String, Object>();
					pageModel.put("posts", category.getPosts().subList(fromIndex, toIndex));
					blogDataModel.put("page", pageModel);
					blogDataModel.put("pager", new Pager(
						new Link("前のページ", getCategoryPageUrl(page - 1, pages)),
						new Link("次のページ", getCategoryPageUrl(page + 1, pages))
					));
					
					String baseUrl = "../../";
					for(int i = fromIndex; i < toIndex; i++) {
						Post post = category.getPosts().get(i);
						String relativePath = inputPath.relativize(post.getPath().getParent()).toString();
						String relativeUrlPrefix = baseUrl + relativePath.replace('\\', '/');
						if(relativeUrlPrefix.length() > 0) {
							relativeUrlPrefix += '/';
						}
						post.setRelativeUrlPrefix(relativeUrlPrefix);
					}
					dataModel.put("baseurl", baseUrl);
					
					Path path;
					if(page == pages) {
						path = context.getOutputPath().resolve("category").resolve(category.getId()).resolve("index.html");
					} else {
						path = context.getOutputPath().resolve("category").resolve(category.getId()).resolve(page + ".html");
					}

					try(ByteArrayOutputStream out = new ByteArrayOutputStream();
							BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
						synchronized (context.getFreeMarker()) {
							categoryTemplate.process(dataModel, writer);
						}
						writer.flush();
						catalpa.write(path, out.toByteArray());
					}
					
					// next page
					page--;
					fromIndex = toIndex;
				}
			}
		}
	}
	
	protected String getPageUrl(int from, int to, int pages) {
		if(to < 1 || to > pages) {
			return null;
		}
		
		if(to == pages) {
			return "../index.html";
		} else if(from == pages) {
			return "page/" + to + ".html";
		} else {
			return to + ".html";
		}
	}
	
	protected String getCategoryPageUrl(int to, int pages) {
		if(to < 1 || to > pages) {
			return null;
		}
		
		if(to == pages) {
			return "index.html";
		} else {
			return to + ".html";
		}
	}
	
	protected Factory createFactory(Path inputPath, Path outputPath) throws IOException {
		Factory factory = new Factory(inputPath);
		List<Path> list = new ArrayList<Path>();

		Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if(catalpa.isExclude(dir) || catalpa.isCopyOnlyDirectory(dir)) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if(Post.isApplicable(file)) {
					list.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		for(Path path : list) {
			factory.getPostBy(path);
		}
		if(factory.hasDraft) {
			draftDataModel = new HashMap<String, Object>();
			draftDataModel.put("postCount", factory.posts.size());
			blogDataModel.put("draft", draftDataModel);
			
			factory.categories.clear();
			Iterator<Entry<Path, Post>> it = factory.posts.entrySet().iterator();
			while(it.hasNext()) {
				Entry<Path, Post> e = it.next();
				Post post = e.getValue();
				if(post.isDraft()) {
					for(Category category : post.getCategories()) {
						if(category.getId() == null) {
							category.setId(category.getName().toLowerCase());
						}
						Category c = factory.getCategoryBy(category.getName() + "(" + category.getId() + ")");
						c.add(post);
					}
				} else {
					it.remove();
				}
			}
		}
		for(Category category : factory.getCategories()) {
			if(category.getId() == null) {
				category.setId(category.getName().toLowerCase());
			}
			category.getPosts().sort(Comparator.comparing(Post::getDate).reversed());
		}
		
		return factory;
	}
	
	/* package private */ class Factory {

		private Path inputPath;
		private Map<String, Category> categories = new HashMap<String, Category>();
		private Map<Path, Post> posts = new HashMap<Path, Post>();
		private boolean hasDraft = false;

		public Factory(Path inputPath) {
			this.inputPath = inputPath;
		}
		
		public List<Category> getCategories() {
			return new ArrayList<Category>(categories.values());
		}
		
		public List<Post> getPosts() {
			return new ArrayList<Post>(posts.values());
		}
		
		public boolean containsCategory(String text) {
			String name = text;
			
			Matcher m = CATEGORY_ID_PATTERN.matcher(text);
			if(m.matches()) {
				name = m.group(1);
			}
			return categories.containsKey(name);
		}
		
		public Category getCategoryBy(String text) {
			String id = null;
			String name = text;
			
			Matcher m = CATEGORY_ID_PATTERN.matcher(text);
			if(m.matches()) {
				name = m.group(1);
				id = m.group(2);
			}
			
			Category category = categories.get(name);
			if(category == null) {
				category = new Category(name);
				categories.put(name, category);
			}
			if(category.getId() == null && id != null && !id.isEmpty()) {
				category.setId(id.toLowerCase());
			}
			
			return category;
		}
		
		public boolean containsPost(Path path) {
			return posts.containsKey(path);
		}
		
		public Post getPostBy(Path path) throws IOException {
			Post post = posts.get(path);
			if(post == null) {
				Map<String, Object> map = new HashMap<String, Object>();
				List<String> lines = AutoDetectReader.readAllLines(path);
				
				Iterator<String> iterator = lines.iterator();
				while(iterator.hasNext()) {
					if(iterator.next().trim().length() > 0) {
						break;
					}
					iterator.remove();
				}
				
				// YAML front matter
				if(lines.size() > 0 && ('^' + lines.get(0)).trim().equals("^---")) {
					for(int i = 1; i < lines.size(); i++) {
						if(('^' + lines.get(i)).trim().equals("^---")) {
							StringBuilder sb = new StringBuilder();
							for(int j = 1; j < i; j++) {
								sb.append(lines.get(j));
								sb.append("\r\n");
							}
							while(i >= 0) {
								lines.remove(i--);
							}
							Object obj = new YamlReader(sb.toString()).read();
							if(obj instanceof Map) {
								@SuppressWarnings("unchecked")
								Map<String, Object> m = (Map<String, Object>)obj;
								for(Entry<String, Object> entry : m.entrySet()) {
									map.put(entry.getKey(), entry.getValue());
								}
							}
							break;
						}
					}
				}
				
				// Blocks
				LinkedHashMap<String, String> blocks = new LinkedHashMap<String, String>();
				String blockName = null;
				StringBuilder blockBody = new StringBuilder();
				for(String line : lines) {
					String s = ('^' + line).trim();
					if(s.length() > 6 && s.startsWith("^#--") && s.endsWith("--")) {
						if(blockBody.length() > 0) {
							blocks.put(blockName, Util.trim(blockBody, "\r\n").toString());
						}
						blockName = s.substring(4, s.length() - 2);
						blockBody.setLength(0);
					} else if(blockBody.length() > 0 || line.trim().length() > 0) {
						blockBody.append(line);
						blockBody.append("\r\n");
					}
				}
				if(blockBody.length() > 0) {
					blocks.put(blockName, Util.trim(blockBody, "\r\n").toString());
				}
				if(blocks.containsKey(null) && !blocks.containsKey("content")) {
					blocks.put("content", blocks.remove(null));
				}
				String content = blocks.get("content");
				
				//
				if(map.get("date") != null && map.get("title") != null && content != null) {
					LocalDate date;
					try {
						date  = LocalDate.parse(map.get("date").toString());
					} catch(DateTimeParseException e) {
						throw new CatalpaException(path.toString(), "date", e);
					}
					String title = map.get("title").toString();

					Set<Category> categories = new LinkedHashSet<Category>(); 
					Object c = map.get("categories");
					if(c == null) {
						c = map.get("category");
					}
					if(c instanceof List) {
						for(Object e : (List<?>)c) {
							if(e != null) {
								Category category = getCategoryBy(e.toString());
								if(category != null) {
									categories.add(category);
								}
							}
						}
					} else if(c != null) {
						Category category = getCategoryBy(c.toString());
						if(category != null) {
							categories.add(category);
						}
					}
					
					String url = inputPath.relativize(Util.replaceFileExtension(path, TemplateHandler.APPLICABLE_EXTENSIONS, ".html")).toString().replace('\\', '/');
					String leading = content;
					Matcher m = LEADING_SEPARATOR.matcher(content);
					if(m.find()) {
						leading = content.substring(0, m.start(1));
					}
					post = new Post(path, url, date, title, categories, leading);
					for(Category category : categories) {
						category.add(post);
					}
					
					if(map.containsKey("draft")) {
						Object obj = map.get("draft");
						if(obj instanceof String && ((String)obj).equalsIgnoreCase("skip")) {
							post = null;
						} else {
							post.setDraft(true);
							hasDraft = true;
							characterCounts.put(post, countCharacters(content));
						}
					}
					if(post != null) {
						posts.put(path, post);
					}
				}
			}
			return post;
		}
	}
	
	private int countCharacters(String markdown) {
		MutableDataSet options = catalpa.getMarkdownOptions();
		Parser parser = Parser.builder(options).build();
		HtmlRenderer renderer = HtmlRenderer.builder(options).build();
		Document document = parser.parse(markdown);
		String html = renderer.render(document);
		String text = html
				.replaceAll("<[^>]*>", "")
				.replaceAll("&amp;", "&")
				.replaceAll("&lt;", "<")
				.replaceAll("&gt;", ">")
				.replaceAll("&quot;", "\"")
				.replaceAll("&ldquo;", "\"")
				.replaceAll("&rdquo;", "\"")
				.replaceAll("&lsquo;", "'")
				.replaceAll("&rsquo;", "'")
				.replaceAll("\r", "")
				.replaceAll("\n", "")
				.replaceAll(" ", "");
		return text.length();
	}
}
