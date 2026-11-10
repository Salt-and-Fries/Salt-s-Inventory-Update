"""Wrap the unchanged production QA preparer with read-only feature fields."""
from pathlib import Path
import importlib.util,sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from observer_features import enhance

spec=importlib.util.spec_from_file_location('specialized_production_prepare',ROOT/'functional-tests/project-everything-regression/prepare_production.py')
prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
def main():
    # Capture before the Forge launcher rewrites argv to its native arguments.
    mc=sys.argv[sys.argv.index('--minecraft')+1]
    original=prep.prep.qa_source
    def observed(side):
        path,text=original(side)
        return path,enhance(text,mc)
    prep.prep.qa_source=observed
    prep.main()

if __name__=='__main__':main()
