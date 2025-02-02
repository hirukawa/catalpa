package onl.oss.catalpa;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import onl.oss.catalpa.blog.Blog;
import onl.oss.catalpa.blog.Category;
import onl.oss.catalpa.blog.Page;
import onl.oss.catalpa.blog.Post;
import onl.oss.catalpa.freemarker.MarkdownDirective;
import onl.oss.catalpa.model.Content;
import onl.oss.catalpa.model.Folder;
import onl.oss.catalpa.model.Progress;
import onl.oss.catalpa.model.SearchIndex;
import onl.oss.catalpa.model.SitemapItem;

import java.io.IOException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static onl.oss.catalpa.Logger.INFO;

public class Generator {

    /* Markdown 処理をせずにそのまま出力フォルダーにコピーするフォルダーです。
     * jsOnlyLightbox などの JavaScript ライブラリをこのフォルダーに格納しておけば、内部の README.md などが .html に変換されるのを防げます。
     */
    private static final List<String> copyOnlyDirectoryNames = List.of("lib");

    /* 出力フォルダーにコピーしないファイルの拡張子です。（小文字で定義する必要があります）
     * 秘密鍵ファイルなどインターネット上にアップロードすべきではないファイルを誤ってアップロードしてしまうのを防げます。
     */
    private static final List<String> copyExcludeFileExtensions = List.of(".ppk");

    private final Path input;
    private final Path output;
    private final Map<String, Object> systemDataModel;
    private final Configuration freeMarker;

    public String siteurl;

    private Blog blog;
    private List<SearchIndex> searchIndexes = Collections.synchronizedList(new ArrayList<>());
    private List<SitemapItem> sitemapItems = Collections.synchronizedList(new ArrayList<>());

    private Consumer<Progress> consumer;
    private long progressMax;
    private AtomicLong progressValue = new AtomicLong(0L);

