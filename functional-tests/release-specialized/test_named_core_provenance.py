"""Contracts for retaining the named observer's genuine source and binary."""
from pathlib import Path
import unittest
from toms_all_observer import private_named_init


class NamedCoreProvenance(unittest.TestCase):
    def source(self):
        return (Path(__file__).resolve().parents[2]/'functional-tests/project-everything-regression/core.init.gradle').read_text()

    def test_private_paths_and_actual_copy_hash_are_recorded(self):
        text=self.source();result=private_named_init(text,'fixture-01')
        self.assertIn("toms-observer/generated-metadata",result)
        self.assertIn("toms-observer/artifact",result)
        self.assertIn("inputs.add([file:observedQa,filename:observedQa.name,family:'qa'])",result)
        self.assertIn("digest(copiedQa.bytes)",result)
        self.assertEqual(text.count('mc.gameMode'),result.count('mc.gameMode'))
        self.assertLess(result.index('from(qaJar.get().archiveFile); into(mods)'),result.index('inputs.add([file:observedQa'))

    def test_changed_or_duplicate_anchors_and_repeat_are_rejected(self):
        text=self.source()
        for anchor in ["def generated = target.layout.buildDirectory.dir('chest-close-qa-generated').get().asFile",
                       "destinationDirectory.set(target.layout.buildDirectory.dir('chest-close-qa-artifact'))",
                       "} else root.copy { from(qaJar.get().archiveFile); into(mods) }"]:
            for changed in [text.replace(anchor,''),text+anchor]:
                with self.assertRaises(AssertionError): private_named_init(changed,'fixture-01')
        with self.assertRaises(AssertionError): private_named_init(private_named_init(text,'fixture-01'),'fixture-02')

    def test_escaping_run_is_rejected(self):
        with self.assertRaises(AssertionError): private_named_init(self.source(),'../escape')


if __name__=='__main__':unittest.main()
