package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.network.NetworkClient;
import edu.hitsz.network.NetworkEventListener;
import edu.hitsz.network.OnlineSessionManager;

public class OnlineSetupActivity extends AppCompatActivity {

    private EditText etServerIp;
    private EditText etServerPort;
    private RadioGroup rgDifficulty;
    private Button btnJoinRoom;
    private TextView tvStatus;
    private String currentUsername = "";

    private boolean launchingGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_setup);

        etServerIp = findViewById(R.id.etServerIp);
        etServerPort = findViewById(R.id.etServerPort);
        rgDifficulty = findViewById(R.id.rgDifficulty);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        tvStatus = findViewById(R.id.tvStatus);
        TextView tvOnlineUser = findViewById(R.id.tvOnlineUser);
        currentUsername = new UserSessionManager(this).getCurrentUsername();
        tvOnlineUser.setText("Current user: " + currentUsername);

        btnJoinRoom.setOnClickListener(v -> connectAndJoinRoom());
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && !launchingGame) {
            OnlineSessionManager.getInstance().clearSession();
        }
        super.onDestroy();
    }

    private void connectAndJoinRoom() {
        String serverIp = etServerIp.getText().toString().trim();
        String portText = etServerPort.getText().toString().trim();
        if (currentUsername.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (serverIp.isEmpty()) {
            etServerIp.setError("Server IP required");
            return;
        }
        if (portText.isEmpty()) {
            etServerPort.setError("Port required");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            etServerPort.setError("Invalid port");
            return;
        }

        int difficulty = resolveDifficulty();
        btnJoinRoom.setEnabled(false);
        tvStatus.setText("Connecting to server...");

        NetworkClient client = new NetworkClient(serverIp, port);
        OnlineSessionManager manager = OnlineSessionManager.getInstance();
        manager.replaceSession(serverIp, port, currentUsername, difficulty, client);
        client.setNetworkEventListener(new SetupNetworkListener());
        client.connectAndLogin(currentUsername);
    }

    private int resolveDifficulty() {
        int checkedId = rgDifficulty.getCheckedRadioButtonId();
        if (checkedId == R.id.rbEasy) {
            return 1;
        }
        if (checkedId == R.id.rbHard) {
            return 3;
        }
        return 2;
    }

    private class SetupNetworkListener implements NetworkEventListener {
        @Override
        public void onLoginSuccess() {
            runOnUiThread(() -> tvStatus.setText("Login success. Waiting for another player..."));
        }

        @Override
        public void onLoginFailed(String reason) {
            runOnUiThread(() -> handleFailure(reason.isEmpty() ? "Login failed" : reason));
        }

        @Override
        public void onMatched(String opponentName) {
            runOnUiThread(() -> {
                OnlineSessionManager manager = OnlineSessionManager.getInstance();
                manager.setOpponentName(opponentName);
                launchingGame = true;
                tvStatus.setText("Matched with: " + opponentName);

                Intent intent = new Intent(OnlineSetupActivity.this, GameActivity.class);
                intent.putExtra(GameActivity.EXTRA_DIFFICULTY, manager.getDifficulty());
                intent.putExtra(GameActivity.EXTRA_ONLINE_MODE, true);
                intent.putExtra(GameActivity.EXTRA_USERNAME, manager.getUsername());
                intent.putExtra(GameActivity.EXTRA_OPPONENT_NAME, opponentName);
                startActivity(intent);
                finish();
            });
        }

        @Override
        public void onOpponentScoreChanged(int score) {
            // ignored on setup page
        }

        @Override
        public void onOpponentDead(int finalScore) {
            // ignored on setup page
        }

        @Override
        public void onGameOver(int selfScore, int opponentScore, String result, String opponentName) {
            // ignored on setup page
        }

        @Override
        public void onDisconnected(String reason) {
            runOnUiThread(() -> {
                if (launchingGame || isFinishing()) {
                    return;
                }
                handleFailure(reason.isEmpty() ? "Connection closed" : reason);
            });
        }

        private void handleFailure(String message) {
            OnlineSessionManager.getInstance().clearSessionWithoutDisconnect();
            btnJoinRoom.setEnabled(true);
            tvStatus.setText(message);
            Toast.makeText(OnlineSetupActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
