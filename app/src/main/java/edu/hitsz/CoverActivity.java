package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import edu.hitsz.auth.GameConfigManager;
import edu.hitsz.auth.GameConfigState;
import edu.hitsz.auth.SessionManager;
import edu.hitsz.audio.AudioSettings;
import edu.hitsz.network.CloudApiClient;

public class CoverActivity extends AppCompatActivity {
    private static final String BASE_URL = "http://10.0.2.2:8080";
    private CloudApiClient cloudApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);
        cloudApiClient = new CloudApiClient(BASE_URL);

        Button btnEasy = findViewById(R.id.btnEasy);
        Button btnNormal = findViewById(R.id.btnNormal);
        Button btnHard = findViewById(R.id.btnHard);
        Button btnRankings = findViewById(R.id.btnRankings);
        Button btnOnline = findViewById(R.id.btnOnline);
        SwitchCompat switchAudio = findViewById(R.id.switchAudio);
        TextView tvCloudInfo = findViewById(R.id.tvCloudInfo);

        GameConfigState localState = GameConfigManager.load(this);
        btnNormal.setEnabled(true);
        btnHard.setEnabled(true);
        tvCloudInfo.setText(getString(R.string.cloud_info_format, localState.unlockedDifficulty, localState.coins));

        switchAudio.setChecked(AudioSettings.isAudioEnabled(this));
        switchAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AudioSettings.setAudioEnabled(this, isChecked);
            GameConfigState state = GameConfigManager.load(this);
            state.audioEnabled = isChecked;
            GameConfigManager.save(this, state);
            pushConfigBestEffort();
        });

        btnEasy.setOnClickListener(v -> startGame(1));
        btnNormal.setOnClickListener(v -> startGame(2));
        btnHard.setOnClickListener(v -> startGame(3));
        btnRankings.setOnClickListener(v -> startActivity(new Intent(this, RankActivity.class)));
        btnOnline.setOnClickListener(v -> showOnlineDialog());
    }

    private void startGame(int difficulty) {
        GameConfigState state = GameConfigManager.load(this);
        if (difficulty > state.unlockedDifficulty) {
            state.unlockedDifficulty = difficulty;
            GameConfigManager.save(this, state);
            pushConfigBestEffort();
        }
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }

    private void showOnlineDialog() {
        showJoinConfigDialog();
    }

    private void showJoinConfigDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding / 2);

        EditText hostInput = new EditText(this);
        hostInput.setHint("Host IP, e.g. 192.168.1.8");
        hostInput.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText portInput = new EditText(this);
        portInput.setHint("Port, e.g. 8989");
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setText(getString(R.string.default_online_port));

        EditText difficultyInput = new EditText(this);
        difficultyInput.setHint("Difficulty: 1=Easy, 2=Normal, 3=Hard");
        difficultyInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        difficultyInput.setText(getString(R.string.default_online_difficulty));

        container.addView(hostInput);
        container.addView(portInput);
        container.addView(difficultyInput);

        new AlertDialog.Builder(this)
                .setTitle("Join Settings")
                .setView(container)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String host = hostInput.getText().toString().trim();
                    int port = parsePort(portInput.getText().toString());
                    int difficulty = parseDifficulty(difficultyInput.getText().toString());
                    if (host.isEmpty()) {
                        host = "127.0.0.1";
                    }
                    startOnlineGame(host, port, difficulty);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startOnlineGame(String hostAddress, int port, int difficulty) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("online_mode", true);
        intent.putExtra("host_address", hostAddress);
        intent.putExtra("port", port);
        startActivity(intent);
    }

    private int parsePort(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return 8989;
        }
    }

    private int parseDifficulty(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 1 || value > 3) {
                return 2;
            }
            return value;
        } catch (Exception e) {
            return 2;
        }
    }

    private void pushConfigBestEffort() {
        String token = SessionManager.getToken(this);
        if (token == null || token.isEmpty()) {
            return;
        }
        GameConfigState state = GameConfigManager.load(this);
        cloudApiClient.pushConfig(token, state, new CloudApiClient.SimpleCallback() {
            @Override
            public void onSuccess(String value) {
                // no-op
            }

            @Override
            public void onError(String message) {
                // offline fallback: keep local config
            }
        });
    }
}
