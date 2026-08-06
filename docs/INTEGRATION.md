# Integrationskonzept

## 1. Ergebnis der Bestandsaufnahme

Die sechs Archive sind keine Varianten derselben App, sondern sechs
eigenständige Android-/Compose-Produkte. Die Prototypen verwenden überwiegend
dieselbe Beispiel-Package (`com.example`) und bringen jeweils eigene
`MainActivity`, Room-Datenbank, Theme-, Gradle- und Manifest-Konfiguration mit.
Ein direktes Zusammenkopieren würde daher Klassen-, Ressourcen-, Manifest- und
Datenbankkonflikte erzeugen.

Inhaltlich gibt es dagegen eine klare gemeinsame Produktidee: ein persönlicher,
KI-gestützter Workspace für Apps, Dateien, Web-Inhalte, Automationen und
systemweite Schnellaktionen. BELLOWS ist dabei keine eigene Hauptnavigation,
sondern die Infrastruktur hinter allen Features. Evolver ist ebenfalls besser
als Automations- und UI-Laufzeit aufgehoben als als zweite App-Shell.

## 2. Zielarchitektur

### Bereits implementierter Modul-Kern

`app.axolotl.evolver` bildet inzwischen die verbindliche Integrationsgrenze:
`AxolotlModule` veröffentlicht Manifest, Fähigkeiten, Status, Aktionen und eine
deklarative Startoberfläche. `ModuleRegistry` verhindert ungültige und doppelte
Module. `EvolverEngine` nimmt ausschließlich `EvolutionPatch`-Daten an, prüft
Revision, Node-Anzahl/-Tiefe, eindeutige IDs und registrierte Actions und führt
Snapshots/Rollback. `ModuleActionDispatcher` führt nur Actions aktiver Module
aus, für die die App explizit einen Handler gebunden hat.

Borderline, Apps, Files, Browser und AI & Models sind aktive Module. Apps liest reale
Launcher-Aktivitäten; Files navigiert DocumentProvider-Verzeichnisse über SAF;
Browser stellt eine minimal gehärtete HTTP/HTTPS-WebView bereit; AI & Models
sendet echte OpenAI-kompatible Requests über eine getrennte `AiGateway`-Grenze.
Automate ist als `PLANNED` registriert und kann deshalb weder Evolver-Patches
noch Actions ausführen. Ein neues Feature wird künftig durch eine
`AxolotlModule`-Implementierung eingehängt, statt die App-Shell direkt zu
verändern.

```text
:app                    App-Shell, Navigation, Deep Links, Onboarding
├── :core:model         gemeinsame IDs, Resultate und Domain-Events
├── :core:database      genau eine versionierte Room-Datenbank
├── :core:ai            Provider, Router, Prompt Policy, Memory, Kosten
├── :core:search        föderierter Suchindex für Dateien, Apps und Web
├── :core:permissions   Android-Rollen und nachvollziehbare Freigaben
├── :core:designsystem  Theme, Komponenten, Icons und Accessibility
├── :feature:home       Dashboard und globale Suche
├── :feature:apps       App Cluster AI
├── :feature:files      Everything Files
├── :feature:browser    Nexus ModuBrowser
├── :feature:automation Evolver Engine
├── :feature:dock       Borderline Dock und Overlay-Service
```

Feature-Module dürfen nur über Interfaces aus `core` miteinander sprechen.
Beispiele sind `SearchContributor`, `AiGateway`, `ClipboardRepository`,
`WorkspaceExporter` und ein typisierter `AppEventBus`. Dadurch kann etwa der
Browser einen Fund an Files übergeben, ohne dessen Room-Entities zu kennen.

## 3. Gemeinsames Nutzererlebnis

Die App erhält fünf primäre Ziele:

1. **Home** – globale Suche, letzte Objekte, laufende Automationen.
2. **Apps** – Katalog, KI-Cluster, Design-Inspektion und Batch-Aktionen.
3. **Files** – lokaler/entfernter Bestand, Versionen, Vault und Bereinigung.
4. **Browser** – Web, Notebook, GitHub und MCP.
5. **Automate** – Evolver-Workflows, Snapshots, Logs und Freigaben.

