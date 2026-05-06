package edu.hitsz.android;

public interface OnlineGameEventListener {
    void onScoreChanged(int score);

    void onLocalPlayerDead(int finalScore, String difficulty);
}
