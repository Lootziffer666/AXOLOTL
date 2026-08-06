#!/usr/bin/env python3
"""Validate the retained provenance catalog for the imported prototypes."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "docs" / "prototypes.json"


def main() -> int:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    prototypes = catalog.get("prototypes", [])
    if not prototypes:
        raise SystemExit("Prototype catalog is empty")

    required_fields = {"sourceArchive", "name", "platform", "domain", "targetModule"}
    source_archives: set[str] = set()
    for item in prototypes:
        missing = required_fields - item.keys()
        if missing:
            raise SystemExit(f"Incomplete prototype entry {item.get('name')!r}: {sorted(missing)}")
        source_archive = item["sourceArchive"]
        if source_archive in source_archives:
            raise SystemExit(f"Duplicate source archive: {source_archive}")
        source_archives.add(source_archive)

    checked_in_archives = sorted(archive.name for archive in ROOT.glob("*.zip"))
    if checked_in_archives:
        raise SystemExit(f"Prototype archives must not be checked in: {checked_in_archives}")

    print(f"Verified {len(prototypes)} provenance catalog entries; no ZIP archives checked in.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
