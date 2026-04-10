package edu.hitsz.rank;

public final class DifficultyMapper {

    public static final String EASY = "Easy";
    public static final String NORMAL = "Normal";
    public static final String HARD = "Hard";

    private DifficultyMapper() {
    }

    public static String fromLevel(int level) {
        switch (level) {
            case 1:
                return EASY;
            case 2:
                return NORMAL;
            case 3:
                return HARD;
            default:
                return EASY;
        }
    }
}
