"""Read-only tracing around unchanged native aim/use actions; no ray repair."""
from native_observation_schema import enhance as identities

TRACE = r'''
    static void releaseRayTrace(String phase,JsonObject c){
        var row=new LinkedHashMap<String,Object>();
        var eye=mc.player.getEyePosition();var look=mc.player.getViewVector(1.0f);
        row.put("phase",phase);row.put("command",c.toString());row.put("position",mc.player.position().toString());
        row.put("eye",eye.toString());row.put("look",look.toString());row.put("pose",mc.player.getPose().toString());
        row.put("sneaking",mc.player.isShiftKeyDown());row.put("shiftKey",mc.options.keyShift.isDown());
        row.put("yaw",mc.player.getYRot());row.put("pitch",mc.player.getXRot());
        var camera=mc.getCameraEntity();row.put("camera",camera==null?"none":camera.getClass().getName()+":"+camera.getId());
        row.put("hit",mc.hitResult==null?"none":mc.hitResult.getType().toString());
        if(mc.hitResult!=null)row.put("hitLocation",mc.hitResult.getLocation().toString());
        if(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)row.put("hitEntity",BuiltInRegistries.ENTITY_TYPE.getKey(hit.getEntity().getType()).toString());
        var entities=new ArrayList<Object>();String wanted=str(c,"entity","");
        for(var e:mc.level.getEntities(mc.player,mc.player.getBoundingBox().inflate(12))){
            if(!BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(wanted))continue;
            var entity=new LinkedHashMap<String,Object>();entity.put("id",e.getId());entity.put("type",wanted);
            entity.put("position",e.position().toString());entity.put("box",e.getBoundingBox().toString());
            entity.put("pickable",e.isPickable());entity.put("alive",e.isAlive());entity.put("removed",e.isRemoved());
            entity.put("passenger",e.isPassenger());entity.put("vehicle",e.isVehicle());
            entity.put("lookIntersectsBoxAtSixBlocks",e.getBoundingBox().clip(eye,eye.add(look.scale(6))).isPresent());
            entities.add(entity);
        }
        row.put("entities",entities);
        write("native-ray-"+c.get("sequence").getAsInt()+"-"+phase+".json",row);
    }
'''


def enhance(text,mc,named=True):
    assert mc=='1.21.1' and named
    text=identities(text,mc,named)
    before='case "aim-entity"->{'
    assert text.count(before)==1
    text=text.replace(before,before+'releaseRayTrace("before-aim",c);',1)
    after='mc.player.setXRot((float)(-Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI));'
    assert text.count(after)==1
    text=text.replace(after,after+'releaseRayTrace("after-aim",c);',1)
    use='case "client-use-entity-ray"->{'
    assert text.count(use)==1
    text=text.replace(use,use+'releaseRayTrace("before-use",c);',1)
    return text[:text.rfind('}')]+TRACE+'\n}\n'
