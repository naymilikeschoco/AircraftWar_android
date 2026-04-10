package edu.hitsz.rank;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RankDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "aircraft_war_rank.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "rank_records";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PLAYER_NAME = "player_name";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_RECORD_TIME = "record_time";

    public RankDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + " ("
                        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_PLAYER_NAME + " TEXT NOT NULL, "
                        + COLUMN_SCORE + " INTEGER NOT NULL, "
                        + COLUMN_DIFFICULTY + " TEXT NOT NULL, "
                        + COLUMN_RECORD_TIME + " TEXT NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
