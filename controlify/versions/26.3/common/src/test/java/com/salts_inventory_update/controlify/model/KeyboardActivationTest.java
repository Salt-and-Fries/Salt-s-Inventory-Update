package com.salts_inventory_update.controlify.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class KeyboardActivationTest {
    @Test void autoFocusedSearchDoesNotHijackAnItemClick() {assertFalse(KeyboardActivation.shouldOpen(true,true,true,false));assertFalse(KeyboardActivation.shouldOpen(false,true,true,false));}
    @Test void selectingTheFieldReopensTheKeyboardWithRetainedFocus() {assertTrue(KeyboardActivation.shouldOpen(true,true,true,true));}
    @Test void FocusOnlyOwnersOpenOnAnActualEditorFocusChange() {assertTrue(KeyboardActivation.shouldOpen(false,true,false,false));assertFalse(KeyboardActivation.shouldOpen(true,true,false,false));}
    @Test void nonEditableControlsNeverOpenTheKeyboard() {assertFalse(KeyboardActivation.shouldOpen(false,false,true,true));}
}
