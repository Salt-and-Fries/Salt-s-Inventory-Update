"""Require the original strict logger delta plus one explicit 1.21.11 book fix.

The original logger-only inspector and its acceptance mode remain unchanged.
The additional mode changes exactly one LDC feeding ENCHANTING_BOOK_TEXTURE
in <clinit>. It grants no gameplay coverage or automatic evidence retention.
"""
from pathlib import Path
import argparse
import datetime
import importlib.util
import json

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('original_core_log_delta', ROOT / 'tools/reconcile-core-diagnostic-delta.py')
original = importlib.util.module_from_spec(spec)
spec.loader.exec_module(original)

OLD_BOOK = 'textures/entity/enchantment/enchanting_table_book.png'
NEW_BOOK = 'textures/entity/enchanting_table_book.png'
BOOK_TARGETS = {'1.21.11/fabric', '1.21.11/neoforge'}
BOOK_MODE = 'core-book-1.21.11'


def java_source():
    text = original.JAVA
    additions = '''    static byte[] expectedBook(byte[] original) {
        ClassNode node=read(expected(original));int changes=0;
        var fields=node.fields.stream().filter(f->f.name.equals("ENCHANTING_BOOK_TEXTURE")).toList();
        require(fields.size()==1,"Unique book texture field required");
        FieldNode field=fields.get(0);
        require(field.access==(ACC_PRIVATE|ACC_STATIC|ACC_FINAL),"Book field declaration changed");
        for(MethodNode method:node.methods) for(AbstractInsnNode at:method.instructions.toArray()) {
            if(!(at instanceof LdcInsnNode literal)||!literal.cst.equals("OLD_BOOK"))continue;
            require(method.name.equals("<clinit>"),"Book literal is outside the static initializer");
            require(changes++==0,"Duplicate original book literal");
            AbstractInsnNode invoke=next(at),store=next(invoke);
            require(invoke instanceof MethodInsnNode call&&call.getOpcode()==INVOKESTATIC
                &&call.desc.equals("(Ljava/lang/String;)"+field.desc),"Book identifier construction changed");
            require(store instanceof FieldInsnNode target&&store.getOpcode()==PUTSTATIC
                &&target.owner.equals(node.name)&&target.name.equals(field.name)&&target.desc.equals(field.desc),
                "Book literal does not initialize the exact original field");
            literal.cst="NEW_BOOK";
        }
        require(changes==1,"Missing unique original 1.21.11 book literal");return bytes(node);
    }
'''.replace('OLD_BOOK', OLD_BOOK).replace('NEW_BOOK', NEW_BOOK)
    replacements = [
        ('    static byte[] entry(ZipFile jar,ZipEntry member)', additions + '    static byte[] entry(ZipFile jar,ZipEntry member)'),
        ('static void audit(Path before,Path after,boolean core)', 'static void audit(Path before,Path after,boolean core,boolean book)'),
        ('Arrays.equals(expected(x),bytes(read(y)))', 'Arrays.equals(book?expectedBook(x):expected(x),bytes(read(y)))'),
        ('require(args[3].equals("core")||args[3].equals("unchanged"),"Unknown audit scope");',
         'require(args[3].equals("core")||args[3].equals("core-book-1.21.11")||args[3].equals("unchanged"),"Unknown audit scope");'),
        ('audit(Path.of(args[1]),Path.of(args[2]),args[3].equals("core"));',
         'audit(Path.of(args[1]),Path.of(args[2]),!args[3].equals("unchanged"),args[3].equals("core-book-1.21.11"));'),
        ('System.out.println("CORE_LOG_ONLY_DELTA_VERIFIED");',
         'System.out.println(book?"CORE_LOG_AND_12111_BOOK_DELTA_VERIFIED":"CORE_LOG_ONLY_DELTA_VERIFIED");')]
    for before, after in replacements:
        assert text.count(before) == 1, 'Original strict inspector contract changed'
        text = text.replace(before, after)
    return text


JAVA = java_source()


def prepare(folder, jdk):
    saved = original.JAVA
    original.JAVA = java_source()
    try:
        return original.prepare(folder, jdk)
    finally:
        original.JAVA = saved


def mode(target, family):
    if family != 'core':
        return 'unchanged'
    return BOOK_MODE if target in BOOK_TARGETS else 'core'


execute = original.execute
sha = original.sha


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--id', required=True)
    p.add_argument('--before-ledger', type=Path, required=True)
    p.add_argument('--after-ledger', type=Path, required=True)
    p.add_argument('--before-root', type=Path, required=True)
    p.add_argument('--jdk', type=Path, default=Path('C:/Program Files/Java/jdk-26'))
    args = p.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.id)
    before, after = json.loads(args.before_ledger.read_text()), json.loads(args.after_ledger.read_text())
    assert before['status'] == after['status'] == 'passed'
    assert len(before['artifacts']) == len(after['artifacts']) == 36
    keys = lambda values: {(v['minecraft'], v['loader'], v['family']) for v in values}
    assert len(keys(before['artifacts'])) == 36 and keys(before['artifacts']) == keys(after['artifacts'])
    folder = ROOT / 'build/release-readiness' / ('core-final-bytecode-' + args.id)
    tool = prepare(folder, args.jdk)
    destination = ROOT / 'output/release-readiness' / ('core-final-bytecode-' + args.id + '.json')
    assert not destination.exists()
    record = dict(status='running', started=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        inspector=dict(file=Path(__file__).relative_to(ROOT).as_posix(), sha256=sha(__file__)),
        original_strict_inspector=dict(file='tools/reconcile-core-diagnostic-delta.py', sha256=sha(ROOT / 'tools/reconcile-core-diagnostic-delta.py')),
        tool=tool, records=[], book_targets=sorted(BOOK_TARGETS),
        limits='Exact compiled/source/resource delta gate only. Requires separate twelve logger smokes and two book rendering/reload workflows; creates no execution, coverage, release approval or publication.')
    def save(): destination.write_text(json.dumps(record, indent=2) + '\n')
    save()
    try:
        for old in before['artifacts']:
            new = next(v for v in after['artifacts'] if all(v[k] == old[k] for k in ['minecraft', 'loader', 'family']))
            x, y = args.before_root / old['file'], ROOT / new['file']
            assert sha(x) == old['sha256'] and sha(y) == new['sha256']
            target = old['minecraft'] + '/' + old['loader']
            scope = mode(target, old['family'])
            result = execute(tool, 'audit-jar', x, y, scope)
            log = folder / (old['family'] + '-' + target.replace('/', '-') + '.log')
            log.write_text(result.stdout + result.stderr)
            record['records'].append(dict(target=target, family=old['family'], scope=scope,
                before=dict(file=str(x), sha256=sha(x)), after=dict(file=str(y), sha256=sha(y)),
                exit_code=result.returncode, log=dict(file=str(log), sha256=sha(log))))
            save()
            assert result.returncode == 0, 'Unreviewed compiled/member/resource change: ' + str(log)
        assert sum(v['scope'] == BOOK_MODE for v in record['records']) == 2
        record.update(status='exact-10-Core-log-only-2-Core-log-and-book-24-identical-modules',
            finished=datetime.datetime.now(datetime.timezone.utc).isoformat())
    except BaseException as error:
        record.update(status='failed', error=repr(error))
        raise
    finally:
        save()
    print(record['status'])


if __name__ == '__main__':
    main()
