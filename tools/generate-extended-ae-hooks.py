"""Optional 1.20.1 hooks; targets/packets remain original mod implementations."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main'
JAVA=BASE/'java/com/salts_inventory_update/compat/ae2/extended/mixin';JAVA.mkdir(parents=True,exist_ok=True)
targets=[]
targets.append('appeng.menu.implementations.PatternAccessTermMenu')
for loader in ['fabric','forge-release']:
 source=ROOT/'build/extended-ae-window-design/source'/loader/'src/main/java'
 for p in source.rglob('Container*.java'):
  if'void broadcastChanges('in p.read_text():targets.append(p.relative_to(source).with_suffix('').as_posix().replace('/','.'))
head='package com.salts_inventory_update.compat.ae2.extended.mixin;\nimport org.spongepowered.asm.mixin.*;\nimport org.spongepowered.asm.mixin.injection.*;\nimport org.spongepowered.asm.mixin.injection.callback.CallbackInfo;\nimport com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;\n'
(JAVA/'ContainerUpdatesMixin.java').write_text(head+'import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;\nimport com.llamalad7.mixinextras.injector.wrapoperation.Operation;\n@Pseudo\n@Mixin(targets='+json.dumps(sorted(set(targets))).replace('[','{').replace(']','}')+',remap=false)\npublic abstract class ContainerUpdatesMixin {\n @WrapMethod(method={"broadcastChanges","method_7623","m_38946_"},remap=false) private void siu$updates(Operation<Void> original){ExtendedAePackets.scoped(this,()->original.call());}\n}\n')
net=['com.github.glodblock.extendedae.network.EAENetworkServer','com.github.glodblock.extendedae.network.EAENetworkClient','com.glodblock.github.glodium.network.NetworkHandler']
(JAVA/'NetworkMixin.java').write_text(head+'import net.minecraft.server.level.ServerPlayer;\n@Pseudo\n@Mixin(targets='+json.dumps(net).replace('[','{').replace(']','}')+',remap=false)\npublic abstract class NetworkMixin {\n @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,require=0,remap=false) private void siu$action(@Coerce Object packet,CallbackInfo ci){if(ExtendedAePackets.clientSend(packet))ci.cancel();}\n @Inject(method="sendTo",at=@At("HEAD"),cancellable=true,require=0,remap=false) private void siu$update(@Coerce Object packet,ServerPlayer player,CallbackInfo ci){if(ExtendedAePackets.serverSend(packet,player))ci.cancel();}\n}\n')
classes=['com.github.glodblock.extendedae.client.gui.GuiPatternModifier','com.glodblock.github.extendedae.client.gui.GuiPatternModifier']
(JAVA/'PatternModifierLabelsMixin.java').write_text(head+'import net.minecraft.client.gui.GuiGraphics;\nimport com.salts_inventory_update.compat.ae2.nativeview.ExtendedAeLayout;\n@Pseudo\n@Mixin(targets='+json.dumps(classes).replace('[','{').replace(']','}')+',remap=false)\npublic abstract class PatternModifierLabelsMixin {\n @Inject(method="drawFG",at=@At("HEAD"),cancellable=true,remap=false) private void siu$labels(GuiGraphics g,int ox,int oy,int mx,int my,CallbackInfo ci){if(ExtendedAeLayout.patternLabels(this,g))ci.cancel();}\n}\n')
config={'required':True,'minVersion':'0.8','package':'com.salts_inventory_update.compat.ae2.extended.mixin','plugin':'com.salts_inventory_update.compat.ae2.mixin.AE2MixinPlugin','mixins':['ContainerUpdatesMixin','NetworkMixin'],'client':['PatternModifierLabelsMixin'],'injectors':{'defaultRequire':1}}
pat=['com.github.glodblock.extendedae.client.gui.GuiExPatternTerminal','com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal']
(JAVA/'PatternRowsMixin.java').write_text(head+'import net.minecraft.client.gui.GuiGraphics;\nimport com.salts_inventory_update.compat.ae2.nativeview.ExtendedAeLayout;\n@Pseudo\n@Mixin(targets='+json.dumps(pat).replace('[','{').replace(']','}')+',remap=false)\npublic abstract class PatternRowsMixin {\n @Inject(method="drawFG",at=@At("TAIL"),remap=false) private void siu$rows(GuiGraphics g,int ox,int oy,int mx,int my,CallbackInfo ci){ExtendedAeLayout.afterForeground(this);}\n}\n')
config['client'].append('PatternRowsMixin')
native=['appeng.core.sync.network.NetworkHandler','appeng.core.sync.network.ClientNetworkHandler']
(JAVA/'ExtendedAeNativePacketsMixin.java').write_text(head+'import appeng.core.sync.BasePacket;\nimport appeng.menu.AEBaseMenu;\nimport com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;\n@Pseudo\n@Mixin(targets='+json.dumps(native).replace('[','{').replace(']','}')+',remap=false)\npublic abstract class ExtendedAeNativePacketsMixin {\n @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,require=0,remap=false) private void siu$native(BasePacket packet,CallbackInfo ci){if(MenuPacketScope.current() instanceof AEBaseMenu menu&&ExtendedAePackets.supports(menu)&&menu.desktopSyncId()!=null){menu.sendPacketToServer(packet);ci.cancel();}}\n}\n')
config['client'].append('ExtendedAeNativePacketsMixin')
(BASE/'resources/salts_inventory_compat.extended-ae.mixins.json').write_text(json.dumps(config,indent=2)+'\n')
p=ROOT/'compatibility/versions/1.20.1/fabric/src/main/resources/fabric.mod.json';d=json.loads(p.read_text());cfg='salts_inventory_compat.extended-ae.mixins.json'
if cfg not in d['mixins']:d['mixins'].append(cfg)
p.write_text(json.dumps(d,indent=2)+'\n')
p=ROOT/'compatibility/versions/1.20.1/forge/feeding-trough-manifest.mf';s=p.read_text();lines=s.splitlines();lines=[line+','+cfg if line.startswith('MixinConfigs:')and cfg not in line else line for line in lines];p.write_text('\n'.join(lines)+'\n')
print('Generated optional native hooks for',len(set(targets)),'container overrides')
