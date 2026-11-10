"""Explicit native Create 6.0.10 item-component oracles and fixture encoding.

Only QA data/expectations are adapted. Actual server observations stay raw.
Component identities and enum serialization were inspected in the exact pinned
publisher: build/release-readiness/create-1211-*-component-*.txt.
"""
import copy,re
import nbtlib as n


def nodes(value):
    yield value
    if isinstance(value,dict):
        for v in value.values():yield from nodes(v)
    elif isinstance(value,list):
        for v in value:yield from nodes(v)


def component_contains(actual,expected):
    """Return None for unchanged native NBT checks, otherwise a strict result."""
    count=re.fullmatch(r'Count:(\d+)b',expected)
    boolean=re.fullmatch(r'(Blacklist|RespectNBT):([01])b',expected)
    mode=re.fullmatch(r'WhitelistMode:([012])',expected)
    address=re.fullmatch(r'Address:"(.*)"',expected)
    filename=re.fullmatch(r'File:"(.*)"',expected)
    replace=re.fullmatch(r'ReplaceMode:([0123])',expected)
    skip=expected=='SkipMissing:1b'
    empty=expected=='id:"minecraft:air"'
    if not(count or boolean or mode or address or filename or replace or skip or empty or expected=='Size:12'):return None
    value=n.parse_nbt(actual)
    if empty:return value=={} # Native ItemStack.saveOptional's empty sentinel.
    if count:
        return any(isinstance(v,dict)and'id'in v and'count'in v and int(v['count'])==int(count[1])for v in nodes(value))
    if filename:
        return any(isinstance(v,dict)and str(v.get('id',''))=='create:schematic'
            and str(v.get('components',{}).get('create:schematic_file',''))==filename[1]for v in nodes(value))
    if replace or skip:
        # SchematicannonBlockEntity writes its native options record under
        # Options; lookalike legacy/custom-data keys cannot satisfy it.
        options=value.get('Options',{})
        if replace:return isinstance(options.get('replace_mode'),n.Int)and int(options['replace_mode'])==int(replace[1])
        return isinstance(options.get('skip_missing'),n.Byte)and int(options['skip_missing'])==1
    components=[v['components']for v in nodes(value)if isinstance(v,dict)and isinstance(v.get('components'),dict)]
    if expected=='Size:12':
        for v in components:
            items=v.get('create:linked_controller_items')
            if items is None:continue
            if len(items)!=12 or sorted(int(i['slot'])for i in items)!=list(range(12)):return False
            return all(str(i['item']['id'])=='minecraft:stone'and int(i['item']['count'])>0 for i in items)
        return False
    if boolean:
        key={'Blacklist':'create:filter_items_blacklist','RespectNBT':'create:filter_items_respect_nbt'}[boolean[1]]
        # FilterMenu uses getOrDefault(false); the component may be omitted
        # when native defaults are restored. Require the actual filter item.
        filters=[v for v in nodes(value)if isinstance(v,dict)and str(v.get('id',''))=='create:filter']
        return bool(filters)and all(bool(v.get('components',{}).get(key,n.Byte(0)))==bool(int(boolean[2]))for v in filters)
    if mode:
        wanted=['whitelist_disj','whitelist_conj','blacklist'][int(mode[1])]
        filters=[v for v in nodes(value)if isinstance(v,dict)and str(v.get('id',''))=='create:attribute_filter']
        return bool(filters)and all(str(v.get('components',{}).get('create:attribute_filter_whitelist_mode','whitelist_disj'))==wanted for v in filters)
    if address:
        if address[1]=='':return any(isinstance(v,dict)and str(v.get('id',''))=='create:package_filter'
            and str(v.get('components',{}).get('create:package_address',''))==''for v in nodes(value))
        return any(str(v.get('create:package_address',''))==address[1]and'create:package_address'in v for v in components)


def modern_stack(old):
    assert 'Count'in old and'id'in old,old
    allowed={'Count','id','Slot','tag'};assert set(old)<=allowed,('Unreviewed legacy fixture fields',old)
    result=n.Compound({'id':copy.deepcopy(old['id']),'count':n.Int(int(old['Count']))})
    if'Slot'in old:result['Slot']=copy.deepcopy(old['Slot'])
    tag=old.get('tag');components=n.Compound()
    if tag and str(old['id'])=='create:filter':
        assert set(tag)<={'Items','Blacklist','RespectNBT'},tag
        contents=[]
        for entry in tag.get('Items',{}).get('Items',[]):
            item=modern_stack(entry);slot=int(item.pop('Slot'))
            contents.append(n.Compound({'slot':n.Int(slot),'item':item}))
        components['create:filter_items']=n.List[n.Compound](contents)
        components['create:filter_items_blacklist']=n.Byte(int(tag.get('Blacklist',0)))
        components['create:filter_items_respect_nbt']=n.Byte(int(tag.get('RespectNBT',0)))
    elif tag:
        assert str(old['id']).startswith('minecraft:'),('Unreviewed mod item fixture codec',old['id'])
        components['minecraft:custom_data']=copy.deepcopy(tag)
    if components:result['components']=components
    return result


def fixture_command(command):
    # The retained groups use exactly a vanilla stock chest and one authored
    # Stock Ticker Categories fixture. Never rewrite arbitrary native state.
    if 'Count:'not in command:return command
    prefix,separator,tail=command.partition('{')
    assert separator and (re.fullmatch(r'setblock -?\d+ -?\d+ -?\d+ minecraft:chest',prefix)
        or re.fullmatch(r'data merge block -?\d+ -?\d+ -?\d+ ',prefix)),command
    value=n.parse_nbt('{'+tail)
    if prefix.startswith('data merge'):assert set(value)=={'Categories'},value
    def convert(v):
        if isinstance(v,dict):
            if'Count'in v and'id'in v:return modern_stack(v)
            return n.Compound({k:convert(x)for k,x in v.items()})
        if isinstance(v,list):return type(v)([convert(x)for x in v])
        return copy.deepcopy(v)
    return prefix+convert(value).snbt(compact=True)


def schematic_stack(tag):
    """The exact registered native schematic components, not CUSTOM_DATA."""
    value=n.parse_nbt(tag)
    assert set(value)=={'Anchor','Bounds','Deployed','File','Mirror','Owner','Rotation'},value
    anchor=value['Anchor'];assert set(anchor)=={'X','Y','Z'}and len(value['Bounds'])==3
    assert all(isinstance(anchor[k],n.Int)for k in ['X','Y','Z'])
    assert all(isinstance(v,n.Int)and int(v)>0 for v in value['Bounds'])
    assert isinstance(value['Deployed'],n.Byte)and int(value['Deployed'])==1
    assert str(value['Mirror'])=='NONE'and str(value['Rotation'])=='NONE'
    assert re.fullmatch(r'[A-Za-z0-9_]{1,16}',str(value['Owner']))
    assert str(value['File'])in ['siu-create-functional-line.nbt','siu-create-functional-stone.nbt']
    components=n.Compound({
        'create:schematic_anchor':n.IntArray([int(anchor[k])for k in ['X','Y','Z']]),
        'create:schematic_bounds':n.IntArray([int(v)for v in value['Bounds']]),
        'create:schematic_deployed':n.Byte(1),'create:schematic_file':n.String(str(value['File'])),
        'create:schematic_owner':n.String(str(value['Owner'])),
        'create:schematic_rotation':n.String('none'),'create:schematic_mirror':n.String('none')})
    return n.Compound({'id':n.String('create:schematic'),'count':n.Int(1),'components':components}).snbt(compact=True)
