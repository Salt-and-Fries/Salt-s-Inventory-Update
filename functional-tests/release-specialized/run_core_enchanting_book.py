"""Run the original ordinary Core smoke plus book rendering before/after reload."""
from pathlib import Path
import argparse, datetime, json, msvcrt, subprocess, sys, traceback, zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path[:0] = [str(ROOT / 'tools'), str(ROOT / 'functional-tests/project-everything-regression')]
import full_suite_provenance as provenance
from focused_release_evidence import reference, checked
from release_candidate_identity import current
from subject_artifacts import current_subjects
from core_render_diagnostic import BASE, script
from enchanting_book_12111 import BASE_JAVA, TARGETS, RESOURCE, PHASES, derive_java, init_source, verify_report, verify_log


def descriptor(target, subjects):
    mc, loader = target.split('/')
    return dict(name='focused-core-enchanting-book', minecraft=mc, loader=loader, script=BASE,
        args=['-Minecraft', mc, '-Loader', loader, '-Scenario', 'core'],
        artifacts={'dist/core/' + subjects['core'][0]: subjects['core'][1]})


def observe(root, summary):
    root = Path(root).resolve()
    folder, scenario = provenance.ordinary_report(root, summary['base_case'], summary)
    path = folder / 'results.json'
    report = json.loads(path.read_text())
    assert scenario == 'core' and summary['target'] == report['minecraft'] + '/' + report['loader']
    verify_report(report)
    log = folder / 'launch.log'
    text = log.read_text(errors='replace')
    verify_log(text)
    assert 'SIU_GAMEPLAY_SUMMARY status=PASS' in text
    frames = []
    for phase in PHASES:
        frame = Path(next(v['path'] for v in report['results'] if v['name'] == phase + '-framebuffer'))
        frames.append(reference(root, frame))
    loaded = checked(root, summary['runtime_evidence']['loaded_code_sources']['coreCodeSource'])
    with zipfile.ZipFile(loaded) as jar:
        raw = jar.read('com/salts_inventory_update/client/InventoryDesktopScreen.class')
        assert raw[:4] == b'\xca\xfe\xba\xbe'
        assert RESOURCE.split(':', 1)[1].encode() in raw
        assert b'textures/entity/enchantment/enchanting_table_book.png' not in raw
    return dict(target=summary['target'], original_report=reference(root, path), actual_log=reference(root, log),
        actual_loaded_core=reference(root, loaded), screenshots=frames,
        total_report_rows=len(report['results']), pass_records=sum(v['status'] == 'PASS' for v in report['results']),
        scope='Original ordinary Core smoke plus real native resource bytes and textured-model framebuffers before and after the original reload. Focused two-loader acceptance only; not a complete gameplay suite.')


def verify_run(root, summary, expected):
    root = Path(root).resolve()
    assert root == ROOT and summary['target'] in TARGETS
    assert summary['status'] == 'passed' and type(summary['exit_code']) is int and summary['exit_code'] == 0
    assert summary['subjects'] == {k: list(v) for k, v in expected.items()} and set(expected) == {'core'}
    assert summary['base_case'] == descriptor(summary['target'], expected)
    provenance.verify_attempt(root, summary, summary['base_case'])
    with zipfile.ZipFile(checked(root, summary['qa_source_archive'])) as archive:
        normalized = lambda value: value.decode().replace('\r\n', '\n')
        original = normalized(archive.read(BASE_JAVA))
        java = checked(root, summary['derived_java'])
        wrapper = checked(root, summary['executed_wrapper'])
        init = checked(root, summary['derived_init'])
        assert normalized(archive.read(java.relative_to(root).as_posix())) == derive_java(original)
        assert normalized(archive.read(init.relative_to(root).as_posix())) == init_source(root, wrapper.parent)
        assert normalized(archive.read(wrapper.relative_to(root).as_posix())) == script(normalized(archive.read(BASE)), root, init)
    assert observe(root, summary) == json.loads(checked(root, summary['acceptance']).read_text())
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', choices=TARGETS, required=True)
    parser.add_argument('--run', required=True)
    args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            out = ROOT / 'output/release-readiness' / args.run
            out.mkdir()
            subjects = {'core': current_subjects(args.target)['core']}
            case = descriptor(args.target, subjects)
            record = dict(status='preparing', target=args.target, run_id=args.run, subjects=subjects, base_case=case,
                started=datetime.datetime.now(datetime.timezone.utc).isoformat(), candidate_identity=current(ROOT, verify_worktree=True))
            def save(): (out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
            save()
            try:
                java = out / 'java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'
                java.parent.mkdir(parents=True)
                java.write_text(derive_java((ROOT / BASE_JAVA).read_text()))
                init = out / 'book-observations.init.gradle'
                init.write_text(init_source(ROOT, out))
                wrapper = out / 'Run-BookObservations.ps1'
                wrapper.write_text(script((ROOT / BASE).read_text(), ROOT, init))
                inputs = provenance.sources(ROOT, case)
                for path in [Path(__file__), Path(__file__).with_name('enchanting_book_12111.py'), Path(__file__).with_name('core_render_diagnostic.py'), java, init, wrapper]:
                    inputs[path.relative_to(ROOT).as_posix()] = provenance.sha(path)
                record.update(status='running', qa_provenance_schema=provenance.SCHEMA, qa_inputs=inputs,
                    qa_source_archive=provenance.bundle(ROOT, inputs), executed_wrapper=reference(ROOT, wrapper),
                    derived_init=reference(ROOT, init), derived_java=reference(ROOT, java))
                mc, loader = args.target.split('/')
                command = ['pwsh', '-NoProfile', '-File', str(wrapper), '-Minecraft', mc, '-Loader', loader, '-Scenario', 'core', '-RunId', args.run]
                record['command'] = command
                save()
                log = out / 'driver.log'
                with log.open('x') as stream:
                    result = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
                record.update(exit_code=result.returncode, log=log.relative_to(ROOT).as_posix(), log_sha256=provenance.sha(log))
                save()
                assert result.returncode == 0, 'Actual native book workflow failed; preserve the result'
                record['runtime_evidence'] = provenance.ordinary_gameplay_evidence(ROOT, case, record)
                acceptance = out / 'book-rendering-acceptance.json'
                acceptance.write_text(json.dumps(observe(ROOT, record), indent=2) + '\n')
                assert inputs == {name: provenance.sha(ROOT / name) for name in inputs}
                assert current_subjects(args.target)['core'] == subjects['core']
                record.update(status='passed', acceptance=reference(ROOT, acceptance))
                save()
                verify_run(ROOT, json.loads((out / 'summary.json').read_text()), subjects)
            except BaseException as error:
                record.update(status='failed', error=repr(error), trace=traceback.format_exc())
                raise
            finally:
                record['finished'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
                save()
        finally:
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__':
    main()
