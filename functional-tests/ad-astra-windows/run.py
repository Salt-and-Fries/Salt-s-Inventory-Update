"""Run only the changed Ad Astra and Giselle window checks."""
from pathlib import Path
import argparse
from focused import Check
import actions,management
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();c=Check(a.run);c.call('wait',ticks=2)
actions.main(c);c.configuration();management.main(c);c.geometry()
c.check('final-no-native-template-callback-failures','native template callback failed'not in c.log())
print('FOCUSED CHECKS COMPLETE',len(c.data['checks']),flush=True)
