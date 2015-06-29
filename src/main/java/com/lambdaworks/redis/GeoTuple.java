package com.lambdaworks.redis;

/**
 * A tuple consisting of two numerical geo data points.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoTuple {

    private Number x;
    private Number y;

    public GeoTuple() {
    }

    public Number getX() {
        return x;
    }

    public void setX(Number x) {
        this.x = x;
    }

    public Number getY() {
        return y;
    }

    public void setY(Number y) {
        this.y = y;
    }
}
