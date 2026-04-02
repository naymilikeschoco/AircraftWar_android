package edu.hitsz.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.audio.GameAudioEventListener;
import edu.hitsz.factory.enemy.EnemyType;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.BulletProp;
import edu.hitsz.prop.BulletPlusProp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
public class GameEngine {

    private static class EnemyConfig {
        public final int mobHp;
        public final int mobSpeedY;
        public final int eliteHp;
        public final int eliteBaseSpeedY;
        public final int elitePlusHp;
        public final int elitePlusBaseSpeedY;

        public EnemyConfig(int mobHp, int mobSpeedY,
                            int eliteHp, int eliteBaseSpeedY,
                            int elitePlusHp, int elitePlusBaseSpeedY) {
            this.mobHp = mobHp;
            this.mobSpeedY = mobSpeedY;
            this.eliteHp = eliteHp;
            this.eliteBaseSpeedY = eliteBaseSpeedY;
            this.elitePlusHp = elitePlusHp;
            this.elitePlusBaseSpeedY = elitePlusBaseSpeedY;
        }
    }

    private final String difficulty;

    private final int timeIntervalMs = 40;
    private int time = 0;

    // 周期（ms)，指示子弹的发射、敌机的产生频率
    private int cycleDurationMs;
    private int cycleTimeMs = 0;

    // 敌机出现控制
    private int enemyMaxNumber;
    private double eliteProbability;
    private double elitePlusProbability;

    // Boss 控制
    private boolean bossExists = false;
    private int bossScoreThreshold;
    private int bossHp;

    // 当前得分（不做排行榜，仅用于游戏内显示）
    private int score = 0;
    private int lastScore = 0;

    // 背景滚动
    private int backGroundTop = 0;
    private final Bitmap backgroundImage;

    // 对象集合
    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircraft;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;

    private final UnifiedEnemyFactory enemyFactory;
    private GameAudioEventListener audioEventListener;

    private boolean gameOver = false;

    // 文本绘制
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameEngine(String difficulty) {
        this.difficulty = difficulty;

        this.backgroundImage = getBackgroundImage();

        // 1) 读取难度参数
        EnemyConfig enemyConfig = getEnemyConfig();
        int heroShootCycle = getHeroShootCycle();
        this.enemyMaxNumber = getEnemyMaxNumber();
        this.cycleDurationMs = getCycleDuration();
        this.eliteProbability = getInitialEliteProbability();
        this.elitePlusProbability = getInitialElitePlusProbability();
        this.bossScoreThreshold = getBossScoreThreshold();
        this.bossHp = getBossHp();

        // 2) 初始化英雄
        heroAircraft = HeroAircraft.getInstance();
        heroAircraft.setShootCycle(heroShootCycle);

        enemyAircraft = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        props = new LinkedList<>();

        // 3) 初始化敌机工厂（随机生成）
        enemyFactory = new UnifiedEnemyFactory()
                .configMob(enemyConfig.mobHp, enemyConfig.mobSpeedY)
                .configElite(enemyConfig.eliteHp, enemyConfig.eliteBaseSpeedY)
                .configElitePlus(enemyConfig.elitePlusHp, enemyConfig.elitePlusBaseSpeedY)
                .enableRandom(eliteProbability, elitePlusProbability);

        textPaint.setColor(Color.rgb(0xFF, 0x00, 0x00));
        textPaint.setTextSize(22f);

        gameOverPaint.setColor(Color.WHITE);
        gameOverPaint.setTextSize(48f);
    }

