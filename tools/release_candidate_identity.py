"""Validate the source/artifact identity behind the current release dashboard."""
from pathlib import Path
import hashlib,importlib.util,json,zipfile


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def checked(root,reference):
    root=Path(root).resolve();path=(root/reference['file']).resolve()
    assert path.is_relative_to(root),'Candidate reference escapes the workspace'
    assert sha(path)==reference['sha256'],('Changed candidate reference',reference['file'])
    return path


def validate(root,pointer,*,verify_worktree=False,verify_archive=False):
    root=Path(root).resolve()
    assert pointer['status']=='recorded-qualification-pending'
    source_path=checked(root,pointer['source_snapshot']);source=json.loads(source_path.read_text())
    assert source['status']=='candidate-recorded-qualification-pending'
    assert source['source_tree_sha256']==pointer['source_tree_sha256']
    tree=hashlib.sha256(json.dumps(source['source_files'],sort_keys=True,separators=(',',':')).encode()).hexdigest()
    assert tree==source['source_tree_sha256'],'Invalid source-file tree digest'
    integrity_path=checked(root,pointer['artifact_integrity']);integrity=json.loads(integrity_path.read_text())
    assert integrity['status']=='passed'and len(integrity['artifacts'])==36
    assert source['artifacts']==integrity['artifacts'],'Source snapshot and artifact ledger disagree'
    assert source['integrity_sha256']==sha(integrity_path)
    assert len({v['file']for v in integrity['artifacts']})==36,'Duplicate candidate artifact'
    for artifact in integrity['artifacts']:checked(root,artifact)
    archive=checked(root,source['review_source_archive'])
    if verify_archive:
        with zipfile.ZipFile(archive)as bundle:
            assert len(bundle.namelist())==len(set(bundle.namelist())),'Duplicate source archive member'
            assert set(bundle.namelist())==set(source['source_files'])|{'CANDIDATE-SOURCE-IDENTITY.json'}
            identity=json.loads(bundle.read('CANDIDATE-SOURCE-IDENTITY.json'))
            assert identity['source_tree_sha256']==tree and identity['source_files']==source['source_files']
            for name,expected in source['source_files'].items():assert hashlib.sha256(bundle.read(name)).hexdigest()==expected,name
    if verify_worktree:
        spec=importlib.util.spec_from_file_location('current_release_source_files',root/'tools/record-release-candidate.py')
        recorder=importlib.util.module_from_spec(spec);spec.loader.exec_module(recorder)
        assert recorder.source_files()==source['source_files'],'Working source differs from the activated candidate; record and activate a fresh snapshot'
    return dict(source_snapshot=pointer['source_snapshot'],source_tree_sha256=tree,
        source_archive=source['review_source_archive'],artifact_integrity=pointer['artifact_integrity'],
        scope='Exact recorded source and 36 artifact identities; gameplay qualification remains separate.')


def current(root,*,verify_worktree=False):
    root=Path(root);path=root/'output/release-readiness/candidate-current.json'
    if not path.exists():return None
    result=validate(root,json.loads(path.read_text()),verify_worktree=verify_worktree)
    return dict(pointer=dict(file=str(path.relative_to(root)),sha256=sha(path)),**result)
