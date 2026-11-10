"""Private raw-loader observations for real MI pump/tank prerequisites."""
from pathlib import Path
import importlib.util,sys
from mi_multiblock_observer import enhance
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))


def main():
    mc=sys.argv[sys.argv.index('--minecraft')+1]
    spec=importlib.util.spec_from_file_location('mi_infra_raw_prepare',ROOT/'functional-tests/project-everything-regression/prepare_production.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    original=module.patch;module.patch=lambda text:enhance(original(text),mc)
    module.main()


if __name__=='__main__':main()
