package edu.hitsz.android;

public interface GameOverListener {
    void onGameOver(int score, String difficulty, int remoteScore, boolean onlineMode);
}
