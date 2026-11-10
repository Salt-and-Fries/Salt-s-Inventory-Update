"""Isolated detailed-render configuration and actual compiled-path observations."""
from pathlib import Path
import hashlib,json,re,zipfile

BASE='functional-tests/scripts/Run-ReleaseGameplayQa.ps1'
CONFIG='config/salts_inventory_update/config.json'
WANTED={'enableMod':True,'enableDetailedConsoleLogs':True}
MARKER='client render container desktop='
OLD='client render container desktop={} window={} menu={} slots={} api={} resizable={} false={} mouse={},{}'
NEW='client render container desktop={} window={} menu={} slots={} api={} resizable={} mouse={},{}'
PATTERN=re.compile(r'client render container desktop=(.+?) window=(.*?) menu=(.*?) slots=([0-9]+) api=(.*?) resizable=(true|false) mouse=(-?[0-9]+),(-?[0-9]+)$')
ANSI=re.compile(r'\x1b\[[0-?]*[ -/]*[@-~]')


def script(base,root,init):
    """Preserve every original assertion; change only repo location and extra init."""
    base=base.replace('\r\n','\n');root=Path(root).resolve();init=Path(init).resolve();assert init.is_relative_to(root)
    anchor="$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path";assert base.count(anchor)==1
    base=base.replace(anchor,"$repo='"+str(root).replace("'","''")+"'",1)
    anchor='Push-Location -LiteralPath $repo';assert base.count(anchor)==1
    return base.replace(anchor,"$argsList+=@('-I','"+init.relative_to(root).as_posix().replace("'","''")+"')\n"+anchor,1)


def init_source(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)
    receipt=(folder/'initial-config-receipt.json').as_posix();snapshot=(folder/'initial-config.json').as_posix()
    return '''import groovy.json.JsonOutput
import java.security.MessageDigest
// Observation fixture configuration only; never changes original QA assertions.
gradle.projectsEvaluated {
    def root=gradle.rootProject
    def mc=root.providers.gradleProperty('qaMinecraft').get()
    def loader=root.providers.gradleProperty('qaLoader').get()
    def task=root.project(":${mc}:${loader}").tasks.named(loader=='fabric' ? 'runPackagedGameplay' : 'runFullGameplayQa')
    task.configure {
        doFirst {
            def runtime=workingDir.canonicalFile
            if (!runtime.canonicalPath.startsWith(root.projectDir.canonicalPath+File.separator))
                throw new GradleException('Diagnostic runtime must be inside the isolated workspace')
            def config=new File(runtime,'config/salts_inventory_update/config.json').canonicalFile
            if (!config.canonicalPath.startsWith(runtime.canonicalPath+File.separator) || config.exists())
                throw new GradleException('Preserve existing configuration; choose a new diagnostic run ID')
            config.parentFile.mkdirs()
            def text=JsonOutput.toJson([enableMod:true,enableDetailedConsoleLogs:true])
            config.setText(text,'UTF-8')
            def snapshot=new File('''+json.dumps(snapshot)+''');snapshot.setText(text,'UTF-8')
            def receipt=new File('''+json.dumps(receipt)+''')
            receipt.setText(JsonOutput.prettyPrint(JsonOutput.toJson([
                runtime:runtime.absolutePath,config:config.absolutePath,
                initialSha256:MessageDigest.getInstance('SHA-256').digest(config.bytes).encodeHex().toString(),
                existingConfigOverwritten:false,expected:[enableMod:true,enableDetailedConsoleLogs:true]
            ])),'UTF-8')
            println('DETAILED_RENDER_CONFIG_WRITTEN '+config.absolutePath)
        }
    }
}
'''


def prepare(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)
    init=folder/'detailed-render.init.gradle';wrapper=folder/'Run-DetailedRender.ps1'
    assert not init.exists()and not wrapper.exists()
    init.write_text(init_source(root,folder));wrapper.write_text(script((root/BASE).read_text(),root,init))
    return wrapper,init


def validate_log(text):
    """Every observed render row must expose both actual integer coordinates."""
    rows=[]
    for line in text.splitlines():
        line=ANSI.sub('',line)
        if MARKER not in line:continue
        value=line[line.index(MARKER):].strip();match=PATTERN.fullmatch(value)
        assert match,'Malformed shifted/obsolete detailed render row: '+value[:350]
        desktop,window,menu,slots,api,resizable,x,y=match.groups()
        rows.append(dict(desktop=desktop,window=window,menu=menu,slots=int(slots),api=api,resizable=resizable=='true',mouseX=int(x),mouseY=int(y)))
    assert rows and any(v['slots']>0 for v in rows),'No actual nonempty container rendering with detailed logging enabled'
    return dict(render_rows=len(rows),first=rows[0],last=rows[-1],
        scope='Observed rendering rows, not additional gameplay assertions; all matched numeric X/Y and exact corrected field layout.')


def loaded_literal(path):
    with zipfile.ZipFile(path)as jar:
        value=jar.read('com/salts_inventory_update/client/InventoryDesktopScreen.class')
        assert value[:4]==b'\xca\xfe\xba\xbe','Actual loaded member is not a classfile'
        assert NEW.encode()in value and OLD.encode()not in value,'Actual loaded Core does not contain the corrected compiled log'
    return True


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
