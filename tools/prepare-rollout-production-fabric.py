"""Prepare a normal Fabric client for the remaining-UI screenshot audit.

Only the disposable observation mod is mapped from named to intermediary.
Minecraft, Core, Fabric API and audited upstream JARs retain their original
release bytes. No account credentials are read; the local test world uses an
offline test identity. Preparation does not open a game window.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "build/all-target-rollout/production-cache-1211"
GRADLE = Path.home() / ".gradle/caches/modules-2/files-2.1"
MC_CACHE = Path.home() / ".gradle/caches/fabric-loom/1.21.1"
JAVA = Path("C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/java.exe")
HEADERS = {"User-Agent":"SIU-Local-UI-Audit/1.0 (https://github.com/Salt-and-Fries/Salt-s-Inventory-Update)"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def digest(path, algorithm="sha256"):
    return hashlib.new(algorithm, path.read_bytes()).hexdigest()


def fetch(url, path, expected=None, algorithm="sha1"):
    if path.exists() and (not expected or digest(path,algorithm)==expected):
        return path
    path.parent.mkdir(parents=True,exist_ok=True)
    with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=60) as response:
        data=response.read()
    if expected and hashlib.new(algorithm,data).hexdigest()!=expected:
        raise ValueError("Official download checksum mismatch: "+url)
    path.write_bytes(data)
    return path


def coordinate_path(name):
    group,artifact,version,*classifier=name.split(":")
    suffix="-"+classifier[0] if classifier else ""
    return "/".join([group.replace(".","/"),artifact,version,artifact+"-"+version+suffix+".jar"])


def library(record, fabric=False):
    artifact=record if fabric else record["downloads"]["artifact"]
    relative=coordinate_path(record["name"]) if fabric else artifact["path"]
    target=CACHE/"libraries"/relative
    expected=artifact.get("sha1")
    if target.exists() and (not expected or digest(target,"sha1")==expected):
        return target
    group,name,version,*_=record["name"].split(":")
    for existing in (GRADLE/group/name/version).glob("*/"+Path(relative).name):
        if not expected or digest(existing,"sha1")==expected:
            target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(existing,target)
            return target
    url=record["url"].rstrip("/")+"/"+relative if fabric else artifact["url"]
    return fetch(url,target,expected)


def allowed(record):
    value=not bool(record.get("rules"))
    for rule in record.get("rules",[]):
        system=rule.get("os",{})
        matches=(not system.get("name") or system["name"]=="windows")
        matches=matches and (not system.get("arch") or system["arch"] in ("amd64","x86_64"))
        if rule.get("features"):
            matches=False
        if matches:
            value=rule["action"]=="allow"
    return value and not re.search(r"natives-(?:linux|macos|osx|windows-arm64|windows-x86)",record["name"])


def java_argument(value):
    return '"'+str(value).replace("\\","/").replace('"','\\"')+'"'


EXTRA_JAVA = r'''
    static boolean coreMode(){return Boolean.parseBoolean(System.getenv("SIU_AUDIT_CORE"));}
    static void configureCore(){
        if(!coreMode())return;
        try{Class.forName("com.salts_inventory_update.client.SaltsInventoryConfig").getMethod("update",java.util.function.Consumer.class).invoke(null,(java.util.function.Consumer<Object>)config->{
            try{var seen=config.getClass().getField("hasSeenInstructionsWindow");seen.setBoolean(config,true);
                config.getClass().getField("enableMod").setBoolean(config,true);
                ((java.util.Collection<?>)config.getClass().getField("forcedContainerWindows").get(config)).clear();
            }catch(Exception error){throw new IllegalStateException(error);}
        });}catch(Exception error){throw new IllegalStateException(error);}
    }
    static Object coreCall(Object owner,String name,Object...args){
        Class<?> type=owner instanceof Class<?> cls?cls:owner.getClass();
        for(;type!=null;type=type.getSuperclass())for(Method method:type.getDeclaredMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length){
            try{method.setAccessible(true);return method.invoke(owner instanceof Class<?>?null:owner,args);}
            catch(IllegalArgumentException ignored){}catch(Exception error){throw new IllegalStateException(error);}
        }throw new IllegalArgumentException(name);
    }
    static void closeCore(){
        if(!coreMode())return;
        try{coreCall(Class.forName("com.salts_inventory_update.client.InventoryDesktopScreen"),"permanentlyCloseAllOpenWindows",mc);}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    static Field auditField(Class<?> owner,String name)throws NoSuchFieldException{
        try{return owner.getDeclaredField(name);}catch(NoSuchFieldException original){
            String intermediary=switch(name){
                case "leftPos"->"field_2776";case "topPos"->"field_2800";
                case "imageWidth"->"field_2792";case "imageHeight"->"field_2779";
                default->name;
            };
            if(intermediary.equals(name))throw original;
            return owner.getDeclaredField(intermediary);
        }
    }
    static Map<String,Object> coreState(){
        var out=new LinkedHashMap<String,Object>();out.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());
        out.put("guiWidth",mc.getWindow().getGuiScaledWidth());out.put("guiHeight",mc.getWindow().getGuiScaledHeight());
        out.put("worldLoaded",mc.player!=null&&mc.level!=null);
        if(mc.player!=null){out.put("selected",mc.player.getInventory().selected);out.put("held",mc.player.getMainHandItem().save(new net.minecraft.nbt.CompoundTag()).toString());}
        out.put("siu_loaded",true);try{out.put("nativeTemplateMenus",coreCall(Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi"),"nativeTemplateMenuIds"));}catch(Exception ignored){}var windows=new ArrayList<Map<String,Object>>();
        try{Object desktop=coreCall(Class.forName("com.salts_inventory_update.client.InventoryDesktopScreen"),"current",mc);
            var workspace=new ArrayList<Object>();
            if(desktop!=null)for(Object window:(List<?>)field(desktop,"windows")){
                var info=new LinkedHashMap<String,Object>();info.put("kind",field(window,"kind").toString());info.put("name",coreCall(window,"debugName"));
                int wx=((Number)field(window,"x")).intValue(),wy=((Number)field(window,"y")).intValue(),ww=((Number)field(window,"width")).intValue();
                info.put("bounds",Map.of("x",wx,"y",wy,"width",ww,"height",field(window,"height")));
                for(String key:List.of("locked","pinMode","focused","persistentHidden"))try{info.put(key,String.valueOf(field(window,key)));}catch(Exception ignored){}
                int controlY=wy+9;
                try{Object chrome=field(window,"apiChrome");controlY=wy+((Number)coreCall(chrome,"controlTopInset")).intValue()+((Number)coreCall(chrome,"controlHeight")).intValue()/2;}catch(Exception ignored){}
                var controls=new LinkedHashMap<String,Object>();for(int px=wx;px<wx+ww;px++){
                    Object control=coreCall(desktop,"titleBarControlAt",window,(double)px,(double)controlY);
                    if(control!=null)controls.putIfAbsent(control.toString(),Map.of("x",px+1,"y",controlY));
                }info.put("controls",controls);
                if(field(window,"kind").toString().equals("INVENTORY")){
                    var cells=new ArrayList<Object>();var inventorySlots=(List<net.minecraft.world.inventory.Slot>)coreCall(desktop,"mainInventorySlots");
                    Object layout=coreCall(desktop,"storageLayout",window,coreCall(desktop,"inventoryVirtualSlotCount"));
                    int columns=((Number)coreCall(layout,"columns")).intValue(),rows=((Number)coreCall(layout,"visibleRows")).intValue(),scroll=((Number)field(window,"scrollRow")).intValue();
                    int cx=((Number)coreCall(window,"contentX")).intValue(),cy=((Number)coreCall(window,"contentY")).intValue();
                    for(int row=0;row<rows;row++)for(int col=0;col<columns;col++){
                        int index=scroll*columns+row*columns+col;if(index>=inventorySlots.size())continue;
                        var cell=inventorySlots.get(index);cells.add(Map.of("inventorySlot",cell.getContainerSlot(),"x",cx+col*18+8,"y",cy+row*18+8,"item",cell.getItem().save(new net.minecraft.nbt.CompoundTag()).toString()));
                    }info.put("inventoryCells",cells);
                }workspace.add(info);
            }out.put("workspaceWindows",workspace);
            if(desktop!=null)for(Object window:(List<?>)field(desktop,"windows")){
                if(!field(window,"kind").toString().equals("CONTAINER"))continue;
                var row=new LinkedHashMap<String,Object>();row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
                Object definition=field(window,"apiDefinition");row.put("definition",definition==null?"builtin":definition.getClass().getName());
                if(definition!=null)try{row.put("composition",G.toJsonTree(field(definition,"plan")));String templateName=field(definition,"template").getClass().getName();row.put("template",templateName);if(!templateName.contains("TravelersWindows"))row.put("templateState",G.toJsonTree(field(definition,"consumerState")));else row.put("backpackResult",String.valueOf(field(field(definition,"consumerState"),"result")));row.put("hostScrollX",field(definition,"scrollX"));row.put("hostScrollY",field(definition,"scrollY"));}catch(Exception ignored){}
                Object state=field(window,"apiState");if(state!=null){try{Object screen;try{screen=field(state,"screen");}catch(Exception absent){screen=field(state,"content");}row.put("nativeScreen",screen.getClass().getName());try{row.put("layout",field(state,"layout").toString());}catch(Exception absent){}
                    if(screen instanceof net.minecraft.client.gui.screens.Screen nativeScreen){
                        row.put("nativeBounds",Map.of("x",field(screen,"leftPos"),"y",field(screen,"topPos"),"width",field(screen,"imageWidth"),"height",field(screen,"imageHeight")));
                        var widgets=new ArrayList<Object>();auditWidgets(nativeScreen,widgets,0);row.put("nativeWidgets",widgets);
                        row.put("nativeValues",nativeValues(screen));
                    }
                    if(screen.getClass().getSimpleName().equals("GuiSculptorTrade")){Object owner=field(screen,"sculptor");row.put("nativeOwnerDesires",coreCall(owner,"getDesires").toString());row.put("nativeOwnerBlocked",coreCall(owner,"isTestObstructed"));row.put("nativeInput",((Container)field(screen,"inventory")).getItem(0).toString());}
                }catch(Exception ignored){}}
                Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){
                    row.put("menu",menu.getClass().getName());try{row.put("menuId",BuiltInRegistries.MENU.getKey(menu.getType()).toString());}catch(Exception ignored){}
                    try{
                        Class<?> merchant=Class.forName("com.salts_inventory_update.api.desktop.DesktopMerchantAccess");
                        if(merchant.isInstance(menu)){
                            var data=new LinkedHashMap<String,Object>();
                            data.put("selected",field(window,"merchantSelectedTrade"));data.put("scroll",field(window,"merchantScroll"));
                            data.put("xp",coreCall(menu,"desktopXp"));data.put("futureXp",coreCall(menu,"desktopFutureXp"));data.put("level",coreCall(menu,"desktopLevel"));
                            var offers=new ArrayList<Object>();for(Object rawOffer:(List<?>)coreCall(menu,"desktopOffers")){
                                var offer=(net.minecraft.world.item.trading.MerchantOffer)rawOffer;
                                offers.add(Map.of("used",offer.getUses(),"outOfStock",offer.isOutOfStock()));
                            }data.put("offers",offers);row.put("merchant",data);
                        }
                    }catch(ClassNotFoundException absent){}catch(Exception error){row.put("merchantObserverError",error.toString());}
                    var slots=new ArrayList<Object>();for(int i=0;i<menu.slots.size();i++){var s=menu.getSlot(i);slots.add(Map.of("index",i,"x",s.x,"y",s.y,"player",s.container instanceof Inventory,"item",BuiltInRegistries.ITEM.getKey(s.getItem().getItem()).toString(),"count",s.getItem().getCount()));}row.put("slots",slots);
                }windows.add(row);
            }
        }catch(Exception error){out.put("observerError",error.toString());}
        out.put("allWindows",windows);out.put("sessions",windows.size());
        if(!windows.isEmpty()){var latest=windows.get(windows.size()-1);out.put("window",latest.get("bounds"));out.put("menu",latest.get("menu"));out.put("menuId",latest.get("menuId"));out.put("slots",latest.get("slots"));}
        else if(mc.screen instanceof AbstractContainerScreen<?> screen){out.put("menu",screen.getMenu().getClass().getName());try{out.put("menuId",BuiltInRegistries.MENU.getKey(screen.getMenu().getType()).toString());}catch(Exception ignored){}out.put("title",screen.getTitle().getString());out.put("bounds",Map.of("x",field(screen,"leftPos"),"y",field(screen,"topPos"),"width",field(screen,"imageWidth"),"height",field(screen,"imageHeight")));}
        return out;
    }
    static void auditWidgets(net.minecraft.client.gui.components.events.GuiEventListener owner,List<Object> widgets,int depth){
        if(depth>8)return;
        if(owner instanceof net.minecraft.client.gui.components.AbstractWidget w)widgets.add(observeWidget(w));
        if(owner instanceof net.minecraft.client.gui.components.events.ContainerEventHandler parent)for(var child:parent.children())auditWidgets(child,widgets,depth+1);
    }
    static Map<String,Object> nativeValues(Object screen){
        var values=new LinkedHashMap<String,Object>();
        if(screen.getClass().getName().startsWith("net.favouriteless.enchanted.client.screens."))try{
            Object menu=((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).getMenu();
            for(String name:List.of("getCurrentPower","getMaxPower","getRechargeMultiplier","getBurnProgress","getBurnDuration","getCookProgress","getCookDuration","getSpinProgress","getSpinDuration"))try{
                values.put(name,coreCall(menu,name));
            }catch(IllegalArgumentException absent){}
        }catch(Exception error){values.put("enchantedObserverError",error.toString());}
        if(screen.getClass().getName().endsWith(".MachineScreen"))try{
            var menu=((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).getMenu();
            values.put("miComponentBounds",coreCall(Class.forName("com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationContents"),"describe",menu));
        }catch(Exception error){values.put("miObserverError",error.toString());}
        if(screen.getClass().getName().endsWith(".CellCrafterScreen"))try{
            values.put("isRecipeListVisible",field(screen,"isRecipeListVisible"));
            Object browser=field(screen,"recipeList"),box=field(browser,"textBox");
            if(box!=null){values.put("recipeSearch",coreCall(box,"getValue"));values.put("recipeSearchFocused",coreCall(box,"isFocused"));}
        }catch(Exception error){values.put("minecellsObserverError",error.toString());}
        if(screen.getClass().getName().endsWith(".CrystallineFlowerScreen"))try{
            Object menu=((AbstractContainerScreen<?>)screen).getMenu();
            for(String key:List.of("xpBarPercent","xpTier","playerHasXPForTier","consumeSlotFullyObstructed","bottomBlockPosXUpper","bottomBlockPosXLower","bottomBlockPosYUpper","bottomBlockPosYLower","bottomBlockPosZUpper","bottomBlockPosZLower"))
                try{values.put(key,coreCall(field(menu,key),"get"));}catch(Exception absent){}
            values.put("cachedObstructions",field(screen,"cachedObstructions"));
        }catch(Exception error){values.put("flowerObserverError",error.toString());}
        if(screen.getClass().getName().startsWith("com.tiviacz.travelersbackpack.client.screens."))try{
            var s=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen;Object menu=s.getMenu(),wrapper=coreCall(menu,"getWrapper");
            for(String key:List.of("BACKPACK_INV_END","TOOL_START","TOOL_END","UPGRADE_START","UPGRADE_END","CRAFTING_RESULT","CRAFTING_GRID_START","CRAFTING_GRID_END"))try{values.put(key,field(menu,key));}catch(Exception absent){}
            values.put("showTools",coreCall(wrapper,"showToolSlots"));values.put("screenID",coreCall(wrapper,"getScreenID"));values.put("scrollAmount",field(screen,"scrollAmount"));values.put("visibleRows",field(screen,"visibleRows"));
            var widgets=new ArrayList<Object>();for(var child:s.children())if(child.getClass().getName().startsWith("com.tiviacz.travelersbackpack."))try{
                var row=new LinkedHashMap<String,Object>();row.put("type",child.getClass().getSimpleName());row.put("bounds",coreCall(child,"getWidgetSizeAndPos"));try{row.put("open",coreCall(child,"isTabOpened"));}catch(Exception absent){}widgets.add(row);
            }catch(Exception absent){}values.put("customWidgets",widgets);
        }catch(Exception error){values.put("travelersObserverError",error.toString());}
        if(screen.getClass().getName().equals("io.ejekta.bountiful.client.BoardScreen"))try{
            Object handler=coreCall(screen,"getBoardHandler"),inventory=coreCall(handler,"getInventory");
            values.put("completed",coreCall(handler,"getTotalNumComplete"));values.put("selectedBounty",coreCall(inventory,"getSelectedIndex"));
            Object range=coreCall(field(screen,"buttonList"),"getShownRange");
            values.put("firstBountyRow",coreCall(range,"getFirst"));values.put("lastBountyRow",coreCall(range,"getLast"));
            var indices=new ArrayList<Object>();for(Object button:(List<?>)coreCall(screen,"getValidButtons"))indices.add(coreCall(button,"getBountyIndex"));
            values.put("bountyIndices",indices);
        }catch(Exception error){values.put("bountifulObserverError",error.toString());}
        if(screen.getClass().getName().endsWith(".TradingPostScreen"))try{
            values.put("tradeScroll",coreCall(screen,"tradingpost$getScrollOff"));
            values.put("selectedTrade",coreCall(screen,"tradingpost$getShopItem"));
            var nativeScreen=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen;
            for(var widget:nativeScreen.children())if(widget instanceof net.minecraft.client.gui.components.EditBox box)values.put("search",box.getValue());
            var offers=((net.minecraft.world.inventory.MerchantMenu)nativeScreen.getMenu()).getOffers();
            values.put("filteredOfferCount",offers.size());var results=new ArrayList<Object>();
            for(int i=0;i<offers.size();i++)results.add(offers.get(i).getResult().save(new net.minecraft.nbt.CompoundTag()).toString());
            values.put("filteredResults",results);
        }catch(Exception error){values.put("tradingPostObserverError",error.toString());}
        if(screen.getClass().getName().equals("appeng.client.gui.me.common.MEStorageScreen"))try{
            Object repo=field(screen,"repo");
            values.put("repositorySize",coreCall(repo,"size"));values.put("search",coreCall(repo,"getSearchString"));
            values.put("sortOrder",coreCall(screen,"getSortBy").toString());values.put("sortDirection",coreCall(screen,"getSortDir").toString());
            values.put("view",coreCall(screen,"getSortDisplay").toString());
            var entries=new ArrayList<Object>();for(Object entry:(java.util.Collection<?>)coreCall(repo,"getAllEntries")){
                var data=new LinkedHashMap<String,Object>();for(String key:List.of("getWhat","getStoredAmount","getRequestableAmount"))try{data.put(key,String.valueOf(coreCall(entry,key)));}catch(Exception absent){}entries.add(data);
            }values.put("repositoryEntries",entries);
        }catch(Exception error){values.put("aeObserverError",error.toString());}
        for(String name:List.of("isLocked","isAdmin","amounts","itemsToOrder","categories","schedule","selectedAttributes"))try{
            Object value=field(screen,name);if(value==null)continue;
            if(value instanceof Number||value instanceof Boolean||value instanceof String)values.put(name,value);
            else if(value instanceof List<?> list){var rows=new ArrayList<Object>();for(Object element:list){
                if(element instanceof ItemStack stack)rows.add(stack.save(new net.minecraft.nbt.CompoundTag()).toString());
                else if(element instanceof net.minecraft.network.chat.Component component)rows.add(component.getString());
                else if(element instanceof Number||element instanceof Boolean||element instanceof String)rows.add(element);
                else if(field(element,"stack") instanceof ItemStack stack)rows.add(Map.of("item",stack.save(new net.minecraft.nbt.CompoundTag()).toString(),"count",field(element,"count")));
            }values.put(name,rows);}
            else if(name.equals("schedule"))values.put(name,coreCall(value,"write").toString());
        }catch(Exception ignored){}
        return values;
    }
'''


def qa_source(side):
    source=ROOT/"functional-tests/remaining-ui-audit/native/java/com/siu_reference/NativeUiReference.java"
    text=source.read_text(encoding="utf-8")
    old='for(String id:List.of("salts_inventory_update","salts_inventory_compat","salts_inventory_controlify"))'
    assert old in text
    text=text.replace(old,'for(String id:coreMode()?List.of("salts_inventory_compat","salts_inventory_controlify"):List.of("salts_inventory_update","salts_inventory_compat","salts_inventory_controlify"))')
    if side == 'integrated':
        text=text.replace('coreMode()?List.of("salts_inventory_compat","salts_inventory_controlify")', 'coreMode()?List.of("salts_inventory_controlify")')
    text=text.replace('mc.options.pauseOnLostFocus=false;','configureCore();mc.options.pauseOnLostFocus=false;',1)
    text=text.replace('Field f=t.getDeclaredField(name);','Field f=auditField(t,name);')
    text=text.replace('static Map<String,Object> state(){','static Map<String,Object> state(){\n        if(coreMode())return coreState();',1)
    text=text.replace('mc.player.closeContainer();','closeCore();mc.player.closeContainer();')
    text=text.replace('"siu_loaded",false','"siu_loaded",coreMode()')
    text=text.replace('case "inventory"->{mc.setScreen(new InventoryScreen(mc.player));}',
        'case "inventory"->{if(coreMode()){try{Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("openPlayerWorkspace").invoke(null);}catch(Exception e){throw new IllegalStateException(e);}}else mc.setScreen(new InventoryScreen(mc.player));}')
    text=text.replace('method.getName().equals("use")','(method.getName().equals("use")||method.getName().equals("method_9534"))')
    text=text.replace('method.getName().equals("getMenuProvider")','(method.getName().equals("getMenuProvider")||method.getName().equals("method_17454"))')
    text=text.replace('method.getName().equals("newBlockEntity")','(method.getName().equals("newBlockEntity")||method.getName().equals("method_10123"))')
    text=text[:text.rfind('}')]+EXTRA_JAVA+'\n}\n'
    text=text.replace('case "snapshot-server"',r'''case "keybind"->{
                var key=com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(num(c,"key",72));
                net.minecraft.client.KeyMapping.click(key);
            }
            case "workspace-window"->{
                try{
                    Class<?> api=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi");
                    if(((java.util.Optional<?>)api.getMethod("currentWorkspace").invoke(null)).isEmpty())api.getMethod("openPlayerWorkspace").invoke(null);
                    Object workspace=((java.util.Optional<?>)api.getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object kind=java.util.Arrays.stream(Class.forName("com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow$Kind").getEnumConstants()).filter(v->v.toString().equals(str(c,"kind","CHARACTER"))).findFirst().orElseThrow();
                    coreCall(workspace,"ensureWindow",kind);
                }catch(Exception error){throw new IllegalStateException(error);}
            }
            case "snapshot-server"''',1)
    text=text.replace('case "capture", "wait"->{}','case "capture"->{mc.getToasts().clear();mc.gui.getChat().clearMessages(false);}\n            case "wait"->{}')
    text=text.replace('inventory=coreCall(handler,"getInventory")','inventory=coreCall(handler,"getContainer")')
    text=text.replace('createWorldDimensions());','createWorldDimensions(),new TitleScreen());')
    text=text.replace('new ResourceLocation(s)','ResourceLocation.parse(s)')
    text=text.replace('stack.setTag(net.minecraft.nbt.TagParser.parseTag(c.get("snbt").getAsString()))','stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(net.minecraft.nbt.TagParser.parseTag(c.get("snbt").getAsString())))')
    text=text.replace('.saveWithFullMetadata()', '.saveWithFullMetadata(mc.level.registryAccess())')
    text=text.replace('.save(new net.minecraft.nbt.CompoundTag())','.saveOptional(mc.level.registryAccess())')
    text=text.replace('c.get("y").getAsDouble(),c.get("amount").getAsDouble())','c.get("y").getAsDouble(),0,c.get("amount").getAsDouble())')
    return text


def prepare_qa(side):
    source=ROOT/"functional-tests/remaining-ui-audit/native/java/com/siu_reference/NativeUiReference.java"
    text=qa_source(side)
    folder=CACHE/"qa"/hashlib.sha256(text.encode()).hexdigest()[:12]
    mapped=folder/"audit-intermediary.jar"
    if mapped.exists():
        return mapped
    java_dir=folder/"source/com/siu_reference";java_dir.mkdir(parents=True,exist_ok=True)
    (java_dir/"NativeUiReference.java").write_text(text,encoding="utf-8")
    (java_dir/"NativeReferenceFabric.java").write_text('''package com.siu_reference;
public final class NativeReferenceFabric implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc->NativeUiReference.tick());}
}''',encoding="utf-8")
    staged={}
    cp=read(ROOT/"build/all-target-rollout/gameplay/1.21.1-fabric-rollout-actions-15/launch-details.json")["classpath"]
    classes=folder/"classes";classes.mkdir(exist_ok=True)
    arguments=['--release','21','-proc:none','-encoding','UTF-8','-cp',';'.join(cp),'-d',str(classes),
               str(java_dir/"NativeUiReference.java"),str(java_dir/"NativeReferenceFabric.java")]
    args_file=folder/"javac.args";args_file.write_text('\n'.join(map(java_argument,arguments)),encoding="utf-8")
    with (folder/"compile.log").open('w',encoding="utf-8") as log:
        subprocess.run([str(JAVA.with_name('javac.exe')),'@'+str(args_file.resolve())],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
    original=folder/"audit-named.jar"
    with zipfile.ZipFile(original,'w',zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path,path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'native_ui_reference','version':'1.0.0','environment':'client','entrypoints':{'client':['com.siu_reference.NativeReferenceFabric']},'depends':{'minecraft':'1.21.1','fabric-api':'*'}}))
    tiny=next((GRADLE/'net.fabricmc/tiny-remapper/0.14.1').glob('*/*.jar'))
    mapping=next(p for p in (GRADLE/'net.fabricmc/mapping-io/0.8.0').glob('*/*.jar') if not p.name.endswith('-sources.jar'))
    remapper_cp=[str(tiny),str(mapping)]+[s for s in cp if 'org.ow2.asm' in s]
    mappings=MC_CACHE/'loom.mappings.1_21_1.layered+hash.2198-v2/mappings.tiny'
    minecraft=next(s for s in cp if Path(s).name.startswith('minecraft-merged-'))
    command=[str(JAVA),'-cp',';'.join(remapper_cp),'net.fabricmc.tinyremapper.Main',str(original),str(mapped),str(mappings),'named','intermediary',minecraft]
    with (folder/'remap.log').open('w',encoding='utf-8') as log:
        subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
    (folder/'lineage.json').write_text(json.dumps({'scope':'Only disposable QA classes are remapped; subject mod binaries untouched','source':str(source),'source_sha256':digest(source),'named_qa_sha256':digest(original),'intermediary_qa_sha256':digest(mapped),'mappings_sha256':digest(mappings)},indent=2)+'\n',encoding='utf-8')
    return mapped


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixture',type=Path,required=True)
    parser.add_argument('--side',choices=['native','core','integrated'],required=True)
    parser.add_argument('--compat-jar',type=Path)
    parser.add_argument('--core-jar',type=Path)
    parser.add_argument('--desktop-debug',action='store_true')
    parser.add_argument('--run',required=True)
    parser.add_argument('--fabric-loader',default='0.16.14')
    args=parser.parse_args()
    if not re.fullmatch('[a-z0-9-]+',args.run):
        raise ValueError('Invalid run ID')
    fixture=read(args.fixture)
    if (fixture['minecraft'],fixture['loader'])!=('1.21.1','fabric'):
        raise ValueError('Production preparation currently supports Fabric1.21.1 only')
    output=ROOT/'build/remaining-ui-audit/gameplay'/f'1.21.1-fabric-{args.side}-{args.run}'
    runtime=ROOT/'build/remaining-ui-audit/production-runtime'/f'{args.side}-{args.run}'
    if output.exists() or runtime.exists():
        raise ValueError('Preserve previous evidence; choose a fresh run ID')
    qa=prepare_qa(args.side)
    version=read(MC_CACHE/'minecraft-info.json')
    profile_path=CACHE/f'fabric-loader-{args.fabric_loader}-1.21.1.json'
    fetch(f'https://meta.fabricmc.net/v2/versions/loader/1.21.1/{args.fabric_loader}/profile/json',profile_path)
    profile=read(profile_path)
    cp=[library(record,True) for record in profile['libraries']]
    cp += [library(record) for record in version['libraries'] if allowed(record) and 'artifact' in record.get('downloads',{})]
    client=MC_CACHE/'minecraft-client.jar'
    assert digest(client,'sha1')==version['downloads']['client']['sha1']
    cp.append(client)
    assets=Path.home()/'.gradle/caches/fabric-loom/assets'
    asset_index='rollout-1.21.1-'+version['assetIndex']['id']
    fetch(version['assetIndex']['url'],assets/'indexes'/f'{asset_index}.json',version['assetIndex']['sha1'])
    assert digest(assets/'indexes'/f'{asset_index}.json','sha1')==version['assetIndex']['sha1'], 'Official asset-index checksum mismatch'
    runtime.mkdir(parents=True);output.mkdir(parents=True);mods=runtime/'mods';mods.mkdir()
    inputs=[]
    for artifact in fixture['artifacts']:
        # Original root/dependency JARs retain their own nested libraries.
        # Expanded inspection copies are never additional forced root mods.
        if not artifact.get('coordinate'):continue
        path=ROOT/artifact['path'];assert digest(path,'sha512')==artifact['sha512']
        inputs.append(('upstream',path,artifact['filename']))
    # Fabric API normally came from the dev classpath. Use the original audited
    # aggregate release in production, identically for both sides.
    api_meta=CACHE/'fabric-api-versions-1.21.1.json'
    fetch('https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22fabric%22%5D',api_meta)
    release=next(v for v in read(api_meta) if v['version_type']=='release')
    api=next(f for f in release['files'] if f.get('primary'))
    api_file=fetch(api['url'],CACHE/api['filename'],api['hashes']['sha512'],'sha512')
    inputs.append(('runtime-api',api_file,api['filename']))
    inputs.append(('qa',qa,'native-ui-reference.jar'))
    if args.side in ('core','integrated'):
        core=[args.core_jar] if args.core_jar is not None else list((ROOT/'dist/core').glob('salts_inventory_update-1.21.1-Fabric-*.jar'))
        if len(core)!=1:raise ValueError('Ambiguous production Core')
        inputs.append(('core',core[0],core[0].name))
    if args.side=='integrated':
        if args.compat_jar is None or not args.compat_jar.is_file():
            raise ValueError('Integrated evidence requires an explicit built Compatibility JAR')
        inputs.append(('compatibility',args.compat_jar,args.compat_jar.name))
    staged=[]
    for family,path,filename in inputs:
        if filename in {r['filename'] for r in staged}:raise ValueError('Duplicate staged filename '+filename)
        shutil.copy2(path,mods/filename)
        staged.append({'family':family,'file':str(path.resolve()),'filename':filename,'sha256':digest(path),'sha512':digest(path,'sha512')})
    (runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n',encoding='utf-8')
    mod_ids={name for artifact in fixture['artifacts'] for name in artifact.get('mod_ids',[])}
    if 'supplementaries' in mod_ids and 'amendments' not in mod_ids:
        # Every run creates an empty disposable world. This migration notice
        # concerns old saves, and otherwise prevents the observer reaching title.
        config=runtime/'config/supplementaries-client.json';config.parent.mkdir(parents=True,exist_ok=True)
        config.write_text(json.dumps({'general':{'no_amendments_screen':True}},indent=2)+'\n',encoding='utf-8')
    if any('bclib' in a.get('mod_ids',[]) for a in fixture['artifacts']):
        settings={'version':{'didShowWelcome [default: false]':True,'check [default: true]':False}}
        config=runtime/'config/bclib/client.json';config.parent.mkdir(parents=True,exist_ok=True)
        config.write_text(json.dumps(settings,indent=2)+'\n',encoding='utf-8')
        (output/'fixture-settings.json').write_text(json.dumps({'scope':'Disposable audit only: suppress BCLib one-time welcome and optional update check identically on both sides','values':settings},indent=2)+'\n',encoding='utf-8')
    argv=['-Xms512m','-Xmx3g','-XX:ActiveProcessorCount=4','-Dfabric.development=false','-Dfabric.noGui=true','-Dfabric.gameJarPath='+str(client),
          '-Dsalts_inventory_update.desktopDebug='+str(args.desktop_debug).lower(),
          '-Djava.library.path='+str(runtime/'natives'),'-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(runtime/'natives'),
          '-cp',';'.join(map(str,cp)),profile['mainClass'],'--username','UIAudit','--version','1.21.1',
          '--gameDir',str(runtime),'--assetsDir',str(assets),'--assetIndex',asset_index,
          '--uuid','14ea713fe12d3d8bb7b6baf52d52d598','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','900']
    arguments=output/'java.args';arguments.write_text('\n'.join(map(java_argument,argv))+'\n',encoding='utf-8')
    env={'NATIVE_REFERENCE_OUTPUT':str(output.resolve()),'NATIVE_REFERENCE_LOADER':'fabric','SIU_AUDIT_CORE':str(args.side in ('core','integrated')).lower()}
    launch={'java':str(JAVA),'arguments':str(arguments.resolve()),'cwd':str(runtime.resolve()),'environment':env,'log':str((output/'launch.log').resolve())}
    (output/'launch.json').write_text(json.dumps(launch,indent=2)+'\n',encoding='utf-8')
    (output/'staged-artifacts.json').write_text(json.dumps({'minecraft':'1.21.1','loader':'fabric','fabric_loader':args.fabric_loader,
        'runtime':'production intermediary; audited subject release bytes untouched','siu_loaded':args.side in ('core','integrated'),'compatibility_loaded':args.side=='integrated','artifacts':staged,
        'classpath':[{'path':str(p),'sha256':digest(p)} for p in cp]},indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'prepared':str(output),'launch':launch,'upstream_artifacts':len(fixture['artifacts']),'started':False},indent=2))


if __name__=='__main__':
    main()

