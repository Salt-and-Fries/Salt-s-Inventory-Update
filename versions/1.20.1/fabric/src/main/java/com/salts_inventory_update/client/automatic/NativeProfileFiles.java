package com.salts_inventory_update.client.automatic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.debug.DesktopDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Opt-in resource/config profiles. Existing registered API definitions always win over these. */
public final class NativeProfileFiles {
    private record Entry(String screen,DesktopNativeLayout layout,int priority,String source) { }
    private static final Map<ResourceLocation,Entry> PROFILES=new HashMap<>();
    private static boolean loaded;
    private NativeProfileFiles() { }
    public static synchronized void reload(){loaded=false;PROFILES.clear();load();}
    public static synchronized List<String> menuIds(){load();return PROFILES.keySet().stream().map(Object::toString).sorted().toList();}
    public static synchronized boolean has(MenuType<?> type){load();return PROFILES.containsKey(BuiltInRegistries.MENU.getKey(type));}
    public static synchronized DesktopNativeTemplate<?,?> find(MenuType<?> type,String screen){
        load();var e=PROFILES.get(BuiltInRegistries.MENU.getKey(type));if(e==null||!e.screen().isEmpty()&&!e.screen().equals(screen))return null;
        return new DesktopNativeTemplate<net.minecraft.world.inventory.AbstractContainerMenu,Void>(){
            @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<net.minecraft.world.inventory.AbstractContainerMenu,Void> c){return e.layout();}
        };
    }
    private static void load(){
        if(loaded)return;var mc=Minecraft.getInstance();
        if(mc==null||mc.getResourceManager()==null)return;
        loaded=true;
        var resources=mc.getResourceManager().listResources("siu/native_windows",id->id.getPath().endsWith(".json"));
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(256).forEach(e->{try(var reader=e.getValue().openAsReader()){
            var data=new StringBuilder();var chunk=new char[4096];int count;
            while((count=reader.read(chunk))!=-1){if(data.length()+count>262144)throw new IllegalArgumentException("Profile is too large");data.append(chunk,0,count);}
            parse(JsonParser.parseString(data.toString()).getAsJsonObject(),0,e.getKey().toString());
        }catch(Exception failure){DesktopDebug.warn("native resource profile declined {}: {}",e.getKey(),failure.toString());}});
        var dir=mc.gameDirectory.toPath().resolve("config/salts_inventory_update/native-windows");
        if(Files.isDirectory(dir))try(var files=Files.list(dir)){
            files.filter(p->p.getFileName().toString().endsWith(".json")).sorted().limit(256).forEach(p->{try{
                if(Files.size(p)>262144)throw new IllegalArgumentException("Profile is too large");
                parse(JsonParser.parseString(Files.readString(p,StandardCharsets.UTF_8)).getAsJsonObject(),1000,p.getFileName().toString());
            }catch(Exception failure){DesktopDebug.warn("native config profile declined {}: {}",p.getFileName(),failure.toString());}});
        }catch(java.io.IOException failure){DesktopDebug.warn("native profiles unavailable: {}",failure.toString());}
    }
    private static void parse(JsonObject json,int basePriority,String source){
        if(json.has("minecraft")&&!json.get("minecraft").getAsString().equals(com.salts_inventory_update.VersionInfo.MINECRAFT_VERSION))return;
        var id=new ResourceLocation(json.get("menu").getAsString());
        if(!BuiltInRegistries.MENU.containsKey(id))return;
        var entry=new Entry(json.has("screenType")?json.get("screenType").getAsString():"",DesktopNativeProfiles.decode(json.getAsJsonObject("layout")),
            basePriority+(json.has("priority")?Math.max(-100,Math.min(100,json.get("priority").getAsInt())):0),source);
        var previous=PROFILES.get(id);
        if(previous==null||entry.priority()>previous.priority())PROFILES.put(id,entry);
        else if(entry.priority()==previous.priority())DesktopDebug.warn("native profile conflict for {}: keeping {}, declining {}",id,previous.source(),source);
    }
}
