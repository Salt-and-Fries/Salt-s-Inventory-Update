"""Generate only the QA probe and held-key seam for one Spatial GUI port."""
import argparse
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def generate(mc,loader,output):
    base=ROOT/'functional-tests/spatial-java'
    names=['SpatialTransitionProbe.java','SpatialTransitionProbeMixin.java','SpatialHeldInputMixin.java']
    if mc=='26.3' and loader=='fabric':names.append('SpatialServerClientQa.java')
    for name in names:
        path=next(base.rglob(name));source=path.read_text(encoding='utf-8')
        if mc not in ('26.2','26.3'):source=source.replace('.gameRenderer.mainCamera()', '.gameRenderer.getMainCamera()')
        if mc in ('1.20.1','1.21.1'):
            source=source.replace('camera.position()', 'camera.getPosition()').replace('camera.xRot()', 'camera.getXRot()').replace('camera.yRot()', 'camera.getYRot()')
        source=source.replace('camera.getFov()', 'com.salts_inventory_update.spatial.SpatialPlatform.fov()')
        if name=='SpatialHeldInputMixin.java' and mc!='26.3':
            window_type='long' if mc in ('1.20.1','1.21.1') else 'com.mojang.blaze3d.platform.Window'
            source=source.replace('salts$heldFixture(int key,', f'salts$heldFixture({window_type} window, int key,')
        file=output/'java'/path.relative_to(base);file.parent.mkdir(parents=True,exist_ok=True);file.write_text(source,encoding='utf-8')
    config=json.loads((ROOT/'functional-tests/spatial-resources/salts_spatial_qa.mixins.json').read_text(encoding='utf-8'))
    config['compatibilityLevel']='JAVA_17' if mc=='1.20.1' else 'JAVA_21' if mc.startswith('1.') else 'JAVA_25'
    if mc in ('1.20.1','1.21.1'):config['refmap']='salts_inventory_update.refmap.json'
    file=output/'resources/salts_spatial_qa.mixins.json';file.parent.mkdir(parents=True,exist_ok=True);file.write_text(json.dumps(config,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--minecraft',required=True);parser.add_argument('--loader',required=True);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();generate(args.minecraft,args.loader,args.output.resolve())
