"""Reject incomplete CPU selector observations; these are not gameplay results."""
import copy,unittest
from pathlib import Path
from ae2_cpu_status_features import MENU,SCREEN,NAMES,synchronized
from ae2_cpu_status_observer import enhance


def fixture():
    menu=dict(menu=MENU,session=17,selected=8,cpus=list(range(9)),ready=True,available=True,active=False)
    return dict(client=[dict(menu,screen=SCREEN)],server=[copy.deepcopy(menu)]),dict(session=17)


class CpuStatusData(unittest.TestCase):
    def test_real_server_and_client_must_agree_on_selected_cpu(self):
        native,client=fixture();self.assertTrue(synchronized(native,client))
        for selected in [0,7]:
            changed=copy.deepcopy(native);changed['server'][0]['selected']=selected
            self.assertFalse(synchronized(changed,client))

    def test_incomplete_duplicate_wrong_order_or_unknown_cpu_list_rejected(self):
        for mode in ['missing','duplicate','reordered','unknown','bool']:
            native,client=fixture();server=native['server'][0]
            if mode=='missing':server['cpus'].pop(0)
            if mode=='duplicate':server['cpus'][0]=1
            if mode=='reordered':server['cpus'].reverse()
            if mode=='unknown':server['selected']=99
            if mode=='bool':server['cpus'][0]=False
            self.assertFalse(synchronized(native,client),mode)

    def test_foreign_or_duplicate_sessions_and_wrong_native_screen_rejected(self):
        for mode in ['foreign','duplicate','screen','menu']:
            native,client=fixture()
            if mode=='foreign':native['server'][0]['session']=99
            if mode=='duplicate':native['server'].append(copy.deepcopy(native['server'][0]))
            if mode=='screen':native['client'][0]['screen']='OtherScreen'
            if mode=='menu':native['server'][0]['menu']='OtherMenu'
            self.assertFalse(synchronized(native,client),mode)

    def test_unreceived_unavailable_or_active_job_is_not_idle_selector_acceptance(self):
        for key,value in [('ready',False),('available',False),('active',True),('selected',True)]:
            native,client=fixture();native['server'][0][key]=value
            self.assertFalse(synchronized(native,client),key)

    def test_observer_cannot_assign_client_status_or_call_native_selection_handler(self):
        root=Path(__file__).resolve().parents[2]
        text=(root/'functional-tests/release-specialized/ae2_cpu_status_observer.py').read_text()
        for forbidden in ['"selectCpu"','postUpdate(','setField(','setItem(','.cpuList =']:
            self.assertNotIn(forbidden,text)
        self.assertIn('getSelectedCpuSerial',text)
        # Both sides must observe SIU's actual numeric desktop session key;
        # AE2 desktopSyncId is a different, native UUID namespace.
        self.assertIn('field(state,"sessions")).entrySet()',text)
        self.assertIn('row.put("session",entry.getKey())',text)
        self.assertNotIn('desktopSyncId',text)
        self.assertIn('.mouseScrolled(x,y,0,direction)',text)
        self.assertEqual(len(NAMES),10)

    def test_unreviewed_source_target_and_raw_observer_are_rejected(self):
        for mc,named in [('1.20.1',True),('26.3',True),('1.21.1',False)]:
            with self.assertRaises(AssertionError):enhance('unused',mc,named)


if __name__=='__main__':unittest.main(verbosity=2)
