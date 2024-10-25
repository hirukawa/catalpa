package onl.oss.catalpa;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import onl.oss.catalpa.freemarker.MarkdownDirective;
import onl.oss.catalpa.model.Content;
import onl.oss.catalpa.model.Folder;
import onl.oss.catalpa.model.Progress;
import onl.oss.catalpa.model.SearchIndex;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Generator {

    private final Path input;
    private final Path output;
    private final Map<String, Object> systemDataModel;
    private final Configuration freeMarker;

    private List<SearchIndex> searchIndexes = new ArrayList<>();

    private Consumer<Progress> consumer;
    private long progressMax;
    private AtomicLong progressValue = new AtomicLong(0L);

    public Generator(Path input, Path output, Map<String, Object> systemDataModel, Consumer<Progress> consumer) throws IOException {
        this.input = input.toRealPath(LinkOption.NOFOLLOW_LINKS);
        this.output = output.toRealPath(LinkOption.NOFOLLOW_LINKS);
        this.systemDataModel = systemDataModel != null ? systemDataModel : new HashMap<>();
        this.consumer = consumer;

        freeMarker = new Configuration(Configuration.VERSION_2_3_33);
        freeMarker.setDefaultEncoding("UTF-8");
        freeMarker.setURLEscapingCharset("UTF-8");
        freeMarker.setSharedVariable("markdown", new MarkdownDirective());
        freeMarker.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
                new FileTemplateLoader(input.resolve("templates").toFile()),
                new FileTemplateLoader(input.toFile())
        }));
    }

    public void generate() throws IOException {
        if (consumer != null) {
            consumer.accept(new Progress(0.0));
        }

        progressMax = countFiles(input);
        progressValue.set(0L);

        searchIndexes.clear();

        Folder rootFolder = retrieve(null, input);

        if (Files.exists(input.resolve("templates").resolve("search.ftl"))) {
            try {
                applyTemplate(rootFolder, searchIndexes);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

        if (dir.getFileName().startsWith("_")) {
            // アンダーバーで始まるフォルダーはスキップします。
            return true;
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
                } else {
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

    private Folder retrieve(Folder parent, Path dir) throws IOException {
        setProgress(dir);

        Folder folder = new Folder(parent, dir);

        Path targetDirectory = output.resolve(input.relativize(dir));
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }
        System.out.println("targetDirectory=" + targetDirectory);

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

        others.parallelStream().forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    // ディレクトリ
                    retrieve(folder, path);
                } else {
                    // ファイル
                    if (Util.isMarkdownFile(path)) {
                        // Markdownファイルは、テンプレートを適用します。
                        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                        Content content = CacheManager.getContent(path, lastModifiedTime);
                        if (content == null) {
                            content = new Content(path);
                            CacheManager.putContent(path, lastModifiedTime, content);
                        }
                        applyTemplate(folder, content);
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
        String filename = content.getPath().getFileName().toString();
        int i = filename.lastIndexOf('.');
        if (i < 0) {
            filename = filename + ".html";
        } else {
            filename = filename.substring(0, i) + ".html";
        }

        Path source = content.getPath();
        Path target = output.resolve(input.relativize(source)).resolveSibling(filename);

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

        Path relativeInputPath = input.relativize(content.getPath());
        Path relativeOutputPath = relativeInputPath.resolveSibling(filename);

        //
        // siteurl
        //
        String siteurl = "";
        Object obj = dataModel.get("siteurl");
        if (obj instanceof String s) {
            siteurl = s.trim();
            if (siteurl.endsWith("/")) {
                siteurl = siteurl.substring(0, siteurl.length() - 1);
            }
        }
        dataModel.put("siteurl", siteurl);

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
        String url = siteurl + "/" + URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");
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

        // コンテンツを FreeMarker で変数展開します。
        dataModel.putAll(folder.expandBlocks(freeMarker, dataModel));
        dataModel.putAll(content.expandBlocks(freeMarker, dataModel));

        Template template;
        if (dataModel.get("template") instanceof String name) {
            if (!name.toLowerCase().endsWith(".ftl")) {
                name = name + ".ftl";
            }
            template = freeMarker.getTemplate(name);
        } else {
            template = freeMarker.getTemplate("default.ftl");
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            try {
                template.process(dataModel, writer);
            } catch (TemplateException e) {
                throw new IOException(e);
            }
        }

        FileTime sourceLastModifiedTime = Files.getLastModifiedTime(source);
        Files.setLastModifiedTime(target, sourceLastModifiedTime);

        obj = content.getYaml().get("title");
        String title = obj instanceof String ? (String)obj : null;
        searchIndex = SearchIndex.create(path, title, target);
        if (searchIndex != null) {
            searchIndexes.add(searchIndex);
            CacheManager.putSearchIndex(target, cacheLastModifiedTime, searchIndex);
        }
    }

    private void applyTemplate(Folder rootFolder, List<SearchIndex> searchIndexes) throws IOException, TemplateException {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.putAll(systemDataModel);
        dataModel.putAll(rootFolder.createDataModel());

        String path = "search.html";

        //
        // siteurl
        //
        String siteurl = "";
        Object obj = dataModel.get("siteurl");
        if (obj instanceof String s) {
            siteurl = s.trim();
            if (siteurl.endsWith("/")) {
                siteurl = siteurl.substring(0, siteurl.length() - 1);
            }
        }
        dataModel.put("siteurl", siteurl);

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
        String url = siteurl + "/" + URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");
        dataModel.put("url", url);

        //
        // db
        //
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

        Template template = freeMarker.getTemplate("search.ftl");

        Path target = output.resolve(path);
        Path targetDirectory = target.getParent();
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            try {
                template.process(dataModel, writer);
            } catch (TemplateException e) {
                throw new IOException(e);
            }
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
