"""Use the actual installed briefcase screen's background ID for title erasure."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
for version in ['1.20.1','1.21.1','26.1.2']:
 path=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat/bumblezone/BumblezoneWindows.java'
 s=path.read_text()
 if 'private static '+('ResourceLocation' if version!='26.1.2' else 'Identifier')+' caseBackground;' in s:continue
 kind=re.search(r'static final (\w+) CASE=',s)[1]
 method='''
    // The publisher relocated this asset in newer releases. Read the native
    // screen's own identifier so the title patch also follows resource packs.
    private static TYPE caseBackground;
    private static TYPE caseTexture() {
        if(caseBackground!=null)return caseBackground;
        try {
            var screen=Class.forName("com.telepathicgrunt.the_bumblezone.client.screens.BuzzingBriefcaseScreen");
            var background=screen.getDeclaredField("BACKGROUND");background.setAccessible(true);
            return caseBackground=(TYPE)background.get(null);
        }catch(ReflectiveOperationException|LinkageError unavailable){return caseBackground=CASE;}
    }
'''.replace('TYPE',kind)
 needle='    private BumblezoneWindows() { }';assert needle in s,path
 s=s.replace(needle,needle+method).replace('c.texture(CASE,','c.texture(caseTexture(),')
 path.write_text(s);print(version,'uses native background identifier')
