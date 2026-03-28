package edu.hitsz.aircraft;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.application.Main;
import edu.hitsz.factory.prop.PropType;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.factory.prop.UnifiedPropFactory;
import edu.hitsz.strategy.DirectShoot;

import java.util.LinkedList;
import java.util.List;

/**
 * 精英敌机
 * 继承AbstractEnemy，具有射击能力的敌机
 */
public class EliteEnemy extends AbstractEnemy {

    /**
     * 射击方向 (向下为正)
     */
    private int direction = 1;
    /**
     * 子弹伤害
     */
    private int power = 25;

    /**
     * 道具掉落概率
     */
    private double propDropRate = 0.5;
    private final UnifiedPropFactory propFactory = new UnifiedPropFactory().enableRandomDrop(propDropRate);

    /**
     * 精英敌机构造方法
     *
     * @param locationX X 坐标
     * @param locationY Y 坐标
     * @param speedX    X 轴速度
     * @param speedY    Y 轴速度
     * @param hp        生命值
     */
    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.shootNum = 1;
        this.power = 25;
        this.direction = 1;
        this.shootStrategy = new DirectShoot();
        this.shootCycle = 800;
    }

    @Override
    public void forward() {
        super.forward();

        // 检查是否飞出屏幕下方
        if (locationY >= Main.WINDOW_HEIGHT) {
            vanish();
        }
    }

    @Override
    public List<BaseBullet> shoot() {
        return shootStrategy.shoot(
                this.getLocationX(),
                this.getLocationY() + direction * 2,
                this.getSpeedX(),
                this.getSpeedY(),
                this.direction,
                this.shootNum,
                this.power);
    }

    @Override
    public int getScore() {
        return 30;
    }

//    @Override
//    public List<AbstractProp> mayDrop() {
//        return propFactory.generate(this.getLocationX(), this.getLocationY());
//    }

    @Override
    public List<AbstractProp> mayDrop() {
        List<AbstractProp> props = new LinkedList<>();

        // 定义炸弹掉落概率（20%）
        double bombDropProbability = 0.2;
        double rand = Math.random();

        PropType type;
        if (rand < bombDropProbability) {
            // 掉落炸弹道具
            type = PropType.BOMB;
        } else {
            // 其余概率随机生成加血或火力道具
            type = Math.random() < 0.5 ? PropType.BLOOD : PropType.BULLET;
        }

        props.add(propFactory.setType(type).create(getLocationX(), getLocationY()));
        return props;
    }

}