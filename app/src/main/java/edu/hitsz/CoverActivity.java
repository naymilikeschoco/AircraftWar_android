package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class CoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover); // 使用XML布局

        Button btnEasy   = findViewById(R.id.btnEasy);
        Button btnNormal = findViewById(R.id.btnNormal);
        Button btnHard   = findViewById(R.id.btnHard);

        // 点击简单 → 跳转游戏，传入难度值
        btnEasy.setOnClickListener(v -> startGame(1));
        btnNormal.setOnClickListener(v -> startGame(2));
        btnHard.setOnClickListener(v -> startGame(3));
    }

    private void startGame(int difficulty) {
        // 步骤1：创建Intent，指定目标Activity
        Intent intent = new Intent(this, GameActivity.class);

        // 步骤2：putExtra传递难度数据
        intent.putExtra("difficulty", difficulty);

        // 步骤3：启动Activity
        startActivity(intent);
    }
}