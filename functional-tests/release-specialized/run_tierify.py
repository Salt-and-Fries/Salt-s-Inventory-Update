"""Run the same native reforge contract for the exact Tierify publisher."""
import sys
from run_reforge import main

if __name__=='__main__':
    assert '--publisher-kind'not in sys.argv
    sys.argv += ['--publisher-kind','tierify']
    main()