    @SuppressWarnings("this-escape")
    public Generator(Path input, Path output, Map<String, Object> systemDataModel, Consumer<Progress> consumer) throws IOException {
        this.input = input.toRealPath(LinkOption.NOFOLLOW_LINKS);
        this.output = output.toRealPath(LinkOption.NOFOLLOW_LINKS);
        this.systemDataModel = systemDataModel != null ? systemDataModel : new HashMap<>();
        this.consumer = consumer;

        freeMarker = new Configuration(Configuration.VERSION_2_3_34);
        freeMarker.setDefaultEncoding("UTF-8");
        freeMarker.setURLEscapingCharset("UTF-8");
        freeMarker.setLogTemplateExceptions(false);
        freeMarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freeMarker.setSharedVariable("markdown", new MarkdownDirective(this));
        freeMarker.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
                new FileTemplateLoader(input.resolve("templates").toFile()),
                new FileTemplateLoader(input.toFile())
        }));
    }

    public void generate(Lock writeLock) throws IOException {
        try {
            writeLock.lock();

            if (consumer != null) {
                consumer.accept(new Progress(0.0));
            }

            progressMax = countFiles(input);
            progressValue.set(0L);

            sitemapItems.clear();
            searchIndexes.clear();

            blog = Blog.create(input);

            Folder rootFolder = retrieve(null, input, false);

            Path searchIndexTemplatePath = input.resolve("templates").resolve("search.ftl");
            if (Files.exists(searchIndexTemplatePath)) {
                try {
                    createSearchIndex(rootFolder, searchIndexes);
                } catch (GeneratorException e) {
                    throw e;
                } catch (Exception e) {
                    throw new GeneratorException(searchIndexTemplatePath, e);
                }
            }

            if (!sitemapItems.isEmpty()) {
                Path sitemapTemplatePath = input.resolve("templates").resolve("sitemap.ftl");
                if (Files.exists(sitemapTemplatePath)) {
                    try {
                        createSitemap(rootFolder, sitemapItems);
                    } catch (GeneratorException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new GeneratorException(sitemapTemplatePath, e);
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private boolean isSkip(Path dir) {
        if (dir.equals(output)) {
            // 出力フォルダーはスキップします。
            return true;
        }

        if (Util.isTemplatesDirectory(dir)) {
            // templates フォルダーはスキップします。
            return true;
        }

        if (dir.getFileName().toString().startsWith("_")) {
            // アンダーバーで始まるフォルダーはスキップします。
            return true;
        }

        return false;
    }

    private boolean isExcludeFile(Path file) {
        String filename = file.getFileName().toString();
        int i = filename.lastIndexOf('.');
        if (i >= 0) {
            String ext = filename.substring(i).toLowerCase();
            if (copyExcludeFileExtensions.contains(ext)) {
                return true;
            }
        }
        return false;
    }

    private long countFiles(Path dir) throws IOException {
        long count = 1;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                if (Files.isDirectory(path)) {
                    if (!isSkip(path)) {
                        count += countFiles(path);
                    }
                } else if (!isExcludeFile(path)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void setProgress(Path path) {
        long value = progressValue.incrementAndGet();
        if (consumer != null) {
            double d = (double)value / (double)progressMax;
            consumer.accept(new Progress(d, path));
        }
    }

    private Folder retrieve(Folder parent, Path dir, boolean isAncestorCopyOnly) throws IOException {
        setProgress(dir);

        Folder folder = new Folder(parent, dir);

        // このディレクトリの名前が copyOnlyDirectoryNames に含まれている場合、
        // このディレクトリ下位のファイルは Markdown 処理をせずに、そのままコピーされます。
        boolean copyOnly = isAncestorCopyOnly || copyOnlyDirectoryNames.contains(dir.getFileName().toString());

        Path targetDirectory = output.resolve(input.relativize(dir));
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        List<Path> config = new ArrayList<>();
        List<Path> shared = new ArrayList<>();
        List<Path> others = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String filename = path.getFileName().toString();
                    if (filename.equalsIgnoreCase("config.yml")) {
                        config.add(path);
                        return;
                    } else if (filename.startsWith("_") && Util.isContentFile(path)) {
                        shared.add(path);
                        return;
                    }
                } else if (Files.isDirectory(path)) {
                    if (isSkip(path)) {
                        return;
                    }
                }
                others.add(path);
            });
        }

        for (Path file : config) {
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            Content content = CacheManager.getContent(file, lastModifiedTime);
            if (content == null) {
                content = new Content(file);
                CacheManager.putContent(file, lastModifiedTime, content);
            }
            folder.addContent(content);

            if (siteurl == null) {
                if (content.getYaml().get("siteurl") instanceof String s) {
                    s = s.trim();
                    if (s.endsWith("/")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    siteurl = s;
                }
            }

            setProgress(file);
        }

        for (Path file : shared) {
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            Content content = CacheManager.getContent(file, lastModifiedTime);
            if (content == null) {
                content = new Content(file);
                CacheManager.putContent(file, lastModifiedTime, content);
            }
            folder.addContent(content);
            setProgress(file);
        }

        //
        // ブログのインデックス・ページを生成します。
        //
        if (blog != null && blog.getPath().equals(dir)) {
            try {
                for (Page page : blog.getPages()) {
                    applyTemplate(folder, page, null);
                }

                for (Category category : blog.getCategories()) {
                    Page page = new Page(category.getUrl(), category.getName(), category.getPosts());
                    applyTemplate(folder, page, category);
                }
            } catch (GeneratorException e) {
                throw e;
            } catch (Exception e) {
                Path blogIndexTemplatePath = input.resolve("templates").resolve("blog-index.ftl");
                throw new GeneratorException(blogIndexTemplatePath, e);
            }
        }

        //
        // 子ファイルを再帰的に並列処理します。
        //
        others.parallelStream().forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    // ディレクトリ
                    retrieve(folder, path, copyOnly);
                } else {
                    // Markdownファイル（かつ、コピーオンリーの指定がない場合）
                    if (!copyOnly && Util.isMarkdownFile(path)) {
                        // Markdownファイルは、テンプレートを適用します。
                        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                        Content content = CacheManager.getContent(path, lastModifiedTime);
                        if (content == null) {
                            content = new Content(path);
                            CacheManager.putContent(path, lastModifiedTime, content);
                        }
                        applyTemplate(folder, content);
                        setProgress(path);
                    } else if (isExcludeFile(path)) {
                        // 除外ファイルの拡張子に該当する場合は出力ファイルにコピーしません。
                        INFO("SKIP: " + path);
                    } else {
                        // その他のファイルは、出力ファイルにコピーします。
                        Path target = output.resolve(input.relativize(path));
                        copy(path, target);
                        setProgress(path);
                    }
                }
            } catch (GeneratorException e) {
                throw e;
            } catch (Exception e) {
                throw new GeneratorException(path, e);
            }
        });

        return folder;
    }

    private void applyTemplate(Folder folder, Content content) throws IOException, TemplateException {
        //
        // ブログ
        //
        Post post = null;
        if (blog != null) {
            post = blog.getPostBy(content.getPath());
            if (post != null && post.isSkip()) {
                INFO("スキップ: " + content.getPath());
                return;
            }
        }

        // filename
        String filename = content.getPath().getFileName().toString();
        int i = filename.lastIndexOf('.');
        if (i < 0) {
            filename = filename + ".html";
        } else {
            filename = filename.substring(0, i) + ".html";
        }

        Path source = content.getPath();
        Path target = output.resolve(input.relativize(source)).resolveSibling(filename);

        // Markdown ファイルの YAMLフロントマターに draft: skip が設定されている場合はスキップします。
        Object draft = content.getYaml().get("draft");
        if (draft != null && draft.toString().trim().equalsIgnoreCase("skip")) {
            INFO("スキップ: " + content.getPath());
            // 出力フォルダーのファイルは削除します。
            Files.deleteIfExists(target);
            return;
        }

        Path relativeInputPath = input.relativize(content.getPath());
        Path relativeOutputPath = relativeInputPath.resolveSibling(filename);

        // path
        String path = relativeOutputPath.toString().replace('\\', '/');

        // url
        // URLEncoder.encode ではパス区切り文字 "/" が "%2F" にエンコードされてしまうので、"%2F" を "/" に戻しています。
        String url = (siteurl != null ? siteurl : "") + "/" + URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");

        // サイトマップ
        if (siteurl != null) {
            SitemapItem sitemapItem = new SitemapItem(url, content.getLastModifiedTime(), SitemapItem.ChangeFreq.Daily, 1.0);
            sitemapItems.add(sitemapItem);
        }

        // 最終更新日時に一致する検索インデックスがあり、出力HTMLが存在する場合、出力HTMLを再作成する必要はありません。
        FileTime cacheLastModifiedTime = getLastModifiedTime(folder, content);
        SearchIndex searchIndex = CacheManager.getSearchIndex(target, cacheLastModifiedTime);
        if (searchIndex != null && Files.exists(target)) {
            searchIndexes.add(searchIndex);
            return;
        }

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.putAll(systemDataModel);
        dataModel.putAll(folder.createDataModel());
        dataModel.putAll(content.getYaml());

        //
        // siteurl
        //
        dataModel.put("siteurl", (siteurl != null ? siteurl : ""));

        //
        // baseurl
        //
        String baseurl = "../".repeat(relativeOutputPath.getNameCount() - 1);
        dataModel.put("baseurl", baseurl);

        //
        // path
        //
        dataModel.put("path", path);

        //
        // url
        //
        dataModel.put("url", url);

        //
        // dateModified（非推奨）
        //
        Date dateModified = new Date(cacheLastModifiedTime.toMillis());
        dataModel.put("dateModified", dateModified);

        //
        // contentLastModified
        //
        Date contentLastModified = new Date(content.getLastModifiedTime().toMillis());
        dataModel.put("contentLastModified", contentLastModified);

        //
        // ファイルパス
        //
        dataModel.put("_ROOTPATH", folder.getRootPath().toString());
        dataModel.put("_FILEPATH", content.getPath().toString());

        //
        // ブログ
        //
        if (post != null) {
            Blog clone = blog.clone();
            clone.setPost(post);
            dataModel.put("blog", clone);
        } else {
            dataModel.put("blog", blog);
        }

        // コンテンツを FreeMarker で変数展開します。
        dataModel.putAll(folder.expandBlocks(freeMarker, dataModel));
        dataModel.putAll(content.expandBlocks(freeMarker, dataModel));

        Template template;
        if (dataModel.get("template") instanceof String name) {
            if (!name.toLowerCase().endsWith(".ftl")) {
                name = name + ".ftl";
            }
            template = freeMarker.getTemplate(name);
        } else if (post != null) {
            template = freeMarker.getTemplate("blog-post.ftl");
        } else {
            template = freeMarker.getTemplate("default.ftl");
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }

        FileTime sourceLastModifiedTime = Files.getLastModifiedTime(source);
        Files.setLastModifiedTime(target, sourceLastModifiedTime);

        Object obj = content.getYaml().get("title");
        String title = obj instanceof String ? (String)obj : null;
        searchIndex = SearchIndex.create(path, title, target);
        if (searchIndex != null) {
            searchIndexes.add(searchIndex);
            CacheManager.putSearchIndex(target, cacheLastModifiedTime, searchIndex);
        }
    }

    private void createSearchIndex(Folder rootFolder, List<SearchIndex> searchIndexes) throws IOException, TemplateException {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.putAll(systemDataModel);
        dataModel.putAll(rootFolder.createDataModel());

        String path = "search.html";

        //
        // siteurl
        //
        dataModel.put("siteurl", (siteurl != null ? siteurl : ""));

        //
        // baseurl
        //
        dataModel.put("baseurl", "");

        //
        // path
        //
        dataModel.put("path", path);

        //
        // url
        //
        // URLEncoder.encode ではパス区切り文字 "/" が "%2F" にエンコードされてしまうので、"%2F" を "/" に戻しています。
        String url = (siteurl != null ? siteurl : "") + "/" + URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");
        dataModel.put("url", url);

        //
        // db
        //
        searchIndexes.sort(Comparator.comparing(SearchIndex::getLastModifiedTime).reversed());

        StringBuilder db = new StringBuilder("\n");
        for (int i = 0; i < searchIndexes.size(); i++) {
            SearchIndex si = searchIndexes.get(i);
            db.append("\t\t\t{url:\"");
            db.append(si.getUrl());
            db.append("\", title:\"");
            db.append(si.getTitle());
            db.append("\", text:\"");
            db.append(si.getTitle());
            db.append("\\n");
            db.append(si.getText());
            db.append("\"}");
            if (i + 1 < searchIndexes.size()) {
                db.append(",\n");
            }
        }
        db.append("\n\t\t");
        dataModel.put("db", db);

        //
        // ファイルパス
        //
        dataModel.put("_FILEPATH", rootFolder.getRootPath().resolve(path));

        // コンテンツを FreeMarker で変数展開します。
        dataModel.putAll(rootFolder.expandBlocks(freeMarker, dataModel));

        Template template = freeMarker.getTemplate("search.ftl");

        Path target = output.resolve(path);
        Path targetDirectory = target.getParent();
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }
    }

    private void createSitemap(Folder rootFolder, List<SitemapItem> sitemapItems) throws IOException, TemplateException {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.putAll(systemDataModel);
        dataModel.putAll(rootFolder.createDataModel());

        String path = "sitemap.xml";

        dataModel.put("sitemap", sitemapItems);

        //
        // siteurl
        //
        dataModel.put("siteurl", (siteurl != null ? siteurl : ""));

        //
        // baseurl
        //
        dataModel.put("baseurl", "");

        //
        // path
        //
        dataModel.put("path", path);

        //
        // url
        //
        // URLEncoder.encode ではパス区切り文字 "/" が "%2F" にエンコードされてしまうので、"%2F" を "/" に戻しています。
        String url = (siteurl != null ? siteurl : "") + "/" + URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");
        dataModel.put("url", url);

        //
        // ファイルパス
        //
        dataModel.put("_FILEPATH", rootFolder.getRootPath().resolve(path));

        // コンテンツを FreeMarker で変数展開します。
        dataModel.putAll(rootFolder.expandBlocks(freeMarker, dataModel));

        Template template = freeMarker.getTemplate("sitemap.ftl");

        Path target = output.resolve(path);
        Path targetDirectory = target.getParent();
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }
    }

    private void applyTemplate(Folder folder, Page page, Category category) throws IOException, TemplateException {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.putAll(systemDataModel);
        dataModel.putAll(folder.createDataModel());
        Blog clone = blog.clone();
        clone.setPage(page);
        clone.setCategory(category);
        dataModel.put("blog", clone);

        Path relativeOutputPath = Path.of(URLDecoder.decode(page.getUrl(), StandardCharsets.UTF_8).replace('/', '\\'));
        Path target = output.resolve(relativeOutputPath);

        //
        // siteurl
        //
        dataModel.put("siteurl", (siteurl != null ? siteurl : ""));

        //
        // baseurl
        //
        String baseurl = "../".repeat(relativeOutputPath.getNameCount() - 1);
        dataModel.put("baseurl", baseurl);

        //
        // path
        //
        String path = relativeOutputPath.toString().replace('\\', '/');
        dataModel.put("path", path);

        //
        // url
        //
        // URLEncoder.encode ではパス区切り文字 "/" が "%2F" にエンコードされてしまうので、"%2F" を "/" に戻しています。
        String url = (siteurl != null ? siteurl : "") + "/" + URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");
        dataModel.put("url", url);

        //
        // ファイルパス
        //
        dataModel.put("_ROOTPATH", folder.getRootPath().toString());
        dataModel.put("_FILEPATH", null);

        // コンテンツを FreeMarker で変数展開します。
        dataModel.putAll(folder.expandBlocks(freeMarker, dataModel));

        Template template = freeMarker.getTemplate("blog-index.ftl");

        Path targetDirectory = target.getParent();
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }
    }

    private static boolean copy(Path source, Path target) throws IOException {
        Path targetDirectory = target.getParent();
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        FileTime sourceLastModifiedTime = null;
        try {
            sourceLastModifiedTime = Files.getLastModifiedTime(source);
        } catch (Exception ignored) {}

        FileTime targetLastModifiedTime = null;
        try {
            targetLastModifiedTime = Files.getLastModifiedTime(target);
        } catch (Exception ignored) {}

        if (targetLastModifiedTime != null && targetLastModifiedTime.equals(sourceLastModifiedTime)) {
            return false;
        }

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        if (sourceLastModifiedTime != null) {
            Files.setLastModifiedTime(target, sourceLastModifiedTime);
        }

        return true;
    }

    private static FileTime getLastModifiedTime(Folder folder, Content content) {
        FileTime lastModifiedTime = content.getLastModifiedTime();
        while (folder != null) {
            FileTime t = folder.getLastModifiedTime();
            if (t != null && t.compareTo(lastModifiedTime) > 0) {
                lastModifiedTime = t;
            }
            folder = folder.getParent();
        }
        return lastModifiedTime;
    }
}
