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

    expected = {"apps", "files", "browser", "ai-models", "automate"}
    if modules.keys() != expected:
        raise SystemExit(f"Bundled module mismatch: expected={sorted(expected)}, actual={sorted(modules)}")
    print(f"Verified {len(modules)} optional Android module contracts.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
