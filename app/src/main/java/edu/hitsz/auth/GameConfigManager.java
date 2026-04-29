package edu.hitsz.auth;

import android.content.Context;
import android.content.SharedPreferences;

public final class GameConfigManager {

    private static final String PREF_NAME = "game_config";
    private static final String KEY_UNLOCKED_DIFFICULTY = "unlocked_difficulty";
    private static final String KEY_COINS = "coins";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";

    private GameConfigManager() {
    }

    public static GameConfigState load(Context context) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new GameConfigState(
                sp.getInt(KEY_UNLOCKED_DIFFICULTY, 1),
                sp.getInt(KEY_COINS, 0),
                sp.getBoolean(KEY_AUDIO_ENABLED, true)
        );
    }

    public static void save(Context context, GameConfigState state) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putInt(KEY_UNLOCKED_DIFFICULTY, Math.max(1, state.unlockedDifficulty))
                .putInt(KEY_COINS, Math.max(0, state.coins))
                .putBoolean(KEY_AUDIO_ENABLED, state.audioEnabled)
                .apply();
    }
}
