package onl.oss.catalpa;

import java.io.Serial;
import java.nio.file.Path;

public class GeneratorException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -1;

    private transient final Path path;

    public GeneratorException(Path path, String message) {
        super(message);
        this.path = path;
    }

    public GeneratorException(Path path, Throwable cause) {
        super(cause);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
