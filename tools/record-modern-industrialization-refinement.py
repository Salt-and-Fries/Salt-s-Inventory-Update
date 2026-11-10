"""Publish crop/tray observations with the exact tested production archives."""
from pathlib import Path
from importlib.machinery import SourceFileLoader
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/modern-industrialization-implementation'
WORK=ROOT/'build/mi-window-refinement'
geometry=SourceFileLoader('mi_geometry',str(ROOT/'tools/check-modern-industrialization-windows.py')).load_module()
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()

def main():
    report=json.loads((WORK/'before-report/verification.json').read_text())
    design=json.loads((ROOT/'output/modern-industrialization-design/mockup-manifest.json').read_text())
    approved=[c['id'] for c in design['cases']];review=[]
    for target in report['targets']:
        mc=target['minecraft'];run='1.20.1-fabric-siu-mi-refine03' if mc=='1.20.1' else '1.21.1-neoforge-integrated-mi-refine03'
        folder=ROOT/'build/modern-industrialization-implementation'/run;runtime=ROOT/'build/remaining-ui-audit/gameplay'/run
        records=json.loads((folder/'geometry-results.json').read_text());assert len(records)==108 and all(r['status']=='pass' for r in records)
        actions=json.loads((folder/'refinement-actions.json').read_text());assert all(r['status']=='pass' for r in actions)
        for slug in ['item-pipe','fluid-pipe']:
            observation=folder/(slug+'-reopened-client.json');s=json.loads(observation.read_text());w,cells=geometry.check(s)
            records.append(dict(id='own-mi-connected-'+slug,status='pass',cells=cells,bounds=w['bounds'],menu=w['menuId'],observation=str(observation.relative_to(ROOT))))
        by_id={r['id']:r for r in records};assert set(by_id)==set(approved)
        previous={r['id']:r for r in target['cases']}
        for ident,row in by_id.items():
            before=previous[ident]['bounds'];after=row['bounds']
            review.append(dict(minecraft=mc,id=ident,before=dict(width=before['width'],height=before['height']),after=dict(width=after['width'],height=after['height']),
                active_state_space_reserved=any(c['reserved'] for c in row.get('components',[]))))
        target['cases']=[by_id[i] for i in approved];target['complete_cell_checks']=sum(r['cells'] for r in records)
        target['final_profile']=run;target['refinement_actions']=actions
        target['sha256']=sha(ROOT/target['compatibility_jar'])
        inputs=json.loads((runtime/'staged-artifacts.json').read_text())
        artifact=next(a for a in inputs['artifacts'] if a['filename']==Path(target['compatibility_jar']).name)
        assert artifact['sha256']==target['sha256'],(run,'tested/staged JAR mismatch')
        target['final_runtime_matches_staged_jar']=True
        shutil.copy2(runtime/'screenshots/final-macerator-with-inventory.png',OUT/'screenshots'/f"{mc}-{target['loader']}-machine.png")
    before=json.loads((WORK/'core-before.json').read_text());after={p.name:sha(p) for p in (ROOT/'dist/core').glob('*.jar')}
    assert before==after,'Core archives changed'
    report.update(core_unchanged=True,public_api_changed=False,unchanged_core_hashes=after,
        refinement=dict(scope='All 110 approved views per supported target; complete sprites/cells/widgets including region cutouts. Focused native input checks only.',
            fixes=['complete exposed tray inner walls and corners','six-pixel component padding where native working area permits','compact sparse panels with long-title tooltips',
                'complete Large Tank gauge and boiler thermometer','reserved active-state component space','remove duplicate modern MI title/Inventory labels'],
            baseline=str((WORK/'before-report/verification.json').relative_to(ROOT)),dimension_review=review))
    (OUT/'verification.json').write_text(json.dumps(report,indent=2))
    summary=dict(views_per_target=110,targets=2,core_jars_unchanged=len(after),
        reduced_window_area=sum(r['after']['width']*r['after']['height']<r['before']['width']*r['before']['height'] for r in review),
        reserved_active_state_views=sum(r['active_state_space_reserved'] for r in review))
    (WORK/'verification-summary.json').write_text(json.dumps(summary,indent=2));print(json.dumps(summary,indent=2))

if __name__=='__main__':main()
