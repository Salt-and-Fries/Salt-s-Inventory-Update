"""Read-only named-observer fields used by the unchanged native feature drivers."""

PROBE=r'''
        if(kind.equals("regression-native-entry-world")){
            var client=new LinkedHashMap<String,Object>();
            client.put("position",Map.of("x",mc.player.getX(),"y",mc.player.getY(),"z",mc.player.getZ()));
            client.put("sneaking",mc.player.isShiftKeyDown());
            client.put("held",BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString());
            client.put("yaw",mc.player.getYRot());client.put("pitch",mc.player.getXRot());
            if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit){
                client.put("ray",Map.of("type",hit.getType().toString(),"position",hit.getBlockPos().toShortString(),
                    "face",hit.getDirection().toString(),"location",hit.getLocation().toString()));
            }
            work=server(()->{
                var blocks=new ArrayList<Object>();
                for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=2;dy++)for(int dz=-1;dz<=1;dz++){
                    var pos=origin().offset(dx,dy,dz);var state=level().getBlockState(pos);var be=level().getBlockEntity(pos);
                    blocks.add(Map.of("position",pos.toShortString(),"block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                        "state",state.toString(),"entity",be==null?"none":be.getClass().getName()));
                }
                write("native-entry-world.json",Map.of("client",client,"origin",origin().toShortString(),"blocks",blocks,
                    "server",Map.of("sneaking",player().isShiftKeyDown(),"held",BuiltInRegistries.ITEM.getKey(player().getMainHandItem().getItem()).toString(),
                        "position",player().position().toString(),"menu",player().containerMenu.getClass().getName())));return null;
            });return true;
        }
'''


def enhance(text,mc,named=True):
    assert named and mc in ['1.21.1','1.21.11','26.1.2','26.2','26.3']
    assert 'regression-native-entry-world' not in text, 'Named observation adapter already applied'
    menu='row.put("sessionId",session.sessionId());'
    assert text.count(menu)==1,'Named window observation contract changed'
    text=text.replace(menu,menu+'''try{
                var observedMenuKey=BuiltInRegistries.MENU.getKey(session.menu().getType());
                row.put("menuId",observedMenuKey==null?null:observedMenuKey.toString());
            }catch(UnsupportedOperationException unregisteredMenu){row.put("menuId",null);}''',1)
    layout='row.put("template",call(definition,"layout"));'
    assert text.count(layout)==1,'Named native template observation contract changed'
    text=text.replace(layout,layout+'''row.put("composition",call(definition,"layout"));
                Object observedTemplate=field(definition,"template");row.put("templateClass",observedTemplate==null?"none":observedTemplate.getClass().getName());
                row.put("hostScrollX",field(definition,"scrollX"));row.put("hostScrollY",field(definition,"scrollY"));''',1)
    anchor='if(kind.equals("regression-world"))return true;'
    assert text.count(anchor)==1,'Read-only native diagnostic command contract changed'
    return text.replace(anchor,anchor+PROBE,1)
