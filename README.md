# AXOLOTL

AXOLOTL bündelt die in diesem Repository abgelegten AI-Studio-Prototypen zu
einer Android-App. Die Archive bleiben zunächst unverändert als Referenz; die
verbindliche Produkt- und Integrationsarchitektur steht in
[`docs/INTEGRATION.md`](docs/INTEGRATION.md).

## Enthaltene Prototypen

| Bereich | Prototyp | Ziel im gemeinsamen Produkt |
| --- | --- | --- |
| KI-Infrastruktur | BELLOWS / AI Router | Zentrale Provider-, Routing- und Memory-Schicht |
| Apps | App Cluster AI | App-Katalog, Cluster, Inspektion und Batch-Aktionen |
| Dateien | Everything Files | Index, Suche, Versionen, Vault und Netzwerkdateien |
| Automation | Evolver Engine | Kontrollierte generative Oberflächen und Workflows |
| System-UI | Borderline Dock / Hyperdock | Overlay, Clipboard, Snippets und Handoffs |
| Web | Nexus ModuBrowser | Browser, Module, MCP, Notebook- und GitHub-Integration |

Die maschinenlesbare Bestandsaufnahme liegt in
[`docs/prototypes.json`](docs/prototypes.json). Sie kann ohne Entpacken der
Archive geprüft werden:

```bash
python3 scripts/verify_prototypes.py
```

## Entwicklung

Die Zusammenführung hat mit Borderline als ausführbarem App-Rahmen begonnen.
Das Android-Projekt liegt in `app/`; die AXOLOTL-Startseite öffnet das bestehende
Borderline Control Center und zeigt die nächsten zu migrierenden Workspaces.
Eine genaue Trennung zwischen dem von Gemini erzeugten Funktionsumfang, den
übernommenen Teilen und den offenen Risiken steht in [`docs/AUDIT.md`](docs/AUDIT.md).
Der neue Evolver-Kern registriert diese Workspaces über stabile Modulverträge
und akzeptiert ausschließlich validierte, deklarative UI-Patches – niemals
beliebigen Kotlin-, HTML- oder JavaScript-Code.

```bash
gradle testDebugUnitTest
```

Für lokale Builds wird JDK 17 empfohlen. Die übrigen ZIP-Archive bleiben bis zur
schrittweisen Migration unveränderte Referenzen.

## Leitentscheidung

AXOLOTL wird **eine native Android-App mit modularen Feature-Grenzen**.
Gemeinsame Belange wie LLM-Zugriff, Datenhaltung, Berechtigungen, Suche und
Designsystem werden nur einmal implementiert.

> API-Schlüssel gehören weder in Git noch in die App-Binary. Vor dem Entpacken
> oder Übernehmen von Prototype-Dateien müssen `.env`-Dateien ausgeschlossen
> und eventuell enthaltene Schlüssel rotiert werden.
