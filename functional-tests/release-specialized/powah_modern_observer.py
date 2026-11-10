"""Read exact modern Powah native owners, components and real settings only."""


def enhance(text, mc, named=False):
    assert named and mc in ['1.21.1', '26.1.2']
    anchor = 'if(kind.equals("regression-world"))return true;'
    assert text.count(anchor) == 1
    commands = r'''
        if(kind.equals("regression-powah-modern-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop!=null)for(Object window:(List<?>)field(desktop,"windows")){
                Object session=field(window,"session");if(session==null||field(window,"apiState")==null)continue;
                var menu=(AbstractContainerMenu)call(session,"menu");if(!menu.getClass().getName().startsWith("owmii.powah.inventory."))continue;
                Object screen=field(field(window,"apiState"),"screen");var row=new LinkedHashMap<String,Object>();
                row.put("session",call(session,"sessionId"));row.put("menu",menu.getClass().getName());row.put("screen",screen.getClass().getName());
                row.put("locked",field(window,"locked"));row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
                row.put("values",releasePowahModernValues(menu));clients.add(row);
            }
            var ray=new LinkedHashMap<String,Object>();
            if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit){
                ray.put("position",List.of(hit.getBlockPos().getX(),hit.getBlockPos().getY(),hit.getBlockPos().getZ()));
                ray.put("hit",List.of(hit.getLocation().x,hit.getLocation().y,hit.getLocation().z));ray.put("face",hit.getDirection().getName());
            }
            work=server(()->{
                var p=player();var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");
                var nativeMenus=new ArrayList<AbstractContainerMenu>();
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(p,menu->{nativeMenus.add(menu);return false;});
                var menus=new ArrayList<Object>();
                for(var menu:nativeMenus){
                    if(!menu.getClass().getName().startsWith("owmii.powah.inventory."))throw new IllegalStateException("Unexpected other owned source");
                    var slots=new ArrayList<Object>();
                    for(int i=0;i<menu.slots.size();i++){
                        var slot=menu.getSlot(i);if(slot.container==p.getInventory())continue;
                        slots.add(Map.of("index",i,"containerIndex",slot.getContainerSlot(),"virtual",com.salts_inventory_update.api.desktop.DesktopMenuBindings.isVirtualSlot(menu,slot),"item",releasePowahModernStack(slot.getItem())));
                    }
                    var row=new LinkedHashMap<String,Object>();row.put("session",call(menu,"desktopSyncId"));row.put("menu",menu.getClass().getName());row.put("valid",menu.stillValid(p));
                    row.put("sameWorldTile",field(menu,"TILE_FIELD")==level.getBlockEntity(origin()));
                    row.put("values",releasePowahModernValues(menu));row.put("slots",slots);row.put("cursor",releasePowahModernStack(menu.getCarried()));menus.add(row);
                }
                var inventory=new ArrayList<Object>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releasePowahModernStack(p.getInventory().getItem(i))));
                var observed=new LinkedHashMap<String,Object>();observed.put("menus",menus);observed.put("ownedSessions",nativeMenus.size());observed.put("inventory",inventory);
                observed.put("cursor",releasePowahModernStack(p.inventoryMenu.getCarried()));observed.put("uuid",p.getUUID().toString());observed.put("name",p.getDisplayName().getString());
                var tile=level.getBlockEntity(origin());if(tile==null||!tile.getClass().getName().startsWith("owmii.powah."))throw new IllegalStateException("Original Powah fixture tile absent");
                var ownedItems=new ArrayList<Object>();boolean holder;
                try{holder=Class.forName("owmii.powah.lib.block.IInventoryHolder").isInstance(tile);}
                catch(ClassNotFoundException e){throw new IllegalStateException("Exact Powah inventory contract missing",e);}
                if(holder){
                    Object machine=call(tile,"getInventory");int size=((Number)call(machine,"INVENTORY_SIZE")).intValue();
                    for(int i=0;i<size;i++)ownedItems.add(Map.of("index",i,"item",releasePowahModernStack((ItemStack)call(machine,"getStackInSlot",i))));
                }
                observed.put("world",Map.of("tile",tile.getClass().getName(),"inventoryHolder",holder,"items",ownedItems));
                write("release-powah-modern-state.json",Map.of("client",clients,"server",observed,"ray",ray));return null;
            });return true;
        }
'''
    helpers = r'''
    static String releasePowahModernStack(ItemStack value){
        if(value.isEmpty())return "{}";
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native Powah item codec refused serialization")).toString();
    }
    static Map<String,Object> releasePowahModernValues(AbstractContainerMenu menu){
        Object tile=field(menu,"TILE_FIELD"),sides=call(tile,"getSideConfig"),energy=call(tile,"getEnergy");
        var row=new LinkedHashMap<String,Object>();row.put("tile",tile.getClass().getName());row.put("redstone",((Enum<?>)call(tile,"getRedstoneMode")).name());
        row.put("transfer",((Enum<?>)call(tile,"getTransferType")).name());row.put("stored",call(energy,"getStored"));row.put("capacity",call(energy,"getCapacity"));
        var faces=new ArrayList<Object>();var available=new ArrayList<Object>();
        for(var side:net.minecraft.core.Direction.values()){
            faces.add(((Enum<?>)call(sides,"getType",side)).name());available.add(call(tile,"isEnergyPresent",side));
        }
        row.put("faces",faces);row.put("available",available);
        if(menu.getClass().getSimpleName().equals("CableSUFFIX"))row.put("face",((net.minecraft.core.Direction)call(menu,"getSide")).getName());
        if(menu.getClass().getSimpleName().equals("SolarSUFFIX"))row.put("canSeeSky",call(tile,"canSeeSky"));
        return row;
    }
'''
    for old, new in {'TILE_FIELD': 'te' if mc == '1.21.1' else 'blockEntity',
                     'INVENTORY_SIZE': 'getSlots' if mc == '1.21.1' else 'size',
                     'SUFFIX': 'Container' if mc == '1.21.1' else 'Menu'}.items():
        commands, helpers = commands.replace(old, new), helpers.replace(old, new)
    text = text.replace(anchor, anchor + commands, 1)
    return text[:text.rfind('}')] + helpers + '\n}\n'
