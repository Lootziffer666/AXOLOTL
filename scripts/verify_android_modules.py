#!/usr/bin/env python3
"""Validate bundled optional-module manifests and their Activity source files."""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
ANDROID = "{http://schemas.android.com/apk/res/android}"
ACTION = "app.axolotl.action.MODULE"
REQUIRED_META = {
    "app.axolotl.module.ID",
    "app.axolotl.module.TITLE",
    "app.axolotl.module.DESCRIPTION",
    "app.axolotl.module.VERSION",
    "app.axolotl.module.ICON",
    "app.axolotl.module.CAPABILITIES",
}


def main() -> int:
    root = ET.parse(MANIFEST).getroot()
    modules: dict[str, str] = {}
    for activity in root.findall("./application/activity"):
        actions = {
            action.get(f"{ANDROID}name")
            for action in activity.findall("./intent-filter/action")
        }
        if ACTION not in actions:
            continue
        name = activity.get(f"{ANDROID}name", "")
        if activity.get(f"{ANDROID}exported") != "true":
            raise SystemExit(f"Optional module Activity must be exported: {name}")
        metadata = {
            item.get(f"{ANDROID}name"): item.get(f"{ANDROID}value")
            for item in activity.findall("meta-data")
        }
        missing = REQUIRED_META - metadata.keys()
        if missing:
            raise SystemExit(f"Module {name} is missing metadata: {sorted(missing)}")
        module_id = metadata["app.axolotl.module.ID"]
        if module_id in modules:
            raise SystemExit(f"Duplicate bundled module id: {module_id}")
        class_name = name.removeprefix(".")
        source = ROOT / "app/src/main/java/app/axolotl" / f"{class_name}.kt"
        if not source.is_file():
            raise SystemExit(f"Module Activity source is missing: {source.relative_to(ROOT)}")
        modules[module_id] = name

    expected = {"files", "browser", "ai-models", "automate"}
    if modules.keys() != expected:
        raise SystemExit(f"Bundled module mismatch: expected={sorted(expected)}, actual={sorted(modules)}")
    standalone_modules = {
        "apps": (
            ROOT / "modules/apps/src/main/AndroidManifest.xml",
            ROOT / "modules/apps/src/main/java/app/axolotl/module/apps/AppsActivity.kt",
        ),
        "pwa-studio": (
            ROOT / "modules/pwa/src/main/AndroidManifest.xml",
            ROOT / "modules/pwa/src/main/java/app/axolotl/module/pwa/PwaStudioActivity.kt",
        ),
    }
    for expected_id, (manifest, source) in standalone_modules.items():
        standalone_root = ET.parse(manifest).getroot()
        standalone = standalone_root.find("./application/activity")
        if standalone is None:
            raise SystemExit(f"Standalone {expected_id} module Activity is missing")
        standalone_meta = {
            item.get(f"{ANDROID}name"): item.get(f"{ANDROID}value")
            for item in standalone.findall("meta-data")
        }
        standalone_actions = {
            item.get(f"{ANDROID}name")
            for item in standalone.findall("./intent-filter/action")
        }
        if standalone_meta.get("app.axolotl.module.ID") != expected_id or ACTION not in standalone_actions:
            raise SystemExit(f"Standalone {expected_id} module contract is invalid")
        if not source.is_file():
            raise SystemExit(f"Standalone {expected_id} module source is missing")
    print(f"Verified {len(modules)} bundled and {len(standalone_modules)} standalone Android module contracts.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
