"""Index native classes only in an outer jar and its declared nested dependencies."""
from pathlib import Path,PurePosixPath
import hashlib,io,json,tomllib,zipfile


def metadata(jar,loader):
    names=set(jar.namelist());mods=[];children=[]
    if loader=='fabric'and 'fabric.mod.json'in names:
        # These exact native Fabric releases include literal line breaks in
        # description strings (notably BetterEnd). The loader accepts them.
        # Relax only that JSON-string restriction; keep syntax/path/hash checks.
        data=json.loads(jar.read('fabric.mod.json'),strict=False)
        mods.append(dict(id=data['id'],version=str(data.get('version',''))))
        children += [(v['file'],'fabric.mod.json',None)for v in data.get('jars',[])]
    # Other-loader metadata can be an unexpanded template in multi-loader
    # libraries. Only the active loader's declaration defines this classpath.
    toml_names=(['META-INF/neoforge.mods.toml']if 'META-INF/neoforge.mods.toml'in names
        else ['META-INF/mods.toml'])if loader=='neoforge'else ['META-INF/mods.toml']if loader=='forge'else []
    for name in toml_names:
        if name in names:
            data=tomllib.loads(jar.read(name).decode('utf-8'))
            mods += [dict(id=v['modId'],version=str(v.get('version','')))for v in data.get('mods',[])]
    if loader in ['forge','neoforge']and 'META-INF/jarjar/metadata.json'in names:
        data=json.loads(jar.read('META-INF/jarjar/metadata.json'))
        children += [(v['path'],'META-INF/jarjar/metadata.json',v.get('identifier'))for v in data.get('jars',[])]
    return mods,children


def index(path,expected_sha512,loader):
    assert loader in ['fabric','forge','neoforge'],'Explicit native loader is required'
    raw=Path(path).read_bytes();actual=hashlib.sha512(raw).hexdigest()
    assert actual==expected_sha512,'Native publisher differs from the exact fixture pin'
    nodes=[]
    def walk(payload,chain,depth):
        assert depth<=6,'Nested dependency depth exceeds the reviewed bound'
        with zipfile.ZipFile(io.BytesIO(payload))as jar:
            names=set(jar.namelist());mods,children=metadata(jar,loader)
            nodes.append(dict(classes={v for v in names if v.endswith('.class')},mods=mods,chain=chain))
            seen=set()
            for entry,declared_by,identifier in children:
                name=PurePosixPath(entry)
                assert not name.is_absolute()and '..'not in name.parts and ':'not in entry and '\\'not in entry,'Invalid declared nested path'
                assert entry.endswith('.jar')and entry in names,'Missing declared nested dependency'
                if entry in seen:continue
                seen.add(entry);child=jar.read(entry)
                step=dict(entry=entry,declared_by=declared_by,sha256=hashlib.sha256(child).hexdigest())
                if identifier is not None:step['identifier']=identifier
                walk(child,chain+[step],depth+1)
    walk(raw,[],0)
    return dict(sha512=actual,loader=loader,nodes=nodes)


def matches(indexed,classes):
    wanted={v.replace('.','/')+'.class'for v in classes};found=[]
    for node in indexed['nodes']:
        names=sorted(wanted.intersection(node['classes']))
        if names:found.append(dict(classes=names,embedded_chain=node['chain'],declared_mods=node['mods']))
    return found
