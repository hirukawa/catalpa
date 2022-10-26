package net.osdn.catalpa.ui.javafx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import freemarker.template.TemplateException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.Util;
import net.osdn.catalpa.upload.UploadConfig;
import net.osdn.catalpa.upload.UploadConfigFactory;
import net.osdn.util.javafx.application.FxApplicationThread;
import net.osdn.util.javafx.application.SingletonApplication;
import net.osdn.util.javafx.concurrent.Async;
import net.osdn.util.javafx.event.SilentEventHandler;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.stage.StageUtil;

public class MainApp extends SingletonApplication implements Initializable, ProgressObserver {

	public static final String APPLICATION_NAME = "Catalpa";
	public static final String APPLICATION_VERSION;

	private static final String MARKDOWN_CHEAT_SHEET_URL = "https://catalpa.oss.onl/markdown.html";

	static {
		System.setProperty("org.freemarker.loggerLibrary", "none");

		int[] version = Datastore.getApplicationVersion();
		if(version != null) {
			if (version[2] == 0) {
				APPLICATION_VERSION = String.format("%d.%d", version[0], version[1]);
			} else {
				APPLICATION_VERSION = String.format("%d.%d.%d", version[0], version[1], version[2]);
			}
		} else {
			APPLICATION_VERSION = "";
		}
	}

	private int MAX_RECENT_FILES = 10;
	private int HTTP_SERVER_PORT = 4000;
	private int HTTP_SERVER_PORT_MAX = HTTP_SERVER_PORT + 5;

	private Preferences preferences = Preferences.userNodeForPackage(getClass());
	private UploadConfigFactory uploadConfigFactory;
	private FileWatchService fileWatchService;
	private LocalDateTime lastModified;
	private HttpServer httpServer;
	private double progressOffset = 0.0;
	private double progressScale = 1.0;

