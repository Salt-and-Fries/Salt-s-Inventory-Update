"""Exact signature accounting, including items already owned before a scenario."""
import hashlib
from pathlib import Path
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def tagged_total(data,item,signature):
    cells=[*data['physical'].values(),*data['inventory'],data['cursor']]
    return sum(s['count'] for s in cells if s['item']==item and s.get('signature')==signature)

def after_seed(before,item,signature,added):
    if added<0:raise ValueError('A seeded quantity cannot be negative')
    return tagged_total(before,item,signature)+added

def with_closed_source(data,session,cells):
    if any(m['session']==session for m in data['menus']):
        raise ValueError('An open native source is already represented in the physical ledger')
    physical=dict(data['physical'])
    for cell in cells:
        key=f'closed/{session}/{cell["index"]}'
        if key in physical:raise ValueError('Closed native stock was already accounted for')
        physical[key]=dict(cell)
    return {**data,'physical':physical}
