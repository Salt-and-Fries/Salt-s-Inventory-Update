package com.salts_inventory_update.controlify.qa;

import java.util.LinkedHashMap;
import java.util.Map;

/** QA-only observations of native vertices after queued GUI states have been converted. */
public final class WorkspaceOpacityProbe {
    private static long fractionalCalls, premultipliedCalls, ordinaryCalls, fullOpacityCalls, deferredFractionalCalls;
    private static float minimum = 1, maximum;
    private static final java.util.List<String> inputEdges = new java.util.ArrayList<>();

    private WorkspaceOpacityProbe() { }

    public static synchronized void reset() {
        fractionalCalls = premultipliedCalls = ordinaryCalls = fullOpacityCalls = deferredFractionalCalls = 0;
        minimum = 1;
        maximum = 0;
        inputEdges.clear();
    }

    public static synchronized void observe(float capturedOpacity, boolean premultiplied) {
        // Read the immutable value captured for this queued state's vertex consumer. The
        // current thread scope has already closed by the time native conversion can run.
        if (capturedOpacity > 0 && capturedOpacity < 1) {
            fractionalCalls++;
            if (com.salts_inventory_update.controlify.render.WorkspaceOpacity.current() == 1) deferredFractionalCalls++;
            if (premultiplied) premultipliedCalls++; else ordinaryCalls++;
            minimum = Math.min(minimum, capturedOpacity);
            maximum = Math.max(maximum, capturedOpacity);
        } else if (capturedOpacity == 1) {
            fullOpacityCalls++;
        }
    }

    public static synchronized long fractionalCalls() { return fractionalCalls; }
    public static synchronized long premultipliedCalls() { return premultipliedCalls; }
    public static synchronized long ordinaryCalls() { return ordinaryCalls; }
    public static synchronized long deferredFractionalCalls() { return deferredFractionalCalls; }

    public static synchronized Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fractionalColorCalls", fractionalCalls);
        result.put("premultipliedColorCalls", premultipliedCalls);
        result.put("ordinaryColorCalls", ordinaryCalls);
        result.put("deferredFractionalColorCalls", deferredFractionalCalls);
        result.put("fullOpacityColorCalls", fullOpacityCalls);
        result.put("minimumCapturedOpacity", minimum);
        result.put("maximumCapturedOpacity", maximum);
        result.put("nativeInputEdges", new java.util.ArrayList<>(inputEdges));
        return result;
    }

    public static synchronized void observeInput(String phase, dev.isxander.controlify.controller.ControllerEntity controller,
            dev.isxander.controlify.screenop.ScreenProcessor<?> processor) {
        var input = controller.input().orElseThrow();
        var button = dev.isxander.controlify.controller.input.GamepadInputs.MISC_1_BUTTON;
        if (!input.stateNow().isButtonDown(button) || input.stateThen().isButtonDown(button)) return;
        var screen = (com.salts_inventory_update.controlify.input.WorkspaceContainerScreen) processor.screen;
        var quickMove = dev.isxander.controlify.bindings.ControlifyBindings.INV_QUICK_MOVE.on(controller);
        var slot = screen.controllerHoveredSlot();
        String line = "phase=" + phase + ", quickPressed=" + quickMove.justPressed()
                + ", quickDown=" + quickMove.digitalNow() + ", hovered="
                + (slot == null ? "none" : slot.index + ":" + slot.getItem())
                + ", cursor=" + dev.isxander.controlify.Controlify.instance().virtualMouseHandler().getCurrentX(1)
                + "," + dev.isxander.controlify.Controlify.instance().virtualMouseHandler().getCurrentY(1)
                + ", radial=" + com.salts_inventory_update.controlify.input.ControlifyInputBridge.suppressVirtualMouseRendering()
                + ", lt=" + input.stateNow().getAxisState(dev.isxander.controlify.controller.input.GamepadInputs.LEFT_TRIGGER_AXIS)
                + ", backDown=" + dev.isxander.controlify.bindings.ControlifyBindings.GUI_BACK.on(controller).digitalNow()
                + ", backPrevious=" + dev.isxander.controlify.bindings.ControlifyBindings.GUI_BACK.on(controller).boundInput().state(input.stateThen());
        inputEdges.add(line);
        System.out.println("CONTROLIFY_QA_NATIVE_INPUT " + line);
    }
}