	public MainApp() throws IOException {
		uploadConfigFactory = new UploadConfigFactory();
		
		fileWatchService = new FileWatchService();
		fileWatchService.setOnSucceeded(wrap(this::fileWatchService_onSucceeded));

		while(HTTP_SERVER_PORT <= HTTP_SERVER_PORT_MAX) {
			try {
				httpServer = new HttpServer(HTTP_SERVER_PORT);
				httpServer.start();
				break;
			} catch(BindException e) {
				if(++HTTP_SERVER_PORT > HTTP_SERVER_PORT_MAX) {
					throw e;
				}
			}
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/app-icon-48px.png")));
		primaryStage.titleProperty().bind(new StringBinding() {
			{
				bind(inputPath);
			}
			@Override
			protected String computeValue() {
				return (inputPath.get() != null ? inputPath.get().getFileName() + " - " : "")
							+ APPLICATION_NAME + " " + APPLICATION_VERSION;
			}
		});

		Parent root = Fxml.load(this);

		Scene scene = new Scene(root);
		scene.setOnDragOver(wrap(this::scene_onDragOver));
		scene.setOnDragDropped(wrap(this::scene_onDragDropped));

		StageUtil.setRestorable(primaryStage, Preferences.userNodeForPackage(getClass()));

		primaryStage.showingProperty().addListener((observable, oldValue, newValue) -> {
			if(oldValue == true && newValue == false) {
				Platform.exit();
			}
		});

		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
		primaryStage.sizeToScene();

		Thread.currentThread().setUncaughtExceptionHandler(handler);
	}

	protected Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if(e instanceof Exception) {
				showException((Exception)e);
			} else if(Thread.getDefaultUncaughtExceptionHandler() != null) {
				Thread.getDefaultUncaughtExceptionHandler().uncaughtException(t, e);
			} else {
				e.printStackTrace();
			}
		}
	};

	protected void showException(Exception e) {
		e.printStackTrace();

		Platform.runLater(()-> {
			String title = e.getClass().getName();
			String message = e.getLocalizedMessage();

			if(e instanceof ToastMessage) {
				title = ((ToastMessage)e).getTitle();
			}

			if(e instanceof TemplateException) {
				int i = message.indexOf("\n----\n");
				if(i >= 0) {
					message = message.substring(0, i).trim();
				}
			}

			toast.show(Toast.RED, title, message, null);
		});
	}


	/////
	/////
	/////
	private ObjectProperty<Path> inputPath = new SimpleObjectProperty<Path>();
	private ObjectProperty<Path> previewOutputPath = new SimpleObjectProperty<Path>();
	private ObjectProperty<UploadConfig> uploadConfig = new SimpleObjectProperty<UploadConfig>();
	private BooleanProperty busy = new SimpleBooleanProperty();
	private BooleanProperty dirty = new SimpleBooleanProperty();
	private BooleanProperty draft = new SimpleBooleanProperty();
	private StringProperty defaultUrl = new SimpleStringProperty();

	@FXML MenuBar   menuBar;
	@FXML Menu      menuFile;
	@FXML MenuItem  menuFileOpen;
	@FXML MenuItem  menuFileSaveAs;
	@FXML MenuItem  menuFileExit;
	@FXML MenuItem  menuHelpAbout;
	@FXML Label     lblCheatSheet;
	@FXML Label     lblVSCode;
	@FXML Node      body;
	@FXML TextField tfInputPath;
	@FXML Button    btnOpen;
	@FXML Button    btnReload;
	@FXML CheckBox  cbAutoReload;
	@FXML Button    btnOpenBrowser;
	@FXML Button    btnSaveAs;
	@FXML Button    btnUpload;
	@FXML Pane      blocker;
	@FXML Toast     toast;
	@FXML ProgressBar progressBar;
	@FXML Label       progressLabel;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		menuFileOpen.setOnAction(wrap(this::btnOpen_onAction));
		menuFileSaveAs.setOnAction(wrap(this::btnSaveAs_onAction));
		menuFileExit.setOnAction(wrap(this::menuFileExit_onAction));
		menuHelpAbout.setOnAction(wrap(this::menuHelpAbout_onAction));
		lblVSCode.setOnMouseClicked(wrap(this::lblVSCode_onMouseClicked));
		lblCheatSheet.setOnMouseClicked(wrap(this::lblCheatSheet_onMouseClicked));
		btnOpen.setOnAction(wrap(this::btnOpen_onAction));
		btnReload.setOnAction(wrap(this::btnReload_onAction));
		btnOpenBrowser.setOnAction(wrap(this::btnOpenBrowser_onAction));
		btnSaveAs.setOnAction(wrap(this::btnSaveAs_onAction));
		btnUpload.setOnAction(wrap(this::btnUpload_onAction));

		cbAutoReload.setSelected(preferences.getBoolean("isAutoReload", false));
		cbAutoReload.selectedProperty().addListener((observable, oldValue, newValue)-> {
			preferences.putBoolean("isAutoReload", newValue);
		});

		menuBar.disableProperty().bind(busy);
		menuFileSaveAs.disableProperty().bind(Bindings.isNull(inputPath));

		Tooltip ttCheatSheet = new Tooltip("Markdown早見表をブラウザーで表示します。");
		ttCheatSheet.setFont(Font.font("Meiryo", 12.0));
		ttCheatSheet.setShowDelay(Duration.millis(200));
		lblCheatSheet.setTooltip(ttCheatSheet);

		Tooltip ttVSCode = new Tooltip("Visual Studio Codeでフォルダーを開きます。");
		ttVSCode.setFont(Font.font("Meiryo", 12.0));
		ttVSCode.setShowDelay(Duration.millis(200));
		lblVSCode.setTooltip(ttVSCode);
		lblVSCode.disableProperty().bind(inputPath.isNull().or(new SimpleBooleanProperty(!VSCode.isInstalled())));

		body.disableProperty().bind(busy);
		body.disabledProperty().addListener((observable, oldValue, newValue)-> {
			if(newValue == false && !btnReload.isDisabled()) {
				btnReload.requestFocus();
			}
		});

		tfInputPath.textProperty().addListener((observable, oldValue, newValue)-> {
			inputPath.setValue(null);
		});
		inputPath.addListener((observable, oldValue, newValue)-> {
			uploadConfig.setValue(null);
		});

		btnReload.disableProperty().bind(Bindings.createBooleanBinding(()-> {
			return tfInputPath.getText().length() == 0 || !Files.isDirectory(Paths.get(tfInputPath.getText()));
		}, tfInputPath.textProperty()));
		cbAutoReload.disableProperty().bind(Bindings.isNull(inputPath));

		btnSaveAs.disableProperty().bind(Bindings.isNull(inputPath));
		btnOpenBrowser.disableProperty().bind(Bindings
				.or(Bindings.isNull(inputPath), Bindings.isNull(previewOutputPath)));
		btnUpload.disableProperty().bind(Bindings
				.or(Bindings.isNull(uploadConfig), draft));

		blocker.visibleProperty().bind(busy);
		progressBar.visibleProperty().bind(Bindings.lessThan(0.0, progressBar.progressProperty()));
		progressLabel.visibleProperty().bind(progressBar.visibleProperty());

		fileWatchService.pathProperty().bind(Bindings
				.when(cbAutoReload.selectedProperty())
				.then(inputPath)
				.otherwise((Path)null));

		busy.addListener(wrap(this::busy_onChange));

		updateRecentFile(null);
	}

	protected void menuFileExit_onAction(ActionEvent event) {
		Platform.exit();
	}

	void menuHelpAbout_onAction(ActionEvent event) throws IOException {
		String license = Datastore.getLicense();
		LicenseDialog dialog = new LicenseDialog(getPrimaryStage(),
				APPLICATION_NAME + " " + APPLICATION_VERSION, license);
		dialog.showAndWait();
	}

	protected void btnOpen_onAction(ActionEvent event) throws IOException {
		toast.hide();

		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("フォルダーを開く");

		String s = preferences.get("lastOpenFolder", null);
		if(s != null) {
			File lastOpenDirectory = new File(s);
			if(lastOpenDirectory.exists() && lastOpenDirectory.isDirectory()) {
				dc.setInitialDirectory(lastOpenDirectory);
			}
		}
		File dir = dc.showDialog(getPrimaryStage());
		if(dir != null) {
			if(prepareOpening(dir.toPath())) {
				open(dir.toPath());
			}
			preferences.put("lastOpenFolder", dir.getAbsolutePath());
		}
	}

	protected void btnReload_onAction(ActionEvent event) throws IOException {
		toast.hide();

		String text = tfInputPath.getText();
		if(text.length() == 0) {
			return;
		}
		Path path = Paths.get(text);
		if(!Files.isDirectory(path)) {
			return;
		}
		open(path);
	}

	protected void btnOpenBrowser_onAction(ActionEvent event) throws IOException, URISyntaxException {
		toast.hide();

		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.BROWSE)) {
				String url = defaultUrl.get() != null ? defaultUrl.get() : "";
				desktop.browse(new URI("http://localhost:" + HTTP_SERVER_PORT + "/" + url));
			}
		}
	}

	protected void btnSaveAs_onAction(ActionEvent event) {
		toast.hide();

		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("名前を付けて保存");

		String key = inputPath.get().getFileName().toString();
		try {
			key = String.format("%032X", new BigInteger(1,	MessageDigest.getInstance("MD5").digest(
					inputPath.get().toString().getBytes(StandardCharsets.UTF_8))));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String s = preferences.get("lastSaveDirectory_" + key, null);
		if(s == null) {
			File defaultDirectory = inputPath.get().resolve("htdocs").toFile();
			if(defaultDirectory.exists() && defaultDirectory.isDirectory()) {
				dc.setInitialDirectory(defaultDirectory);
			}
		} else {
			File lastSaveDirectory = new File(s);
			while(lastSaveDirectory != null && lastSaveDirectory.toString().length() >= 4) {
				if(lastSaveDirectory.exists() && lastSaveDirectory.isDirectory()) {
					dc.setInitialDirectory(lastSaveDirectory);
					break;
				}
				lastSaveDirectory = lastSaveDirectory.getParentFile();
			}
		}
		File dir = dc.showDialog(getPrimaryStage());
		if(dir != null) {
			preferences.put("lastSaveDirectory_" + key, dir.getAbsolutePath());
			save(inputPath.get(), dir.toPath());
		}
	}

	protected void btnUpload_onAction(ActionEvent event) {
		upload(inputPath.getValue());
	}

	protected void lblCheatSheet_onMouseClicked(MouseEvent event) throws IOException, URISyntaxException {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(new URI(MARKDOWN_CHEAT_SHEET_URL));
			}
		}
	}

	protected void lblVSCode_onMouseClicked(MouseEvent event) throws IOException {
		Path dir = inputPath.get();
		if(dir != null && Files.isDirectory(dir) && VSCode.isInstalled()) {
			VSCode.open(dir);
		}
	}

	void scene_onDragOver(DragEvent event) {
		if(!busy.get() && DragboardHelper.hasOneDirectory(event.getDragboard())) {
			event.acceptTransferModes(TransferMode.COPY);
		} else {
			event.consume();
		}
	}

	void scene_onDragDropped(DragEvent event) {
		boolean success = false;
		if(!busy.get() && DragboardHelper.hasOneDirectory(event.getDragboard())) {
			success = true;
			Path dir = DragboardHelper.getDirectory(event.getDragboard());
			Platform.runLater(wrap(() -> {
				toast.hide();
				if(prepareOpening(dir)) {
					open(dir);
				}
			}));
		}
		event.setDropCompleted(success);
		event.consume();
	}
	
	void fileWatchService_onSucceeded(WorkerStateEvent event) {
		// htdocs以下のフォルダーで更新が検出されても無視します。
		boolean isModified = false;
		for(Path path : ((FileWatchService)event.getSource()).getValue()) {
			if(!(path + File.separator).toLowerCase().startsWith("htdocs" + File.separator)) {
				isModified = true;
			}
		}
		if(isModified) {
			// ファイル変更イベントは連続で発生するので
			// 600ミリ秒以上連続でイベントが発生しなくなってから処理を開始します。
			// （たとえばファイルを書き換えると DELETE + CREATE で2回ファイル変更イベントが連続発生することがあります。）
			LocalDateTime t = this.lastModified = LocalDateTime.now();
			FxApplicationThread.runLater(600, wrap(() -> {
				if(t.equals(lastModified) && inputPath.getValue() != null) {
					if(busy.get()) {
						// 更新や保存処理中は更新が検出されても無視します。
						dirty.setValue(true);
					} else {
						update(inputPath.getValue());
					}
				}
			}));
		}
	}

	protected boolean prepareOpening(Path inputPath) throws IOException {
		if(BlogWizard.isApplicable(inputPath)) {
			BlogWizard wizard = new BlogWizard(getPrimaryStage(), inputPath);
			BlogWizard.InputData data = wizard.showAndWait().orElse(null);
			if(data != null) {
				if(Files.exists(data.file)) {
					toast.show(Toast.RED, "指定したファイルは既に存在しています", data.file.toString(), null);
					return false;
				} else {
					wizard.createNewPost(data);
					fileWatchService.restart(inputPath);
					String relativePath = inputPath.relativize(data.file).toString();
					toast.show(Toast.GREEN, "記事ファイルを作成しました", relativePath, Toast.SHORT_PERSISTENT);
				}
			}
		}
		return true;
	}

	protected void open(Path path) throws IOException {
		if(path == null) {
			return;
		}

		updateRecentFile(path);

		if(Files.isDirectory(path)) {
			tfInputPath.setText(path.toAbsolutePath().toString());
			inputPath.setValue(path);
			createTemporaryDirectory("preview-htdocs", true);
			update(path);
		} else {
			toast.show(Toast.RED, "Not Found", path.toString(), Toast.SHORT);
		}
	}

	void busy_onChange(boolean newValue) throws IOException {
		if(newValue == false && dirty.getValue() == true) {
			dirty.setValue(false);
			if(inputPath.getValue() != null) {
				update(inputPath.getValue());
			}
		}
	}
	
	protected void update(Path inputPath) throws IOException {
		progressOffset = 0.0;
		progressScale = 1.0;
		progressBar.setProgress(0.0);
		progressLabel.setText("");
		
		toast.hide();
		busy.setValue(true);
		draft.set(false);
		if(uploadConfigFactory != null) {
			Path mydataPath = Util.getApplicationPath(getClass()).resolve("mydata");
			uploadConfig.setValue(uploadConfigFactory.create(inputPath, mydataPath));
		}
		previewOutputPath.setValue(null);

		Async.execute(() -> {
			Path outputPath = createTemporaryDirectory("preview-htdocs", false);
			Catalpa catalpa = new Catalpa(inputPath);
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("_PREVIEW", true);
			catalpa.process(outputPath, options, this);
			httpServer.setDocumentRoot(outputPath);
			httpServer.update();
			Platform.runLater(() -> {
				draft.setValue(options.containsKey("_DRAFT"));
				defaultUrl.setValue((String) options.get("_DEFAULT_URL"));
				previewOutputPath.setValue(outputPath);
			});
		}).onSucceeded(() -> {
			toast.show(Toast.GREEN, "更新プロセスが正常に終了しました", Toast.SHORT);
		}).onCompleted(state -> {
			busy.setValue(false);
		});
	}
	
	protected void save(Path inputPath, Path outputPath) {
		progressOffset = 0.0;
		progressScale = 1.0;
		progressBar.setProgress(0.0);
		progressLabel.setText("");
		
		toast.hide();
		busy.setValue(true);
		Async.execute(() -> {
			Catalpa catalpa = new Catalpa(inputPath);
			Map<String, Object> options = new HashMap<String, Object>();
			catalpa.process(outputPath, options, this);
		}).onSucceeded(() -> {
			toast.show(Toast.GREEN, "保存しました", outputPath.toString(), Toast.LONG);
		}).onCompleted(state -> {
			busy.setValue(false);
		});
	}
	
	protected void upload(Path inputPath) {
		progressOffset = 0.0;
		progressScale = 0.5;
		progressBar.setProgress(0.0);
		progressLabel.setText("");

		toast.hide();
		busy.setValue(true);
		Async.execute(() -> {
			Path outputPath = createTemporaryDirectory("upload-htdocs", true);
			Catalpa catalpa = new Catalpa(inputPath);
			Map<String, Object> options = new HashMap<String, Object>();
			catalpa.process(outputPath, options, this);
			progressOffset = 0.5;
			uploadConfig.get().upload(outputPath.toFile(), this);
		}).onSucceeded(() -> {
			toast.show(Toast.GREEN, "アップロードが完了しました", Toast.LONG);
		}).onCompleted(state -> {
			busy.setValue(false);
		});
	}
	
	@Override
	public void setProgress(double value) {
		Platform.runLater(() -> progressBar.setProgress(progressOffset + value * progressScale));
	}

	@Override
	public void setText(String text) {
		Platform.runLater(() -> progressLabel.setText(text));
	}
	
	protected void updateRecentFile(Path path) {
		List<String> recentFiles = new LinkedList<String>();
		
		String s = preferences.get("recentFiles", null);
		if(s != null && !s.isEmpty()) {
			String absPath = path != null ? path.toAbsolutePath().toString() : null;
			for(String p : s.split("\\|")) {
				if(!p.isEmpty() && !recentFiles.contains(p) && !p.equalsIgnoreCase(absPath)) {
					recentFiles.add(p);
					if(recentFiles.size() >= MAX_RECENT_FILES) {
						break;
					}
				}
			}
		}
		if(path != null && Files.isDirectory(path)) {
			recentFiles.add(0, path.toAbsolutePath().toString());
			if(recentFiles.size() > MAX_RECENT_FILES) {
				recentFiles.remove(recentFiles.size() - 1);
			}
		}
		preferences.put("recentFiles", String.join("|", recentFiles));
		
		for(int i = menuFile.getItems().size() - 1; i >= 0; i--) {
			MenuItem item = menuFile.getItems().get(i);
			if("RECENT_FILES".equals(item.getUserData())) {
				menuFile.getItems().remove(i);
			}
		}
		
		int i = menuFile.getItems().size() - 1;
		for(String p : recentFiles) {
			MenuItem item = new MenuItem(p);
			item.setOnAction(SilentEventHandler.wrap(event -> {
				toast.hide();
				String text = ((MenuItem)event.getSource()).getText();
				if(prepareOpening(Paths.get(text))) {
					open(Paths.get(text));
				}
			}));
			item.setUserData("RECENT_FILES");
			menuFile.getItems().add(i++, item);
		}
		if(recentFiles.size() > 0) {
			MenuItem separator = new SeparatorMenuItem();
			separator.setUserData("RECENT_FILES");
			menuFile.getItems().add(i, separator);
		}
	}
	
	public static Path createTemporaryDirectory(String dir, boolean isDeleteIfExists) throws IOException {
		Path path = Paths.get(System.getProperty("java.io.tmpdir"))
				.resolve("catalpa")
				.resolve(dir);

		if(isDeleteIfExists && Files.exists(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if(!path.equals(dir)) {
						Files.delete(dir);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		
		if(!Files.exists(path)) {
			Files.createDirectories(path);
		}
		
		return path;
	}
	
}
