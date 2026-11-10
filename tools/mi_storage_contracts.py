"""Verify the native Nuclear Hatch preview lock without relaxing other cell policies."""
import copy
import nbtlib

CASE = 'machine-modern_industrialization-nuclear_fluid_hatch'


def transfer_policies(cells, case, mc, policies):
    if case != CASE:
        return policies(cells)
    assert mc in {'1.20.1', '1.21.1'} and len(cells) == 3
    key = 'fluid' if mc == '1.20.1' else 'id'
    assert isinstance(cells[0]['amount'], nbtlib.Long)
    water = int(cells[0]['amount']) > 0 and str(cells[0]['key'].get(key)) == 'minecraft:water'
    if not water:
        return policies(cells)
    # NuclearHatch.tick clears/recomputes this product lock in a preview
    # transaction. Water's neutron product is deuterium; no fluid is produced.
    output = cells[1]
    assert isinstance(output['machineLocked'], nbtlib.Byte) and int(output['machineLocked']) == 1
    assert isinstance(output.get('locked'), nbtlib.String) and str(output['locked']) == 'modern_industrialization:deuterium'
    normalized = copy.deepcopy(cells)
    normalized[1]['machineLocked'] = nbtlib.Byte(0)
    del normalized[1]['locked']
    return policies(normalized)


def native_empty_stack(text):
    value=nbtlib.parse_nbt(text)
    if not isinstance(value,nbtlib.Compound):return False
    if not value:return True
    return (set(value)=={'id','Count'}and isinstance(value['id'],nbtlib.String)
        and str(value['id'])=='minecraft:air'and isinstance(value['Count'],nbtlib.Byte)and int(value['Count'])==0)


def observed_stack(text,stack):
    return stack('{}')if native_empty_stack(text)else stack(text)


def valid_carried_stack(text,item_count):
    if native_empty_stack(text):return True
    item,number=item_count(text)
    return item!='minecraft:air'and number>0


def output_examples(count):
    assert type(count) is int and count >= 1
    examples = [(0, 'minecraft:stone', 7, 9)]
    if count > 1:
        examples.append((count-1, 'minecraft:iron_ingot', 9, 10))
    return examples


def execution_cases(fluids, outputs):
    assert len(fluids) == 12 and len(outputs) == 5 and fluids.count('nuclear_fluid_hatch') == 1
    order = list(outputs)+['nuclear_fluid_hatch']+[name for name in fluids if name != 'nuclear_fluid_hatch']
    assert len(order) == len(set(order)) == 17
    return ['machine-modern_industrialization-'+name for name in order]


def native_item_variant(mc,item,metadata):
    assert mc in {'1.20.1','1.21.1'}
    return nbtlib.parse_nbt('{item:"'+item+'",tag:'+metadata+'}'if mc=='1.20.1'else'{id:"'+item+'",components:{"minecraft:custom_data":'+metadata+'}}')



def hatch_kinds(case):
    name=case.removeprefix('machine-modern_industrialization-')
    assert name in {'coke_oven','steam_blast_furnace','steam_quarry'}
    return ('ITEM_INPUT','ITEM_OUTPUT','FLUID_INPUT')if name=='steam_quarry'else('ITEM_INPUT','ITEM_OUTPUT','FLUID_INPUT','FLUID_OUTPUT')

def select_native_hatches(blueprint,case):
    required=hatch_kinds(case);supported={s.split(':')[-1].upper()for v in blueprint for s in v['allows']}
    assert set(required)<=supported
    if case.removeprefix('machine-modern_industrialization-')=='steam_quarry':assert 'FLUID_OUTPUT'not in supported
    selected=[];used=set()
    for kind in required:
        choices=[v for v in blueprint if(v['x'],v['y'],v['z'])not in used and kind in {s.split(':')[-1].upper()for s in v['allows']}]
        assert choices,('Native blueprint lacks its actual required hatch',kind)
        value=copy.deepcopy(min(choices,key=lambda v:(v['y'],v['x'],v['z'])))
        used.add((value['x'],value['y'],value['z']));value['kind']=kind;value['block']='modern_industrialization:'+('turbo'if case.removeprefix('machine-modern_industrialization-')=='steam_quarry'and kind=='ITEM_OUTPUT'else'bronze')+'_'+kind.lower()+'_hatch';selected.append(value)
    return selected
