"""Verify the global, optional Charm suppression in every shipping Core port."""
from pathlib import Path
import hashlib,json,subprocess,zipfile

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/redundant-sort-suppression'
report=json.loads((ROOT/'build/all-target-rollout/artifact-integrity.json').read_text())
rows=[]
for artifact in report['artifacts']:
    if artifact['family']!='core':continue
    path=Path(artifact['file'])
    assert hashlib.sha256(path.read_bytes()).hexdigest()==artifact['sha256']
    with zipfile.ZipFile(path) as jar:
        config=json.loads(jar.read('salts_inventory_update.mixins.json'))
        assert 'client.compat.CharmInventoryTidyingMixin' in config['client']
        assert 'com/salts_inventory_update/mixin/client/compat/CharmInventoryTidyingMixin.class' in jar.namelist()
        assert not any('siu_reference' in name for name in jar.namelist())
    verbose=subprocess.check_output(['C:/Program Files/Java/jdk-26/bin/javap.exe','-p','-v','-classpath',str(path),
        'com.salts_inventory_update.mixin.client.compat.CharmInventoryTidyingMixin'],text=True)
    for expected in ['method=["handleScreenSetup","screenSetup","addSortingButton"]',
        'value="HEAD"','cancellable=true','require=0','org.spongepowered.asm.mixin.Pseudo','remap=false',
        'svenhjol/charm/feature/inventory_tidying/InventoryTidyingClient',
        'svenhjol/charm/feature/item_tidying/client/Handlers','CallbackInfo.cancel:()V']:
        assert expected in verbose or expected.replace('/','.') in verbose,(path,expected)
    evidence=WORK/(artifact['minecraft']+'-'+artifact['loader']+'-mixin-bytecode.txt')
    evidence.write_text(verbose)
    rows.append({**artifact,'suppression':'pass','bytecode':str(evidence),
        'checks':['registered in Core client mixins','both publisher class names',
        'legacy/modern screen setup and factory canceled at HEAD',
        'optional absent-mod target (@Pseudo, require=0)','excluded observer not packaged']})
    print('PASS',artifact['minecraft'],artifact['loader'],'global Charm guard packaged',flush=True)
assert len(rows)==12
(WORK/'packaging-verification.json').write_text(json.dumps({'artifacts':rows,'passes':len(rows),
    'scope':'Compiled class annotations, cancellation bytecode and registration in all 12 Core production archives; live publisher validation is recorded separately.'},indent=2))
