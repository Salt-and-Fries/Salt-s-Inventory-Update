"""Keep the latest native Anvil text while the authenticated mutation lane is busy."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
CLIENT='''package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;

/** Native text is a replacement value, so only its newest unsent value is retained. */
public final class EasyAnvilsRenameClient {
    private static final java.util.Map<AbstractContainerMenu,Pending> OWNERS=new java.util.IdentityHashMap<>();
    private static boolean initialized;
    private static final class Pending {
        DesktopInputContext<?,?> input;
        String name;
        Pending(DesktopInputContext<?,?> input){this.input=input;}
    }
    private EasyAnvilsRenameClient() { }
    static void initialize(){
        if(initialized)return;
        initialized=true;
        DesktopClientApi.onClientTick(client->flush());
    }
    static void bind(DesktopInputContext<?,?> input){
        if(input.model() instanceof AnvilMenu menu&&menu.getClass().getName().startsWith("fuzs.easyanvils."))
            OWNERS.computeIfAbsent(menu,key->new Pending(input)).input=input;
    }
    public static void changed(AbstractContainerMenu menu,String name){
        var pending=OWNERS.get(menu);
        if(pending!=null&&pending.input.windowHandle().isOpen())pending.name=name;
    }
    static void forget(AbstractContainerMenu menu){OWNERS.remove(menu);}
    private static void flush(){
        var iterator=OWNERS.entrySet().iterator();
        while(iterator.hasNext()){
            var pending=iterator.next().getValue();
            if(!pending.input.windowHandle().isOpen()){iterator.remove();continue;}
            if(pending.name==null)continue;
            String name=pending.name;
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            byte[] bytes;
            try{buffer.writeUtf(name);bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);}
            finally{buffer.release();}
            if(pending.input.sendPayload(CHANNEL,bytes)&&name.equals(pending.name))pending.name=null;
        }
    }
}
'''
MIXIN='''package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.EasyAnvilsRenameClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the normalized native text after its own prediction and formatting. */
@Pseudo
@Mixin(targets="fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen",remap=false)
abstract class EasyAnvilsRenameMixin {
    @Inject(method="onNameChanged(Ljava/lang/String;)V",at=@At("TAIL"),remap=false)
    private void salts$rememberLatestName(String name,CallbackInfo ci){
        EasyAnvilsRenameClient.changed(((AbstractContainerScreen<?>)(Object)this).getMenu(),name);
    }
}
'''
for version in ['1.21.11','26.1.2','26.2','26.3']:
 folder=ROOT/'compatibility/versions'/version/'fabric/src/main/java/com/salts_inventory_update/compat/nativewindows'
 channel='net.minecraft.resources.Identifier.fromNamespaceAndPath("easyanvils","main/1")'
 (folder/'EasyAnvilsRenameClient.java').write_text(CLIENT.replace('CHANNEL',channel))
 mixin=MIXIN if version=='1.21.11' else MIXIN.replace('fuzs.easyanvils.client.','fuzs.easyanvils.common.client.')
 (folder/'mixin/EasyAnvilsRenameMixin.java').write_text(mixin)
 config=ROOT/'compatibility/versions'/version/'common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json'
 data=json.loads(config.read_text())
 if 'EasyAnvilsRenameMixin' not in data['client']:data['client'].insert(0,'EasyAnvilsRenameMixin')
 config.write_text(json.dumps(data,indent=2)+'\n')
 path=folder/'ReviewedNativeWindows.java';text=path.read_text()
 anchor='    public static void initializeClient() {\n'
 addition='        EasyAnvilsRenameClient.initialize();\n'
 if addition not in text:text=text.replace(anchor,anchor+addition)
 text=text.replace('closed(DesktopWindowContext<AbstractContainerMenu,Void> c){MowzieTradeActions.forget(c.model());}',
                   'closed(DesktopWindowContext<AbstractContainerMenu,Void> c){EasyAnvilsRenameClient.forget(c.model());MowzieTradeActions.forget(c.model());}')
 mouse='''                net.minecraft.client.input.MouseButtonEvent e,boolean twice) {
            var p=profile'''
 if 'EasyAnvilsRenameClient.bind(c);' not in text:
  text=text.replace(mouse,'''                net.minecraft.client.input.MouseButtonEvent e,boolean twice) {
            EasyAnvilsRenameClient.bind(c);
            var p=profile''')
  anchor='        @Override public boolean retainPlayerSlot('
  methods='''        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,Void> c,net.minecraft.client.input.KeyEvent e){EasyAnvilsRenameClient.bind(c);return false;}
        @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,Void> c,net.minecraft.client.input.CharacterEvent e){EasyAnvilsRenameClient.bind(c);return false;}
'''
  assert anchor in text,version
  text=text.replace(anchor,methods+anchor)
 path.write_text(text)
 print(version,'native rename latest-value retry')
