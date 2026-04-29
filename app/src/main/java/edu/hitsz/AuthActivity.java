package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.auth.GameConfigManager;
import edu.hitsz.auth.GameConfigState;
import edu.hitsz.auth.SessionManager;
import edu.hitsz.audio.AudioSettings;
import edu.hitsz.network.CloudApiClient;

public class AuthActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080";

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvStatus;
    private CloudApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnOffline = findViewById(R.id.btnOffline);
        tvStatus = findViewById(R.id.tvStatus);
        apiClient = new CloudApiClient(BASE_URL);

        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        etUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        btnLogin.setOnClickListener(v -> doLogin());
        btnRegister.setOnClickListener(v -> doRegister());
        btnOffline.setOnClickListener(v -> openCover());

        trySilentLogin();
    }

    private void trySilentLogin() {
        String token = SessionManager.getToken(this);
        if (token == null || token.isEmpty()) {
            return;
        }
        setStatus("Trying silent login...");
        apiClient.fetchConfig(token, new CloudApiClient.ConfigCallback() {
            @Override
            public void onSuccess(GameConfigState state) {
                runOnUiThread(() -> {
                    GameConfigManager.save(AuthActivity.this, state);
                    AudioSettings.setAudioEnabled(AuthActivity.this, state.audioEnabled);
                    setStatus("Silent login success");
                    openCover();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> setStatus("Silent login failed, please login manually"));
            }
        });
    }

    private void doRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (hasInvalidInput(username, password)) {
            return;
        }
        setStatus("Registering...");
        apiClient.register(username, password, new CloudApiClient.SimpleCallback() {
            @Override
            public void onSuccess(String value) {
                runOnUiThread(() -> {
                    setStatus("Register success, now login.");
                    Toast.makeText(AuthActivity.this, "Register success", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> setStatus("Register failed: " + message));
            }
        });
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (hasInvalidInput(username, password)) {
            return;
        }
        setStatus("Logging in...");
        apiClient.login(username, password, new CloudApiClient.SimpleCallback() {
            @Override
            public void onSuccess(String token) {
                if (token == null || token.isEmpty()) {
                    runOnUiThread(() -> setStatus("Login failed: empty token"));
                    return;
                }
                SessionManager.saveSession(AuthActivity.this, username, token);
                apiClient.fetchConfig(token, new CloudApiClient.ConfigCallback() {
                    @Override
                    public void onSuccess(GameConfigState state) {
                        runOnUiThread(() -> {
                            GameConfigManager.save(AuthActivity.this, state);
                            AudioSettings.setAudioEnabled(AuthActivity.this, state.audioEnabled);
                            setStatus("Login success");
                            openCover();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            setStatus("Login success, config sync failed, using local config");
                            openCover();
                        });
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> setStatus("Login failed: " + message));
            }
        });
    }

    private boolean hasInvalidInput(String username, String password) {
        if (username.isEmpty()) {
            setStatus("Username required");
            return true;
        }
        if (!password.matches("^[A-Za-z0-9_!@#$%^&*]{6,20}$")) {
            setStatus("Password must be 6-20 chars");
            return true;
        }
        return false;
    }

    private void setStatus(String text) {
        tvStatus.setText(text);
    }

    private void openCover() {
        startActivity(new Intent(this, CoverActivity.class));
        finish();
    }
}
