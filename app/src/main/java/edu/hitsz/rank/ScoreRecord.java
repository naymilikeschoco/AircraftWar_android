package edu.hitsz.rank;

public class ScoreRecord {

    private final long id;
    private final String playerName;
    private final int score;
    private final String difficulty;
    private final String recordTime;

    public ScoreRecord(long id, String playerName, int score, String difficulty, String recordTime) {
        this.id = id;
        this.playerName = playerName;
        this.score = score;
        this.difficulty = difficulty;
        this.recordTime = recordTime;
    }

    public long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getRecordTime() {
        return recordTime;
    }
}
