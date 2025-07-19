package tech.catheu.jeamlit.watcher;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FileWatcher {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);
    
    private final Path watchedFile;
    private final Consumer<Path> onFileChange;
    private DirectoryWatcher watcher;
    private CompletableFuture<Void> watcherFuture;
    
    public FileWatcher(String filePath, Consumer<Path> onFileChange) {
        this.watchedFile = Paths.get(filePath).toAbsolutePath();
        this.onFileChange = onFileChange;
    }
    
    public void start() throws IOException {
        if (watcher != null) {
            throw new IllegalStateException("FileWatcher is already running");
        }
        
        Path directory = watchedFile.getParent();
        
        logger.info("Starting file watcher for: {}", watchedFile);
        logger.info("Watching directory: {}", directory);
        
        watcher = DirectoryWatcher.builder()
            .path(directory)
            .listener(new DirectoryChangeListener() {
                @Override
                public void onEvent(DirectoryChangeEvent event) {
                    Path changedFile = event.path();
                    
                    // Only respond to changes to our specific file
                    if (changedFile.equals(watchedFile)) {
                        if (event.eventType() == DirectoryChangeEvent.EventType.MODIFY) {
                            logger.debug("File changed: {}", changedFile);
                            onFileChange.accept(changedFile);
                        }
                    }
                }
            })
            .build();
        
        watcherFuture = watcher.watchAsync();
        logger.info("File watcher started successfully");
    }
    
    public void stop() {
        if (watcher != null) {
            try {
                watcher.close();
                if (watcherFuture != null) {
                    watcherFuture.cancel(true);
                }
            } catch (IOException e) {
                logger.error("Error stopping file watcher", e);
            } finally {
                watcher = null;
                watcherFuture = null;
            }
        }
    }
}