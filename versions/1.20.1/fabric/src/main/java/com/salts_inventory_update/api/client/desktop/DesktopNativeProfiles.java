package com.salts_inventory_update.api.client.desktop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Versioned, client-side geometry only. Profiles cannot grant server inventory permissions. */
public final class DesktopNativeProfiles {
    private DesktopNativeProfiles() { }
    public static JsonObject encode(DesktopNativeLayout layout) {
        var out=new JsonObject();out.addProperty("format",1);out.addProperty("padding",layout.padding());
        out.addProperty("minimumWidth",layout.minimumWidth());out.addProperty("minimumHeight",layout.minimumHeight());out.addProperty("scrollable",layout.scrollable());
        var regions=new JsonArray();
        for(var r:layout.regions()){
            var j=new JsonObject();j.addProperty("id",r.id());j.add("source",rect(r.source()));j.addProperty("x",r.x());j.addProperty("y",r.y());j.addProperty("interactive",r.interactive());
            if(r.exterior())j.addProperty("exterior",true);
            var cuts=new JsonArray();for(var cut:r.cutouts())cuts.add(rect(cut));j.add("cutouts",cuts);regions.add(j);
        }out.add("regions",regions);return out;
    }
    public static DesktopNativeLayout decode(JsonObject json) {
        if(!json.has("format")||json.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported native profile format");
        var list=json.getAsJsonArray("regions");if(list==null||list.size()>64)throw new IllegalArgumentException("Invalid native profile regions");
        var builder=DesktopNativeLayout.builder().padding(integer(json,"padding",0)).canvas(integer(json,"minimumWidth",0),integer(json,"minimumHeight",0))
            .scrollable(!json.has("scrollable")||json.get("scrollable").getAsBoolean());
        for(var entry:list){var j=entry.getAsJsonObject();var cuts=new java.util.ArrayList<DesktopWindowBounds>();
            if(j.has("cutouts")){var a=j.getAsJsonArray("cutouts");if(a.size()>64)throw new IllegalArgumentException("Too many cutouts");for(var cut:a)cuts.add(read(cut.getAsJsonObject()));}
            builder.region(new DesktopNativeRegion(j.get("id").getAsString(),read(j.getAsJsonObject("source")),integer(j,"x",0),integer(j,"y",0),!j.has("interactive")||j.get("interactive").getAsBoolean(),cuts,j.has("exterior")&&j.get("exterior").getAsBoolean()));
        }return builder.build();
    }
    private static int integer(JsonObject j,String name,int fallback){return j.has(name)?j.get(name).getAsInt():fallback;}
    private static JsonObject rect(DesktopWindowBounds b){var j=new JsonObject();j.addProperty("x",b.x());j.addProperty("y",b.y());j.addProperty("width",b.width());j.addProperty("height",b.height());return j;}
    private static DesktopWindowBounds read(JsonObject j){return new DesktopWindowBounds(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("width").getAsInt(),j.get("height").getAsInt());}
}
