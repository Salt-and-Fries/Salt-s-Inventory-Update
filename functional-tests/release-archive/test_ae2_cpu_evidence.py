"""Reject fabricated or incomplete CPU selector/job evidence, without gameplay."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_native_cpu_evidence',ROOT/'tools/reconcile-ae2-cpu-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture():
    tag=nbtlib.parse_nbt(m.marker_item('1.21.1'));tag['count']=nbtlib.Int(1)
    marker=dict(item='minecraft:diamond',count=7,signature=tag.snbt());empty=dict(item='minecraft:air',count=0,signature='{}')
    frames=[]
    for i,label in enumerate(m.LABELS):
        terminal=label in{'original-terminal','ordinary-terminal-reopened'};menu=m.TERMINAL_MENU if terminal else m.STATUS_MENU
        screen=m.TERMINAL_SCREEN if terminal else m.STATUS_SCREEN;sid=20+i;moved=4<=i<=6
        widgets=[dict(type='appeng.client.gui.widgets.TabButton',message='Crafting Status',tooltips=[],visible=True,active=True,icon='CRAFTING_STATUS')]if terminal else[]
        client=dict(session=sid,menu=menu,screen=screen,widgets=widgets,bounds=dict(x=26 if moved else 10,y=32 if moved else 20,width=176,height=120))
        server=dict(session=sid,menu=menu)
        data=dict(inventory=[dict(index=j,**copy.deepcopy(marker if j==12 else empty))for j in range(41)],physical={},cursor=copy.deepcopy(empty),menus=[dict(session=sid,type=menu,slots=[])])
        native=None
        if not terminal:
            selected=8 if i in[3,4,5]else 0;scroll=6 if i in[2,3,4]else 0
            base=dict(session=sid,menu=menu,selected=selected,cpus=list(range(9)),ready=True,available=True,active=False)
            c=dict(base,screen=screen,left=10,top=20,visibleRows=3,scroll=scroll,narrow=False,selection=dict(x=3,y=8,width=140,height=80))
            native=dict(client=[c],server=[copy.deepcopy(base)])
        frames.append((label,dict(widgets=dict(client=[client],server=[server]),native=native,ledger=data)))
    closed=copy.deepcopy(frames[-1][1]['ledger']);closed['menus']=[]
    capture=dict(minecraft='1.21.1',loader='neoforge',id='atlas-ae2-terminal',expected_native_menu=m.TERMINAL_MENU,expected_native_screen=m.TERMINAL_SCREEN)
    world=[f'setblock {i} 0 0 ae2:cable_bus'for i in range(17)]+['setblock -1 0 0 ae2:creative_energy_cell','setblock 1 1 0 ae2:controller']+[f'setblock {i} 0 1 ae2:64k_crafting_storage'for i in range(9)]
    journal=dict(complete=True,driver_sha256=DRIVER,cases=[m.STATUS_CASE],checks=[dict(case=m.STATUS_CASE,name=name,passed=True)for name in m.NAMES],original_entries=[dict(original_capture=capture,executed_actions=[dict(type='fixture'),dict(type='use-block-ray')],additional_world_commands=world)])
    return journal,frames,closed


def controls_record(identity='one',pin='a'):
    return dict(schema=m.SCHEMA,target='1.20.1/fabric',case=m.CPU_CASE,disposition='functional-pass',original_native_classes=[m.CPU_MENU],native_classes=[m.CPU_MENU,m.CPU_SCREEN],
        subjects={'core':['core.jar','digest']},native_class_basis='native-typed actual QA contract',scope=m.CPU_SCOPE,limits=m.LIMITS,
        evidence=dict(file=identity,sha256=identity),acceptance=dict(file=identity+'-acceptance',sha256=identity),publishers=[dict(filename=pin+'.jar',sha512=pin,sha256=pin)],validation=dict(assertions=13,native_job_submissions=2,native_server_cancellations=2))


class CpuEvidenceTests(unittest.TestCase):
    def reject(self,mutate):
        j,s,c=fixture();mutate(j,s,c)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate_status('1.21.1/neoforge',j,s,c,DRIVER)
    def test_exact_idle_workflow_requires_ten_checks_and_ten_states(self):
        j,s,c=fixture();self.assertEqual(m.validate_status('1.21.1/neoforge',j,s,c,DRIVER),dict(assertions=10,states=10,native_cpus=9,contract='idle-native-scroll-selection-not-active-job-controls'))
    def test_original_driver_complete_checks_and_frame_order_required(self):
        for change in[lambda j,s,c:j.update(complete=1),lambda j,s,c:j.update(driver_sha256='wrong'),lambda j,s,c:j['checks'].pop(),lambda j,s,c:j['checks'][0].update(passed=1),lambda j,s,c:s.reverse()]:self.reject(change)
    def test_native_terminal_entry_and_actual_nine_cpu_prerequisite_required(self):
        self.reject(lambda j,s,c:j['original_entries'][0]['original_capture'].update(loader='fabric'))
        self.reject(lambda j,s,c:j['original_entries'][0]['executed_actions'][0].update(type='provider'))
        self.reject(lambda j,s,c:j['original_entries'][0]['executed_actions'][-1].update(type='command'))
        self.reject(lambda j,s,c:j['original_entries'][0]['additional_world_commands'].pop())
    def test_native_menu_screen_and_both_session_owners_must_match(self):
        self.reject(lambda j,s,c:s[1][1]['widgets']['client'][0].update(screen='OtherScreen'))
        self.reject(lambda j,s,c:s[1][1]['widgets']['server'][0].update(session=999))
        self.reject(lambda j,s,c:s[1][1]['native']['server'][0].update(session=999))
        self.reject(lambda j,s,c:s[1][1]['ledger'].update(menus=[]))
    def test_original_terminal_status_button_cannot_be_missing_hidden_or_inactive(self):
        self.reject(lambda j,s,c:s[0][1]['widgets']['client'][0].update(widgets=[]))
        self.reject(lambda j,s,c:s[7][1]['widgets']['client'][0]['widgets'][0].update(visible=False))
        self.reject(lambda j,s,c:s[0][1]['widgets']['client'][0]['widgets'][0].update(active=False))
    def test_real_cpu_ids_and_server_agreement_cannot_be_inferred(self):
        self.reject(lambda j,s,c:s[2][1]['native']['server'][0]['cpus'].reverse())
        self.reject(lambda j,s,c:s[3][1]['native']['server'][0].update(selected=0))
        self.reject(lambda j,s,c:s[8][1]['native']['client'][0]['cpus'].__setitem__(0,True))
        self.reject(lambda j,s,c:s[1][1]['native']['server'][0].update(active=True))
    def test_scroll_endpoints_first_last_selection_and_moved_input_required(self):
        self.reject(lambda j,s,c:s[2][1]['native']['client'][0].update(scroll=0))
        self.reject(lambda j,s,c:s[5][1]['native']['client'][0].update(scroll=6))
        self.reject(lambda j,s,c:s[4][1]['widgets']['client'][0]['bounds'].update(y=20))
        for at in[3,6]:
            def change(j,s,c,at=at):
                for side in['client','server']:s[at][1]['native'][side][0]['selected']=4
            self.reject(change)
    def test_scroll_geometry_and_original_java_numeric_types_required(self):
        for key,value in[('scroll',True),('visibleRows',9),('left',10.0),('narrow',0)]:self.reject(lambda j,s,c,key=key,value=value:s[2][1]['native']['client'][0].update({key:value}))
        self.reject(lambda j,s,c:s[3][1]['native']['client'][0]['selection'].update(height=0))
    def test_typed_foreign_items_physical_stacks_cursor_and_close_preserved(self):
        self.reject(lambda j,s,c:s[5][1]['ledger']['inventory'][12].update(count=6))
        self.reject(lambda j,s,c:s[3][1]['ledger']['inventory'][12].update(signature=s[3][1]['ledger']['inventory'][12]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s,c:s[4][1]['ledger']['physical'].update(hidden=copy.deepcopy(s[4][1]['ledger']['inventory'][12])))
        self.reject(lambda j,s,c:c['cursor'].update(count=1))
        self.reject(lambda j,s,c:c.update(menus=[dict(session=1)]))
    def test_only_reviewed_target_and_native_aliases_are_accepted(self):
        for target in m.TARGETS:self.assertEqual(m.specification(target,m.CPU_CASE,[m.CPU_MENU]),'controls')
        self.assertEqual(m.specification('1.21.1/neoforge',m.STATUS_ALIAS,[m.STATUS_SCREEN]),'idle-selector')
        for args in[('1.20.1/fabric',m.STATUS_ALIAS,[m.STATUS_SCREEN]),('1.21.1/neoforge',m.CPU_CASE,[m.STATUS_MENU]),('26.3/neoforge','other',[m.CPU_MENU])]:
            with self.assertRaises(AssertionError):m.specification(*args)
    def test_distinct_real_cpu_executions_keep_original_counts_and_both_pins(self):
        first=controls_record();second=controls_record('two','b');value=m.combine_controls([first,second])
        self.assertEqual(value['validation']['assertions'],13);self.assertEqual(value['additional_workflows'],[second]);self.assertEqual(len(value['publishers']),2)
        self.assertEqual(m.combine_controls([first]),first)
    def test_duplicate_nested_or_incompatible_cpu_execution_rejected(self):
        with self.assertRaises(AssertionError):m.combine_controls([controls_record(),controls_record(pin='b')])
        for key in['target','case','subjects','native_class_basis','scope']:
            second=controls_record('two','b');second[key]='other'
            with self.assertRaises(AssertionError):m.combine_controls([controls_record(),second])
        second=controls_record('two','b');second['additional_workflows']=[controls_record('three','c')]
        with self.assertRaises(AssertionError):m.combine_controls([controls_record(),second])


if __name__=='__main__':unittest.main(verbosity=2)
