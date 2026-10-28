package com.salts_inventory_update.client;

import com.salts_inventory_update.client.automatic.NativeTemplateViewport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeTemplateViewportTest {
    @Test void exactFitHasNoHiddenGutters(){
        assertEquals(new NativeTemplateViewport(162,108,false,false),NativeTemplateViewport.resolve(162,108,162,108,true));
    }
    @Test void narrowBackpackDoesNotAcquireNineColumnMinimum(){
        assertArrayEquals(new int[]{54,18},NativeTemplateViewport.fit(54,18,620,400));
        assertEquals(new NativeTemplateViewport(54,18,false,false),NativeTemplateViewport.resolve(54,18,54,18,true));
    }
    @Test void verticalOverflowCanGrowWidthToAvoidHorizontalScrollbar(){
        var size=NativeTemplateViewport.fit(162,108,620,90);
        assertArrayEquals(new int[]{170,90},size);
        assertEquals(new NativeTemplateViewport(162,90,false,true),NativeTemplateViewport.resolve(162,108,size[0],size[1],true));
    }
    @Test void horizontalOverflowCanGrowHeightToAvoidVerticalScrollbar(){
        var size=NativeTemplateViewport.fit(270,108,230,400);
        assertArrayEquals(new int[]{230,116},size);
        assertEquals(new NativeTemplateViewport(230,108,true,false),NativeTemplateViewport.resolve(270,108,size[0],size[1],true));
    }
    @Test void oneScrollbarCanInduceTheOtherOnConstrainedScreens(){
        assertEquals(new NativeTemplateViewport(154,92,true,true),NativeTemplateViewport.resolve(162,108,162,100,true));
    }
    @Test void nonScrollingAndTinyViewportsRemainWellDefined(){
        assertEquals(new NativeTemplateViewport(54,18,false,false),NativeTemplateViewport.resolve(162,108,54,18,false));
        var tiny=NativeTemplateViewport.resolve(162,108,2,2,true);
        assertEquals(1,tiny.width());assertEquals(1,tiny.height());
    }
}
