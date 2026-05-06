package edu.hitsz;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {

    private static final String PREF_NAME = "aircraft_war_session";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences preferences;

    public UserSessionManager(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void login(String username) {
        preferences.edit().putString(KEY_USERNAME, username).apply();
    }

    public void logout() {
        preferences.edit().remove(KEY_USERNAME).apply();
    }

    public String getCurrentUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public boolean isLoggedIn() {
        return !getCurrentUsername().isEmpty();
    }
}
