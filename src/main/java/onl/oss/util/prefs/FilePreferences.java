package onl.oss.util.prefs;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

public class FilePreferences extends AbstractPreferences {

    private static final Logger logger = System.getLogger(FilePreferences.class.getName());

    private final Map<String, String> root = new TreeMap<>();
    private final Map<String, FilePreferences> children = new TreeMap<>();
    private boolean removed;

    @SuppressWarnings("this-escape")
    public FilePreferences(AbstractPreferences parent, String name) {
        super(parent, name);

        try {
            sync();
        } catch (BackingStoreException e) {
            logger.log(Level.ERROR, "Unable to sync on creation of node " + name, e);
        }
    }

    @Override
    protected void putSpi(String key, String value) {
        root.put(key, value);
        try {
            flush();
        } catch (BackingStoreException e) {
            logger.log(Level.ERROR, "Unable to flush after putting " + key, e);
        }
    }

    @Override
    protected String getSpi(String key) {
        return root.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        root.remove(key);
        try {
            flush();
        } catch (BackingStoreException e) {
            logger.log(Level.ERROR, "Unable to flush after removing " + key, e);
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        removed = true;
        flush();
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return root.keySet().toArray(new String[]{});
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return children.keySet().toArray(new String[]{});
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        FilePreferences child = children.get(name);
        if (child == null || child.isRemoved()) {
            child = new FilePreferences(this, name);
            children.put(name, child);
        }
        return child;
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        if (isRemoved()) {
            return;
        }

        Path file = FilePreferencesFactory.getFilePath();

        if (file == null || Files.notExists(file)) {
            return;
        }

        Properties properties = new Properties();
        String path = getPath();

        synchronized (file) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }

        Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            if (name.startsWith(path)) {
                String key = name.substring(path.length());
                root.put(key, properties.getProperty(name));
            }
        }
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        Path file = FilePreferencesFactory.getFilePath();

        if (file == null) {
            return;
        }

        Properties properties = new Properties();
        String path = getPath();

        synchronized (file) {
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                } catch (IOException e) {
                    throw new BackingStoreException(e);
                }
            }

            @SuppressWarnings("unchecked")
            List<String> names = Collections.list((Enumeration<String>)properties.propertyNames());
            for (String name : names) {
                if (name.startsWith(path)) {
                    properties.remove(name);
                }
            }

            if (!removed) {
                for (String key : root.keySet()) {
                    properties.setProperty(path + key, root.get(key));
                }
            }

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(writer, null);
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }
    }

    private String getPath() {
        List<String> names = new ArrayList<>(List.of(""));
        FilePreferences node = this;

        do {
            if (!node.name().isBlank()) {
                names.addFirst(node.name());
            }
            node = (FilePreferences)node.parent();
        } while (node != null);

        return String.join(".", names);
    }

    public static String toString(List<String> list) {
        if (list == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');

        for (String value : list) {
            if (value != null) {
                value = value.replace("%", "%25")
                        .replace("=", "%3D")
                        .replace(",", "%2C")
                        .replace("\"", "%22");
                value = '"' + value + '"';
            } else {
                value = "null";
            }
            sb.append(value);
            sb.append(',');
        }

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append(']');
        return sb.toString();
    }

    public static List<String> toList(String s) {
        List<String> list = new ArrayList<>();

        if (s == null || s.isBlank()) {
            return list;
        }

        s = s.trim();
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
            for (String value : s.split(",")) {
                value = value.trim();
                if (value.equals("null")) {
                    value = null;
                } else {
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    value = value.replace("%22", "\"")
                            .replace("%2C", ",")
                            .replace("%3D", "=")
                            .replace("%25", "%");
                }
                list.add(value);
            }
        }

        return list;
    }

    public static String toString(Map<String, String> map) {
        if (map == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            key = key.replace("%", "%25")
                    .replace("=", "%3D")
                    .replace(",", "%2C")
                    .replace("\"", "%22");
            key = '"' + key + '"';

            String value = entry.getValue();
            if (value == null) {
                value = "null";
            } else {
                value = value.replace("%", "%25")
                        .replace("=", "%3D")
                        .replace(",", "%2C")
                        .replace("\"", "%22");
                value = '"' + value + '"';
            }

            sb.append(key);
            sb.append('=');
            sb.append(value);
            sb.append(',');
        }

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append('}');
        return sb.toString();
    }

    public static Map<String, String> toMap(String s) {
        Map<String, String> map = new LinkedHashMap<>();

        if (s == null || s.isBlank()) {
            return map;
        }

        s = s.trim();
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
            for (String entry : s.split(",")) {
                int i = entry.indexOf('=');
                if (i > 1) {
                    String key = entry.substring(0, i).trim();
                    if (key.equals("null")) {
                        continue;
                    }

                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() -1);
                    }
                    key = key.replace("%22", "\"")
                            .replace("%2C", ",")
                            .replace("%3D", "=")
                            .replace("%25", "%");

                    String value = entry.substring(i + 1).trim();
                    if (value.equals("null")) {
                        value = null;
                    } else {
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        value = value.replace("%22", "\"")
                                .replace("%2C", ",")
                                .replace("%3D", "=")
                                .replace("%25", "%");
                    }

                    map.put(key, value);
                }
            }
        }

        return map;
    }
}
