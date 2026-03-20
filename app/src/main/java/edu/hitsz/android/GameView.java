package edu.hitsz.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;

/**
 * Surface 渲染 + 触摸控制（将触摸点映射到逻辑坐标系 512x768）。
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private final SurfaceHolder holder;
    private GameEngine game;

    private volatile boolean running = false;
    private Thread gameThread;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);

        // 只初始化一次图片资源（assets -> Bitmap）
        ImageManager.init(context.getApplicationContext());

        // 先直接开一个难度（先确保能跑起来；后续再接菜单）
        game = new GameEngine("Easy");

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        running = true;
        gameThread = new Thread(this::runLoop, "aircraft-game-loop");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // no-op
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void runLoop() {
        final int frameMs = game.getTimeIntervalMs();

        while (running) {
            long frameStart = System.currentTimeMillis();

            // 更新逻辑
            game.tick();

            // 渲染
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);

                    float scaleX = canvas.getWidth() / (float) Main.WINDOW_WIDTH;
                    float scaleY = canvas.getHeight() / (float) Main.WINDOW_HEIGHT;

                    canvas.save();
                    canvas.scale(scaleX, scaleY);
                    game.draw(canvas);
                    canvas.restore();
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // 控制帧率
            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepMs = frameMs - elapsed;
            if (sleepMs < 1) sleepMs = 1;
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            int logicalX = (int) (x * Main.WINDOW_WIDTH / (float) getWidth());
            int logicalY = (int) (y * Main.WINDOW_HEIGHT / (float) getHeight());

            // clamp 到逻辑边界
            if (logicalX < 0) logicalX = 0;
            if (logicalX > Main.WINDOW_WIDTH) logicalX = Main.WINDOW_WIDTH;
            if (logicalY < 0) logicalY = 0;
            if (logicalY > Main.WINDOW_HEIGHT) logicalY = Main.WINDOW_HEIGHT;

            game.setHeroLocation(logicalX, logicalY);
            return true;
        }

        return super.onTouchEvent(event);
    }
}

