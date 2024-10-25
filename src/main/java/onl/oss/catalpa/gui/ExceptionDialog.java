package onl.oss.catalpa.gui;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import onl.oss.javafx.fxml.Fxml;
import onl.oss.javafx.scene.SceneUtil;
import onl.oss.javafx.scene.control.DialogEx;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import static onl.oss.catalpa.Logger.ERROR;

public class ExceptionDialog extends DialogEx<Void> {

    @FXML private ScrollPane scrollPane;
    @FXML private TextFlow textFlow;

    private String vsCodePath;

    @SuppressWarnings("this-escape")
    public ExceptionDialog(Window owner, Path path, Throwable exception) {
        super(owner);

        vsCodePath = MainApp.getInstance().vsCodePath;

        setResizable(true);

        Pane content = Fxml.load(this);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        scrollPane.prefViewportWidthProperty().bind(textFlow.widthProperty());
        scrollPane.prefViewportHeightProperty().bind(textFlow.heightProperty());

        if (path != null) {
            Text link = new Text(path + "\n");
            link.getStyleClass().add("link");
            if (vsCodePath != null) {
                link.setCursor(Cursor.HAND);
                link.setOnMouseClicked(event -> {
                    link_onClicked(path);
                });
            }
            textFlow.getChildren().add(link);

            Text blank = new Text("\n");
            blank.getStyleClass().add("blank");
            textFlow.getChildren().add(blank);
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        pw.flush();
        String text = sw.toString();
        textFlow.getChildren().add(new Text(text));

        content.setOpacity(0.0);
        SceneUtil.invokeAfterLayout(content, () -> {
            textFlow.requestFocus();
            content.setOpacity(1.0);
        });
    }

    private void link_onClicked(Path path) {
        if (vsCodePath == null) {
            return;
        }

        if (path == null) {
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(vsCodePath, path.toString());
            pb.start();
        } catch (IOException e) {
            ERROR(e);
        }
    }
}
