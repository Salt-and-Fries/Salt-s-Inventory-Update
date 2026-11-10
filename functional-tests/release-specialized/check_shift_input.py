"""Isolated JVM checks of the synthetic input fixture, not Minecraft gameplay."""
from pathlib import Path
import argparse,hashlib,json,subprocess
from shift_input import prepare

ROOT=Path(__file__).resolve().parents[2]


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True);a=p.parse_args()
    assert a.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.run)
    folder=ROOT/'output/release-readiness'/a.run;assert not folder.exists();folder.mkdir()
    java=Path('C:/Program Files/Java/jdk-26/bin/java.exe');agent=prepare(folder/'agent',java)
    source=folder/'InputConstants.java'
    source.write_text('''package com.mojang.blaze3d.platform;
public class InputConstants {
 public static boolean isKeyDown(long window,int key){return key==17||window==999L;}
 static void check(boolean value){if(!value)throw new AssertionError("Synthetic input invariant");}
 public static void main(String[] ignored){
  check("1".equals(System.getProperty("siu.release.qa.shift.matches")));
  check(!isKeyDown(0L,340)&&!isKeyDown(0L,344));
  check(isKeyDown(0L,17)&&!isKeyDown(0L,23)&&isKeyDown(999L,23));
  System.setProperty("siu.release.qa.shift","true");
  check(isKeyDown(0L,340)&&isKeyDown(0L,344));
  check(isKeyDown(0L,17)&&!isKeyDown(0L,23)&&isKeyDown(999L,23));
  System.setProperty("siu.release.qa.shift","false");
  check(!isKeyDown(0L,340)&&!isKeyDown(0L,344));
  check(isKeyDown(0L,17)&&!isKeyDown(0L,23)&&isKeyDown(999L,23));
  System.out.println("native keys/window predicate retained; only explicit Shift changes; restored afterward");
 }
}''')
    classes=folder/'fixture-classes';classes.mkdir()
    subprocess.run([str(java.with_name('javac.exe')),'--release','17','-d',str(classes),str(source)],check=True,capture_output=True,text=True)
    result=subprocess.run([str(java),'-javaagent:'+str(agent),'-cp',str(classes),'com.mojang.blaze3d.platform.InputConstants'],capture_output=True,text=True)
    (folder/'stdout.log').write_text(result.stdout);(folder/'stderr.log').write_text(result.stderr)
    assert result.returncode==0,(result.stdout,result.stderr)
    source.write_text('''package com.mojang.blaze3d.platform;
public class InputConstants {
 public static boolean one(long window,int key){return false;}
 public static boolean two(long window,int key){return false;}
 public static void main(String[] ignored){throw new AssertionError("Unreviewed input signature was not refused");}
}''')
    rejected=folder/'rejected-classes';rejected.mkdir()
    subprocess.run([str(java.with_name('javac.exe')),'--release','17','-d',str(rejected),str(source)],check=True,capture_output=True,text=True)
    bad=subprocess.run([str(java),'-javaagent:'+str(agent),'-cp',str(rejected),'com.mojang.blaze3d.platform.InputConstants'],capture_output=True,text=True)
    (folder/'rejected-stdout.log').write_text(bad.stdout);(folder/'rejected-stderr.log').write_text(bad.stderr)
    assert bad.returncode!=0 and'ClassFormatError'in bad.stderr,(bad.stdout,bad.stderr)
    sha=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
    record=dict(status='passed',scope='Isolated synthetic input fixture self-check. Not Minecraft gameplay or physical keyboard validation.',
        assertions=['one reviewed native query','Shift initially unpressed','other-key and window semantics initially unchanged',
            'both Shift keys pressed only while the explicit property is true','other keys/window unchanged while enabled',
            'original Shift restored afterward','unknown two-query shape refused'],
        agent_sha256=sha(agent),sources={str(p.relative_to(ROOT)):sha(p)for p in [Path(__file__),Path(__file__).with_name('shift_input.py'),Path(__file__).with_name('NativeInputShiftAgent.java')]},
        ordinary_exit=result.returncode,unreviewed_shape_exit=bad.returncode)
    (folder/'summary.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record,indent=2))


if __name__=='__main__':main()
