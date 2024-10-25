package onl.oss.util.prefs;

import java.nio.file.Path;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class FilePreferencesFactory implements PreferencesFactory {

    private static Path file;

    public static void setFilePath(Path file) {
        FilePreferencesFactory.file = file;
    }

    public static Path getFilePath() {
        return file;
    }

    private final Preferences root = new FilePreferences(null, "");

    @Override
    public Preferences systemRoot() {
        return userRoot();
    }

    @Override
    public Preferences userRoot() {
        return root;
    }
}
