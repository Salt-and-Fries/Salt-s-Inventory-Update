"""Disposable Kibe production client with an excluded, focused observation mod.

Uses original publisher and subject JARs; only this observer is remapped.
"""
from pathlib import Path
import importlib.util
import json

ROOT=Path(__file__).resolve().parents[2]

EXTRA=r'''
            case "kibe-source-debug"->work=server(()->{
                var result=new LinkedHashMap<String,Object>();
                try{
                    Class<?> locks=Class.forName("com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks");
                    var f=locks.getDeclaredField("RESOLVERS");f.setAccessible(true);var resolvers=(Map<?,?>)f.get(null);
                    result.put("registered",resolvers.keySet().stream().map(Object::toString).toList());
                    var menus=new ArrayList<AbstractContainerMenu>();
                    Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions")
                        .getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class)
                        .invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{if(m.getClass().getName().contains("CoolerBlockItem"))menus.add(m);return false;});
                    for(var m:menus){var row=new LinkedHashMap<String,Object>();row.put("type",m.getType().toString());row.put("hand",coreCall(m,"getHand").toString());
                        Object resolver=resolvers.get(m.getType());row.put("resolver",resolver==null?"missing":resolver.getClass().getName());
                        if(resolver!=null)try{Object source=coreCall(resolver,"resolve",player(),m);row.put("resolved",source==null?"null":source.getClass().getName());
                            if(source!=null)for(var sf:source.getClass().getDeclaredFields()){sf.setAccessible(true);row.put(sf.getName(),String.valueOf(sf.get(source)));}}
                        catch(Exception error){row.put("error",error.toString());row.put("cause",String.valueOf(error.getCause()));}
                        result.put("menu",row);}
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                write("kibe-source-debug.json",result);return true;
            });
            case "kibe-slot"->{
                try{
                    var workspace=((Optional<?>)Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object window=((List<?>)coreCall(workspace,"windows")).stream().filter(w->coreCall(w,"title").toString().equals(str(c,"title",""))).findFirst().orElseThrow();
                    Object identity=coreCall(coreCall(window,"handle"),"instanceId");
                    var slots=(List<?>)coreCall(workspace,"visibleSlots",identity);
                    Object slot=slots.stream().filter(s->((Number)coreCall(s,"menuSlotId")).intValue()==num(c,"slot",0)).findFirst().orElseThrow();
                    double x=((Number)coreCall(slot,"x")).doubleValue(),y=((Number)coreCall(slot,"y")).doubleValue();
                    coreCall(workspace,"clickAt",identity,x+8,y+8,0,c.has("quick")&&c.get("quick").getAsBoolean()?ClickType.QUICK_MOVE:ClickType.PICKUP);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            }
            case "kibe-window-action"->{
                try{
                    var workspace=((Optional<?>)Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object window=((List<?>)coreCall(workspace,"windows")).stream().filter(w->coreCall(w,"title").toString().equals(str(c,"title",""))).findFirst().orElseThrow();
                    Object handle=coreCall(window,"handle");
                    if(c.has("action")){
                        @SuppressWarnings({"rawtypes","unchecked"}) Object action=Enum.valueOf((Class)Class.forName("com.salts_inventory_update.api.client.desktop.DesktopWindowAction"),c.get("action").getAsString());
                        coreCall(handle,"request",action);
                    }
                    if(c.has("x"))coreCall(handle,"moveTo",num(c,"x",0),num(c,"y",0));
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            }
            case "kibe-quick"->{
                try{
                    var workspace=((Optional<?>)Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object window=((List<?>)coreCall(workspace,"windows")).stream().filter(w->coreCall(w,"title").toString().equals(str(c,"title","Inventory"))).findFirst().orElseThrow();
                    Object identity=coreCall(coreCall(window,"handle"),"instanceId");
                    coreCall(workspace,"clickAt",identity,c.get("x").getAsDouble(),c.get("y").getAsDouble(),0,ClickType.QUICK_MOVE);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            }
            case "kibe-state"->work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();
                if(coreMode())try{Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions")
                    .getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class)
                    .invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{if(m.getClass().getName().startsWith("io.github.lucaargolo.kibe.")||m instanceof net.minecraft.world.inventory.CraftingMenu)menus.add(m);return false;});}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                else menus.add(player().containerMenu);
                var rows=new ArrayList<Object>();
                for(var m:menus){var row=new LinkedHashMap<String,Object>();row.put("class",m.getClass().getName());row.put("id",m.containerId);
                    try{var owner=coreCall(m,"getEntity");if(owner instanceof net.minecraft.world.level.block.entity.BlockEntity te)row.put("ownerNbt",te.saveWithFullMetadata().toString());}catch(Exception absent){}
                    try{row.put("tag",coreCall(m,"getTag").toString());}catch(Exception absent){}
                    var slots=new ArrayList<Object>();for(int i=0;i<m.slots.size();i++){var s=m.getSlot(i);if(!(s.container instanceof Inventory))slots.add(Map.of("index",i,"item",BuiltInRegistries.ITEM.getKey(s.getItem().getItem()).toString(),"count",s.getItem().getCount()));}
                    row.put("slots",slots);rows.add(row);}
                var inv=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++){var s=player().getInventory().getItem(i);inv.add(Map.of("index",i,"item",BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount(),"nbt",s.save(new net.minecraft.nbt.CompoundTag()).toString()));}
                var be=player().serverLevel().getBlockEntity(POS);
                write("kibe-state.json",Map.of("menus",rows,"inventory",inv,"food",player().getFoodData().getFoodLevel(),"block",player().serverLevel().getBlockState(POS).toString(),"blockNbt",be==null?"":be.saveWithFullMetadata().toString()));return true;
            });
            case "kibe-fill"->work=server(()->{
                var be=player().serverLevel().getBlockEntity(POS);
                if(!(be instanceof net.minecraft.world.Container inventory))throw new IllegalStateException("No native Kibe container");
                int index=num(c,"slot",0);inventory.setItem(index,stack(c));inventory.setChanged();
                try{coreCall(be,"sync");}catch(Exception absent){}return true;
            });
            case "kibe-fill-xp"->work=server(()->{
                var be=player().serverLevel().getBlockEntity(POS);var tank=coreCall(be,"getTank");
                var fluid=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(BuiltInRegistries.FLUID.get(id("kibe:liquid_xp")));
                try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                    coreCall(tank,"insert",fluid,(long)num(c,"amount",8000)*81,tx);tx.commit();}
                coreCall(be,"sync");return true;
            });
            case "kibe-runes"->work=server(()->{
                var be=player().serverLevel().getBlockEntity(POS);
                @SuppressWarnings("unchecked") var colors=(Map<Integer,net.minecraft.world.item.DyeColor>)coreCall(be,"getRuneColors");
                for(int i=1;i<=8;i++)colors.put(i,net.minecraft.world.item.DyeColor.byId(i));
                ((net.minecraft.world.level.block.entity.BlockEntity)be).setChanged();coreCall(be,"sync");return true;
            });
            case "kibe-food"->work=server(()->{player().getFoodData().setFoodLevel(num(c,"food",10));player().getFoodData().setSaturation(0);return true;});
            case "kibe-offhand-use"->{mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND);}
'''


if __name__=='__main__':
    spec=importlib.util.spec_from_file_location('kibe_production',ROOT/'tools/prepare-rollout-production-modern.py')
    prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
    old=prep.qa_source
    def qa(side):
        path,text=old(side)
        assert 'case "fixture"->{' in text
        return path,text.replace('case "fixture"->{',EXTRA+'case "fixture"->{',1)
    prep.qa_source=qa
    prep.main()
