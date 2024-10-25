package onl.oss.catalpa.model;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import onl.oss.catalpa.Util;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Folder {

    private final Folder parent;
    private final Path path;
    private final Map<String, Object> config = new HashMap<>();
    private final List<Content> contents = new ArrayList<>();
    private FileTime lastModifiedTime;

    public Folder(Folder parent, Path path) {
        this.parent = parent;
        this.path = path;
    }

    public Folder getParent() {
        return parent;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void addContent(Content content) {
        contents.add(content);

        if (Util.isConfigFile(content.getPath())) {
            config.putAll(content.getYaml());
        }

        if (lastModifiedTime == null || content.getLastModifiedTime().compareTo(lastModifiedTime) > 0) {
            lastModifiedTime = content.getLastModifiedTime();
        }
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public Map<String, Object> createDataModel() {
        Map<String, Object> dataModel = new HashMap<>();

        if (parent != null) {
            dataModel.putAll(parent.createDataModel());
        }

        for (Content content : contents) {
            dataModel.putAll(content.getYaml());
        }

        Map<String, Object> config = new HashMap<>();

        if (parent != null) {
            config.putAll(parent.getConfig());
        }

        if (!this.config.isEmpty()) {
            config.putAll(this.config);
        }
        dataModel.put("config", config);

        return dataModel;
    }

    public Map<String, String> expandBlocks(Configuration freeMarker, Map<String, Object> dataModel) throws IOException, TemplateException {
        Map<String, String> expandedBlocks = new HashMap<>();
        Map<String, Object> dm = new HashMap<>(dataModel);

        if (parent != null) {
            expandedBlocks.putAll(parent.expandBlocks(freeMarker, dataModel));
        }

        for (Content content : contents) {
            for (Map.Entry<String, String> block : content.getBlocks().entrySet()) {
                StringWriter out = new StringWriter();
                Template template = new Template(null, block.getValue(), freeMarker);
                template.process(dm, out);
                String value = out.toString();
                dm.put(block.getKey(), value);
                expandedBlocks.put(block.getKey(), value);
            }
        }

        return expandedBlocks;
    }

    public Path getRootPath() {
        if (parent == null) {
            return path;
        } else {
            return parent.getRootPath();
        }
    }
}
