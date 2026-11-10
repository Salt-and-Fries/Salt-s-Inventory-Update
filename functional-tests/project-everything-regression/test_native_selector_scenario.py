"""A virtual browser must never be certified by a physical storage oracle."""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

class NativeSelectorScenarioTests(unittest.TestCase):
    def test_selector_has_native_navigation_and_physical_coexistence_assertions(self):
        text=Path(__file__).with_name('polymer_checks.py').read_text()
        for fragment in ['regression-native-slot-tap','regression-native-key','native category click replaces',
                         'category navigation grants no physical items','same category browser',
                         'ordinary barrel still accepts','metadata']:
            self.assertIn(fragment,text)
        self.assertNotIn("setItem(",text)
    def test_each_adapter_excludes_browser_cells_from_physical_policy_queries(self):
        for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
            text=(ROOT/f'functional-tests/all-target-rollout/java-{mc}/com/salts_inventory_update/functionaltest/EverythingAuditQa.java').read_text()
            start=text.index('static boolean regressionVirtual(');end=text.index('static boolean regressionFake(',start)
            self.assertIn('eu.pb4.polymer.core.impl.ui.MicroUi$InternalScreenHandler',text[start:end])
    def test_input_discovery_checks_for_mutation_even_on_early_return(self):
        text=Path(__file__).with_name('observer-fragment.java.txt').read_text()
        start=text.index('if(kind.equals("regression-discover-input"))')
        end=text.index('if(kind.equals("regression-player-click"))',start)
        block=text[start:end]
        self.assertIn('finally{',block)
        self.assertEqual(2,block.count('regressionContentsFingerprint(active)'))
        self.assertIn('Read-only input discovery changed menu contents',block)

if __name__=='__main__':unittest.main()
