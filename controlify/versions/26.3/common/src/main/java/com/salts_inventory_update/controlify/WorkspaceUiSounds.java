package com.salts_inventory_update.controlify;

import com.salts_inventory_update.controlify.model.UiSoundGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Quiet native UI-source cues, submitted only for a changed or accepted workspace action. */
public final class WorkspaceUiSounds {
    public enum Action {
        OPEN(1.05f, .16f, 160), CLOSE(.8f, .14f, 160), FOCUS(1.15f, .12f, 90), CYCLE(1.1f, .1f, 100),
        TUCK(.75f, .14f, 160), RESTORE(1.05f, .16f, 160), SCROLL(1.25f, .08f, 100), NAVIGATE(1.2f, .08f, 100),
        CAROUSEL(1.3f, .08f, 110), CATEGORY(1.1f, .14f, 120), FILTER(.95f, .14f, 140),
        CONFIRM(1f, .18f, 120), TAB(1.15f, .12f, 120), SORT(1f, .14f, 160), REJECT(.65f, .08f, 220);
        private final float pitch, volume;
        private final long intervalMillis;
        Action(float pitch, float volume, long intervalMillis) { this.pitch = pitch; this.volume = volume; this.intervalMillis = intervalMillis; }
        public float pitch() { return pitch; }
        public float volume() { return volume; }
        public long intervalMillis() { return intervalMillis; }
    }
    private static final UiSoundGate gate = new UiSoundGate();
    private WorkspaceUiSounds() { }

    /** Returns true when a native sound request was submitted; mute settings and throttles return false. */
    public static boolean play(Action action) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.options == null || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.UI) <= 0) return false;
        if (!gate.allow(action.name(), action.intervalMillis(), System.nanoTime() / 1_000_000L)) return false;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), action.pitch(), action.volume()));
        return true;
    }
}
