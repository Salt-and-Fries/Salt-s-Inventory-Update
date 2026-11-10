"""Retain separate jobs for the older exact Fabric publisher releases."""
import sys
from run_accessories_equipment import main

if __name__=='__main__':
    assert '--older-release' not in sys.argv
    sys.argv.append('--older-release');main()
