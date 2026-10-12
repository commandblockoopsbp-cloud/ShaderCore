package net.toancb.shader.helper;

public class Vector2i {
    public static final Vector2i ZERO = new Vector2i(0, 0);
    public static final Vector2i MINUS = new Vector2i(-1, -1);
    private final int x;
    private final int y;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public Vector2i multiply(Vector2i v) {
        return new Vector2i(this.x * v.x(), this.y * v.y());
    }

    public Vector2i divide(Vector2i v) {
        return new Vector2i(this.x / v.x(), this.y / v.y());
    }

    public Vector2i add(Vector2i v) {
        return new Vector2i(this.x + v.x(), this.y + v.y());
    }

    public Vector2i sub(Vector2i v) {
        return this.add(v.multiply(Vector2i.MINUS));
    }
}
