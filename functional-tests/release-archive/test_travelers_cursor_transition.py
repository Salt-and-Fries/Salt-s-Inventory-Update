"""Execute the shipping callback's cursor-finally block against session retirement.

This isolates the API lifetime contract; native-world settings/return checks are
separate and must still pass with the built jars and fully tagged carried items.
"""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
VERSIONS = ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']
SOURCES = [ROOT/'compatibility/versions'/v/'fabric/src/main/java/com/salts_inventory_update/compat/travelersbackpack/TravelersPackets.java'
           for v in VERSIONS] + [ROOT/'tools/rollout-templates/TravelersPackets.java']


class TravelersCursorTransitionTests(unittest.TestCase):
    def test_native_callback_retirement_preserves_canonical_cursor_and_live_commits(self):
        bodies = []
        for path in SOURCES:
            text = path.read_text()
            # The outer callback finally is immediately followed by the public
            # broadcast request. Execute those actual statements, not a copy.
            matches = re.findall(r'finally\s*\{([^{}]*c\.setCarriedStack[^{}]*)\}\s*c\.broadcastChanges\(\);', text)
            self.assertEqual(1, len(matches), str(path))
            bodies.append(matches[0])
        methods = '\n'.join('static void callback%d(C c, boolean carried) { boolean cursor=carried; try { c.nativeAction(); } finally { %s } c.broadcastChanges(); }' % (i, body)
                            for i, body in enumerate(bodies))
        harness = '''import java.util.function.Predicate;
public final class TravelersCursorContract {
  static final String FULL_TAGGED = "diamond*7:{name:cursor,components:{nested:[1,2,3]}}";
  static final String MODIFIED = "diamond*3:{name:cursor,components:{nested:[1,2,3]}}";
  static final RuntimeException NATIVE = new IllegalArgumentException("original native failure");
  static final class Menu { String cursor=FULL_TAGGED; String getCarried(){return cursor;} }
  static final class C {
    final Menu old=new Menu(); Menu active=old; String canonical=FULL_TAGGED;
    int mode,writes,broadcasts,dispatches;
    C(int mode){this.mode=mode;}
    C player(){return this;} Menu menu(){return old;}
    void setCarriedStack(String value){if(active!=old)throw new IllegalStateException("retired cursor context");writes++;canonical=value;old.cursor="empty";}
    void broadcastChanges(){broadcasts++;}
    void nativeAction(){
      dispatches++;
      if(mode==1||mode==4){active=new Menu();old.cursor="empty";}
      else if(mode!=5)old.cursor=MODIFIED;
      if(mode==3||mode==4)throw NATIVE;
    }
  }
  static final class DesktopSessions {
    static boolean hasOpenSessionMatching(C c, Predicate<Menu> predicate){return predicate.test(c.active);}
  }
  interface Callback {void run(C c,boolean carried);}
  static void check(Callback callback){
    for(int mode=0;mode<6;mode++){
      C c=new C(mode);RuntimeException caught=null;
      try{callback.run(c,mode!=5);}catch(RuntimeException failure){caught=failure;}
      if(mode==3||mode==4){if(caught!=NATIVE)throw new AssertionError("native failure replaced",caught);}
      else if(caught!=null)throw new AssertionError("successful native callback threw, mode="+mode,caught);
      boolean retired=mode==1||mode==4;
      if(c.dispatches!=1||c.writes!=(retired||mode==5?0:1)||c.broadcasts!=(mode==3||mode==4?0:1))throw new AssertionError("dispatch/commit/broadcast lifetime, mode="+mode);
      if(!c.canonical.equals(retired||mode==5?FULL_TAGGED:MODIFIED))throw new AssertionError("lost or duplicated full carried data, mode="+mode);
    }
  }
  METHODS
  public static void main(String[] args){CALLS}
}
'''.replace('METHODS', methods).replace('CALLS', '\n'.join('check(TravelersCursorContract::callback%d);' % i for i in range(len(bodies))))
        javac = Path('C:/Program Files/Java/jdk-25.0.3/bin/javac.exe')
        with tempfile.TemporaryDirectory(prefix='siu-travelers-cursor-') as temporary:
            folder = Path(temporary); source=folder/'TravelersCursorContract.java'
            source.write_text(harness)
            subprocess.run([str(javac),'--release','17','-d',str(folder),str(source)],check=True)
            subprocess.run([str(javac.with_name('java.exe')),'-cp',str(folder),'TravelersCursorContract'],check=True)


if __name__ == '__main__': unittest.main()
