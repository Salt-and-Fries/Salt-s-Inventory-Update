package com.salts_inventory_update.compat.jei;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RecipeHitBoundsTest {
    @Test void distantArtworkOriginDoesNotMoveOrEnlargeTheIngredientHitTarget() {
        assertEquals(new DesktopWindowBounds(72,18,16,16),RecipeHitBounds.find(new DesktopWindowBounds(-1,-1,90,36),
            (x,y)->x>=72 && x<88 && y>=18 && y<34));
    }
    @Test void narrowTargetsAndEmptyArtworkUseTheRealNativeHitTest() {
        assertEquals(new DesktopWindowBounds(7,9,1,3),RecipeHitBounds.find(new DesktopWindowBounds(0,0,20,20),
            (x,y)->x>=7 && x<8 && y>=9 && y<12));
        assertNull(RecipeHitBounds.find(new DesktopWindowBounds(0,0,20,20),(x,y)->false));
    }
}
