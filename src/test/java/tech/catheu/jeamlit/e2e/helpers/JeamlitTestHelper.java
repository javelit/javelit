/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.catheu.jeamlit.e2e.helpers;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.core.Server;

/**
 * Helper class for Jeamlit E2E tests.
 * Provides utilities for creating test apps, starting servers, and managing test lifecycle.
 */
public final class JeamlitTestHelper {

    private static final Logger LOG = LoggerFactory.getLogger(JeamlitTestHelper.class);

    /// Write a test app to a temporary file.
    ///
    /// @param appCode The Java source code for the test app
    /// @return Path to the created file
    public static Path writeTestApp(final String appCode) {
        try {
            final Path tempDir = Files.createTempDirectory("jeamlit-test-");
            final Path appFile = tempDir.resolve("TestApp.java");
            Files.writeString(appFile, appCode);
            return appFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write test app", e);
        }
    }
    
    /**
     * Start a Jeamlit server for the given app.
     * 
     * @param appFile Path to the app Java file
     * @return The started Server instance
     */
    public static Server startServer(final Path appFile) {
        final int port = PortAllocator.getNextAvailablePort();
        try {
            // Create server instance
            final Server server = new Server(appFile, null, port, null);
            
            // Start server in a separate thread
            final Thread serverThread = new Thread(() -> {
                try {
                    server.start();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to start server", e);
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
            // Wait for server to be ready
            waitForServerReady(port);
            
            return server;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Jeamlit server", e);
        }
    }
    
    /**
     * Stop a Jeamlit server.
     * 
     * @param server The server to stop
     */
    public static void stopServer(final Server server) {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // Log but don't throw - we're cleaning up
                LOG.error("Error stopping server. ", e);
            }
        }
    }
    
    /**
     * Wait for the server to be ready to accept connections.
     * 
     * @param port The port to check
     */
    private static void waitForServerReady(int port) {
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                // Try to connect to the server
                Socket socket = new Socket("localhost", port);
                socket.close();
                return;
            } catch (IOException e) {
                // Server not ready yet
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for server", ie);
                }
            }
        }
        throw new RuntimeException("Server did not start within timeout");
    }
    
    /**
     * Clean up a temporary directory and its contents.
     * 
     * @param dir The directory to delete
     */
    public static void cleanupTempDir(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try (final var paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder())// Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path);
                        }
                    });
            } catch (IOException e) {
                System.err.println("Failed to cleanup temp dir: " + e.getMessage());
            }
        }
    }

    private JeamlitTestHelper() {
    }
}
