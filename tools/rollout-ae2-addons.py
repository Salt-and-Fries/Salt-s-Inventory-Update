"""Carry addon session behavior forward while retaining each AE2 ABI's artwork.

No native PNG or ordinary AE2 screen document is copied between releases.
"""
from pathlib import Path
import subprocess,json,importlib.util,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout/ae2';OUT.mkdir(exist_ok=True)
spec=importlib.util.spec_from_file_location('ports',ROOT/'tools/port-rollout-compatibility.py');ports=importlib.util.module_from_spec(spec);spec.loader.exec_module(ports)
spec=importlib.util.spec_from_file_location('core',ROOT/'tools/resolve-rollout-core.py');core=importlib.util.module_from_spec(spec);spec.loader.exec_module(core)

def main():
    source=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main'
    for version in ['1.21.1','26.1.2']:
        target=ROOT/f'compatibility/ae2/versions/{version}/neoforge/src/main'
        for name in ['nativeview/AEWindow.java','server/AE2ServerIntegration.java']:
            rel='java/com/salts_inventory_update/compat/ae2/'+name;src=source/rel;dest=target/rel
            folder=OUT/version/rel;folder.mkdir(parents=True,exist_ok=True)
            before=subprocess.check_output(['git','show','HEAD:'+src.relative_to(ROOT).as_posix()]).decode()
            (folder/'base').write_text(ports.convert(before,version));(folder/'reviewed').write_text(ports.convert(src.read_text(),version));(folder/'target').write_text(dest.read_text())
            result=subprocess.run(['git','merge-file','-p','--diff3',str(folder/'target'),str(folder/'base'),str(folder/'reviewed')],stdout=subprocess.PIPE)
            text=result.stdout.decode().replace("\r\n","\n")
            if result.returncode:
                (folder/'proposal').write_text(text)
                # The only expected overlap is the already-forwarded cursor fix.
                for match in core.BLOCK.finditer(text):
                    if 'setCarried' not in match[0] and 'TOOLBOX_CHANNEL' not in match[0]:raise RuntimeError('AE2 merge needs review: '+str(folder))
                text=core.BLOCK.sub(lambda m:('        if(channel.equals(RequesterDesktop.SYNC)) { RequesterDesktop.receive(context.state().content,data);return; }\n'+m[1]) if name.endswith("AEWindow.java") else m[1],text)
            assert "<<<<<<<" not in text
            dest.write_text(text)
        for name in ['AENativeWidgets.java','RequesterDesktop.java']:
            src=source/'java/com/salts_inventory_update/compat/ae2/nativeview'/name
            dest=target/'java/com/salts_inventory_update/compat/ae2/nativeview'/name
            text=ports.convert(src.read_text(),version)
            if name=='RequesterDesktop.java':
                # The modern contour renderer already owns edge completion.
                text=core.replace_method(text,'    public static void bottom(', '')
                text=text.replace('var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));','var b=new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(c.data()),c.player().registryAccess());')
                text=text.replace('var stack=b.readItem();','var stack=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);')
                text=text.replace('private static boolean send(int kind,long owner,int slot,java.util.function.Consumer<FriendlyByteBuf> tail)','private static boolean send(int kind,long owner,int slot,java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> tail)')
                text=text.replace('var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeByte(kind);','var b=new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(),net.minecraft.client.Minecraft.getInstance().level.registryAccess());try{b.writeByte(kind);')
                text=text.replace('b->b.writeItem(stack)','b->ItemStack.OPTIONAL_STREAM_CODEC.encode(b,stack)')
                if version=='1.21.1':text=text.replace('invoke(invoke(t,"getServer"),"serialize")','invoke(invoke(t,"getServer"),"serializeNBT",c.player().registryAccess())')
                else:
                    text=text.replace('var data=(CompoundTag)invoke(invoke(t,"getServer"),"serialize");','var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.player().registryAccess());\n            invoke(invoke(t,"getServer"),"serialize",output);var data=output.buildResult();')
                    text=text.replace('data.getBoolean("reset")','data.getBooleanOr("reset",false)').replace('data.getList("entries",Tag.TAG_COMPOUND)','data.getListOrEmpty("entries")').replace('row.getLong("id")','row.getLongOr("id",0L)').replace('row.getCompound("data")','row.getCompoundOrEmpty("data")')
            dest.write_text(text)
        # Row styles must be captured before AE2 lays out runtime-created widgets.
        for resources in [target/'resources/ae2-native-patches']:
            patch=resources/'appeng/client/gui/WidgetContainer.json';data=json.loads(patch.read_text())
            method=next(m for m in data['methods'] if m['name']=='populateAeDesktop')
            owner='com/salts_inventory_update/compat/ae2/nativeview/AENativeWidgets'
            if not any(i.get('owner')==owner for i in method['instructions']):
                method['instructions'][:0]=[{'type':2,'op':25,'var':i} for i in [0,2,3]]+[{'type':5,'op':184,'owner':owner,'name':'retainRuntimeRows','desc':'(Lappeng/client/gui/WidgetContainer;Lnet/minecraft/client/renderer/Rect2i;Lappeng/client/gui/AEBaseScreen;)V','interface':False}]
                method['maxStack']=max(3,method['maxStack']);patch.write_text(json.dumps(data,separators=(',',':')))
        # Desktop documents include the installed addon/native ordinary documents.
        # Only integration metadata is shared; all native textures remain upstream-owned.
        for relative in ['requester.json','requester_terminal.json']+[p.relative_to(source/'resources/assets/ae2/screens/desktop').as_posix() for p in (source/'resources/assets/ae2/screens/desktop/megacells').rglob('*.json')]:
            src=source/'resources/assets/ae2/screens/desktop'/relative
            dest=target/'resources/assets/ae2/screens/desktop'/relative;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(src.read_bytes())
        if version=='1.21.1':
            rel='terminals/portable_mana_cell.json';src=source/'resources/assets/ae2/screens/desktop'/rel;dest=target/'resources/assets/ae2/screens/desktop'/rel;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(src.read_bytes())
        print(version,'native artwork retained; addon session/state helpers carried forward')

if __name__=='__main__':main()
