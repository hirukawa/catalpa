package net.osdn.catalpa.ui.javafx;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class Datastore {

	private static Path appDir;
	private static String license;
	private static Boolean isRunningAsUWP;

	public static Path getApplicationDirectory() {
		if(appDir == null) {
			appDir = getApplicationDirectory(Datastore.class);
		}
		return appDir;
	}

	public static Path getApplicationDirectory(Class<?> cls) {
		try {
			ProtectionDomain pd = cls.getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			URL location = cs.getLocation();
			URI uri = location.toURI();
			Path path = Paths.get(uri);
			// IntelliJで実行した場合にプロジェクトディレクトリが返されるように classes/java/main を遡ります。
			while(Files.isDirectory(path)) {
				if(!"classes/java/main/".contains(path.getFileName().toString() + "/")) {
					break;
				}
				path = path.getParent();
			}
			return path.getParent().toRealPath();
		} catch (Exception e) {
			try {
				return Paths.get(".").toRealPath();
			} catch (IOException e1) {
				return new File(".").getAbsoluteFile().toPath();
			}
		}
	}

	public static int[] getApplicationVersion() {
		String s = System.getProperty("java.application.version");
		if(s == null || s.trim().length() == 0) {
			return null;
		}

		s = s.trim() + ".0.0.0.0";
		String[] array = s.split("\\.", 5);
		int[] version = new int[4];
		for(int i = 0; i < 4; i++) {
			try {
				version[i] = Integer.parseInt(array[i]);
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if(version[0] == 0 && version[1] == 0 && version[2] == 0 && version[3] == 0) {
			return null;
		}
		return version;
	}

	public static boolean isRunningAsUWP() {
		if(isRunningAsUWP == null) {
			isRunningAsUWP = Files.exists(Datastore.getApplicationDirectory().resolve("AppxManifest.xml"));
		}
		return isRunningAsUWP;
	}

	public static String getLicense() throws IOException {
		if(license == null) {
			InputStream is = null;
			BufferedReader reader = null;

			if(reader == null) {
				Class<?> cls = Datastore.class;
				String name = "/" + cls.getPackageName().replace('.', '/') + "/LICENSE.txt";
				is = cls.getResourceAsStream(name);
				if(is != null) {
					reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
				}
			}
			if(reader == null) {
				Path path = getApplicationDirectory().resolve("LICENSE.txt");
				if(Files.isReadable(path)) {
					reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				}
			}
			if(reader != null) {
				try {
					StringBuilder sb = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null) {
						sb.append(line);
						sb.append("\r\n");
					}
					license = sb.toString();
				} finally {
					reader.close();
					if(is != null) {
						is.close();
					}
				}
			}
			if(license == null) {
				license = "";
			}
		}
		return license.isEmpty() ? null : license;
	}
}
