package edu.hitsz;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 动态创建根布局——FrameLayout
        FrameLayout rootFrameLayout = new FrameLayout(this);

        // 2. 设置根布局ID
        rootFrameLayout.setId(View.generateViewId());

        // 3. 设置根布局宽高（match_parent）
        FrameLayout.LayoutParams rootLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootFrameLayout.setLayoutParams(rootLayoutParams);

        // 4. 动态创建TextView
        TextView textView = new TextView(this);

        // 5. 设置文本内容
        textView.setText("飞机大战");

        // 6. 设置控件宽高，并通过 gravity 实现居中
        FrameLayout.LayoutParams tvLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tvLayoutParams.gravity = Gravity.CENTER; //FrameLayout 的居中方式
        textView.setLayoutParams(tvLayoutParams);

        // 7. 生成唯一ID
        textView.setId(View.generateViewId());

        // 8. 将TextView添加到根布局中
        rootFrameLayout.addView(textView);

        // 9. 将布局设置为Activity的内容视图
        setContentView(rootFrameLayout);
        //setContentView(R.layout.activity_main);
    }
}