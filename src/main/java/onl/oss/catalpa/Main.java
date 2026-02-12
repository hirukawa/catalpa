package onl.oss.catalpa;

import javafx.application.Application;
import javafx.application.Platform;
import onl.oss.catalpa.gui.MainApp;
import onl.oss.util.prefs.FilePreferencesFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static onl.oss.catalpa.Logger.ERROR;
import static onl.oss.catalpa.Logger.INFO;
import static onl.oss.catalpa.Logger.WARN;

public class Main {

    public static final String APPLICATION_NAME;
    public static final String APPLICATION_VERSION;

    static {
        String version = "";
        int[] v = Util.getApplicationVersion();
        if (v != null) {
            version = String.format("%d.%d.%d.%d", v[0], v[1], v[2], v[3]);
            if (version.endsWith(".0")) {
                version = version.substring(0, version.length() - 2);
            }
        }

        APPLICATION_NAME = "Catalpa";
        APPLICATION_VERSION = version;
    }

    public static void main(String[] args) {

        // データディレクトリを作成します。
        Path dir;
        try {
            dir = Util.getDataDirectory();
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            // まだログ出力の設定が完了していないので、ここではログを出力できません。
            return;
        }

        INFO("プロセスが開始されました");

        // プロセスの二重起動チェック
        FileChannel fc = null;
        FileLock lock = null;

        try {
            // データディレクトリのルートに ".lock" ファイルを作成してロックします。
            INFO(".lockファイルをロックします");
            Path path = dir.resolve(".lock");
            fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);
            lock = fc.tryLock();
        } catch (IOException e) {
            ERROR(e);
        }

        if (lock == null) {
            INFO("プロセスはすでに起動されています（.lockファイルがロックされています）");
            return;
        }

        INFO(".lockファイルをロックしました");


        // Preferences を catalpa.preferences ファイルに保存するようにします。（既定では Preferences はレジストリに保存されます）
        try {
            FilePreferencesFactory.setFilePath(Util.getDataDirectory().resolve("catalpa.preferences"));
            System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
        } catch (IOException e) {
            ERROR(e);
        }

        // 画面の一部が再描画されずに白くなってしまうバグを回避するために、prism.dirtyopts=false を指定しています。
        System.setProperty("prism.dirtyopts", "false");

        // ユーザーのホームディレクトリに .accessibility.properties ファイルが配置されていて、
        // 以下のような内容が設定されていると AccessBridge クラスが見つからずにエラーになってしまいます。
        // assistive_technologies=com.sun.java.accessibility.AccessBridge
        // screen_magnifier_present=true
        // ユーザーのホームディレクトリの .accessibility.properties の影響を受けないように
        // 強制的に javax.accessibility.assistive_technologies を空にしています。
        System.setProperty("javax.accessibility.assistive_technologies", "");

        INFO("JavaFXスレッドを開始します");
        Platform.setImplicitExit(true);
        Application.launch(MainApp.class, args);

        try {
            INFO("非同期タスクが完了するまで最大5秒待ちます");
            if (MainApp.getExecutor().awaitTermination(5, TimeUnit.SECONDS)) {
                INFO("非同期タスクが5秒以内に完了しました");
            } else {
                WARN("非同期タスクが5秒以内に完了しませんでした");
            }
        } catch (InterruptedException e) {
            WARN(e);
        }

        // ロック解除
        try {
            INFO(".lockファイルのロックを解除します");
            lock.release();
            fc.close();
            INFO(".lockファイルのロックを解除しました");
        } catch (IOException e) {
            ERROR(e);
        }

        INFO("プロセスが終了します");
        Logger.shutdown();
        System.exit(0);
    }
}
