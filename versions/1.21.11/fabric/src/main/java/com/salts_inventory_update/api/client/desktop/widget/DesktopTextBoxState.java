package com.salts_inventory_update.api.client.desktop.widget;

public final class DesktopTextBoxState {
    private String text = "";
    private boolean focused;

    public String text() {
        return this.text;
    }

    public void text(String text) {
        this.text = text == null ? "" : text;
    }

    public boolean focused() {
        return this.focused;
    }

    public void focused(boolean focused) {
        this.focused = focused;
    }
}
