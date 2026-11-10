"""Keep the six excluded QA adapters on the same regression command protocol."""
from pathlib import Path
from native_input_geometry import correct_block_hits
ROOT = Path(__file__).resolve().parents[2]
for mc in ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']:
    path = ROOT / f'functional-tests/all-target-rollout/java-{mc}/com/salts_inventory_update/functionaltest/EverythingAuditQa.java'
    text = path.read_text(); fragment = (ROOT / 'functional-tests/project-everything-regression/observer-fragment.java.txt').read_text()
    fragment = fragment.replace('REGRESSION_STACK_SIGNATURE', 'String.valueOf(s.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()))' if mc == '1.20.1' else 'regressionItemSignature(s)')
    fragment = fragment.replace('REGRESSION_ITEM_DATA_FORMAT', 'native-item-stack-save' if mc == '1.20.1' else 'native-item-codec')
    fragment = fragment.replace('REGRESSION_SELECT_EMPTY_HAND', 'mc.player.getInventory().setSelectedSlot(emptySlot);' if mc not in ['1.20.1','1.21.1'] else 'mc.player.getInventory().selected=emptySlot;')
    fragment = fragment.replace('REGRESSION_SET_SERVER_SELECTED', 'player().getInventory().setSelectedSlot(0);' if mc not in ['1.20.1','1.21.1'] else 'player().getInventory().selected=0;')
    fragment=fragment.replace('REGRESSION_SEND_SHIFT_PACKET',
        'mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(mc.player,down?net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));'
        if mc in ['1.20.1','1.21.1'] else
        'mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false,false,false,false,false,down,false)));')
    native_click='call(s,"mouseClicked",x,y,button);call(s,"mouseReleased",x,y,button);' if mc in ['1.20.1','1.21.1'] else 'var event=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));call(s,"mouseClicked",event,false);call(s,"mouseReleased",event);'
    fragment=fragment.replace('REGRESSION_NATIVE_CLICK',native_click)
    fragment=fragment.replace('REGRESSION_NATIVE_KEY','call(s,"keyPressed",key,0,modifiers);' if mc in ['1.20.1','1.21.1'] else 'call(s,"keyPressed",new net.minecraft.client.input.KeyEvent(key,'+('num(c,"keycode",0)' if mc=='26.3' else '0')+',modifiers));')
    fragment=fragment.replace('REGRESSION_DESKTOP_ESCAPE','call(desktop,"keyPressed",256,0,0);' if mc in ['1.20.1','1.21.1'] else 'call(desktop,"keyPressed",new net.minecraft.client.input.KeyEvent('+('41' if mc=='26.3' else '256')+',0,0));')
    fragment=fragment.replace('REGRESSION_USE_ENTITY_AT','mc.gameMode.'+('interact' if mc.startswith('26.') else 'interactAt')+'(mc.player,entity,new net.minecraft.world.phys.EntityHitResult(entity),net.minecraft.world.InteractionHand.MAIN_HAND);')
    fragment=fragment.replace('REGRESSION_USE_ENTITY','mc.gameMode.interact(mc.player,entity,'+('new net.minecraft.world.phys.EntityHitResult(entity),' if mc.startswith('26.') else '')+'net.minecraft.world.InteractionHand.MAIN_HAND);')
    if mc=='26.3':
        text=text.replace('new net.minecraft.client.input.KeyEvent(auditKey(num(c,"key",256)),0,num(c,"modifiers",0))',
                          'new net.minecraft.client.input.KeyEvent(c.has("sdl")&&c.get("sdl").getAsBoolean()?num(c,"key",41):auditKey(num(c,"key",256)),num(c,"keycode",0),num(c,"modifiers",0))')
    parser = 'parseTag' if mc in ['1.20.1', '1.21.1'] else 'parseCompoundFully'
    tag = 'net.minecraft.nbt.TagParser.' + parser + '(c.get("snbt").getAsString())'
    legacy_data='if(c.has("native_decree")&&c.get("native_decree").getAsBoolean())s.getOrCreateTag().merge('+tag+');else s.setTag('+tag+');'
    fragment = fragment.replace('REGRESSION_APPLY_STACK_DATA', legacy_data if mc == '1.20.1' else 's.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(' + tag + '));')
    decree='io.ejekta.bountiful.content.'+('' if mc=='1.20.1' else 'item.')+'DecreeItem'
    fragment=fragment.replace('REGRESSION_INITIALIZE_DECREE','s=(ItemStack)call(Class.forName("'+decree+'").getField("Companion").get(null),'+('"create","farmer"' if mc=='1.20.1' else '"createWithAllDecrees"')+');')
    text = text.replace('history.add(result);write("observations.json",history);write("reply.json",result);', 'regressionObserve(result);write("reply.json",result);')
    text = text.replace('result.put("client",state());', 'result.put("client",str(pending,"type","").startsWith("regression-")&&!str(pending,"type","").equals("regression-move")?Map.of("regression",true):state());')
    text = text.replace('work=server(EverythingAuditQa::authoritative);','work=server(str(pending,"type","").startsWith("regression-")?()->Map.of("regressionBarrier",true):EverythingAuditQa::authoritative);')
    if 'row.put("locked",field(window,"locked"))' not in text:
        text=text.replace('Object window=h("windowSession",session.sessionId());','Object window=h("windowSession",session.sessionId());row.put("locked",field(window,"locked"));')
    # Aether's owned source deliberately has no standalone window. Observe it
    # through the authoritative ledger, never fabricate a rendered window row.
    text=text.replace('Object window=h("windowSession",session.sessionId());row.put',
                      'Object window=h("windowSession",session.sessionId());if(window==null)continue;row.put')
    text=text.replace('Object w=h("windowSession",id);row.put("bounds"',
                      'Object w=h("windowSession",id);if(w==null)continue;row.put("bounds"')
    at = text.index('    // Excluded regression commands:')
    path.write_text(correct_block_hits(text[:at] + fragment + '\n}\n'))
