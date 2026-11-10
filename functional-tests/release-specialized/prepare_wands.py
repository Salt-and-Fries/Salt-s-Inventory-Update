"""Compile a private raw-loader observer for native wand tool selection."""
from pathlib import Path
import importlib.util, sys
from wands_observer import enhance

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'functional-tests/project-everything-regression'))


def main():
    mc = sys.argv[sys.argv.index('--minecraft')+1]
    spec = importlib.util.spec_from_file_location('wands_raw_prepare', ROOT/'functional-tests/project-everything-regression/prepare_production.py')
    prep = importlib.util.module_from_spec(spec); spec.loader.exec_module(prep)
    original = prep.patch
    prep.patch = lambda text: enhance(original(text), mc)
    prep.main()


if __name__ == '__main__': main()
