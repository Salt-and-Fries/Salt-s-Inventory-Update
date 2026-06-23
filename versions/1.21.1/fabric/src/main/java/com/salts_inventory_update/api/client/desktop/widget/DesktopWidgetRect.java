package com.salts_inventory_update.api.client.desktop.widget;

public record DesktopWidgetRect(int x, int y, int width, int height) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x
            && mouseY >= this.y
            && mouseX < this.x + this.width
            && mouseY < this.y + this.height;
    }
}
