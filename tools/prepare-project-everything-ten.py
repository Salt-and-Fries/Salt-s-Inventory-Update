"""Pin hash-verified 1.20.1 upstream fixtures for the second Project Everything batch."""
from pathlib import Path
import argparse
import importlib.util
import json

ROOT = Path(__file__).resolve().parents[1]
FAMILIES = {
    "bartering-station": {"fabric": "bartering-station", "forge": "bartering-station"},
    "alloy-forgery": {"fabric": "alloy-forgery"},
    "fabric-seasons-extras": {"fabric": "fabric-seasons-extras"},
    "alloyed": {"fabric": "create-alloyed", "forge": "create-alloyed"},
    "farmers-delight": {"fabric": "farmers-delight-refabricated", "forge": "farmers-delight"},
    "miners-delight": {"forge": "miners-delight"},
    "better-archeology": {"fabric": "better-archeology", "forge": "better-archeology"},
    "illager-invasion": {"fabric": "illager-invasion", "forge": "illager-invasion"},
    "galosphere": {"fabric": "galosphere", "forge": "galosphere"},
    "sawmill": {"fabric": "universal-sawmill", "forge": "universal-sawmill"},
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", required=True, choices=["fabric", "forge"])
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--fixture-set", choices=["all", "without-alloyed", "without-galosphere"], default="all",
                        help="Isolate Create's obsolete Galosphere silver recipes on Forge without modifying upstream JARs")
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location("compatibility_qa_fixtures", ROOT / "tools/prepare-compatibility-qa.py")
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    # Modrinth orders by publication date, not semantic version. A recently
    # republished 8.0.15 is older than Illager Invasion's required 8.0.19.
    original_get_json = helper.get_json
    def get_json(endpoint):
        result = original_get_json(endpoint)
        if isinstance(result, dict) and result.get("project_id") == "QAGBst4M":
            # Some upstream dependency records still point at the old exact
            # Puzzles Lib release even though another target requires >=8.0.19.
            result = original_get_json("version/" + {"fabric": "QSW0oJPl", "forge": "hOfDHA4g"}[args.loader])
        if endpoint == "project/QAGBst4M/version":
            import re
            def release_key(release):
                match = re.search(r"(?:^|v)(8)\.(\d+)\.(\d+)", release["version_number"])
                return tuple(map(int, match.groups())) if match else (0, 0, 0)
            result = sorted(result, key=release_key, reverse=True)
        return result
    helper.get_json = get_json
    projects = [loaders[args.loader] for loaders in FAMILIES.values() if args.loader in loaders]
    if args.fixture_set == "without-alloyed":
        projects.remove(FAMILIES["alloyed"][args.loader])
    elif args.fixture_set == "without-galosphere":
        projects.remove(FAMILIES["galosphere"][args.loader])
    helper.prepare("1.20.1", args.loader, "extension-empty", args.output.resolve(), projects)
    report = json.loads(args.output.read_text())
    report["families"] = {name: loaders.get(args.loader) for name, loaders in FAMILIES.items()}
    report["fixture_set"] = args.fixture_set
    args.output.write_text(json.dumps(report, indent=2) + "\n")


if __name__ == "__main__":
    main()
