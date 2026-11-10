"""Reconcile named completed native workflows without broadening their scope."""
from pathlib import Path
import importlib.util,json,hashlib
from functools import lru_cache
from focused_release_evidence import checked,reference,verify as verify_seal
from additional_native_release_evidence import publishers
from full_suite_provenance import verify_attempt

PLAN={'file':'output/release-readiness/remaining-native-alias-audit-1534-01/feature-plan.json','sha256':'e9a6424c3a6348add30c5173c35d9bb51b6ecb8096bc451db3cfd59e28f3d667'}
SCHEMA='original-completed-native-feature-aliases-v1'
COUNTS={'create':190,'altar':6,'chipped':175,'film':27,'minecells':29,'prominent':49,'pouch':202,'mowzie':33}
FLAGS={'create':'create_features_checked','altar':'native_read_only_altar_checked','chipped':'chipped_features_checked','minecells':'minecells_features_checked','prominent':'prominent_features_checked','pouch':'pouch_features_checked','mowzie':'mowzie_features_checked','accessories_equipment':'accessories_equipment_checked','accessories_equipment_older':'accessories_equipment_checked'}
GROUPS={'toolbox_frame','table_frame','filter_frame','attribute_frame','blueprint_frame','ports','remaining_configurations','ammo','mounted_gauge','fuze_controls','categories','stock','stock_category_hiding','connected_port','schedules','schedule_timeline','autocomplete','processing','management'}

def plan(root):
    value=json.loads(checked(root,PLAN).read_text());assert value['schema']==SCHEMA
    assert len(value['records'])==88 and len({(v['target'],v['case'])for v in value['records']})==88
    return value

@lru_cache(maxsize=64)
def native_execution(root,run):
    root=Path(root);p=plan(root);spec=next(v for v in p['families']if v['run']==run)
    seal_path=checked(root,spec['seal']);seal=json.loads(seal_path.read_text())
    verify_seal(root,seal,seal['subjects'])
    summary=json.loads(checked(root,seal['summary']).read_text());assert summary['status']=='passed'and summary['target']==spec['target']
    assert seal['assertions']==spec['assertions']
    assert seal['process']['measured']==spec['measured']
    family=spec['family'];measured=spec['measured']
    if family in COUNTS:assert spec['assertions']==COUNTS[family]
    else:assert family.startswith('accessories_equipment')and spec['assertions']in {17,19,21}
    if family in FLAGS:assert measured[FLAGS[family]] is True
    if family=='create':assert set(measured['feature_groups'])==GROUPS and len(measured['feature_groups'])==19
    if family=='chipped':assert set(measured['stations'])=={'mason_table','carpenters_table','loom_table','glassblower','botanist_workbench','alchemy_bench','tinkering_table'}
    # Every publisher is independently indexed from the original measured jar.
    classes=sorted({c for row in p['records']if run in row['eligible']for c in row['original_native_classes']})
    native=publishers(root,seal_path,spec['target'],classes)if classes else []
    return dict(run=run,target=spec['target'],family=family,subjects=seal['subjects'],seal=spec['seal'],
        summary=seal['summary'],acceptance=seal['acceptance'],checks=seal['assertions'],publishers=native,
        scope=summary['scope'],limits=seal['limits'])

@lru_cache(maxsize=12)
def curios_execution(root,target):
    root=Path(root);p=plan(root);spec=next(v for v in p['curios']if v['target']==target)
    phase=json.loads(checked(root,p['curios_phase']).read_text());case=next(v for v in phase['cases']if v['key']==spec['key'])
    assert case['status']=='PASS'and case['name']=='curios'
    attempt=case['attempts'][-1];assert attempt['run_id']==spec['run']and attempt['exit_code']==0
    # This independently checks original QA bundles, actual runtime copies and
    # the declared Forge remapping lineage, including original upstream bytes.
    verify_attempt(root,attempt,case)
    report=json.loads(checked(root,spec['report']).read_text())
    assert report['nativeAccessoryFixture']=='curios'and report['failures']==0
    passes={v['name']for v in report['results']if v['status']=='PASS'}
    assert len([v for v in report['results']if v['status']=='PASS'])==67==spec['assertions']
    assert not any(v['status']=='FAIL'for v in report['results'])
    required={'curios-native-equipment-place','curios-native-equipment-pickup','curios-cosmetic-place','curios-cosmetic-take','curios-cosmetic-return','curios-late-click-rejected-safely','curios-owner-closes-panel','curios-final-conservation'}
    assert required<=passes
    native=attempt['runtime_evidence']['dependency_evidence']['upstream']
    found=[]
    for row in native:
        if not row['filename'].startswith('curios-'):continue
        path=checked(root,row);raw=path.read_bytes();assert hashlib.sha512(raw).hexdigest()==row['sha512'];found.append(row)
    assert len(found)==1
    return dict(run=spec['run'],target=target,family='curios',checks=67,phase=p['curios_phase'],case_key=case['key'],
        report=spec['report'],original_artifacts=case['artifacts'],publishers=found,
        scope='Original Curios native-capability equipment and cosmetic edits, visible toggles, joined movement/edges, shrink/stale-input refusal, owner closure and exact recovery.',
        limits='Six original full-phase target executions; not new runs, every addon effect or Accessories Compat Layer acceptance. Forge runtime byte translation retains its explicit lineage.')

def derive(root,target,case,consumer,current):
    root=Path(root);p=plan(root);spec=next(v for v in p['records']if(v['target'],v['case'])==(target,case))
    if spec['family']=='curios':
        execution=curios_execution(str(root),target)
        # Match the complete original artifact scope through the strict existing
        # consumer, never a fabricated per-jar mixture.
        original=execution['original_artifacts'];now={n:next(v[1]for f,v in current.items()if Path(n).name==v[0])for n in original}
        consumer.fingerprints(original,now)
        subjects={f:[Path(n).name,d]for f in current for n,d in original.items()if Path(n).name==current[f][0]}
        assert set(subjects)==set(current)
        executions=[execution];acceptance=execution['report']
    else:
        executions=[native_execution(str(root),run)for run in spec['eligible']];assert executions
        for execution in executions:
            assert execution['target']==target and consumer.bind(target,execution['subjects'],current)
        subjects=executions[0]['subjects'];acceptance=executions[0]['acceptance']
    publishers_by_hash={v['sha512']:v for e in executions for v in e['publishers']}
    assert publishers_by_hash
    return dict(schema=SCHEMA,target=target,case=case,family=spec['family'],
        disposition='read-only-status-pass'if spec['family']=='altar'else'functional-pass',
        original_native_classes=spec['original_native_classes'],native_classes=spec['original_native_classes'],
        subjects=subjects,executions=executions,acceptance=acceptance,publishers=list(publishers_by_hash.values()),
        scope='; '.join(dict.fromkeys(e['scope']for e in executions)),
        limits='Historical aliases retain the named original workflow and publisher scope. Shared aliases create no additional gameplay execution. Approved replacement equipment surfaces edit the native capability; they do not claim to instantiate the original native Screen. '+ '; '.join(dict.fromkeys(e['limits']for e in executions)))

def verify_record(root,record,consumer,current):
    assert record['schema']==SCHEMA
    assert derive(root,record['target'],record['case'],consumer,current)==record
    return True
