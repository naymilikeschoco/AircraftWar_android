package edu.hitsz.audio;

public interface GameAudioEventListener {
    void onHeroShoot();

    void onEnemyHit();

    void onSupplyCollected();

    void onBombExploded();

    void onBossAppear();

    void onBossDefeated();

    void onGameOver();
}
