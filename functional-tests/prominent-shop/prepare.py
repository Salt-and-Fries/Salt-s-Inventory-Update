"""Excluded read-only shop observations plus the existing pointer input fixture."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('forge_prepare',ROOT/'functional-tests/prominent-forge/prepare.py')
forge=importlib.util.module_from_spec(spec);spec.loader.exec_module(forge)
prep=forge.prep;original=prep.qa_source


def source(side):
    s=original(side)
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    observer=r'''
        if(menu.getClass().getName().equals("elocindev.prominentbase.client.gui.CinderTraderScreenHandler")){
            int cx=((Number)coreCall(window,"contentX")).intValue(),cy=((Number)coreCall(window,"contentY")).intValue();
            var screen=(AbstractContainerScreen<?>)Class.forName("com.salts_inventory_update.compat.prominent.ProminentAccess").getMethod("screen",AbstractContainerMenu.class).invoke(null,menu);
            var shop=new LinkedHashMap<String,Object>();shop.put("content",Map.of("x",cx,"y",cy));
            shop.put("group",String.valueOf(field(screen,"currentGroup")));shop.put("groups",String.valueOf(field(screen,"availableGroups")));
            shop.put("remainingTime",field(screen,"remainingTime"));shop.put("portrait",String.valueOf(coreCall(field(menu,"serkonidId"),"get")));
            shop.put("offers",shopOffers(menu));
            var elements=new ArrayList<Object>();Object root=field(field(screen,"uiAdapter"),"rootComponent");
            shopElements(root,elements,cx-((Number)field(screen,"leftPos")).intValue()+64,cy-((Number)field(screen,"topPos")).intValue()+28,0);
            shop.put("elements",elements);row.put("shop",shop);
        }
    '''
    assert anchor in s;s=s.replace(anchor,anchor+observer,1)
    methods=r'''
    static List<Object> shopOffers(Object menu){
        var out=new ArrayList<Object>();Object list=coreCall(field(menu,"trades"),"get");
        for(Object offer:(List<?>)coreCall(list,"trades")){
            var row=new LinkedHashMap<String,Object>();for(String key:List.of("group","priceCinders","rewardCinders","stock","specialPrice"))row.put(key,String.valueOf(coreCall(offer,key)));
            for(String key:List.of("priceItems","rewardItems")){
                var items=new ArrayList<String>();for(Object item:(List<?>)coreCall(offer,key))items.add(((ItemStack)item).save(new net.minecraft.nbt.CompoundTag()).toString());row.put(key,items);
            }out.add(row);
        }return out;
    }
    static void shopElements(Object component,List<Object> out,int dx,int dy,int depth){
        if(component==null||depth>10)return;
        var row=new LinkedHashMap<String,Object>();row.put("type",component.getClass().getName());
        for(String k:List.of("x","y","width","height"))try{int v=((Number)coreCall(component,k)).intValue();row.put(k,v+(k.equals("x")?dx:k.equals("y")?dy:0));}catch(Exception absent){}
        try{row.put("text",String.valueOf(coreCall(component,"text")));}catch(Exception absent){}
        try{row.put("scroll",coreCall(component,"currentScrollPosition"));}catch(Exception absent){}
        out.add(row);try{for(Object child:(List<?>)coreCall(component,"children"))shopElements(child,out,dx,dy,depth+1);}catch(Exception leaf){}
    }
    '''
    s=s.replace('static boolean coreMode()',methods+'\n    static boolean coreMode()',1)
    command=r'''
            case "prominent-shop-state"->work=server(()->{
                try{
                var result=new LinkedHashMap<String,Object>();var menus=new ArrayList<Object>();
                Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions").getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class).invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{
                    if(m.getClass().getName().equals("elocindev.prominentbase.client.gui.CinderTraderScreenHandler"))menus.add(Map.of("offers",shopOffers(m),"valid",m.stillValid(player()),"portrait",coreCall(field(m,"serkonidId"),"get")));return false;
                });result.put("menus",menus);
                var cls=Class.forName("elocindev.prominentbase.data.cinders.CindersComponent");var component=coreCall(cls.getField("KEY").get(null),"get",player());result.put("cinders",coreCall(component,"getCinders"));
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",player().getInventory().getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString()));result.put("inventory",inventory);
                var entities=new ArrayList<Object>();for(var e:player().serverLevel().getAllEntities())if(e.getType()==BuiltInRegistries.ENTITY_TYPE.get(new net.minecraft.resources.ResourceLocation("prominent","serkonid"))){
                    entities.add(Map.of("id",e.getId(),"handlers",((List<?>)field(e,"handlers")).size(),"nbt",e.saveWithoutId(new net.minecraft.nbt.CompoundTag()).toString()));
                }result.put("entities",entities);write("prominent-shop-state.json",result);return true;
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            });
    '''
    return s.replace('case "snapshot-server"',command+'\n            case "snapshot-server"',1)


prep.qa_source=source
if __name__=='__main__':prep.main()
