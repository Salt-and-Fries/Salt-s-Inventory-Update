"""Compile a private raw-loader observer with the native Tom feature readouts."""
from pathlib import Path
import importlib.util, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from toms_all_observer import inject


def main():
    mc = sys.argv[sys.argv.index('--minecraft') + 1]
    spec = importlib.util.spec_from_file_location('toms_all_raw_prepare', ROOT / 'functional-tests/project-everything-regression/prepare_production.py')
    prep = importlib.util.module_from_spec(spec); spec.loader.exec_module(prep)
    original = prep.patch
    prep.patch = lambda text: inject(original(text), mc)
    prep.main()


if __name__ == '__main__': main()
