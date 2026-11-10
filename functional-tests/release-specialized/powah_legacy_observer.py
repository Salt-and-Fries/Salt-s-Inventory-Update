"""Read original Powah5 controls, tile values and complete native item owners."""


def enhance(text,mc,named=False):
    assert mc=='1.20.1'and named is False
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    commands=r'''
        if(kind.equals("regression-powah-legacy-state")){
            var clients=new ArrayList<Object>();Object desktop=regressionH("screen");
            if(desktop!=null)for(Object window:(List<?>)field(desktop,"windows")){
                Object session=field(window,"session");if(session==null||field(window,"apiState")==null)continue;
                var menu=(AbstractContainerMenu)coreCall(session,"menu");if(!menu.getClass().getName().startsWith("owmii.powah.inventory."))continue;
                Object screen=field(field(window,"apiState"),"screen");var row=new LinkedHashMap<String,Object>();
                row.put("session",coreCall(session,"sessionId"));row.put("menu",menu.getClass().getName());row.put("screen",screen.getClass().getName());
                row.put("locked",field(window,"locked"));row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
                row.put("values",releasePowahLegacyValues(menu));clients.add(row);
            }
            var ray=new LinkedHashMap<String,Object>();
            if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit){
                ray.put("position",List.of(hit.getBlockPos().getX(),hit.getBlockPos().getY(),hit.getBlockPos().getZ()));
                ray.put("hit",List.of(hit.getLocation().x,hit.getLocation().y,hit.getLocation().z));ray.put("face",hit.getDirection().getName());
            }
            work=server(()->{
                var p=player();var menus=new ArrayList<Object>();Object state=((Map<?,?>)field(com.salts_inventory_update.server.DesktopContainerSessions.class,"PLAYERS")).get(p.connection);
                if(state==null)throw new IllegalStateException("Actual negotiated native state absent");
                int owned=((Map<?,?>)field(state,"sessions")).size();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");if(!menu.getClass().getName().startsWith("owmii.powah.inventory."))continue;
                    var slots=new ArrayList<Object>();
                    for(int i=0;i<menu.slots.size();i++){
                        var slot=menu.getSlot(i);if(slot.container==p.getInventory())continue;
                        slots.add(Map.of("index",i,"containerIndex",slot.getContainerSlot(),"virtual",com.salts_inventory_update.api.desktop.DesktopMenuBindings.isVirtualSlot(menu,slot),"item",releasePowahLegacyStack(slot.getItem())));
                    }
                    var row=new LinkedHashMap<String,Object>();row.put("session",entry.getKey());row.put("menu",menu.getClass().getName());row.put("valid",menu.stillValid(p));
                    row.put("sameWorldTile",field(menu,"te")==p.serverLevel().getBlockEntity(POS));
                    row.put("values",releasePowahLegacyValues(menu));row.put("slots",slots);row.put("cursor",releasePowahLegacyStack(menu.getCarried()));menus.add(row);
                }
                var inventory=new ArrayList<Object>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releasePowahLegacyStack(p.getInventory().getItem(i))));
                var server=new LinkedHashMap<String,Object>();server.put("menus",menus);server.put("ownedSessions",owned);server.put("inventory",inventory);
                server.put("cursor",releasePowahLegacyStack(p.inventoryMenu.getCarried()));server.put("uuid",p.getUUID().toString());server.put("name",p.getDisplayName().getString());
                var tile=p.serverLevel().getBlockEntity(POS);if(tile==null||!tile.getClass().getName().startsWith("owmii.powah."))throw new IllegalStateException("Original Powah fixture tile absent");
                var ownedItems=new ArrayList<Object>();boolean holder;
                try{holder=Class.forName("owmii.powah.lib.block.IInventoryHolder").isInstance(tile);}
                catch(ClassNotFoundException e){throw new IllegalStateException("Exact Powah inventory contract missing",e);}
                if(holder){
                    Object machine=coreCall(tile,"getInventory");int size=((Number)coreCall(machine,"getSlots")).intValue();
                    for(int i=0;i<size;i++)ownedItems.add(Map.of("index",i,"item",releasePowahLegacyStack((ItemStack)coreCall(machine,"getStackInSlot",i))));
                }
                server.put("world",Map.of("tile",tile.getClass().getName(),"inventoryHolder",holder,"items",ownedItems,"nbt",tile.saveWithFullMetadata().toString()));
                write("release-powah-legacy-state.json",Map.of("client",clients,"server",server,"ray",ray));return null;
            });return true;
        }
'''
    helpers=r'''
    static String releasePowahLegacyStack(ItemStack value){return value.isEmpty()?"{}":value.save(new net.minecraft.nbt.CompoundTag()).toString();}
    static Map<String,Object> releasePowahLegacyValues(AbstractContainerMenu menu){
        Object tile=field(menu,"te"),sides=coreCall(tile,"getSideConfig"),energy=coreCall(tile,"getEnergy");
        var row=new LinkedHashMap<String,Object>();row.put("tile",tile.getClass().getName());row.put("redstone",((Enum<?>)coreCall(tile,"getRedstoneMode")).name());
        row.put("transfer",((Enum<?>)coreCall(tile,"getTransferType")).name());row.put("stored",coreCall(energy,"getStored"));row.put("capacity",coreCall(energy,"getCapacity"));
        var faces=new ArrayList<Object>();var available=new ArrayList<Object>();
        for(var side:net.minecraft.core.Direction.values()){
            faces.add(((Enum<?>)coreCall(sides,"getType",side)).name());available.add(coreCall(tile,"isEnergyPresent",side));
        }
        row.put("faces",faces);row.put("available",available);
        if(menu.getClass().getSimpleName().equals("CableContainer"))row.put("face",((net.minecraft.core.Direction)coreCall(menu,"getSide")).getName());
        if(menu.getClass().getSimpleName().equals("SolarContainer"))row.put("canSeeSky",coreCall(tile,"canSeeSky"));
        row.put("nbt",((net.minecraft.world.level.block.entity.BlockEntity)tile).saveWithFullMetadata().toString());return row;
    }
'''
    text=text.replace(anchor,anchor+commands,1);return text[:text.rfind('}')]+helpers+'\n}\n'
