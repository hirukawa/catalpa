package net.osdn.catalpa;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import org.brotli.wrapper.Brotli;
import org.brotli.wrapper.enc.Encoder;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import net.osdn.blogs.flexmark.ext.highlight.HighlightExtension;
import net.osdn.blogs.flexmark.ext.kbd.KbdExtension;
import net.osdn.blogs.flexmark.ext.samp_button.SampButtonExtension;
import net.osdn.catalpa.SitemapItem.ChangeFreq;
import net.osdn.catalpa.addon.blog.BlogAddOn;
import net.osdn.catalpa.flexmark.ext.BasicNodeExtension;
import net.osdn.catalpa.flexmark.ext.LineDividableTableExtension;
import net.osdn.catalpa.flexmark.ext.RelativeLinkExtension;
import net.osdn.catalpa.freemarker.BaseurlMethod;
import net.osdn.catalpa.freemarker.LastModifiedTracker;
import net.osdn.catalpa.freemarker.MarkdownDirective;
import net.osdn.catalpa.handler.BlockHandler;
import net.osdn.catalpa.handler.LessHandler;
import net.osdn.catalpa.handler.TemplateHandler;
import net.osdn.catalpa.handler.YamlFrontMatterHandler;
import net.osdn.util.io.AutoDetectReader;

public class Catalpa {
	
	static {
		Brotli.loadLibrary();
	}
	
	public static final String CONFIG_FILENAME = "config.yml";
	
	private static final List<Handler> DEFAULT_HANDLERS = Arrays.asList(
		new YamlFrontMatterHandler(),
		new BlockHandler(),
		new TemplateHandler(),
		new LessHandler()
	);
	
	private Path inputPath;
	private List<Handler> handlers = new ArrayList<Handler>();
	private List<AddOn> addons = new ArrayList<AddOn>();
	private List<String> excludeFileNames = new ArrayList<String>(Arrays.asList(new String[] {
		"htdocs",
		"include",
		"templates",
		CONFIG_FILENAME
	}));
	private List<String> copyOnlyDirectoryNames = Arrays.asList(new String[] {
		"lib"
	});
	private List<String> excludePrefixes = Arrays.asList(new String[] {
		"_"
	});
	private List<String> excludeSuffixes = Arrays.asList(new String[] {
		".ppk"	
	});
	/** 出力フォルダーに必ずファイル名のリストです。
	 * ここに記載されているファイルは excludePrefixes, excludeSuffixes よりも優先されます。
	 */
	private List<String> includeFileNames = new ArrayList<>(Arrays.asList(new String[] {
		"_redirects" // Netlifyのリダイレクト定義ファイル
	}));

	private AddOn addon;
	private Configuration freeMarker;
	private List<SitemapItem> sitemap = new ArrayList<SitemapItem>();
	private List<SearchIndex> searchIndexes;
	private Set<String> compressionTargets = new HashSet<String>();
	private Set<String> compressionFormats = new HashSet<String>();
	private ProgressObserver observer;
	private int progress;
	private int maxProgress;
	
	public Catalpa(Path inputPath) {
		this(inputPath, DEFAULT_HANDLERS, Arrays.asList(new BlogAddOn()));
	}
	
	public Catalpa(Path inputPath, Collection<Handler> handlers, Collection<AddOn> addons) {
		this.inputPath = inputPath;
		
		if(handlers != null) {
			for(Handler handler : handlers) {
				this.handlers.add(handler);
			}
		}
		Collections.sort(this.handlers, (o1, o2) -> o1.getPriority() - o2.getPriority());
		if(addons != null) {
			for(AddOn addon : addons) {
				this.addons.add(addon);
			}
		}
	}
	
	public MutableDataSet getMarkdownOptions() {
		MutableDataSet options = new MutableDataSet();
		options.set(HtmlRenderer.FENCED_CODE_NO_LANGUAGE_CLASS, "nohighlight");
		options.set(HighlightExtension.REPLACE_YEN_SIGN, true);
		options.set(Parser.EXTENSIONS, Arrays.asList(new Extension[] {
				AttributesExtension.create(),
				DefinitionExtension.create(),
				WikiLinkExtension.create(),
				StrikethroughExtension.create(),
				TaskListExtension.create(),
				TablesExtension.create(),
				TypographicExtension.create(),

				HighlightExtension.create(),
				KbdExtension.create(),
				SampButtonExtension.create(),
				
				BasicNodeExtension.create(),
				LineDividableTableExtension.create(),
				RelativeLinkExtension.create()
		}));
		return options;
	}
	
