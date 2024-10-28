package onl.oss.catalpa.model;

import java.nio.file.Path;

public class Progress {

    public final double value;
    public final String message;
    public final Path path;

    public Progress(double value) {
        this.value = value;
        this.message = null;
        this.path = null;
    }

    public Progress(double value, Path path) {
        this.value = value;
        this.message = null;
        this.path = path;
    }

    public Progress(double value, String message) {
        this.value = value;
        this.message = message;
        this.path = null;
    }

    public Progress(double value, String message, Path path) {
        this.value = value;
        this.message = message;
        this.path = path;
    }

    @Override
    public String toString() {
        return "Progress{" +
                "value=" + value +
                ", message='" + message + '\'' +
                ", path=" + path +
                '}';
    }
}
