"""Run manifest-defined actual interactions in one already-open audit client.

Each case provides native_actions and core_actions explicitly; no provider or
forced registration is inferred. A missing or failed opening stays an outcome,
not a fabricated comparison. Run the two sides in separate disposable clients.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import re

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('sender', ROOT / 'functional-tests/project-everything-ten/send.py')
sender = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sender)

def run(manifest, output, side):
    data = json.loads(manifest.read_text(encoding='utf-8'))
    if side not in ('native', 'core'):
        raise ValueError('Unknown side')
    rows = []
    output.mkdir(parents=True, exist_ok=True)
    abort_reason = None
    for case in data['cases']:
        case_id = case['id']
        if not re.fullmatch(r'[a-z0-9-]+', case_id):
            raise ValueError('Case id must contain lowercase letters, digits and hyphens')
        row = {'id':case_id, 'family':case['family'], 'side':side,
               'minecraft':data['minecraft'], 'loader':data['loader'], 'runtime_output':str(output), 'actions':[]}
        rows.append(row)
        if abort_reason:
            row.update(status='not attempted', error=abort_reason)
            continue
        try:
            for action in case[side + '_actions']:
                args = dict(action)
                kind = args.pop('type')
                if kind in ('provider','native-template-probe','profile','reload-profiles'):
                    raise ValueError('Forced providers and adapters are forbidden for comparison')
                if side == 'core' and data['loader'] == 'neoforge' and kind == 'open' and not args.get('realUse'):
                    raise ValueError('NeoForge observer open defaults to a provider shortcut; use client-use-block-ray or explicit realUse instead')
                reply = sender.send(output, kind, **args)
                row['actions'].append({'request':action, 'reply':reply})
                if reply.get('client', {}).get('worldLoaded') is False:
                    abort_reason = 'The client left its disposable test world; remaining openings require a fresh client.'
                    raise RuntimeError(abort_reason)
            image_name = side + '-' + case_id
            reply = sender.send(output, 'capture', ticks=12, capture=image_name)
            image = output / 'screenshots' / (image_name + '.png')
            if not image.is_file():
                raise FileNotFoundError('Screenshot was not written: ' + str(image))
            row.update(status='captured; classification and visual review pending',
                       capture=str(image), sha256=hashlib.sha256(image.read_bytes()).hexdigest(),
                       observation=reply)
        except Exception as error:
            row.update(status='capture failed', error=str(error))
            if isinstance(error, TimeoutError):
                abort_reason = 'Capture driver timed out; remaining cases were not attempted.'
        finally:
            if not abort_reason:
                try:
                    sender.send(output,'close',ticks=12)
                except Exception as error:
                    row['close_error'] = str(error)
                    if isinstance(error, TimeoutError):
                        abort_reason = 'Capture driver timed out while closing; remaining cases were not attempted.'
            (output / ('audit-' + side + '-cases.json')).write_text(json.dumps(rows,indent=2),encoding='utf-8')
            print(case_id, row['status'], flush=True)
    (output / ('audit-' + side + '-cases.json')).write_text(json.dumps(rows,indent=2),encoding='utf-8')
    return rows

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('manifest',type=Path)
    parser.add_argument('output',type=Path)
    parser.add_argument('side',choices=['native','core'])
    args=parser.parse_args()
    run(args.manifest,args.output,args.side)
