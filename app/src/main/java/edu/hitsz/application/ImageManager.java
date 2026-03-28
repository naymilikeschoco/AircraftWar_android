package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.BulletPlusProp;
import edu.hitsz.prop.BulletProp;

public final class ImageManager {

    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Bitmap BACKGROUND_IMAGE_EASY;
    public static Bitmap BACKGROUND_IMAGE_NORMAL;
    public static Bitmap BACKGROUND_IMAGE_HARD;
    public static Bitmap HERO_IMAGE;
    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;
    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_IMAGE;
    public static Bitmap ELITE_PLUS_ENEMY_IMAGE;
    public static Bitmap BOSS_ENEMY_IMAGE;
    public static Bitmap PROP_BLOOD_IMAGE;
    public static Bitmap PROP_BOMB_IMAGE;
    public static Bitmap PROP_BULLET_IMAGE;
    public static Bitmap PROP_BULLET_PLUS_IMAGE;

    private static volatile boolean initialized = false;

    private ImageManager() {
    }

    public static synchronized void init(Context context) {
        if (initialized) {
            return;
        }

        BACKGROUND_IMAGE_EASY = loadBitmap(context, "images/bg.jpg");
        BACKGROUND_IMAGE_NORMAL = loadBitmap(context, "images/bg2.jpg");
        BACKGROUND_IMAGE_HARD = loadBitmap(context, "images/bg3.jpg");
        HERO_IMAGE = loadBitmap(context, "images/hero.png");
        HERO_BULLET_IMAGE = loadBitmap(context, "images/bullet_hero.png");
        ENEMY_BULLET_IMAGE = loadBitmap(context, "images/bullet_enemy.png");
        MOB_ENEMY_IMAGE = loadBitmap(context, "images/mob.png");
        ELITE_ENEMY_IMAGE = loadBitmap(context, "images/elite.png");
        ELITE_PLUS_ENEMY_IMAGE = loadBitmap(context, "images/elitePlus.png");
        BOSS_ENEMY_IMAGE = loadBitmap(context, "images/boss.png");
        PROP_BLOOD_IMAGE = loadBitmap(context, "images/prop_blood.png");
        PROP_BOMB_IMAGE = loadBitmap(context, "images/prop_bomb.png");
        PROP_BULLET_IMAGE = loadBitmap(context, "images/prop_bullet.png");
        PROP_BULLET_PLUS_IMAGE = loadBitmap(context, "images/prop_bulletPlus.png");

        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ElitePlusEnemy.class.getName(), ELITE_PLUS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BloodProp.class.getName(), PROP_BLOOD_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BombProp.class.getName(), PROP_BOMB_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletProp.class.getName(), PROP_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletPlusProp.class.getName(), PROP_BULLET_PLUS_IMAGE);

        initialized = true;
    }

    public static Bitmap get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj) {
        if (obj == null) {
            return null;
        }
        return get(obj.getClass().getName());
    }

    private static Bitmap loadBitmap(Context context, String assetPath) {
        try (InputStream inputStream = context.getAssets().open(assetPath)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IllegalStateException("Failed to decode asset: " + assetPath);
            }
            return bitmap;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load asset: " + assetPath, e);
        }
    }
}
