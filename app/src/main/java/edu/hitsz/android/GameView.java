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
import edu.hitsz.audio.GameAudioEventListener;
import edu.hitsz.audio.GameAudioManager;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private final SurfaceHolder holder;
    private final GameAudioManager audioManager;
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
        audioManager = new GameAudioManager(context);

        ImageManager.init(context.getApplicationContext());
        game = new GameEngine("Easy");
        bindAudioEvents();

        setFocusable(true);
    }

    public GameView(Context context, AttributeSet attrs, int difficulty) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        audioManager = new GameAudioManager(context);

        ImageManager.init(context.getApplicationContext());

        switch (difficulty) {
            case 1:
                game = new GameEngine("Easy");
                break;
            case 2:
                game = new GameEngine("Normal");
                break;
            case 3:
                game = new GameEngine("Hard");
                break;
            default:
                game = new GameEngine("Easy");
                break;
        }
        bindAudioEvents();

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        running = true;
        audioManager.startNormalBgm();
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
        audioManager.pauseAll();
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

            game.tick();

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

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepMs = frameMs - elapsed;
            if (sleepMs < 1) {
                sleepMs = 1;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            int logicalX = (int) (x * Main.WINDOW_WIDTH / (float) getWidth());
            int logicalY = (int) (y * Main.WINDOW_HEIGHT / (float) getHeight());

            if (logicalX < 0) {
                logicalX = 0;
            }
            if (logicalX > Main.WINDOW_WIDTH) {
                logicalX = Main.WINDOW_WIDTH;
            }
            if (logicalY < 0) {
                logicalY = 0;
            }
            if (logicalY > Main.WINDOW_HEIGHT) {
                logicalY = Main.WINDOW_HEIGHT;
            }

            game.setHeroLocation(logicalX, logicalY);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void onPauseView() {
        audioManager.pauseAll();
    }

    public void onResumeView() {
        if (holder.getSurface().isValid()) {
            audioManager.resumeBgm();
        }
    }

    public void release() {
        audioManager.release();
    }

    private void bindAudioEvents() {
        game.setAudioEventListener(new GameAudioEventListener() {
            @Override
            public void onHeroShoot() {
                // hero shoot sound disabled by request
            }

            @Override
            public void onEnemyHit() {
                audioManager.playBulletHit();
            }

            @Override
            public void onSupplyCollected() {
                audioManager.playGetSupply();
            }

            @Override
            public void onBombExploded() {
                audioManager.playBombExplosion();
            }

            @Override
            public void onBossAppear() {
                audioManager.startBossBgm();
            }

            @Override
            public void onBossDefeated() {
                audioManager.startNormalBgm();
            }

            @Override
            public void onGameOver() {
                audioManager.stopBgm();
                audioManager.playGameOver();
            }
        });
    }
}
