package onl.oss.catalpa.gui;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import onl.oss.javafx.fxml.Fxml;

public class MainView extends StackPane {

    @FXML public MenuBar menuBar;
    @FXML public Menu menuFile;
    @FXML public MenuItem menuFileOpen;
    @FXML public MenuItem menuFileSaveAs;
    @FXML public MenuItem menuFileExit;
    @FXML public MenuItem menuHelpAbout;
    @FXML public Parent body;
    @FXML public Label lblNewPost;
    @FXML public Label lblVSCode;
    @FXML public Label lblCheatSheet;
    @FXML public TextField tfInputPath;
    @FXML public Button btnOpen;
    @FXML public Button btnReload;
    @FXML public CheckBox cbAutoReload;
    @FXML public Button btnOpenBrowser;
    @FXML public Button btnSaveAs;
    @FXML public Button btnUpload;
    @FXML public StackPane progress;
    @FXML public ProgressBar progressBar;
    @FXML public Label lblProgress;
    @FXML public Pane message;
    @FXML public Label lblMessage;
    @FXML public Button btnMessageClose;
    @FXML public Pane error;
    @FXML public Label lblError;
    @FXML public Button btnErrorClose;

    @SuppressWarnings("this-escape")
    public MainView() {
        Fxml.load(this, this);
    }
}
