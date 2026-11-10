"""Recover later integration pins omitted by the older rollout inventory.

Reads exact local publisher binaries and cached publisher release metadata.
No network request, new upstream availability claim or gameplay pass is made.
"""
from pathlib import Path
import collections, datetime, hashlib, json, sys, tomllib, zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES

FOUR = {'1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge'}
SPECS = [
    ('Ad Astra', {'ad_astra'}, 'ad-astra', FOUR, 'ad-astra-all-targets.md'),
    ('AE2 Wireless Terminals', {'ae2wtlib'}, 'applied-energistics-2-wireless-terminals',
     {'1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge'}, 'ae2/wireless-terminals-modern.md'),
    ('Enchanted Witchcraft', {'enchanted'}, 'enchanted-witchcraft', FOUR-{'1.20.1/forge'}, 'enchanted-all-targets.md'),
    ('Extended AE', {'extendedae','expatternprovider'}, 'extended-ae',
     {'1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge'}, 'ae2/extended-ae-all-targets.md'),
    ('Kibe', {'kibe'}, 'kibe', {'1.20.1/fabric','1.21.1/fabric','1.21.1/neoforge'}, 'kibe-all-targets.md'),
    ('Modern Industrialization', {'modern_industrialization'}, 'modern-industrialization',
     {'1.20.1/fabric','1.21.1/neoforge'}, 'modern-industrialization.md'),
    ('Powah!', {'powah'}, 'powah', {'1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge'}, 'powah-all-targets.md'),
    ('Quark', {'quark'}, 'quark', {'1.20.1/forge','1.21.1/neoforge'}, 'quark-all-targets.md'),
    ('Simple Storage Network', {'storagenetwork'}, 'simple-storage-network',
     {'1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge'}, 'simple-storage-network.md'),
    ('Tech Reborn', {'techreborn'}, 'techreborn',
     {'1.20.1/fabric','1.21.1/fabric','26.1.2/fabric','26.2/fabric','26.3/fabric'}, 'tech-reborn-all-targets.md'),
    ('The Aether', {'aether'}, 'aether', FOUR, 'aether-all-targets.md'),
    ('Zenith Renewed', {'zenith'}, 'zenith-renewed', {'1.20.1/fabric'}, 'zenith-1.20.1.md'),
]


def digest(path, algorithm='sha256'): return hashlib.new(algorithm, path.read_bytes()).hexdigest()


def versions(value):
    if isinstance(value, dict):
        if isinstance(value.get('version_number'), str) and isinstance(value.get('files'), list) and value.get('id'):
            yield value
        for item in value.values(): yield from versions(item)
    elif isinstance(value, list):
        for item in value: yield from versions(item)


def cached_releases():
    paths = set((ROOT/'build/all-target-rollout/metadata').glob('*.json'))
    paths.update((ROOT/'build/ui-modpack-audit-20261025/metadata').glob('*.json'))
    paths.update((ROOT/'output').glob('*/*version*.json'))
    paths.update((ROOT/'output').glob('*/*availability*.json'))
    paths.update((ROOT/'build').glob('*-all-targets/*version*.json'))
    paths.update((ROOT/'build').glob('*-all-targets/*release*.json'))
    paths.update((ROOT/'build').glob('*-all-targets/metadata/*.json'))
    index = collections.defaultdict(list)
    for path in sorted(paths):
        try: data = json.loads(path.read_text(encoding='utf-8-sig'))
        except (UnicodeError, json.JSONDecodeError): continue
        for release in versions(data):
            for file in release['files']:
                sha = file.get('hashes', {}).get('sha512')
                if sha: index[sha].append((release, path))
    return index


def metadata(path):
    with zipfile.ZipFile(path) as jar:
        if 'fabric.mod.json' in jar.namelist():
            row = json.loads(jar.read('fabric.mod.json'), strict=False)
            return [{'id':row['id'], 'version':str(row['version']), 'name':row.get('name',row['id'])}]
        name = next((v for v in ['META-INF/neoforge.mods.toml','META-INF/mods.toml'] if v in jar.namelist()), None)
        if not name: return []
        manifest = {}
        if 'META-INF/MANIFEST.MF' in jar.namelist():
            lines = jar.read('META-INF/MANIFEST.MF').decode(errors='replace').replace('\r\n','\n').splitlines()
            unfolded = []
            for line in lines:
                if line.startswith(' ') and unfolded: unfolded[-1] += line[1:]
                else: unfolded.append(line)
            manifest = dict(line.split(': ',1) for line in unfolded if ': ' in line)
        result = []
        for mod in tomllib.loads(jar.read(name).decode('utf-8-sig')).get('mods',[]):
            version = str(mod.get('version',''))
            if version == '${file.jarVersion}': version = manifest.get('Implementation-Version','')
            result.append(dict(id=mod['modId'],version=version,name=mod.get('displayName',mod['modId'])))
        return result


