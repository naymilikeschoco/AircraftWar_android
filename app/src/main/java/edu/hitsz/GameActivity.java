package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.android.GameView;

public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 取出CoverActivity传来的难度
        Intent intent = getIntent();
        int difficulty = intent.getIntExtra("difficulty", 1); // 默认1=简单

        GameView gameView = new GameView(this, null, difficulty);
        setContentView(gameView);
    }
}