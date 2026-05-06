package edu.hitsz.rank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserRepository {

    public enum LoginStatus {
        SUCCESS,
        REGISTERED,
        WRONG_PASSWORD,
        INVALID_INPUT
    }

    private final RankDatabaseHelper dbHelper;

    public UserRepository(Context context) {
        this.dbHelper = new RankDatabaseHelper(context.getApplicationContext());
    }

    public LoginStatus loginOrRegister(String username, String password) {
        String safeUsername = username == null ? "" : username.trim();
        String safePassword = password == null ? "" : password.trim();
        if (safeUsername.isEmpty() || safePassword.isEmpty()) {
            return LoginStatus.INVALID_INPUT;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try (Cursor cursor = db.query(
                RankDatabaseHelper.USER_TABLE_NAME,
                new String[]{RankDatabaseHelper.COLUMN_PASSWORD},
                RankDatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{safeUsername},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                String storedPassword = cursor.getString(
                        cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_PASSWORD));
                return safePassword.equals(storedPassword)
                        ? LoginStatus.SUCCESS
                        : LoginStatus.WRONG_PASSWORD;
            }
        }

        ContentValues values = new ContentValues();
        values.put(RankDatabaseHelper.COLUMN_USERNAME, safeUsername);
        values.put(RankDatabaseHelper.COLUMN_PASSWORD, safePassword);
        db.insert(RankDatabaseHelper.USER_TABLE_NAME, null, values);
        return LoginStatus.REGISTERED;
    }
}
