package net.osdn.catalpa.ui.javafx;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedHashSet;
import java.util.Set;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;

import com.sun.nio.file.ExtendedWatchEventModifier;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

public class FileWatchService extends ScheduledService<Path[]> {

	@SuppressWarnings("unchecked")
	private static final Kind<Path>[] events = new Kind[] {
		StandardWatchEventKinds.ENTRY_CREATE,
		StandardWatchEventKinds.ENTRY_DELETE,
		StandardWatchEventKinds.ENTRY_MODIFY
	};
	
	private static final Modifier[] modifiers = new Modifier[] {
		ExtendedWatchEventModifier.FILE_TREE
	};

	private ObjectProperty<Path> pathProperty = new SimpleObjectProperty<Path>();
	private WatchService watcher;
	private Path dir;
	
	public ObjectProperty<Path> pathProperty() {
		return pathProperty;
	}
	
	public FileWatchService() {
		pathProperty.addListener((observable, oldValue, newValue)-> {
			try {
				restart(newValue);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	public void restart(Path dir) throws IOException {
		if(this.dir == null || dir == null || !Files.isSameFile(this.dir, dir)) {
			if(watcher != null) {
				cancel();
				watcher.close();
				watcher = null;
			}
			if(dir != null) {
				watcher = FileSystems.getDefault().newWatchService();
				dir.register(watcher, events, modifiers);
			}
			this.dir = dir;
		}
		if(this.dir != null) {
			restart();
		}
	}
	
	@Override
	protected Task<Path[]> createTask() {
		return new Task<Path[]>() {
			@Override
			protected Path[] call() throws Exception {
				return FileWatchService.this.call();
			}
		};
	}
	
	protected Path[] call() throws InterruptedException {
		WatchKey key = null;
		try {
			Set<Path> result = new LinkedHashSet<Path>();
			key = watcher.take();
			for(WatchEvent<?> e : key.pollEvents()) {
				if(e.kind() != StandardWatchEventKinds.ENTRY_CREATE
						&& e.kind() != StandardWatchEventKinds.ENTRY_DELETE
						&& e.kind() != StandardWatchEventKinds.ENTRY_MODIFY) {
					continue;
				}
				@SuppressWarnings("unchecked")
				WatchEvent<Path> event = (WatchEvent<Path>)e;
				result.add(event.context());
			}
			return result.toArray(new Path[]{});
		} finally {
			if(key != null) {
				key.reset();
			}
		}
	}
}
