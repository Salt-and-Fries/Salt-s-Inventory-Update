"""Screenshot atlas of the second Project Everything batch on all twelve targets.

Uses only retained real-client captures. Crops change framing, never UI pixels.
The crop manifest records each source, observation and pixel rectangle.
"""
from pathlib import Path
import hashlib
import json
import math

from PIL import Image
from reportlab.lib.colors import HexColor, white
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT / "tmp/pdfs/project-everything-ten-all-targets"
OUT = ROOT / "output/pdf/siu-project-everything-ten-all-targets-ui.pdf"
LEGACY = ROOT / "build/project-everything-ten-1.20.1/gameplay"
PORT = ROOT / "build/project-everything-ten-port"
W, H = 960, 720
INK = HexColor("#173b45")
MUTED = HexColor("#526b73")
TEAL = HexColor("#087f88")
PALE = HexColor("#eef5f5")
LINE = HexColor("#d5e4e5")
FONT, BOLD = "AtlasUI", "AtlasBold"
pdfmetrics.registerFont(TTFont(FONT, "C:/Windows/Fonts/segoeui.ttf"))
pdfmetrics.registerFont(TTFont(BOLD, "C:/Windows/Fonts/segoeuib.ttf"))

NAMES = {
    "bartering": "Bartering Station",
    "alloy-forge": "Alloy Forgery",
    "heater": "Seasons: Extras - heater",
    "chiller": "Seasons: Extras - chiller",
    "alloyed": "Alloyed",
    "cooking-pot": "Farmer's Delight - cooking pot",
    "copper-pot": "Miner's Delight - copper pot",
    "identifying": "Better Archeology - identifying table",
    "fossil": "Better Archeology - fossil socket",
    "imbuing": "Illager Invasion - Imbuing Table",
    "combustion": "Galosphere - Combustion Table",
    "sawmill": "Sawmill",
}
FAMILIES = [
    ("bartering-station", "Bartering Station"),
    ("alloy-forgery", "Alloy Forgery"),
    ("fabric-seasons-extras", "Fabric Seasons: Extras"),
    ("alloyed", "Alloyed"),
    ("farmers-delight", "Farmer's Delight / Refabricated"),
    ("miners-delight", "Miner's Delight"),
    ("better-archeology", "Better Archeology"),
    ("illager-invasion", "Illager Invasion"),
    ("galosphere", "Galosphere"),
    ("sawmill", "Sawmill"),
]
CASE_FAMILY = {
    "bartering": "bartering-station", "alloy-forge": "alloy-forgery",
    "heater": "fabric-seasons-extras", "chiller": "fabric-seasons-extras",
    "alloyed": "alloyed", "cooking-pot": "farmers-delight",
    "copper-pot": "miners-delight", "identifying": "better-archeology",
    "fossil": "better-archeology", "imbuing": "illager-invasion",
    "combustion": "galosphere", "sawmill": "sawmill",
}
PROJECT_FAMILY = {
    "create-alloyed": "alloyed", "universal-sawmill": "sawmill",
    "farmers-delight-refabricated": "farmers-delight",
}
VERSIONS = ["1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3"]


def targets():
    return [(v, loader) for v in VERSIONS
            for loader in ("fabric", "forge" if v == "1.20.1" else "neoforge")]


def loader_name(loader):
    return {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}[loader]


def inventory():
    result = {}
    for name in ("native-windows-1.20.1.json", "native-windows-all-targets.json"):
        data = json.loads((ROOT / "compatibility" / name).read_text(encoding="utf-8"))
        for target in data["targets"]:
            entries = {}
            for item in target["integrations"]:
                project = item.get("family", item["project"])
                family = PROJECT_FAMILY.get(project, project)
                entries[family] = item
            result[(target.get("minecraft", data.get("minecraft", "1.20.1")), target["loader"])] = entries
    assert len(result) == 12
    assert sum(map(len, result.values())) == 67
    return result


