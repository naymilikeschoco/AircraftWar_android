package edu.hitsz.network;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineMatchClient {

    public interface Listener {
        void onConnected();

        void onMatchReady();

        void onRemoteScoreChanged(int score);

        void onRemoteDead();

        void onDisconnected(String reason);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Listener listener;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean closed = false;

    public OnlineMatchClient(Listener listener) {
        this.listener = listener;
    }

    public void join(String host, int port) {
        ioExecutor.execute(() -> {
            try {
                socket = new Socket(host, port);
                setupStreamsAndReadLoop();
            } catch (IOException e) {
                if (!closed) {
                    notifyDisconnected("Join failed: " + e.getMessage());
                }
            }
        });
    }

    public void sendScore(int score) {
        sendLine("SCORE:" + score);
    }

    public void sendDead() {
        sendLine("DEAD:1");
    }

    public void close() {
        closed = true;
        ioExecutor.execute(() -> {
            closeQuietly();
            ioExecutor.shutdownNow();
        });
    }

    private void setupStreamsAndReadLoop() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        notifyConnected();

        String line;
        while (!closed && (line = reader.readLine()) != null) {
            handleRemoteLine(line);
        }
        if (!closed) {
            notifyDisconnected("Connection closed");
        }
        closeQuietly();
    }

    private void handleRemoteLine(String line) {
        if ("READY".equalsIgnoreCase(line.trim())) {
            mainHandler.post(listener::onMatchReady);
            return;
        }
        if (line.startsWith("SCORE:")) {
            String value = line.substring("SCORE:".length()).trim();
            try {
                int score = Integer.parseInt(value);
                mainHandler.post(() -> listener.onRemoteScoreChanged(score));
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if (line.startsWith("DEAD:")) {
            mainHandler.post(listener::onRemoteDead);
        }
    }

    private void sendLine(String line) {
        ioExecutor.execute(() -> {
            if (closed || writer == null) {
                return;
            }
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                if (!closed) {
                    notifyDisconnected("Send failed: " + e.getMessage());
                }
            }
        });
    }

    private void notifyConnected() {
        mainHandler.post(listener::onConnected);
    }

    private void notifyDisconnected(String reason) {
        mainHandler.post(() -> listener.onDisconnected(reason));
    }

    private void closeQuietly() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
