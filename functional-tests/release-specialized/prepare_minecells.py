"""Compile the excluded production observer with Mine Cells observations."""
from pathlib import Path
import importlib.util, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from minecells_observer import enhance


def main():
    mc = sys.argv[sys.argv.index('--minecraft') + 1]
    spec = importlib.util.spec_from_file_location('minecells_current_prepare',
        ROOT / 'functional-tests/project-everything-regression/prepare_production.py')
    prep = importlib.util.module_from_spec(spec); spec.loader.exec_module(prep)
    original = prep.prep.qa_source
    def observed(side):
        path, text = original(side)
        return path, enhance(text, mc)
    prep.prep.qa_source = observed
    prep.main()


if __name__ == '__main__': main()
