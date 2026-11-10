"""Replay the unchanged four native status workflows on reviewed 5.8.15.

This selects the existing cohort; it does not replace publisher bytes, reorder
fixture indices, modify native controls or retry an original action.
"""
import sys
from run_techreborn_status import main

if __name__=='__main__':
    sys.argv+=['--fixture-name','regression-techreborn-current-1201-fabric']
    main()
