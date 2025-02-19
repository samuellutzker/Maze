package com.sam.maze;

public class Vec3 {
    public float x, y, z;

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vec3(float xyz) {
        x = y = z = xyz;
    }
    public Vec3(Vec3 other) { this(other.x, other.y, other.z); }

    public Vec3 add(Vec3 other) {
        return new Vec3(x+other.x, y+other.y, z+other.z);
    }
    public Vec3 mul(float v) { return new Vec3(v*x, v*y, v*z); }
    public Vec3 normalize() { return mul(1.0f / abs()); }
    public float abs() { return (float)Math.sqrt(x*x + y*y + z*z); }
}
