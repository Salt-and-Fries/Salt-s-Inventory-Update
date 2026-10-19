"""Append current validated compatibility evidence without changing the original 14 guide pages."""
import argparse
import contextlib
from datetime import datetime, timezone
import importlib.util
import io
import json
from pathlib import Path
import sys
from xml.sax.saxutils import escape

from pypdf import PdfReader, PdfWriter
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph, Table, TableStyle

ROOT = Path(__file__).resolve().parents[1]
WIDTH, HEIGHT, MARGIN = 612, 792, 42
CONTENT = WIDTH - 2 * MARGIN
TEAL = colors.HexColor("#117f82")
INK = colors.HexColor("#152b3d")
MUTED = colors.HexColor("#56696c")
PAPER = colors.HexColor("#faf9f5")
LINE = colors.HexColor("#d8e2df")
LOADERS = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


def load_tool(name):
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), ROOT / "tools" / name)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def register_fonts():
    font_dir = Path("C:/Windows/Fonts")
    faces = {"Guide": "calibri.ttf", "GuideBold": "calibrib.ttf", "GuideLight": "calibril.ttf"}
    for name, filename in faces.items():
        path = font_dir / filename
        if not path.is_file():
            raise FileNotFoundError(f"The original guide's matching Calibri font is absent: {path}")
        pdfmetrics.registerFont(TTFont(name, str(path)))
    pdfmetrics.registerFontFamily("Guide", normal="Guide", bold="GuideBold", italic="Guide", boldItalic="GuideBold")


def style(size=10, leading=14, color=INK, bold=False):
    return ParagraphStyle("Guide", fontName="GuideBold" if bold else "Guide", fontSize=size,
                          leading=leading, textColor=color, alignment=TA_LEFT, spaceAfter=0)


def paragraph(pdf, text, x, top, width=CONTENT, size=10, leading=14, color=INK, bold=False):
    item = Paragraph(text, style(size, leading, color, bold))
    _, height = item.wrap(width, HEIGHT)
    item.drawOn(pdf, x, top - height)
    return top - height


def rule(pdf, y):
    pdf.setStrokeColor(LINE)
    pdf.setLineWidth(0.6)
    pdf.line(MARGIN, y, WIDTH - MARGIN, y)


def page_start(pdf, title, subtitle, page_number, final_pages, section="COMPATIBILITY APPENDIX"):
    pdf.setFillColor(PAPER)
    pdf.rect(0, 0, WIDTH, HEIGHT, fill=1, stroke=0)
    pdf.setFillColor(TEAL)
    pdf.rect(MARGIN, 747, 26, 3, fill=1, stroke=0)
    pdf.setFont("GuideBold", 9.1)
    pdf.drawString(78, 745, "SALT'S INVENTORY UPDATE  /  CONTROLIFY")
    pdf.setFillColor(INK)
    pdf.setFont("GuideLight", 27)
    pdf.drawString(MARGIN, 716, title)
    paragraph(pdf, escape(subtitle), MARGIN, 698, size=10.2, leading=13, color=MUTED)
    pdf.setFillColor(TEAL)
    pdf.setFont("GuideBold", 8.3)
    pdf.drawString(MARGIN, 654, section)
    pdf.drawRightString(WIDTH - MARGIN, 654, f"{page_number:02d} / {final_pages:02d}")
    rule(pdf, 643)
    rule(pdf, 46)
    pdf.setFillColor(MUTED)
    pdf.setFont("Guide", 8)
    pdf.drawString(MARGIN, 31, "Current compatibility and packaged runtime evidence")
    pdf.drawRightString(WIDTH - MARGIN, 31, "Original feature guide retained on pages 1-14")


def dependency_link(pin, role):
    project = pin["project"] if role == "controller" else "1eAoo2KR"
    url = f"https://modrinth.com/mod/{project}/version/{pin['version_id']}"
    label = pin["version"]
    if role == "controller" and project == "G2K5sLls":
        label = pin["version"].split("-mc", 1)[0] + " (Forge port)"
    return f'<link href="{url}" color="#117f82">{escape(label)}</link>'


