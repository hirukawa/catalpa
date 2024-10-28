package onl.oss.catalpa.gui;

import com.sun.nio.file.ExtendedWatchEventModifier;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class FileWatchService {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final WatchEvent.Kind<Path>[] events = new WatchEvent.Kind[] {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
    };

    private static final WatchEvent.Modifier[] modifiers = new WatchEvent.Modifier[] {
            ExtendedWatchEventModifier.FILE_TREE
    };

    private Consumer<List<Path>> consumer;
    private Path path;
    private WatchService watcher;
    private Thread worker;
    private final Set<Candidate> candidates = new HashSet<>();

    public FileWatchService(Consumer<List<Path>> consumer) {
        this.consumer = consumer;
    }

    public Path getPath() {
        return path;
    }

    public void start(Path path) throws IOException, InterruptedException {
        stop();

        this.path = path;

        if (path == null) {
            return;
        }

        watcher = FileSystems.getDefault().newWatchService();
        path.register(watcher, events, modifiers);

        worker = Thread.ofVirtual().start(() -> {
            for (;;) {
                WatchKey key = null;
                try {
                    key = watcher.take();
                    synchronized (candidates) {
                        for (WatchEvent<?> e : key.pollEvents()) {
                            if (e.kind() == StandardWatchEventKinds.ENTRY_MODIFY
                                    || e.kind() == StandardWatchEventKinds.ENTRY_CREATE
                                    || e.kind() != StandardWatchEventKinds.ENTRY_DELETE) {
                                @SuppressWarnings("unchecked")
                                WatchEvent<Path> event = (WatchEvent<Path>) e;
                                Candidate candidate = new Candidate(event.context());
                                candidates.remove(candidate);
                                candidates.add(candidate);
                            }
                        }
                        Thread.ofVirtual().start(() -> {
                            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                            synchronized (candidates) {
                                if (candidates.isEmpty()) {
                                    return;
                                }

                                long now = System.nanoTime();
                                for (Candidate candidate : candidates) {
                                    if (now - candidate.nanoTime < 100 * 1000000) {
                                        // candidates の要素に変更されてから 100ミリ秒未満の要素があれば通知しません。
                                        return;
                                    }
                                }

                                // candidates の要素すべてが変更されてから 100ミリ秒以上経過している。
                                synchronized (FileWatchService.this) {
                                    if (FileWatchService.this.path != null) {
                                        List<Path> list = new ArrayList<>();
                                        for (Candidate candidate : candidates) {
                                            list.add(FileWatchService.this.path.resolve(candidate.path));
                                        }
                                        consumer.accept(list);
                                    }
                                }
                                candidates.clear();
                            }
                        });
                    }
                } catch (ClosedWatchServiceException | InterruptedException ignored) {
                    //
                } finally {
                    if (key != null) {
                        key.reset();
                    }
                }
                synchronized (this) {
                    if (this.path == null) {
                        break;
                    }
                }
            }
        });
    }

    public void stop() throws IOException, InterruptedException {
        synchronized (this) {
            this.path = null;
        }

        if (worker != null) {
            worker.interrupt();
            worker.join();
            worker = null;
        }

        if (watcher != null) {
            watcher.close();
            watcher = null;
        }
    }

    private static class Candidate {

        public long nanoTime;
        public Path path;

        public Candidate(Path path) {
            this.nanoTime = System.nanoTime();
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Candidate candidate = (Candidate) o;
            return Objects.equals(path, candidate.path);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(path);
        }

        @Override
        public String toString() {
            return "Candidate{" +
                    "nanoTime=" + nanoTime +
                    ", path=" + path +
                    '}';
        }
    }
}
