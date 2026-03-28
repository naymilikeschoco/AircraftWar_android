package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.factory.prop.PropType;
import edu.hitsz.factory.prop.UnifiedPropFactory;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.strategy.CircleShoot;

import edu.hitsz.application.ImageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Boss 敌机
 * 悬浮于界面上方，左右移动，环形射击
 */
public class BossEnemy extends AbstractEnemy {

    /**
     * 子弹伤害
     */
    private int power = 40;
    /**
     * 射击方向 (环形)
     */
    private int shootNum = 20;
    /**
     * 道具掉落数量范围
     */
    private final int minDrop = 1;
    private final int maxDrop = 3;

    private final UnifiedPropFactory propFactory = new UnifiedPropFactory();
    private final Random random = new Random();

    public BossEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.shootNum = 20;
        this.power = 15;
        this.direction = 1;
        this.shootStrategy = new CircleShoot();
        this.shootCycle = 1000; // 射击周期
    }

    /**
     * 环形射击，委托给策略实现
     */
    @Override
    public List<BaseBullet> shoot() {
        return shootStrategy.shoot(
                this.getLocationX(),
                this.getLocationY(),
                this.getSpeedX(),
                this.getSpeedY(),
                this.direction,
                this.shootNum,
                this.power);
    }

    @Override
    public void update() {} // Boss不受影响

    @Override
    public int getScore() {
        return 100;
    }

//    @Override
//    public List<AbstractProp> mayDrop() {
//        List<AbstractProp> props = new ArrayList<>();
//        int dropCount = random.nextInt(maxDrop - minDrop + 1) + minDrop;
//        PropType[] propTypes = PropType.values();
//
//        // 计算最大道具宽度与安全间距，确保横向不重叠
//        int maxPropWidth = Math.max(
//                ImageManager.PROP_BLOOD_IMAGE.getWidth(),
//                Math.max(ImageManager.PROP_BOMB_IMAGE.getWidth(), ImageManager.PROP_BULLET_IMAGE.getWidth()));
//        int spacing = Math.max(40, maxPropWidth + 10); // 额外留 10px 缝隙
//        int halfMax = maxPropWidth / 2;
//
//        int baseX = this.getLocationX();
//        int y = this.getLocationY();
//        double mid = (dropCount - 1) / 2.0;
//
//        for (int i = 0; i < dropCount; i++) {
//            int offset = (int) Math.round((i - mid) * spacing);
//            int dropX = baseX + offset;
//            // 以中心点约束在屏幕内，避免出界
//            dropX = Math.max(halfMax, Math.min(Main.WINDOW_WIDTH - halfMax, dropX));
//
//            PropType randomType = propTypes[random.nextInt(propTypes.length)];
//            props.add(propFactory.setType(randomType).create(dropX, y));
//        }
//        return props;
//    }
    @Override
    public List<AbstractProp> mayDrop() {
        List<AbstractProp> props = new ArrayList<>();

        // 掉落道具数量：1~3个
        int dropCount = random.nextInt(maxDrop - minDrop + 1) + minDrop;

        // 计算炸弹掉落概率（例如 10%）
        double bombDropProbability = 0.1;

        // 获取道具图片尺寸用于位置计算
        int propWidth = Math.max(ImageManager.PROP_BLOOD_IMAGE.getWidth(),
                Math.max(ImageManager.PROP_BOMB_IMAGE.getWidth(), ImageManager.PROP_BULLET_IMAGE.getWidth()));
        int offset = Math.max(40, propWidth + 10);
        int halfWidth = propWidth / 2;
        int centerX = this.getLocationX();
        int centerY = this.getLocationY();
        double middle = (dropCount - 1) / 2.0;

        for (int i = 0; i < dropCount; i++) {
            int offsetX = (int) Math.round((i - middle) * offset);
            int x = centerX + offsetX;
            x = Math.max(halfWidth, Math.min(512 - halfWidth, x));

            // 按概率决定掉落炸弹或其他道具
            double rand = random.nextDouble();
            PropType type;
            if (rand < bombDropProbability) {
                type = PropType.BOMB; // 10% 概率掉落炸弹
            } else {
                // 其余概率随机掉落加血或火力道具
                type = random.nextBoolean() ? PropType.BLOOD : PropType.BULLET;
            }

            props.add(propFactory.setType(type).create(x, centerY));
        }

        return props;
    }
}