def legacy_cards(loader):
    runs = (["ten-final-fabric-09", "ten-recipes-fabric-10", "ten-polish-fabric-11"]
            if loader == "fabric" else ["ten-forge-03", "ten-forge-04", "ten-recipes-forge-05", "ten-polish-forge-06"])
    records = {}
    for run in runs:
        folder = LEGACY / f"1.20.1-{loader}-{run}"
        for file in folder.glob("images-*.json"):
            for record in json.loads(file.read_text(encoding="utf-8")):
                records[(record["case"], record["state"])] = dict(record, run=run)

    selected = [
        ("bartering", "bartered-loot", "Bartering Station", "Native preview and collected loot", "window"),
    ]
    if loader == "fabric":
        selected += [
            ("alloy-forge", "smelting-active", "Alloy Forgery", "Native smelting animation", "window"),
            ("alloy-forge", "lava-reservoir", "Alloy Forgery", "Exterior lava reservoir", "window"),
        ]
        for key, label in [("bricks", "Bricks"), ("stone_bricks", "Stone bricks"),
                           ("deepslate_bricks", "Deepslate bricks"), ("polished_blackstone", "Polished blackstone"),
                           ("prismarine_bricks", "Prismarine bricks"), ("end_stone_bricks", "End stone bricks")]:
            selected.append(("alloy-" + key, "active", "Alloy Forgery", label + " controller", "window"))
        selected += [
            ("heater", "three-modules", NAMES["heater"], "Three native fuel modules", "window"),
            ("chiller", "three-modules", NAMES["chiller"], "Original chiller artwork and controls", "window"),
            ("seasons-two-windows", "fuel-transferred", "Seasons: Extras", "Two independent windows", "group"),
        ]
    selected += [
        ("cooking-pot", "cooking-active", NAMES["cooking-pot"], "Recipe toggle inside the updated frame", "window"),
        ("cooking-pot", "recipe-selected", NAMES["cooking-pot"], "Native recipe book and ingredient placement", "group"),
    ]
    if loader == "forge":
        selected += [
            ("copper-pot", "cooking-active", NAMES["copper-pot"], "Copper pot and serving slots", "window"),
            ("copper-pot", "recipe-selected", NAMES["copper-pot"], "Native copper-pot recipe book", "group"),
        ]
    selected += [
        ("identifying", "identifying-active", NAMES["identifying"], "Updated parchment frame / processing", "window"),
        ("identifying", "identified", NAMES["identifying"], "Native identified result", "window"),
        ("fossil", "ready", NAMES["fossil"], "Complete native decorative socket", "window"),
        ("imbuing", "valid-upgrade", NAMES["imbuing"], "Native illustration and valid upgrade", "window"),
        ("combustion", "configured-bomb", NAMES["combustion"], "All three native bomb modifiers", "window"),
        ("sawmill-narrow", "search-result", "Sawmill", "Narrow layout / filtered native recipe", "window"),
        ("sawmill-wide", "search-result", "Sawmill", "Wide layout / filtered native recipe", "window"),
        ("farmers-cabinet", "native-storage", "Farmer's Delight - cabinet", "Ordinary SIU storage window", "window"),
        ("canvas-sign", "native-editor", "Farmer's Delight - canvas sign", "Original standalone editor", "editor"),
        ("hanging-canvas-sign", "native-editor", "Farmer's Delight - hanging sign", "Original standalone editor", "editor"),
        ("alloyed", "items-in-storage", "Alloyed - legacy 2.x", "Materials in storage; no owned forge UI", "window"),
    ]
    cards = []
    for case, state, title, detail, mode in selected:
        r = records[(case, state)]
        cards.append(dict(title=title, detail=detail, image=Path(r["file"]), client=r["client"],
                          case=case, capture=state, run=r["run"], mode=mode, legacy=True))
    return cards


