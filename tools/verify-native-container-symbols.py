"""Check the version-specific automatic host against the actual compiler Minecraft inputs.

This is a focused linkage/mixin check, not gameplay validation. In particular, javac
does not resolve the method descriptors embedded in mixin @At strings.
"""
from pathlib import Path
import argparse, importlib.util, json, re, zipfile
from controlify_classfile import ClassIndex, read_class

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('boundaries',ROOT/'tools/verify-module-boundaries.py')
boundaries=importlib.util.module_from_spec(spec);spec.loader.exec_module(boundaries)
PREFIX='com/salts_inventory_update/'
NATIVE=('net/minecraft/','com/mojang/blaze3d/','com/mojang/renderpearl/')

def selected(path):
    return path.startswith((PREFIX+'client/automatic/',PREFIX+'mixin/client/Automatic',
        PREFIX+'internal/desktop/Automatic',PREFIX+'network/Automatic',PREFIX+'network/NativeTemplate'))

def verify(row):
    index=ClassIndex([*row['outputs'],*row['libraries']]);count=selectors=accessors=0
    # javac may encode an inherited java.util/Object member with a Minecraft owner.
    # Resolve those ancestors against the same compiler's standard library.
    base=zipfile.ZipFile(Path(row['jdk'])/'jmods/java.base.jmod');index.archives.append(base)
    for name in base.namelist():
        if name.startswith('classes/') and name.endswith('.class'):
            index.locations.setdefault(name[8:-6],(base,name))
    try:
        for output in row['outputs']:
            for file in Path(output).rglob('*.class'):
                relative=file.relative_to(output).as_posix()
                if not selected(relative):continue
                data=file.read_bytes();clazz=read_class(data)
                if row['minecraft'] != '1.20.1' and relative == PREFIX+'mixin/client/AutomaticScreenBackgroundMixin.class':
                    prefix='extract' if row['minecraft'].startswith('26.') else 'render'
                    required={prefix+'Background',prefix+'TransparentBackground',prefix+'BlurredBackground'}
                    assert required.issubset(clazz.injections),f'{relative}: native capture must suppress inherited backdrop passes: {required-set(clazz.injections)}'
                if row['minecraft'] not in ('1.20.1','1.21.1') and relative in (
                        PREFIX+'client/automatic/NativeFrameBuffer.class',PREFIX+'client/automatic/AutomaticNativeWindow.class'):
                    # New Minecraft invokes background separately from its foreground entrypoint.
                    background='extractBackground' if row['minecraft'].startswith('26.') else 'renderBackground'
                    assert any(kind=='method' and owner.startswith('net/minecraft/') and name==background
                               for kind,owner,name,descriptor in clazz.references),f'{relative}: missing separate native background phase'
                for kind,owner,name,descriptor in clazz.references:
                    if owner.startswith(NATIVE):
                        assert index.member(owner,kind,name,descriptor),f'{row["minecraft"]}/{row["loader"]}/{relative}: missing {owner}.{name}{descriptor}'
                        count+=1
                for target in clazz.mixin_targets:
                    if not target.startswith(NATIVE):continue
                    assert index.get(target),f'{relative}: missing target {target}'
                    for selector in clazz.injections:
                        name,sep,tail=selector.partition('(')
                        assert index.member(target,'method',name,'('+tail if sep else None),f'{relative}: missing mixin selector {target}.{selector}'
                        selectors+=1
                    source=ROOT/f'versions/{row["minecraft"]}/fabric/src/main/java'/relative.replace('.class','.java')
                    if source.exists():
                        text=source.read_text()
                        pattern=r'@(Accessor|Invoker)\("([^"\n]+)"\)\s+(?:[\w.$<>?,]+\s+)+([\w$]+)\('
                        for kind,name,method in re.findall(pattern,text):
                            descriptor=next(desc for member,desc in clazz.methods if member==method)
                            if kind=='Accessor':
                                args,result=descriptor[1:].split(')')
                                assert index.member(target,'field',name,args if result=='V' else result),f'{relative}: missing accessor {target}.{name}'
                            else:assert index.member(target,'method',name,descriptor),f'{relative}: missing invoker {target}.{name}{descriptor}'
                            accessors+=1
                for value in boundaries.utf8_constants(data):
                    match=re.fullmatch(r'L([^;]+);([^(:]+)(\(.*\).+)',value)
                    if match and match[1].startswith(NATIVE):
                        assert index.member(match[1],'method',match[2],match[3]),f'{relative}: missing @At target {value}'
                        selectors+=1
        assert count and selectors and accessors,f'No native host contracts checked for {row}'
        print(f'Native container symbols verified: {row["minecraft"]}/{row["loader"]}, {count} member references, {selectors} selectors, {accessors} accessors/invokers')
    finally:index.close()

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('manifest',type=Path);args=parser.parse_args()
    for row in json.loads(args.manifest.read_text()):verify(row)
