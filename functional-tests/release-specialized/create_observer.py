"""Read-only native player identity and explicitly typed schematic fixtures."""


def enhance(text, mc):
    assert mc in ['1.20.1', '1.21.1']
    anchor='out.put("worldLoaded",mc.player!=null&&mc.level!=null);'
    assert text.count(anchor)==1
    text=text.replace(anchor,anchor+'out.put("nativePlayerName",mc.getUser().getName());',1)
    if mc=='1.21.1':
        anchor='static ItemStack stack(JsonObject c){';assert text.count(anchor)==1
        fixture=r'''
        if(c.has("native_stack")){
            if(!str(c,"type","").equals("reviewed-session-seed")||!str(c,"menu","").equals("com.simibubi.create.content.schematics.cannon.SchematicannonMenu")
                ||!str(c,"item","").equals("create:schematic")||num(c,"slot",-1)!=0||num(c,"count",0)!=1)
                throw new IllegalArgumentException("Native schematic fixture outside reviewed cannon slot");
            try{
                var value=ItemStack.parseOptional(player().registryAccess(),net.minecraft.nbt.TagParser.parseTag(c.get("native_stack").getAsString()));
                if(value.isEmpty()||value.getCount()!=1||!BuiltInRegistries.ITEM.getKey(value.getItem()).toString().equals("create:schematic"))
                    throw new IllegalArgumentException("Invalid native schematic fixture codec");
                return value;
            }catch(Exception error){throw new IllegalArgumentException("Invalid typed native schematic fixture",error);}
        }
'''
        text=text.replace(anchor,anchor+fixture,1)
    return text
