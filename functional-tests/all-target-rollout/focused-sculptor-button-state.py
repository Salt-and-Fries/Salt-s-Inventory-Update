"""Check native payment eligibility and Begin only, without playing the challenge."""
from pathlib import Path
import argparse
import json
from evidence import send

p = argparse.ArgumentParser()
p.add_argument('run', type=Path)
a = p.parse_args()
checks = []


def act(kind, **kwargs):
    return send(a.run, kind, **kwargs)['client']


def window(state):
    return next(w for w in state.get('allWindows', state.get('windows', []))
                if w.get('menuId') == 'mowziesmobs:sculptor_trade')


def button(w):
    return next(v for v in w['nativeWidgets'] if v.get('visible') and v.get('message') == 'Begin Test')


def check(label, okay):
    checks.append(dict(check=label, passed=bool(okay)))
    (a.run / 'sculptor-button-state-checks.json').write_text(json.dumps(dict(
        scope='Empty/wrong/correct native payment eligibility and native Begin action only; challenge gameplay excluded',
        checks=checks), indent=2))
    assert okay, label
    print('PASS', label, flush=True)


def tap_button(w):
    widget, native = button(w), w['nativeBounds']
    x, y = widget['x'] + widget['width']/2 - native['x'], widget['y'] + widget['height']/2 - native['y']
    region = next(r for r in w['template']['regions'] if r['source']['x'] <= x < r['source']['x'] + r['source']['width']
                  and r['source']['y'] <= y < r['source']['y'] + r['source']['height'])
    b, pad = w['bounds'], w['template']['padding']
    x = b['x'] + 6 + pad + region['x'] + x - region['source']['x']
    y = b['y'] + 20 + pad + region['y'] + y - region['source']['y']
    act('click', x=x, y=y, button=0, ticks=2)
    return act('release', x=x, y=y, button=0, ticks=12)


def seed(item):
    return act('reviewed-session-seed', menu='com.bobmowzie.mowziesmobs.server.inventory.ContainerSculptorTrade',
               slot=0, item=item, count=1, ticks=16)


state = act('wait', ticks=3)
check('empty-payment-disables-native-Begin-button', not button(window(state))['active'])
tap_button(window(state))
act('reviewed-entity-probe', ticks=3)
probe = json.loads((a.run / 'reviewed-entity-probe.json').read_text())
owner = next(e for e in probe['entities'] if e.get('type') == 'mowziesmobs:sculptor')
check('clicking-empty-disabled-button-does-not-start-test', owner['isTesting'] == 'false')
state = seed('minecraft:stone')
check('wrong-payment-stays-disabled', not button(window(state))['active'])
state = seed('mowziesmobs:bluff_rod')
check('correct-payment-enables-native-button', button(window(state))['active'])
act('capture', capture='sculptor-correct-payment-enabled', ticks=5)
state = seed('minecraft:air')
check('removing-payment-disables-button-again', not button(window(state))['active'])
state = seed('mowziesmobs:bluff_rod')
tap_button(window(state))
act('reviewed-entity-probe', ticks=3)
probe = json.loads((a.run / 'reviewed-entity-probe.json').read_text())
owner = next(e for e in probe['entities'] if e.get('type') == 'mowziesmobs:sculptor')
act('snapshot-server', ticks=3)
server = json.loads((a.run / 'server-state.json').read_text())
check('native-Begin-starts-challenge-and-consumes-payment', owner['isTesting'] == 'true' and
      all('mowziesmobs:bluff_rod' not in m['slots'][0]['item'] for m in server['menus']
          if m['class'].endswith('.ContainerSculptorTrade')))
