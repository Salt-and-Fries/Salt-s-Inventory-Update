"""Stage native provider opening data fixes without changing API or other classes."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA

for suffix,entry in [('bridge','com/salts_inventory_update/internal/desktop/AutomaticContainerSupport.class'),
                     ('sessions','com/salts_inventory_update/server/DesktopContainerSessions.class')]:
    baseline=OUT/('release-before-provider-'+suffix+'.json')
    report='staged-provider-'+suffix+'-fix.json'
    assert not (OUT/report).exists(),'Already staged; preserve evidence'
    assert not baseline.exists(),'Existing staging baseline requires inspection'
    baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
        for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
    changes=[('core',mc,loader,entry) for mc in ['1.21.1','1.21.11','26.1.2','26.2','26.3'] for loader in ['Fabric','NeoForge']]
    main(changes,baseline.name,report,'release-before-provider-'+suffix)