    public int getTimeIntervalMs() {
        return timeIntervalMs;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getLastScore() {
        return lastScore;
    }

    public HeroAircraft getHeroAircraft() {
        return heroAircraft;
    }

    public void setAudioEventListener(GameAudioEventListener audioEventListener) {
        this.audioEventListener = audioEventListener;
    }

    public void setHeroLocation(int logicalX, int logicalY) {
        if (gameOver) return;
        heroAircraft.setLocation(logicalX, logicalY);
    }

    public void tick() {
        if (gameOver) return;

        time += timeIntervalMs;

        if (timeCountAndNewCycleJudge()) {
            // 动态调整难度参数（按原 Normal/Hard Game 的逻辑触发）
            updateDifficulty();

            // Boss 生成逻辑
            if (!bossExists && score >= bossScoreThreshold) {
                enemyFactory.disableRandom();
                enemyAircraft.add(
                        enemyFactory
                                .setType(EnemyType.BOSS)
                                .configBoss(bossHp, 5)
                                .createEnemy());
                enemyFactory.enableRandom(eliteProbability, elitePlusProbability);
                bossExists = true;
                bossScoreThreshold += 1000;
                if (audioEventListener != null) {
                    audioEventListener.onBossAppear();
                }
            }
            // 普通敌机生成逻辑
            else if (enemyAircraft.size() < enemyMaxNumber && !bossExists) {
                // 保持与原 GameTemplate 一致的精英概率随时间增长
                eliteProbability = Math.min(0.4, 0.2 + time / 60000.0);
                elitePlusProbability = Math.min(0.2, 0.1 + time / 120000.0);
                enemyFactory.setEliteProbability(eliteProbability);
                enemyFactory.setElitePlusProbability(elitePlusProbability);
                enemyAircraft.add(enemyFactory.createEnemy());
            }
        }

        shootAction();
        bulletsMoveAction();
        aircraftMoveAction();
        propsMoveAction();
        crashCheckAction();
        postProcessAction();

        // 英雄机死亡 → 游戏结束
        if (heroAircraft.getHp() <= 0) {
            gameOver = true;
            lastScore = score;
            if (audioEventListener != null) {
                audioEventListener.onGameOver();
            }
        }
    }

    private Bitmap getBackgroundImage() {
        switch (difficulty) {
            case "Hard":
                return ImageManager.BACKGROUND_IMAGE_HARD;
            case "Easy":
                return ImageManager.BACKGROUND_IMAGE_EASY;
            case "Normal":
            default:
                return ImageManager.BACKGROUND_IMAGE_NORMAL;
        }
    }

    private EnemyConfig getEnemyConfig() {
        switch (difficulty) {
            case "Easy":
                return new EnemyConfig(
                        20, 8,
                        30, 6,
                        60, 5
                );
            case "Hard":
                return new EnemyConfig(
                        30, 12,
                        60, 12,
                        100, 8
                );
            case "Normal":
            default:
                return new EnemyConfig(
                        40, 11,
                        50, 9,
                        90, 8
                );
        }
    }

    private int getHeroShootCycle() {
        switch (difficulty) {
            case "Easy":
                return 180;
            case "Hard":
                return 200;
            case "Normal":
            default:
                return 190;
        }
    }

    private int getEnemyMaxNumber() {
        switch (difficulty) {
            case "Easy":
                return 3;
            case "Hard":
                return 10;
            case "Normal":
            default:
                return 7;
        }
    }

    private int getCycleDuration() {
        switch (difficulty) {
            case "Easy":
                return 800;
            case "Hard":
                return 400;
            case "Normal":
            default:
                return 600;
        }
    }

    private double getInitialEliteProbability() {
        switch (difficulty) {
            case "Easy":
                return 0.2;
            case "Hard":
                return 0.5;
            case "Normal":
            default:
                return 0.3;
        }
    }

    private double getInitialElitePlusProbability() {
        switch (difficulty) {
            case "Easy":
                return 0.05;
            case "Hard":
                return 0.3;
            case "Normal":
            default:
                return 0.15;
        }
    }

    private int getBossScoreThreshold() {
        switch (difficulty) {
            case "Easy":
                // 对齐你原 SimpleGame.initializeGameParameters()：this.bossScoreThreshold = 800
                return 800;
            case "Hard":
                return 300;
            case "Normal":
            default:
                return 500;
        }
    }

    private int getBossHp() {
        switch (difficulty) {
            case "Easy":
                return 1500;
            case "Hard":
                return 3500;
            case "Normal":
            default:
                return 2500;
        }
    }

    private void updateDifficulty() {
        // 按你原 NormalGame / HardGame 的逻辑实现
        if ("Normal".equals(difficulty)) {
            if (time > 30000) {
                cycleDurationMs = Math.max(400, 600 - time / 100);
            }
            if (time > 60000) {
                enemyMaxNumber = Math.min(7, 5 + time / 30000);
            }
        } else if ("Hard".equals(difficulty)) {
            if (time > 20000) {
                cycleDurationMs = Math.max(200, 400 - time / 80);
            }
            if (time > 30000) {
                enemyMaxNumber = Math.min(10, 7 + time / 20000);
            }
            if (time > 40000) {
                eliteProbability = Math.min(0.6, 0.4 + time / 200000.0);
                elitePlusProbability = Math.min(0.25, 0.15 + time / 400000.0);
            }
        }
    }

    private boolean timeCountAndNewCycleJudge() {
        cycleTimeMs += timeIntervalMs;
        if (cycleTimeMs >= cycleDurationMs) {
            cycleTimeMs %= cycleDurationMs;
            return true;
        }
        return false;
    }

    private void shootAction() {
        for (AbstractAircraft enemy : enemyAircraft) {
            if (enemy instanceof EliteEnemy ||
                    enemy instanceof ElitePlusEnemy ||
                    enemy instanceof BossEnemy) {
                if (enemy.updateShootTimer(timeIntervalMs)) {
                    enemyBullets.addAll(enemy.shoot());
                }
            }
        }

        if (heroAircraft.updateShootTimer(timeIntervalMs)) {
            List<BaseBullet> newBullets = heroAircraft.shoot();
            heroBullets.addAll(newBullets);
            if (!newBullets.isEmpty() && audioEventListener != null) {
                audioEventListener.onHeroShoot();
            }
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) bullet.forward();
        for (BaseBullet bullet : enemyBullets) bullet.forward();
    }

    private void aircraftMoveAction() {
        for (AbstractAircraft enemy : enemyAircraft) enemy.forward();
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) prop.forward();
    }

