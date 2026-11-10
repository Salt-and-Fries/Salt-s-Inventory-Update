    // Release QA only: read native server settings and the rendered terminal
    // projection. These observations never invoke a native mutation or packet.
    static net.minecraft.core.BlockPos releaseTomsNetworkPos;
    static Class<?> releaseTomsType(String name){try{return Class.forName(name);}catch(ClassNotFoundException error){throw new IllegalStateException(error);}}
    static Object releaseTomsStaticField(String owner,String name){
        try{var field=releaseTomsType(owner).getDeclaredField(name);field.setAccessible(true);return field.get(null);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    static void releaseTomsNetworkFixture(){
        work=server(()->{
            var world=(net.minecraft.server.level.ServerLevel)player().level();
            var facing=(net.minecraft.core.Direction)coreCall(world.getBlockState(POS),"getValue",
                releaseTomsStaticField("com.tom.storagemod.block.AbstractStorageTerminalBlock","FACING"));
            for(int i=1;i<=2;i++){
                var pos=POS.relative(facing,i);
                var block=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(i==1?"toms_storage:inventory_connector":"minecraft:chest"));
                world.setBlock(pos,block.defaultBlockState(),3);
                block.setPlacedBy(world,pos,world.getBlockState(pos),player(),new ItemStack(block));
            }
            releaseTomsNetworkPos=POS.relative(facing,2);
            var chest=(net.minecraft.world.Container)world.getBlockEntity(releaseTomsNetworkPos);
            chest.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIRT,32));
            chest.setItem(1,new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,32));chest.setChanged();
            write("release-toms-network.json",Map.of("x",releaseTomsNetworkPos.getX(),"y",releaseTomsNetworkPos.getY(),"z",releaseTomsNetworkPos.getZ(),
                "scope","Disposable native terminal/connector/chest placement with ordinary setPlacedBy initialization and initial chest stock only."));return null;
        });
    }
    static void releaseTomsObserve(JsonObject c) {
        int sid=num(c,"session",-1);var client=new LinkedHashMap<String,Object>();
        if(sid>0){
            Object window=regressionWindow(sid,null);
            if(window!=null){
                Object definition=field(window,"apiDefinition"),state=field(window,"apiState");
                client.put("definition",definition.getClass().getName());
                if(definition.getClass().getName().endsWith(".TomsStorageDesktopWindows$InventoryLink")){
                    client.put("scrollRow",field(field(state,"listScroll"),"row"));
                    var channels=new ArrayList<Object>();
                    for(Object channel:(List<?>)field(state,"channels"))channels.add(String.valueOf(coreCall(channel,"id")));
                    client.put("channelIds",channels);
                }
                if(definition.getClass().getName().endsWith(".TomsStorageDesktopWindows$Terminal")){
                    Object context=coreCall(regressionDesktop(),"apiContext",window,null,0,0);
                    var entries=new ArrayList<Object>();
                    for(Object entry:(List<?>)coreCall(definition,"filteredEntries",context)){
                        var row=new LinkedHashMap<String,Object>(regressionStack((ItemStack)coreCall(entry,"stack")));
                        row.put("quantity",coreCall(entry,"quantity"));entries.add(row);
                    }
                    int x=((Number)coreCall(definition,"terminalGridX",context)).intValue();
                    int y=((Number)coreCall(definition,"terminalGridY",context)).intValue();
                    client.put("entries",entries);client.put("search",field(state,"search"));
                    client.put("grid",Map.of("x",x,"y",y,"columns",coreCall(definition,"terminalColumns",context),"scrollRow",field(state,"scrollRow")));
                    client.put("searchPoint",Map.of("x",coreCall(definition,"terminalOriginX",context),"y",coreCall(definition,"terminalOriginY",context)));
                }
            }
        }
        work=server(()->{
            var rows=new ArrayList<Object>();
            Object owner=coreCall(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
            for(var session:((Map<?,?>)field(owner,"sessions")).entrySet()){
                Object menu=field(session.getValue(),"menu");String type=menu.getClass().getName();
                if(!type.startsWith("com.tom.storagemod."))continue;
                var row=new LinkedHashMap<String,Object>();row.put("type",type);
                row.put("session",session.getKey());
                if(type.endsWith(".InventoryConfiguratorMenu")){
                    Object filter=field(menu,"filter");
                    row.put("priority",String.valueOf(coreCall(filter,"getPriority")));
                    row.put("side",String.valueOf(coreCall(filter,"getSide")));
                    row.put("skip",coreCall(filter,"skip"));row.put("keepLast",coreCall(filter,"isKeepLast"));
                    row.put("connected",((java.util.Set<?>)coreCall(filter,"getConnectedBlocks")).stream().map(String::valueOf).sorted().toList());
                }
                if(type.endsWith(".InventoryLinkMenu")){
                    Object host=field(menu,"te");row.put("selected",String.valueOf(coreCall(host,"getChannel")));
                    row.put("beaconLevel",coreCall(host,"getBeaconLevel"));
                    Object remote=coreCall(releaseTomsType("com.tom.storagemod.inventory.RemoteConnections"),"get",player().level());
                    var channels=new ArrayList<Object>();
                    try(var stream=(java.util.stream.Stream<?>)coreCall(remote,"streamChannels",player())){
                        stream.forEach(value->{var entry=(Map.Entry<?,?>)value;Object channel=entry.getValue();
                            channels.add(Map.of("id",String.valueOf(entry.getKey()),"name",field(channel,"displayName"),"public",field(channel,"publicChannel"),"owner",String.valueOf(field(channel,"owner"))));});
                    }
                    row.put("channels",channels);
                }
                rows.add(row);
            }
            var chest=new ArrayList<Object>();
            if(releaseTomsNetworkPos!=null){
                var nativeChest=(net.minecraft.world.Container)player().level().getBlockEntity(releaseTomsNetworkPos);
                for(int i=0;i<nativeChest.getContainerSize();i++)chest.add(regressionStack(nativeChest.getItem(i)));
            }
            write("release-toms-state.json",Map.of("client",client,"menus",rows,"chest",chest));return null;
        });
    }
