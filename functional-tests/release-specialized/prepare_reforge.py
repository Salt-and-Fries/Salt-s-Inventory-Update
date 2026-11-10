"""Compile a private read-only raw observer for the native reforge menus."""
from pathlib import Path
import importlib.util,sys
from reforge_observer import enhance

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))


def main():
    mc=sys.argv[sys.argv.index('--minecraft')+1]
    spec=importlib.util.spec_from_file_location('reforge_raw_prepare',ROOT/'functional-tests/project-everything-regression/prepare_production.py')
    prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep);original=prep.patch
    prep.patch=lambda text:enhance(original(text),mc);prep.main()


if __name__=='__main__':main()
