"""Prepare excluded observation sources for modern runtime screenshot checks."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'functional-tests/all-target-rollout'
source=(BASE/'java-1.21.1/com/salts_inventory_update/functionaltest/EverythingAuditQa.java').read_text()
for version in ['1.21.11','26.1.2','26.2','26.3']:
    text=source
    for assignment in ['mc.player.input.shiftKeyDown=down;','mc.player.input.shiftKeyDown=shift;','mc.player.input.shiftKeyDown=false;']:
        text=text.replace(assignment,'')
    text=text.replace('call(h("screen"),"charTyped",letter,0)','call(h("screen"),"charTyped",new net.minecraft.client.input.CharacterEvent(letter,0))')
    text=text.replace('call(h("screen"),"keyPressed",num(c,"key",256),0,num(c,"modifiers",0))','call(h("screen"),"keyPressed",new net.minecraft.client.input.KeyEvent(num(c,"key",256),0,num(c,"modifiers",0)))')
    text='\n'.join('            case "hover"->{var window=mc.getWindow();double scale=window.getGuiScale();h("spatialMove",c.get("x").getAsDouble()*scale,c.get("y").getAsDouble()*scale,0D,0D);}' if 'case "hover"->' in line else line for line in text.splitlines())+'\n'
    event='new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button(c),0))'
    text=text.replace('call(s,"mouseClicked",x,y,button(c))','call(s,"mouseClicked",'+event+',false)')
    text=text.replace('call(s,"mouseReleased",x,y,button(c))','call(s,"mouseReleased",'+event+')')
    if version.startswith('26.'):
        text=text.replace('var result=mc.gameMode.interactAt(mc.player,entity,hit,net.minecraft.world.InteractionHand.MAIN_HAND);\n                if(!result.consumesAction())mc.gameMode.interact(mc.player,entity,net.minecraft.world.InteractionHand.MAIN_HAND);','mc.gameMode.interact(mc.player,entity,hit,net.minecraft.world.InteractionHand.MAIN_HAND);')
        text=text.replace('mc.gameMode.interact(mc.player,entity,net.minecraft.world.InteractionHand.MAIN_HAND)','mc.gameMode.interact(mc.player,entity,new net.minecraft.world.phys.EntityHitResult(entity),net.minecraft.world.InteractionHand.MAIN_HAND)')
        text=text.replace('new net.minecraft.client.input.CharacterEvent(letter,0)','new net.minecraft.client.input.CharacterEvent(letter)')
    if version=='26.3':
        text=text.replace('new net.minecraft.client.input.KeyEvent(num(c,"key",256),','new net.minecraft.client.input.KeyEvent(auditKey(num(c,"key",256)),')
        text=text.replace('    static Object identifier(String value){','''    // The command protocol uses GLFW codes; 26.3 native input uses SDL keys.
    static int auditKey(int key){return switch(key){
        case 256->27;case 257->13;case 258->9;case 259->8;case 261->127;
        case 262->1073741903;case 263->1073741904;case 264->1073741905;
        case 265->1073741906;case 266->1073741899;case 267->1073741902;
        case 268->1073741898;case 269->1073741901;default->key>=65&&key<=90?key+32:key;};}
    static Object identifier(String value){''')
    folder=BASE/('java-'+version)/'com/salts_inventory_update/functionaltest';folder.mkdir(parents=True,exist_ok=True)
    (folder/'EverythingAuditQa.java').write_text(text)
    (folder/'AuditFabricInitializer.java').write_text((BASE/'java-1.21.1/com/salts_inventory_update/functionaltest/AuditFabricInitializer.java').read_text())
    print('Prepared excluded observer',version)
