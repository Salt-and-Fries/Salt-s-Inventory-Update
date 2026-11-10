"""Read the publisher's analyzer state; input goes through the desktop mouse path.

Never call refreshWidgets, setters, packet handlers or container mutations here.
Expected pool IDs come from the actual decree definitions, independently of the
screen's displayed poolWidgets. This detects a lost native slot-refresh context.
"""
import re


def enhance(text, mc, named=False):
    assert mc == '1.21.1'
    anchor = 'if(kind.equals("regression-world"))return true;'
    assert text.count(anchor) == 1
    commands = r'''
        if(kind.equals("regression-analyzer-state")){
            Object window=releaseAnalyzerWindow(),screen=field(field(window,"apiState"),"screen");
            var nativeMenu=(AbstractContainerMenu)call(screen,"getMenu");
            var client=new LinkedHashMap<String,Object>();client.put("screen",screen.getClass().getName());
            client.put("menu",nativeMenu.getClass().getName());client.put("mode",String.valueOf(call(screen,"getShowMode")));
            client.put("resolution",field(screen,"scanResolution"));
            var pools=new ArrayList<Object>();for(Object widget:(List<?>)field(screen,"poolWidgets"))
                pools.add(Map.of("id",call(call(widget,"getPool"),"getId"),"pixSize",call(widget,"getPixSize")));
            client.put("pools",pools);client.put("decree",releaseAnalyzerStack(nativeMenu.getSlot(0).getItem()));
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            try{
                Object content=field(Class.forName("io.ejekta.bountiful.content.BountifulContent"),"INSTANCE");
                Object component=call(content,"getDECREE_DATA"),data=call(nativeMenu.getSlot(0).getItem(),"get",component);
                var expected=new LinkedHashMap<String,Object>();var ids=new ArrayList<String>();
                if(data!=null)for(Object id:(Iterable<?>)call(data,"ids"))ids.add(id.toString());
                client.put("decreeIds",ids);
                for(String mode:List.of("OBJ","REW")){
                    var values=new java.util.TreeSet<String>();
                    for(Object decree:(Iterable<?>)call(content,"getDecrees"))if(ids.contains(call(decree,"getId")))
                        for(Object pool:(Iterable<?>)call(decree,mode.equals("OBJ")?"getObjectivePools":"getRewardPools"))values.add(call(pool,"getId").toString());
                    expected.put(mode,new ArrayList<String>(values));
                }
                client.put("expectedPools",expected);
            }catch(ClassNotFoundException e){throw new IllegalStateException(e);}
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var analyzers=menus.stream().filter(m->m.getClass().getName().equals("io.ejekta.bountiful.content.gui.AnalyzerScreenHandler")).toList();
                if(analyzers.size()!=1)throw new IllegalStateException("Expected one native analyzer menu");
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseAnalyzerStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var menu:menus)cursors.add(Map.of("menu",menu.getClass().getName(),"item",releaseAnalyzerStack(menu.getCarried())));
                write("release-analyzer-state.json",Map.of("client",client,"decree",releaseAnalyzerStack(analyzers.get(0).getSlot(0).getItem()),"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
        if(kind.equals("regression-analyzer-mouse")){
            double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();String phase=str(c,"phase","");
            if(!List.of("click","drag","release").contains(phase))throw new IllegalArgumentException("Mouse phase required");
            if(phase.equals("click"))h("mouse","mouseClicked",x,y,0,0D,0D);
            else if(phase.equals("drag"))h("mouse","mouseDragged",x,y,0,c.get("dx").getAsDouble(),c.get("dy").getAsDouble());
            else h("mouse","mouseReleased",x,y,0,0D,0D);return true;
        }
        if(kind.equals("regression-analyzer-point")){
            Object w=releaseAnalyzerWindow(),desktop=h("screen"),definition=field(w,"apiDefinition"),state=field(w,"apiState");
            Object context=call(desktop,"apiContext",w,null,0,0);double sx=c.get("source_x").getAsDouble(),sy=c.get("source_y").getAsDouble();
            call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);double x=0,y=0;boolean found=false;
            if(definition.getClass().getName().endsWith(".NativeTemplateWindow")){
                for(Object region:(List<?>)call(call(definition,"layout"),"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))){
                    Object source=call(region,"source");double rx=((Number)call(source,"x")).doubleValue(),ry=((Number)call(source,"y")).doubleValue();
                    if(sx<rx||sy<ry||sx>=rx+((Number)call(source,"width")).doubleValue()||sy>=ry+((Number)call(source,"height")).doubleValue())continue;
                    x=((Number)call(definition,"regionX",context,region)).doubleValue()+sx-rx;y=((Number)call(definition,"regionY",context,region)).doubleValue()+sy-ry;found=true;break;
                }
            }else{
                Object layout=field(state,"layout");double rx=((Number)call(layout,"x")).doubleValue(),ry=((Number)call(layout,"y")).doubleValue();
                if(sx>=rx&&sy>=ry&&sx<rx+((Number)call(layout,"width")).doubleValue()&&sy<ry+((Number)call(layout,"height")).doubleValue()){
                    x=((Number)call(context,"contentX")).doubleValue()+sx-rx;y=((Number)call(context,"contentY")).doubleValue()+sy-ry;found=true;
                }
            }
            if(!found)throw new IllegalStateException("Native analyzer control is outside the interactive extraction: "+sx+","+sy);
            call(desktop,"mouseMoved",x,y);write("release-analyzer-point.json",Map.of("x",x,"y",y));return true;
        }
'''
    helpers = r'''
    static Object releaseAnalyzerWindow(){
        var matches=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object session=field(w,"session");if(session!=null&&call(session,"menu").getClass().getName().equals("io.ejekta.bountiful.content.gui.AnalyzerScreenHandler")&&field(w,"apiState")!=null)matches.add(w);}
        if(matches.size()!=1)throw new IllegalStateException("Expected one analyzer window: "+matches.size());return matches.get(0);
    }
    static String releaseAnalyzerStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native analyzer codec refused serialization")).toString();
    }
'''
    if not named:
        commands = re.sub(r'\bh\(', 'regressionH(', commands)
        commands = re.sub(r'\bcall\(', 'coreCall(', commands)
        helpers = re.sub(r'\bh\(', 'regressionH(', helpers)
        helpers = re.sub(r'\bcall\(', 'coreCall(', helpers)
    text = text.replace(anchor, anchor + commands, 1)
    return text[:text.rfind('}')] + helpers + '\n}\n'
