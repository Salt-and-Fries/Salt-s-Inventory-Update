from pathlib import Path
import json,tempfile,types,unittest
from warp_plate_entry_fixture import TARGET,PUBLISHER,empty_hand_entry

class WarpPlateEntryFixtureTests(unittest.TestCase):
    def test_selects_empty_hand_without_replacing_or_retrying_native_use(self):
        calls=[]; inventory=['attuned_shard',None]
        def send(profile,kind,**kw):
            calls.append((profile,kind,kw))
            return dict(client=dict(selected=1 if kind=='regression-select-empty-hand'else 0,inventory=inventory))
        driver=types.SimpleNamespace(send=send)
        with tempfile.TemporaryDirectory()as folder:
            receipt=Path(folder)/'entry.jsonl'
            with empty_hand_entry(driver,TARGET,PUBLISHER,receipt):
                driver.send('profile','fixture',block='waystones:warp_plate',ticks=12)
                driver.send('profile','command',command='tp @s 6.5 65 8.5',ticks=6)
                result=driver.send('profile','client-use-block',ticks=20)
                driver.send('profile','fixture',block='minecraft:chest',ticks=12)
                driver.send('profile','client-use-block',ticks=20)
            self.assertIs(driver.send,send)
            self.assertEqual(['attuned_shard',None],inventory)
            self.assertEqual(['fixture','command','regression-select-empty-hand','client-use-block','fixture','client-use-block'],[v[1]for v in calls])
            self.assertEqual(('profile','client-use-block',dict(ticks=20)),calls[3])
            rows=[json.loads(v)for v in receipt.read_text().splitlines()]
            self.assertEqual(1,len(rows));self.assertEqual(1,rows[0]['selection']['client']['selected'])
            self.assertEqual(dict(type='client-use-block',ticks=20),rows[0]['next_original_command'])
            self.assertEqual(inventory,result['client']['inventory'])

    def test_selection_failure_preserves_original_exception_and_does_not_click(self):
        calls=[];failure=RuntimeError('no empty hotbar')
        def send(profile,kind,**kw):
            calls.append(kind)
            if kind=='regression-select-empty-hand':raise failure
            return {}
        driver=types.SimpleNamespace(send=send)
        with tempfile.TemporaryDirectory()as folder:
            receipt=Path(folder)/'entry.jsonl'
            with self.assertRaises(RuntimeError)as caught:
                with empty_hand_entry(driver,TARGET,PUBLISHER,receipt):
                    driver.send('p','fixture',block='waystones:warp_plate')
                    driver.send('p','client-use-block',ticks=20)
            self.assertIs(caught.exception,failure);self.assertIs(driver.send,send)
            self.assertFalse(receipt.exists());self.assertNotIn('client-use-block',calls)

    def test_exact_pin_and_target_are_required(self):
        driver=types.SimpleNamespace(send=lambda *a,**kw:None)
        for target,pin in [('26.3/fabric',PUBLISHER),(TARGET,'0'*64)]:
            with self.subTest(target=target,pin=pin),self.assertRaises(ValueError):
                with empty_hand_entry(driver,target,pin,'unused.jsonl'):pass

if __name__=='__main__':unittest.main()
