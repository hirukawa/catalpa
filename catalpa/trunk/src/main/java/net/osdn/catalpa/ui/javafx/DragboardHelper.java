package net.osdn.catalpa.ui.javafx;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javafx.scene.input.Dragboard;

public class DragboardHelper {
	
	public static boolean hasOneDirectory(Dragboard db) {
		if(db.hasFiles()) {
			List<File> files = db.getFiles();
			if(files != null && files.size() == 1) {
				if(files.get(0).isDirectory()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static Path getDirectory(Dragboard db) {
		if(db.hasFiles()) {
			List<File> files = db.getFiles();
			if(files != null && files.size() == 1) {
				File file = files.get(0);
				if(file.isDirectory()) {
					return file.toPath();
				}
			}
		}
		return null;
	}

}
