"""Stage reviewed optional window implementations on the existing target trees.

The output is a port candidate, not a declaration of native ABI compatibility.
Subsequent binary-contract checks and builds own the release table's statuses.
"""
from pathlib import Path
import json,re,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/all-target-rollout'
VERSIONS=['1.21.1','1.21.11','26.1.2','26.2','26.3']
FOLDERS=['accessories','aircraft','alexsmobs','bountiful','bumblezone','cataclysm','create','graves','minecells','nativewindows','prominent','tomstrading','tradingpost','travelersbackpack']

def factories(text,cls):
    pattern='new '+cls+'('
    while pattern in text:
        start=text.index(pattern);a=start+len(pattern);end=a;depth=1;quoted=False;escaped=False;commas=0
        while depth:
            c=text[end]
            if quoted:
                if escaped:escaped=False
                elif c=='\\':escaped=True
                elif c=='"':quoted=False
            else:
                if c=='"':quoted=True
                elif c=='(':depth+=1
                elif c==')':depth-=1
                elif c==',' and depth==1:commas+=1
            end+=1
        text=text[:start]+cls+('.fromNamespaceAndPath(' if commas else '.parse(')+text[a:end]+text[end:]
    return text

def convert(text,version):
    cls='ResourceLocation' if version=='1.21.1' else 'Identifier'
    if version!='1.21.1':text=text.replace('ResourceLocation','Identifier')
    text=factories(text,cls)
    if version!='1.21.1':
        text=re.sub(r'(BuiltInRegistries\.\w+)\.get\(',r'\1.getValue(',text)
        text=text.replace('.isClientSide)', '.isClientSide())').replace('.isClientSide;', '.isClientSide();')
    if version.startswith('26.'):
        text=text.replace('com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor','net.minecraft.client.gui.GuiGraphicsExtractor')
    if version in ['26.2','26.3']:
        text=text.replace('minecraft().screen','minecraft().gui.screen()').replace('mc.screen','mc.gui.screen()')
    return text

def main():
    source=ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat'
    report=[]
    for version in VERSIONS:
        dest=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat'
        for folder in FOLDERS:
            for p in (source/folder).rglob('*.java'):
                if '/accessories/fabric/' in p.as_posix():continue
                target=dest/p.relative_to(source)
                backup=OUT/'compatibility-baseline'/target.relative_to(ROOT)
                if backup.exists():raise RuntimeError('Already staged: '+str(target))
                backup.parent.mkdir(parents=True,exist_ok=True);backup.write_bytes(target.read_bytes() if target.exists() else b'')
                target.parent.mkdir(parents=True,exist_ok=True);target.write_text(convert(p.read_text(),version))
                report.append({'source':str(p.relative_to(ROOT)),'target':str(target.relative_to(ROOT)),'status':'port-candidate'})
        # Append missing bootstrap registrations without dropping existing target integrations.
        target=dest/'CompatibilityBootstrap.java';text=target.read_text();original=(source/'CompatibilityBootstrap.java').read_text()
        for method in ['initializeCommon','initializeClient']:
            pattern=r'(public static void '+method+r'\(\) \{\n)'
            section=original.split('public static void '+method+'() {',1)[1].split('\n    }',1)[0]
            additions=[]
            for line in section.splitlines():
                if any('/'+f+'/' in line.replace('.','/') for f in FOLDERS):
                    call=re.search(r'(initialize\("[^"]+", "[^"]+", "[^"]+"\))',line)
                    if call and call[1] not in text:additions.append('        '+call[1]+';')
                    elif 'ReviewedNativeWindows.'+method+'();' in line and line.strip() not in text:additions.append(line)
            text=re.sub(pattern,lambda m:m[0]+'\n'.join(additions)+'\n',text)
        target.write_text(text)
        resources=ROOT/f'compatibility/versions/{version}/common/src/main/resources'
        for p in (ROOT/'compatibility/versions/1.20.1/common/src/main/resources').rglob('*'):
            if not p.is_file() or (p.name.endswith('.mixins.json') and 'reviewed-native' not in p.name):continue
            target=resources/p.relative_to(ROOT/'compatibility/versions/1.20.1/common/src/main/resources');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
        fabric=ROOT/f'compatibility/versions/{version}/fabric/src/main/resources/fabric.mod.json';data=json.loads(fabric.read_text())
        name='salts_inventory_compat.reviewed-native.mixins.json'
        if name not in data['mixins']:data['mixins'].append(name)
        fabric.write_text(json.dumps(data,indent=2)+'\n')
        neo=ROOT/f'compatibility/versions/{version}/neoforge/src/main/resources/META-INF/neoforge.mods.toml';text=neo.read_text()
        if name not in text:text+='\n[[mixins]]\nconfig="'+name+'"\n'
        neo.write_text(text)
    (OUT/'compatibility-port-candidates.json').write_text(json.dumps(report,indent=2))
    print('Staged',len(report),'Java port candidates; native ABI validation remains required.')

if __name__=='__main__':main()