    private void crashCheckAction() {
        // 敌机子弹击中英雄机
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) continue;
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        // 英雄子弹击中敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) continue;
            for (AbstractAircraft enemy : enemyAircraft) {
                if (enemy.notValid()) continue;

                if (enemy.crash(bullet)) {
                    enemy.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    if (audioEventListener != null) {
                        audioEventListener.onEnemyHit();
                    }

                    if (enemy.notValid()) {
                        if (enemy instanceof AbstractEnemy) {
                            AbstractEnemy e = (AbstractEnemy) enemy;
                            score += e.getScore();
                            props.addAll(e.mayDrop());

                            if (e instanceof BossEnemy) {
                                bossExists = false;
                                bossScoreThreshold += 500;
                                if (audioEventListener != null) {
                                    audioEventListener.onBossDefeated();
                                }
                            }
                        }
                    }
                }

                // 敌机碰撞英雄机（贴到一起即判定英雄死亡）
                if (enemy.crash(heroAircraft) || heroAircraft.crash(enemy)) {
                    enemy.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        // 英雄机获得道具
        for (AbstractProp prop : props) {
            if (prop.notValid()) continue;

            if (heroAircraft.crash(prop)) {
                if (prop instanceof BombProp) {
                    if (audioEventListener != null) {
                        audioEventListener.onSupplyCollected();
                        audioEventListener.onBombExploded();
                    }
                    BombProp bomb = (BombProp) prop;
                    for (AbstractAircraft enemy : enemyAircraft) {
                        if (!(enemy instanceof BossEnemy)) bomb.addObserver(enemy);
                    }
                    for (BaseBullet bullet : enemyBullets) bomb.addObserver(bullet);

                    bomb.effect(heroAircraft);

                    int bombScore = 0;
                    for (AbstractAircraft enemy : enemyAircraft) {
                        if (enemy.notValid() && !(enemy instanceof BossEnemy)) {
                            bombScore += ((AbstractEnemy) enemy).getScore();
                        }
                    }
                    score += bombScore;
                } else {
                    // 普通道具：加血/火力等
                    if (audioEventListener != null) {
                        audioEventListener.onSupplyCollected();
                    }
                    prop.effect(heroAircraft);
                }

                prop.vanish();
            }
        }
    }

    private void postProcessAction() {
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircraft.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
    }

    public void draw(Canvas canvas) {
        // 背景滚动（和原 Swing 版本一致）
        canvas.drawBitmap(backgroundImage, 0, backGroundTop - Main.WINDOW_HEIGHT, null);
        canvas.drawBitmap(backgroundImage, 0, backGroundTop, null);

        backGroundTop += 1;
        if (backGroundTop == Main.WINDOW_HEIGHT) {
            backGroundTop = 0;
        }

        drawObjects(canvas, enemyBullets);
        drawObjects(canvas, heroBullets);
        drawObjects(canvas, enemyAircraft);
        drawObjects(canvas, props);

        // 英雄机
        Bitmap hero = ImageManager.HERO_IMAGE;
        if (hero != null) {
            canvas.drawBitmap(
                    hero,
                    heroAircraft.getLocationX() - hero.getWidth() / 2f,
                    heroAircraft.getLocationY() - hero.getHeight() / 2f,
                    null
            );
        }

        // 分数 & 生命
        canvas.drawText("SCORE:" + score, 10, 25, textPaint);
        canvas.drawText("LIFE:" + heroAircraft.getHp(), 10, 45, textPaint);

        if (gameOver) {
            canvas.drawText("GAME OVER", 70, 380, gameOverPaint);
            canvas.drawText("SCORE:" + lastScore, 110, 440, gameOverPaint);
        }
    }

    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject obj : objects) {
            Bitmap img = obj.getImage();
            if (img == null) continue;
            canvas.drawBitmap(
                    img,
                    obj.getLocationX() - img.getWidth() / 2f,
                    obj.getLocationY() - img.getHeight() / 2f,
                    null
            );
        }
    }
}
