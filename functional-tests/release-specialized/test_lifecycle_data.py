"""Negative controls for actual lifecycle limits and complete resource ownership."""
import collections,copy,unittest
from ae2_quantum_features import expected_item
from lifecycle_features import bounds,closed,conserved,item,resources,RESOURCES,GOLD


def fixture(mc):
    inventory=[dict(index=i,item='{}')for i in range(41)]
    for i,name,count,data in RESOURCES:inventory[i]['item']=expected_item(mc,name,count,data)
    server=dict(inventory=inventory,cursor='{}',menus=[],drops=[],alive=True,uuid='synthetic-oracle-fixture',sameOriginalPlayer=True,
        snapshotLimit=32,sessionLimit=16,sourceLimit=128,graphNodeLimit=128,graphEdgeLimit=512,
        snapshots=32,sources=0,sourceKeys=0,graphNodes=0,graphEdges=0,companions=0,sourceLocks=0,automaticSources=0,
        playerStates=1,pendingUseTargets=0,nonce=13,playerToken=17,worldMatches=True,recipeHover=False)
    return dict(server=server,client=dict(sessions=0,deathScreen=False))


class LifecycleData(unittest.TestCase):
    def test_native_limits_and_settled_ownership_cannot_be_replaced_by_weak_or_boolean_counts(self):
        s=fixture('1.21.1')['server'];self.assertTrue(bounds(s))
        for key,value in [('snapshotLimit',33),('sessionLimit',17),('sourceLimit',129),('graphNodeLimit',129),('graphEdgeLimit',513),('snapshots',33),('sources',129),('sourceKeys',1),('graphNodes',129),('graphEdges',513),('companions',17),('sourceLocks',17),('automaticSources',17),('snapshots',True),('sources',0.0),('playerStates',2),('pendingUseTargets',1),('nonce',0),('playerToken',False),('worldMatches',1),('recipeHover',0)]:
            bad=copy.deepcopy(s);bad[key]=value;self.assertFalse(bounds(bad),(key,value))
    def test_full_typed_totals_reject_missing_items_extra_cursors_inputs_and_world_duplicates(self):
        for mc in ['1.20.1','1.21.1']:
            expected=resources(mc);state=fixture(mc);self.assertTrue(conserved(state['server'],mc,expected))
            for change in ['missing','inventory-duplicate','cursor','input','drop','typed','unknown-item','index']:
                s=copy.deepcopy(state['server']);diamond=s['inventory'][9]['item']
                if change=='missing':s['inventory'][9]['item']='{}'
                if change=='inventory-duplicate':s['inventory'][20]['item']=diamond
                if change=='cursor':s['cursor']=diamond
                if change=='input':s['menus']=[dict(cursor='{}',slots=[dict(projection=False,item=diamond)])]
                if change=='drop':s['drops']=[dict(item=diamond)]
                if change=='typed':s['inventory'][9]['item']=diamond.replace('[B;','[')
                if change=='unknown-item':s['inventory'][20]['item']=expected_item(mc,'minecraft:stone',1,'{unexpected:1b}')
                if change=='index':s['inventory'][20]['index']=19
                self.assertFalse(conserved(s,mc,expected),(mc,change))
    def test_native_cursor_policies_count_one_actual_owner_and_never_a_detached_mirror(self):
        for mc in ['1.20.1','1.21.1','26.3']:
            s=fixture(mc)['server'];diamond=s['inventory'][9]['item'];gold=s['inventory'][11]['item'];s['inventory'][9]['item']='{}';s['inventory'][11]['item']='{}';s['cursor']=gold
            mirror=gold if mc=='1.20.1'else'{}';s['menus']=[dict(cursor=mirror,slots=[dict(projection=False,item=diamond),dict(projection=True,item='{}')])]
            self.assertTrue(conserved(s,mc,resources(mc)))
            bad=copy.deepcopy(s);bad['menus'][0]['cursor']='{}'if mc=='1.20.1'else gold;self.assertFalse(conserved(bad,mc,resources(mc)))
            bad=copy.deepcopy(s);bad['menus'][0]['slots'][1]['item']=diamond;self.assertFalse(conserved(bad,mc,resources(mc)))
            bad=copy.deepcopy(s);bad['menus'][0]['slots'][0]['projection']=1;self.assertFalse(conserved(bad,mc,resources(mc)))
    def test_death_native_input_ownership_and_post_respawn_cleanup_are_distinct_contracts(self):
        mc='1.21.1';expected=resources(mc);state=fixture(mc);self.assertTrue(closed(state,mc,expected,sources_empty=True))
        for change in ['session','companion','source-lock','automatic','source','client-session','dead','native-screen']:
            d=copy.deepcopy(state)
            if change=='session':d['server']['menus']=[dict(cursor='{}',slots=[])]
            if change=='companion':d['server']['companions']=1
            if change=='source-lock':d['server']['sourceLocks']=1
            if change=='automatic':d['server']['automaticSources']=1
            if change=='source':d['server']['sources']=d['server']['sourceKeys']=1
            if change=='client-session':d['client']['sessions']=1
            if change=='dead':d['server']['alive']=False
            if change=='native-screen':d['client']['deathScreen']=True
            self.assertFalse(closed(d,mc,expected,sources_empty=True),change)
        dead=copy.deepcopy(state);dead['server']['alive']=False;dead['client'].update(deathScreen=True,sessions=1)
        # A hidden pre-respawn client cache is not editable while the real
        # DeathScreen owns input. Its server owners must already be gone.
        self.assertTrue(closed(dead,mc,expected,sources_empty=True,native_death=True));self.assertFalse(closed(dead,mc,expected,sources_empty=True))
        dead['client']['sessions']=True;self.assertFalse(closed(dead,mc,expected,sources_empty=True,native_death=True))
    def test_persistent_item_codec_types_and_quantity_are_required(self):
        for mc in ['1.20.1','1.21.1']:
            value=expected_item(mc,'minecraft:gold_ingot',7,GOLD);self.assertEqual(item(value,mc)[0],7)
            bad=value.replace('Count:7b','Count:7').replace('count:7','count:7.0f')
            with self.assertRaises(AssertionError):item(bad,mc)


if __name__=='__main__':unittest.main()
