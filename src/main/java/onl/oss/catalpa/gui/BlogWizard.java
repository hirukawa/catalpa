package onl.oss.catalpa.gui;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import onl.oss.catalpa.CacheManager;
import onl.oss.catalpa.Util;
import onl.oss.catalpa.blog.Category;
import onl.oss.catalpa.model.Content;
import onl.oss.javafx.fxml.Fxml;
import onl.oss.javafx.scene.SceneUtil;
import onl.oss.javafx.scene.control.DialogEx;

import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static onl.oss.catalpa.Logger.ERROR;

public class BlogWizard extends DialogEx<Void> {
    public static char[] ILLEGAL_CHARACTERS = new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|' };

    @FXML private Calendar calendar;
    @FXML private FlowPane fpCategories;
    @FXML private TextField tfFilename;
    @FXML private Button btnCancel;
    @FXML private Button btnCreate;
    @FXML private Pane error;
    @FXML private Label lblError;
    @FXML private Button btnErrorClose;

    private final Path inputPath;
    private final Content blogConfig;
    private final List<String> categories = new ArrayList<>();

    @SuppressWarnings("this-escape")
    public BlogWizard(Window owner, Path inputPath, Content blogConfig) {
        super(owner);
        this.inputPath = inputPath;
        this.blogConfig = blogConfig;

        Pane content = Fxml.load(this);
        getDialogPane().setContent(content);

        //
        // イベント・ハンドラー
        //
        calendar.getDatePicker().valueProperty().addListener(this::calendar_onChanged);
        tfFilename.textProperty().addListener(this::tfFilename_onChanged);
        btnCancel.setOnAction(this::btnCancel_onAction);
        btnCreate.setOnAction(this::btnCreate_onAction);
        btnErrorClose.setOnAction(this::btnErrorClose_onAction);
        lblError.setOnMouseClicked(this::lblError_onMouseClicked);

        //
        // バインディング
        //
        btnCreate.disableProperty().bind(tfFilename.textProperty().isEmpty());

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

        content.setOpacity(0.0);
        SceneUtil.invokeAfterLayout(content, () -> {
            initialize();
            content.setOpacity(1.0);
        });
    }

    private void initialize() {
        calendar.getDatePicker().setValue(LocalDate.now());

        tfFilename.requestFocus();

        Task<Set<String>> task = new Task<>() {
            @Override
            protected Set<String> call() throws Exception {
                Set<String> categoryNames = Collections.synchronizedSet(new TreeSet<>());
                try (Stream<Path> stream = Files.walk(blogConfig.getPath().getParent())) {
                    stream.parallel().forEach(path -> {
                        try {
                            if (Util.isMarkdownFile(path)) {
                                FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                                Content content = CacheManager.getContent(path, lastModifiedTime);
                                if (content == null) {
                                    content = new Content(path);
                                    CacheManager.putContent(path, lastModifiedTime, content);
                                }
                                if (content.getYaml().get("categories") instanceof List<?> list) {
                                    Set<String> set = new LinkedHashSet<>();
                                    for (Object e : list) {
                                        if (e != null) {
                                            Category category = new Category(e.toString());
                                            set.add(category.getName());
                                        }
                                    }
                                    categoryNames.addAll(set);
                                }
                            }
                        } catch (Exception e) {
                            ERROR(e);
                        }
                    });
                }
                return categoryNames;
            }
        };

        task.setOnSucceeded(workerStateEvent -> {
            @SuppressWarnings("unchecked")
            Set<String> categoryNames = (Set<String>)workerStateEvent.getSource().getValue();

            String css = null;
            URL url = getClass().getResource("BlogWizard.css");
            if (url != null) {
                css = url.toExternalForm();
            }

            fpCategories.getChildren().clear();

            for (String name : categoryNames) {
                ToggleButton tb = new ToggleButton(name);
                tb.getStylesheets().add(css);
                tb.getStyleClass().clear();
                tb.getStyleClass().add("category-button");
                tb.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        categories.add(tb.getText());
                    } else {
                        categories.remove(tb.getText());
                    }
                });
                fpCategories.getChildren().add(tb);
            }
        });

        MainApp.getExecutor().execute(task);
    }

    private void calendar_onChanged(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) {
        hideError();
    }

    private void tfFilename_onChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        hideError();
    }

    private void showError(String message) {
        Runnable runnable = () -> {
            lblError.setText(message);
            error.setVisible(true);
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void hideError() {
        Runnable runnable = () -> {
            lblError.setText("");
            error.setVisible(false);
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void btnCancel_onAction(ActionEvent event) {
        close();
    }

    private void btnCreate_onAction(ActionEvent event) {
        try {
            LocalDate date = calendar.getDatePicker().getValue();
            if (date == null) {
                date = LocalDate.now();
            }

            String dir = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String filename = tfFilename.getText().trim();
            if (!filename.toLowerCase().endsWith(".md")) {
                filename += ".md";
            }

            for (char c : ILLEGAL_CHARACTERS) {
                if (filename.indexOf(c) != -1) {
                    showError("ファイル名に使用できない文字 " + c + " が含まれています");
                    return;
                }
            }

            Path target = blogConfig.getPath().getParent().resolve(dir).resolve(filename);

            if (Files.exists(target)) {
                showError("指定したファイルは既に存在します");
                return;
            }

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("date", date);
            dataModel.put("categories", categories);

            Configuration freeMarker = new Configuration(Configuration.VERSION_2_3_34);
            freeMarker.setDefaultEncoding("UTF-8");
            freeMarker.setURLEscapingCharset("UTF-8");
            freeMarker.setLogTemplateExceptions(false);
            freeMarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            freeMarker.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
                    new FileTemplateLoader(inputPath.resolve("templates").toFile()),
                    new FileTemplateLoader(inputPath.toFile())
            }));

            Template template = freeMarker.getTemplate("blog-wizard.ftl");

            Path targetDirectory = target.getParent();
            if (Files.notExists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                template.process(dataModel, writer);
            }

            close();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void btnErrorClose_onAction(ActionEvent event) {
        hideError();
    }

    private void lblError_onMouseClicked(MouseEvent event) {
        hideError();
    }

    private boolean isYamlEscapeRequired(String s) {
        // ダブルクォートで囲む必要のある文字
        char[] chars = new char[] { ':', '{', '}', '[', ']', ',', '&', '*', '#', '?', '|', '-', '<', '>', '=', '!', '%', '@', '\\', '"' };
        for (char c : chars) {
            if (s.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private String escapeYaml(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
