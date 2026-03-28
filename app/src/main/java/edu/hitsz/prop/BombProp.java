package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.observer.Observer;
import edu.hitsz.observer.Subject;

import java.util.ArrayList;
import java.util.List;

public class BombProp extends AbstractProp implements Subject{

    private final List<Observer> observers = new ArrayList<>();

    public BombProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void effect(HeroAircraft heroAircraft) {
        // TODO: Implement bomb effect
        System.out.println("BombSupply active!");

        // 通知all观察者执行各自update()响应
        notifyAllObservers();

        vanish();
    }

    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyAllObservers() {
        for (Observer o : new ArrayList<>(observers)) {
            o.update();
        }
    }

    @Override
    public void update() {
        // 炸弹道具不受炸弹影响，空实现
    }
}
