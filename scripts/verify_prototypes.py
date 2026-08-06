#!/usr/bin/env python3
"""Validate the prototype catalog against the checked-in AI Studio archives."""

from __future__ import annotations

import json
from pathlib import Path
from zipfile import BadZipFile, ZipFile


ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "docs" / "prototypes.json"


def main() -> int:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    prototypes = catalog.get("prototypes", [])
    if not prototypes:
        raise SystemExit("Prototype catalog is empty")

    declared = {item["archive"] for item in prototypes}
    actual = {archive.name for archive in ROOT.glob("*.zip")}
    if declared != actual:
        missing = sorted(declared - actual)
        undocumented = sorted(actual - declared)
        raise SystemExit(
            f"Catalog mismatch; missing={missing}, undocumented={undocumented}"
        )

    for item in prototypes:
        archive_path = ROOT / item["archive"]
        try:
            with ZipFile(archive_path) as archive:
                metadata = json.loads(archive.read("metadata.json"))
                if metadata.get("name") != item["name"]:
                    raise SystemExit(
                        f"Name mismatch for {archive_path.name}: "
                        f"{metadata.get('name')!r} != {item['name']!r}"
                    )
        except (BadZipFile, KeyError, json.JSONDecodeError) as error:
            raise SystemExit(f"Invalid prototype {archive_path.name}: {error}") from error

    print(f"Verified {len(prototypes)} prototype archives and catalog entries.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
