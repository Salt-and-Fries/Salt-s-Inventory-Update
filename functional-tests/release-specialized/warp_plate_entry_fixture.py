"""Select an ordinary empty hotbar cell before the pinned Warp Plate editor use.

The native plate consumes held warp shards instead of opening its editor. The
world fixture can drop/pick up a shard while approaching it. Preserve every
item and the original use packet, ray, assertions and recorded failure.
"""
from contextlib import contextmanager
from pathlib import Path
import datetime
import hashlib
import json

TARGET = '26.3/neoforge'
PUBLISHER = '24d3c0bd2fa5d22fbbdfa1352806fa13b77f2729c3a83ada80919d41149df94d'


@contextmanager
def empty_hand_entry(driver, target, publisher_sha256, receipt):
    if target != TARGET or publisher_sha256 != PUBLISHER:
        raise ValueError('Unreviewed Warp Plate publisher/target')
    receipt = Path(receipt)
    if receipt.exists(): raise FileExistsError(receipt)
    original = driver.send
    block = None
    def send(profile, kind, **kw):
        nonlocal block
        if kind == 'client-use-block' and block == 'waystones:warp_plate':
            selected = original(profile, 'regression-select-empty-hand', ticks=4)
            row = dict(time=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                       target=target, publisher_sha256=publisher_sha256,
                       helper_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                       selection=selected, next_original_command=dict(type=kind, **kw),
                       scope='Normal hotbar selection only; no item mutation, ray repair, use retry, forced menu or assertion substitution.')
            with receipt.open('a', encoding='utf-8') as stream:
                stream.write(json.dumps(row, separators=(',', ':'))+'\n')
        result = original(profile, kind, **kw)
        if kind == 'fixture': block = kw.get('block')
        return result
    driver.send = send
    try: yield
    finally:
        if driver.send is not send: raise RuntimeError('Unexpected concurrent driver adapter')
        driver.send = original