def modern_cards(mc, loader):
    rows = []
    for run in ["ten-port-final-01", "ten-port-final-05", "ten-port-final-07",
                "ten-followup-01", "ten-head-01", "ten-owo-01"]:
        rows += [r for r in json.loads((PORT / f"matrix-{run}.json").read_text())
                 if (r["minecraft"], r["loader"]) == (mc, loader)]
    captures = {}
    for row in rows:
        folder = ROOT / row["output"]
        for record in json.loads((folder / "observations.json").read_text(encoding="utf-8")):
            name = record["command"].get("capture")
            if name:
                assert not record.get("error"), (folder, name)
                captures[name] = dict(record, folder=folder, run=row["run"])
    selected = []
    for case in NAMES:
        primary = case + "-returned"
        if primary not in captures:
            continue
        selected.append((primary, case, "Native window / item returned", "window"))
        if case == "bartering" and "bartering-head-active" in captures:
            selected.append(("bartering-head-active", case, "Native piglin-highlight control active", "window"))
        if case in ("cooking-pot", "copper-pot"):
            book = case + "-book-after-reopen"
            if book not in captures:
                book = case + "-book"
            selected.append((book, case, "Native recipe book after opening", "book"))
        if case == "alloyed" and "alloyed-processing" in captures:
            selected.append(("alloyed-processing", case, "Native forge processing", "window"))
        if case == "alloy-forge" and "alloy-forge-slot-disabled" in captures:
            selected.append(("alloy-forge-slot-disabled", case, "Native input slot locked", "window"))
            selected.append(("alloy-forge-processing", case, "Live fuel/property synchronization", "window"))
    cards = []
    for name, case, detail, mode in selected:
        r = captures[name]
        assert r["client"]["windows"], name
        cards.append(dict(title=NAMES[case], detail=detail, image=r["folder"] / "screenshots" / (name + ".png"),
                          client=r["client"], case=case, capture=name, run=r["run"], mode=mode, legacy=False))
    return cards


def crop(card):
    image = Image.open(card["image"]).convert("RGB")
    client = card["client"]
    gw, gh = client["guiWidth"], client["guiHeight"]
    if card["legacy"]:
        if card["mode"] == "editor":
            box = (max(0, gw / 2 - 160), 20, min(gw, gw / 2 + 160), min(gh, 340))
        else:
            boxes = [client["window"]]
            if card["mode"] == "group":
                boxes = [w["bounds"] for w in client["allWindows"]]
                for window in client.get("desktopWindows", []):
                    if window.get("hidden"):
                        continue
                    boxes.append(window["bounds"])
                    if window.get("recipeVisible"):
                        boxes.append(window["recipeBounds"])
            boxes = [b for b in boxes if b]
            box = (min(b["x"] for b in boxes) - 8,
                   min(b["y"] for b in boxes) - (21 if card["case"].startswith("alloy-") else 6),
                   max(b["x"] + b["width"] for b in boxes) + 8,
                   max(b["y"] + b["height"] for b in boxes) + 8)
    else:
        b = client["windows"][0]["bounds"]
        box = (b["x"] - 4, b["y"] - (21 if card["case"] == "alloy-forge" else 4),
               b["x"] + b["width"] + 4, b["y"] + b["height"] + 4)
        if card["mode"] == "book":
            # Original recipe panel plus its category tab, beside the pot.
            box = (b["x"] - 187, b["y"] - 4, b["x"] + b["width"] + 4,
                   max(b["y"] + b["height"] + 4, b["y"] + 171))
    sx, sy = image.width / gw, image.height / gh
    pixels = (max(0, math.floor(box[0] * sx)), max(0, math.floor(box[1] * sy)),
              min(image.width, math.ceil(box[2] * sx)), min(image.height, math.ceil(box[3] * sy)))
    assert pixels[2] > pixels[0] and pixels[3] > pixels[1]
    return image.crop(pixels), pixels


