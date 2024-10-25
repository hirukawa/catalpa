package onl.oss.catalpa;

import onl.oss.catalpa.model.Content;
import onl.oss.catalpa.model.SearchIndex;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

public class CacheManager {

    private static final Map<Path, Map.Entry<FileTime, Content>> contents = new HashMap<>();
    private static final Map<Path, Map.Entry<FileTime, SearchIndex>> searchIndexes = new HashMap<>();

    public static void clear() {
        contents.clear();
        searchIndexes.clear();
    }

    public static Content getContent(Path file, FileTime lastModifiedTime) {
        Map.Entry<FileTime, Content> entry = contents.get(file);
        if (entry != null && entry.getKey().equals(lastModifiedTime)) {
            return entry.getValue();
        }
        return null;
    }

    public static void putContent(Path file, FileTime lastModifiedTime, Content content) {
        contents.put(file, Map.entry(lastModifiedTime, content));
    }

    public static SearchIndex getSearchIndex(Path file, FileTime lastModifiedTime) {
        Map.Entry<FileTime, SearchIndex> entry = searchIndexes.get(file);
        if (entry != null && entry.getKey().equals(lastModifiedTime)) {
            return entry.getValue();
        }
        return null;
    }

    public static void putSearchIndex(Path file, FileTime lastModifiedTime, SearchIndex searchIndex) {
        searchIndexes.put(file, Map.entry(lastModifiedTime, searchIndex));
    }
}
