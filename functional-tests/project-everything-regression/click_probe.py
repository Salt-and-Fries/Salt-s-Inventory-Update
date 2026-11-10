"""Compile excluded, bounded input/authorization instrumentation for QA only."""
from pathlib import Path
import hashlib, json, subprocess, zipfile

ROOT = Path(__file__).resolve().parents[2]


def prepare(folder, java):
    folder = Path(folder) / 'click-probe'
    classes = folder / 'classes'
    classes.mkdir(parents=True)
    sources = [Path(__file__).with_name(name + '.java') for name in ['UiClickProbe', 'UiClickProbeRuntime']]
    asm = next(p for p in (Path.home() / '.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.10.1').glob('*/*.jar')
               if not p.name.endswith(('-sources.jar', '-javadoc.jar')))
    with (folder / 'compile.log').open('w') as log:
        subprocess.run([str(Path(java).with_name('javac.exe')), '--release', '17', '-proc:none', '-cp', str(asm),
                        '-d', str(classes), *map(str, sources)], stdout=log, stderr=subprocess.STDOUT, check=True)
    agent, runtime = folder / 'click-probe-agent.jar', folder / 'click-probe-runtime.jar'
    with zipfile.ZipFile(agent, 'x', zipfile.ZIP_DEFLATED) as jar:
        # The game already supplies ASM. Adding a second physical copy through
        # Class-Path makes Fabric correctly reject duplicate ASM definitions.
        jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nPremain-Class: UiClickProbe\n\n')
        for p in classes.glob('UiClickProbe*.class'):
            if not p.name.startswith('UiClickProbeRuntime'): jar.write(p, p.name)
    with zipfile.ZipFile(runtime, 'x', zipfile.ZIP_DEFLATED) as jar:
        for p in classes.rglob('UiClickProbeRuntime*.class'): jar.write(p, p.relative_to(classes).as_posix())
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    output = folder.parent / 'click-probe.jsonl'
    (folder / 'lineage.json').write_text(json.dumps({
        'scope': 'Excluded diagnostic recorder; bootstrap helper stores bounded events. No original instruction removed, no menu writes, no retry, no shipping archive changes. Instrumentation can affect timing.',
        'sources': {str(p.relative_to(ROOT)): sha(p) for p in sources}, 'agent_sha256': sha(agent),
        'runtime_sha256': sha(runtime), 'asm_sha256': sha(asm), 'asm_file':str(asm), 'event_limit': 16384,
        'output': str(output)}, indent=2) + '\n')
    return '-javaagent:' + str(agent.resolve()) + '=' + str(runtime.resolve()) + '|' + str(output.resolve())