def runtime_dependency_links(source, targets):
    dependencies = []
    for target in targets:
        for pin in target.get("runtime_dependencies", []):
            name = source["runtime_dependency_labels"].get(pin["mod_id"], pin["mod_id"])
            label = f"{name} {pin['version']}"
            dependencies.append((target["minecraft"], LOADERS[target["loader"]],
                                 f'<link href="{escape(pin["source"])}" color="#117f82">{escape(label)}</link>'))
    return dependencies


def validate_runs(targets, args, runtime):
    runs = {}
    for target in targets:
        key = target["minecraft"], target["loader"]
        report = args.results_root / f"{key[0]}-{key[1]}" / "results.json"
        row = {"status": "Pending"}
        if report.is_file():
            try:
                with contextlib.redirect_stdout(io.StringIO()):
                    row.update(runtime.validate(target, args.results_root, args.artifacts, args.run_profile))
                row["status"] = "Verified"
            except (AssertionError, OSError, KeyError, TypeError, ValueError) as failure:
                row["status"], row["details"] = "Needs rerun", str(failure)
        runs[key] = row
    return runs


def select_captures(source, targets, runs, args):
    selected = []
    for choice in source["modern_evidence"]:
        candidates = [target for target in targets if target["minecraft"] == choice["minecraft"]]
        candidates.sort(key=lambda target: target["loader"] != choice["preferred_loader"])
        for target in candidates:
            key = target["minecraft"], target["loader"]
            if runs[key]["status"] != "Verified":
                continue
            report_folder = args.results_root / f"{key[0]}-{key[1]}"
            report = json.loads((report_folder / "results.json").read_text(encoding="utf-8-sig"))
            passed = {row["name"] for row in report["results"] if row["status"] == "PASS"}
            captures = {row["name"] for row in report["results"] if row["status"] == "CAPTURED"}
            if choice["required_pass"] not in passed or choice["capture"] not in captures:
                continue
            selected.append({**choice, "loader": key[1], "image": report_folder / "screenshots" / (choice["capture"] + ".png"),
                             "report": report_folder / "results.json", "runtime": runs[key]})
            break
    return selected


def matrix_page(pdf, source, targets, runs, generated, total_pages):
    page_start(pdf, source["title"], source["subtitle"], 15, total_pages)
    top = paragraph(pdf, escape(source["historical_note"]), MARGIN, 629, size=10, leading=13.5)
    count = sum(row["status"] == "Verified" for row in runs.values())
    top = paragraph(pdf, f"<b>{count}/12 packaged runtimes verified.</b> "
                    + ("All twelve reports match the final collected Core and companion JARs." if count == 12 else
                       "This is a preflight copy. Pending targets have not completed the required runtime evidence."),
                    MARGIN, top - 11, size=10.2, leading=14)
    headers = ["Minecraft", "Loader", "Native controller", "YACL", "Runtime"]
    rows = [[Paragraph(text, style(8.3, 10.5, colors.white, True)) for text in headers]]
    for target in targets:
        key = target["minecraft"], target["loader"]
        entries = [target["minecraft"], LOADERS[target["loader"]], dependency_link(target["controller"], "controller"),
                   dependency_link(target["yacl"], "yacl"), runs[key]["status"]]
        rows.append([Paragraph(text, style(8.1, 10.2)) for text in entries])
    table = Table(rows, colWidths=[52, 62, 120, 182, 112], rowHeights=[27] + [27] * len(targets))
    table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), TEAL),
                               ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#edf4f1")]),
                               ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                               ("LEFTPADDING", (0, 0), (-1, -1), 7),
                               ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                               ("LINEBELOW", (0, 0), (-1, 0), 0.5, TEAL),
                               ("LINEBELOW", (0, 1), (-1, -1), 0.3, LINE)]))
    _, table_height = table.wrap(CONTENT, HEIGHT)
    table_bottom = top - 13 - table_height
    table.drawOn(pdf, MARGIN, table_bottom)
    extra = " ".join(f"{game} {loader} also requires {pin} as a separate client mod."
                     for game, loader, pin in runtime_dependency_links(source, targets))
    top = paragraph(pdf, "Controller and YACL labels link to the exact releases. " + extra + " "
                    "The 26.x upstream controller JARs have native Fabric/NeoForge entrypoints; each Salt companion is loader-specific.",
                    MARGIN, table_bottom - 14, size=9.5, leading=12.5)
    top = paragraph(pdf, "Pin source: controlify/targets.json, including extra runtime dependencies and SHA-512 digests. "
                    "Snapshot: " + escape(generated) + ".",
                    MARGIN, top - 10, size=8.7, leading=11.5, color=MUTED)
    assert top >= 63, "Compatibility page content exceeds its safe layout area"
    pdf.showPage()


