package com.sam.maze;

public class Vec2 {
    public float x, y;
    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }
    public Vec2 add(Vec2 other) {
        return new Vec2(x+other.x, y+other.y);
    }
    public Vec2 mul(float v) {
        return new Vec2(v*x, v*y);
    }

}
