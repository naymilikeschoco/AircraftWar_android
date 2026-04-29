package edu.hitsz.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.audio.GameAudioEventListener;
import edu.hitsz.audio.GameAudioManager;
import edu.hitsz.network.OnlineMatchClient;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private static final int MSG_GAME_OVER = 1;
    private static final int MSG_DISCONNECTED = 2;
    public static final String KEY_SCORE = "score";
    public static final String KEY_DIFFICULTY = "difficulty";
    public static final String KEY_REMOTE_SCORE = "remote_score";
    public static final String KEY_ONLINE_MODE = "online_mode";

    private final SurfaceHolder holder;
    private final GameAudioManager audioManager;
    private final Handler uiHandler;
    private GameEngine game;
    private GameOverListener gameOverListener;

    private volatile boolean running = false;
    private Thread gameThread;
    private boolean gameOverHandled = false;
    private int lastSentScore = -1;
    private int remoteScore = 0;
    private boolean remoteDead = false;
    private boolean localDeadNotified = false;
    private final boolean onlineMode;
    private volatile boolean matchReady = false;
    private OnlineMatchClient onlineMatchClient;
    private final Paint onlineTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        onlineMode = false;
        bindAudioEvents();
        setFocusable(true);
        initOnlinePaint();
    }

    public GameView(Context context, AttributeSet attrs, int difficulty) {
        this(context, attrs, difficulty, false, null, 0);
    }

    public GameView(Context context, AttributeSet attrs, int difficulty, boolean onlineMode,
                    String hostAddress, int port) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        audioManager = new GameAudioManager(context);
        uiHandler = createUiHandler();
        this.onlineMode = onlineMode;

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
        initOnlinePaint();
        if (onlineMode) {
            setupOnlineClient(hostAddress, port);
        }
    }

    public void setGameOverListener(GameOverListener gameOverListener) {
        this.gameOverListener = gameOverListener;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        running = true;
        audioManager.startNormalBgm();
        gameThread = new Thread(this::runLoop, "aircraft-game-loop");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        // no-op
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        running = false;
        audioManager.pauseAll();
        if (gameThread != null) {
            try {
                gameThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @SuppressWarnings("BusyWait")
    private void runLoop() {
        final int frameMs = game.getTimeIntervalMs();

        while (running) {
            long frameStart = System.currentTimeMillis();

            boolean canRunGameTick = !onlineMode || matchReady;
            if (canRunGameTick) {
                game.tick();
                syncOnlineState();
                notifyGameOverIfNeeded();
            }

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
                    if (onlineMode) {
                        canvas.drawText("RIVAL SCORE:" + remoteScore, 10, 85, onlineTextPaint);
                        if (!matchReady) {
                            canvas.drawText("Waiting rival to join...", 10, 125, onlineTextPaint);
                        }
                        if (game.isGameOver() && !remoteDead) {
                            canvas.drawText("Waiting rival...", 10, 125, onlineTextPaint);
                        }
                    }
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
                        data.getString(KEY_DIFFICULTY, "Easy"),
                        data.getInt(KEY_REMOTE_SCORE, 0),
                        data.getBoolean(KEY_ONLINE_MODE, false)
                );
                return true;
            }
            if (msg.what == MSG_DISCONNECTED && gameOverListener != null) {
                gameOverListener.onGameOver(game.getScore(), game.getDifficulty(), remoteScore, true);
                return true;
            }
            return false;
        });
    }

    private void notifyGameOverIfNeeded() {
        if (!game.isGameOver() || gameOverHandled) {
            return;
        }
        if (onlineMode && !remoteDead) {
            return;
        }
        gameOverHandled = true;
        Message message = uiHandler.obtainMessage(MSG_GAME_OVER);
        Bundle data = new Bundle();
        data.putInt(KEY_SCORE, game.getLastScore());
        data.putString(KEY_DIFFICULTY, game.getDifficulty());
        data.putInt(KEY_REMOTE_SCORE, remoteScore);
        data.putBoolean(KEY_ONLINE_MODE, onlineMode);
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
        if (onlineMatchClient != null) {
            onlineMatchClient.close();
        }
        audioManager.release();
    }

    private void initOnlinePaint() {
        onlineTextPaint.setColor(Color.WHITE);
        onlineTextPaint.setTextSize(36f);
    }

    private void setupOnlineClient(String hostAddress, int port) {
        onlineMatchClient = new OnlineMatchClient(new OnlineMatchClient.Listener() {
            @Override
            public void onConnected() {
                // no-op
            }

            @Override
            public void onMatchReady() {
                matchReady = true;
            }

            @Override
            public void onRemoteScoreChanged(int score) {
                remoteScore = score;
            }

            @Override
            public void onRemoteDead() {
                remoteDead = true;
            }

            @Override
            public void onDisconnected(String reason) {
                uiHandler.sendEmptyMessage(MSG_DISCONNECTED);
            }
        });
        onlineMatchClient.join(hostAddress, port);
    }

    private void syncOnlineState() {
        if (!onlineMode || onlineMatchClient == null) {
            return;
        }
        int score = game.getScore();
        if (score != lastSentScore) {
            lastSentScore = score;
            onlineMatchClient.sendScore(score);
        }
        if (game.isGameOver() && !localDeadNotified) {
            localDeadNotified = true;
            onlineMatchClient.sendDead();
        }
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
