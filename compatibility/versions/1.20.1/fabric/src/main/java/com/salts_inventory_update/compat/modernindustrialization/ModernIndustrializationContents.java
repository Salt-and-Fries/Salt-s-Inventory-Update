package com.salts_inventory_update.compat.modernindustrialization;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native component extents, including artwork that is invisible while idle. */
public final class ModernIndustrializationContents {
    private ModernIndustrializationContents() { }
    public record Content(String type,DesktopWindowBounds bounds,boolean reserved) { }
    private static int parameter(Object component,String name){
        Object params=ModernIndustrializationClient.field(component,"params");
        Object value=params==null?null:ModernIndustrializationClient.field(params,name);
        if(value instanceof Number number)return number.intValue();
        throw new IllegalStateException("Missing MI component parameter: "+component.getClass().getName()+"."+name);
    }
    private static DesktopWindowBounds multiblock(){
        try{
            var type=Class.forName("aztech.modern_industrialization.machines.guicomponents.CraftingMultiblockGui");
            return new DesktopWindowBounds(type.getField("X").getInt(null),type.getField("Y").getInt(null),
                type.getField("W").getInt(null),type.getField("H").getInt(null));
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("MI multiblock canvas",failure);}
    }
    public static List<Content> components(AbstractContainerMenu menu,int imageWidth,int playerTop){
        var result=new ArrayList<Content>();
        Object storage=ModernIndustrializationClient.field(menu,"components");
        if(!(storage instanceof Iterable<?> components))return List.of(new Content("unknown-machine",fallback(imageWidth,playerTop),true));
        for(Object component:components){
            String name=component.getClass().getSimpleName();DesktopWindowBounds bounds=null;boolean reserved=false;
            try{
                switch(name){
                    case "EnergyBarClient" -> bounds=new DesktopWindowBounds(parameter(component,"renderX"),parameter(component,"renderY"),13,18);
                    case "ProgressBarClient", "GunpowderOverclockGuiClient" -> {
                        bounds=new DesktopWindowBounds(parameter(component,"renderX"),parameter(component,"renderY"),20,20);
                        reserved=name.equals("GunpowderOverclockGuiClient");
                    }
                    case "RecipeEfficiencyBarClient" -> bounds=new DesktopWindowBounds(parameter(component,"renderX")-1,parameter(component,"renderY")-1,102,4);
                    case "TemperatureBarClient" -> bounds=new DesktopWindowBounds(parameter(component,"renderX")-22,parameter(component,"renderY")-9,124,20);
                    case "LargeTankFluidDisplayClient" -> bounds=new DesktopWindowBounds(70,12,46,62);
                    case "CraftingMultiblockGuiClient", "GeneratorMultiblockGuiClient" -> {bounds=multiblock();reserved=true;}
                    case "NuclearReactorGuiClient" -> {
                        // The empty error state does not reveal the live reactor
                        // grid, mode display or efficiency text. Keep its canvas.
                        bounds=new DesktopWindowBounds(4,4,imageWidth-8,Math.max(1,playerTop-5));reserved=true;
                    }
                    case "AutoExtractClient", "ReiSlotLockingClient", "ShapeSelectionClient", "SlotPanelClient", "MachineProblemsDisplayClient" -> { }
                    default -> {bounds=fallback(imageWidth,playerTop);reserved=true;}
                }
            }catch(RuntimeException incompatible){bounds=fallback(imageWidth,playerTop);reserved=true;}
            if(bounds!=null)result.add(new Content(name,bounds,reserved));
        }
        return List.copyOf(result);
    }
    private static DesktopWindowBounds fallback(int width,int playerTop){return new DesktopWindowBounds(4,14,width-8,Math.max(1,playerTop-27));}
    /** Read-only geometry for the excluded UI observer, never a source of fake state. */
    public static List<Map<String,Object>> describe(AbstractContainerMenu menu){
        Object params=ModernIndustrializationClient.field(menu,"guiParams");
        int width=((Number)ModernIndustrializationClient.field(params,"backgroundWidth")).intValue();
        int top=((Number)ModernIndustrializationClient.field(params,"playerInventoryY")).intValue();
        return components(menu,width,top).stream().map(p->Map.<String,Object>of("component",p.type(),"x",p.bounds().x(),"y",p.bounds().y(),
            "width",p.bounds().width(),"height",p.bounds().height(),"reserved",p.reserved())).toList();
    }
}
