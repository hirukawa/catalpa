package net.osdn.catalpa.ui.javafx;

public class ToastMessage extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String title;

    public ToastMessage(String title, String message) {
        super(message);
        this.title = title;
    }

    public ToastMessage(String title, String message, Exception cause) {
        super(message, cause);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
