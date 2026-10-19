package com.salts_inventory_update.controlify;

import com.salts_inventory_update.controlify.model.UiSoundPolicy;
import com.salts_inventory_update.controlify.model.WorkspaceSoundSettings;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Companion-owned screen-relative audio; no sound implementation is installed in Core. */
public final class WorkspaceUiSounds {
    private enum Family { NAVIGATION, TRANSITIONS, ACTIONS }
    public enum Action {
        OPEN("open", Family.TRANSITIONS, 160), CLOSE("close", Family.TRANSITIONS, 160),
        FOCUS("navigate", Family.NAVIGATION, 200), CYCLE("window_change", Family.TRANSITIONS, 100),
        TUCK("tuck", Family.TRANSITIONS, 160), RESTORE("restore", Family.TRANSITIONS, 160),
        SCROLL("navigate", Family.NAVIGATION, 0), NAVIGATE("navigate", Family.NAVIGATION, 200),
        CAROUSEL("navigate", Family.NAVIGATION, 0), HOVER("navigate", Family.NAVIGATION, 200),
        CATEGORY("window_change", Family.TRANSITIONS, 120), FILTER("window_change", Family.TRANSITIONS, 140),
        CONFIRM("confirm", Family.ACTIONS, 120), TAB("window_change", Family.TRANSITIONS, 120),
        SORT("window_change", Family.TRANSITIONS, 160), REJECT("unavailable", Family.ACTIONS, 260),
        CRAFT_COMPLETE("craft_complete", Family.ACTIONS, 160),
        REQUEST("window_change", Family.TRANSITIONS, 160);
        private final Identifier event;
        private final Family family;
        private final long intervalMillis;
        Action(String event, Family family, long intervalMillis) {
            this.event = Identifier.fromNamespaceAndPath("salts_inventory_controlify", "ui." + event);
            this.family = family;
            this.intervalMillis = intervalMillis;
        }
        public Identifier event() { return event; }
        public float pitch() { return 1f; }
        public float volume() { return family == Family.NAVIGATION ? .55f : family == Family.TRANSITIONS ? .35f : .4f; }
        public long intervalMillis() { return intervalMillis; }
        public UiSoundPolicy.Priority priority() {
            return family == Family.ACTIONS ? UiSoundPolicy.Priority.RESULT
                : family == Family.NAVIGATION ? UiSoundPolicy.Priority.NAVIGATION : UiSoundPolicy.Priority.STRUCTURE;
        }
        UiSoundPolicy.Cue cue() {
            return new UiSoundPolicy.Cue(name(), priority(), intervalMillis);
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger("salts_inventory_controlify");
    private static final UiSoundPolicy policy = new UiSoundPolicy();
    private static WorkspaceSoundSettings settings = WorkspaceSoundSettings.defaults();
    private static SoundInstance navigationSound;
    private static SoundInstance previewSound;
    private static boolean previewing;
    private static long generation;
    private WorkspaceUiSounds() { }

    static void initialize() {
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/salts_inventory_controlify_sounds.properties");
        try { settings = WorkspaceSoundSettings.load(path); }
        catch (IOException failure) {
            LOGGER.warn("Could not read controller sound settings; keeping inventory sounds off", failure);
            settings = WorkspaceSoundSettings.defaults(path);
        }
    }

    public static WorkspaceSoundSettings settings() { return settings; }

    /** Retains a fresh candidate for this update. Only emit() submits a native sound. */
    public static boolean play(Action action) {
        if (!audible(action)) return false;
        return policy.submit(action.cue(), millis());
    }

    /** One winner per client tick; replaced browsing cues are discarded rather than queued. */
    public static void tick(Minecraft minecraft) {
        if (!nativeVolumeEnabled() || settings.masterVolume() <= 0) {
            reset(); stopPreview();
            return;
        }
        policy.drain(millis()).ifPresent(cue -> emit(Action.valueOf(cue.id())));
    }

    private static boolean nativeVolumeEnabled() {
        var minecraft = Minecraft.getInstance();
        return minecraft.options != null && minecraft.options.getSoundSourceVolume(SoundSource.MASTER) > 0;
    }

    private static double familyGain(Action action) {
        return switch (action.family) {
            case NAVIGATION -> settings.navigationVolume();
            case TRANSITIONS -> settings.transitionsVolume();
            case ACTIONS -> settings.actionsVolume();
        };
    }

    private static boolean audible(Action action) {
        return action != null && nativeVolumeEnabled() && settings.masterVolume() > 0 && familyGain(action) > 0
            && (action != Action.HOVER || settings.hoverEnabled());
    }

    /** One source of actual native emission, also used by the isolated settings preview. */
    private static boolean emit(Action action) {
        if (!audible(action)) return false;
        var minecraft = Minecraft.getInstance();
        if (!previewing && (action.family == Family.NAVIGATION || action.family == Family.ACTIONS) && navigationSound != null) {
            minecraft.getSoundManager().stop(navigationSound);
            navigationSound = null;
        }
        float volume = (float) (action.volume() * settings.masterVolume() * familyGain(action));
        var sound = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(action.event()), action.pitch(), volume);
        if (previewing) previewSound = sound;
        else if (action.family == Family.NAVIGATION) navigationSound = sound;
        minecraft.getSoundManager().play(sound);
        return true;
    }

    public static boolean preview(Action action) {
        stopPreview();
        previewing = true;
        try { return emit(action); }
        finally { previewing = false; }
    }

    public static void stopPreview() {
        if (previewSound == null) return;
        Minecraft.getInstance().getSoundManager().stop(previewSound);
        previewSound = null;
    }

    /** A world/controller transition cannot replay stale navigation in the next workspace. */
    public static void reset() {
        generation++;
        policy.reset();
        if (navigationSound != null) Minecraft.getInstance().getSoundManager().stop(navigationSound);
        navigationSound = null;
    }

    private static long millis() { return System.nanoTime() / 1_000_000L; }
}
