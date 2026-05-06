package edu.hitsz.network;

public class OnlineSessionManager {

    private static final OnlineSessionManager INSTANCE = new OnlineSessionManager();

    private NetworkClient networkClient;
    private String username;
    private String opponentName;
    private String serverIp;
    private int serverPort;
    private int difficulty;

    private OnlineSessionManager() {
    }

    public static OnlineSessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void replaceSession(String serverIp, int serverPort,
                                            String username, int difficulty,
                                            NetworkClient networkClient) {
        clearSession();
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.username = username;
        this.difficulty = difficulty;
        this.networkClient = networkClient;
    }

    public synchronized NetworkClient getNetworkClient() {
        return networkClient;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized int getDifficulty() {
        return difficulty;
    }

    public synchronized void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public synchronized String getOpponentName() {
        return opponentName;
    }

    public synchronized void clearSession() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        clearSessionWithoutDisconnect();
    }

    public synchronized void clearSessionWithoutDisconnect() {
        networkClient = null;
        username = null;
        opponentName = null;
        serverIp = null;
        serverPort = 0;
        difficulty = 0;
    }
}
