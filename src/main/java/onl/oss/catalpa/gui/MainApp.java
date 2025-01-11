package onl.oss.catalpa.gui;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import onl.oss.catalpa.CacheManager;
import onl.oss.catalpa.Generator;
import onl.oss.catalpa.GeneratorException;
import onl.oss.catalpa.Util;
import onl.oss.catalpa.Main;
import onl.oss.catalpa.blog.Blog;
import onl.oss.catalpa.model.Content;
import onl.oss.catalpa.model.Progress;
import onl.oss.catalpa.upload.UploadConfig;
import onl.oss.catalpa.upload.UploadConfigFactory;
import onl.oss.util.prefs.FilePreferences;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import static onl.oss.catalpa.Logger.ERROR;
import static onl.oss.catalpa.Logger.INFO;
import static onl.oss.catalpa.Logger.WARN;

public class MainApp extends Application {

    private static final int MAX_RECENT_FILES = 10;

    private static MainApp instance;
    private static final ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 2));

    public static MainApp getInstance() {
        return instance;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public String vsCodePath;

    private final FileWatchService fileWatchService = new FileWatchService(this::fileWatchService_onChanged);
    private final Preferences preferences = Preferences.userRoot();
    private Stage primaryStage;
    private MainView mainView;

    private Path inputPath;
    private Path outputPath;
    private Path temporaryPath;
    private Content blogConfig;
    private UploadConfig uploadConfig;
    private LocalHttpServer httpServer;
    private boolean isGenerating;
    private boolean isDirty;
    private Path errorPath;
    private Throwable errorThrowable;
    private TranslateTransition messageAnimation = new TranslateTransition();

    public MainApp() {
        instance = this;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        INFO("JavaFXスレッドを開始しました");

        this.primaryStage = primaryStage;

        Thread.setDefaultUncaughtExceptionHandler(this::onCaughtException);

        try (InputStream is = getClass().getResourceAsStream("/img/app-icon-64px.png")) {
            if (is != null) {
                primaryStage.getIcons().add(new Image(is));
            }
        } catch (Exception ignored) {}

        primaryStage.setTitle(Main.APPLICATION_NAME + " " + Main.APPLICATION_VERSION);

        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreen(false);

        double x = preferences.getDouble("X", Double.NaN);
        double y = preferences.getDouble("Y", Double.NaN);
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            primaryStage.setX(x);
            primaryStage.setY(y);
        }

        mainView = new MainView();
        mainView.cbAutoReload.setOnAction(this::cbAutoReload_onAction);
        mainView.btnOpen.setOnAction(this::btnOpen_onAction);
        mainView.btnReload.setOnAction(this::btnReload_onAction);
        mainView.btnOpenBrowser.setOnAction(this::btnOpenBrowser_onAction);
        mainView.btnSaveAs.setOnAction(this::btnSaveAs_onAction);
        mainView.btnUpload.setOnAction(this::btnUpload_onAction);
        mainView.lblNewPost.setOnMouseClicked(this::lblNewPost_onMouseClicked);
        mainView.lblVSCode.setOnMouseClicked(this::lblVSCode_onMouseClicked);
        mainView.lblCheatSheet.setOnMouseClicked(this::lblCheatSheet_onMouseClicked);
        mainView.menuFileOpen.setOnAction(this::btnOpen_onAction);
        mainView.menuFileSaveAs.setOnAction(this::btnSaveAs_onAction);
        mainView.menuHelpAbout.setOnAction(this::menuHelpAbout_onAction);
        mainView.menuFileExit.setOnAction(this::menuFileExit_onAction);
        mainView.lblMessage.setOnMouseClicked(this::lblMessage_onMouseClicked);
        mainView.btnMessageClose.setOnAction(this::btnMessageClose_onAction);
        mainView.lblError.setOnMouseClicked(this::lblError_onMouseClicked);
        mainView.btnErrorClose.setOnAction(this::btnErrorClose_onAction);

        // ツールチップ（新しいブログ記事の作成）
        Tooltip ttNewPost = new Tooltip("新しいブログ記事を作成します");
        ttNewPost.setShowDelay(Duration.millis(200));
        mainView.lblNewPost.setTooltip(ttNewPost);

        // ツールチップ（VSCode）
        Tooltip ttVSCode = new Tooltip("Visual Studio Code\u2005でフォルダーを開きます");
        ttVSCode.setShowDelay(Duration.millis(200));
        mainView.lblVSCode.setTooltip(ttVSCode);

        // ツールチップ（早見表）
        Tooltip ttCheatSheet = new Tooltip("Markdown\u2005早見表をブラウザーで表示します");
        ttCheatSheet.setShowDelay(Duration.millis(200));
        mainView.lblCheatSheet.setTooltip(ttCheatSheet);

        // デスクトップ機能の BROWSE がサポートされている場合、チートシートアイコンを有効にします。
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            mainView.lblCheatSheet.setDisable(false);
        }

        mainView.cbAutoReload.setSelected(preferences.getBoolean("AutoReload", false));

        List<String> recentFiles = FilePreferences.toList(preferences.get("RecentFiles", null));
        updateRecentFiles(recentFiles);

        Scene scene = new Scene(mainView);
        scene.setOnDragOver(this::scene_onDragOver);
        scene.setOnDragDropped(this::scene_onDragDropped);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.sizeToScene();

        temporaryPath = Path.of(System.getProperty("java.io.tmpdir")).resolve("catalpa.tmp");

        //
        // HTTPサーバーを起動します。
        //
        executor.execute(() -> {
            try {
                Path previewPath = temporaryPath.resolve("preview");
                try {
                    Util.deleteDirectory(previewPath);
                } catch (Exception ignored) {}

                Files.createDirectories(previewPath);
                httpServer = new LocalHttpServer(previewPath);
                httpServer.start();

                // VSCode 実行ファイルのパスを検索します。
                String s = Win32.findPath("code.cmd");
                if (s != null) {
                    Platform.runLater(() -> {
                        vsCodePath = s;
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        INFO("JavaFXスレッドが終了します");

        preferences.putDouble("X", primaryStage.getX());
        preferences.putDouble("Y", primaryStage.getY());

        // HTTPサーバーを停止します。
        if (httpServer != null) {
            httpServer.stop();
        }

        // 新たな非同期タスクを追加できないようにします。
        executor.shutdown();

        super.stop();
    }

    private void onCaughtException(Thread thread, Throwable exception) {
        Path path = null;
        if (exception instanceof GeneratorException) {
            path = ((GeneratorException)exception).getPath();
        }

        // GeneratorException, RuntimeException でラップされていて、既定のメッセージを持っている場合は展開します。
        // メッセージを指定せずに RuntimeException でラップした場合には "package.ExceptionClassName: message" となります。
        for (;;) {
            Throwable cause = exception.getCause();
            if (cause != null && (cause.getClass().getName() + ": " + cause.getMessage()).equals(exception.getMessage())) {
                Class<? extends Throwable> cls = exception.getClass();
                if (cls == GeneratorException.class || cls == RuntimeException.class) {
                    exception = cause;
                    continue;
                }
            }
            break;
        }

        ERROR("thread=" + thread.getName() + ": " + exception.getMessage(), exception);

        String message = exception.getMessage();
        if (message == null) {
            message = "エラーが発生しました";
        } else {
            // 例外メッセージが複数行ある場合は先頭行のみをラベルに表示します。
            // （メッセージ全文とスタックトレースはダイアログで確認できます）
            message = message.trim();
            int i = message.indexOf('\n');
            if (i >= 0) {
                message = message.substring(0, i);
            }
            message = message.trim();

            // 例外クラス名の部分を取り除き、メッセージ部分のみを表示します。
            int j = message.indexOf("Exception: ");
            if (j > 0) {
                String s = message.substring(j + "Exception: ".length());
                if (!s.isBlank()) {
                    message = s;
                }
            }

            // メッセージ名の先頭に例外クラス名（パッケージ名を除く）を付加します。
            message = exception.getClass().getSimpleName() + ": " + message;
        }

        showError(message, exception, path);
    }

    private void fileWatchService_onChanged(List<Path> list) {
        // このメソッドはワーカースレッドからコールバックされます。

        // フォルダーと desktop.ini は無視します。
        for (int i = list.size() - 1; i >= 0; i--) {
            Path path = list.get(i);
            if (Files.isDirectory(path)) {
                list.remove(i);
            } else {
                String filename = path.getFileName().toString();
                if (filename.equalsIgnoreCase("desktop.ini")) {
                    list.remove(i);
                }
            }
        }

        if (list.isEmpty()) {
            return;
        }

        Platform.runLater(() -> {
            INFO("ファイルの更新を検出しました: " + list);

            if (!mainView.cbAutoReload.isSelected()) {
                INFO("ファイル監視が有効になっていません");
                return;
            }

            if (inputPath == null) {
                INFO("入力フォルダーが指定されていません");
                return;
            }

            if (!inputPath.equals(fileWatchService.getPath())) {
                INFO("入力フォルダーと監視対象フォルダーが一致していません: inputPath=" + inputPath + ", fileWatchService.getPath()=" + fileWatchService.getPath());
                return;
            }

            boolean update = false;
            PATH_LIST:
            for (Path path : list) {
                // フォルダー名の先頭が _ で始まっている場合はファイル更新が検出されても無視します。
                Path dir = inputPath.relativize(path);
                while (dir != null) {
                    if (dir.getFileName().toString().startsWith("_")) {
                        INFO("除外フォルダー内のファイルです: " + path);
                        continue PATH_LIST;
                    }
                    dir = dir.getParent();
                }

                if (outputPath != null && path.startsWith(outputPath)) {
                    // 出力中（出力パスが存在する）に、出力パスのファイル更新が検出されても無視します。
                    INFO("出力フォルダー内のファイルです: " + path);
                    continue;
                } else if (Util.isTemplateFile(path) || Util.isCssFile(path)) {
                    // ftl または css が更新された場合はキャッシュをクリアして、コンテンツが再作成されるようにします。
                    INFO(Util.getFileExtension(path) + " ファイルが更新されたためキャッシュとテンポラリをクリアします");
                    Util.deleteDirectory(temporaryPath.resolve("preview"));
                    CacheManager.clear();
                    update = true;
                    break;
                } else {
                    update = true;
                }
            }

            if (update) {
                try {
                    INFO("更新します");
                    update();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void scene_onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            List<File> files = event.getDragboard().getFiles();
            if (files != null && files.size() == 1 && files.getFirst().isDirectory()) {
                event.acceptTransferModes(TransferMode.COPY);
                return;
            }
        }
        event.consume();
    }

    private void scene_onDragDropped(DragEvent event) {
        boolean isTransferDone = false;
        if (event.getDragboard().hasFiles()) {
            List<File> files = event.getDragboard().getFiles();
            if (files != null && files.size() == 1 && files.getFirst().isDirectory()) {
                isTransferDone = true;
                Path path = files.getFirst().toPath();
                try {
                    Path inputPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
                    open(inputPath);
                } catch (IOException | InterruptedException e) {
                    ERROR(e);
                }
            }
        }
        event.setDropCompleted(isTransferDone);
        event.consume();
    }

    private void lblNewPost_onMouseClicked(MouseEvent event) {
        if (inputPath == null) {
            return;
        }

        if (Files.notExists(inputPath)) {
            return;
        }

        if (!Files.isDirectory(inputPath)) {
            return;
        }

        try {
            blogConfig = Blog.findConfig(this.inputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (blogConfig == null) {
            return;
        }

        BlogWizard dialog = new BlogWizard(primaryStage, inputPath, blogConfig);
        dialog.showAndWait();
    }

    private void lblVSCode_onMouseClicked(MouseEvent event) {
        if (inputPath == null) {
            return;
        }

        if (Files.notExists(inputPath)) {
            return;
        }

        if (!Files.isDirectory(inputPath)) {
            return;
        }

        try {
            String directory = inputPath.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
            ProcessBuilder pb = new ProcessBuilder(vsCodePath, directory);
            pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void lblCheatSheet_onMouseClicked(MouseEvent event) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return;
        }

        try {
            desktop.browse(URI.create("https://catalpa.oss.onl/markdown.html"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void menuHelpAbout_onAction(ActionEvent event) {
        try {
            Path path = Util.getApplicationDirectory().resolve("LICENSE.txt");
            String license = Files.readString(path, StandardCharsets.UTF_8);
            LicenseDialog dialog = new LicenseDialog(primaryStage, license);
            dialog.showAndWait();
        } catch (Exception e) {
            ERROR(e);
        }
    }

    private void menuFileExit_onAction(ActionEvent event) {
        primaryStage.close();
    }

    private void menuFileRecentFile_onAction(ActionEvent event) {
        Path path = null;
        if (event.getSource() instanceof MenuItem item) {
            path = Path.of(item.getText());
        }

        INFO("path=" + path);
        try {
            open(path);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void btnOpen_onAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("フォルダーを開く");

        String s = preferences.get("LastOpenDirectories", null);
        if (s != null) {
            File lastOpenDirectory = new File(s);
            if (lastOpenDirectory.exists() && lastOpenDirectory.isDirectory()) {
                dc.setInitialDirectory(lastOpenDirectory);
            }
        }

        File dir = dc.showDialog(primaryStage);
        if (dir != null) {
            try {
                open(dir.toPath());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            preferences.put("LastOpenDirectories", dir.getAbsolutePath());
        }
    }

    private void btnReload_onAction(ActionEvent event) {
        INFO("btnReload_onAction");

        try {
            // 更新ボタンを押下した場合は、キャッシュとプレビュー用テンポラリをクリアしてから更新します。
            Util.deleteDirectory(temporaryPath.resolve("preview"));
            CacheManager.clear();

            update();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cbAutoReload_onAction(ActionEvent event) {
        INFO("isSelected=" + mainView.cbAutoReload.isSelected());

        preferences.putBoolean("AutoReload", mainView.cbAutoReload.isSelected());

        INFO("fileWatchService.stop()");
        try {
            fileWatchService.stop();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (inputPath == null) {
            INFO("inputPath=null");
            return;
        }

        if (Files.notExists(inputPath)) {
            INFO("Files.notExists(\"" + inputPath + "\") -> true");
            return;
        }

        if (!Files.isDirectory(inputPath)) {
            INFO("Files.isDirectory(\"" + inputPath + "\") -> false");
            return;
        }

        if (!mainView.cbAutoReload.isSelected()) {
            INFO("cbAutoReload.isSelected=false");
            return;
        }

        INFO("fileWatchService.start(\"" + inputPath + "\")");
        try {
            fileWatchService.start(inputPath);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void btnOpenBrowser_onAction(ActionEvent event) {
        if (httpServer == null) {
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            int port = httpServer.getAddress().getPort();
            String path = "";
            URI uri = URI.create("http://localhost:" + port + "/" + path);
            try {
                desktop.browse(uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void btnSaveAs_onAction(ActionEvent event) {
        if (inputPath == null || Files.notExists(inputPath) || !Files.isDirectory(inputPath)) {
            return;
        }

        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("名前を付けて保存");

        Map<String, String> map = FilePreferences.toMap(preferences.get("LastSaveDirectory", null));
        String key = inputPath.toString().toLowerCase();
        String value = map.get(key);
        if (value != null) {
            File lastSaveDirectory = new File(value);
            while (lastSaveDirectory != null && lastSaveDirectory.toString().length() >= 4) {
                if (lastSaveDirectory.exists() && lastSaveDirectory.isDirectory()) {
                    dc.setInitialDirectory(lastSaveDirectory);
                    break;
                }
                lastSaveDirectory = lastSaveDirectory.getParentFile();
            }
        } else {
            File defaultDirectory = inputPath.resolve("htdocs").toFile();
            if (defaultDirectory.exists() && defaultDirectory.isDirectory()) {
                dc.setInitialDirectory(defaultDirectory);
            }
        }

        File dir = dc.showDialog(primaryStage);
        if (dir != null) {
            map.remove(key);
            if (map.size() >= MAX_RECENT_FILES) {
                String firstKey = map.entrySet().iterator().next().getKey();
                map.remove(firstKey);
            }
            map.put(key, dir.getAbsolutePath());
            preferences.put("LastSaveDirectory", FilePreferences.toString(map));

            save(inputPath, dir.toPath());
        }
    }

    private void btnUpload_onAction(ActionEvent event) {
        INFO("upload");

        upload(inputPath);
    }

    private void lblMessage_onMouseClicked(MouseEvent event) {
        hideMessage();
    }

    private void btnMessageClose_onAction(ActionEvent event) {
        hideMessage();
    }

    private void lblError_onMouseClicked(MouseEvent event) {
        if (errorThrowable != null) {
            ExceptionDialog dialog = new ExceptionDialog(primaryStage, errorPath, errorThrowable);
            dialog.showAndWait();
        }
    }

    private void btnErrorClose_onAction(ActionEvent event) {
        hideError();
    }

    private void showMessage(String message) {
        Runnable runnable = () -> {
            messageAnimation.stop();

            mainView.lblMessage.setText(message);
            mainView.message.setTranslateY(0.0);
            mainView.message.setVisible(true);

            messageAnimation.setNode(mainView.message);
            messageAnimation.setDelay(Duration.millis(3000));
            messageAnimation.setDuration(Duration.millis(500));
            messageAnimation.setInterpolator(Interpolator.EASE_OUT);
            messageAnimation.setToY(mainView.message.getHeight());
            messageAnimation.play();
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void hideMessage() {
        Runnable runnable = () -> {
            mainView.lblMessage.setText("");
            mainView.message.setVisible(false);
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void showError(String message, Throwable throwable, Path path) {
        Runnable runnable = () -> {
            errorThrowable = throwable;
            errorPath = path;
            mainView.lblError.setText(message);
            mainView.error.setVisible(true);
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void hideError() {
        Runnable runnable = () -> {
            errorThrowable = null;
            errorPath = null;
            mainView.lblError.setText("");
            mainView.error.setVisible(false);
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void updateRecentFiles(List<String> recentFiles) {
        INFO("updateRecentFiles");

        for (int i = mainView.menuFile.getItems().size() - 1; i >= 0; i--) {
            MenuItem item = mainView.menuFile.getItems().get(i);
            if ("RECENT_FILES".equals(item.getUserData())) {
                mainView.menuFile.getItems().remove(i);
            }
        }

        int i = mainView.menuFile.getItems().size() - 1;
        for (String file : recentFiles) {
            MenuItem item = new MenuItem(file);
            item.setOnAction(this::menuFileRecentFile_onAction);
            item.setUserData("RECENT_FILES");
            mainView.menuFile.getItems().add(i++, item);
        }

        if (!recentFiles.isEmpty()) {
            MenuItem separator = new SeparatorMenuItem();
            separator.setUserData("RECENT_FILES");
            mainView.menuFile.getItems().add(i, separator);
        }
    }

    private void open(Path inputPath) throws IOException, InterruptedException {
        INFO("inputPath=" + inputPath);

        if (inputPath == null || Files.notExists(inputPath) || !Files.isDirectory(inputPath)) {
            this.inputPath = null;
            return;
        }

        this.inputPath = inputPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
        this.uploadConfig = null;

        primaryStage.setTitle(this.inputPath.getFileName() + " - " + Main.APPLICATION_NAME + " " + Main.APPLICATION_VERSION);

        mainView.tfInputPath.setText(this.inputPath.toString());

        if (mainView.cbAutoReload.isSelected()) {
            INFO("fileWatchService.start(inputPath=\"" + this.inputPath + "\")");
            fileWatchService.start(this.inputPath);
        }

        blogConfig = Blog.findConfig(this.inputPath);
        mainView.lblNewPost.setDisable(blogConfig == null);

        if (vsCodePath != null) {
            mainView.lblVSCode.setDisable(false);
        }

        mainView.btnReload.setDisable(false);
        mainView.cbAutoReload.setDisable(false);
        mainView.btnSaveAs.setDisable(false);
        mainView.btnUpload.setDisable(true);
        hideMessage();
        hideError();

        List<String> recentFiles = FilePreferences.toList(preferences.get("RecentFiles", null));
        String filepath = this.inputPath.toString();
        for (int i = recentFiles.size() - 1; i >= 0; i--) {
            if (filepath.equalsIgnoreCase(recentFiles.get(i))) {
                recentFiles.remove(i);
            }
        }

        while (recentFiles.size() >= MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }

        recentFiles.addFirst(filepath);
        preferences.put("RecentFiles", FilePreferences.toString(recentFiles));
        updateRecentFiles(recentFiles);

        // フォルダーを開いたときに、キャッシュとプレビュー用テンポラリをクリアしてから更新します。
        Util.deleteDirectory(temporaryPath.resolve("preview"));
        CacheManager.clear();

        update();

        mainView.btnOpenBrowser.setDisable(false);
    }

    private void save(Path inputPath, Path outputPath) {
        if (isGenerating) {
            return;
        }

        isGenerating = true;

        this.outputPath = outputPath;

        mainView.body.setDisable(true);
        mainView.progressBar.setProgress(0.0);
        mainView.lblProgress.setText("");
        mainView.progress.setVisible(true);
        hideMessage();
        hideError();

        executor.execute(() -> {
            try {
                // 名前を付けて保存を選択したときに、ユーザーのフォルダーを勝手に削除してはいけない！
                // Util.deleteDirectory(outputPath);
                Files.createDirectories(outputPath);

                Map<String, Object> systemDataModel = new HashMap<>();
                systemDataModel.put("_PREVIEW", false);
                Generator generator = new Generator(inputPath, outputPath, systemDataModel, this::updateProgress);
                generator.generate();
                showMessage("保存しました: " + outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                Platform.runLater(() -> {
                    this.outputPath = null;
                    mainView.progress.setVisible(false);
                    mainView.body.setDisable(false);
                    isGenerating = false;
                });
            }
        });
    }

    private void update() throws IOException {
        if (isGenerating) {
            isDirty = true;
            return;
        }

        isGenerating = true;

        outputPath = temporaryPath.resolve("preview");

        mainView.body.setDisable(true);
        mainView.progressBar.setProgress(0.0);
        mainView.lblProgress.setText("");
        mainView.progress.setVisible(true);
        hideMessage();
        hideError();

        Path mydataPath = Util.getApplicationDirectory().resolve("mydata");
        uploadConfig = new UploadConfigFactory().create(inputPath, mydataPath);
        if (uploadConfig != null) {
            mainView.btnUpload.setDisable(false);
        }

        executor.execute(() -> {
            try {
                long start = System.nanoTime();
                Files.createDirectories(outputPath);

                Map<String, Object> systemDataModel = new HashMap<>();
                systemDataModel.put("_PREVIEW", true);
                Generator generator = new Generator(inputPath, outputPath, systemDataModel, this::updateProgress);
                generator.generate();

                long end = System.nanoTime();
                double time = (end - start) / 1000000000d;
                showMessage("更新処理が完了しました（" + String.format("%,.2f", time) + "秒）");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                Platform.runLater(() -> {
                    this.outputPath = null;
                    mainView.progress.setVisible(false);
                    mainView.body.setDisable(false);
                    isGenerating = false;

                    if (httpServer != null) {
                        httpServer.update(this.inputPath);
                    }

                    if (isDirty) {
                        isDirty = false;
                        try {
                            update();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    private void updateProgress(Progress progress) {
        // このメソッドはワーカスレッドからコールバックされます。
        Platform.runLater(() -> {
            mainView.progressBar.setProgress(progress.value);
            String text = "";
            if (progress.path != null) {
                text = inputPath.relativize(progress.path).toString();
            }
            mainView.lblProgress.setText(text);
        });
    }

    private void upload(Path inputPath) {
        if (uploadConfig == null) {
            return;
        }

        if (isGenerating) {
            isDirty = true;
            return;
        }

        isGenerating = true;

        outputPath = temporaryPath.resolve("upload");

        mainView.body.setDisable(true);
        mainView.progressBar.setProgress(0.0);
        mainView.lblProgress.setText("");
        mainView.progress.setVisible(true);
        hideMessage();
        hideError();

        executor.execute(() -> {
            try {
                // アップロード用フォルダーを削除して再作成します。
                Util.deleteDirectory(outputPath);
                Files.createDirectories(outputPath);

                Map<String, Object> systemDataModel = new HashMap<>();
                systemDataModel.put("_PREVIEW", false);
                Generator generator = new Generator(inputPath, outputPath, systemDataModel, this::uploadProgressPhase1);
                generator.generate();

                int uploadCount = uploadConfig.upload(outputPath, MainApp.this::uploadProgressPhase2);
                INFO("uploadCount=" + uploadCount);
                if (uploadCount == 0) {
                    showMessage("アップロードが完了しました（更新はありません）");
                } else {
                    showMessage("アップロードが完了しました（更新 " + uploadCount + " 件）");
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                Platform.runLater(() -> {
                    this.outputPath = null;
                    mainView.progress.setVisible(false);
                    mainView.body.setDisable(false);
                    isGenerating = false;

                    if (httpServer != null) {
                        httpServer.update(this.inputPath);
                    }

                    if (isDirty) {
                        isDirty = false;
                        try {
                            update();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    private void uploadProgressPhase1(Progress progress) {
        INFO(progress.toString());
        // このメソッドはワーカスレッドからコールバックされます。
        // アップロード前のHTML生成フェーズです。全体進捗の 0% ～ 50% までとします。（progress.value = 0.0 で 0%、progress.value = 1.0 で 50%）
        Platform.runLater(() -> {
            mainView.progressBar.setProgress(progress.value * 0.5);
            String text = "";
            if (progress.path != null) {
                text = inputPath.relativize(progress.path).toString();
            }
            mainView.lblProgress.setText(text);
        });
    }

    private void uploadProgressPhase2(Progress progress) {
        INFO(progress.toString());
        // このメソッドはワーカスレッドからコールバックされます。
        // HTML生成後のアップロードフェーズです。全体進捗の 50% ～ 100% までとします。（progress.value = 0.0 で 50%、progress.value = 1.0 で 100%）
        Platform.runLater(() -> {
            mainView.progressBar.setProgress(0.5 + progress.value * 0.5);

            String message = null;
            if (progress.message != null && !progress.message.isEmpty()) {
                message = progress.message;
            }

            String path = null;
            if (progress.path != null) {
                path = progress.path.toString();
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                path = path.replace('\\', '/');
            }

            String text = "";
            if (message != null && path != null) {
                text = message + " " + path;
            } else if (message != null) {
                text = message;
            } else if (path != null) {
                text = path;
            }

            mainView.lblProgress.setText(text);
        });
    }
}
