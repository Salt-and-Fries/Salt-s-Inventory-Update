package com.salts_inventory_update.controlify;

import com.salts_inventory_update.controlify.model.WorkspaceSoundSettings;
import java.io.IOException;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The native options list supplies scrolling, narration, focus and controller handling. */
public final class WorkspaceSoundSettingsScreen extends OptionsSubScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger("salts_inventory_controlify");
    private static final String PREFIX = "salts_inventory_controlify.sounds.";
    private static final WorkspaceUiSounds.Action[] PREVIEWS = {
        WorkspaceUiSounds.Action.FOCUS, WorkspaceUiSounds.Action.OPEN, WorkspaceUiSounds.Action.CONFIRM,
        WorkspaceUiSounds.Action.REJECT, WorkspaceUiSounds.Action.CLOSE
    };
    private final WorkspaceSoundSettings settings;
    private Button presetButton;
    private int previewIndex;
    private boolean dirty;
    private boolean saveFailed;

    public WorkspaceSoundSettingsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, text("title"));
        settings = WorkspaceUiSounds.settings();
    }

    @Override protected void addOptions() {
        list.addHeader(text(saveFailed ? "save_failed" : "native_volume"));
        presetButton = button("preset", presetMessage(), clicked -> {
            var values = WorkspaceSoundSettings.Preset.values();
            // Custom describes manual adjustments; cycling chooses a complete preset.
            int next = settings.preset() == WorkspaceSoundSettings.Preset.CUSTOM ? 0
                : (settings.preset().ordinal() + 1) % (values.length - 1);
            settings.applyPreset(values[next]);
            dirty = true;
            WorkspaceUiSounds.stopPreview();
            rebuildWidgets();
        });
        list.addBig(presetButton);
        list.addSmall(slider("volume", settings::masterVolume, settings::setMasterVolume),
            slider("navigation", settings::navigationVolume, settings::setNavigationVolume));
        list.addSmall(slider("transitions", settings::transitionsVolume, settings::setTransitionsVolume),
            slider("actions", settings::actionsVolume, settings::setActionsVolume));
        list.addBig(button("hover", hoverMessage(), clicked -> {
            settings.setHoverEnabled(!settings.hoverEnabled());
            dirty = true;
            clicked.setMessage(hoverMessage());
            refreshPreset();
        }));
        list.addHeader(text("preview.heading"));
        list.addSmall(button("preview.cue", previewMessage(), clicked -> {
            WorkspaceUiSounds.stopPreview();
            previewIndex = (previewIndex + 1) % PREVIEWS.length;
            clicked.setMessage(previewMessage());
        }), silentButton("preview.play", clicked -> WorkspaceUiSounds.preview(PREVIEWS[previewIndex])));
        list.addSmall(silentButton("preview.stop", clicked -> WorkspaceUiSounds.stopPreview()),
            button("reset", text("reset"), clicked -> {
                WorkspaceUiSounds.stopPreview();
                settings.resetToDefaults();
                dirty = true;
                rebuildWidgets();
            }));
    }

    private Component presetMessage() {
        return Component.translatable(PREFIX + "preset.value", text("preset." + settings.preset().name().toLowerCase(Locale.ROOT)));
    }

    private Component hoverMessage() {
        return Component.translatable(PREFIX + "hover.value", Component.translatable(settings.hoverEnabled() ? "options.on" : "options.off"));
    }

    private Component previewMessage() {
        return Component.translatable(PREFIX + "preview.cue.value", text("preview." + PREVIEWS[previewIndex].name().toLowerCase(Locale.ROOT)));
    }

    private Button button(String description, Component message, Button.OnPress action) {
        Button button = Button.builder(message, action).width(150).build();
        button.setTooltip(Tooltip.create(text(description + ".description")));
        return button;
    }

    private Button silentButton(String key, Button.OnPress action) {
        Button button = new SilentButton(text(key), action);
        button.setTooltip(Tooltip.create(text(key + ".description")));
        return button;
    }

    private VolumeSlider slider(String key, DoubleSupplier getter, DoubleConsumer setter) {
        VolumeSlider slider = new VolumeSlider(key, getter.getAsDouble(), value -> {
            setter.accept(value);
            dirty = true;
            refreshPreset();
        });
        slider.setTooltip(Tooltip.create(text(key + ".description")));
        return slider;
    }

    private void refreshPreset() { if (presetButton != null) presetButton.setMessage(presetMessage()); }

    private boolean save() {
        if (!dirty) return true;
        try {
            settings.save();
            dirty = false;
            saveFailed = false;
            return true;
        } catch (IOException failure) {
            LOGGER.warn("Could not save controller inventory sound settings", failure);
            saveFailed = true;
            return false;
        }
    }

    @Override public void onClose() {
        WorkspaceUiSounds.stopPreview();
        if (save()) super.onClose();
        else rebuildWidgets();
    }

    @Override public void removed() {
        WorkspaceUiSounds.stopPreview();
        save();
        super.removed();
    }

    private static Component text(String key) { return Component.translatable(PREFIX + key); }

    private static final class VolumeSlider extends AbstractSliderButton {
        private final String key;
        private final DoubleConsumer setter;
        private VolumeSlider(String key, double value, DoubleConsumer setter) {
            super(0, 0, 150, 20, Component.empty(), value);
            this.key = key;
            this.setter = setter;
            updateMessage();
        }
        @Override protected void updateMessage() {
            Component gain = value <= 0 ? Component.translatable("options.off") : Component.literal(Math.round(value * 100) + "%");
            setMessage(Component.translatable(PREFIX + "gain", text(key), gain));
        }
        @Override protected void applyValue() { setter.accept(Math.round(value * 100) / 100.0); }
    }

    /** A preview plays exactly its own cue, without a simultaneous vanilla button click. */
    private static final class SilentButton extends Button.Plain {
        private SilentButton(Component message, OnPress action) { super(0, 0, 150, 20, message, action, DEFAULT_NARRATION); }
        @Override public void playDownSound(SoundManager manager) { }
    }
}
