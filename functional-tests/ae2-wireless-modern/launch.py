"""Start one focused shipping-JAR observer, not the complete gameplay suite.

EverythingAuditQa prunes the harness to world creation, handshake and fixture;
all affected-menu interactions are explicitly driven by run-focused.py.
"""
from pathlib import Path
import subprocess, os, sys

ROOT = Path(__file__).resolve().parents[2]
version = sys.argv[1]
run = sys.argv[2] if len(sys.argv) > 2 else 'ae2-wireless-port-02'
assert version in ['1.21.1', '26.1.2', '26.2', '26.3']
out = ROOT / 'build/ae2-wireless-all-targets'
args = ['cmd', '/c', str(ROOT / 'gradlew.bat'),
        f':{version}:neoforge:runNativePortQa', '-I',
        'functional-tests/all-target-rollout/qa.init.gradle',
        f'-PqaMinecraft={version}', '-PqaLoader=neoforge',
        '-PqaFullGameplay=true', '-PqaIntegrationOnly=true',
        '-PqaScenario=extension-empty', f'-PqaRunId={run}',
        f'-PcompatibilityQaManifest=build/all-target-rollout/fixtures/{version.replace(".", "")}-neoforge-ae2-wireless-port/fixture.json',
        '-PqaRuntimeRoot=C:/Users/texca/.cache/siu-rollout-runtime', '--console=plain']
with (out / f'{version}-{run}.log').open('w') as log:
    process = subprocess.Popen(args, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                               env={**os.environ, 'JAVA_HOME': 'C:/Program Files/Java/jdk-26'},
                               creationflags=subprocess.CREATE_NO_WINDOW)
print(version, process.pid, run)
