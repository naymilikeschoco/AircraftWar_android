package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.strategy.ScatterShoot;

public class BulletProp extends AbstractProp {
    private static final int DURATION = 4000;

    public BulletProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void effect(HeroAircraft heroAircraft) {
        System.out.println("FireSupply active! Change to ScatterShoot.");
        heroAircraft.activatePowerUp(new ScatterShoot(), 3, DURATION);
    }

    @Override
    public void update() {
        // 道具不受炸弹影响，空实现
    }
}
