package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.android.GameView;
import edu.hitsz.android.OnlineGameEventListener;
import edu.hitsz.network.NetworkClient;
import edu.hitsz.network.NetworkEventListener;
import edu.hitsz.network.OnlineSessionManager;
import edu.hitsz.rank.RankRepository;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_ONLINE_MODE = "online_mode";
    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_OPPONENT_NAME = "opponent_name";

    private GameView gameView;
    private boolean resultHandled = false;
    private boolean onlineMode = false;
    private NetworkClient networkClient;
    private String username = "Player";
    private String opponentName = "Opponent";
    private int opponentScore = 0;
    private String onlineResultText = "Draw";
    private boolean localDeadSent = false;
    private boolean scoreSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int difficulty = intent.getIntExtra(EXTRA_DIFFICULTY, 1);
        onlineMode = intent.getBooleanExtra(EXTRA_ONLINE_MODE, false);
        String sessionUser = new UserSessionManager(this).getCurrentUsername();
        username = sessionUser.isEmpty()
                ? (intent.getStringExtra(EXTRA_USERNAME) == null
                ? "Player" : intent.getStringExtra(EXTRA_USERNAME))
                : sessionUser;
        opponentName = intent.getStringExtra(EXTRA_OPPONENT_NAME) == null
                ? "Opponent" : intent.getStringExtra(EXTRA_OPPONENT_NAME);

        if (onlineMode) {
            networkClient = OnlineSessionManager.getInstance().getNetworkClient();
            if (networkClient == null) {
                Toast.makeText(this,
                        "Online session expired. Please log in again.",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        gameView = new GameView(this, null, difficulty, onlineMode);
        if (onlineMode) {
            bindOnlineBattle();
        }
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
        if (onlineMode && networkClient != null) {
            if (isFinishing()) {
                OnlineSessionManager.getInstance().clearSession();
            } else {
                networkClient.setNetworkEventListener(null);
            }
        }
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

        if (onlineMode) {
            showOnlineResultDialog(score);
            return;
        }

        saveScoreIfNeeded(score, difficulty);

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Score: " + score + "\nSaved to ranking as " + username + ".")
                .setCancelable(false)
                .setPositiveButton("Rankings", (dialog, which) -> openRankings())
                .show();
    }

    private void bindOnlineBattle() {
        gameView.setOpponentName(opponentName);
        gameView.setOnlineGameEventListener(new OnlineGameEventListener() {
            @Override
            public void onScoreChanged(int score) {
                if (networkClient != null) {
                    networkClient.sendScoreUpdate(score);
                }
            }

            @Override
            public void onLocalPlayerDead(int finalScore, String difficulty) {
                if (localDeadSent) {
                    return;
                }
                localDeadSent = true;
                if (networkClient != null) {
                    networkClient.sendDead(finalScore);
                }
                runOnUiThread(() -> Toast.makeText(
                        GameActivity.this,
                        "You are down. Waiting for opponent...",
                        Toast.LENGTH_SHORT
                ).show());
            }
        });

        networkClient.setNetworkEventListener(new NetworkEventListener() {
            @Override
            public void onLoginSuccess() {
                // handled in setup activity
            }

            @Override
            public void onLoginFailed(String reason) {
                handleDisconnect(reason.isEmpty() ? "Login expired" : reason);
            }

            @Override
            public void onMatched(String opponent) {
                // already matched before entering the game activity
            }

            @Override
            public void onOpponentScoreChanged(int score) {
                runOnUiThread(() -> {
                    opponentScore = score;
                    gameView.updateOpponentScore(score);
                });
            }

            @Override
            public void onOpponentDead(int finalScore) {
                runOnUiThread(() -> {
                    opponentScore = finalScore;
                    gameView.markOpponentDead(finalScore);
                    Toast.makeText(GameActivity.this,
                            "Opponent is down.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onGameOver(int selfScore, int finalOpponentScore, String result,
                                   String opponent) {
                runOnUiThread(() -> {
                    opponentScore = finalOpponentScore;
                    opponentName = (opponent == null || opponent.isEmpty())
                            ? opponentName : opponent;
                    onlineResultText = resolveResultText(result);
                    gameView.setOpponentName(opponentName);
                    // Wait for the server to confirm the full battle result.
                    gameView.showOnlineGameOver(selfScore, finalOpponentScore, onlineResultText);
                });
            }

            @Override
            public void onDisconnected(String reason) {
                handleDisconnect(reason.isEmpty() ? "Connection closed" : reason);
            }
        });
    }

    private void handleDisconnect(String reason) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            OnlineSessionManager.getInstance().clearSessionWithoutDisconnect();
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
            openCover();
        });
    }

    private void showOnlineResultDialog(int score) {
        saveScoreIfNeeded(score, getIntentDifficultyName());
        new AlertDialog.Builder(this)
                .setTitle("Battle Result")
                .setMessage("Player: " + username
                        + "\nOpponent: " + opponentName
                        + "\nMy score: " + score
                        + "\nOpponent score: " + opponentScore
                        + "\nResult: " + onlineResultText
                        + "\nSaved to ranking as " + username + ".")
                .setCancelable(false)
                .setPositiveButton("Back", (dialog, which) -> openCover())
                .show();
    }

    private void saveScoreIfNeeded(int score, String difficulty) {
        if (scoreSaved) {
            return;
        }
        String currentUser = new UserSessionManager(this).getCurrentUsername();
        if (currentUser.isEmpty()) {
            currentUser = username;
        }
        scoreSaved = true;
        new RankRepository(this).insertRecord(currentUser, score, difficulty);
    }

    private String getIntentDifficultyName() {
        int difficulty = getIntent().getIntExtra(EXTRA_DIFFICULTY, 1);
        switch (difficulty) {
            case 1:
                return "Easy";
            case 3:
                return "Hard";
            case 2:
            default:
                return "Normal";
        }
    }

    private String resolveResultText(String result) {
        if ("WIN".equalsIgnoreCase(result)) {
            return "Win";
        }
        if ("LOSE".equalsIgnoreCase(result)) {
            return "Lose";
        }
        return "Draw";
    }

    private void openCover() {
        Intent intent = new Intent(this, CoverActivity.class);
        startActivity(intent);
        finish();
    }

    private void openRankings() {
        Intent intent = new Intent(this, RankActivity.class);
        startActivity(intent);
        finish();
    }
}
