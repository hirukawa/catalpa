package onl.oss.catalpa.model;

import com.esotericsoftware.yamlbeans.YamlReader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import onl.oss.catalpa.AutoDetectReader;
import onl.oss.catalpa.Util;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Content {

    private final Path file;
    private final FileTime lastModifiedTime;
    private final Map<String, Object> yaml = new HashMap<>();
    private final Map<String, String> blocks = new LinkedHashMap<>();

    public Content(Path file) throws IOException {
        this.file = file;
        this.lastModifiedTime = Files.getLastModifiedTime(file);

        List<String> lines = AutoDetectReader.readAllLines(file);

        // 先頭から連続する空行を削除します。
        while (!lines.isEmpty() && lines.getFirst().isBlank()) {
            lines.removeFirst();
        }

        // YAML文字列
        String strYaml = null;

        if (!lines.isEmpty()) {
            // ファイルの先頭が --- で始まっている場合は YAML front matter 形式として扱います。
            if (lines.getFirst().startsWith("---") && lines.getFirst().substring(3).isBlank()) {
                for (int i = 1; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("---") && lines.getFirst().substring(3).isBlank()) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 1; j < i; j++) {
                            sb.append(lines.get(j));
                            sb.append("\r\n");
                        }
                        while (i >= 0) {
                            lines.remove(i--);
                        }
                        strYaml = sb.toString();
                        break;
                    }
                }
            } else if (Util.isYamlFile(file)) {
                // ファイルの先頭が --- で始まっておらず、ファイルの拡張子が yml などである場合はファイル全体を YAML として扱います。
                strYaml = String.join("\r\n", lines);
                lines.clear();
            }
        }

        if (strYaml != null) {
            try (YamlReader yamlReader = new YamlReader(strYaml)) {
                Object obj = yamlReader.read();
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) obj;
                    yaml.putAll(m);

                    if (file.getFileName().equals(Path.of("config.yml"))) {
                        yaml.put("config", m);
                    }
                }
            }
        }

        String filename = file.getFileName().toString().toLowerCase();

        // Content blocks
        String blockName = "content";
        if (filename.startsWith("_")) {
            int i = filename.indexOf('.');
            blockName = filename.substring(1, i >= 0 ? i : filename.length());
        }

        StringBuilder blockBody = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("#--")) {
                String s = line.trim();
                if (s.length() >= 6 && s.endsWith("--")) {
                    if (!blockBody.isEmpty()) {
                        blocks.put(blockName, blockBody.toString());
                    }
                    blockName = s.substring(3, s.length() - 2);
                    blockBody.setLength(0);
                    continue;
                }
            }

            blockBody.append(line);
            blockBody.append("\r\n");
        }

        if (!blockBody.isEmpty()) {
            blocks.put(blockName, blockBody.toString());
        }
    }

    public Path getPath() {
        return file;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public Map<String, Object> getYaml() {
        return yaml;
    }

    public Map<String, String> getBlocks() {
        return blocks;
    }

    public Map<String, String> expandBlocks(Configuration freeMarker, Map<String, Object> dataModel) throws IOException, TemplateException {
        Map<String, String> expandedBlocks = new HashMap<>();
        Map<String, Object> dm = new HashMap<>(dataModel);

        for (Map.Entry<String, String> block : blocks.entrySet()) {
            StringWriter out = new StringWriter();
            Template template = new Template(null, block.getValue(), freeMarker);
            template.process(dm, out);
            String value = out.toString();
            dm.put(block.getKey(), value);
            expandedBlocks.put(block.getKey(), value);
        }

        return expandedBlocks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("content ").append(file).append("\r\n");
        sb.append("yaml\r\n");
        for (Map.Entry<String, Object> entry : yaml.entrySet()) {
            sb.append("  ").append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
        }
        sb.append("blocks\r\n");
        for (Map.Entry<String, String> entry : blocks.entrySet()) {
            sb.append("  ").append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
        }

        return sb.toString();
    }
}
