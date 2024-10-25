package onl.oss.catalpa;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class Logger {

    private static final String filename = "log4j2.xml";
    private static LoggerContext context;
    private static org.apache.logging.log4j.Logger logger;

    static {
        try {
            Path dir = Util.getDataDirectory();
            Path xml = dir.resolve(filename);

            // 開発環境から実行している場合のみコンソールへのログ出力を有効にします。
            // Gradle からプロジェクトを実行している場合はパスに /classes/java/main/ が含まれるので、これで開発環境で実行されているかを判定します。
            // 開発環境から実行している場合は log.appender システム・プロパティに "console" を設定します。
            // それ以外の場合は log.appender システム・プロパティに "null" が設定された状態のままになります。
            // log4j2.xml はアペンダーの名前として ${sys:log.appender} を参照するように構成されています。
            // "console" が設定されていれば console という名前のアペンダーが使用されます。"null" が設定されていれば null という名前のアペンダーが使用されます。
            System.setProperty("log.appender", "null");
            try {
                ProtectionDomain pd = Logger.class.getProtectionDomain();
                CodeSource cs = pd.getCodeSource();
                URL location = cs.getLocation();
                if (location.toString().contains("/classes/java/main/")) {
                    System.setProperty("log.appender", "console");
                }
            } catch (Exception ignored) {}

            // ログ・ディレクトリを作成します。
            // ログ・ディレクトリの位置を log.directory システム・プロパティに設定します。（これは log4j2.xml で参照されます）
            try {
                Path logdir = dir.resolve("log");
                if (Files.notExists(logdir)) {
                    Files.createDirectories(logdir);
                }
                System.setProperty("log.directory", logdir.toString());
            } catch (IOException ignored) {}

            // データ・ディレクトリに "log4j2.xml" が存在しない場合、
            // "resources/log4j2.xml" をデータ・ディレクトリにコピーします。
            if (Files.notExists(xml)) {
                try (InputStream in = Logger.class.getResourceAsStream("/" + filename)) {
                    if (in != null) {
                        Files.copy(in, xml);
                    }
                }
            }

            // データ・ディレクトリの "log4j2.xml" を参照してログ出力を構成します。
            try (InputStream in = Files.newInputStream(xml)) {
                ConfigurationSource source = new ConfigurationSource(in);
                context = Configurator.initialize(null, source);
            }

            logger = context.getLogger("");
            INFO(xml.toString());
        } catch (Exception ignored) {}
    }

    private static void setCaller() {
        String pkg = "";
        String cls = "";
        String method = "";
        String file = "";
        String line = "";
        String caller = "";
        StackTraceElement[] array = Thread.currentThread().getStackTrace();
        if (array.length > 3) {
            StackTraceElement e = array[3];
            cls = e.getClassName();
            int i = cls.lastIndexOf('.');
            if (i >= 0) {
                pkg = cls.substring(0, i);
                cls = cls.substring(i + 1);
            }
            method = e.getMethodName();
            file = e.getFileName();
            line = Integer.toString(e.getLineNumber());
            caller = "(" + file + ":" + line + ") " + method;
        }
        ThreadContext.put("package", pkg);
        ThreadContext.put("class", cls);
        ThreadContext.put("method", method);
        ThreadContext.put("file", file);
        ThreadContext.put("line", line);
        ThreadContext.put("caller", caller);
    }

    public static void shutdown() {
        if (context != null) {
            context.close();
        }
    }

    public static void TRACE(String message) {
        try {
            if (logger != null) {
                setCaller();
                logger.trace(message);
            }
        } catch (Throwable ignored) {}
    }

    public static void TRACE(Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.trace(throwable.getMessage(), throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void TRACE(String message, Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.trace(message, throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void DEBUG(String message) {
        try {
            if (logger != null) {
                setCaller();
                logger.debug(message);
            }
        } catch (Throwable ignored) {}
    }

    public static void DEBUG(Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.debug(throwable.getMessage(), throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void DEBUG(String message, Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.debug(message, throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void INFO(String message) {
        try {
            if (logger != null) {
                setCaller();
                logger.info(message);
            }
        } catch (Throwable ignored) {}
    }

    public static void INFO(Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.info(throwable.getMessage(), throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void INFO(String message, Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.info(message, throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void WARN(String message) {
        try {
            if (logger != null) {
                setCaller();
                logger.warn(message);
            }
        } catch (Throwable ignored) {}
    }

    public static void WARN(Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.warn(throwable.getMessage(), throwable);
            }
        } catch (Throwable ignored) {}

    }

    public static void WARN(String message, Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.warn(message, throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void ERROR(String message) {
        try {
            if (logger != null) {
                setCaller();
                logger.error(message);
            }
        } catch (Throwable ignored) {}
    }

    public static void ERROR(Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.error(throwable.getMessage(), throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void ERROR(String message, Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.error(message, throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void FATAL(String message) {
        try {
            if (logger != null) {
                setCaller();
                logger.fatal(message);
            }
        } catch (Throwable ignored) {}
    }

    public static void FATAL(Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.fatal(throwable.getMessage(), throwable);
            }
        } catch (Throwable ignored) {}
    }

    public static void FATAL(String message, Throwable throwable) {
        try {
            if (logger != null) {
                setCaller();
                logger.error(message, throwable);
            }
        } catch (Throwable ignored) {}
    }
}
