"""Version-qualified registry entry corrections, disclosed in each case record."""
import copy


def adapt(case, minecraft, loader, blocks):
    actions=copy.deepcopy(case['actions'])
    correction=None
    if (minecraft,loader)==('26.1.2','fabric') and case['id']=='atlas-waystones-modifiers' \
            and case.get('expected_native_menu','').endswith('.WaystoneModifierMenu') \
            and 'waystones:waystone' not in blocks and 'waystones:andesite_waystone' in blocks:
        for action in actions:
            if action.get('type')=='fixture' and action.get('block')=='waystones:waystone':
                action['block']='waystones:andesite_waystone'
                correction={'scope':'Exact 26.1.2 Fabric modifier-entry workflow only',
                    'previous_block':'waystones:waystone','registered_block':'waystones:andesite_waystone',
                    'verification':'Normal placement and native modifier-button entry must still reach the expected publisher menu; no provider/screen is constructed.'}
    return actions,correction
