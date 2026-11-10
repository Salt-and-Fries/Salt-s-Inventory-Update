"""Additional focused acceptance for the controller workspace's REI policy."""
import re

TARGETS=('26.3/fabric','26.3/neoforge')
WARNING='com.salts_inventory_update.controlify.RadialInventoryScreen did not render background and foreground'
REQUIRED={
    'compat-browser-real-provider','compat-browser-correct-provider','compat-browser-tabs-selectable',
    'compat-browser-tab-native-selection','compat-browser-custom-keyboard-edits-native-filter',
    'compat-browser-search-native-filter','compat-browser-virtual-item-navigable',
    'compat-browser-real-recipe-view','compat-browser-recipe-ingredient-navigable',
    'compat-browser-virtual-recipe-retains-inventory','compat-browser-recipe-geometry-uses-native-hit-test',
    'compat-browser-controls-inside-presentation-bounds','compat-browser-fitted-panes-bounded',
    'compat-browser-fitted-panes-separate','compat-browser-adjacent-ingredients-do-not-skip',
    'compat-browser-native-recipes-ready','compat-browser-native-render-state',
    *{f'compat-browser-{kind}-{check}'for kind in ['entry','recipe']for check in [
        'native-items-extracted','retains-window-transform','retains-window-clip']},
}


def verify_observation(report,launch_log,rei_config):
    assert report['scope']=='mod-compat' and type(report['failures'])is int and report['failures']==0
    rows=report['results'];assert rows and not any(v['status']in {'FAIL','FAILED','ERROR'}for v in rows)
    passed={v['name']for v in rows if v['status']=='PASS'}
    assert REQUIRED<=passed,sorted(REQUIRED-passed)
    assert WARNING not in launch_log,'The repeated native-overlay lifecycle warning is still present'
    assert 'RadialInventoryScreen' in launch_log,'No actual controller workspace was observed'
    flags=re.findall(r'["\']?overlayVisible["\']?\s*:\s*(true|false)',rei_config)
    assert flags==['true'],'Disabling the global REI overlay is not this fix'
    captures={v['name']for v in rows if v['status']=='CAPTURED'}
    assert {'compat-browser-controls','compat-browser-recipe-virtual-slots'}<=captures
    return dict(scope='Original mod-compat browser and controller checks, plus enabled global REI and no repeated workspace lifecycle warning',
        required_browser_checks=len(REQUIRED),passed_rows=sum(v['status']=='PASS'for v in rows),
        global_REI_overlay_enabled=True,repeated_workspace_warning_count=0)
