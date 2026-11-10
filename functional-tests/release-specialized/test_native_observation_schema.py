"""Fail-closed contracts for the private read-only observation adapter."""
from pathlib import Path
import unittest
from native_observation_schema import enhance


class NamedObservationContracts(unittest.TestCase):
    def source(self):
        root=Path(__file__).resolve().parents[2]
        return (root/'functional-tests/all-target-rollout/java-1.21.1/com/salts_inventory_update/functionaltest/EverythingAuditQa.java').read_text()

    def test_real_named_source_has_actual_identity_and_template_fields(self):
        original=self.source(); result=enhance(original, '1.21.1', True)
        self.assertIn('BuiltInRegistries.MENU.getKey(session.menu().getType())', result)
        self.assertIn('observedTemplate.getClass().getName()', result)
        self.assertIn('row.put("hostScrollY",field(definition,"scrollY"))', result)
        self.assertIn('regression-native-entry-world', result)
        self.assertEqual(original.count('if(kind.equals("regression-world"))return true;'), 1)

    def test_changed_or_duplicate_anchors_are_rejected(self):
        text=self.source()
        for anchor in ['row.put("sessionId",session.sessionId());', 'row.put("template",call(definition,"layout"));',
                       'if(kind.equals("regression-world"))return true;']:
            for changed in [text.replace(anchor, ''), text+anchor]:
                with self.assertRaises(AssertionError): enhance(changed, '1.21.1', True)

    def test_repeated_adaptation_and_wrong_backend_are_rejected(self):
        text=self.source()
        with self.assertRaises(AssertionError): enhance(text, '1.20.1', False)
        # Repeating adaptation adds ambiguous observations; it must not happen.
        result=enhance(text, '1.21.1', True)
        self.assertIn('catch(UnsupportedOperationException unregisteredMenu){row.put("menuId",null);}',result)
        with self.assertRaises(AssertionError): enhance(result, '1.21.1', True)

    def test_all_named_mapping_sources_preserve_original_actions(self):
        root=Path(__file__).resolve().parents[2]
        for mc in ['1.21.1','1.21.11','26.1.2','26.2','26.3']:
            text=(root/('functional-tests/all-target-rollout/java-'+mc)/'com/salts_inventory_update/functionaltest/EverythingAuditQa.java').read_text()
            with self.subTest(mc=mc):
                result=enhance(text, mc, True)
                for token in ['mc.gameMode.useItem(', 'mc.gameMode.useItemOn(']:
                    self.assertEqual(text.count(token), result.count(token))


if __name__ == '__main__': unittest.main()
