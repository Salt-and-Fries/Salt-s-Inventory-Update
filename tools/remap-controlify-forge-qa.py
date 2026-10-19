"""Run ModDev's genuine SRG_TO_MCP tool on original packaged Forge QA inputs.

Only QA copies are produced. Published dependencies and final Salt artifacts are
preserved, hashed, and linked to their named-runtime derivatives in the evidence.
See neoforged/ModDevGradle LEGACY.md and tasks/RemapOperation.java.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import subprocess
import zipfile


def digest(path, algorithm="sha256"):
    return hashlib.new(algorithm, Path(path).read_bytes()).hexdigest()


def verify_resources(source, mapped, label=None):
    """Namespace remapping must preserve actual mod resources and bundled libraries."""
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(mapped) as output:
        label = label or str(source)
        expected = set(original.namelist())
        found = set(output.namelist())
        signatures = {name for name in expected if name.upper().startswith("META-INF/")
                      and name.upper().endswith((".SF", ".RSA", ".DSA", ".EC"))}
        assert expected - signatures == found, f"Remapping changed JAR entries: {label}"
        for name in expected - signatures:
            if name.endswith(".class") or name == "META-INF/MANIFEST.MF":
                continue
            if name.endswith(".jar"):
                # ModDev's official tool also remaps JarJar children and strips their signatures.
                verify_resources(io.BytesIO(original.read(name)), io.BytesIO(output.read(name)), label + "!" + name)
            else:
                assert original.read(name) == output.read(name), f"Remapping changed mod resource: {label}!{name}"


def remap(java, tool, mappings, sources, output, evidence):
    sources = [path.resolve(strict=True) for path in sources]
    output = output.resolve()
    assert len(sources) == len({path.name for path in sources}), "Input JAR filenames must be unique"
    assert all(path.suffix == ".jar" and path != output / path.name for path in sources), "Preserve original JARs separately"
    output.mkdir(parents=True, exist_ok=True)
    artifacts = []
    for source in sources:
        before = digest(source)
        mapped = output / source.name
        subprocess.run([str(java), "-jar", str(tool), "--input", str(source), "--output", str(mapped),
                        "--task", "SRG_TO_MCP", "--mcp", str(mappings), "--strip-signatures"], check=True)
        assert digest(source) == before, f"Original artifact changed: {source}"
        verify_resources(source, mapped)
        artifacts.append({
            "original": str(source), "original_sha256": before, "original_sha512": digest(source, "sha512"),
            "mapped": str(mapped), "mapped_sha256": digest(mapped), "mapped_sha512": digest(mapped, "sha512"),
        })
    result = {"schema_version": 1, "operation": "SRG_TO_MCP", "installer_tools": str(tool),
              "installer_tools_sha256": digest(tool), "mappings": str(mappings), "mappings_sha256": digest(mappings),
              "artifacts": artifacts}
    evidence.parent.mkdir(parents=True, exist_ok=True)
    evidence.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Preserved and remapped {len(artifacts)} original Forge JARs; lineage: {evidence}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--installer-tools", type=Path, required=True)
    parser.add_argument("--mappings", type=Path, required=True, help="ModDev intermediateToNamed.zip")
    parser.add_argument("--jar", type=Path, action="append", required=True)
    parser.add_argument("--output-directory", type=Path, required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    remap(args.java.resolve(strict=True), args.installer_tools.resolve(strict=True), args.mappings.resolve(strict=True),
          args.jar, args.output_directory, args.evidence.resolve())
