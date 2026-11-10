"""Private raw-loader adapter for original MI energy observations."""
from pathlib import Path
import importlib.util,sys
from mi_energy_observer import enhance
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))


def main():
    mc=sys.argv[sys.argv.index('--minecraft')+1]
    spec=importlib.util.spec_from_file_location('mi_energy_raw_prepare',ROOT/'functional-tests/project-everything-regression/prepare_production.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    original=module.patch;module.patch=lambda text:enhance(original(text),mc)
    module.main()


if __name__=='__main__':main()
