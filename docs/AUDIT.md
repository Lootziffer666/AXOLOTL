# Implementierungs- und Übernahmeaudit

Stand: 6. August 2026 · Basis: `hyperdock (1).zip`

## Kurzfazit

Gemini hat für Borderline einen breiten **funktionsfähigen Prototypkern** erzeugt:
eine Compose-Konfigurationsoberfläche, einen echten Android-Overlay-Service,
Room-Persistenz, App-Auswahl, Snippets, Clipboard-Verlauf, heuristische
Quick-Actions, Handoffs und einen Logcat-Viewer. Das ist mehr als ein UI-Mock.
Es ist aber noch kein produktionsreifes System: AI-Aufrufe fehlen vollständig,
mehrere Einstellungen sind nur teilweise verdrahtet, sensible Daten liegen
unverschlüsselt, Migrationen löschen Daten und Android-/Play-Richtlinien sind
noch nicht abschließend berücksichtigt.

## Was Gemini im Hyperdock-Prototyp tatsächlich umgesetzt hatte

### Tatsächlich implementiert

| Bereich | Befund | Nachweis im migrierten Code |
| --- | --- | --- |
| Overlay | Foreground-Service mit vier Compose-Edge-Handles und animierten Panels | `DockOverlayService` |
| App-Dock | Liest Launcher-Apps, speichert Auswahl und startet Packages | `DockViewModel`, `DockItemsTab` |
| Snippets | Room-CRUD, Favoriten-Sortierung, Default-Inhalte, Copy/Share | `SnippetEntity`, `SnippetDao`, `DockViewModel` |
| Clipboard+ | Listener, Klassifizierung als Text/Markdown/Link, maximal 50 sichtbare Einträge, Löschen | `DockOverlayService`, `ClipDao` |
| Appendix | Hängt Clipboard-Texte als Markdown-Liste in SharedPreferences an | `SettingsManager` |
| Quick Actions | Lokale Regex-Erkennung für URL, E-Mail, IBAN, Telefon, Adresse und Stacktraces | `QuickActionHelper` |
| Handoffs | Android-Intents für Share, Browser-Suche, Maps sowie AI-App-Chooser | `HandoffHelper` |
| Diagnostik | Liest ausschließlich die von Android für die eigene App freigegebenen Logs | `LogReaderHelper` |
| Konfiguration | Position, Größe, Opazität, Edge, Privacy-/Emergency-/Appendix-Schalter | `DockSettings`, `BorderlineActivity` |
| Integration | Custom URI, Broadcast-Receiver und Shortcut-Activity öffnen Overlay-Menüs | `ShortcutActivity`, `BorderlineShortcutReceiver` |

### Nur angedeutet oder unvollständig

- **Keine LLM-Integration:** Trotz Firebase-/Retrofit-/Moshi-Abhängigkeiten gibt
  es in Hyperdock keinen API-Client und keinen Modellaufruf. „Send to AI“ ist nur
  ein Android Share-Intent zu einer installierten App.
- **Kein echter Sensitive-App-Schutz:** `sensitiveAppMode` wird gespeichert und
  in der UI angezeigt, aber der Overlay-Service prüft nicht, welche App gerade
  im Vordergrund ist.
- **Control Modes unvollständig:** `GESTURE`, `ACTION_BUTTON` und `MULTI_HANDLE`
  werden persistiert; der Service rendert dennoch immer dieselben vier Handles.
- **Position und Opazität teilweise wirkungslos:** `positionY`, `barOpacity` und
  `iconOpacity` werden konfiguriert, aber die aktuelle Overlay-Anordnung nutzt
  sie nicht vollständig.
- **Private Mode ist keine zusätzliche Authentifizierung:** Er stoppt neue
  Clipboard-Aufnahmen; die Room-Datei ist inzwischen vollständig mit SQLCipher
  und einer per Android Keystore geschützten Zufallspassphrase verschlüsselt.
  Eine separate biometrische Zugriffssperre innerhalb der laufenden App fehlt.
- **Clipboard-Historie ist opt-in und verschlüsselt:** Duplikate werden ersetzt,
  Einträge auf 100.000 Zeichen begrenzt und nicht angeheftete Einträge nach
  30 Tagen oder oberhalb von 50 Datensätzen gelöscht. Bestehende Klartextbanken
  werden vor dem Öffnen atomar exportiert und erst nach Integritätsprüfung
  ersetzt.
- **IBAN wird nicht validiert:** Der Text „Validated IBAN“ basiert nur auf einem
  Regex-Match; eine Mod-97-Prüfung fehlt.
- **Clipboard-Diagnose bleibt grob:** Einschränkungen des Background-Clipboard
  werden nun im Control Center angezeigt, aber noch nicht nach genauer
  Android-Ursache oder Herstellerverhalten aufgeschlüsselt.
- **Historische Migrationen nicht rekonstruierbar:** Der destruktive Fallback
  wurde entfernt und Schema 2 als versionierte Ausgangsbasis exportiert. Der
  Originalexport enthielt ebenfalls nur Version 2; deshalb wird keine
  spekulative Migration von einer unbekannten Version 1 implementiert.

## Was in AXOLOTL übernommen wurde

Übernommen wurde der Android-Quellkern von Hyperdock:

