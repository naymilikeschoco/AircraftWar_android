package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.rank.UserRepository;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvLoginHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserSessionManager sessionManager = new UserSessionManager(this);
        if (sessionManager.isLoggedIn()) {
            openCover();
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        tvLoginHint = findViewById(R.id.tvLoginHint);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        UserRepository repository = new UserRepository(this);
        UserRepository.LoginStatus status = repository.loginOrRegister(username, password);
        switch (status) {
            case SUCCESS:
                new UserSessionManager(this).login(username);
                Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show();
                openCover();
                finish();
                break;
            case REGISTERED:
                new UserSessionManager(this).login(username);
                Toast.makeText(this, "New user registered", Toast.LENGTH_SHORT).show();
                openCover();
                finish();
                break;
            case WRONG_PASSWORD:
                tvLoginHint.setText("Password incorrect for this username.");
                break;
            case INVALID_INPUT:
            default:
                tvLoginHint.setText("Username and password are required.");
                break;
        }
    }

    private void openCover() {
        startActivity(new Intent(this, CoverActivity.class));
    }
}
