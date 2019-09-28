package net.osdn.catalpa.ui.javafx;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.addon.blog.BlogAddOn;
import net.osdn.catalpa.addon.blog.Post;
import net.osdn.catalpa.handler.YamlFrontMatterHandler;
import net.osdn.util.io.AutoDetectReader;

public class BlogWizard extends Stage implements Initializable {
	public static char[] ILLEGAL_CHARACTERS = new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|' };
	
	@FXML private Calendar  calendar;
	@FXML private FlowPane  categories;
	@FXML private TextField tfFilename;
	@FXML private Button    btnSkip;
	@FXML private Button    btnCreate;
	
	private Path inputPath;
	private boolean isCreateRequested;

	public BlogWizard(Stage owner, Path inputPath) throws IOException {
		this.inputPath = inputPath;
		
		initModality(Modality.WINDOW_MODAL);
		initOwner(owner);
		setResizable(false);
		
		getIcons().addAll(owner.getIcons());
		setTitle(inputPath + " - " + Main.getTitle());

		FXMLLoader loader = new FXMLLoader(getClass().getResource("BlogWizard.fxml"));
		loader.setController(this);
		Parent root = (Parent)loader.load();

		Scene scene = new Scene(root);
		root.layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {
			@Override
			public void changed(ObservableValue<? extends Bounds> observable,
					Bounds oldValue, Bounds newValue) {
				
				final int X_END_MARGIN = 8;
				final int Y_END_MARGIN = 40;
				if(newValue.getWidth() > 0 && newValue.getHeight() > 0) {
					double x = (owner.getX() + owner.getWidth() / 2) - ((newValue.getWidth() + X_END_MARGIN) / 2);
					double y = (owner.getY() + owner.getHeight() / 2) - ((newValue.getHeight() + Y_END_MARGIN) / 2);
					Rectangle2D workarea = Screen.getPrimary().getVisualBounds();
					if(x + (newValue.getWidth() + X_END_MARGIN) > workarea.getMaxX()) {
						x = workarea.getMaxX() - (newValue.getWidth() + X_END_MARGIN);
					}
					if(x < workarea.getMinX()) {
						x = workarea.getMinX();
					}
					if(y + (newValue.getHeight() + Y_END_MARGIN) > workarea.getMaxY()) {
						y = workarea.getMaxY() - (newValue.getHeight() + Y_END_MARGIN);
					}
					if(y < workarea.getMinY()) {
						y = workarea.getMinY();
					}
					BlogWizard.this.setX(x);
					BlogWizard.this.setY(y);
					root.layoutBoundsProperty().removeListener(this);
				}
			}
		});
		
