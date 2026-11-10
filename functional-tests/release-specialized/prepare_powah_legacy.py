"""Compile an excluded read-only Powah5 observer on the original raw backend."""
from pathlib import Path
import importlib.util,sys
from powah_legacy_observer import enhance
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))


def main():
    mc=sys.argv[sys.argv.index('--minecraft')+1];assert mc=='1.20.1'
    path=ROOT/'functional-tests/project-everything-regression/prepare_production.py'
    spec=importlib.util.spec_from_file_location('powah_legacy_raw_prepare',path);prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
    original=prep.patch;prep.patch=lambda text:enhance(original(text),mc);prep.main()


if __name__=='__main__':main()
