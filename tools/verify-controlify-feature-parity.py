"""Verify complete feature/API inventory and identical editable assets across the controller matrix.

This is a packaging/source contract check, not a substitute for native gameplay evidence.
"""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']
NATIVE_CLASSES = [
    'SaltsControlifyClient', 'RadialInventoryScreen', 'ContextualRadialOverlay',
    'ContextualRecipeBinding', 'CreativeSearchBinding', 'CreativeSearchTarget',
    'WorkspaceActionGuide', 'WorkspaceSearchKeyboard', 'WorkspaceCraftFeedback',
    'WorkspaceUiSounds', 'WorkspaceSoundSettingsScreen', 'TuckedWindowTabs',
]
WORKSPACE_METHODS = [
    'renderWindowFrame', 'previewIcon', 'textInputActive', 'textInputBounds',
    'visibleControls', 'controllerTargets', 'scrollController', 'controllerScrollAvailable',
    'creativeHotbarAt', 'creativeCatalogAt', 'creativeHotbarDeleteAt', 'presentationBounds',
    'carriedStack', 'sortWindow', 'canSortWindow', 'contextualMenu', 'craftToWindow',
    'craftStackToWindow', 'prepareCookingRecipe', 'beginRecipeHover', 'updateRecipeHover',
    'endRecipeHover', 'recipeHoverGhosts', 'recipeHoverRejected',
    'setContextualRecipePresentation', 'contextualBookHint', 'selectContextualTab',
]
API_TYPES = ['DesktopContextualMenu', 'DesktopContextualBookHint', 'DesktopRecipeGhost',
             'DesktopControllerTarget', 'DesktopScrollRegion']


def sha(data):
    return hashlib.sha256(data).hexdigest()


def validate(artifacts=None):
    shared = ROOT / 'controlify/common/src/main/java/com/salts_inventory_update/controlify'
    models = sorted(p.stem for p in (shared/'model').glob('*.java'))
    assert 'ContextualWheelSelection' in models and 'KeyboardActivation' in models
    # The test-only model compilation exercises one legacy copy; all native copies must retain that policy.
    tracker_sources = [ROOT/f'controlify/versions/{game}/common/src/main/java/com/salts_inventory_update/controlify/model/CursorWarpEchoTracker.java'
                       for game in ('1.20.1','1.21.1','1.21.11')]
    tracker_policy = tracker_sources[0].read_text(encoding='utf-8-sig')
    assert all(path.read_text(encoding='utf-8-sig') == tracker_policy for path in tracker_sources[1:]), \
        'Legacy cursor-warp tracker policies differ; the test-only representative no longer covers every target'
    reference = ROOT/'controlify/versions/26.3/common/src/main/resources/assets/salts_inventory_controlify'
    expected = {'assets/salts_inventory_controlify/'+p.relative_to(reference).as_posix(): sha(p.read_bytes())
                for p in reference.rglob('*') if p.is_file() and p.suffix.lower() != '.pdn'}
    rows = []
    for game in VERSIONS:
        native = ROOT/f'controlify/versions/{game}/common/src/main/java/com/salts_inventory_update/controlify'
        missing = [name for name in NATIVE_CLASSES if not (native/(name+'.java')).is_file()]
        assert not missing, (game, 'missing native feature implementations', missing)
        for path in native.rglob('*.java'):
            text = path.read_text(encoding='utf-8-sig')
            assert not any(broken in text for broken in ('\u00e2\u20ac\u00a2', '\u00e2\u20ac\u00a6', '\ufffd')), \
                (game, path.name, 'corrupted UTF-8 UI text')
        api = ROOT/f'versions/{game}/fabric/src/main/java/com/salts_inventory_update/api/client/desktop'
        source = (api/'DesktopWorkspace.java').read_text()
        assert all(name+'(' in source for name in WORKSPACE_METHODS), (game, 'workspace feature inventory')
        assert all((api/(name+'.java')).is_file() for name in API_TYPES), (game, 'contextual/editor API types')
        definition = (api/'DesktopWindowDefinition.java').read_text()
        assert all(name+'(' in definition for name in ['textInputBounds','controllerScrollRegions']), game
        for loader in ['fabric', 'forge' if game == '1.20.1' else 'neoforge']:
            row = {'minecraft':game, 'loader':loader, 'native_classes':len(NATIVE_CLASSES),
                   'workspace_features':len(WORKSPACE_METHODS), 'shared_models':len(models)}
            if artifacts:
                label={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[loader]
                matches=sorted((artifacts/'controlify').glob(f'salts_inventory_controlify-{game}-{label}-*.jar'))
                assert len(matches)==1, (game,loader,'require one current companion artifact',matches)
                jar=matches[0]
                with ZipFile(jar) as archive:
                    assert archive.testzip() is None, jar
                    names=set(archive.namelist())
                    assert not any(n.endswith('.pdn') or '/qa/' in n for n in names), jar
                    for name,digest in expected.items():
                        assert name in names and sha(archive.read(name))==digest, (jar,name,'reference asset mismatch')
                    for name in NATIVE_CLASSES:
                        assert f'com/salts_inventory_update/controlify/{name}.class' in names, (jar,name)
                    for name in models:
                        assert f'com/salts_inventory_update/controlify/model/{name}.class' in names, (jar,name)
                row.update(jar=jar.relative_to(ROOT).as_posix(), sha256=sha(jar.read_bytes()),
                           reference_assets=len(expected), pngs=sum(n.endswith('.png') for n in expected))
            rows.append(row)
    return {'status':'passed','scope':'source/API and packaging; native runtime verified separately',
            'targets':rows, 'shared_model_types':models}


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifacts',type=Path)
    parser.add_argument('--summary',type=Path,required=True)
    args=parser.parse_args()
    report=validate(args.artifacts.resolve() if args.artifacts else None)
    args.summary.parent.mkdir(parents=True,exist_ok=True)
    args.summary.write_text(json.dumps(report,indent=2)+'\n')
    print(f"Controller feature inventory verified for {len(report['targets'])} targets; shared models and native adaptations present.")
