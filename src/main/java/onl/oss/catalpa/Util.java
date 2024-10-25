package onl.oss.catalpa;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.stream.Stream;

public class Util {

    public enum OperationgSystem {
        Windows,
        MacOS,
        Linux,
        Unknown
    }

    private static Boolean isWindows;
    private static Boolean isMacOS;
    private static Boolean isLinux;

    private static Boolean isRunningAsUWP;
    private static Boolean isRunningAsSandbox;

    private static Path appDir;
    private static Path dataDir;

    public static OperationgSystem getOperatingSystem() {
        if (isWindows()) {
            return OperationgSystem.Windows;
        }
        if (isMacOS()) {
            return OperationgSystem.MacOS;
        }
        if (isLinux()) {
            return OperationgSystem.Linux;
        }
        return OperationgSystem.Unknown;
    }

    public static boolean isWindows() {
        if (isWindows == null) {
            isWindows = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
        }
        return isWindows;
    }

    public static boolean isMacOS() {
        if (isMacOS == null) {
            isMacOS = System.getProperty("os.name", "").toLowerCase().startsWith("mac");
        }
        return isMacOS;
    }

    public static boolean isLinux() {
        if (isLinux == null) {
            isLinux = System.getProperty("os.name", "").toLowerCase().startsWith("linux");
        }
        return isLinux;
    }

    public static boolean isRunningAsSandbox() {
        if (isRunningAsSandbox == null) {
            if (isMacOS()) {
                // ~/Library/Containers/onl.oss.catalpa/Data
                try {
                    Path userHome = Paths.get(System.getProperty("user.home", ""));
                    if (Files.isDirectory(userHome)) {
                        String current = userHome.getFileName().toString();
                        String parent = userHome.getParent().getFileName().toString();
                        String packageName = Util.class.getPackageName();
                        if (current.equalsIgnoreCase("Data") && parent.equalsIgnoreCase(packageName)) {
                            isRunningAsSandbox = true;
                        }
                    }
                } catch (Exception ignore) {}
            }
            if (isRunningAsSandbox == null) {
                isRunningAsSandbox = false;
            }
        }
        return isRunningAsSandbox;
    }

