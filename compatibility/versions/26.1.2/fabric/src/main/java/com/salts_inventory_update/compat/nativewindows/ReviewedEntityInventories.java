package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;

/** Original untyped pet menus send custom entity opening packets. */
public final class ReviewedEntityInventories {
    private record Kind(String id,String mod,String menu,String screen,String entity) { }
    private static final List<Kind> KINDS=List.of(
        new Kind("moa","aether_protect_your_moa","com.aetherteam.protect_your_moa.inventory.menu.MoaInventoryMenu","com.aetherteam.protect_your_moa.client.gui.screen.inventory.MoaInventoryScreen","com.aetherteam.aether.entity.passive.Moa"),
        new Kind("guard","guardvillagers","tallestegg.guardvillagers.common.entities.GuardContainer","tallestegg.guardvillagers.client.gui.GuardInventoryScreen","tallestegg.guardvillagers.common.entities.Guard"),
        new Kind("wolf","revampedwolf","baguchan.revampedwolf.inventory.WolfInventoryMenu","baguchan.revampedwolf.client.screen.WolfInventoryScreen","net.minecraft.world.entity.animal.Wolf"),
        new Kind("ministrosity","cataclysm","com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu","com.github.L_Ender.cataclysm.client.gui.MinistrosityInventoryScreen","com.github.L_Ender.cataclysm.entity.Pet.Netherite_Ministrosity_Entity"));
    private static final Map<String,java.util.function.Supplier<MenuType<AbstractContainerMenu>>> TYPES=new HashMap<>();
    private static final Map<AbstractContainerMenu,Entity> ENTITIES=Collections.synchronizedMap(new WeakHashMap<>());
    private ReviewedEntityInventories() { }
    public static MenuType<?> typeFor(String menu){for(var kind:KINDS)if(kind.menu.equals(menu)){var type=TYPES.get(kind.id);return type==null?null:type.get();}return null;}
    public static Entity owner(AbstractContainerMenu menu){return ENTITIES.get(menu);}
    @SuppressWarnings("unchecked")
    public static void initializeCommon(){
        boolean fabric=DesktopEnvironment.isModLoaded("fabricloader");
        if(!fabric&&!DesktopEnvironment.isModLoaded("forge")&&!DesktopEnvironment.isModLoaded("neoforge"))return;
        for(var kind:KINDS)if(DesktopEnvironment.isModLoaded(kind.mod)&&nativeMenuPresent(kind)&&(!fabric||kind.id.equals("moa"))){
            if(TYPES.containsKey(kind.id))continue;
            final java.util.function.Supplier<MenuType<AbstractContainerMenu>> registration;
            try{
                java.util.function.Supplier<MenuType<AbstractContainerMenu>> factory=()->new MenuType<>((id,inventory)->{throw new IllegalStateException("Entity opening data required");},FeatureFlags.DEFAULT_FLAGS);
                if(fabric){
                    // The modern Fabric Moa port also uses an unregistered entity menu.
                    var registered=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.MENU,net.minecraft.resources.Identifier.fromNamespaceAndPath("salts_inventory_compat",kind.id),factory.get());
                    registration=()->registered;
                 }else {
                    boolean neo=DesktopEnvironment.isModLoaded("neoforge");
                    String registrar=neo?"com.salts_inventory_update.compat.platform.LoaderMenus":"com.salts_inventory_update.compat.forge.ReviewedEntityMenuRegistration";
                    registration=(java.util.function.Supplier<MenuType<AbstractContainerMenu>>)Class.forName(registrar)
                        .getMethod(neo?"registerEntity":"register",String.class,java.util.function.Supplier.class).invoke(null,kind.id,factory);
                }
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Forge entity menu registration failed",failure);}
            TYPES.put(kind.id,registration);
            DesktopEnvironment.onServerStarting(server->{
                var type=registration.get();
                if(DesktopServerApi.findWindowHandler(type)!=null)return;
                NativeMenuSupport.bindNative(type,true);
                if(kind.id.equals("ministrosity"))DesktopMenuBindings.registerStorage(type,(menu,slot)->!(slot.container instanceof Inventory));
                DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->{
                    var entity=Objects.requireNonNull(ENTITIES.get(menu));buffer.writeInt(entity.getId());
                    buffer.writeVarInt(kind.id.equals("ministrosity")?cargo(entity,"miniInventory").getContainerSize()
                        :(int)menu.slots.stream().filter(s->s.container!=player.getInventory()).count());
                });
                DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                    @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){var e=ENTITIES.get(menu);return e!=null&&e.isAlive()&&e.level()==player.level()&&e.distanceToSqr(player)<=64&&menu.stillValid(player);}
                    @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
                    @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){ENTITIES.remove(c.menu());}
                });
            });
        }
    }
    private static boolean nativeMenuPresent(Kind kind){
        try{Class.forName(kind.menu,false,ReviewedEntityInventories.class.getClassLoader());return true;}
        catch(ClassNotFoundException absent){return false;}
    }
    private static AbstractContainerMenu menu(Kind kind,int id,Inventory inventory,Container cargo,Entity entity){
        try {
            var constructor=Arrays.stream(Class.forName(kind.menu).getConstructors()).filter(c->{var p=c.getParameterTypes();return p.length==4&&p[0]==int.class&&p[1]==Inventory.class&&p[2]==Container.class&&p[3].isInstance(entity);}).findFirst().orElseThrow();
            var menu=(AbstractContainerMenu)constructor.newInstance(id,inventory,cargo,entity);ENTITIES.put(menu,entity);return menu;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native entity menu constructor changed",failure);}
    }
    public static boolean open(String id,Entity entity,ServerPlayer player,Container cargo){
        var type=TYPES.get(id);if(type==null||!DesktopSessions.shouldCapture(player))return false;
        // Fabric can dispatch both interactAt and interact for the same use.
        // Keep the existing native owner instead of constructing a second menu.
        if(id.equals("moa")&&DesktopEnvironment.isModLoaded("fabricloader")
            &&DesktopSessions.hasOpenSessionMatching(player,m->ENTITIES.get(m)==entity))return true;
        var kind=KINDS.stream().filter(k->k.id.equals(id)).findFirst().orElseThrow();
        var result=DesktopSessions.openMenuSession(player,new SimpleMenuProvider((session,inventory,p)->menu(kind,session,inventory,cargo,entity),entity.getDisplayName()),
            buffer->{buffer.writeInt(entity.getId());buffer.writeVarInt(cargo.getContainerSize());});
        org.slf4j.LoggerFactory.getLogger("SaltsInventoryCompatibility").debug("Reviewed entity opening: kind={} result={}",id,result);
        return result!=null;
    }
    public static Container cargo(Object owner,String field){
        try {
            for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(field);f.setAccessible(true);return (Container)f.get(owner);}catch(NoSuchFieldException next){}
            throw new NoSuchFieldException(field);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Missing native entity inventory",failure);}
    }
    public static void initializeClient(){
        DesktopClientApi.onClientStarted(mc->{for(var kind:KINDS){
            var registration=TYPES.get(kind.id);if(registration==null)continue;var type=registration.get();
            if(kind.id.equals("ministrosity")){
                NativeMenuSupport.bindNative(type,true);
                DesktopMenuBindings.registerStorage(type,(menu,slot)->!(slot.container instanceof Inventory));
            }
            DesktopMenuFactories.register(type,(minecraft,player,id,data)->{
                var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
                try{
                    int entityId=buffer.readInt(),size=buffer.readVarInt();
                    if(size<1||size>256||buffer.isReadable()||minecraft.level==null)throw new IllegalArgumentException("Invalid entity inventory opening");
                    var entity=minecraft.level.getEntity(entityId);
                    if(entity==null||!Class.forName(kind.entity).isInstance(entity))throw new IllegalArgumentException("Missing native inventory owner");
                    // These native validity contracts compare container identity.
                    Container contents=kind.id.equals("wolf")?cargo(entity,"inventory")
                        :kind.id.equals("ministrosity")?cargo(entity,"miniInventory"):new SimpleContainer(size);
                    if(contents.getContainerSize()!=size)throw new IllegalArgumentException("Native entity inventory size changed");
                    return menu(kind,id,player.getInventory(),contents,entity);
                }catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}finally{buffer.release();}
            });
            DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
                var entity=Objects.requireNonNull(ENTITIES.get(menu));
                try{
                    var constructor=Arrays.stream(Class.forName(kind.screen).getConstructors()).filter(c->{var p=c.getParameterTypes();return p.length==3&&p[0].isInstance(menu)&&p[1]==Inventory.class&&p[2].isInstance(entity);}).findFirst().orElseThrow();
                    return (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)constructor.newInstance(menu,inventory,entity);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native entity screen constructor changed",failure);}
            });
        }});
    }
}
