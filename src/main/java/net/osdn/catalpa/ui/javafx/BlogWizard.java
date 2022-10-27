package net.osdn.catalpa.ui.javafx;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.Set;
import java.util.regex.Matcher;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Window;
import net.osdn.catalpa.Catalpa;
import net.osdn.catalpa.Context;
import net.osdn.catalpa.addon.blog.BlogAddOn;
import net.osdn.catalpa.addon.blog.Post;
import net.osdn.catalpa.handler.YamlFrontMatterHandler;
import net.osdn.util.io.AutoDetectReader;
import net.osdn.util.javafx.Unchecked;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.scene.control.DialogEx;

public class BlogWizard extends DialogEx<BlogWizard.InputData> {
	public static char[] ILLEGAL_CHARACTERS = new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|' };
	
	@FXML Calendar  calendar;
	@FXML FlowPane  categories;
	@FXML TextField tfFilename;
	@FXML Button    btnSkip;
	@FXML Button    btnCreate;
	@FXML Toast     toast;

	private Path inputPath;

	public BlogWizard(Window owner, Path inputPath) {

		super(owner);
		this.inputPath = inputPath;

		Node content = Fxml.load(this);
		getDialogPane().setContent(content);

		calendar.getDatePicker().setValue(LocalDate.now());
		tfFilename.requestFocus();
		btnSkip.setOnAction(event -> Unchecked.execute(() -> {
			btnSkip_onAction(event);
		}));
		btnCreate.setOnAction(event -> Unchecked.execute(() -> {
			btnCreate_onAction(event);
		}));
		btnCreate.disableProperty().bind(tfFilename.textProperty().isEmpty());
		setResultConverter(param -> { return getResult(); });
		setOnShown(event -> Unchecked.execute(() -> {
			dialog_onShown(event);
		}));

		// ButtonTypes に何もボタンが追加されていないと、
		// ダイアログ右上の×ボタンを押してもダイアログを閉じることができなくなります。
		// ダイアログ右上の×ボタンでダイアログを閉じるためには
		// ButtonType.CANCEL または ButtonType.CLOSE を追加する必要があります。
		// しかし、ButtonTypes にボタンを追加すると、そのボタンがダイアログ下部に表示されてしまいます。
		// 今回は ButtonTypes によって作成される既定のダイアログボタンを表示したくないので、
		// ボタンバー（.button-barスタイルクラスで探すことができる）を取り除いてしまいます。
		getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		Node buttonBar = getDialogPane().lookup(".button-bar");
		if(buttonBar != null) {
			getDialogPane().getChildren().remove(buttonBar);
		}
	}

	void dialog_onShown(DialogEvent event) {

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
	}

	void btnSkip_onAction(ActionEvent event) {
		close();
	}

	void btnCreate_onAction(ActionEvent event) {
		try {
			setResult(getInputData());
			close();
		} catch(IOException e) {
			tfFilename.requestFocus();
			toast.show(Toast.RED, "エラー", e.getMessage(), Toast.SHORT);
		}
	}

	public InputData getInputData() throws IOException {
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
				throw new IOException("ファイル名に使用できない文字 " + c + " が含まれています。");
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
	
	public void createNewPost(InputData data) throws IOException {
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
