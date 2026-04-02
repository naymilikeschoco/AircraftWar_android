package edu.hitsz.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;

public class GameAudioManager {

    private static final String AUDIO_DIR = "audio/";
    private static final String BGM_FILE = AUDIO_DIR + "bgm.wav";
    private static final String BGM_BOSS_FILE = AUDIO_DIR + "bgm_boss.wav";
    private static final String BULLET_FILE = AUDIO_DIR + "bullet.wav";
    private static final String BULLET_HIT_FILE = AUDIO_DIR + "bullet_hit.wav";
    private static final String GET_SUPPLY_FILE = AUDIO_DIR + "get_supply.wav";
    private static final String BOMB_EXPLOSION_FILE = AUDIO_DIR + "bomb_explosion.wav";
    private static final String GAME_OVER_FILE = AUDIO_DIR + "game_over.wav";

    private static final float NORMAL_BGM_VOLUME = 1.0f;
    private static final float BOSS_BGM_VOLUME = 1.0f;
    private static final float EFFECT_VOLUME = 0.45f;

    private enum BgmMode {
        NORMAL,
        BOSS
    }

    private final Context appContext;
    private final SoundPool soundPool;
    private final int bulletSoundId;
    private final int bulletHitSoundId;
    private final int getSupplySoundId;
    private final int bombExplosionSoundId;
    private final int gameOverSoundId;

    private MediaPlayer normalBgmPlayer;
    private MediaPlayer bossBgmPlayer;
    private BgmMode currentBgmMode = BgmMode.NORMAL;
    private boolean bgmPaused = true;
    private boolean released = false;

    public GameAudioManager(Context context) {
        this.appContext = context.getApplicationContext();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build();

        this.bulletSoundId = loadSound(BULLET_FILE);
        this.bulletHitSoundId = loadSound(BULLET_HIT_FILE);
        this.getSupplySoundId = loadSound(GET_SUPPLY_FILE);
        this.bombExplosionSoundId = loadSound(BOMB_EXPLOSION_FILE);
        this.gameOverSoundId = loadSound(GAME_OVER_FILE);
    }

    public void startNormalBgm() {
        currentBgmMode = BgmMode.NORMAL;
        bgmPaused = false;
        switchBgm(getNormalBgmPlayer());
    }

    public void startBossBgm() {
        currentBgmMode = BgmMode.BOSS;
        bgmPaused = false;
        switchBgm(getBossBgmPlayer());
    }

    public void stopBgm() {
        pausePlayer(normalBgmPlayer);
        pausePlayer(bossBgmPlayer);
        bgmPaused = true;
    }

    public void pauseAll() {
        if (released) {
            return;
        }
        stopBgm();
    }

    public void resumeBgm() {
        if (released || !bgmPaused) {
            return;
        }
        if (currentBgmMode == BgmMode.BOSS) {
            startBossBgm();
        } else {
            startNormalBgm();
        }
    }

    public void playBullet() {
        playSound(bulletSoundId);
    }

    public void playBulletHit() {
        playSound(bulletHitSoundId);
    }

    public void playGetSupply() {
        playSound(getSupplySoundId);
    }

    public void playBombExplosion() {
        playSound(bombExplosionSoundId);
    }

    public void playGameOver() {
        playSound(gameOverSoundId);
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        releasePlayer(normalBgmPlayer);
        releasePlayer(bossBgmPlayer);
        soundPool.release();
    }

    private int loadSound(String assetPath) {
        try (AssetFileDescriptor afd = appContext.getAssets().openFd(assetPath)) {
            return soundPool.load(afd, 1);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load audio asset: " + assetPath, e);
        }
    }

    private void playSound(int soundId) {
        if (released || soundId == 0) {
            return;
        }
        soundPool.play(soundId, EFFECT_VOLUME, EFFECT_VOLUME, 1, 0, 1f);
    }

    private void switchBgm(MediaPlayer targetPlayer) {
        if (released || targetPlayer == null) {
            return;
        }
        MediaPlayer otherPlayer = targetPlayer == normalBgmPlayer ? bossBgmPlayer : normalBgmPlayer;
        pausePlayer(otherPlayer);
        if (!targetPlayer.isPlaying()) {
            targetPlayer.start();
        }
    }

    private MediaPlayer getNormalBgmPlayer() {
        if (normalBgmPlayer == null) {
            normalBgmPlayer = createLoopPlayer(BGM_FILE, NORMAL_BGM_VOLUME);
        }
        return normalBgmPlayer;
    }

    private MediaPlayer getBossBgmPlayer() {
        if (bossBgmPlayer == null) {
            bossBgmPlayer = createLoopPlayer(BGM_BOSS_FILE, BOSS_BGM_VOLUME);
        }
        return bossBgmPlayer;
    }

    private MediaPlayer createLoopPlayer(String assetPath, float volume) {
        try {
            AssetFileDescriptor afd = appContext.getAssets().openFd(assetPath);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.setVolume(volume, volume);
            player.prepare();
            return player;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare BGM asset: " + assetPath, e);
        }
    }

    private void pausePlayer(MediaPlayer player) {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    private void releasePlayer(MediaPlayer player) {
        if (player != null) {
            player.release();
        }
    }
}
