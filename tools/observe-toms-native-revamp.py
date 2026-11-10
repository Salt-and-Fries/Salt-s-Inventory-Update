"""Add read-only Tom menu observations to excluded gameplay observers."""
from pathlib import Path
R=Path(__file__).resolve().parents[1]
W=R/'build/toms-native-window-revamp'
helper='''    static Map<String,Object> observedTomsSettings(AbstractContainerMenu menu) {
        var out=new LinkedHashMap<String,Object>();String name=menu.getClass().getSimpleName();
        if(!menu.getClass().getName().startsWith("com.tom.storagemod."))return out;
        try {
            Class<?> bridge=Class.forName("com.salts_inventory_update.compat.toms_storage.TomsStorageReflect");
            if(name.equals("LevelEmitterMenu")) {
                Object te=field(menu,"te");out.put("count",coreCall(te,"getCount"));out.put("lessThan",coreCall(te,"isLessThan"));
            }
            if(name.equals("TagItemFilterMenu")) {
                out.put("allowList",coreCall(field(menu,"filter"),"isAllowList"));
                out.put("tags",bridge.getMethod("tagFilterTags",AbstractContainerMenu.class).invoke(null,menu));
            }
            if(name.equals("InventoryLinkMenu")) {
                var channels=(java.util.List<?>)bridge.getMethod("inventoryLinkChannels",net.minecraft.server.level.ServerPlayer.class).invoke(null,player());
                var rows=new ArrayList<Object>();for(Object ch:channels)rows.add(Map.of("id",String.valueOf(coreCall(ch,"id")),"name",coreCall(ch,"name"),"public",coreCall(ch,"publicChannel")));
                out.put("channels",rows);out.put("selected",String.valueOf(bridge.getMethod("inventoryLinkSelected",AbstractContainerMenu.class).invoke(null,menu)));
            }
        } catch(Exception error) {out.put("observerError",error.toString());}
        return out;
    }
'''
for p in [R/'functional-tests/remaining-ui-audit/native/java/com/siu_reference/NativeUiReference.java',R/'build/all-target-rollout/native-observers/java-1.21.1-current/com/siu_reference/NativeUiReference.java']:
    text=p.read_text()
    if 'static Map<String,Object> observedTomsSettings' in text:continue
    backup=W/'observer-before'/p.relative_to(R);backup.parent.mkdir(parents=True,exist_ok=True);backup.write_bytes(p.read_bytes())
    text=text.replace('    static Map<String,Object> state(){',helper+'    static Map<String,Object> state(){',1)
    if 'var settings=new LinkedHashMap<String,Object>();' in text:
        text=text.replace('var settings=new LinkedHashMap<String,Object>();','var settings=new LinkedHashMap<String,Object>();settings.putAll(observedTomsSettings(menu));',1)
    else:
        target='"slots",cells));}'
        assert target in text
        text=text.replace(target,'"slots",cells,"settings",observedTomsSettings(menu)));}',1)
    p.write_text(text)
    print(p.relative_to(R))
