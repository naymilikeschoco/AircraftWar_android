package edu.hitsz.audio;

import android.content.Context;
import android.content.SharedPreferences;

public final class AudioSettings {

    private static final String PREF_NAME = "audio_settings";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";

    private AudioSettings() {
    }

    public static boolean isAudioEnabled(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_AUDIO_ENABLED, true);
    }

    public static void setAudioEnabled(Context context, boolean enabled) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply();
    }
}
