package net.toancb.shader.helper;

public class Vector2i {
    private int x;
    private int y;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int y() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
