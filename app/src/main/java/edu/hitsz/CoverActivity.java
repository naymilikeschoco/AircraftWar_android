package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.audio.AudioSettings;

public class CoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);

        Button btnEasy = findViewById(R.id.btnEasy);
        Button btnNormal = findViewById(R.id.btnNormal);
        Button btnHard = findViewById(R.id.btnHard);
        Button btnRankings = findViewById(R.id.btnRankings);
        Switch switchAudio = findViewById(R.id.switchAudio);

        switchAudio.setChecked(AudioSettings.isAudioEnabled(this));
        switchAudio.setOnCheckedChangeListener((buttonView, isChecked) ->
                AudioSettings.setAudioEnabled(this, isChecked));

        btnEasy.setOnClickListener(v -> startGame(1));
        btnNormal.setOnClickListener(v -> startGame(2));
        btnHard.setOnClickListener(v -> startGame(3));
        btnRankings.setOnClickListener(v -> startActivity(new Intent(this, RankActivity.class)));
    }

    private void startGame(int difficulty) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }
}
