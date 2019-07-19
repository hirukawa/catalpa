package net.osdn.catalpa.ui.javafx;

import java.io.IOException;
import java.nio.file.Path;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class VSCode {
	private static String path;
	
	static {
		try {
			path = findPath("code.cmd");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public interface Shlwapi extends Library {
		Shlwapi INSTANCE = (Shlwapi)Native.load("shlwapi", Shlwapi.class);
		
		boolean PathFindOnPathW(char[] pszPath, Pointer ppszOtherDirs);
    }

	private static String findPath(String executableFilename) {
		char[] pszPath = new char[1024];
		Pointer ppszOtherDirs = Pointer.NULL;
		
		char[] src = executableFilename.toCharArray();
		System.arraycopy(src, 0, pszPath, 0, src.length);
		boolean ret = Shlwapi.INSTANCE.PathFindOnPathW(pszPath, ppszOtherDirs);
		if(ret) {
			return new String(pszPath).trim();
		} else {
			return null;
		}
	}
	
	public static boolean isInstalled() {
		return (path != null);
	}
	
	public static String getPath() {
		return path;
	}
	
	public static void open(Path dir) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(path, dir.toAbsolutePath().toString());
		pb.start();
	}
}
