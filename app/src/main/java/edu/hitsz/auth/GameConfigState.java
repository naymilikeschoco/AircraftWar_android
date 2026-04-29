package edu.hitsz.auth;

public class GameConfigState {
    public int unlockedDifficulty;
    public int coins;
    public boolean audioEnabled;

    public GameConfigState(int unlockedDifficulty, int coins, boolean audioEnabled) {
        this.unlockedDifficulty = unlockedDifficulty;
        this.coins = coins;
        this.audioEnabled = audioEnabled;
    }
}
