package net.osdn.catalpa.ui.javafx;

import java.awt.Desktop;
import java.awt.SplashScreen;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import freemarker.template.TemplateException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.ProgressObserver;
import net.osdn.catalpa.Util;
import net.osdn.catalpa.upload.UploadConfig;
import net.osdn.catalpa.upload.UploadConfigFactory;

public class Main extends Application implements Initializable, ProgressObserver {
	private static final String MARKDOWN_CHEAT_SHEET_URL = "https://catalpa.osdn.jp/markdown.html";
	
	private static AtomicInteger count = new AtomicInteger(0);
	private static CountDownLatch latch = new CountDownLatch(1);
	private static Main instance;
	
	public static void main(String[] args) throws Throwable {
		if(instance == null || instance.primaryStage == null) {
			if(count.getAndIncrement() == 0) {
				try {
					launch(args);
				} catch(Throwable t) {
					if(t.getCause() instanceof InvocationTargetException && t.getCause().getCause() != null) {
						throw t.getCause().getCause();
					} else {
						throw t;
					}
				}
				return;
			}
			latch.await();
		}
		Platform.runLater(()-> {
			if(Main.instance.primaryStage.isIconified()) {
				Main.instance.primaryStage.setIconified(false);
			}
			Main.instance.primaryStage.toFront();
		});
	}
	
	public static String getTitle() {
		String version = "";
		int[] v = Util.getApplicationVersion();
		if(v != null) {
			if(v[2] == 0) {
				version = String.format(" %d.%d", v[0], v[1]);
			} else {
				version = String.format(" %d.%d.%d", v[0], v[1], v[2]);
			}
		}
		return "Catalpa" + version;
	}
	
	private int MAX_RECENT_FILES = 10;
	private int HTTP_SERVER_PORT = 4000;
	private int HTTP_SERVER_PORT_MAX = HTTP_SERVER_PORT + 5;

	private Stage primaryStage;
	private Preferences preferences = Preferences.userNodeForPackage(getClass());
	
	private UploadConfigFactory uploadConfigFactory;
	private FileWatchService fileWatchService;
	private LocalDateTime lastModified;
	private ExecutorService executorService;
	private HttpServer httpServer;
	private double progressOffset = 0.0;
	private double progressScale = 1.0;

