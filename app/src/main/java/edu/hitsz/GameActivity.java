package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.auth.GameConfigManager;
import edu.hitsz.auth.GameConfigState;
import edu.hitsz.auth.SessionManager;
import edu.hitsz.android.GameView;
import edu.hitsz.network.CloudApiClient;
import edu.hitsz.rank.RankRepository;

public class GameActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080";
    private GameView gameView;
    private boolean resultHandled = false;
    private CloudApiClient cloudApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        cloudApiClient = new CloudApiClient(BASE_URL);
        int difficulty = intent.getIntExtra("difficulty", 1);
        boolean onlineMode = intent.getBooleanExtra("online_mode", false);
        String hostAddress = intent.getStringExtra("host_address");
        int port = intent.getIntExtra("port", 8989);
        gameView = new GameView(this, null, difficulty, onlineMode, hostAddress, port);
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

    private void handleGameOver(int score, String difficulty, int remoteScore, boolean onlineMode) {
        if (resultHandled || isFinishing()) {
            return;
        }
        resultHandled = true;

        EditText input = new EditText(this);
        input.setHint("Player name");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(buildGameOverMessage(score, remoteScore, onlineMode))
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save", (dialog, which) -> {
                    String playerName = input.getText().toString().trim();
                    if (playerName.isEmpty()) {
                        playerName = "Player";
                    }
                    new RankRepository(this).insertRecord(playerName, score, difficulty);
                    syncProgressAfterGame(score, difficulty);
                    openRankings();
                })
                .show();
    }

    private String buildGameOverMessage(int score, int remoteScore, boolean onlineMode) {
        if (onlineMode) {
            return "Your score: " + score +
                    "\nRival score: " + remoteScore +
                    "\nEnter your name to save the record.";
        }
        return "Score: " + score + "\nEnter your name to save the record.";
    }

    private void syncProgressAfterGame(int score, String difficulty) {
        String token = SessionManager.getToken(this);
        boolean loggedIn = token != null && !token.isEmpty();
        GameConfigState state = GameConfigManager.load(this);
        if (loggedIn) {
            state.coins += Math.max(0, score);
        }
        int unlocked = mapDifficultyToLevel(difficulty);
        if (unlocked > state.unlockedDifficulty) {
            state.unlockedDifficulty = unlocked;
        }
        GameConfigManager.save(this, state);

        if (!loggedIn) {
            return;
        }
        cloudApiClient.pushConfig(token, state, new CloudApiClient.SimpleCallback() {
            @Override
            public void onSuccess(String value) {
                // no-op
            }

            @Override
            public void onError(String message) {
                // offline fallback: local config already updated
            }
        });
    }

    private int mapDifficultyToLevel(String difficulty) {
        if ("Hard".equalsIgnoreCase(difficulty)) {
            return 3;
        }
        if ("Normal".equalsIgnoreCase(difficulty)) {
            return 2;
        }
        return 1;
    }

    private void openRankings() {
        Intent intent = new Intent(this, RankActivity.class);
        startActivity(intent);
        finish();
    }
}
