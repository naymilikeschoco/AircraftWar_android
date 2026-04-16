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
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.audio.GameAudioEventListener;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.factory.enemy.EnemyType;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BombProp;

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
    private int cycleDurationMs;
    private int cycleTimeMs = 0;
    private int enemyMaxNumber;
    private double eliteProbability;
    private double elitePlusProbability;
    private boolean bossExists = false;
    private int bossScoreThreshold;
    private int bossHp;
    private int score = 0;
    private int lastScore = 0;
    private int backGroundTop = 0;

    private final Bitmap backgroundImage;
    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircraft;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;
    private final UnifiedEnemyFactory enemyFactory;
    private GameAudioEventListener audioEventListener;
    private boolean gameOver = false;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameEngine(String difficulty) {
        this.difficulty = difficulty;
        this.backgroundImage = getBackgroundImage();

        EnemyConfig enemyConfig = getEnemyConfig();
        int heroShootCycle = getHeroShootCycle();
        this.enemyMaxNumber = getEnemyMaxNumber();
        this.cycleDurationMs = getCycleDuration();
        this.eliteProbability = getInitialEliteProbability();
        this.elitePlusProbability = getInitialElitePlusProbability();
        this.bossScoreThreshold = getBossScoreThreshold();
        this.bossHp = getBossHp();

        heroAircraft = HeroAircraft.getInstance();
        heroAircraft.resetForNewGame();
        heroAircraft.setShootCycle(heroShootCycle);

        enemyAircraft = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        props = new LinkedList<>();

        enemyFactory = new UnifiedEnemyFactory()
                .configMob(enemyConfig.mobHp, enemyConfig.mobSpeedY)
                .configElite(enemyConfig.eliteHp, enemyConfig.eliteBaseSpeedY)
                .configElitePlus(enemyConfig.elitePlusHp, enemyConfig.elitePlusBaseSpeedY)
                .enableRandom(eliteProbability, elitePlusProbability);

        textPaint.setColor(Color.RED);
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setAudioEventListener(GameAudioEventListener audioEventListener) {
        this.audioEventListener = audioEventListener;
    }

    public void setHeroLocation(int logicalX, int logicalY) {
        if (gameOver) {
            return;
        }
        heroAircraft.setLocation(logicalX, logicalY);
    }

    public void tick() {
        if (gameOver) {
            return;
        }

        time += timeIntervalMs;

        if (timeCountAndNewCycleJudge()) {
            updateDifficulty();

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
            } else if (enemyAircraft.size() < enemyMaxNumber && !bossExists) {
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
                return new EnemyConfig(20, 8, 30, 6, 60, 5);
            case "Hard":
                return new EnemyConfig(30, 12, 60, 12, 100, 8);
            case "Normal":
            default:
                return new EnemyConfig(40, 11, 50, 9, 90, 8);
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
            if (enemy instanceof EliteEnemy || enemy instanceof ElitePlusEnemy || enemy instanceof BossEnemy) {
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
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftMoveAction() {
        for (AbstractAircraft enemy : enemyAircraft) {
            enemy.forward();
        }
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) {
            prop.forward();
        }
    }

    private void crashCheckAction() {
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemy : enemyAircraft) {
                if (enemy.notValid()) {
                    continue;
                }

                if (enemy.crash(bullet)) {
                    enemy.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    if (audioEventListener != null) {
                        audioEventListener.onEnemyHit();
                    }

                    if (enemy.notValid() && enemy instanceof AbstractEnemy) {
                        AbstractEnemy defeatedEnemy = (AbstractEnemy) enemy;
                        score += defeatedEnemy.getScore();
                        props.addAll(defeatedEnemy.mayDrop());

                        if (defeatedEnemy instanceof BossEnemy) {
                            bossExists = false;
                            bossScoreThreshold += 500;
                            if (audioEventListener != null) {
                                audioEventListener.onBossDefeated();
                            }
                        }
                    }
                }

                if (enemy.crash(heroAircraft) || heroAircraft.crash(enemy)) {
                    enemy.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        for (AbstractProp prop : props) {
            if (prop.notValid()) {
                continue;
            }
            if (heroAircraft.crash(prop)) {
                if (prop instanceof BombProp) {
                    if (audioEventListener != null) {
                        audioEventListener.onSupplyCollected();
                        audioEventListener.onBombExploded();
                    }
                    BombProp bomb = (BombProp) prop;
                    for (AbstractAircraft enemy : enemyAircraft) {
                        if (!(enemy instanceof BossEnemy)) {
                            bomb.addObserver(enemy);
                        }
                    }
                    for (BaseBullet bullet : enemyBullets) {
                        bomb.addObserver(bullet);
                    }
                    bomb.effect(heroAircraft);

                    int bombScore = 0;
                    for (AbstractAircraft enemy : enemyAircraft) {
                        if (enemy.notValid() && !(enemy instanceof BossEnemy)) {
                            bombScore += ((AbstractEnemy) enemy).getScore();
                        }
                    }
                    score += bombScore;
                } else {
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

        Bitmap hero = ImageManager.HERO_IMAGE;
        if (hero != null) {
            canvas.drawBitmap(
                    hero,
                    heroAircraft.getLocationX() - hero.getWidth() / 2f,
                    heroAircraft.getLocationY() - hero.getHeight() / 2f,
                    null
            );
        }

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
            if (img == null) {
                continue;
            }
            canvas.drawBitmap(
                    img,
                    obj.getLocationX() - img.getWidth() / 2f,
                    obj.getLocationY() - img.getHeight() / 2f,
                    null
            );
        }
    }
}
