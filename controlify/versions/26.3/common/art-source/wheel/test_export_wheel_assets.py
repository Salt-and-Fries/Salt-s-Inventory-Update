"""Read-only authoring-check regressions; all test images live in disposable directories."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from PIL import Image

spec = importlib.util.spec_from_file_location("wheel_export", Path(__file__).with_name("export-wheel-assets.py"))
exporter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(exporter)


class ManualWheelAssetsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.target = Path(self.temp.name)
        self.previous = exporter.TARGET
        exporter.TARGET = self.target
        self.assets = [{"id": name, "width": 6, "height": 6}
                       for name in ("backdrop", "wheel_ring", "wheel_ring_highlight", "hovered_card")]
        for asset in self.assets:
            image = Image.new("P", (6, 6), 0 if asset["id"] == "backdrop" else 1)
            image.putpalette([0, 0, 0, 212, 175, 54] + [0] * (768 - 6))
            image.info["transparency"] = 0
            image.save(self.target / (asset["id"] + ".png"))

    def tearDown(self):
        exporter.TARGET = self.previous
        self.temp.cleanup()

    def test_indexed_full_ring_opaque_hover_transparent_backdrop_and_backups_are_read_only(self):
        Image.new("RGBA", (8, 8), "magenta").save(self.target / "wheel_ring_original_backup.png")
        before = {path.name: path.read_bytes() for path in self.target.glob("*.png")}
        exporter.check(self.assets)
        self.assertEqual(before, {path.name: path.read_bytes() for path in self.target.glob("*.png")})

    def test_manual_partial_alpha_is_preserved(self):
        Image.new("RGBA", (6, 6), (71, 111, 181, 127)).save(self.target / "hovered_card.png")
        before = (self.target / "hovered_card.png").read_bytes()
        exporter.check(self.assets)
        self.assertEqual(before, (self.target / "hovered_card.png").read_bytes())

    def test_missing_required_png_is_rejected(self):
        (self.target / "hovered_card.png").unlink()
        with self.assertRaisesRegex(AssertionError, "Missing wheel textures"):
            exporter.check(self.assets)

    def test_wrong_source_size_is_rejected(self):
        Image.new("RGBA", (5, 6), "white").save(self.target / "wheel_ring.png")
        with self.assertRaisesRegex(AssertionError, "wheel_ring"):
            exporter.check(self.assets)

    def test_non_png_with_png_extension_is_rejected(self):
        Image.new("RGB", (6, 6), "white").save(self.target / "wheel_ring.png", format="JPEG")
        with self.assertRaisesRegex(AssertionError, "wheel_ring"):
            exporter.check(self.assets)

    def test_empty_required_foreground_is_rejected(self):
        Image.new("RGBA", (6, 6), (0, 0, 0, 0)).save(self.target / "hovered_card.png")
        with self.assertRaisesRegex(AssertionError, "hovered_card"):
            exporter.check(self.assets)

    def test_retired_panel_and_arc_prompts_do_not_require_runtime_images(self):
        retired = [{"id": name, "runtime": False, "width": 6, "height": 6,
                    "prompt": "Preserved original authoring prompt"}
                   for name in ("panel", "wheel_panel", "arc_boundary")]
        before = json.dumps(retired, sort_keys=True)
        exporter.check(self.assets + retired)
        self.assertEqual(before, json.dumps(retired, sort_keys=True))
        self.assertTrue(all(not (self.target / (row["id"] + ".png")).exists() for row in retired))

    def test_export_does_not_recreate_retired_runtime_pngs_or_remove_their_prompts(self):
        retired = [{"id": name, "runtime": False, "prompt": "Keep this generated reference"}
                   for name in ("panel", "wheel_panel", "arc_boundary")]
        original_manifest = exporter.MANIFEST
        try:
            exporter.MANIFEST = self.target / "manifest.json"
            exporter.export(retired)
            self.assertEqual(retired, json.loads(exporter.MANIFEST.read_text())["assets"])
            self.assertTrue(all(not (self.target / (row["id"] + ".png")).exists() for row in retired))
        finally:
            exporter.MANIFEST = original_manifest


if __name__ == "__main__":
    unittest.main()
