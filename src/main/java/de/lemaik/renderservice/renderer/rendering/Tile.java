package de.lemaik.renderservice.renderer.rendering;

import se.llbit.chunky.renderer.scene.Scene;

public class Tile {
    private int x;

    private int y;

    private int width;

    private int height;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void applyToScene(Scene scene, int fullWidth, int fullHeight) {
        scene.setCanvasCropSize(
                getWidth(), getHeight(),
                fullWidth, fullHeight,
                getX(), getY()
        );
    }
}