    public static int[] getApplicationVersion() {
        String s = System.getProperty("java.application.version");
        if (s == null || s.isBlank()) {
            String sv = Util.class.getPackage().getSpecificationVersion();
            if (sv != null) {
                if (sv.trim().matches("^(\\d+)(.(\\d+))*$")) {
                    s = sv.trim();
                }
            }
            if (s == null || s.isBlank()) {
                return null;
            }
        }

        s = s.trim() + ".0.0.0.0";
        String[] array = s.split("\\.", 5);
        int[] version = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                version[i] = Integer.parseInt(array[i]);
            } catch (NumberFormatException ignored) {}
        }
        return version;
    }

    public static Path getApplicationDirectory() {
        if (appDir == null) {
            appDir = getApplicationDirectory(Util.class);
        }
        return appDir;
    }

    public static Path getApplicationDirectory(Class<?> cls) {
        try {
            ProtectionDomain pd = cls.getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            URL location = cs.getLocation();
            URI uri = location.toURI();
            Path path = Paths.get(uri);
            // IntelliJで実行した場合にプロジェクトディレクトリが返されるように classes/java/main を遡ります。
            while(Files.isDirectory(path)) {
                if(!"classes/java/main/".contains(path.getFileName().toString() + "/")) {
                    break;
                }
                path = path.getParent();
            }
            return path.getParent().toRealPath();
        } catch (Exception e) {
            try {
                return Paths.get(".").toRealPath();
            } catch (IOException e1) {
                return new File(".").getAbsoluteFile().toPath();
            }
        }
    }

    public static Path getDataDirectory() throws IOException {
        if (dataDir == null) {
            if (isWindows()) {
                // Windows
                String s = System.getenv("LOCALAPPDATA");
                if(s == null) {
                    throw new IOException("LOCALAPPDATA not found");
                }
                Path path = Paths.get(s);
                if (!Files.isDirectory(path)) {
                    throw new IOException("LOCALAPPDATA not exists");
                }
                dataDir = path.resolve("catalpa");
            } else if (isRunningAsSandbox()) {
                // macOS Sandbox
                String userHome = System.getProperty("user.home");
                if (userHome == null) {
                    throw new IOException("user.home is empty");
                }
                Path userHomePath = Paths.get(userHome);
                if (!Files.isDirectory(userHomePath)) {
                    throw new IOException("user.home not exists");
                }
                dataDir = userHomePath.normalize();
            } else {
                // Other OS
                String userHome = System.getProperty("user.home");
                if (userHome == null) {
                    throw new IOException("user.home is empty");
                }
                Path userHomePath = Paths.get(userHome);
                if (!Files.isDirectory(userHomePath)) {
                    throw new IOException("user.home not exists");
                }
                Path path = userHomePath.resolve("Library").resolve("Application Support");
                if (Files.isDirectory(path)) {
                    dataDir = path.normalize().resolve("catalpa");
                } else {
                    dataDir = userHomePath.normalize().resolve(".catalpa");
                }
            }
        }
        return dataDir;
    }

    /** 指定したファイルの拡張子を小文字で返します。
     *
     * @param file 対象ファイルパス
     * @return 小文字化された拡張子（ドットは含みません）
     */
    public static String getFileExtension(Path file) {
        String filename = file.getFileName().toString();
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            return filename.substring(i + 1).toLowerCase();
        }
        return "";
    }

    /** 指定したファイルの拡張子でコンテンツファイルかどうかを判定します。
     * YAML または Markdown がコンテンツファイルに該当します。
     *
     * @param file 判定するファイルのパス
     * @return コンテンツファイルであれば true、そうでなければ false
     */
    public static boolean isContentFile(Path file) {
        return isMarkdownFile(file) || isYamlFile(file);
    }

    /** 指定したファイルの拡張子でYAMLファイルかどうかを判定します。
     *
     * @param file 判定するファイルのパス
     * @return YAMLファイルであれば true、そうでなければ false
     */
    public static boolean isYamlFile(Path file) {
        return Files.isRegularFile(file) && Arrays.asList("yml", "yaml").contains(getFileExtension(file));
    }

    /** 指定したファイルの拡張子でMarkdownファイルかどうかを判定します。
     *
     * @param file 判定するファイルのパス
     * @return Markdownファイルであれば true、そうでなければ false
     */
    public static boolean isMarkdownFile(Path file) {
        return Files.isRegularFile(file) && Arrays.asList("md", "markdown").contains(getFileExtension(file));
    }

    /** 指定したファイルがコンフィグ・ファイルかどうかを判定します。
     *
     * @param file 判定するファイルのパス
     * @return コンフィグ・ファイルであれば true、そうでなければ false
     */
    public static boolean isConfigFile(Path file) {
        return Files.isRegularFile(file) && file.getFileName().equals(Path.of("config.yml"));
    }

    /** 指定したファイルがCSSファイルかどうかを判定します。
     *
     * @param file 判定するファイルのパス
     * @return CSSファイブであれば true、そうでなければ false
     */
    public static boolean isCssFile(Path file) {
        return Files.isRegularFile(file) && "css".equals(getFileExtension(file));
    }

    /** 指定したファイルがテンプレート・ファイルかどうかを判定します。
     *
     * @param file 判定するファイルのパス
     * @return テンプレート・ファイルであれば true、そうでなければ false
     */
    public static boolean isTemplateFile(Path file) {
        return Files.isRegularFile(file) && "ftl".equals(getFileExtension(file));
    }

    /** 指定したパスが FreeMarker テンプレート・フォルダーかどうかを判定します。
     *
     * @param dir 判定するフォルダーのパス
     * @return FreeMarker テンプレート・フォルダーであれば true、そうでなければ false
     */
    public static boolean isTemplatesDirectory(Path dir) {
        return Files.isDirectory(dir) && dir.getFileName().equals(Path.of("templates"));
    }

    public static Path createTemporaryDirectory(String dir, boolean isDeleteIfExists) throws IOException {
        Path path = Path.of(System.getProperty("java.io.tmpdir"))
                .resolve(dir);

        if (isDeleteIfExists && Files.exists(path) && Files.isDirectory(path)) {
            deleteDirectory(path);
        }

        Files.createDirectories(path);
        return path;
    }

    /** 指定したディレクトリの削除を試みます。
     * 削除できなくても例外はスローされません。
     *
     * @param dir 削除対象のディレクトリ
     */
    public static void deleteDirectory(Path dir) {
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            return;
        }

        try (Stream<Path> stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                if (Files.isDirectory(path)) {
                    deleteDirectory(path);
                } else {
                    try {
                        Files.delete(path);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        try {
            Files.delete(dir);
        } catch (Exception ignored) {}
    }
}
