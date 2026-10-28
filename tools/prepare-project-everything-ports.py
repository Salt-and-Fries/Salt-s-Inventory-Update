"""Prepare the second Project Everything batch for a selected post-1.20.1 target.

Only exact upstream Minecraft/loader declarations qualify. Downloads and transitive
dependencies are hash checked by the common fixture preparer; no upstream JAR is edited.
"""
from pathlib import Path
import argparse
import importlib.util
import json
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
PROJECTS = ["bartering-station", "alloy-forgery", "fabric-seasons-extras",
            "create-alloyed", "farmers-delight", "farmers-delight-refabricated",
            "miners-delight", "better-archeology", "illager-invasion", "galosphere",
            "universal-sawmill"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("--loader", required=True, choices=["fabric", "neoforge"])
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location("compatibility_qa_fixtures", ROOT / "tools/prepare-compatibility-qa.py")
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    original = helper.get_json
    memory = {}

    def releases(endpoint):
        # A fresh project listing avoids an old metadata cache hiding new ports.
        if endpoint.endswith("/version"):
            if endpoint not in memory:
                with urllib.request.urlopen(urllib.request.Request(
                        "https://api.modrinth.com/v2/" + endpoint, headers=helper.HEADERS), timeout=60) as response:
                    memory[endpoint] = sorted(json.load(response), key=lambda v: v["date_published"], reverse=True)
            return memory[endpoint]
        return original(endpoint)

    helper.get_json = releases
    selected = {}
    for project in PROJECTS:
        version = next((v for v in releases("project/" + project + "/version")
                        if args.minecraft in v["game_versions"] and args.loader in v["loaders"]
                        and v.get("status", "listed") == "listed"), None)
        if version:
            selected[project] = version
    helper.prepare(args.minecraft, args.loader, "extension-empty", args.output.resolve(), selected)
    report = json.loads(args.output.read_text(encoding="utf-8"))
    if args.loader == "fabric":
        # Recent upstream libraries can raise the API requirement independently
        # of the older general coverage fixture. Keep this isolated QA runtime current.
        api = next(v for v in releases("project/fabric-api/version")
                   if args.minecraft in v["game_versions"] and "fabric" in v["loaders"])
        report["runtime"]["fabric_api"] = api["version_number"]
        with urllib.request.urlopen("https://meta.fabricmc.net/v2/versions/loader", timeout=60) as response:
            report["runtime"]["fabric_loader"] = next(v["version"] for v in json.load(response) if v["stable"])
    report["families"] = {p: {"version_id": v["id"], "version": v["version_number"],
                               "project_id": v["project_id"]} for p, v in selected.items()}
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