def installation_page(pdf, source, targets, runs, suites, generated, total_pages):
    page_start(pdf, "Install matching native files", "The third JAR and its matching rebuilt Core remain separate mods.", 16, total_pages)
    top = 626
    blocks = [
        ("Choose one Minecraft version and loader", "Install its rebuilt Salt Core and Controlify companion from "
         "dist/controlify-test/core/ and dist/controlify-test/controlify/. Their Salt version numbers must match. "
         "An older published Core archive with the same 0.1.4 label may lack these workspace hooks."),
        ("Add the exact native dependencies", "Install the controller and YACL releases linked on page 15. "
         "Fabric also needs matching Fabric API. Forge uses Controlify: Forgified, Forge YACL and the separate original Mixin Args 1.0 JAR. "
         "NeoForge uses its native dependency builds. Extended compatibility remains optional."),
        ("Match Java and Fabric Loader", source["java_requirements"] + " " + source["fabric_loader_requirements"]
         + " The 26.x minimum provides the MixinExtras compatibility fix needed by the unchanged upstream mods."),
    ]
    for title, body in blocks:
        top = paragraph(pdf, escape(title), MARGIN, top, size=12, leading=15, bold=True)
        top = paragraph(pdf, escape(body), MARGIN, top - 5, size=10.1, leading=14)
        top -= 15
    top = paragraph(pdf, "<b>Extra runtime dependency:</b> " + " ".join(
                    f"{game} {loader}: {pin}. Install this unchanged upstream JAR separately; it is not bundled in the Salt companion."
                    for game, loader, pin in runtime_dependency_links(source, targets)),
                    MARGIN, top, size=9.4, leading=12.5)
    top = paragraph(pdf, f'<link href="{escape(source["fabric_loader_source"])}" color="#117f82">Fabric Loader 0.19.5 release notes</link>. '
                    "The original controls and configuration on pages 1-14 apply across the matrix.",
                    MARGIN, top - 10, size=9.4, leading=12.5)
    top = paragraph(pdf, "<b>Existing 1.20.1 preview limit:</b> " + escape(source["legacy_loom_note"]),
                    MARGIN, top - 10, size=9.4, leading=12.5)
    top -= 15
    rule(pdf, top + 3)
    top = paragraph(pdf, "What the current evidence establishes", MARGIN, top - 12, size=13, leading=17, bold=True)
    total = sum(row["tests"] for row in suites)
    failures = sum(row["failures"] + row["errors"] for row in suites)
    text = f"The current seven JUnit report directories contain <b>{total} tests</b> and {failures} failures/errors. "
    text += "Core is tested once per Minecraft version; each loader additionally needs its own packaged runtime report. "
    count = sum(row["status"] == "Verified" for row in runs.values())
    text += f"<b>{count}/12 packaged targets are verified</b> in this snapshot."
    top = paragraph(pdf, text, MARGIN, top - 7, size=10.1, leading=14)
    top = paragraph(pdf, "Runtime verification requires real controller bindings and screen processing, an integrated server, "
                    "all required scenario assertions, native PNG captures, original pinned dependency hashes, and final "
                    "Core/companion JAR hashes matching the collected installation bundle. "
                    "The driver is synthetic; physical controller hardware coverage is not claimed.",
                    MARGIN, top - 12, size=10, leading=13.5)
    paragraph(pdf, "Current sources: controlify/VALIDATION.md; tools/verify-controlify-runtime.py; "
              "each target's results.json and screenshots. Snapshot: " + escape(generated) + ".",
              MARGIN, top - 13, size=8.8, leading=11.5, color=MUTED)
    assert top >= 90, "Installation page content exceeds its safe layout area"
    pdf.showPage()


