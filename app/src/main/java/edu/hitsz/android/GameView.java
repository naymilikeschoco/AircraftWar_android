package edu.hitsz.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.audio.GameAudioEventListener;
import edu.hitsz.audio.GameAudioManager;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private static final int MSG_GAME_OVER = 1;
    public static final String KEY_SCORE = "score";
    public static final String KEY_DIFFICULTY = "difficulty";

    private final SurfaceHolder holder;
    private final GameAudioManager audioManager;
    private final Handler uiHandler;
    private GameEngine game;
    private GameOverListener gameOverListener;

    private volatile boolean running = false;
    private Thread gameThread;
    private boolean gameOverHandled = false;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        audioManager = new GameAudioManager(context);
        uiHandler = createUiHandler();

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
        uiHandler = createUiHandler();

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

    public void setGameOverListener(GameOverListener gameOverListener) {
        this.gameOverListener = gameOverListener;
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
            notifyGameOverIfNeeded();

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

    private Handler createUiHandler() {
        return new Handler(Looper.getMainLooper(), msg -> {
            if (msg.what == MSG_GAME_OVER && gameOverListener != null) {
                Bundle data = msg.getData();
                gameOverListener.onGameOver(
                        data.getInt(KEY_SCORE),
                        data.getString(KEY_DIFFICULTY, "Easy")
                );
                return true;
            }
            return false;
        });
    }

    private void notifyGameOverIfNeeded() {
        if (!game.isGameOver() || gameOverHandled) {
            return;
        }
        gameOverHandled = true;
        Message message = uiHandler.obtainMessage(MSG_GAME_OVER);
        Bundle data = new Bundle();
        data.putInt(KEY_SCORE, game.getLastScore());
        data.putString(KEY_DIFFICULTY, game.getDifficulty());
        message.setData(data);
        uiHandler.sendMessage(message);
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
                // disabled by request
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
