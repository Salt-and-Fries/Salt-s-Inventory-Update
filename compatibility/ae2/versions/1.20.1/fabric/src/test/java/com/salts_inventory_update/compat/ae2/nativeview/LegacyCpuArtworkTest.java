package com.salts_inventory_update.compat.ae2.nativeview;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyCpuArtworkTest {
    private static final class Canvas implements LegacyCpuArtwork.Brush {
        final int width; final int[] pixels;
        Canvas(int width, int height) { this.width=width; pixels=new int[width*height]; }
        @Override public void fill(int x0,int y0,int x1,int y1,int argb) {
            for (int y=y0;y<y1;y++) Arrays.fill(pixels,y*width+x0,y*width+x1,argb);
        }
        String hash() throws Exception {
            byte[] rgba=new byte[pixels.length*4];
            for (int i=0;i<pixels.length;i++) {
                int c=pixels[i];rgba[4*i]=(byte)(c>>16);rgba[4*i+1]=(byte)(c>>8);
                rgba[4*i+2]=(byte)c;rgba[4*i+3]=(byte)(c>>24);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rgba));
        }
    }
    @Test void allSevenRegionsMatchTheMeasuredUpstreamPixels() throws Exception {
        // Digests measured independently from the original upstream PNG regions.
        var expected=Map.of(
            "row","3c1812506c97e9e03eee196a511bc92b8e07f866a6beec0b2adc671b4f438d6d",
            "rowSelected","08de849d5edeae0a0b7d65729526a3c1b884fcd925b5367252c96a55cbe2eac6",
            "storage","49702f2388e90f4a8bedac5e249a87b7ddf6704b27ecf944bd45b9f89835292f",
            "processor","86446b8f184251c4bcc7180682e9bff2f409bf15668b413ba247e86272511284",
            "craft","d17677a4321329de44710e592d945eb1dd48655704ac24309f9847d9bd110580",
            "terminal","f753f8eff410c2e160c9aeef850daff7b5523a14e5cd9cb7fc3bc839058bfa9a",
            "machine","16c505459b01fa1bb51a088d079baf417dd938324c657b0edf52a2647cecf11d");
        for (var entry:expected.entrySet()) {
            var canvas=new Canvas(entry.getKey().startsWith("row")?67:10,entry.getKey().startsWith("row")?22:10);
            LegacyCpuArtwork.paint(entry.getKey(),canvas,0,0);
            assertEquals(entry.getValue(),canvas.hash(),entry.getKey());
        }
    }
    @Test void resizedRowsKeepThreePixelCapsAndRepeatOnlyTheCenter() {
        for (boolean selected:new boolean[]{false,true}) {
            var source=new Canvas(67,22);LegacyCpuArtwork.paint(selected?"rowSelected":"row",source,0,0);
            for (int width:new int[]{6,40,67,95,143}) {
                var target=new Canvas(width,22);LegacyCpuArtwork.button(target,0,0,width,selected);
                for (int y=0;y<22;y++) for (int x=0;x<width;x++) {
                    int sx=x<3?x:x>=width-3?64+x-(width-3):3+(x-3)%61;
                    assertEquals(source.pixels[y*67+sx],target.pixels[y*width+x],width+" at "+x+","+y);
                }
            }
        }
    }
}
