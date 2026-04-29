package edu.hitsz.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

public final class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "username";
    private static final String SECRET = "AircraftWarSecret";

    private SessionManager() {
    }

    public static void saveSession(Context context, String username, String token) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_USER, username)
                .putString(KEY_TOKEN, encode(token))
                .apply();
    }

    public static String getToken(Context context) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String encoded = sp.getString(KEY_TOKEN, null);
        if (encoded == null) {
            return null;
        }
        return decode(encoded);
    }

    @SuppressWarnings("unused")
    public static String getUsername(Context context) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER, null);
    }

    @SuppressWarnings("unused")
    public static void clear(Context context) {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    private static String encode(String source) {
        byte[] input = source.getBytes(StandardCharsets.UTF_8);
        byte[] key = SECRET.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            out[i] = (byte) (input[i] ^ key[i % key.length]);
        }
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    private static String decode(String encoded) {
        byte[] input = Base64.decode(encoded, Base64.NO_WRAP);
        byte[] key = SECRET.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            out[i] = (byte) (input[i] ^ key[i % key.length]);
        }
        return new String(out, StandardCharsets.UTF_8);
    }
}
