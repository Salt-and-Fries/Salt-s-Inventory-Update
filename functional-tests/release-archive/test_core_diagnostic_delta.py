"""Compile independent fixtures to reject overly broad diagnostic equivalence."""
from pathlib import Path
import importlib.util,shutil,subprocess,tempfile,unittest,zipfile

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('core_diagnostic_delta',ROOT/'tools/reconcile-core-diagnostic-delta.py')
audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
OLD='client render container desktop={} window={} menu={} slots={} api={} resizable={} false={} mouse={},{}'
NEW='client render container desktop={} window={} menu={} slots={} api={} resizable={} mouse={},{}'
DEBUG='''package com.salts_inventory_update.debug;
public final class DesktopDebug { public static void detail(String message,Object... values) { } }
'''
SOURCE='''package com.salts_inventory_update.client;
import com.salts_inventory_update.debug.DesktopDebug;
public class InventoryDesktopScreen {
    public String desktopId="desktop";
    public int unrelated() { return OTHER; }
    public void renderContainerWindow(int mouseX,int mouseY) {
        String window="window",menu="menu";
        DesktopDebug.detail("MESSAGE",desktopId,window,menu,27,"API",true,ARGUMENTS);
    }
}
'''


class CoreDiagnosticDelta(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp=tempfile.TemporaryDirectory(dir=ROOT/'build/release-readiness')
        cls.folder=Path(cls.temp.name).resolve();assert cls.folder.is_relative_to(ROOT/'build/release-readiness')
        cls.tool=audit.prepare(cls.folder/'inspector',Path('C:/Program Files/Java/jdk-26'))
        cls.old=cls.compile('old',OLD,'false,false,mouseX,mouseY')
        cls.new=cls.compile('new',NEW,'mouseX,mouseY')
        cls.swapped=cls.compile('swapped',NEW,'mouseY,mouseX')
        cls.other=cls.compile('other',NEW,'mouseX,mouseY',other=8)
        cls.partial=cls.compile('partial',NEW,'false,mouseX,mouseY')
        cls.field=cls.compile('field',NEW,'mouseX,mouseY',field=True)

    @classmethod
    def tearDownClass(cls):
        assert cls.folder.is_relative_to(ROOT/'build/release-readiness');cls.temp.cleanup()

    @classmethod
    def compile(cls,name,message,args,other=7,field=False):
        folder=cls.folder/name;java=folder/'source/com/salts_inventory_update/client';java.mkdir(parents=True)
        debug=java.parent/'debug';debug.mkdir();(debug/'DesktopDebug.java').write_text(DEBUG)
        source=SOURCE.replace('MESSAGE',message).replace('ARGUMENTS',args).replace('OTHER',str(other))
        if field:source=source.replace('public String desktopId','private String desktopId')
        p=java/'InventoryDesktopScreen.java';p.write_text(source);classes=folder/'classes';classes.mkdir()
        result=subprocess.run([cls.tool['javac'],'--release','17','-g','-d',str(classes),str(p),str(debug/'DesktopDebug.java')],capture_output=True,text=True)
        assert result.returncode==0,result.stdout+result.stderr
        jar=folder/'fixture.jar'
        with zipfile.ZipFile(jar,'x',zipfile.ZIP_DEFLATED)as archive:
            for member in sorted(classes.rglob('*.class')):archive.write(member,member.relative_to(classes).as_posix())
            archive.writestr('native-layout.json','{"unchanged":true}')
        return jar

    def check_delta(self,path,accepted):
        result=audit.execute(self.tool,'audit-jar',self.old,path,'core')
        self.assertEqual(result.returncode==0,accepted,result.stdout+result.stderr)

    def test_exact_independently_compiled_log_correction_is_accepted(self):
        self.check_delta(self.new,True)

    def test_old_contract_inspection_does_not_accept_unchanged_as_a_fix(self):
        result=audit.execute(self.tool,'inspect-old',self.old);self.assertEqual(result.returncode,0,result.stderr)
        self.assertIn('NO_NEW_ARTIFACT_ACCEPTED',result.stdout);self.check_delta(self.old,False)

    def test_mouse_coordinates_cannot_be_swapped(self):self.check_delta(self.swapped,False)
    def test_unrelated_method_cannot_change(self):self.check_delta(self.other,False)
    def test_one_false_argument_cannot_be_left_behind(self):self.check_delta(self.partial,False)
    def test_public_member_access_cannot_change(self):self.check_delta(self.field,False)

    def changed_jar(self,name,change):
        p=self.folder/(name+'.jar');assert not p.exists()
        with zipfile.ZipFile(self.new)as before,zipfile.ZipFile(p,'x',zipfile.ZIP_DEFLATED)as after:
            for item in before.infolist():
                if item.filename=='native-layout.json'and change=='remove':continue
                value=before.read(item.filename)
                if item.filename=='native-layout.json'and change=='edit':value=b'{"unchanged":false}'
                after.writestr(item,value)
            if change=='add':after.writestr('unexpected.json','{}')
            if change=='directory':after.writestr('unexpected/','')
        return p

    def test_unrelated_resource_changes_and_additions_are_rejected(self):
        for mode in ['edit','add','remove','directory']:
            with self.subTest(mode=mode):self.check_delta(self.changed_jar(mode,mode),False)

    def test_duplicate_archive_members_are_rejected(self):
        p=self.folder/'duplicates.jar';shutil.copyfile(self.new,p)
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter('ignore',UserWarning)
            with zipfile.ZipFile(p,'a')as jar:jar.writestr('native-layout.json','{}')
        self.check_delta(p,False)

    def test_noncore_requires_exact_archive_bytes(self):
        same=audit.execute(self.tool,'audit-jar',self.old,self.old,'unchanged');self.assertEqual(same.returncode,0,same.stderr)
        changed=audit.execute(self.tool,'audit-jar',self.old,self.new,'unchanged');self.assertNotEqual(changed.returncode,0)


if __name__=='__main__':unittest.main(verbosity=2)
