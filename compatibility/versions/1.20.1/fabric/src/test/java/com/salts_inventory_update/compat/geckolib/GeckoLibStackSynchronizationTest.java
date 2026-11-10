package com.salts_inventory_update.compat.geckolib;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeckoLibStackSynchronizationTest {
    private static CompoundTag id(int value) {
        CompoundTag tag = new CompoundTag(); tag.putInt("GeckoLibID", value); return tag;
    }

    @Test void nullAndEmptyTagsRepresentTheSameAbsentIdentity() {
        assertEquals(Boolean.TRUE, GeckoLibStackSynchronization.compareMissingIdentities(null, new CompoundTag()));
        assertEquals(Boolean.TRUE, GeckoLibStackSynchronization.compareMissingIdentities(new CompoundTag(), null));
    }
    @Test void twoMissingTagsNeedNoAdditionalGeckoIdentityConstraint() {
        assertEquals(Boolean.TRUE, GeckoLibStackSynchronization.compareMissingIdentities(null, null));
        assertEquals(Boolean.TRUE, GeckoLibStackSynchronization.compareMissingIdentities(new CompoundTag(), new CompoundTag()));
    }
    @Test void unrelatedTagDataIsNotAnAnimationIdentity() {
        CompoundTag tag = new CompoundTag(); tag.putInt("siu_regression_marker", 42);
        assertEquals(Boolean.TRUE, GeckoLibStackSynchronization.compareMissingIdentities(tag, null));
        assertEquals(42, tag.getInt("siu_regression_marker"));
    }
    @Test void aRealZeroIdIsDifferentFromAnAbsentId() {
        assertEquals(Boolean.FALSE, GeckoLibStackSynchronization.compareMissingIdentities(id(0), new CompoundTag()));
        assertEquals(Boolean.FALSE, GeckoLibStackSynchronization.compareMissingIdentities(null, id(0)));
    }
    @Test void otherRealIdsAlsoRequirePresenceOnBothSides() {
        assertEquals(Boolean.FALSE, GeckoLibStackSynchronization.compareMissingIdentities(id(17), null));
        assertEquals(Boolean.FALSE, GeckoLibStackSynchronization.compareMissingIdentities(new CompoundTag(), id(-1)));
    }
    @Test void twoRealIdsAlwaysDelegateToThePublishersComparison() {
        assertNull(GeckoLibStackSynchronization.compareMissingIdentities(id(0), id(0)));
        assertNull(GeckoLibStackSynchronization.compareMissingIdentities(id(0), id(17)));
        assertNull(GeckoLibStackSynchronization.compareMissingIdentities(id(-1), id(17)));
    }
    @Test void malformedButPresentIdentityIsLeftToThePublisherWhenBothPresent() {
        CompoundTag malformed = new CompoundTag(); malformed.putString("GeckoLibID", "unexpected");
        assertNull(GeckoLibStackSynchronization.compareMissingIdentities(malformed, id(17)));
    }
    @Test void observationsNeverChangeEitherTag() {
        CompoundTag first = id(17), second = new CompoundTag(); second.putString("custom", "keep");
        CompoundTag firstBefore = first.copy(), secondBefore = second.copy();
        GeckoLibStackSynchronization.compareMissingIdentities(first, second);
        assertEquals(firstBefore, first); assertEquals(secondBefore, second);
    }
}
