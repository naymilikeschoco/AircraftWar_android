package edu.hitsz.network;

public interface NetworkEventListener {
    void onLoginSuccess();

    void onLoginFailed(String reason);

    void onMatched(String opponentName);

    void onOpponentScoreChanged(int score);

    void onOpponentDead(int finalScore);

    void onGameOver(int selfScore, int opponentScore, String result, String opponentName);

    void onDisconnected(String reason);
}