	public Main() throws IOException {
		Main.instance = this;

		System.setProperty("org.freemarker.loggerLibrary", "none");
		
		uploadConfigFactory = new UploadConfigFactory();
		
		fileWatchService = new FileWatchService();
		fileWatchService.setOnSucceeded(event -> {
			// htdocs以下のフォルダーで更新が検出されても無視します。
			boolean isModified = false;
			for(Path path : ((FileWatchService)event.getSource()).getValue()) {
				if(!(path + File.separator).toLowerCase().startsWith("htdocs" + File.separator)) {
					isModified = true;
				}
			}
			if(isModified) {
				// 更新や保存処理中は更新が検出されても無視します。
				if(busy.get()) {
					dirty.setValue(true);
					return;
				}
				LocalDateTime t = this.lastModified = LocalDateTime.now();
				new Timeline(new KeyFrame(Duration.millis(600), onFinished -> {
					if(t.equals(lastModified) && inputPath.getValue() != null) {
						try {
						update(inputPath.getValue());
						} catch (IOException e) {
							showException(e);
						}
					}
				})).play();
			}
		});
		
		executorService = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			return thread;
		});
		
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
		this.primaryStage = primaryStage;
		
		Image icon64 = new Image(getClass().getResourceAsStream("/img/app-icon-64px.png"));
		//Image icon48 = new Image(getClass().getResourceAsStream("/img/app-icon-48px.png"));
		//Image icon32 = new Image(getClass().getResourceAsStream("/img/app-icon-32px.png"));
		//Image icon16 = new Image(getClass().getResourceAsStream("/img/app-icon-16px.png"));
		primaryStage.getIcons().addAll(icon64);
		primaryStage.setTitle(getTitle());
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
		loader.setController(this);
		Parent root = (Parent)loader.load();
		
		Scene scene = new Scene(root);
		scene.setOnDragOver(event -> {
			if(!busy.get() && DragboardHelper.hasOneDirectory(event.getDragboard())) {
				event.acceptTransferModes(TransferMode.COPY);
			} else {
				event.consume();
			}
		});
		scene.setOnDragDropped(event -> {
			boolean success = false;
			if(!busy.get() && DragboardHelper.hasOneDirectory(event.getDragboard())) {
				success = true;
				Path dir = DragboardHelper.getDirectory(event.getDragboard());
				Platform.runLater(() -> {
					toast.hide();
					
					try {
						if(prepareOpening(dir)) {
							open(dir);
						}
					} catch (IOException e) {
						showException(e);
					}
				});
			}
			event.setDropCompleted(success);
			event.consume();
		});
		
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		double width = preferences.getDouble("stageWidth", 0.0);
		double height = preferences.getDouble("stageHeight", 0.0);
		if(width > 0.0 && height > 0.0) {
			primaryStage.setWidth(width);
			primaryStage.setHeight(height);
		}
		primaryStage.show();
		primaryStage.sizeToScene();
		preferences.putDouble("stageWidth", primaryStage.getWidth());
		preferences.putDouble("stageHeight", primaryStage.getHeight());
		
		Main.latch.countDown();
	}
	
	protected boolean prepareOpening(Path inputPath) throws IOException {
		try {
			if(BlogWizard.isApplicable(inputPath)) {
				BlogWizard wizard = new BlogWizard(primaryStage, inputPath);
				wizard.showAndWait();
				BlogWizard.InputData data = wizard.getInputData();
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
		} catch(Exception e) {
			showException(e);
			return false;
		}

		return true;
	}
	
	protected void open(Path path) throws IOException {
		if(path != null) {
			updateRecentFile(path);
		}
		
		if(path != null) {
			if(Files.isDirectory(path)) {
				tfInputPath.setText(path.toAbsolutePath().toString());
				inputPath.setValue(path);
				createTemporaryDirectory("preview-htdocs", true);
				update(path);
			} else {
				toast.show(Toast.RED, "Not Found", path.toString(), Toast.SHORT);
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
		executorService.submit(() -> {
			try {
				Path outputPath = createTemporaryDirectory("preview-htdocs", false);
				Catalpa catalpa = new Catalpa(inputPath);
				Map<String, Object> options = new HashMap<String, Object>();
				options.put("_PREVIEW", true);
				catalpa.process(outputPath, options, this);
				httpServer.setDocumentRoot(outputPath);
				httpServer.update();
				Platform.runLater(()-> {
					draft.setValue(options.containsKey("_DRAFT"));
					defaultUrl.setValue((String)options.get("_DEFAULT_URL"));
					previewOutputPath.setValue(outputPath);
				});
				toast.show(Toast.GREEN, "更新プロセスが正常に終了しました", Toast.SHORT);
			} catch (Exception e) {
				showException(e);
			} finally {
				Platform.runLater(()-> busy.setValue(false));
			}
		});
	}
	
	protected void save(Path inputPath, Path outputPath) {
		progressOffset = 0.0;
		progressScale = 1.0;
		progressBar.setProgress(0.0);
		progressLabel.setText("");
		
		toast.hide();
		busy.setValue(true);
		executorService.submit(()-> {
			try {
				Catalpa catalpa = new Catalpa(inputPath);
				Map<String, Object> options = new HashMap<String, Object>();
				catalpa.process(outputPath, options, this);
				toast.show(Toast.GREEN, "保存しました", outputPath.toString(), Toast.LONG);
			} catch (Exception e) {
				showException(e);
			} finally {
				Platform.runLater(()-> busy.setValue(false));
			}
		});
	}
	
	protected void upload(Path inputPath) {
		progressOffset = 0.0;
		progressScale = 0.5;
		progressBar.setProgress(0.0);
		progressLabel.setText("");

		toast.hide();
		busy.setValue(true);
		executorService.submit(()-> {
			try {
				Path outputPath = createTemporaryDirectory("upload-htdocs", true);
				Catalpa catalpa = new Catalpa(inputPath);
				Map<String, Object> options = new HashMap<String, Object>();
				catalpa.process(outputPath, options, this);
				progressOffset = 0.5;
				uploadConfig.get().upload(outputPath.toFile(), this);
				toast.show(Toast.GREEN, "アップロードが完了しました", Toast.LONG);
			} catch(Exception e) {
				showException(e);
			} finally {
				Platform.runLater(()-> busy.setValue(false));
			}
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
	
	//
	//
	//
	
	protected void openFolder() throws IOException {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("フォルダーを開く");
		
		String s = preferences.get("lastOpenFolder", null);
		if(s != null) {
			File lastOpenDirectory = new File(s);
			if(lastOpenDirectory.exists() && lastOpenDirectory.isDirectory()) {
				dc.setInitialDirectory(lastOpenDirectory);
			}
		}
		File dir = dc.showDialog(primaryStage);
		if(dir != null) {
			if(prepareOpening(dir.toPath())) {
				open(dir.toPath());
			}
			preferences.put("lastOpenFolder", dir.getAbsolutePath());
		}
	}

	protected void openBrowser() throws IOException, URISyntaxException {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.BROWSE)) {
				String url = defaultUrl.get() != null ? defaultUrl.get() : "";
				desktop.browse(new URI("http://localhost:" + HTTP_SERVER_PORT + "/" + url));
			}
		}
	}
	
	protected void showCheatSheet() throws IOException, URISyntaxException {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(new URI(MARKDOWN_CHEAT_SHEET_URL));
			}
		}
	}
	
	protected void saveAs(Path inputPath) {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("名前を付けて保存");
		String s = preferences.get("lastSaveDirectory_" + inputPath, null);
		if(s == null) {
			File defaultDirectory = inputPath.resolve("htdocs").toFile();
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
		File dir = dc.showDialog(primaryStage);
		if(dir != null) {
			preferences.put("lastSaveDirectory_" + inputPath, dir.getAbsolutePath());
			save(inputPath, dir.toPath());
		}
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
			item.setOnAction(event -> {
				toast.hide();
				String text = ((MenuItem)event.getSource()).getText();
				try {
					if(prepareOpening(Paths.get(text))) {
						open(Paths.get(text));
					}
				} catch (IOException e) {
					showException(e);
				}
			});
			item.setUserData("RECENT_FILES");
			menuFile.getItems().add(i++, item);
		}
		if(recentFiles.size() > 0) {
			MenuItem separator = new SeparatorMenuItem();
			separator.setUserData("RECENT_FILES");
			menuFile.getItems().add(i, separator);
		}
	}
	
	protected void showException(Exception e) {
		e.printStackTrace();
		
		Platform.runLater(()-> {
			String title = e.getClass().getName();
			String message = e.getLocalizedMessage();
			
			if(e instanceof TemplateException) {
				int i = message.indexOf("\n----\n");
				if(i >= 0) {
					message = message.substring(0, i).trim();
				}
			}
			
			toast.show(Toast.RED, title, message, null);
		});
	}
	
	protected void closeSplashScreen() {
		long SPLASH_TIME = 1500;
		
		SplashScreen splash = SplashScreen.getSplashScreen();
		if(splash != null) {
			long startup = 0;
			String s = System.getProperty("java.application.startup");
			if(s != null) {
				try {
					startup = LocalDateTime.parse(s).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
				} catch(DateTimeParseException e) {}
			}
			long elapsed = System.currentTimeMillis() - startup;
			long delay = Math.max(SPLASH_TIME - elapsed, 1L);
			new Timeline(new KeyFrame(Duration.millis(delay), onFinished -> {
				splash.close();
			})).play();
		}
	}
	
	private static Path createTemporaryDirectory(String dir, boolean isDeleteIfExists) throws IOException {
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
		primaryStage.setX(preferences.getDouble("stageX", 50.0));
		primaryStage.setY(preferences.getDouble("stageY", 50.0));
		primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
			Rectangle2D workarea = Screen.getPrimary().getVisualBounds();
			double x = preferences.getDouble("stageX", 50.0);
			if(x + primaryStage.getWidth() > workarea.getMaxX()) {
				x = workarea.getMaxX() - primaryStage.getWidth();
			}
			if(x < workarea.getMinX()) {
				x = workarea.getMinX();
			}
			primaryStage.setX(x);
			primaryStage.xProperty().addListener((observable, oldValue, newValue)-> {
				preferences.putDouble("stageX", newValue.doubleValue());
			});
			double y = preferences.getDouble("stageY", 50.0);
			if(y + primaryStage.getHeight() > workarea.getMaxY()) {
				y = workarea.getMaxY() - primaryStage.getHeight();
			}
			if(y < workarea.getMinY()) {
				y = workarea.getMinY();
			}
			primaryStage.setY(y);
			primaryStage.yProperty().addListener((observable, oldValue, newValue)-> {
				preferences.putDouble("stageY", newValue.doubleValue());
			});
			closeSplashScreen();
		});
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
		
		busy.addListener((observable, oldValue, newValue) -> {
			if(newValue == false && dirty.getValue() == true) {
				dirty.setValue(false);
				if(inputPath.getValue() != null) {
					try {
						update(inputPath.getValue());
					} catch (IOException e) {
						showException(e);
					}
				}
			}
		});
		
		updateRecentFile(null);
	}
	
	@FXML
	void menuFileOpen_onAction(ActionEvent event) {
		toast.hide();
		
		try {
			openFolder();
		} catch(Exception e) {
			showException(e);
		}
	}
	
	@FXML
	void menuFileSaveAs_onAction(ActionEvent event) {
		toast.hide();
		
		saveAs(inputPath.getValue());
	}
	
	@FXML
	void menuFileExit_onAction(ActionEvent event) {
		primaryStage.close();
	}
	
	@FXML
	void btnOpen_onAction(ActionEvent event) {
		toast.hide();
		
		try {
			openFolder();
		} catch(Exception e) {
			showException(e);
		}
	}
	
	@FXML
	void btnReload_onAction(ActionEvent event) {
		toast.hide();
		
		String text = tfInputPath.getText();
		if(text.length() == 0) {
			return;
		}
		Path path = Paths.get(text);
		if(!Files.isDirectory(path)) {
			return;
		}
		try {
			open(path);
		} catch (IOException e) {
			showException(e);
		}
	}
	
	@FXML
	void btnOpenBrowser_onAction(ActionEvent event) {
		try {
			openBrowser();
		} catch (IOException | URISyntaxException e) {
			showException(e);
		}
	}
	
	@FXML
	void btnSaveAs_onAction(ActionEvent event) {
		saveAs(inputPath.getValue());
	}
	
	@FXML
	void btnUpload_onAction(ActionEvent event) {
		upload(inputPath.getValue());
	}
	
	@FXML
	void lblCheatSheet_onMouseClicked(MouseEvent event) {
		try {
			showCheatSheet();
		} catch (Exception e) {
			showException(e);
		}
	}
	
	@FXML
	void lblVSCode_onMouseClicked(MouseEvent event) {
		try {
			Path dir = inputPath.get();
			if(dir != null && Files.isDirectory(dir) && VSCode.isInstalled()) {
				VSCode.open(dir);
			}
		} catch(Exception e) {
			showException(e);
		}
	}
}