	public AddOn getApplicableAddOn(String type) throws Exception {
		for(AddOn addon : addons) {
			if(addon.isApplicable(type)) {
				return addon;
			}
		}
		return null;
	}
	
	public List<Handler> getApplicableHandlers(Path path) {
		List<Handler> applicableHandlers = new ArrayList<Handler>();
		for(Handler handler : handlers) {
			if(handler.isApplicable(path)) {
				applicableHandlers.add(handler);
			}
		}
		return applicableHandlers;
	}
	
	public ProgressObserver getProgressObserver() {
		return (this.observer != null) ? this.observer : ProgressObserver.EMPTY;
	}
	
	public void process(Path outputPath, Map<String, Object> options, ProgressObserver observer) throws Exception {
		this.observer = (observer != null) ? observer : ProgressObserver.EMPTY;
		
		freeMarker = new Configuration(Configuration.VERSION_2_3_28);
		freeMarker.setDefaultEncoding("UTF-8");
		freeMarker.setSharedVariable("baseurl", new BaseurlMethod());
		freeMarker.setSharedVariable("markdown", new MarkdownDirective(getMarkdownOptions()));
		freeMarker.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
			new LastModifiedTracker.TemplateLoader(new File(inputPath.toFile(), "templates")),
			new LastModifiedTracker.TemplateLoader(inputPath.toFile())
		}));

		Context context = new Context(inputPath, outputPath);
		if(options != null) {
			for(Entry<String, Object> option : options.entrySet()) {
				context.getSystemDataModel().put(option.getKey(), option.getValue());
			}
		}
		context.setFreeMarker(freeMarker);

		Map<String, Object> config = null;
		String type = null;
		Path filename = inputPath.resolve(Catalpa.CONFIG_FILENAME);
		if(Files.exists(filename) && !Files.isDirectory(filename)) {
			config = context.load(filename);
			type = config.get("type") != null ? config.get("type").toString() : null;

			
			compressionTargets.clear();
			compressionFormats.clear();
			if(!Boolean.TRUE.equals(options.get("_PREVIEW"))) {
				for(String s : Util.getValues(config, "compression.target")) {
					if(s.startsWith(".")) {
						s = s.substring(1);
					}
					compressionTargets.add(s.toLowerCase());
				}
				for(String s : Util.getValues(config, "compression.format")) {
					if(s.startsWith(".")) {
						s = s.substring(1);
					}
					compressionFormats.add(s.toLowerCase());
				}
			}
			
			addon = getApplicableAddOn(type);
		}
		if(addon != null) {
			addon.setCatalpa(this);
			addon.prepare(inputPath, outputPath, config, options, context);
		}
		if(config == null) {
			config = new HashMap<String, Object>();
		}
		context.getSystemDataModel().put("config", config);
		
		filename = inputPath.resolve("templates").resolve("search.ftl");
		if(Files.exists(filename) && !Files.isDirectory(filename)) {
			searchIndexes = new ArrayList<SearchIndex>();
			excludeFileNames.add("search.md");
		}
		
		maxProgress = countFiles(inputPath);
		context.setInputPath(inputPath);
		context.setOutputPath(outputPath);
		retrieve(context);
		
		if(addon != null) {
			addon.postExecute(inputPath, outputPath, options, context, sitemap, searchIndexes);
		}
		
		createSitemap(context);
		createSearchIndex(context);
	}
	
	protected int countFiles(Path path) throws IOException {
		if(isExclude(path)) {
			return 0;
		}
		if(isCopyOnlyDirectory(path)) {
			try (Stream<Path> stream = Files.walk(path)) {
				return (int)stream.filter(Files::isRegularFile).count();
			}
		}
		if(Files.isDirectory(path)) {
			try(Stream<Path> stream = Files.list(path)) {
				int count = 0;
				for(Path child : stream.collect(Collectors.toList())) {
					if(child.getFileName().toString().equalsIgnoreCase(CONFIG_FILENAME)) {
						continue;
					}
					count += countFiles(child);
				}
				return count;
			}
		} else {
			return 1;
		}
	}
	
	protected void retrieve(Context context) throws Exception {
		if(isExclude(context.getInputPath())) {
			return;
		}
		if(isCopyOnlyDirectory(context.getInputPath())) {
			copyRecursively(context.getInputPath(), context.getOutputPath());
			return;
		}

		if(Files.isDirectory(context.getInputPath())) {
			Files.createDirectories(context.getOutputPath());
			Path config = context.getInputPath().resolve(CONFIG_FILENAME);
			if(Files.exists(config) && !Files.isDirectory(config)) {
				context.load(config);
			}
			FileTime lastModifiedTime = context.getLastModifiedTime();
			try(Stream<Path> list = Files.list(context.getInputPath())) {
				for(Iterator<Path> it = list.iterator(); it.hasNext();) {
					Path child = it.next();
					if(child.getFileName().toString().equalsIgnoreCase(CONFIG_FILENAME)) {
						continue;
					}
					Context subContext = context.clone();
					subContext.setInputPath(context.getInputPath().resolve(child.getFileName()));
					subContext.setOutputPath(context.getOutputPath().resolve(child.getFileName()));
					retrieve(subContext);
					Path out = subContext.getOutputPath();
					if(out != null && Files.exists(out)) {
						if(!Files.isDirectory(out) || out.toFile().listFiles(f->f.isFile()).length > 0) {
							FileTime ft = Files.getLastModifiedTime(out);
							if(lastModifiedTime == null || ft.compareTo(lastModifiedTime) > 0) {
								lastModifiedTime = ft;
							}
						}
					}
				}
			}
			if(lastModifiedTime != null) {
				Files.setLastModifiedTime(context.getOutputPath(), lastModifiedTime);
			}
		} else {
			Reader reader = null;
			try {
				Path path = context.getInputPath();
				FileTime lastModifiedTime = Files.getLastModifiedTime(path);
				context.setLastModifiedTime(lastModifiedTime);
				if(addon != null) {
					addon.execute(context);
				}
				List<Handler> handlers = getApplicableHandlers(path);
				if(context.getOutputPath() != null && handlers.size() > 0) {
					reader = new AutoDetectReader(path);
					for(Handler handler : handlers) {
						Writer writer = new StringWriter();
						handler.handle(context, reader, writer);
						reader.close();
						reader = new StringReader(writer.toString());
					}
				}
				if(context.getOutputPath() != null) {
					if(reader == null) {
						copyFileIfModified(context.getInputPath(), context.getOutputPath());
						return;
					} else {
						observer.setProgress(++progress / (double)maxProgress);
						observer.setText(inputPath.relativize(path).toString());
						List<String> lines = Util.readAllLines(reader);
						write(context.getOutputPath(), lines, StandardCharsets.UTF_8);
						// create search index
						if(searchIndexes != null && context.getOutputPath().getFileName().toString().toLowerCase().endsWith(".html")) {
							SearchIndex index = SearchIndex.create(context, lines);
							if(index != null) {
								searchIndexes.add(index);
							}
						}
					}
					if(Files.exists(context.getOutputPath()) && !Files.isDirectory(context.getOutputPath())) {
						Files.setLastModifiedTime(context.getOutputPath(), context.getLastModifiedTime());

						SitemapItem item = createSitemapItem(context);
						if(item != null) {
							sitemap.add(item);
						}
					}
				} else {
					observer.setProgress(++progress / (double)maxProgress);
					observer.setText("");
				}
			} finally {
				if(reader != null) {
					reader.close();
				}
			}
		}
	}
	
	public boolean isExclude(Path path) {
		if(!Files.exists(path)) {
			return true;
		}
		String name = path.getFileName().toString().toLowerCase();
		for(String i : includeFileNames) {
			if(name.equals(i)) {
				return false;
			}
		}
		for(String d : excludeFileNames) {
			if(name.equals(d)) {
				return true;
			}
		}
		for(String p : excludePrefixes) {
			if(name.startsWith(p)) {
				return true;
			}
		}
		for(String s : excludeSuffixes) {
			if(name.endsWith(s)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isCopyOnlyDirectory(Path path) {
		if(Files.isDirectory(path)) {
			String name = path.getFileName().toString().toLowerCase();
			for(String n : copyOnlyDirectoryNames) {
				if(name.equals(n)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void write(Path path, Iterable<? extends CharSequence> lines, Charset cs) throws IOException {
		CharsetEncoder encoder = cs.newEncoder();
		try(ByteArrayOutputStream out = new ByteArrayOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
			for(CharSequence line : lines) {
				writer.append(line);
				writer.newLine();
			}
			writer.flush();
			write(path, out.toByteArray());
		}
	}
	
	public void write(Path path, byte[] bytes) throws IOException {

		Files.write(path, bytes);
		
		String ext = Util.getFileExtension(path);
		if(compressionTargets.contains(ext)) {
			for(String format : compressionFormats) {
				byte[] data = null;
				if(format.equals("gz") || format.equals("gzip")) {
					data = Zopfli.compress(bytes);
				} else if(format.equals("br") || format.equals("brotli")) {
					data = Encoder.compress(bytes);
				}
				if(data != null) {
					Path p = path.resolveSibling(path.getFileName().toString() + "." + format);
					Files.write(p, data);
				}
			}
		}
	}
	
	protected void copyRecursively(Path src, Path dst) throws IOException {
		Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path relativePath = src.relativize(dir);
				Files.createDirectories(dst.resolve(relativePath));
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path relativePath = src.relativize(file);
				copyFileIfModified(file, dst.resolve(relativePath));
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	protected void copyFileIfModified(Path src, Path dst) throws IOException {
		observer.setProgress(++progress / (double)maxProgress);
		observer.setText(inputPath.relativize(src).toString());
				
		if(Files.exists(dst)
				&& Files.getLastModifiedTime(src).equals(Files.getLastModifiedTime(dst))
				&& Files.size(src) == Files.size(dst)) {
			return;
		}
		
		String ext = Util.getFileExtension(dst);
		if(compressionTargets.contains(ext)) {
			write(dst, Files.readAllBytes(src));
		} else {
			Files.copy(src, dst,
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES);
		}
	}
	
	protected SitemapItem createSitemapItem(Context context) throws IOException {
		if(context.getSiteUrl() == null) {
			return null;
		}
		if(context.getOutputPath() == null) {
			return null;
		}
		String filename = context.getOutputPath().getFileName().toString().toLowerCase();
		if(!filename.endsWith(".html")) {
			return null;
		}

		return new SitemapItem(
				URLEncoder.encode(context.getUrl()),
				context.getLastModifiedTime(),
				ChangeFreq.Daily,
				1.0);
	}
	
	protected void createSitemap(Context context) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		if(sitemap.size() == 0) {
			return;
		}
		
		List<SitemapItem> copyList = new ArrayList<SitemapItem>(sitemap);
		Collections.reverse(copyList);
		
		Map<String, Object> dataModel = new HashMap<String, Object>();
		dataModel.put("sitemap", copyList);
		
		Template template = context.getFreeMarker().getTemplate("sitemap.ftl");
		Path sitemap = context.getRootOutputPath().resolve("sitemap.xml");
		try (Writer out = Files.newBufferedWriter(sitemap, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			synchronized(context.getFreeMarker()) {
				template.process(dataModel, out);
			}
		}
	}
	
	protected void createSearchIndex(Context context) throws Exception {
		if(searchIndexes == null) {
			return;
		}
		
		observer.setText("検索用インデックスを作成しています…");

        Collections.sort(searchIndexes, Comparator.comparing(SearchIndex::getLastModifiedTime).reversed());

		StringBuilder db = new StringBuilder("\r\n");
		for(int i = 0; i < searchIndexes.size(); i++) {
			SearchIndex index = searchIndexes.get(i);
			db.append("\t\t\t{ title:\"");
			db.append(index.getTitle());
			db.append("\", url:\"");
			db.append(index.getUrl());
			db.append("\", text:\"");
			db.append(index.getTitle());
			db.append("\\n");
			db.append(index.getText());
			db.append("\" }");
			if(i + 1 < searchIndexes.size()) {
				db.append(",\r\n");
			}
		}
		db.append("\r\n\t\t");
		
		Context subContext = context.clone();
		Path path = context.getRootInputPath().resolve("search.md");
		if(Files.exists(path) && !Files.isDirectory(path)) {
			List<Handler> handlers = getApplicableHandlers(path);
			if(handlers.size() > 0) {
				Reader reader = null;
				try {
					reader = new AutoDetectReader(path);
					for(Handler handler : handlers) {
						Writer writer = new StringWriter();
						handler.handle(subContext, reader, writer);
						reader.close();
						reader = new StringReader(writer.toString());
					}
				} finally {
					if(reader != null) {
						reader.close();
					}
				}
			}
		}

		Map<String, Object> dataModel = subContext.getDataModel();
		dataModel.put("db", db.toString());

		Template template = context.getFreeMarker().getTemplate("search.ftl");
		Path searchHtml = context.getRootOutputPath().resolve("search.html");
		try(ByteArrayOutputStream out = new ByteArrayOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			synchronized (context.getFreeMarker()) {
				template.process(dataModel, writer);
			}
			writer.flush();
			write(searchHtml, out.toByteArray());
		}
	}
}
