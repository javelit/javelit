package tech.catheu.jeamlit.e2e.helpers;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to allocate unique ports for parallel test execution.
 */
class PortAllocator {
    private static final int BASE_PORT = 8501;
    private static final AtomicInteger portOffset = new AtomicInteger(0);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Get the next available port for testing.
     * Tries to find an available port starting from BASE_PORT.
     * Times out after 10 seconds if no port can be found.
     */
    protected static synchronized int getNextAvailablePort() {
        final CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int port = BASE_PORT + portOffset.getAndIncrement();
                if (isPortAvailable(port)) {
                    return port;
                }
                // Small delay to avoid tight loop
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Port allocation interrupted", e);
                }
            }
            throw new RuntimeException("Port allocation interrupted");
        }, executor);
        
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Could not find an available port within 10 seconds timeout", e);
        } catch (InterruptedException | ExecutionException e) {
            future.cancel(true);
            throw new RuntimeException("Error while finding available port", e);
        }
    }
    
    /**
     * Check if a port is available by attempting to create a server socket.
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}