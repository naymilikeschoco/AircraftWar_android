package edu.hitsz.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkClient {

    private static final String TAG = "NetworkClient";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int HEARTBEAT_INTERVAL_MS = 5000;

    private final String serverIp;
    private final int serverPort;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final Object closeLock = new Object();
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);

    private volatile NetworkEventListener listener;
    private volatile boolean running = false;
    private volatile String username = "";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;
    private Thread heartbeatThread;

    public NetworkClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void setNetworkEventListener(NetworkEventListener listener) {
        this.listener = listener;
    }

    public void connectAndLogin(String username) {
        this.username = username == null ? "" : username.trim();
        Thread connectThread = new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIp, serverPort), CONNECT_TIMEOUT_MS);
                reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8));
                running = true;
                startReaderLoop();
                startHeartbeatLoop();
                sendMessage(ProtocolMessage.of("LOGIN").put("username", this.username));
            } catch (IOException e) {
                Log.e(TAG, "connect failed", e);
                notifyDisconnected("Connect failed: " + e.getMessage());
                shutdown(false);
            }
        }, "network-connect");
        connectThread.start();
    }

    public void sendScoreUpdate(int score) {
        sendMessage(ProtocolMessage.of("SCORE_UPDATE").put("score", score));
    }

    public void sendDead(int finalScore) {
        sendMessage(ProtocolMessage.of("DEAD").put("score", finalScore));
    }

    public void disconnect() {
        if (!running && socket == null) {
            shutdown(false);
            return;
        }
        sendDirectly(ProtocolMessage.of("DISCONNECT").put("username", username));
        shutdown(false);
    }

    private void startReaderLoop() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleServerMessage(ProtocolMessage.parse(line));
                }
                if (running) {
                    notifyDisconnected("Server connection closed.");
                }
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "reader loop failed", e);
                    notifyDisconnected("Connection interrupted: " + e.getMessage());
                }
            } finally {
                shutdown(false);
            }
        }, "network-reader");
        readerThread.start();
    }

    private void startHeartbeatLoop() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                    sendMessage(ProtocolMessage.of("HEARTBEAT").put("username", username));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "network-heartbeat");
        heartbeatThread.start();
    }

    private void handleServerMessage(ProtocolMessage message) {
        String type = message.getType();
        NetworkEventListener currentListener = listener;
        if (currentListener == null) {
            if ("LOGIN_RESULT".equals(type) && message.getBoolean("success", false)) {
                sendMessage(ProtocolMessage.of("JOIN_ROOM").put("username", username));
            }
            return;
        }

        switch (type) {
            case "LOGIN_RESULT":
                if (message.getBoolean("success", false)) {
                    currentListener.onLoginSuccess();
                    sendMessage(ProtocolMessage.of("JOIN_ROOM").put("username", username));
                } else {
                    currentListener.onLoginFailed(message.getString("reason"));
                    shutdown(false);
                }
                break;
            case "MATCHED":
                currentListener.onMatched(message.getString("opponent"));
                break;
            case "SCORE_UPDATE":
                currentListener.onOpponentScoreChanged(message.getInt("score", 0));
                break;
            case "OPPONENT_DEAD":
                currentListener.onOpponentDead(message.getInt("score", 0));
                break;
            case "GAME_OVER":
                currentListener.onGameOver(
                        message.getInt("selfScore", 0),
                        message.getInt("opponentScore", 0),
                        message.getString("result"),
                        message.getString("opponent")
                );
                break;
            case "DISCONNECTED":
                notifyDisconnected(message.getString("reason"));
                shutdown(false);
                break;
            case "WAITING":
            default:
                break;
        }
    }

    private void sendMessage(ProtocolMessage message) {
        if (message == null || !running || sendExecutor.isShutdown()) {
            return;
        }
        try {
            sendExecutor.execute(() -> sendDirectly(message));
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void sendDirectly(ProtocolMessage message) {
        synchronized (closeLock) {
            if (writer == null) {
                return;
            }
            try {
                writer.write(message.serialize());
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "send failed", e);
                    notifyDisconnected("Send failed: " + e.getMessage());
                    shutdown(false);
                }
            }
        }
    }

    private void notifyDisconnected(String reason) {
        if (!disconnectNotified.compareAndSet(false, true)) {
            return;
        }
        NetworkEventListener currentListener = listener;
        if (currentListener != null) {
            currentListener.onDisconnected(
                    reason == null || reason.isEmpty() ? "Connection closed." : reason
            );
        }
    }

    private void shutdown(boolean notifyDisconnect) {
        synchronized (closeLock) {
            running = false;
            if (heartbeatThread != null) {
                heartbeatThread.interrupt();
                heartbeatThread = null;
            }
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
            reader = null;
            writer = null;
            socket = null;
            sendExecutor.shutdownNow();
        }
        if (notifyDisconnect) {
            notifyDisconnected("Connection closed.");
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
