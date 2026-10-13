"""Portable regression checks for native/integration instruction rebasing."""
import copy
import importlib
import unittest
import json
import tempfile
from pathlib import Path

transfer = importlib.import_module('transfer-ae2-native-deltas')
legacy = importlib.import_module('transfer-ae2-legacy-common-deltas')
legacy15 = importlib.import_module('transfer-ae2-15-menu-deltas')
screens15 = importlib.import_module('transfer-ae2-15-screen-deltas')


class NativeDeltaMergeTest(unittest.TestCase):
    def test_cut_frame_adapter_preserves_the_public_graphics_wrapper_signature(self):
        signature = '(Lcom/salts_inventory_update/api/client/gui/GuiGraphicsExtractor;IIIIF)V'
        draw = {'type':5,'op':182,'owner':screens15.SCREEN,'name':'drawBG','desc':signature,'interface':False}
        target = {'methods':[{'name':'extractDesktopContent','instructions':[draw]},
                    {'name':'drawDesktopBottomEdge','instructions':[]}]}
        result = screens15.cut_frame_contract(target)
        actual = result['methods'][0]['instructions'][0]
        self.assertEqual(184, actual['op'])
        self.assertEqual('(Lappeng/client/gui/AEBaseScreen;' + signature[1:], actual['desc'])
        self.assertEqual('background', actual['name'])

    def test_plain_legacy_terminal_contours_use_the_native_97_pixel_footer(self):
        methods = [{'name':name,'instructions':[{'type':1,'op':16,'operand':99}]} for name in
                   ['desktopSurfaceSegments','drawDesktopBottomEdge','ordinaryNativeMethod']]
        result = screens15.adapt({'class':'appeng/client/gui/me/common/MEStorageScreen','methods':methods})
        self.assertEqual([97,97,99],[m['instructions'][0]['operand'] for m in result['methods']])

    def test_catalog_defaults_use_the_installed_version_footer(self):
        methods = [{'name': name, 'desc':'()V', 'instructions':[
            {'type':1,'op':16,'operand':99}, {'type':0,'op':177}]} for name in ['init','desktopDefaultContentSize','unrelated']]
        result = screens15.adapt({'class':screens15.CATALOG,'methods':methods})
        self.assertEqual([97,97,99], [m['instructions'][0]['operand'] for m in result['methods']])

    def test_legacy_client_action_uses_native_menu_class_invocation(self):
        native = {'instructions': [
            {'type': 5, 'op': 184, 'owner': 'appeng/core/sync/network/NetworkHandler', 'name': 'instance', 'desc': '()Lappeng/core/sync/network/NetworkHandler;', 'interface': False},
            {'type': 5, 'op': 185, 'owner': 'appeng/core/sync/network/NetworkHandler', 'name': 'sendToServer', 'desc': '(Lappeng/core/sync/BasePacket;)V', 'interface': True}]}
        result = legacy15.route_client_action(native)
        self.assertEqual({'type': 2, 'op': 25, 'var': 0}, result['instructions'][0])
        self.assertEqual((182, False, legacy15.MENU, 'sendPacketToServer'),
                         tuple(result['instructions'][1][key] for key in ['op', 'interface', 'owner', 'name']))
        self.assertEqual(185, native['instructions'][1]['op'])

    def test_desktop_cpu_fields_do_not_extend_the_vanilla_gui_sync_schema(self):
        annotation={'desc':'Lappeng/menu/guisync/GuiSync;','values':[]}
        fields=[{'name':'cantStoreItems','visibleAnnotations':[copy.deepcopy(annotation)]},
                {'name':'cpuStatusReady','visibleAnnotations':[copy.deepcopy(annotation)]},
                {'name':'cpuStatusSerial','visibleAnnotations':[copy.deepcopy(annotation)]}]
        value={'class':'appeng/menu/me/crafting/CraftingCPUMenu','fields':fields}
        transfer.g.session_cpu_state(value)
        self.assertEqual([annotation],value['fields'][0]['visibleAnnotations'])
        self.assertEqual([],value['fields'][1]['visibleAnnotations'])
        self.assertEqual([],value['fields'][2]['visibleAnnotations'])

    def test_desktop_cpu_drawing_preserves_the_entire_native_method_body(self):
        body=[{'type':8,'op':-1,'label':'native_entry'},{'type':0,'op':177}]
        native={'name':'drawBackgroundLayer','desc':'(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/Rect2i;Lappeng/client/Point;)V',
                'instructions':copy.deepcopy(body),'maxStack':2,'maxLocals':4,'handlers':[]}
        target={'class':'appeng/client/gui/widgets/CPUSelectionList','methods':[]}
        actual=screens15.cpu_renderer_contract(target,native)
        self.assertEqual(body,actual['instructions'][-len(body):])
        transfer.g.validate_labels(actual)
        self.assertEqual(1,sum(n.get('name')=='render' and n.get('owner','').endswith('/LegacyCpuSelection') for n in actual['instructions']))
        self.assertEqual(['siu$desktopCpuName','siu$desktopCpuStorage'],[m['name'] for m in target['methods']])
        self.assertEqual(native['instructions'],body)

    def test_hosted_checkbox_preserves_native_painting_and_selects_all_eight_icons(self):
        original = [{'type':8,'op':-1,'label':'ordinary'}, {'type':0,'op':177}]
        value = {'class':'appeng/client/gui/widgets/AECheckbox','methods':[{
            'name':'renderWidget','desc':'(Lnet/minecraft/client/gui/GuiGraphics;IIF)V',
            'instructions':copy.deepcopy(original),'handlers':[], 'maxStack':1,'maxLocals':5}]}
        actual = screens15.checkbox_contract(value)
        render = actual['methods'][0]
        self.assertEqual(original, render['instructions'][-len(original):])
        transfer.g.validate_labels(render)
        selector = actual['methods'][1]
        transfer.g.validate_labels(selector)
        code = selector['instructions']; labels = {n['label']:i for i,n in enumerate(code) if n['type']==8}
        for radio in [False,True]:
            for selected in [False,True]:
                for focused in [False,True]:
                    stack=[];pos=0
                    while True:
                        n=code[pos];pos+=1
                        if n['op']==25:stack.append({'isRadio':radio,'isSelected':selected})
                        elif n['op']==21:stack.append(focused)
                        elif n['op']==182:stack.append(stack.pop()[n['name']])
                        elif n['op']==153:
                            if not stack.pop():pos=labels[n['label']]
                        elif n['op']==178:stack.append(n['name'])
                        elif n['op']==176:break
                    expected=('RADIO_' if radio else '')+('CHECKED' if selected else 'UNCHECKED')+('_FOCUS' if focused else '')
                    self.assertEqual(expected,stack.pop())

    def test_cpu_hit_area_adapts_only_the_native_fallback_offset(self):
        value={'class':'appeng/client/gui/widgets/CPUSelectionList','methods':[
            {'name':'hitTestCpu','instructions':[{'type':10,'op':132,'var':2,'increment':-8},
                {'type':0,'op':4},{'type':0,'op':7},{'type':1,'op':16,'operand':8}]},
            {'name':'visibleRows','instructions':[{'type':1,'op':16,'operand':8}]}]}
        actual=screens15.adapt(copy.deepcopy(value))
        self.assertEqual(-9,actual['methods'][0]['instructions'][0]['increment'])
        self.assertEqual([4,7], [n['op'] for n in actual['methods'][0]['instructions'][1:3]])
        self.assertEqual(9,actual['methods'][0]['instructions'][3]['operand'])
        self.assertEqual(value['methods'][1],actual['methods'][1])

    def test_fluent_scrollbar_height_remains_a_method_call(self):
        node = {'type':5,'op':182,'owner':'appeng/client/gui/widgets/Scrollbar','name':'setHeight',
                'desc':'(I)Lappeng/client/gui/widgets/Scrollbar;','interface':False}
        value = {'class':'appeng/client/gui/AEBaseScreen','methods':[{'name':'resize','instructions':[node]}]}
        self.assertEqual(node, screens15.adapt(copy.deepcopy(value))['methods'][0]['instructions'][0])

    def test_runtime_lambda_names_and_private_handle_tags_match_the_native_jar(self):
        handle = {'kind':'handle','tag':5,'owner':'appeng/Fixture','name':'callback','desc':'()V','interface':False}
        named_node = {'type':6,'op':186,'name':'onPress','desc':'(Lappeng/Fixture;)Lnet/minecraft/Button$OnPress;',
                      'bootstrap':handle,'args':[handle]}
        raw_node = copy.deepcopy(named_node);raw_node['name']='m_93751_';raw_node['args'][0]['tag']=7;raw_node['bootstrap']['tag']=7
        named = {'class':'appeng/Fixture','superName':'java/lang/Object','interfaces':[],'fields':[],
                 'methods':[{'name':'create','desc':'()V','instructions':[named_node]}]}
        raw = copy.deepcopy(named);raw['methods'][0]['instructions']=[raw_node]
        with tempfile.TemporaryDirectory() as folder:
            roots=[Path(folder)/x for x in ['raw','named']]
            for root,value in zip(roots,[raw,named]):
                p=root/'appeng/Fixture.json';p.parent.mkdir(parents=True);p.write_text(json.dumps(value),encoding='utf-8')
            result=legacy15.remap_runtime(named,*roots)
            actual=result['methods'][0]['instructions'][0]
            self.assertEqual('m_93751_',actual['name'])
            self.assertEqual(7,actual['args'][0]['tag'])

    def test_complete_srg_mapping_disambiguates_a_minecraft_widget_getter(self):
        node={'type':5,'op':182,'owner':'net/minecraft/client/gui/components/AbstractWidget','name':'getY','desc':'()I','interface':False}
        value={'class':'appeng/Fixture','fields':[],'methods':[{'name':'position','desc':'()V','instructions':[node]}]}
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'raw').mkdir();(root/'named').mkdir()
            mapping=root/'mapping.srg'
            mapping.write_text('MD: net/minecraft/client/gui/components/AbstractWidget/m_252907_ ()I net/minecraft/client/gui/components/AbstractWidget/getY ()I\n',encoding='utf-8')
            actual=legacy15.remap_runtime(value,root/'raw',root/'named',minecraft_mappings=mapping)
            self.assertEqual('m_252907_',actual['methods'][0]['instructions'][0]['name'])

    def test_runtime_mapper_cache_invalidates_changed_mapping_inputs(self):
        node={'type':5,'op':182,'owner':'net/minecraft/Widget','name':'getY','desc':'()I','interface':False}
        value={'class':'appeng/Fixture','fields':[],'methods':[{'name':'position','desc':'()V','instructions':[node]}]}
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'raw').mkdir();(root/'named').mkdir();mapping=root/'mapping.srg'
            for native_name in ['m_1_', 'm_222_']:
                mapping.write_text(f'MD: net/minecraft/Widget/{native_name} ()I net/minecraft/Widget/getY ()I\n',encoding='utf-8')
                for repeat in range(2):
                    actual=legacy15.remap_runtime(value,root/'raw',root/'named',minecraft_mappings=mapping)
                    self.assertEqual(native_name,actual['methods'][0]['instructions'][0]['name'])

    def test_removed_rendering_abi_call_preserves_branch_destinations(self):
        method = {'name': 'drawBG', 'handlers': [], 'maxStack': 1, 'instructions': [
            {'type': 8, 'op': -1, 'label': 'entry'},
            {'type': 7, 'op': 167, 'label': 'native_body'},
            {'type': 8, 'op': -1, 'label': 'native_body'},
            {'type': 2, 'op': 25, 'var': 1},
            {'type': 5, 'op': 182, 'owner': 'net/minecraft/client/gui/GuiGraphicsExtractor',
             'name': 'nextStratum', 'desc': '()V', 'interface': False},
            {'type': 8, 'op': -1, 'label': 'native_header'}, {'type': 0, 'op': 177}]}
        result = legacy.native_legacy_render_method(method)
        transfer.g.validate_labels(result)
        self.assertEqual('native_body', result['instructions'][1]['label'])
        self.assertFalse(any(x.get('label') == 'native_header' for x in result['instructions']))

    def test_undefined_generated_branch_targets_are_rejected(self):
        method = {'name': 'bad', 'handlers': [], 'instructions': [{'type': 7, 'op': 167, 'label': 'missing'}]}
        with self.assertRaisesRegex(ValueError, 'Undefined branch labels'):
            transfer.g.validate_labels(method)

    def test_independent_new_native_and_integration_labels_do_not_alias(self):
        base = {'name': 'value', 'instructions': [
            {'type': 8, 'op': -1, 'label': 'o0'}, {'type': 0, 'op': 0},
            {'type': 0, 'op': 3}, {'type': 0, 'op': 172},
            {'type': 8, 'op': -1, 'label': 'o1'}],
            'handlers': [], 'maxStack': 1, 'maxLocals': 0}
        fork = copy.deepcopy(base); native = copy.deepcopy(base)
        for method, label, opcode in [(fork, 'fork_new', 4), (native, 'native_new', 5)]:
            method['instructions'][2:2] = [
                {'type': 8, 'op': -1, 'label': label},
                {'type': 0, 'op': opcode}, {'type': 0, 'op': 87}]
        merged = transfer.merge_method(base, fork, native)
        labels = [x['label'] for x in merged['instructions'] if x['type'] == 8]
        self.assertEqual(len(labels), len(set(labels)))
        self.assertTrue(any(x.startswith('native_') for x in labels))
        self.assertEqual(1, sum(x.get('op') == 4 for x in merged['instructions']))
        self.assertEqual(1, sum(x.get('op') == 5 for x in merged['instructions']))

    def test_conditional_pose_scale_duplicates_the_selected_stack_value(self):
        method = {'instructions': [
            {'type': 9, 'op': 18, 'constant': {'kind': 'floatBits', 'value': 1059749626}},
            {'type': 8, 'op': -1, 'label': 'selected'},
            {'type': 14, 'op': -1, 'frameType': -1, 'locals': [],
             'stack': [{'kind': 'Integer', 'value': 2}]},
            {'type': 5, 'op': 182, 'owner': 'org/joml/Matrix3x2fStack',
             'name': 'scale', 'desc': '(F)Lorg/joml/Matrix3x2f;', 'interface': False},
            {'type': 0, 'op': 87}], 'maxStack': 2}
        code = legacy.native_legacy_render_method(method)['instructions']
        call = next(i for i, x in enumerate(code) if x.get('name') == 'scale')
        self.assertEqual([89, 12], [x['op'] for x in code[call - 2:call]])
        self.assertEqual('(FFF)V', code[call]['desc'])


if __name__ == '__main__':
    unittest.main()
