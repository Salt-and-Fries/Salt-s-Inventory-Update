package com.salts_inventory_update.compat.adastra;

import com.google.gson.Gson;
import com.salts_inventory_update.api.client.desktop.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Measured GUI-pixel geometry; publisher artwork remains in the owning mod. */
final class AdAstraSpec {
    String id,menu,screen,title,texture;
    int width,height,textureHeight,header,plate,labelY,titleY,titleColor,reserve;
    int[] body;
    boolean battery,vehicle;
    Fragment[] fragments;
    static final class Fragment {String id;int[] source,at;int[][] cutouts;}
    private static List<AdAstraSpec> specs;
    static synchronized List<AdAstraSpec> all(){
        if(specs==null)try(var stream=AdAstraSpec.class.getResourceAsStream("/salts_inventory_compat/ad-astra/native-windows.json")){
            if(stream==null)throw new IllegalStateException("Missing Ad Astra native-frame geometry");
            specs=List.of(new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),AdAstraSpec[].class));
        }catch(IOException error){throw new IllegalStateException(error);}
        return specs;
    }
    DesktopWindowBounds body(){return new DesktopWindowBounds(body[0],body[1],body[2]-body[0],body[3]-body[1]);}
    static DesktopWindowBounds box(int[] r){return new DesktopWindowBounds(r[0],r[1],r[2],r[3]);}
}
