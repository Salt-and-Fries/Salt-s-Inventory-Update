"""Retained upload/cannon actions with current disposable runtime identity."""
from pathlib import Path
import hashlib,inspect,json,re,textwrap
from native_five import NativeFive
from suite import Suite
from fixtures import structure

ROOT=Path(__file__).resolve().parents[2]


class CreateProcessing:
    def fixture_runtime(self):
        metadata=self.output/('launch.json'if(self.output/'launch.json').exists()else'native-runtime.json')
        runtime=Path(json.loads(metadata.read_text())['cwd']).resolve()
        roots=[ROOT/'build/remaining-ui-audit/production-runtime',
               Path('C:/Users/texca/.cache/siu-rollout-runtime'),Path('C:/Users/texca/.cache/siu-regression-runtime')]
        assert runtime.parent in [p.resolve()for p in roots]and runtime.is_dir()and(runtime/'mods').is_dir(),runtime
        self.results['schematic_runtime']=dict(file=str(metadata.relative_to(ROOT)),
            sha256=hashlib.sha256(metadata.read_bytes()).hexdigest(),cwd=str(runtime))
        return runtime

    def write_schematics(self,runtime,owner=None):
        runtime=Path(runtime).resolve();assert runtime==self.fixture_runtime()
        owner=owner or self.state['nativePlayerName'];assert re.fullmatch(r'[A-Za-z0-9_]{1,16}',owner),owner
        for name,length in [('stone',1),('line',16)]:
            filename='siu-create-functional-'+name+'.nbt';raw=structure(length)
            for folder in [runtime/'schematics',runtime/'schematics/uploaded'/owner]:
                folder.mkdir(parents=True,exist_ok=True);path=folder/filename;path.write_bytes(raw)
                self.results.setdefault('schematic_fixture_files',[]).append(dict(file=str(path),sha256=hashlib.sha256(raw).hexdigest()))
        self.save()

    def replay_retained(self,original,replacements,name):
        source=textwrap.dedent(inspect.getsource(original))
        for before,after in replacements:
            assert source.count(before)==1,('Retained driver shape changed',before)
            source=source.replace(before,after,1)
        output=self.evidence/('replayed-'+name+'.py');assert not output.exists();output.write_text(source)
        self.results.setdefault('retained_driver_adaptations',[]).append(dict(group=name,
            original_module=original.__module__,generated=str(output.relative_to(ROOT)),sha256=hashlib.sha256(output.read_bytes()).hexdigest(),
            scope='Only disposable runtime path/player identity or fixture writer adapted. All native actions and assertions retained.'))
        self.save()
        namespace=dict(Path=Path,json=json,re=re,prepare_schematics=self.write_schematics)
        exec(compile(source,str(output),'exec'),namespace)
        return namespace[original.__name__](self)

    def table_frame(self):
        return self.replay_retained(NativeFive.table_frame,[
            ("Path(json.loads((self.output/'launch.json').read_text())['cwd'])",'self.fixture_runtime()')], 'table-frame')

    def processing(self):
        original=textwrap.dedent(inspect.getsource(Suite.processing))
        runtime=next(line for line in original.splitlines()if line.strip().startswith('runtime=('))
        return self.replay_retained(Suite.processing,[
            ('from fixtures import prepare_schematics,deployed_schematic','from fixtures import deployed_schematic\n    prepare_schematics=self.write_schematics'),
            ("forge='-forge-' in self.output.name;owner='Dev' if forge else 'UIAudit'","owner=self.state['nativePlayerName']"),
            (runtime,'    runtime=self.fixture_runtime()')], 'processing')
