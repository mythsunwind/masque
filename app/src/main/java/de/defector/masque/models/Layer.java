package de.defector.masque.models;

import android.graphics.Rect;

public class Layer {

    private TYPE type;
    private Rect bounds;
    private boolean selected;
    private float scale = 1f;

    public enum TYPE {
        BLACK, PIXALIZED, EMOJI
    }

    public Layer(TYPE type, Rect bounds) {
        this.type = type;
        this.bounds = bounds;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public Rect getBounds() {
        return getScaledBounds();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void move(int x, int y) {
        bounds.left = bounds.left - x;
        bounds.right = bounds.right - x;
        bounds.top = bounds.top - y;
        bounds.bottom = bounds.bottom - y;
    }

    public void setScale(float factor) {
        scale = factor;
    }

    private Rect getScaledBounds() {
        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        int overlapWidth = (scaledWidth - width) / 2;
        int overlapHeight = (scaledHeight - height) / 2;

        return new Rect(bounds.left - overlapWidth,
                bounds.top - overlapHeight,
                bounds.right + overlapWidth,
                bounds.bottom + overlapHeight);
    }

    public void normalize() {
        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        int overlapWidth = (scaledWidth - width) / 2;
        int overlapHeight = (scaledHeight - height) / 2;

        bounds.left = bounds.left - overlapWidth;
        bounds.right = bounds.right + overlapWidth;
        bounds.top = bounds.top - overlapHeight;
        bounds.bottom = bounds.bottom + overlapHeight;
        scale = 1f;
    }

    public String toString() {
        String selected = isSelected() ? "[*]" : "[ ]";
        return String.format("%s Layer (%s) left: %s right: %s top: %s bottom: %s",
                selected, type, bounds.left, bounds.right, bounds.top, bounds.bottom);
    }
}
