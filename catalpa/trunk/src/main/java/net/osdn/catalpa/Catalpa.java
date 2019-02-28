package net.osdn.catalpa;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import net.osdn.catalpa.SitemapItem.ChangeFreq;
import net.osdn.catalpa.addon.blog.BlogAddOn;
import net.osdn.catalpa.freemarker.BaseurlMethod;
import net.osdn.catalpa.freemarker.MarkdownDirective;
import net.osdn.catalpa.handler.BlockHandler;
import net.osdn.catalpa.handler.LessHandler;
import net.osdn.catalpa.handler.TemplateHandler;
import net.osdn.catalpa.handler.YamlFrontMatterHandler;
import net.osdn.util.io.AutoDetectReader;

public class Catalpa {

	public static void main(String[] args) throws Exception {
	
		Path inputPath = Paths.get("sample/input");
		Path outputPath = Paths.get("sample/output");
		Catalpa catalpa = new Catalpa(inputPath);
		long s = System.currentTimeMillis();
		catalpa.process(outputPath);
		long e = System.currentTimeMillis();
		System.out.println("time=" + (e - s) + "ms");
	}
	
	public static final String CONFIG_FILENAME = "config.yml";
	
	private static final List<Handler> DEFAULT_HANDLERS = Arrays.asList(
		new YamlFrontMatterHandler(),
		new BlockHandler(),
		new TemplateHandler(),
		new LessHandler()
	);
	
	private static final List<AddOn> DEFAULT_ADDONS = Arrays.asList(
		new BlogAddOn()
	);
		
	private Path inputPath;
	private List<Handler> handlers = new ArrayList<Handler>();
	private List<AddOn> addons = new ArrayList<AddOn>();
	private List<String> excludeFileNames = Arrays.asList(new String[] {
		"htdocs",
		"include",
		"templates",
		CONFIG_FILENAME
	});
	private List<String> excludePrefixes = Arrays.asList(new String[] {
		"_"
	});
	private List<String> execludeSuffixes = Arrays.asList(new String[] {
		".ppk"	
	});
	
	private Configuration freeMarker;
	private List<SitemapItem> sitemap = new ArrayList<SitemapItem>();
	
	public Catalpa(Path inputPath) {
		this(inputPath, DEFAULT_HANDLERS, DEFAULT_ADDONS);
	}
	
	public Catalpa(Path inputPath, Collection<Handler> handlers, Collection<AddOn> addons) {
		this.inputPath = inputPath;
		if(handlers != null) {
			for(Handler handler : handlers) {
				this.handlers.add(handler);
			}
		}
		Collections.sort(this.handlers, new Comparator<Handler>() {
			@Override
			public int compare(Handler o1, Handler o2) {
				return o1.getPriority() - o2.getPriority();
			}
		});
		if(addons != null) {
			for(AddOn addon : addons) {
				this.addons.add(addon);
			}
		}
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
	
	public void process(Path outputPath) throws Exception {
		process(outputPath, null);
	}
	
	public void process(Path outputPath, Map<String, Object> options) throws Exception {
		freeMarker = new Configuration(Configuration.VERSION_2_3_28);
		freeMarker.setDefaultEncoding("UTF-8");
		freeMarker.setSharedVariable("baseurl", new BaseurlMethod());
		freeMarker.setSharedVariable("markdown", new MarkdownDirective());
		freeMarker.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
			new FileTemplateLoader(new File(inputPath.toFile(), "templates")),
			new FileTemplateLoader(inputPath.toFile())
		}));

		Context context = new Context(inputPath, outputPath);
		if(options != null) {
			for(Entry<String, Object> option : options.entrySet()) {
				context.getSystemDataModel().put(option.getKey(), option.getValue());
			}
		}

		String type = null;
		Path filename = inputPath.resolve(Catalpa.CONFIG_FILENAME);
		if(Files.exists(filename) && !Files.isDirectory(filename)) {
			Map<String, Object> config = context.load(filename);
			type = config.get("type") != null ? config.get("type").toString() : null;
		}
		AddOn addon = getApplicableAddOn(type);
		if(addon != null) {
			addon.prepare(inputPath, outputPath, options, context);
		}
		
		context.setFreeMarker(freeMarker);
		context.setInputPath(inputPath);
		context.setOutputPath(outputPath);
		retrieve(context);
		createSitemap(context);
		
		if(addon != null) {
			addon.execute(inputPath, outputPath, options, context);
		}
	}
	
	protected void retrieve(Context context) throws Exception {
		if(isExclude(context.getInputPath())) {
			return;
		}

		if(Files.isDirectory(context.getInputPath())) {
			Files.createDirectories(context.getOutputPath());
			Path config = context.getInputPath().resolve(CONFIG_FILENAME);
			if(Files.exists(config) && !Files.isDirectory(config)) {
				context.load(config);
			}
			FileTime lastModifiedTime = null;
			try(Stream<Path> stream = Files.list(context.getInputPath())) {
				for(Iterator<Path> it = stream.iterator(); it.hasNext();) {
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
				if(context.getLastModifiedTime() == null || lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
					context.setLastModifiedTime(lastModifiedTime);
				}
				List<Handler> handlers = getApplicableHandlers(path);
				if(handlers.size() > 0) {
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
						Files.copy(context.getInputPath(), context.getOutputPath(), StandardCopyOption.REPLACE_EXISTING);
					} else {
						List<String> lines = Util.readAllLines(reader);
						Files.write(context.getOutputPath(), lines, StandardCharsets.UTF_8);
					}
					if(Files.exists(context.getOutputPath()) && !Files.isDirectory(context.getOutputPath())) {
						Files.setLastModifiedTime(context.getOutputPath(), context.getLastModifiedTime());

						SitemapItem item = createSitemapItem(context);
						if(item != null) {
							sitemap.add(item);
						}
					}
				}
			} finally {
				if(reader != null) {
					reader.close();
				}
			}
		}
	}
	
	protected boolean isExclude(Path path) {
		if(!Files.exists(path)) {
			return true;
		}
		String name = path.getFileName().toString().toLowerCase();
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
		for(String s : execludeSuffixes) {
			if(name.endsWith(s)) {
				return true;
			}
		}
		return false;
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
		
		Map<String, Object> dataModel = new HashMap<String, Object>();
		dataModel.put("sitemap", sitemap);
		
		
		Template template = context.getFreeMarker().getTemplate("sitemap.ftl");
		Path sitemap = context.getRootOutputPath().resolve("sitemap.xml");
		try (Writer out = Files.newBufferedWriter(sitemap, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			template.process(dataModel, out);
		}
	}
}
