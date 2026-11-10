"""Read original Gallery choices and held-item ledgers; never select a variant here."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    assert mc in ['1.20.1','1.21.1']
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    commands=r'''
        if(kind.equals("regression-gallery-state")||kind.equals("regression-gallery-ledger")){
            var client=new LinkedHashMap<String,Object>();
            if(kind.equals("regression-gallery-state")){
                Object window=releaseGalleryWindow(),screen=field(field(field(window,"apiDefinition"),"initial"),"screen");
                var menu=(AbstractContainerMenu)call(screen,"getMenu");Object search=field(screen,"searchBox");
                client.put("menu",menu.getClass().getName());client.put("screen",screen.getClass().getName());client.put("slots",menu.slots.size());
                client.put("selected",releaseGalleryVariantId(call(menu,"getSelectedPainting")));
                client.put("paintings",releaseGalleryVariants((List<?>)call(menu,"getPaintings")));
                client.put("allPaintings",releaseGalleryVariants((List<?>)call(menu,"getAllPaintings")));
                client.put("search",call(search,"getValue"));client.put("searchFocused",call(search,"isFocused"));
                client.put("startIndex",field(screen,"startIndex"));client.put("scrollOffs",field(screen,"scrollOffs"));
                client.put("searchPoint",Map.of("x",((Number)call(search,"getX")).intValue()+8-((Number)field(screen,"leftPos")).intValue(),
                    "y",((Number)call(search,"getY")).intValue()+4-((Number)field(screen,"topPos")).intValue()));
                client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
                client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            }
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
                if(player().containerMenu!=player().inventoryMenu)menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var galleries=menus.stream().filter(m->m.getClass().getName().equals("com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu")).toList();
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseGalleryStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseGalleryStack(m.getCarried())));
                var result=new LinkedHashMap<String,Object>();result.put("client",client);result.put("menus",galleries.size());result.put("inventory",inventory);result.put("cursors",cursors);
                result.put("selected",galleries.size()==1?releaseGalleryVariantId(call(galleries.get(0),"getSelectedPainting")):"");
                write("release-gallery-state.json",result);return null;
            });return true;
        }
        if(kind.equals("regression-gallery-input")){
            Object window=releaseGalleryWindow(),desktop=h("screen"),definition=field(window,"apiDefinition"),context=call(desktop,"apiContext",window,null,0,0);
            call(desktop,"bringToFront",window);call(desktop,"setFocusedWindow",window);
            int sx=num(c,"source_x",-1),sy=num(c,"source_y",-1);String action=str(c,"action","");
            if(!List.of("tap","scroll").contains(action))throw new IllegalArgumentException("Explicit native mouse action required");
            var point=new com.salts_inventory_update.api.client.desktop.DesktopWindowBounds(sx,sy,1,1);
            for(Object region:(List<?>)call(call(definition,"layout"),"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))&&Boolean.TRUE.equals(call(region,"covers",point))){
                Object source=call(region,"source");double x=((Number)call(definition,"regionX",context,region)).doubleValue()+sx-((Number)call(source,"x")).doubleValue();
                double y=((Number)call(definition,"regionY",context,region)).doubleValue()+sy-((Number)call(source,"y")).doubleValue();
                call(desktop,"mouseMoved",x,y);
                if(action.equals("scroll"))h("scroll",x,y,(double)num(c,"amount",-1));
                else{h("mouse","mouseClicked",x,y,0,0D,0D);h("mouse","mouseReleased",x,y,0,0D,0D);}return true;
            }
            throw new IllegalStateException("Gallery input is outside every retained interactive region");
        }
'''
    helpers=r'''
    static Object releaseGalleryWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object window:(List<?>)field(desktop,"windows")){Object session=field(window,"session");
            if(session!=null&&call(session,"menu").getClass().getName().equals("com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu")&&field(window,"apiDefinition")!=null)found.add(window);}
        if(found.size()!=1)throw new IllegalStateException("Expected exactly one Gallery extraction: "+found.size());return found.get(0);
    }
    static String releaseGalleryVariantId(Object holder){
        if(holder==null)return "";Object key=((java.util.Optional<?>)call(holder,"unwrapKey")).orElseThrow();return call(key,"location").toString();
    }
    static List<Object> releaseGalleryVariants(List<?> holders){
        var result=new ArrayList<Object>();for(Object holder:holders){String id=releaseGalleryVariantId(holder);int colon=id.indexOf(':');
            String prefix="painting."+id.substring(0,colon)+"."+id.substring(colon+1).replace('/','.');Object variant=call(holder,"value");
            result.add(Map.of("id",id,"title",net.minecraft.network.chat.Component.translatable(prefix+".title").getString(),
                "author",net.minecraft.network.chat.Component.translatable(prefix+".author").getString(),
                "dimensions",net.minecraft.network.chat.Component.translatable("painting.dimensions",Math.max(1,((Number)call(variant,"GALLERY_WIDTH")).intValue()/16),Math.max(1,((Number)call(variant,"GALLERY_HEIGHT")).intValue()/16)).getString()));
        }return result;
    }
    static String releaseGalleryStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native Gallery item codec refused serialization")).toString();
    }
'''
    helpers=helpers.replace('GALLERY_WIDTH','getWidth'if mc=='1.20.1'else'width').replace('GALLERY_HEIGHT','getHeight'if mc=='1.20.1'else'height')
    if mc!='1.20.1':
        for axis in ['width','height']:
            old='Math.max(1,((Number)call(variant,"'+axis+'")).intValue()/16)'
            assert helpers.count(old)==1
            helpers=helpers.replace(old,'((Number)call(variant,"'+axis+'")).intValue()')
    helpers=storage_item_saves(helpers,mc)
    if not named:
        commands=re.sub(r'\bh\(','regressionH(',commands);commands=re.sub(r'\bcall\(','coreCall(',commands)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+commands,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
