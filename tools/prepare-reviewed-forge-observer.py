"""Generate the same non-invasive observer for Forge's named development launcher."""
import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('production_audit', ROOT/'tools/prepare-production-fabric-audit.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)
out = ROOT/'build/project-everything-56-implementation/forge-observer/com/siu_reference/NativeUiReference.java'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(audit.qa_source('integrated'), encoding='utf-8')
print(out)
