package edu.hitsz.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {

    private static final int DEFAULT_PORT = 9999;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int port;
    private final Set<String> activeUsernames = ConcurrentHashMap.newKeySet();
    private final Queue<PlayerSession> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger roomIdGenerator = new AtomicInteger(1);
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private final Object matchLock = new Object();

    private ServerSocket serverSocket;

    public GameServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid port, fallback to 9999");
            }
        }

        try {
            new GameServer(port).start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        log("Server start, listening to " + port);
        while (true) {
            Socket socket = serverSocket.accept();
            PlayerSession session = new PlayerSession(socket);
            log("Client in :" + session.remoteAddress);
            clientExecutor.execute(() -> handleClient(session));
        }
    }

    private void handleClient(PlayerSession session) {
        try {
            String line;
            while ((line = session.reader.readLine()) != null) {
                handleMessage(session, ProtocolMessage.parse(line));
            }
        } catch (IOException e) {
            log("Connection failed:" + session.displayName() + " - " + e.getMessage());
        } finally {
            cleanupSession(session, "Connection disabled");
        }
    }

    private void handleMessage(PlayerSession session, ProtocolMessage message) {
        switch (message.getType()) {
            case "LOGIN":
                handleLogin(session, message.getString("username"));
                break;
            case "JOIN_ROOM":
                handleJoinRoom(session);
                break;
            case "SCORE_UPDATE":
                handleScoreUpdate(session, message.getInt("score", 0));
                break;
            case "DEAD":
                handleDead(session, message.getInt("score", 0));
                break;
            case "DISCONNECT":
                cleanupSession(session, "player leaves");
                break;
            case "HEARTBEAT":
                session.lastHeartbeatAt = System.currentTimeMillis();
                break;
            default:
                break;
        }
    }

    private void handleLogin(PlayerSession session, String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (username.isEmpty()) {
            session.send(ProtocolMessage.of("LOGIN_RESULT")
                    .put("success", false)
                    .put("reason", "username can't be null"));
            return;
        }

        synchronized (matchLock) {
            if (session.loggedIn) {
                session.send(ProtocolMessage.of("LOGIN_RESULT").put("success", true));
                return;
            }
            if (activeUsernames.contains(username)) {
                session.send(ProtocolMessage.of("LOGIN_RESULT")
                        .put("success", false)
                        .put("reason", "username has been taken"));
                return;
            }
            activeUsernames.add(username);
            session.username = username;
            session.loggedIn = true;
        }

        log("Logging in succeed" + username);
        session.send(ProtocolMessage.of("LOGIN_RESULT").put("success", true));
    }

    private void handleJoinRoom(PlayerSession session) {
        if (!session.loggedIn || session.isClosed()) {
            return;
        }

        synchronized (matchLock) {
            if (session.room != null) {
                return;
            }

            PlayerSession waitingPlayer;
            while ((waitingPlayer = waitingPlayers.poll()) != null) {
                if (waitingPlayer == session || waitingPlayer.isClosed() || waitingPlayer.room != null) {
                    continue;
                }

                int roomId = roomIdGenerator.getAndIncrement();
                Room room = new Room(roomId, waitingPlayer, session);
                waitingPlayer.room = room;
                session.room = room;
                rooms.put(roomId, room);

                log("Matched:Room #" + roomId + "，" + waitingPlayer.username + " VS " + session.username);
                waitingPlayer.send(ProtocolMessage.of("MATCHED")
                        .put("roomId", roomId)
                        .put("opponent", session.username));
                session.send(ProtocolMessage.of("MATCHED")
                        .put("roomId", roomId)
                        .put("opponent", waitingPlayer.username));
                return;
            }

            waitingPlayers.offer(session);
            log("player in waiting line" + session.username);
            session.send(ProtocolMessage.of("WAITING").put("message", "waiting"));
        }
    }

    private void handleScoreUpdate(PlayerSession session, int score) {
        Room room = session.room;
        if (room == null || room.gameOverSent) {
            return;
        }
        PlayerSession opponent = room.getOpponent(session);
        if (opponent == null || opponent.isClosed()) {
            return;
        }

        log("score updated" + session.username + " -> " + score);
        opponent.send(ProtocolMessage.of("SCORE_UPDATE").put("score", score));
    }

    private void handleDead(PlayerSession session, int finalScore) {
        Room room = session.room;
        if (room == null) {
            return;
        }

        session.dead = true;
        session.finalScore = finalScore;
        log("player died: " + session.username + ", ultimate score: " + finalScore);

        PlayerSession opponent = room.getOpponent(session);
        if (opponent != null && !opponent.isClosed()) {
            opponent.send(ProtocolMessage.of("OPPONENT_DEAD").put("score", finalScore));
        }

        if (opponent != null && opponent.dead) {
            broadcastGameOver(room);
        }
    }

    private void broadcastGameOver(Room room) {
        synchronized (room) {
            if (room.gameOverSent) {
                return;
            }
            room.gameOverSent = true;
        }

        PlayerSession first = room.playerA;
        PlayerSession second = room.playerB;
        log("Game over, Room #" + room.roomId + "，"
                + first.username + "=" + first.finalScore + "，"
                + second.username + "=" + second.finalScore);

        first.send(ProtocolMessage.of("GAME_OVER")
                .put("selfScore", first.finalScore)
                .put("opponentScore", second.finalScore)
                .put("result", compareScore(first.finalScore, second.finalScore))
                .put("opponent", second.username));

        second.send(ProtocolMessage.of("GAME_OVER")
                .put("selfScore", second.finalScore)
                .put("opponentScore", first.finalScore)
                .put("result", compareScore(second.finalScore, first.finalScore))
                .put("opponent", first.username));

        rooms.remove(room.roomId);
    }

    private String compareScore(int selfScore, int opponentScore) {
        if (selfScore > opponentScore) {
            return "WIN";
        }
        if (selfScore < opponentScore) {
            return "LOSE";
        }
        return "DRAW";
    }

    private void cleanupSession(PlayerSession session, String reason) {
        if (!session.closeGuard.compareAndSet(false, true)) {
            return;
        }

        synchronized (matchLock) {
            waitingPlayers.remove(session);
            if (session.username != null) {
                activeUsernames.remove(session.username);
            }
        }

        Room room = session.room;
        if (room != null) {
            PlayerSession opponent = room.getOpponent(session);
            rooms.remove(room.roomId);
            session.room = null;

            if (opponent != null) {
                opponent.room = null;
                if (!room.gameOverSent && !opponent.isClosed()) {
                    opponent.send(ProtocolMessage.of("DISCONNECTED")
                            .put("reason", session.displayName() + " connection disabled"));
                }
            }
        }

        session.close();
        log("player disconnected: " + session.displayName() + ", Reason:" + reason);
    }

    private void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " + message);
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

    private static class Room {
        private final int roomId;
        private final PlayerSession playerA;
        private final PlayerSession playerB;
        private volatile boolean gameOverSent = false;

        private Room(int roomId, PlayerSession playerA, PlayerSession playerB) {
            this.roomId = roomId;
            this.playerA = playerA;
            this.playerB = playerB;
        }

        private PlayerSession getOpponent(PlayerSession session) {
            if (Objects.equals(playerA, session)) {
                return playerB;
            }
            if (Objects.equals(playerB, session)) {
                return playerA;
            }
            return null;
        }
    }

    private class PlayerSession {
        private final Socket socket;
        private final String remoteAddress;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final AtomicBoolean closeGuard = new AtomicBoolean(false);

        private volatile String username;
        private volatile boolean loggedIn = false;
        private volatile boolean dead = false;
        private volatile int finalScore = 0;
        private volatile long lastHeartbeatAt = System.currentTimeMillis();
        private volatile Room room;

        private PlayerSession(Socket socket) throws IOException {
            this.socket = socket;
            this.remoteAddress = socket.getRemoteSocketAddress().toString();
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private synchronized void send(ProtocolMessage message) {
            if (isClosed()) {
                return;
            }
            try {
                writer.write(message.serialize());
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                cleanupSession(this, "Message transmission failed");
            }
        }

        private boolean isClosed() {
            return closeGuard.get();
        }

        private String displayName() {
            return username == null ? remoteAddress : username;
        }

        private void close() {
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
        }
    }
}