def evidence_page(pdf, selection, number, total_pages):
    game, loader = selection["minecraft"], LOADERS[selection["loader"]]
    page_start(pdf, selection["title"], f"Verified native capture: Minecraft {game}, {loader}", number, total_pages,
               "PACKAGED MODERN RUNTIME")
    from PIL import Image
    with Image.open(selection["image"]) as source:
        image_width, image_height = source.size
    height = CONTENT * image_height / image_width
    assert height <= 422, "Capture is too tall for the full-image gallery layout"
    image_top = 626
    pdf.drawImage(str(selection["image"]), MARGIN, image_top - height, width=CONTENT, height=height,
                  preserveAspectRatio=True, mask="auto")
    top = paragraph(pdf, "IN GAME " + escape(selection["caption"]), MARGIN, image_top - height - 14,
                    size=10.4, leading=14.2)
    run = selection["runtime"]
    top = paragraph(pdf, f"This target's completed run records <b>{run['passes']} PASS assertions/steps</b> and "
                    f"<b>{run['screenshots']} native screenshots</b>, with zero reported failures. The verifier "
                    "confirms matching game/dependency versions, original dependency pins, and final staged/collected artifact hashes.",
                    MARGIN, top - 17, size=10, leading=14)
    top = paragraph(pdf, "Capture: " + escape(selection["image"].relative_to(ROOT).as_posix()) + "<br/>" +
                    "Scenario evidence: " + escape(selection["required_pass"]) + "<br/>" +
                    "Report: " + escape(selection["report"].relative_to(ROOT).as_posix()),
                    MARGIN, top - 17, size=8.7, leading=11.5, color=MUTED)
    assert top >= 63, "Evidence page content exceeds its safe layout area"
    pdf.showPage()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path, default=ROOT / "controlify/docs/salts-inventory-controlify-feature-guide.pdf")
    parser.add_argument("--source", type=Path, default=ROOT / "controlify/docs/compatibility-appendix.json")
    parser.add_argument("--targets", type=Path, default=ROOT / "controlify/targets.json")
    parser.add_argument("--results-root", type=Path, default=ROOT / "build/controlify-qa")
    parser.add_argument("--artifacts", type=Path, default=ROOT / "dist/controlify-test")
    parser.add_argument("--run-profile", default="packaged-controlify")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--publish", action="store_true", help="Replace the published guide only after all twelve exact reports validate")
    args = parser.parse_args()
    args.base = args.base.resolve()
    if args.output is None:
        args.output = args.base if args.publish else ROOT / "tmp/pdfs/controlify-compatibility-appendix/feature-guide-preflight.pdf"
    args.output = args.output.resolve()
    if args.output == args.base and not args.publish:
        parser.error("Updating the published guide requires --publish and twelve verified runtime reports")
    if not args.publish and not args.output.is_relative_to(ROOT / "tmp/pdfs"):
        parser.error("Preflight output must stay under tmp/pdfs; publication requires all twelve verified reports")
    source = json.loads(args.source.read_text(encoding="utf-8-sig"))
    assert source["schema_version"] == 1 and source["preserved_pages"] == 14
    validation = load_tool("report-controlify-validation.py")
    runtime = load_tool("verify-controlify-runtime.py")
    boundary = load_tool("verify-controlify-boundaries.py")
    with contextlib.redirect_stdout(io.StringIO()):
        targets = list(boundary.matrix(args.targets).values())
    args.results_root, args.artifacts = args.results_root.resolve(), args.artifacts.resolve()
    runs = validate_runs(targets, args, runtime)
    complete = sum(row["status"] == "Verified" for row in runs.values())
    if args.publish and complete != 12:
        parser.error(f"Publication refused: only {complete}/12 exact packaged runtimes validate")
    suites = [validation.junit("Controller", ROOT / "controlify/model/build/test-results/test")]
    suites += [validation.junit(game, ROOT / f"versions/{game}/fabric/build/test-results/test") for game in validation.VERSIONS]
    if args.publish and any(row["status"] != "Passed" for row in suites):
        parser.error("Publication refused: an automated suite is missing or not passing")
    captures = select_captures(source, targets, runs, args)
    if args.publish and len(captures) != len(source["modern_evidence"]):
        parser.error("Publication refused: a required modern verified screenshot is absent")
    register_fonts()
    generated = datetime.now(timezone.utc).strftime("%d %B %Y, %H:%M UTC")
    original = PdfReader(args.base)
    assert len(original.pages) >= 14 and "Controlify feature guide" in original.pages[0].extract_text()
    total_pages = 16 + len(captures)
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(WIDTH, HEIGHT))
    matrix_page(pdf, source, targets, runs, generated, total_pages)
    installation_page(pdf, source, targets, runs, suites, generated, total_pages)
    for number, capture in enumerate(captures, start=17):
        evidence_page(pdf, capture, number, total_pages)
    pdf.save()
    appendix = PdfReader(io.BytesIO(buffer.getvalue()))
    writer = PdfWriter()
    writer.append(original, pages=(0, 14), import_outline=True)
    writer.append(appendix, import_outline=False)
    writer.add_outline_item("Original illustrated feature guide", 0)
    writer.add_outline_item("Current compatibility and installation", 14)
    for number, capture in enumerate(captures, start=16):
        writer.add_outline_item(f"{capture['minecraft']} {LOADERS[capture['loader']]} native evidence", number)
    writer.add_metadata({"/Title": "Salt's Inventory Controlify - features and native compatibility",
                         "/Author": "Salt's Inventory Update", "/Subject": f"12 native targets; {complete} exact packaged runtimes verified"})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_name(args.output.stem + ".writing.pdf")
    with temporary.open("wb") as stream:
        writer.write(stream)
    check = PdfReader(temporary)
    assert len(check.pages) == total_pages
    assert all(original.pages[number].extract_text() == check.pages[number].extract_text() for number in range(14)), \
        "The original fourteen feature guide pages changed"
    assert sum(len(page.get("/Annots", [])) for page in check.pages[14:]) >= 24, "Pinned dependency links are absent"
    temporary.replace(args.output)
    evidence = {"generated_at": generated, "published": args.publish, "verified_runtimes": complete,
                "junit_tests": sum(row["tests"] for row in suites), "pages": total_pages,
                "preserved_feature_pages": 14,
                "java_requirements": source["java_requirements"],
                "fabric_loader_requirements": source["fabric_loader_requirements"],
                "runtime_dependencies": [{"minecraft": target["minecraft"], "loader": target["loader"], **pin}
                                         for target in targets for pin in target.get("runtime_dependencies", [])],
                "captures": [{"minecraft": row["minecraft"], "loader": row["loader"],
                              "capture": row["capture"], "path": row["image"].relative_to(ROOT).as_posix()} for row in captures]}
    evidence_path = args.output.with_suffix(".evidence.json") if not args.publish else ROOT / "build/controlify-guide-evidence.json"
    evidence_path.write_text(json.dumps(evidence, indent=2) + "\n", encoding="utf-8")
    print(f"Guide {'published' if args.publish else 'preflight created'}: {total_pages} pages, {complete}/12 runtimes, "
          f"{len(captures)} verified modern captures; original fourteen pages preserved")
    print(args.output)


if __name__ == "__main__":
    main()
