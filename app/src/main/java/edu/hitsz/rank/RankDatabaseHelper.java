package edu.hitsz.rank;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RankDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "aircraft_war_rank.db";
    public static final int DATABASE_VERSION = 2;
    public static final String TABLE_NAME = "rank_records";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PLAYER_NAME = "player_name";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_RECORD_TIME = "record_time";
    public static final String USER_TABLE_NAME = "users";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    public RankDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createRankTable(db);
        createUserTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createUserTable(db);
        }
    }

    private void createRankTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_PLAYER_NAME + " TEXT NOT NULL, "
                        + COLUMN_SCORE + " INTEGER NOT NULL, "
                        + COLUMN_DIFFICULTY + " TEXT NOT NULL, "
                        + COLUMN_RECORD_TIME + " TEXT NOT NULL)"
        );
    }

    private void createUserTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + USER_TABLE_NAME + " ("
                        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_USERNAME + " TEXT NOT NULL UNIQUE, "
                        + COLUMN_PASSWORD + " TEXT NOT NULL)"
        );
    }
}
