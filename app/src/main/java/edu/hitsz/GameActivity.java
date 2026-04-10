package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.android.GameView;
import edu.hitsz.rank.RankRepository;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private boolean resultHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int difficulty = intent.getIntExtra("difficulty", 1);
        gameView = new GameView(this, null, difficulty);
        gameView.setGameOverListener(this::handleGameOver);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onPauseView();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onResumeView();
        }
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.release();
        }
        super.onDestroy();
    }

    private void handleGameOver(int score, String difficulty) {
        if (resultHandled || isFinishing()) {
            return;
        }
        resultHandled = true;

        EditText input = new EditText(this);
        input.setHint("Player name");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Score: " + score + "\nEnter your name to save the record.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save", (dialog, which) -> {
                    String playerName = input.getText().toString().trim();
                    if (playerName.isEmpty()) {
                        playerName = "Player";
                    }
                    new RankRepository(this).insertRecord(playerName, score, difficulty);
                    openRankings();
                })
                .show();
    }

    private void openRankings() {
        Intent intent = new Intent(this, RankActivity.class);
        startActivity(intent);
        finish();
    }
}
