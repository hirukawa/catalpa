package onl.oss.catalpa.gui;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class Win32 {

    public interface Shlwapi extends Library {
        Shlwapi INSTANCE = Native.load("shlwapi", Shlwapi.class);

        boolean PathFindOnPathW(char[] pszPath, Pointer ppszOtherDirs);
    }

    public static String findPath(String executableFilename) {
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
}
