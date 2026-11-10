"""Pin the original publisher cohorts needed to fill availability-table gaps."""
from pathlib import Path
import hashlib,json
from prepare_recent_fixtures import platform,write_fixture
from run_mod_matrix import FIXTURES
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def main():
    plan=json.loads((OUT/'coverage-fill-plan.json').read_text())
    available=json.loads((ROOT/'output/all-target-rollout/availability-and-registration.json').read_text())
    rows=[];verified={}
    for choice in plan['selected']:
        target=choice['target'];source=choice['fixture']
        if source in FIXTURES[target]:continue
        mc,loader=target.split('/');path=ROOT/'build/all-target-rollout/fixtures'/source/'fixture.json'
        data=json.loads(path.read_text());mods=set()
        for row in available:
            if row['minecraft']==mc and row['loader']==loader and row['project'] in choice['covers']:
                mods.update(row['mod_ids'].replace(',',' ').replace(';',' ').split())
        assert mods
        for artifact in data['artifacts']:
            file=ROOT/artifact['path']
            if str(file) not in verified:verified[str(file)]=hashlib.sha512(file.read_bytes()).hexdigest()
            assert verified[str(file)]==artifact['sha512'],file
        name='regression-fill-'+source;destination=path.parent.parent/name/'fixture.json';destination.parent.mkdir(exist_ok=True)
        data.update(minecraft=mc,loader=loader,scenario='extension-empty',runtime={**platform(mc,loader),**data.get('runtime',{})},
                    regression_source_fixture=str(path.relative_to(ROOT)))
        write_fixture(destination,data)
        rows.append({'target':target,'fixture':name,'source_fixture':source,'mods':sorted(mods),'projects':choice['covers']})
    (OUT/'coverage-fill-fixtures.json').write_text(json.dumps(rows,indent=2)+'\n')
    print('Prepared',len(rows),'additional cohorts for',sum(len(r['projects']) for r in rows),'missing mod/target pairs.')

if __name__=='__main__':main()
