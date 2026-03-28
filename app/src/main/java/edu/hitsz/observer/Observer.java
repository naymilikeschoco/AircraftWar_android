package edu.hitsz.observer;

/**
 * 观察者接口，用于响应炸弹爆炸事件
 */
public interface Observer {
    /**
     * 炸弹生效时的响应方法
     */
    void update();
}