Der Dock ist systemweit verfügbar, aber kein sechster Navigationspunkt. Er zeigt
dieselben Quick Actions, Snippets, Zwischenablageobjekte und Handoffs wie Home.
BELLOWS erscheint in den Einstellungen unter **AI & Modelle**.

## 4. Sicherheits- und Produktgrenzen

- LLM-Schlüssel werden über lokale, verschlüsselte Konfiguration oder einen
  Backend-Token-Broker bezogen; niemals über `BuildConfig` in die APK gebacken.
- Aktionen wie Deinstallation, Löschen, Vault-Export, Shell/Shizuku und vom LLM
  vorgeschlagene Änderungen benötigen eine explizite Bestätigung.
- Generative UI rendert ausschließlich eine versionierte Allowlist von
  Komponenten und Aktionen. Keine Modellantwort wird als Code ausgeführt.
- Overlay-, Storage-, Accessibility-, Notification- und Package-Rechte werden
  erst bei Nutzung des jeweiligen Features angefragt.
- WebView-Bridges sind origin-beschränkt; Dateizugriffe laufen über explizite
  Android-Schnittstellen statt über globale JavaScript-Objekte.
- Telemetrie, Router-Logs und Zwischenablageinhalte haben getrennte
  Aufbewahrungsfristen und einen sichtbaren Löschpfad.

## 5. Migrationsreihenfolge

### Phase A – Shell und Verträge

1. Neues Gradle-Projekt mit eindeutiger Namespace `app.axolotl` anlegen.
2. App-Shell, Navigation und Designsystem implementieren.
3. Gemeinsame Datenbank, AI-Gateway, Berechtigungsmodell und Feature-Interfaces
   definieren.
4. Archive nur in temporäre, ignorierte Arbeitsverzeichnisse entpacken; keine
   `.env` oder generierten Signing-Konfigurationen übernehmen.

### Phase B – vertikaler Kernpfad

1. Everything Files als erstes Feature migrieren, weil sein Index die globale
   Suche trägt.
2. App Cluster als zweiten `SearchContributor` ergänzen.
3. BELLOWS-Provider hinter `AiGateway` portieren und beide Features damit
   verbinden.
4. Einen Ende-zu-Ende-Pfad liefern: globale Suche → Ergebnis → AI-Aktion →
   bestätigte Änderung → Audit-Eintrag.

### Phase C – Browser und systemweite UI

1. Nexus-Browser portieren und WebView-Sicherheitsgrenze härten.
2. Gemeinsames Clipboard/Handoff-Repository einführen.
3. Dock-Service auf dieselben Repositories und Actions umstellen.

### Phase D – kontrollierte Evolution

1. Evolver-Schema und Renderer auf das gemeinsame Designsystem abbilden.
2. Snapshots, Migrationen, Signatur/Integritätsprüfung und Rollback ergänzen.
3. Generierte Workflows zunächst im Preview-, danach im Bestätigungsmodus
   aktivieren; autonome privilegierte Aktionen bleiben ausgeschlossen.

## 6. Definition of Done pro migriertem Feature

- Keine `com.example`-Packages oder duplizierten Theme-/Database-Singletons.
- Abhängigkeiten zeigen nur von Feature zu Core, nie von Feature zu Feature.
- Zustände überleben Prozessneustart und haben getestete DB-Migrationen.
- Empty-, Loading-, Error- und Permission-denied-Zustände sind abgedeckt.
- Sensible Aktion besitzt Bestätigung, Audit-Event und Undo, soweit technisch
  möglich.
- Unit-Tests für Domainlogik, Compose-Navigationstest und mindestens ein
  Ende-zu-Ende-Test sind vorhanden.
- TalkBack, dynamische Schriftgrößen und Light/Dark Theme funktionieren.

## 7. Bewusst nicht getan

Die Archive wurden nicht blind ins Git-Working-Tree entpackt. Das würde sowohl
potenzielle Secrets (ein Archiv enthält eine `.env`-Datei) als auch mehrere
konkurrierende Gradle-Projekte und tausende generierte/duplizierte Ressourcen
übernehmen. Die Migration soll dateiweise entlang der oben definierten Module
erfolgen; das Archiv bleibt dabei die unveränderte Referenz.
