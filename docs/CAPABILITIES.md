# AXOLOTL Feature- und Prototype-Vergleich

Stand: 6. August 2026. Bewertet wurden die Kotlin-/Manifest-/Gradle-Dateien in
allen sechs Original-ZIPs sowie der aktuell kompilierte Quellbaum unter `app/`.
Ein sichtbarer Button oder Dialog gilt hier **nicht** automatisch als Funktion.

## Direkte Antwort

**AXOLOTL lädt weiterhin keine PWA- oder Fremdcode-Module.** Es besitzt jetzt
aber den kontrollierten Evolver-Kern, über den App-Teile registriert und
declarative UI-Patches validiert, versioniert und zurückgerollt werden. Aktuell
sind Borderline, Apps, Files und Browser aktiv. Automate und AI & Models bleiben
registrierte, aber geplante Module (`NEXT`).

Zwei Originalprototypen enthielten Konzepte, die später eine kontrollierte
Erweiterbarkeit liefern könnten:

1. **BELLOWS** speichert HTML-PWA-Module in Room und zeigt sie in einer WebView.
   Das erweitert Web-Inhalte, nicht den nativen Android-Code. Dieses System ist
   noch nicht in AXOLOTL übernommen.
2. **Evolver Engine** lieferte die Idee eines begrenzten UI-Schemas. AXOLOTL hat
   davon jetzt einen neu geschriebenen, LLM-unabhängigen Sicherheitskern:
   Modulvertrag, Registry, geschlossene `UiNode`-Allowlist, Action-Allowlist,
   Revisionsprüfung, Snapshots und Rollback. Beliebiger Kotlin-/Web-Code bleibt
   ausdrücklich ausgeschlossen.

## Legende

- **Vorhanden:** ausführender Code ist in AXOLOTL enthalten.
- **Teilweise:** Code existiert, hat aber relevante Plattform- oder
  Produktlücken.
- **Nur Shell:** sichtbarer AXOLOTL-Einstieg ohne migrierte Funktion.
- **Nicht übernommen:** nur im Original-ZIP vorhanden.
- **Mock im Original:** schon im Gemini-Prototyp nur In-Memory-Demo, Dialog oder
  Statusumschalter ohne behauptetes Backend.

## Was AXOLOTL jetzt tatsächlich kann

| Funktion | Status | Tatsächliches Verhalten |
| --- | --- | --- |
| AXOLOTL-Startseite | Vorhanden | Zeigt Borderline und fünf Roadmap-Workspaces |
| Borderline Control Center | Vorhanden | Konfiguriert Dock, Snippets, Clipboard und Provisioning |
| System-Overlay | Teilweise | Foreground-Service mit vier Edge-Handles; benötigt Overlay-Recht und Gerätetest |
| App-Dock | Vorhanden | Listet Launcher-Apps, speichert Auswahl, startet Apps |
| Snippet Capsule | Vorhanden | Room-CRUD, Defaults, Copy/Share und Suche |
| Clipboard+ | Teilweise | Zeichnet Text lokal auf; moderne Android-Hintergrundlimits und Retention sind offen |
| Appendix | Vorhanden | Fügt Clipboard-Inhalte einer lokalen Markdown-Liste hinzu |
| Quick Actions | Teilweise | Lokale Regex-Erkennung und Android-Intents; keine echte AI- oder IBAN-Validierung |
| Handoffs | Vorhanden | Share, Websuche, Maps und „Send to AI“ über Android-App-Chooser |
| Logcat Inspector | Teilweise | Funktioniert nur mit privilegiertem `READ_LOGS`-ADB-Grant |
| Emergency/Private Mode | Teilweise | Stoppt Overlay bzw. neue Aufzeichnung; verschlüsselt keine vorhandenen Daten |
| Apps | Vorhanden | Liest reale Launcher-Apps, filtert nach Label/Package, startet Apps und öffnet Android-Appdetails |
| Files | Vorhanden | Navigiert echte DocumentProvider-Verzeichnisse per SAF, behält URI-Rechte und öffnet Dateien über Android |
| Browser | Vorhanden | Öffnet HTTP/HTTPS in einer gehärteten WebView ohne JavaScript, DOM Storage oder Datei-/Content-Zugriff |
| Automate / AI | Nur Shell | Karten sind noch nicht klickbare Feature-Implementierungen |
| PWA-Module | Nicht vorhanden | Kein PWA-Repository, Editor oder WebView-Container im aktuellen App-Code |
| Modul-Registry | Vorhanden | Ein stabiler Vertrag registriert Borderline und fünf geplante Module |
| Evolver-Patches | Vorhanden | Daten-Patches werden gegen UI-/Action-Allowlist, Revision, Tiefe und Größe validiert |
| Rollback | Teilweise | In-Memory-Snapshots funktionieren; persistente Room-Snapshots folgen |
| Selbst-Erweiterung | Kontrolliert vorbereitet | Module und UI-Daten sind erweiterbar; kein Fremdcode- oder PWA-Lader |

## Vergleich mit den sechs Originalprototypen

### 1. BELLOWS / AI Router

