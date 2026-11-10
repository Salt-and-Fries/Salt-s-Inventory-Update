"""Repair MEGA Cells artwork bounds, workbench style and native addon titles."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/window-detail-fixes-04'
WORK.mkdir(exist_ok=True)
changes=[]

def save(path,text):
    before=path.read_text() if path.exists() else None
    if before==text:return
    backup=WORK/'before'/path.relative_to(ROOT)
    if before is not None and not backup.exists():
        backup.parent.mkdir(parents=True,exist_ok=True)
        backup.write_bytes(path.read_bytes())
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(text)
    changes.append(str(path.relative_to(ROOT)))

# The 4.x documents extend these same PNGs by two transparent rows. Correct
# the desktop-only source rectangles; the 2.x/vintage documents are exact.
resources=ROOT/'compatibility/ae2/versions/1.21.1/neoforge/src/main/resources/assets/ae2/screens/desktop/megacells'
for name,height in [('mega_interface',238),('mega_pattern_provider',213)]:
    path=resources/(name+'.json');data=json.loads(path.read_text())
    data['background']={'texture':f'guis/megacells/{name}.png','srcRect':[0,0,176,height]}
    save(path,json.dumps(data,indent=2)+'\n')

save(resources/'portable_cell_workbench.json',json.dumps({
    'includes':['../../megacells/portable_cell_workbench.json'],
    'desktop':{'width':176,'height':162,'resize':'fixed','showTitle':True,
               'preserveAeLayout':True,'inventoryCut':91}
},indent=2)+'\n')

helper='''    /** Unnamed addon menus use their own localized screen-document title. */
    private static Component nativeContentTitle(Component title) {
        if (!(title.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text)
                || !text.getKey().startsWith("gui.ae2.DesktopWindow.")
                || !Component.translatable(text.getKey()).getString().equals(text.getKey()))
            return title;
        // MenuTypeBuilder may append the real custom name after its synthetic
        // prefix. Preserve that name, while omitting only the unknown prefix.
        var siblings=title.getSiblings();
        var nativeTitle=Component.empty().withStyle(title.getStyle());
        int first=!siblings.isEmpty() && siblings.get(0).getString().equals(" - ") ? 1 : 0;
        for(int i=first;i<siblings.size();i++)nativeTitle.append(siblings.get(i).copy());
        return nativeTitle;
    }

'''
for branch in ['1.20.1/common','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']:
    path=ROOT/f'compatibility/ae2/versions/{branch}/src/main/java/com/salts_inventory_update/compat/ae2/nativeview/AEWindow.java'
    text=path.read_text().replace("net.minecraft.client.resources.language.I18n.exists(text.getKey())", "!Component.translatable(text.getKey()).getString().equals(text.getKey())")
    old='InitScreens.createDesktopContent(menu, menu.getPlayerInventory(), title)'
    new='InitScreens.createDesktopContent(menu, menu.getPlayerInventory(), nativeContentTitle(title))'
    assert old in text or new in text,path
    text=text.replace(old,new)
    if 'private static Component nativeContentTitle(' not in text:
        marker='    public static final class State {'
        assert marker in text,path
        text=text.replace(marker,helper+marker,1)
    save(path,text)

manifest=WORK/'source-changes.json'
previous=json.loads(manifest.read_text()) if manifest.exists() else []
manifest.write_text(json.dumps(sorted(set(previous+changes)),indent=2))
print('Updated',len(changes),'Compatibility files; publisher PNGs and Core/API unchanged.')
