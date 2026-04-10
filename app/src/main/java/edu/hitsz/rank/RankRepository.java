package edu.hitsz.rank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RankRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RankDatabaseHelper dbHelper;

    public RankRepository(Context context) {
        this.dbHelper = new RankDatabaseHelper(context.getApplicationContext());
    }

    public void insertRecord(String playerName, int score, String difficulty) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RankDatabaseHelper.COLUMN_PLAYER_NAME, playerName);
        values.put(RankDatabaseHelper.COLUMN_SCORE, score);
        values.put(RankDatabaseHelper.COLUMN_DIFFICULTY, difficulty);
        values.put(RankDatabaseHelper.COLUMN_RECORD_TIME, LocalDateTime.now().format(FORMATTER));
        db.insert(RankDatabaseHelper.TABLE_NAME, null, values);
    }

    public List<ScoreRecord> getAllRecords() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ScoreRecord> records = new ArrayList<>();
        try (Cursor cursor = db.query(
                RankDatabaseHelper.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                RankDatabaseHelper.COLUMN_SCORE + " DESC, " + RankDatabaseHelper.COLUMN_RECORD_TIME + " DESC")) {
            while (cursor.moveToNext()) {
                records.add(new ScoreRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_PLAYER_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_SCORE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_DIFFICULTY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_RECORD_TIME))
                ));
            }
        }
        return records;
    }

    public void deleteRecord(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(RankDatabaseHelper.TABLE_NAME, RankDatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