**Was laut Produktidee vorhanden sein sollte:** zentraler OpenAI-kompatibler
Router, Provider-Auswahl, Routingregeln, MemWiki und lokale PWA-Module.

**Was Gemini tatsächlich implementierte:**

- lokalen HTTP-Server und OpenAI-artige Chat-Endpunkte;
- Weiterleitung an konfigurierte externe und lokale Provider;
- Provider-/Modelllisten und Routingkonfiguration;
- Room-Tabellen für Memories und HTML-PWA-Module;
- Editor mit Beispielmodulen sowie WebView-Container für gespeichertes HTML;
- Start/Stop und lokale IP-/Portanzeige.

**Lücken im Original:** Provider-Definitionen sind weitgehend statisch;
mehrere UI-Aktionen sind TODO; Secrets/SSRF/Origin-Isolation und belastbare
Serverauthentifizierung sind nicht produktionsreif. PWA-Module sind HTML in
einer WebView, keine installierbaren nativen Feature-Module.

**In AXOLOTL:** **nicht übernommen**. Die Karte „AI & Models“ ist nur Shell.

### 2. App Cluster AI

**Was vorhanden sein sollte:** installierte Apps erfassen, mit Gemini clustern,
Design inspizieren und Apps gesammelt verwalten/deinstallieren.

**Was Gemini tatsächlich implementierte:**

- Package-Inventar und Package-Change-Receiver;
- Room-Cache für App-Metadaten;
- Retrofit-Gemini-Request zur Batch-Kategorisierung;
- Cluster-, Detail- und Design-Inspector-Compose-Oberflächen;
- Standard-Uninstall-Intent;
- Generator für `pm uninstall --user 0`-Skripte und Launcher für
  Termux/Shizuku.

**Lücken im Original:** keine direkte Shizuku-API-Ausführung, sondern vor allem
kopierbare Shell-Kommandos; Designanalyse basiert überwiegend auf erfassten
Metadaten/UI; API-Key- und Modellkonfiguration sind Prototype-Code.

**In AXOLOTL:** Ein echtes Apps-Basismodul ist aktiv. Es inventarisiert die vom
PackageManager gemeldeten Launcher-Apps, sucht nach Label oder Package, startet
Apps und öffnet bei langem Druck die echten Android-Appdetails. Gemini-Cluster,
Designanalyse und privilegierte Batch-Deinstallation werden nicht behauptet,
solange ihre sichere Implementierung fehlt.

### 3. Everything Files

**Was vorhanden sein sollte:** Everything-artiger Index, Volltextsuche,
Versionen/Shadow Volumes, Deduplizierung, Papierkorb, Vault, Editor sowie
Remote-/Netzwerkdateien.

**Was Gemini tatsächlich implementierte:**

- Room-Index und Suche über Dateiname und Content-Snippet;
- Scan-/Hash-/Versionslogik für zugängliche lokale Dateien;
- einfache Deduplizierungs- und Versionsbereinigung;
- Listen/Grid-/Kategorie-/Sortier-UI und Versionsdialog;
- Texteditor-UI und Borderline-Deep-Link-Adapter.

**Mocks bzw. gefährliche Lücken im Original:** Vault-Dateien, Passwort
`master123`, Recovery-Antwort, Papierkorb, FTP-Status und LAN-Geräte sind
In-Memory-Beispieldaten; „Verschlüsselung“, FTP, SMB-Verbindung und LAN-Scan
werden nur behauptet/simuliert. Der Indexer erzeugt in kleinen Sandboxes sogar
Mock-Dateien und seine Löschlogik braucht vor einer Übernahme ein separates
Sicherheitsaudit.

**In AXOLOTL:** Der unsichere Mock-Komplex wurde nicht übernommen. Stattdessen
ist ein echtes, bewusst kleines Files-Modul aktiv: Nutzer wählen über Androids
Storage Access Framework einen DocumentProvider-Ordner, AXOLOTL übernimmt die
persistierbare URI-Freigabe und liest dessen wirkliche Einträge. Suche,
Versionierung und Index folgen erst, wenn sie auf dieser realen Datenquelle
implementiert sind.

### 4. Evolver Engine

**Was vorhanden sein sollte:** generative, selbstheilende UI mit mehreren
LLM-Providern, Snapshots, Integritätsanzeige, Logs und Rollback.

**Was Gemini tatsächlich implementierte:**

- Retrofit-Aufrufe für Gemini und OpenAI-kompatible Endpunkte;
- LLM-Konfiguration und Speicherung;
- JSON-`UiSchema` mit Allowlist-Renderer für definierte Komponenten;
- bis zu drei Parse-/Repair-Versuche;
- Room-Snapshots, Runtime-Logs und Rollback auf letzten Snapshot;
- Konfigurations-, Historien-, Integrity- und Log-UI.

**Lücken im Original:** „Self-healing“ repariert nur LLM-generiertes JSON;
Integrität ist kein kryptographischer Code-Nachweis; API-Keys werden in der
lokalen DB gespeichert; Schema-Validierung und Aktionsfreigaben sind zu schwach
für privilegierte Funktionen. Es modifiziert keinen nativen App-Code.

