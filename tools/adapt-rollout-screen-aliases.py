"""Add unambiguous publisher class relocations without dropping older ABI names."""
from pathlib import Path
import json,re,zipfile,io
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout'

def main():
    availability=json.loads((OUT/'availability.json').read_text());report=[]
    for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
        classes=set()
        def index(z):
            for name in z.namelist():
                if name.endswith('.class'):classes.add(name[:-6].replace('/','.'))
                elif name.startswith('META-INF/') and name.endswith('.jar'):
                    with zipfile.ZipFile(io.BytesIO(z.read(name))) as nested:index(nested)
        seen=set()
        for project in availability['projects'].values():
            for target,release in project['targets'].items():
                if not target.startswith(version+'/') or release['sha512'] in seen:continue
                seen.add(release['sha512']);path=OUT/'upstream'/(release['sha512'][:16]+'-'+release['filename'])
                with zipfile.ZipFile(path) as z:index(z)
        simple={}
        for name in classes:simple.setdefault(name.rsplit('.',1)[-1],[]).append(name)
        root=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat'
        for path in root.rglob('*.java'):
            text=path.read_text();changes=[]
            def relocated(m):
                name=m[1]
                if not name.startswith(('com.','io.','net.','dev.','me.','fuzs.','vazkii.','svenhjol.','baguchan.','tallestegg.')):return m[0]
                if name in classes or name.startswith(('net.minecraft.','java.','javax.','org.','com.salts_inventory_update.')):return m[0]
                candidates=simple.get(name.rsplit('.',1)[-1],[])
                if len(candidates)!=1:return m[0]
                noise=set('com net io github dev me client common core api server gui menu menus screen screens inventory inventories util utils entity entities network networking world handler handlers data slot slots block blocks impl implementation platform package'.split())
                if not (set(name.split('.')[:-1])-noise).intersection(set(candidates[0].split('.')[:-1])-noise):return m[0]
                changes.append((name,candidates[0]));return '"'+candidates[0]+'"'
            text=re.sub(r'"([A-Za-z_][\w$]*(?:\.[\w$]+){2,})"',relocated,text)
            if changes:path.write_text(text);report.append({'version':version,'source':str(path.relative_to(ROOT)),'relocations':changes})
        path=ROOT/f'compatibility/versions/{version}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json';profiles=json.loads(path.read_text())
        for p in profiles:
            name=p.get('screen');candidates=simple.get(name.rsplit('.',1)[-1],[]) if name else []
            if name not in classes and len(candidates)==1:
                p['screens']=sorted(set(p.get('screens',[])+candidates));report.append({'version':version,'profile':p['id'],'alias':candidates[0]})
            if p['id']=='bf-warp-plate' and version!='1.20.1':
                candidate='net.blay09.mods.waystones.client.gui.screen.WaystoneContainerScreen'
                if candidate in classes:p['screens']=sorted(set(p.get('screens',[])+[candidate]))
        path.write_text(json.dumps(profiles,indent=2)+'\n')
        p=root/'nativewindows/ReviewedNativeWindows.java';s=p.read_text().replace('String[] mods, menus;','String[] mods, menus, screens;')
        s=s.replace('p.screen.equals(view.screenType())','(p.screen.equals(view.screenType())||p.screens!=null&&Arrays.asList(p.screens).contains(view.screenType()))')
        s=s.replace('        if("net.satisfy.vinery.client.gui.FermentationBarrelGui".equals(p.screen)) {','        if(p.id.equals("bf-warp-plate")&&view.screenType().endsWith(".WaystoneContainerScreen"))return NativeWindowPolish.slots(view);\n        if("net.satisfy.vinery.client.gui.FermentationBarrelGui".equals(p.screen)) {')
        p.write_text(s)
        print(version,'publisher classes indexed:',len(classes))
    (OUT/'screen-aliases.json').write_text(json.dumps(report,indent=2));print('Class/profile relocations:',len(report))

if __name__=='__main__':main()
