package com.salts_inventory_update.controlify.qa;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Observes the matrix actually supplied to Core's native carried-item draw. */
public final class QaPlaytestProbe {
    public static boolean inTray;
    public static long trayFrames, sideEffectsBodies;
    public static int trayTexts, trayX, trayY, trayWidth, trayHeight;
    public static boolean trayDark = true, trayNoShadow = true;
    public static void trayText(String text, int color, boolean shadow) {
        if (!inTray || !(text.matches("[0-9]+:[0-9]{2}") || text.equals("\u221e") || text.startsWith("+"))) return;
        trayTexts++; trayDark &= color == 0xFF404040; trayNoShadow &= !shadow;
    }
    public static void trayFrame(int x, int y, int width, int height) {
        if (!inTray) return;
        trayX=x;trayY=y;trayWidth=width;trayHeight=height;
    }
    public static long carriedFrames;
    public static float carriedScale;
    public static void carried(GuiGraphicsExtractor graphics, ItemStack stack) {
        if (stack.isEmpty()) return;
        carriedFrames++;
        carriedScale = graphics.pose().m00();
    }
}