def self_test():
    import argparse
    p = argparse.ArgumentParser(); p.add_argument('--run', required=True);p.add_argument('--protocol',choices=['legacy','modern'],default='legacy'); a = p.parse_args()
    folder = ROOT / 'build/ae2-click-investigation' / a.run
    assert not folder.exists(), 'Choose a fresh self-test run ID'
    folder.mkdir(parents=True)
    java = Path('C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/java.exe')
    argument = prepare(folder, java)
    sources = {
        'com/salts_inventory_update/debug/DesktopDebug.java': '''package com.salts_inventory_update.debug;
public class DesktopDebug {public static void trace(String s,Object...args){} public static void warn(String s,Object...args){}}''',
        'com/salts_inventory_update/api/desktop/DesktopMenuBindings.java': '''package com.salts_inventory_update.api.desktop;
public class DesktopMenuBindings {public static Object slot(Object menu,int slot){try{return menu.getClass().getMethod("slot",int.class).invoke(menu,slot);}catch(Exception e){throw new RuntimeException(e);}}}''',
        'com/salts_inventory_update/client/DesktopContainerClient.java': '''package com.salts_inventory_update.client;
public class DesktopContainerClient {private static Object collectedClicks;private static int stateIdFor(int id){return 4;}public static boolean clickSlot(int id,int session,int slot,int button,Object type,Object stack){return true;}}''',
        'com/salts_inventory_update/server/DesktopContainerSessions.java': '''package com.salts_inventory_update.server;
import java.util.*;import java.util.function.*;import com.salts_inventory_update.debug.DesktopDebug;
public class DesktopContainerSessions {
 public static class Slot {public String value="tagged-card";public String getItem(){return value;}}
 public static class Menu {public int stateId=4;public int containerId=0;public Slot slot=new Slot();public String carried="empty";public Slot slot(int id){return slot;}public String getCarried(){return carried;}}
 public static class Player {public Menu inventoryMenu=new Menu();}
 private static class PlayerSessions {public Map<Integer,Object> sessions=new HashMap<>();}
 public record DesktopClickPayload(int debugId,int sessionId,int slotIndex,int button,String inputName,String clientCarried){}
 public record Envelope(int primaryStateId,int secondaryStateId,String innerType,DesktopClickPayload payload){}
 private static class MutationRegistration {
  Function<Envelope,DesktopClickPayload> decoder=Envelope::payload;BiConsumer<Player,DesktopClickPayload> handler=(p,c)->{p.inventoryMenu.carried=p.inventoryMenu.slot.value;p.inventoryMenu.slot.value="empty";};
  private void dispatch(Player player,PlayerSessions sessions,Envelope envelope){
   try {DesktopClickPayload payload=decoder.apply(envelope);if(player.inventoryMenu.stateId!=envelope.primaryStateId()){DesktopDebug.trace("server mutation rejected player={} type={} reason=stale-state","qa",envelope.innerType());return;}handler.accept(player,payload);}
   catch(RuntimeException e){DesktopDebug.warn("server mutation rejected player={} type={} reason={}","qa",envelope.innerType(),e.toString());}
  }
 }
 public static void main(String[] args){
  Player p=new Player();var s=new PlayerSessions();var r=new MutationRegistration();
  if(!com.salts_inventory_update.client.DesktopContainerClient.clickSlot(1,0,0,0,"PICKUP","empty"))throw new AssertionError();
  r.dispatch(p,s,new Envelope(4,-1,"desktop_click",new DesktopClickPayload(1,0,0,0,"PICKUP","empty")));
  if(!p.inventoryMenu.carried.equals("tagged-card")||!p.inventoryMenu.slot.value.equals("empty"))throw new AssertionError("accepted semantics changed");
  r.dispatch(p,s,new Envelope(3,-1,"desktop_click",new DesktopClickPayload(2,0,0,0,"PICKUP","tagged-card")));
  if(!p.inventoryMenu.carried.equals("tagged-card")||!p.inventoryMenu.slot.value.equals("empty"))throw new AssertionError("rejection semantics changed");
  System.out.println("accepted and rejected native paths preserved");
 }
}''',
    }
    if a.protocol=='modern':
        sources['com/salts_inventory_update/client/DesktopContainerClient.java']='''package com.salts_inventory_update.client;
import com.salts_inventory_update.server.DesktopContainerSessions.DesktopClickPayload;
public class DesktopContainerClient {
 private record SessionAuth(int stateId){}private static SessionAuth sessionAuth(int id){return new SessionAuth(4);}
 private static boolean send(Object payload,String label){return true;}
 public static boolean clickSlot(int id,int session,int slot,int button,Object input,Object carried){return send(new DesktopClickPayload(id,id,session,id==1?4:3,slot,button,"PICKUP",carried.toString()),"click");}
}'''
        sources['com/salts_inventory_update/server/DesktopContainerSessions.java']='''package com.salts_inventory_update.server;
import java.util.*;import com.salts_inventory_update.debug.DesktopDebug;
public class DesktopContainerSessions {
 public static class Slot {public String value="tagged-card";public String getItem(){return value;}}
 public static class Menu {public int stateId=4;public int containerId=0;public Slot slot=new Slot();public String carried="empty";public Slot slot(int id){return slot;}public String getCarried(){return carried;}}
 public static class Player {public Menu inventoryMenu=new Menu();}
 private static class PlayerSessions {public Map<Integer,Object> sessions=new HashMap<>();}
 private static final Map<Object,PlayerSessions> PLAYERS=new HashMap<>();
 public record DesktopClickPayload(int debugId,long mutationId,int sessionId,int stateId,int slotIndex,int button,String inputName,String clientCarried){}
 private static void handleMutation(){}
 private static boolean authorizeMenu(Player p,DesktopClickPayload c){if(p.inventoryMenu.stateId!=c.stateId()){DesktopDebug.trace("server click rejected expected={} actual={} reason=stale-state",c.stateId(),p.inventoryMenu.stateId);return false;}return true;}
 private static void clickMenu(Player p,DesktopClickPayload c){p.inventoryMenu.carried=p.inventoryMenu.slot.value;p.inventoryMenu.slot.value="empty";}
 private static void click(Player p,DesktopClickPayload c){if(!authorizeMenu(p,c))return;clickMenu(p,c);}
 public static void main(String[] args){
  Player p=new Player();PLAYERS.put(p,new PlayerSessions());
  com.salts_inventory_update.client.DesktopContainerClient.clickSlot(1,0,0,0,"PICKUP","empty");
  click(p,new DesktopClickPayload(1,1,0,4,0,0,"PICKUP","empty"));
  if(!p.inventoryMenu.carried.equals("tagged-card")||!p.inventoryMenu.slot.value.equals("empty"))throw new AssertionError("accepted semantics changed");
  com.salts_inventory_update.client.DesktopContainerClient.clickSlot(2,0,0,0,"PICKUP","tagged-card");
  click(p,new DesktopClickPayload(2,2,0,3,0,0,"PICKUP","tagged-card"));
  if(!p.inventoryMenu.carried.equals("tagged-card")||!p.inventoryMenu.slot.value.equals("empty"))throw new AssertionError("rejection semantics changed");
  System.out.println("accepted and rejected modern paths preserved");
 }
}'''
    paths = []
    for name, text in sources.items():
        path = folder / 'fixture' / name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text(text); paths.append(path)
    classes = folder / 'fixture-classes'; classes.mkdir()
    subprocess.run([str(java.with_name('javac.exe')), '--release', '17', '-d', str(classes), *map(str, paths)], check=True, capture_output=True)
    asm=json.loads((folder/'click-probe/lineage.json').read_text())['asm_file']
    run = subprocess.run([str(java), argument, '-cp', str(classes)+';'+asm, 'com.salts_inventory_update.server.DesktopContainerSessions'], check=True, capture_output=True, text=True)
    (folder / 'stdout.log').write_text(run.stdout); (folder / 'stderr.log').write_text(run.stderr)
    rows = [json.loads(line) for line in (folder / 'click-probe.jsonl').read_text().splitlines()]
    assert not any(r['event'] == 'probe-error' for r in rows), rows
    mutations = [r for r in rows if r['event'] == 'server-mutation']
    assert len(mutations) == 2 and mutations[0]['handler_entered'] is True and mutations[1]['handler_entered'] is False
    assert mutations[0]['before']['revision'] == 4 and mutations[0]['after']['carried'] == 'tagged-card'
    assert 'read_error' not in mutations[0]['before'] and 'read_error' not in mutations[0]['player_before'], mutations
    assert 'reason=stale-state' in mutations[1]['rejection_format']
    assert len([r for r in rows if r['event'] == 'client-click']) == (1 if a.protocol=='legacy' else 2) and rows[-1]['overflow'] == 0
    if a.protocol=='modern':assert len([r for r in rows if r['event']=='client-click-packet'])==2
    print('Excluded probe self-test passed: original accepted/rejected behavior, click correlation, before/after state and rejection reason retained.')


if __name__ == '__main__': self_test()