- Borderline Control Center und Dark/Violet Compose-Theme;
- `DockOverlayService` und `ServiceLifecycleOwner`;
- Room-Entities, DAOs, Datenbank und Repository für Dock, Clips und Snippets;
- Settings/ViewModels;
- Shortcut-, Handoff-, Quick-Action- und Logcat-Helfer;
- Manifest-Rechte, Launcher-Ressourcen und Tests als Ausgangsbasis.

Der Code wurde von `com.example` nach `app.axolotl` migriert. Borderline ist nun
nicht mehr die Launcher-App selbst, sondern das Control Center hinter der neuen
AXOLOTL-Shell. Diese Shell führt die künftigen Bereiche Apps, Files, Browser,
Automate sowie AI & Models und öffnet Borderline als gemeinsamen Rahmen.

## Was bewusst geändert oder gestrichen wurde

| Änderung | Grund |
| --- | --- |
| Neue `MainActivity` als AXOLOTL-Shell; alte UI heißt `BorderlineActivity` | Ein gemeinsamer Produkteinstieg statt sechs separater Apps |
| Namespace/Application-ID `app.axolotl` | Keine AI-Studio-Beispielidentität im Produkt |
| AGP 9.1.1 → 8.7.3, Kotlin 2.0.21, systemweites Gradle 8.9+ | Existierende, miteinander kompatible Toolchain statt nicht auflösbarer Prototype-Version |
| AI-Studio Release-/Debug-Signing-Konfiguration entfernt | Keine lokalen Keystore-Annahmen oder Klartext-Debug-Credentials im Build |
| Unbenutzte Firebase-, Retrofit-, OkHttp-, Moshi- und Secrets-Abhängigkeiten entfernt | Hyperdock hatte dafür keinen ausführenden Code; kleinere und ehrlichere Angriffsfläche |
| Overlay-Service und Broadcast-Receiver nicht exportiert | Andere Apps dürfen keine internen Komponenten direkt starten oder Daten schreiben |
| `READ_LOGS` und `QUERY_ALL_PACKAGES` entfernt | Nur app-sichtbare Logs sowie gezielte Launcher-/Modul-Abfragen statt privilegierter Sichtbarkeit |
| Externe `borderline://open`-Links bestätigt | Browser und Fremd-Apps dürfen das Overlay nicht mehr ohne Nutzerfreigabe öffnen |
| Schreibende `borderline://add-snippet`-/`appendix`-Deep-Links entfernt | Beliebige Webseiten/Apps hätten sonst unbestätigt lokale Inhalte verändern können |
| AI-Studio-Metadaten, generisches README, `.aistudio`, `.env` und Signing-Artefakte nicht in die App kopiert | Generierter Ballast und Secret-Risiko |
| Screenshot-/App-Namens-Tests auf AXOLOTL angepasst | Alte Tests referenzierten eine nicht vorhandene `Greeting`-Composable bzw. „My Application“ |

Der Name des inzwischen entfernten Quell-ZIPs bleibt im Provenienzkatalog
dokumentiert; das Archiv ist nicht Teil des Repositorys oder Android-Builds.

## Offene Risiken, priorisiert

### P0 – vor Verteilung

1. Foreground-Service-Startrestriktionen auf realen Android-13–15-Geräten und
   relevanten Hersteller-ROMs testen; Manifest-/Notification-Verträge sind per
   Robolectric abgesichert.
2. Den nativen Klartext-zu-SQLCipher-Instrumentationstest in der Geräte-CI auf
   allen unterstützten ABIs ausführen.
3. Bei jeder künftigen Schemaänderung eine explizite Room-Migration samt
   Migrationstest gegen die versionierten Schemas schreiben.
4. Falls künftig HTTPS-Deep-Links hinzukommen, Android App Links mit verifizierter
   Domain verwenden; der bestehende Custom-Scheme-Link verlangt Bestätigung.

### P1 – bevor weitere Features andocken

1. Borderline-Zustand aus Activity/Service in gemeinsame Core-Repositories
   verschieben und per Dependency Injection bereitstellen.
2. UI-Einstellungen entweder vollständig verdrahten oder entfernen.
3. Status und Diagnose für moderne Android-Hintergrundrestriktionen des
   Clipboards implementieren.
4. Quick-Action-Erkennung als reine, umfassend getestete Domainlogik auslagern;
   IBAN korrekt validieren.
5. Service- und Receiver-Lifecycle, Prozessneustart und Emergency-Off mit
   Instrumentation-Tests absichern.

## Reproduzierbare Audit-Methode

- `unzip -l 'hyperdock (1).zip'` inventarisiert den Originalexport, ohne ihn in
  den Build zu übernehmen.
- `rg` über Imports, Manifest, Room, Intents, Berechtigungen und TODOs trennt
  tatsächlich ausführenden Code von Labels und deklarierten Dependencies.
- `scripts/verify_prototypes.py` validiert die sechs Provenienzeinträge und
  stellt sicher, dass keine Prototype-ZIP-Dateien eingecheckt sind.
- Gradle-Tests sind der verbindliche Buildcheck; Netzwerk-/SDK-Einschränkungen
  müssen als solche ausgewiesen und dürfen nicht als bestandener Test behauptet
  werden.
