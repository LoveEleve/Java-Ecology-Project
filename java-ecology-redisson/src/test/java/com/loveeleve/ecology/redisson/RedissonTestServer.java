package com.loveeleve.ecology.redisson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class RedissonTestServer implements AutoCloseable {

    private final Process process;
    private final int port;

    private RedissonTestServer(Process process, int port) {
        this.process = process;
        this.port = port;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    static RedissonTestServer start() {
        int port = findFreePort();
        try {
            Process process = new ProcessBuilder(
                    "redis-server",
                    "--bind", "127.0.0.1",
                    "--port", String.valueOf(port),
                    "--save", "",
                    "--appendonly", "no",
                    "--daemonize", "no")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            RedissonTestServer server = new RedissonTestServer(process, port);
            server.awaitReady();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("redis-server is required for Redisson tests", ex);
        }
    }

    int port() {
        return port;
    }

    String host() {
        return "127.0.0.1";
    }

    private void awaitReady() {
        for (int i = 0; i < 100; i++) {
            if (!process.isAlive()) {
                throw new IllegalStateException("redis-server exited before becoming ready");
            }
            try (Socket socket = new Socket(host(), port);
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {
                writer.write("PING\r\n");
                writer.flush();
                if ("+PONG".equals(reader.readLine())) {
                    return;
                }
            } catch (IOException ignored) {
                // server is still starting
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for redis-server", ex);
            }
        }
        throw new IllegalStateException("redis-server did not become ready on port " + port);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("cannot allocate a free port", ex);
        }
    }

    @Override
    public void close() {
        process.destroy();
        try {
            if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