class Atlas:
    def __init__(self, cards, coverage):
        TMP.mkdir(parents=True, exist_ok=True)
        OUT.parent.mkdir(parents=True, exist_ok=True)
        self.c = canvas.Canvas(str(OUT), pagesize=(W, H), pageCompression=1)
        self.c.setTitle("SIU | Project Everything - every supported version and loader")
        self.c.setAuthor("Salt's Inventory Update")
        self.cards, self.coverage = cards, coverage
        self.page = 0
        self.figures = []
        self.pages = []
        self.starts = {}

    def text(self, x, y, value, size=11, bold=False, color=INK):
        self.c.setFillColor(color)
        self.c.setFont(BOLD if bold else FONT, size)
        self.c.drawString(x, y, value)

    def start(self, title, subtitle, key=None):
        if self.page:
            self.c.showPage()
        self.page += 1
        self.pages.append(dict(page=self.page, title=title, subtitle=subtitle))
        self.c.setFillColor(white)
        self.c.rect(0, 0, W, H, stroke=0, fill=1)
        self.text(36, H - 28, "PROJECT EVERYTHING / SCREENSHOT ATLAS", 9, True, TEAL)
        self.text(36, H - 64, title, 27, True)
        self.text(36, H - 86, subtitle, 11, color=MUTED)
        self.c.setStrokeColor(LINE)
        self.c.line(36, 39, W - 36, 39)
        self.text(36, 23, "Core 0.1.6 + Compatibility 0.1.3 | Real gameplay captures | 27 October 2026", 9, color=MUTED)
        self.c.setFont(FONT, 9)
        self.c.drawRightString(W - 36, 23, str(self.page))
        if key:
            self.c.bookmarkPage(key)
            self.c.addOutlineEntry(title, key, 0)

    def picture(self, card, x, y, w, h, target):
        self.c.setFillColor(PALE)
        self.c.roundRect(x, y, w, h, 8, stroke=0, fill=1)
        self.text(x + 14, y + h - 24, card["title"], 13, True)
        self.text(x + 14, y + h - 43, card["detail"], 10, color=MUTED)
        im, box = crop(card)
        dest = TMP / f"figure-{len(self.figures) + 1:03}.png"
        im.save(dest)
        scale = min((w - 24) / im.width, (h - 77) / im.height)
        iw, ih = im.width * scale, im.height * scale
        self.c.drawImage(ImageReader(im), x + (w - iw) / 2, y + 22 + (h - 77 - ih) / 2,
                         iw, ih, mask="auto")
        self.text(x + 14, y + 9, card["run"] + " / " + card["capture"], 7, color=MUTED)
        self.figures.append(dict(page=self.page, target=target, title=card["title"], detail=card["detail"],
                                 source=str(card["image"].relative_to(ROOT)), capture=card["capture"],
                                 run=card["run"], pixel_crop=list(box), crop=str(dest.relative_to(ROOT)),
                                 source_sha256=hashlib.sha256(card["image"].read_bytes()).hexdigest()))

    def build(self):
        total = sum(len(a) for a in self.cards.values())
        self.start("Every supported target. Every adapted UI.", "Ten mod families | Minecraft 1.20.1 through 26.3 | Fabric, Forge and NeoForge", "cover")
        self.text(36, 588, f"12 TARGETS     67 AVAILABLE MOD/TARGET PAIRS     {total} SCREENSHOT VIEWS", 13, True, TEAL)
        heroes = [self.cards[("26.3", "fabric")][0],
                  next(c for c in self.cards[("1.20.1", "fabric")] if c["case"] == "identifying"),
                  next(c for c in self.cards[("26.1.2", "fabric")] if c["capture"] == "alloy-forge-slot-disabled"),
                  next(c for c in self.cards[("1.21.1", "neoforge")] if c["case"] == "copper-pot")]
        for i, card in enumerate(heroes):
            self.picture(card, 36 + (i % 2) * 452, 317 - (i // 2) * 225, 436, 211, "cover")
        self.text(36, 70, "Observed screenshots, not an exhaustive gameplay certification. Test limits are on the final page.", 11, color=MUTED)

        # Starts are predictable because each target uses complete four-card pages.
        next_page = 3
        for target in targets():
            self.starts[target] = next_page
            next_page += math.ceil(len(self.cards[target]) / 4)
        self.start("Version and loader index", "Click a column heading or use the PDF bookmarks to jump to a target.", "index")
        x0, cw, y0 = 232, 57, 535
        for i, (mc, loader) in enumerate(targets()):
            x = x0 + i * cw
            if i % 2 == 0:
                self.text(x + 4, y0 + 47, mc, 11, True)
            label = {"fabric": "Fabric", "forge": "Forge", "neoforge": "Neo"}[loader]
            self.text(x + 4, y0 + 24, label, 10, True, TEAL)
            self.text(x + 4, y0 + 7, "p. " + str(self.starts[(mc, loader)]), 9, color=MUTED)
            self.c.linkRect("", f"target-{mc}-{loader}", (x, y0, x + cw - 2, y0 + 63), thickness=0)
        for row, (family, title) in enumerate(FAMILIES):
            y = y0 - 38 - row * 34
            if row % 2 == 0:
                self.c.setFillColor(PALE)
                self.c.rect(36, y - 11, W - 72, 33, fill=1, stroke=0)
            self.text(44, y, title, 10, True)
            for i, target in enumerate(targets()):
                value = "Yes" if family in self.coverage[target] else "-"
                if family == "alloyed" and target[0] == "1.20.1":
                    value = "Items"
                self.text(x0 + i * cw + 7, y, value, 10, color=TEAL if value != "-" else MUTED)
        self.text(36, 118, "Yes = available native UI integration. Items = legacy Alloyed materials; no standalone owned UI.", 11, color=MUTED)
        self.text(36, 96, "- = no matching upstream release on that SIU target. Unsupported combinations are not pictured.", 11, color=MUTED)
        self.text(36, 74, "Scope: the ten integrations in this Project Everything batch, including their alternate menu views.", 11, color=MUTED)

        for target in targets():
            mc, loader = target
            cards = self.cards[target]
            for offset in range(0, len(cards), 4):
                n = offset // 4 + 1
                pages = math.ceil(len(cards) / 4)
                suffix = f" | {n}/{pages}" if pages > 1 else ""
                subtitle = "Corrected layouts and native controls | " + str(len(self.coverage[target])) + " available mod families"
                if loader == "fabric" and mc in ("1.21.1", "1.21.11"):
                    subtitle += " | Loom-mapped release classes"
                self.start(mc + " / " + loader_name(loader) + suffix, subtitle,
                           f"target-{mc}-{loader}" if offset == 0 else None)
                if offset == 0:
                    assert self.page == self.starts[target]
                for i, card in enumerate(cards[offset:offset + 4]):
                    self.picture(card, 36 + (i % 2) * 452, 335 - (i // 2) * 280,
                                 436, 266, mc + "/" + loader)

        self.start("What these screenshots establish", "Focused affected-window evidence, collected in real Minecraft clients.", "limits")
        bullets = [
            ("Visible coverage", "Each available menu family is pictured for every supported SIU version and loader."),
            ("Corrected 1.20.1 designs", "Pot buttons are inside their frames. Identifying tables use the final parchment texture."),
            ("Native UI retained", "Recipe books, processing artwork, slot roles, reservoir and locking controls remain native."),
            ("Legacy Alloyed", "1.20.1 adds Create materials, not its own forge screen. Its storage workflow is shown instead."),
            ("Native editors", "Canvas sign editors stay original screens. Cabinets use normal SIU storage windows."),
            ("Fabric 1.21.x qualification", "Those gameplay captures use Loom-mapped release classes due to an upstream runtime remapper failure."),
            ("Test boundaries", "Integrated-server worlds only. These pictures do not certify every recipe, modpack or dedicated server."),
            ("Optional combinations", "Legacy Forge Create/Galosphere used separate fixtures. Create-present modern Alloyed knife crafting was not tested."),
            ("Screenshot selection", "Images are cropped from stored captures. No UI elements, slots, artwork or animation frames were fabricated."),
        ]
        y = 570
        for title, body in bullets:
            self.text(36, y, title, 12, True, TEAL)
            # Keep prose brief and on a separate line with generous spacing.
            self.text(36, y - 22, body, 11)
            y -= 53
        self.text(36, 72, "Evidence: build/project-everything-ten-1.20.1/gameplay and build/project-everything-ten-port/gameplay.", 10, color=MUTED)
        self.c.save()
        manifest = dict(pdf=str(OUT.relative_to(ROOT)), pages=self.page,
                        unique_views=total, displayed_figures=len(self.figures),
                        targets=[dict(minecraft=m, loader=l, views=len(self.cards[(m,l)]), first_page=self.starts[(m,l)]) for m,l in targets()],
                        page_index=self.pages, figures=self.figures)
        (TMP / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
        assert all(set(self.coverage[t]) <= represented_families(t, self.cards[t]) for t in targets())
        print(json.dumps(dict(pdf=str(OUT), pages=self.page, unique_views=total, displayed_figures=len(self.figures))))


def represented_families(target, cards):
    result = set()
    for card in cards:
        case = card["case"]
        if case.startswith("alloy-") and case != "alloyed":
            result.add("alloy-forgery")
        elif case in ("seasons-two-windows",):
            result.add("fabric-seasons-extras")
        elif case.startswith("sawmill"):
            result.add("sawmill")
        elif case in ("farmers-cabinet", "canvas-sign", "hanging-canvas-sign"):
            result.add("farmers-delight")
        elif case in CASE_FAMILY:
            result.add(CASE_FAMILY[case])
    return result


if __name__ == "__main__":
    coverage = inventory()
    cards = {t: legacy_cards(t[1]) if t[0] == "1.20.1" else modern_cards(*t) for t in targets()}
    Atlas(cards, coverage).build()
