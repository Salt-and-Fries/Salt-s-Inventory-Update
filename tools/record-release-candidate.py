"""Record an uncommitted source snapshot and exact candidate files without publishing."""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import os
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]
SOURCE_ROOTS=['versions','compatibility','controlify','core-integrations','artwork','api-examples','gradle',
              'tools','functional-tests','docs','changelogs']
GENERATED_DIRECTORIES={'build','.gradle','run','runs','logs','results','__pycache__','node_modules','.git'}
ROOT_INPUTS=['build.gradle.kts','settings.gradle.kts','gradle.properties','LICENSE','gradlew','gradlew.bat',
             'README.md','CHANGELOG.md','RELEASE_READINESS.md','AGENTS.md']

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def source_files():
    result={}
    for name in SOURCE_ROOTS:
        for directory,children,files in os.walk(ROOT/name):
            children[:]=[p for p in children if p not in GENERATED_DIRECTORIES]
            for filename in files:
                path=Path(directory)/filename
                # Audio, editable artwork, binary fixtures and wrapper files
                # matter too. An extension allow-list silently lost those inputs.
                result[path.relative_to(ROOT).as_posix()]=digest(path)
    for name in ROOT_INPUTS:
        result[name]=digest(ROOT/name)
    return dict(sorted(result.items()))

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--id',required=True);parser.add_argument('--source-archive',action='store_true',help='Save exact source/helper/QA inputs for local review; does not add it to public dist');args=parser.parse_args()
    assert all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.id)
    output=ROOT/'output/release-readiness'/('candidate-'+args.id+'.json');assert not output.exists(),'Preserve previous source snapshots'
    integrity=json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text());assert integrity['status']=='passed'
    for artifact in integrity['artifacts']:assert digest(ROOT/artifact['file'])==artifact['sha256']
    files=source_files();tree=hashlib.sha256(json.dumps(files,sort_keys=True,separators=(',',':')).encode()).hexdigest()
    git=lambda *args:subprocess.run(['git',*args],cwd=ROOT,capture_output=True,check=True).stdout
    diff=git('diff','--binary','HEAD');diff_file=output.with_suffix('.patch');diff_file.write_bytes(diff)
    result=dict(id=args.id,status='candidate-recorded-qualification-pending',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        git_head=git('rev-parse','HEAD').decode().strip(),git_status=git('status','--porcelain=v1').decode().splitlines(),
        committed=False,published=False,source_tree_sha256=tree,source_files=files,
        source_scope=dict(roots=SOURCE_ROOTS,root_files=ROOT_INPUTS,excluded_generated_directories=sorted(GENERATED_DIRECTORIES),
            includes_untracked_sources=True,includes_build_helpers=True,includes_binary_assets_and_fixtures=True),
        tracked_diff=dict(file=str(diff_file.relative_to(ROOT)),sha256=digest(diff_file)),
        artifacts=integrity['artifacts'],integrity_sha256=digest(ROOT/'output/release-readiness/candidate-integrity.json'),
        note='The per-file tree includes untracked intended source files. Git HEAD alone does not identify this dirty-worktree candidate. QA evidence is accepted separately against these artifact bytes.')
    if args.source_archive:
        archive=output.with_suffix('.review-source.zip');assert not archive.exists()
        with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED) as bundle:
            for name,expected in files.items():
                raw=(ROOT/name).read_bytes();assert hashlib.sha256(raw).hexdigest()==expected,'Source changed while recording '+name
                bundle.writestr(name,raw)
            bundle.writestr('CANDIDATE-SOURCE-IDENTITY.json',json.dumps(dict(source_tree_sha256=tree,source_files=files),indent=2)+'\n')
        assert source_files()==files,'Source inputs changed during archive creation'
        result['review_source_archive']=dict(file=str(archive.relative_to(ROOT)),sha256=digest(archive),
            purpose='Immutable local review/reproduction inputs, including QA fixtures. Not staged to public dist or published by this task.')
    output.write_text(json.dumps(result,indent=2)+'\n');print(tree,output)

if __name__=='__main__':main()
