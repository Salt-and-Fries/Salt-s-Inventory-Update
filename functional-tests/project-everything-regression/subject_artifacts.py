"""Keep runtime evidence tied to both exact shipping subjects."""
from pathlib import Path
import hashlib
ROOT=Path(__file__).resolve().parents[2]

def subject_signature(artifacts):
    return {a['family']:(a.get('filename'),a.get('sha256')) for a in artifacts if a.get('family') in {'core','compatibility'}}

def current_subjects(target):
    mc,loader=target.split('/');label={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[loader]
    names={'core':f'salts_inventory_update-{mc}-{label}-0.1.6.jar',
           'compatibility':f'salts_inventory_compat-{mc}-{label}-0.1.3.jar'}
    return {family:(name,hashlib.sha256((ROOT/'dist'/family/name).read_bytes()).hexdigest()) for family,name in names.items()}

def subjects_match(artifacts,expected):
    actual=subject_signature(artifacts)
    return set(actual)=={'core','compatibility'} and actual==expected
