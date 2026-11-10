"""Native ownership must reject desktop conversion and hidden inventory edits."""
import copy,unittest
from native_only_features import field,owns_native
from mowzie_features import stack


class NativeOnlyContracts(unittest.TestCase):
    def state(self):
        item='{id:"minecraft:diamond",Count:7b,tag:{release_native_sentinel:71,typed:[B;2b,8b]}}'
        d=dict(client=dict(screen='publisher.NativeScreen',desktopActive=False,fields={'publisher.NativeScreen#pageIndex':0}),
            sessions=[],inventory=[dict(index=i,item=item if i==12 else '{}') for i in range(41)],cursor='{}',nativeCursor='{}')
        return d,stack(item)

    def test_actual_native_owner_with_complete_foreign_data_is_accepted(self):
        d,expected=self.state();self.assertTrue(owns_native(d,'publisher.NativeScreen',expected))

    def test_desktop_wrong_screen_or_hidden_source_is_rejected(self):
        d,expected=self.state()
        for patch in [dict(screen='com.salts.InventoryDesktopScreen'),dict(desktopActive=True),dict(desktopActive=0)]:
            other=copy.deepcopy(d);other['client'].update(patch);self.assertFalse(owns_native(other,'publisher.NativeScreen',expected))
        for sources in [['native.hosted.Menu'],None]:
            other=copy.deepcopy(d);other['sessions']=sources;self.assertFalse(owns_native(other,'publisher.NativeScreen',expected))

    def test_foreign_loss_typed_changes_and_hidden_cursor_are_rejected(self):
        d,expected=self.state()
        for value in ['{}',d['inventory'][12]['item'].replace('[B;2b,8b]','[2,8]'),d['inventory'][12]['item'].replace('7b','6b')]:
            other=copy.deepcopy(d);other['inventory'][12]['item']=value;self.assertFalse(owns_native(other,'publisher.NativeScreen',expected))
        for key in ['cursor','nativeCursor']:
            other=copy.deepcopy(d);other[key]=d['inventory'][12]['item'];self.assertFalse(owns_native(other,'publisher.NativeScreen',expected))
            other[key]='[]'
            with self.assertRaises(AssertionError):owns_native(other,'publisher.NativeScreen',expected)

    def test_missing_or_ambiguous_native_page_observation_is_rejected(self):
        d,_=self.state();self.assertEqual(field(d,'pageIndex'),0)
        with self.assertRaises(AssertionError):field(d,'unobserved')
        d['client']['fields']['publisher.ParentScreen#pageIndex']=0
        with self.assertRaises(AssertionError):field(d,'pageIndex')


if __name__=='__main__':unittest.main()