def main():
    output = ROOT/'output/release-readiness/public-support-supplement.json'
    releases = cached_releases(); cache = {}; pins = {}; absent = []
    inputs = {str(path.relative_to(ROOT)):digest(path)for path in
              [Path(__file__),ROOT/'functional-tests/project-everything-regression/run_mod_matrix.py']}
    manifests = [(target, ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json')
                 for target, fixtures in FIXTURES.items() for fixture in fixtures]
    # These current focused boundary fixtures deliberately isolate optional
    # equipment combinations absent from the broader publisher cohorts.
    for target, run in [('1.20.1/fabric','boundary-build13-1201-fabric'),('1.20.1/forge','boundary-build13-1201-forge')]:
        summary = ROOT/'output/release-readiness'/run/'summary.json'
        data = json.loads(summary.read_text()); assert data['status']=='passed' and data['target']==target
        command = data['prepare_command']; manifest = Path(command[command.index('--fixture')+1])
        manifests.append((target,manifest));inputs[str(summary.relative_to(ROOT))]=digest(summary)
    extra_plan = ROOT/'build/release-readiness/publisher-extra-plan-01.json'
    inputs[str(extra_plan.relative_to(ROOT))]=digest(extra_plan)
    for job in json.loads(extra_plan.read_text())['jobs']:manifests.append((job['target'],ROOT/job['fixture']))
    for target, manifest in manifests:
        mc, loader = target.split('/')
        fixture = str(manifest.relative_to(ROOT))
        inputs[fixture] = digest(manifest)
        for artifact in json.loads(manifest.read_text())['artifacts']:
                path = Path(artifact['path']); path = path if path.is_absolute() else ROOT/path
                if path not in cache: cache[path] = metadata(path)
                for title, ids, slug, allowed, guide in SPECS:
                    matched = [v for v in cache[path] if v['id'] in ids]
                    if not matched: continue
                    assert target in allowed, ('Unreviewed target in later-integration fixture',title,target)
                    assert len(matched)==1
                    sha = digest(path,'sha512'); assert sha==artifact['sha512'], path
                    key = title,target,sha
                    if key in pins:
                        pins[key]['fixtures'].append(fixture); continue
                    found = releases.get(sha,[])
                    release = found[0][0] if found else None
                    if found:
                        assert len({v['version_number'] for v,_ in found})==1, 'Conflicting exact-file release metadata'
                        assert mc in release['game_versions'] and loader in release['loaders'], (title,target,release['id'])
                        version = release['version_number']; url = 'https://modrinth.com/mod/'+slug+'/version/'+release['id']
                        metadata_source = found[0][1]
                        inputs[str(metadata_source.relative_to(ROOT))] = digest(metadata_source)
                    else:
                        version = matched[0]['version']; assert version and '${' not in version, ('Unresolved native version',title,path)
                        url = 'https://modrinth.com/mod/'+slug+'/versions'
                    source = ROOT/'compatibility'/guide; assert source.is_file()
                    inputs[str(source.relative_to(ROOT))] = digest(source)
                    inputs[str(manifest.relative_to(ROOT))] = digest(manifest)
                    pins[key] = dict(project=title,minecraft=mc,loader=loader,source_registration=True,
                        source_registration_route='Later dedicated integration; exact publisher fixture pin',
                        upstream_version=version,upstream_sha512=sha,release_url=url,
                        version_source='Cached exact-file publisher release' if release else 'Native archive metadata; link opens publisher release index',
                        publisher_file=str(path),publisher_filename=artifact['filename'],fixtures=[fixture],
                        integration_guide=str(source.relative_to(ROOT)),gameplay_status='Separate current-artifact qualification required')
    for title, ids, slug, allowed, guide in SPECS:
        for target in sorted(allowed):
            if not any(k[0]==title and k[1]==target for k in pins): absent.append(dict(project=title,target=target))
    assert not absent, ('Missing declared integration publisher fixture',absent)
    rows = sorted(pins.values(),key=lambda v:(v['project'].casefold(),v['minecraft'],v['loader'],v['upstream_version']))
    record = dict(status='exact-local-pins-registration-only',generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        rows=rows,source_inputs=inputs,scope='Later integration families missing from the old rollout inventory. Exact binary hashes and native metadata/cached publisher releases; not new availability or gameplay acceptance.')
    output.write_text(json.dumps(record,indent=2)+'\n')
    print('Supplement',len(rows),'exact pins;',len(SPECS),'later families;',sum(v['version_source'].startswith('Native')for v in rows),'native-metadata/index links')


if __name__=='__main__': main()
