"""Read native Red Merchant offers, payment/result cells and real builtin geometry."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    assert mc in ['1.20.1','1.21.1']
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    commands=r'''
        if(kind.equals("regression-red-merchant-state")||kind.equals("regression-red-merchant-ledger")){
            var client=new LinkedHashMap<String,Object>();
            if(kind.equals("regression-red-merchant-state")){
                Object window=releaseRedMerchantWindow();var menu=(AbstractContainerMenu)call(field(window,"session"),"menu");
                client.put("menu",menu.getClass().getName());client.put("offers",releaseRedMerchantOffers(menu));
                client.put("selection",field(window,"merchantSelectedTrade"));client.put("scroll",field(window,"merchantScroll"));
                client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
                client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            }
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
                if(player().containerMenu!=player().inventoryMenu)menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var merchants=menus.stream().filter(m->m.getClass().getName().equals("net.mehvahdjukaar.supplementaries.common.inventories.RedMerchantMenu")).toList();
                var result=new LinkedHashMap<String,Object>();result.put("client",client);result.put("menus",merchants.size());
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseRedMerchantStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var menu:menus)cursors.add(Map.of("menu",menu.getClass().getName(),"item",releaseRedMerchantStack(menu.getCarried())));
                result.put("inventory",inventory);result.put("cursors",cursors);
                if(merchants.size()==1){var menu=merchants.get(0);result.put("offers",releaseRedMerchantOffers(menu));
                    result.put("owner",call(field(menu,"trader"),"getUUID").toString());
                    var cells=new ArrayList<String>();for(int i=0;i<3;i++)cells.add(releaseRedMerchantStack(menu.getSlot(i).getItem()));result.put("cells",cells);
                    result.put("foreignAccepted",menu.getSlot(0).mayPlace(new ItemStack(net.minecraft.world.item.Items.DIAMOND)));
                }
                write("release-red-merchant-state.json",result);return null;
            });return true;
        }
        if(kind.equals("regression-red-merchant-row")){
            Object window=releaseRedMerchantWindow(),desktop=h("screen");call(desktop,"bringToFront",window);call(desktop,"setFocusedWindow",window);
            int scroll=((Number)field(window,"merchantScroll")).intValue(),index=num(c,"index",scroll),height=((Number)field(desktop.getClass(),"MERCHANT_TRADE_ROW_HEIGHT")).intValue();
            int visible=((Number)field(desktop.getClass(),"MERCHANT_VISIBLE_TRADES")).intValue();
            if(index<scroll||index>=scroll+visible)throw new IllegalArgumentException("Requested native offer is not visible");
            double x=((Number)call(desktop,"merchantContentX",window)).doubleValue()+((Number)field(desktop.getClass(),"MERCHANT_TRADE_LIST_X")).intValue()+44;
            double y=((Number)call(desktop,"merchantContentY",window)).doubleValue()+((Number)field(desktop.getClass(),"MERCHANT_TRADE_LIST_Y")).intValue()+(index-scroll)*height+height/2;
            call(desktop,"mouseMoved",x,y);
            if(c.has("scroll"))h("scroll",x,y,(double)num(c,"scroll",0));
            else{h("mouse","mouseClicked",x,y,0,0D,0D);h("mouse","mouseReleased",x,y,0,0D,0D);}return true;
        }
'''
    helpers=r'''
    static Object releaseRedMerchantWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object window:(List<?>)field(desktop,"windows")){Object session=field(window,"session");
            if(session!=null&&call(session,"menu").getClass().getName().equals("net.mehvahdjukaar.supplementaries.common.inventories.RedMerchantMenu"))found.add(window);}
        if(found.size()!=1)throw new IllegalStateException("Expected one builtin Red Merchant: "+found.size());return found.get(0);
    }
    static List<Object> releaseRedMerchantOffers(AbstractContainerMenu menu){
        if(!(menu instanceof com.salts_inventory_update.api.desktop.DesktopMerchantAccess access))throw new IllegalStateException("Original merchant lacks its public SIU bridge");
        var result=new ArrayList<Object>();int index=0;for(var offer:access.desktopOffers()){
            result.add(Map.of("index",index++,"costA",releaseRedMerchantStack(offer.getCostA()),"costB",releaseRedMerchantStack(offer.getCostB()),
                "result",releaseRedMerchantStack(offer.getResult()),"uses",offer.getUses(),"maximum",offer.getMaxUses(),"outOfStock",offer.isOutOfStock()));
        }return result;
    }
    static String releaseRedMerchantStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native Red Merchant item codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        commands=re.sub(r'\bh\(','regressionH(',commands);commands=re.sub(r'\bcall\(','coreCall(',commands)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+commands,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