		calendar.getDatePicker().setValue(LocalDate.now());
		tfFilename.requestFocus();
		setScene(scene);
		setOnShowing(event -> Platform.runLater(() -> BlogWizard.this.sizeToScene()));
		setOnShown(windowEvent -> {
			Task<Set<Category>> task = new Task<Set<Category>>() {
				@Override
				protected Set<Category> call() throws Exception {
					return getCategories(inputPath);
				}
			};
			task.setOnSucceeded(workerStateEvent -> {
				categories.getChildren().clear();

				@SuppressWarnings("unchecked")
				Set<Category> result = (Set<Category>)workerStateEvent.getSource().getValue();
				if(result != null) {
					String css = getClass().getResource("BlogWizard.css").toExternalForm();
					for(Category category : result) {
						ToggleButton tb = new ToggleButton(category.name);
						tb.setUserData(category.id);
						tb.getStylesheets().add(css);
						tb.getStyleClass().clear();
						tb.getStyleClass().add("category-button");
						categories.getChildren().add(tb);
					}
				}
			});
			new Thread(task).start();
		});
	}
	
	public InputData getInputData() throws IOException {
		if(!isCreateRequested) {
			return null;
		}
		
		InputData data = new InputData();
		
		data.date = calendar.getDatePicker().getValue();
		if(data.date == null) {
			data.date = LocalDate.now();
		}
		
		String subdir = data.date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		String filename = tfFilename.getText().trim();
		if(filename.indexOf('.') == -1) {
			filename += ".md";
		}
		for(char c : ILLEGAL_CHARACTERS) {
			if(filename.indexOf(c) != -1) {
				throw new IOException("ファイル名に使用できない文字 " + c + " が含まれています");
			}
		}
		data.file = inputPath.resolve(subdir).resolve(filename);

		data.categories = new ArrayList<Category>();
		for(int i = 0; i < categories.getChildren().size(); i++) {
			if(categories.getChildren().get(i) instanceof ToggleButton) {
				ToggleButton tb = (ToggleButton)categories.getChildren().get(i);
				if(tb.isSelected()) {
					Category category = new Category();
					category.name = tb.getText();
					category.id = tb.getUserData().toString();
					data.categories.add(category);
				}
			}
		}
		
		return data;
	}
	
	public void createNewPost(InputData data) throws Exception {
		StringWriter writer = new StringWriter();
		writer.write("---\n");
		writer.write("draft:\r\n");
		writer.write("date: " + data.date.format(DateTimeFormatter.ISO_DATE) + "\r\n");
		writer.write("title: \"記事のタイトル\"\r\n");
		writer.write("description: |\r\n");
		writer.write("  ここに記事の説明文を書きます。\r\n");
		writer.write("  パイプ文字 | を使っているので複数行に渡って文字データを書くことができます。\r\n");
		writer.write("categories: [");
		if(data.categories.size() == 0) {
			writer.write("\"カテゴリーA(cat-a)\", \"カテゴリーB(cat-b)\"");
		} else {
			for(int i = 0; i < data.categories.size(); i++) {
				Category category = data.categories.get(i);
				writer.write(category.name);
				if(!category.name.toLowerCase().equals(category.id)) {
					writer.write("(" + category.id + ")");
				}
				if(i + 1 < data.categories.size()) {
					writer.write(", ");
				}
			}
		}
		writer.write("]\r\n");
		writer.write("---\r\n");
		writer.write("\r\n");
		
		writer.write("ここに記事の冒頭の文章を書きます。\r\n");
		writer.write("冒頭の文章はリストページにも表示されます。\r\n");
		writer.write("\r\n");
		writer.write("<!-- more -->\r\n");
		writer.write("\r\n");
		writer.write("ここから記事の続きを書きます。\r\n");
		writer.write("HTMLコメント more 以降は、記事の個別ページに表示されます。\r\n");
		writer.write("\r\n");
		writer.write("ヘッダーに draft: 行が含まれていると、下書きモードになります。\r\n");
		writer.write("下書きモードでは draft: が指定されている記事のみが処理されるため高速です。\r\n");
		writer.write("記事を書き終えたら、draft: 行を削除してください。すべての記事が処理されるようになります。\r\n");
		writer.write("\r\n");
		writer.write("下書き中の記事を保留して、他の記事を処理したい場合は draft: の値に skip を指定してください\r\n");
		writer.write("draft: の値に skip を指定すると、その記事は処理対象から除外されます。\r\n");
		writer.write("\r\n");
		
		Path dir = data.file.getParent();
		if(!Files.exists(dir)) {
			Files.createDirectories(dir);
		}
		Files.writeString(data.file, writer.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		btnSkip.setOnAction(this::btnSkip_onAction);
		btnCreate.setOnAction(this::btnCreate_onAction);
		btnCreate.disableProperty().bind(tfFilename.textProperty().isEmpty());
	}
	
	protected void btnSkip_onAction(ActionEvent event) {
		close();
	}
	
	protected void btnCreate_onAction(ActionEvent event) {
		isCreateRequested = true;
		close();
	}
	
	public static boolean isApplicable(Path inputPath) throws IOException {
		boolean isApplicable = false;
		
		Path file = inputPath.resolve(Catalpa.CONFIG_FILENAME);
		Map<String, Object> config = getYamlFrontMatter(file);
		if(config != null) {
			Object obj = config.get("type");
			if(obj != null) {
				String type = obj.toString();
				isApplicable = (type != null && type.equals("blog"));
			}
		}
		
		return isApplicable;
	}
	
	public static Set<Category> getCategories(Path inputPath) throws IOException {
		Set<String> names = new LinkedHashSet<String>();
		Map<String, String> ids = new HashMap<String, String>();
		
		Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if(Post.isApplicable(file)) {
					for(Category category : getCategoriesByPost(file)) {
						names.add(category.name);
						if(category.id != null) {
							ids.put(category.name, category.id);
						}
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		
		Set<Category> categories = new LinkedHashSet<Category>();
		for(String name : names) {
			String id = ids.get(name);
			if(id == null) {
				id = name.toLowerCase();
			}
			Category category = new Category();
			category.name = name;
			category.id = id;
			categories.add(category);
		}
		return categories;
	}
	
	public static List<Category> getCategoriesByPost(Path file) throws IOException {
		List<Category> categories = new ArrayList<Category>();
		
		Map<String, Object> map = getYamlFrontMatter(file);
		if(map.get("date") != null && map.get("title") != null) {
			Object c = map.get("categories");
			if(c == null) {
				c = map.get("category");
			}
			if(c instanceof List) {
				for(Object e : (List<?>)c) {
					if(e != null) {
						String text = e.toString();
						Category category = new Category();
						category.name = text;
						category.id = null;
						Matcher m = BlogAddOn.CATEGORY_ID_PATTERN.matcher(text);
						if(m.matches()) {
							category.name = m.group(1);
							category.id = m.group(2).toLowerCase();
						}
						categories.add(category);
					}
				}
			} else if(c != null) {
				String text = c.toString();
				Category category = new Category();
				category.name = text;
				category.id = null;
				Matcher m = BlogAddOn.CATEGORY_ID_PATTERN.matcher(text);
				if(m.matches()) {
					category.name = m.group(1);
					category.id = m.group(2).toLowerCase();
				}
			}
		}
		return categories;
	}
	
	public static Map<String, Object> getYamlFrontMatter(Path file) throws IOException {
		Context context = new Context(null, null);
		if(Files.exists(file) && !Files.isDirectory(file)) {
			try(Reader reader = new AutoDetectReader(file);
					Writer writer = new StringWriter()) {
				new YamlFrontMatterHandler().handle(context, reader, writer);
			}
		}
		Map<String, Object> yamlFrontMatter = context.getDeclaredYamlFrontMatter();
		return yamlFrontMatter;
	}
	
	/* package private */ static class Category {
		public String name;
		public String id;
	}
	
	/* package private */ static class InputData {
		public LocalDate      date;
		public Path           file;
		public List<Category> categories;
	}
}
