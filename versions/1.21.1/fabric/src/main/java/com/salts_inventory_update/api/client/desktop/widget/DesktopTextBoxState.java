package com.salts_inventory_update.api.client.desktop.widget;

public final class DesktopTextBoxState {
    private String text = "";
    private boolean focused;
    private int cursor;
    private int selectionAnchor;
    private int viewStart;

    public String text() {
        return this.text;
    }

    public void text(String text) {
        String value = text == null ? "" : text;
        if (this.text.equals(value)) {
            return;
        }

        this.text = value;
        this.cursor = clampToCodePointBoundary(this.text, Math.min(this.cursor, this.text.length()));
        this.selectionAnchor = clampToCodePointBoundary(this.text, Math.min(this.selectionAnchor, this.text.length()));
        this.viewStart = clampToCodePointBoundary(this.text, Math.min(this.viewStart, this.text.length()));
    }

    public boolean focused() {
        return this.focused;
    }

    public void focused(boolean focused) {
        this.focused = focused;
    }

    public int cursor() {
        return this.cursor;
    }

    public void cursor(int cursor) {
        this.setCursor(cursor, false);
    }

    public int selectionAnchor() {
        return this.selectionAnchor;
    }

    public int selectionStart() {
        return Math.min(this.cursor, this.selectionAnchor);
    }

    public int selectionEnd() {
        return Math.max(this.cursor, this.selectionAnchor);
    }

    public boolean hasSelection() {
        return this.cursor != this.selectionAnchor;
    }

    public String selectedText() {
        return this.hasSelection() ? this.text.substring(this.selectionStart(), this.selectionEnd()) : "";
    }

    public int viewStart() {
        return this.viewStart;
    }

    public void viewStart(int viewStart) {
        this.viewStart = clampToCodePointBoundary(this.text, viewStart);
    }

    public void selectAll() {
        this.cursor = this.text.length();
        this.selectionAnchor = 0;
    }

    public void clearSelection() {
        this.selectionAnchor = this.cursor;
    }

    public void moveToStart(boolean selecting) {
        this.setCursor(0, selecting);
    }

    public void moveToEnd(boolean selecting) {
        this.setCursor(this.text.length(), selecting);
    }

    public void moveByCodePoints(int amount, boolean selecting) {
        if (amount == 0) {
            return;
        }

        int next = this.cursor;
        int steps = Math.abs(amount);
        for (int i = 0; i < steps; i++) {
            if (amount < 0) {
                if (next <= 0) {
                    break;
                }
                next = this.text.offsetByCodePoints(next, -1);
            } else {
                if (next >= this.text.length()) {
                    break;
                }
                next = this.text.offsetByCodePoints(next, 1);
            }
        }
        this.setCursor(next, selecting);
    }

    public void moveByWords(int amount, boolean selecting) {
        if (amount < 0) {
            this.setCursor(this.previousWordBoundary(this.cursor), selecting);
        } else if (amount > 0) {
            this.setCursor(this.nextWordBoundary(this.cursor), selecting);
        }
    }

    public boolean insert(String value) {
        return this.insert(value, Integer.MAX_VALUE);
    }

    public boolean insert(String value, int maxLength) {
        String filtered = filterText(value);
        if (filtered.isEmpty() && !this.hasSelection()) {
            return false;
        }

        int start = this.selectionStart();
        int end = this.selectionEnd();
        int limit = Math.max(0, maxLength);
        int available = limit - (this.text.length() - (end - start));
        if (available <= 0) {
            filtered = "";
        } else if (filtered.length() > available) {
            filtered = filtered.substring(0, clampToCodePointBoundary(filtered, available));
        }

        String next = this.text.substring(0, start) + filtered + this.text.substring(end);
        if (this.text.equals(next)) {
            this.setCursor(start, false);
            return false;
        }

        this.text = next;
        this.setCursor(start + filtered.length(), false);
        return true;
    }

    public boolean deletePrevious() {
        if (this.hasSelection()) {
            return this.deleteSelection();
        }
        if (this.cursor <= 0) {
            return false;
        }

        int start = this.text.offsetByCodePoints(this.cursor, -1);
        this.text = this.text.substring(0, start) + this.text.substring(this.cursor);
        this.setCursor(start, false);
        return true;
    }

    public boolean deleteNext() {
        if (this.hasSelection()) {
            return this.deleteSelection();
        }
        if (this.cursor >= this.text.length()) {
            return false;
        }

        int end = this.text.offsetByCodePoints(this.cursor, 1);
        this.text = this.text.substring(0, this.cursor) + this.text.substring(end);
        this.setCursor(this.cursor, false);
        return true;
    }

    public boolean deletePreviousWord() {
        if (this.hasSelection()) {
            return this.deleteSelection();
        }
        int start = this.previousWordBoundary(this.cursor);
        return this.deleteRange(start, this.cursor);
    }

    public boolean deleteNextWord() {
        if (this.hasSelection()) {
            return this.deleteSelection();
        }
        int end = this.nextWordBoundary(this.cursor);
        return this.deleteRange(this.cursor, end);
    }

    public boolean deleteSelection() {
        return this.deleteRange(this.selectionStart(), this.selectionEnd());
    }

    private boolean deleteRange(int start, int end) {
        start = clampToCodePointBoundary(this.text, start);
        end = clampToCodePointBoundary(this.text, end);
        if (start >= end) {
            this.setCursor(start, false);
            return false;
        }

        this.text = this.text.substring(0, start) + this.text.substring(end);
        this.setCursor(start, false);
        return true;
    }

    private void setCursor(int cursor, boolean selecting) {
        int next = clampToCodePointBoundary(this.text, cursor);
        if (!selecting) {
            this.selectionAnchor = next;
        }
        this.cursor = next;
    }

    private int previousWordBoundary(int index) {
        int current = clampToCodePointBoundary(this.text, index);
        while (current > 0) {
            int previous = this.text.offsetByCodePoints(current, -1);
            if (!Character.isWhitespace(this.text.codePointAt(previous))) {
                break;
            }
            current = previous;
        }
        while (current > 0) {
            int previous = this.text.offsetByCodePoints(current, -1);
            if (Character.isWhitespace(this.text.codePointAt(previous))) {
                break;
            }
            current = previous;
        }
        return current;
    }

    private int nextWordBoundary(int index) {
        int current = clampToCodePointBoundary(this.text, index);
        while (current < this.text.length()) {
            int codePoint = this.text.codePointAt(current);
            if (Character.isWhitespace(codePoint)) {
                break;
            }
            current = this.text.offsetByCodePoints(current, 1);
        }
        while (current < this.text.length()) {
            int codePoint = this.text.codePointAt(current);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            current = this.text.offsetByCodePoints(current, 1);
        }
        return current;
    }

    public static String filterText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(text.length());
        text.codePoints()
            .filter(DesktopTextBoxState::isAllowedTextCodePoint)
            .forEach(result::appendCodePoint);
        return result.toString();
    }

    public static boolean isAllowedTextCodePoint(int codePoint) {
        return codePoint >= ' ' && codePoint != 127;
    }

    private static int clampToCodePointBoundary(String text, int index) {
        int clamped = Math.max(0, Math.min(index, text.length()));
        if (clamped > 0 && clamped < text.length() && Character.isLowSurrogate(text.charAt(clamped))) {
            return clamped - 1;
        }
        return clamped;
    }
}
