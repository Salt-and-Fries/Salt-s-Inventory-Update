import unittest
from native_entries import settings_transition

class SettingsTransitionTests(unittest.TestCase):
    def test_traveler_uses_its_real_custom_settings_widget(self):
        self.assertEqual('com.tiviacz.travelersbackpack.client.screens.widgets.SettingsWidget',settings_transition(
            'com.tiviacz.travelersbackpack.client.screens.BackpackScreen','com.tiviacz.travelersbackpack.client.screens.BackpackSettingsScreen'))
    def test_sophisticated_native_settings_tab_has_a_narrow_family_guard(self):
        self.assertEqual('net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab',settings_transition(
            'net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen','net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen'))
        self.assertIsNone(settings_transition('another.StorageScreen','net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen'))
        self.assertIsNone(settings_transition('net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen','net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen'))
    def test_unrelated_clicks_and_existing_settings_screens_are_not_invented_transitions(self):
        self.assertIsNone(settings_transition('native.Screen','native.Child'))
        self.assertIsNone(settings_transition('net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen','net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen'))