**In AXOLOTL:** Das ursprüngliche Netzwerk-/Repository-System ist nicht
übernommen. Stattdessen ist sein sicheres Prinzip neu implementiert: Module
melden Manifest, Fähigkeiten und erlaubte Aktionen an; Evolver akzeptiert nur
declarative Allowlist-Knoten und kann Revisionen zurückrollen. Ein LLM-Adapter
und persistente Snapshots sind bewusst noch nicht angeschlossen.

### 5. Borderline / Hyperdock

**Was vorhanden sein sollte:** vier systemweite Edge-Menüs für Apps, Snippets,
Clipboard und Handoffs/Quick Actions.

**Was Gemini tatsächlich implementierte:** der größte Teil des sichtbaren
Prototyps: Overlay-Service, vier Menüs, App-Auswahl, Room-Snippets,
Clipboard-History, Appendix, Quick-Action-Heuristiken, Handoffs, Einstellungen,
Shortcuts und Logcat-UI.

**Lücken im Original:** keine echte AI-Anbindung, mehrere wirkungslose
Einstellungen, unverschlüsselte Clipboard-Daten, destruktive DB-Migration,
privilegierte Rechte und unsichere externe Schreib-Einstiegspunkte.

**In AXOLOTL:** **übernommen und gehärtet**. Namespace/App-Shell wurden geändert,
ungenutzte AI-/Netzwerkabhängigkeiten entfernt, interne Komponenten nicht mehr
exportiert und externe schreibende Deep Links gestrichen. Die verbleibenden
Plattform- und Datenschutzlücken stehen in `AUDIT.md`.

### 6. Nexus ModuBrowser

**Was vorhanden sein sollte:** modularer Browser, LLM-Radar, NotebookLM-Export,
GitHub-Sync/LFS, virtuelles Clipboard-Laufwerk und lokaler MCP-Server.

**Was Gemini tatsächlich implementierte:**

- echten Multi-Tab-WebView-Browser;
- JavaScript-Injektion zur Erkennung ausgewählter LLM-Webseiten;
- LLM-Statusoverlay, Minigames und Auto-Reply-JavaScript;
- Tab-/Modulverwaltung und Linklistenexport;
- Dialoge für NotebookLM, GitHub, Virtual Drive und MCP.

**Mocks im Original:** MCP ist nur ein Boolean plus statische Toolliste, kein
SSE-/HTTP-Server; GitHub/LFS hat keinen GitHub-Client; NotebookLM-Aktionen zeigen
vor allem Erfolgsmeldungen; Virtual Drive lebt nur im RAM und ist kein Android
DocumentsProvider; „Zero RAM modules“ sind normale In-Memory-Objekte. Außerdem
ist die JavaScript-Injektion sicherheitlich nicht ausreichend isoliert.

**In AXOLOTL:** Ein echtes Browser-Basismodul ist aktiv. Es lädt ausschließlich
HTTP/HTTPS und startet mit deaktiviertem JavaScript, DOM Storage sowie Datei-
und Content-Zugriff. Die vorgetäuschten MCP-, GitHub-, NotebookLM- und
Virtual-Drive-Funktionen wurden nicht übernommen und werden erst sichtbar,
wenn reale Backends und Tests existieren.

## Sollbild versus aktueller Stand

| Produktbereich | Soll aus den Prototypen | Heute in AXOLOTL |
| --- | --- | --- |
| Gemeinsamer Rahmen | Borderline überall verfügbar | Grundkern vorhanden |
| AI-Gateway | BELLOWS Router + Memory | Nicht vorhanden |
| PWA-Erweiterungen | BELLOWS HTML-Module | Nicht vorhanden |
| Apps | Cluster, Inspector, Batch-Aktionen | Reales Inventar/Suche/Öffnen vorhanden; Cluster/Batch folgen |
| Files | Index, Suche, Versionen | Reale SAF-Navigation/Dateiöffnung vorhanden; Index/Versionen folgen |
| Vault/Remote Files | Sicherer Vault, SMB/FTP | Auch im Original nur Mock; nicht übernehmen |
| Browser | WebView-Tabs + sichere Module | Gehärtete Single-WebView vorhanden; Tabs/Module folgen |
| MCP/GitHub/Notebook | reale Integrationen | Im Original überwiegend Mock; nicht vorhanden |
| Generative UI | Evolver-Schema + Review/Rollback | Sicherer Runtime-Kern vorhanden; Renderer/LLM/Persistenz folgen |
| Selbstmodifikation | kontrollierte Erweiterbarkeit | Modul-/Datenpatches vorhanden; beliebige Codeausführung ausgeschlossen |

## Nächster sinnvoller Integrationsschritt

Nicht Evolver oder PWA zuerst, sondern BELLOWS als testbares `AiGateway` hinter
einer klaren Sicherheitsgrenze. Danach kann genau **ein** vertikaler Pfad aus
App Cluster oder Files integriert werden. PWA-/Evolver-Module sollten erst
folgen, wenn Origin-Isolation, Schema-Allowlist, Bestätigung, Signierung und
Rollback getestet sind.
