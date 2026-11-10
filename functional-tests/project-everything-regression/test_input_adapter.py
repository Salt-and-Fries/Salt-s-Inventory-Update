"""Regress native JSON numeric counts before they become Minecraft commands."""
from pathlib import Path
import sys,unittest,tempfile,json
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
import evidence

class HeldInputTests(unittest.TestCase):
    profile=ROOT/'build/all-target-rollout/gameplay/26.2-neoforge-adapter-test'
    def test_capture_json_float_count_becomes_an_integer_command(self):
        with patch.object(evidence,'raw_send',return_value={'client':{}}) as send:
            evidence.send(self.profile,'held',item='sophisticatedbackpacks:backpack',count=1.0,ticks=6)
        send.assert_called_once_with(self.profile,'server-command',command='item replace entity @a weapon.mainhand with sophisticatedbackpacks:backpack 1',ticks=6)
    def test_regular_count_and_wait_are_retained(self):
        with patch.object(evidence,'raw_send',return_value={'client':{}}) as send:
            evidence.send(self.profile,'held',item='minecraft:diamond',count=7,ticks=12)
        self.assertEqual('item replace entity @a weapon.mainhand with minecraft:diamond 7',send.call_args.kwargs['command'])
        self.assertEqual(12,send.call_args.kwargs['ticks'])
    def test_fractional_count_is_rejected_instead_of_silently_truncated(self):
        with patch.object(evidence,'raw_send') as send:
            with self.assertRaisesRegex(ValueError,'integer'):evidence.send(self.profile,'held',item='minecraft:diamond',count=1.5)
        send.assert_not_called()
    def test_window_kind_is_payload_not_the_command_parameter(self):
        with patch.object(evidence,'raw_send',return_value={'client':{}}) as send:
            evidence.send(self.profile,'regression-builtin-info',kind='CHARACTER')
        send.assert_called_once_with(self.profile,'regression-builtin-info',kind='CHARACTER')
    def test_transport_publishes_kind_field_without_a_python_argument_collision(self):
        transport=sys.modules[evidence.raw_send.__module__]
        with tempfile.TemporaryDirectory() as folder:
            with patch.object(transport,'read',side_effect=[{}, {}, {'sequence':0,'client':{}}]):
                result=evidence.raw_send(Path(folder),'regression-builtin-info',kind='CHARACTER')
            request=json.loads((Path(folder)/'command.json').read_text())
            self.assertEqual('regression-builtin-info',request['type'])
            self.assertEqual('CHARACTER',request['kind'])
            self.assertEqual(0,result['sequence'])

class SdlKeyTests(unittest.TestCase):
    profile=ROOT/'build/all-target-rollout/gameplay/26.3-neoforge-adapter-test'
    def test_select_all_has_a_scancode_character_and_native_control_mask(self):
        with patch.object(evidence,'raw_send',return_value={'client':{}}) as send:
            evidence.send(self.profile,'key',key=65,modifiers=2,ticks=1)
        send.assert_called_once_with(self.profile,'key',key=4,keycode=97,modifiers=64,sdl=True,ticks=1)
    def test_native_screen_uses_the_same_key_abi(self):
        with patch.object(evidence,'raw_send',return_value={'client':{}}) as send:
            evidence.send(self.profile,'regression-native-key',key=269,modifiers=1)
        send.assert_called_once_with(self.profile,'regression-native-key',key=77,keycode=0,modifiers=1,sdl=True)
    def test_legacy_keyboard_events_are_unchanged(self):
        with patch.object(evidence,'raw_send',return_value={'client':{}}) as send:
            evidence.send(ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-adapter-test','key',key=65,modifiers=2)
        send.assert_called_once_with(ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-adapter-test','key',key=65,modifiers=2)

if __name__=='__main__':unittest.main()
