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
| Logcat | Liest/leert `logcat` nach separat per ADB gewährtem `READ_LOGS` | `LogReaderHelper` |
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
- **Private Mode ist kein Vault:** Er stoppt neue Clipboard-Aufnahmen, schützt
  aber bereits gespeicherte Clips/Snippets weder durch Verschlüsselung noch
  durch Authentifizierung.
- **Clipboard-Limit ist nur ein Query-Limit:** Die DAO zeigt 50 Clips, löscht
  ältere Datensätze jedoch nicht aus der Datenbank.
- **IBAN wird nicht validiert:** Der Text „Validated IBAN“ basiert nur auf einem
  Regex-Match; eine Mod-97-Prüfung fehlt.
- **Fehler werden verschluckt:** Einschränkungen des Background-Clipboard werden
  pauschal gefangen, ohne Status für Nutzer oder Diagnose.
- **Datenbankmigration:** `fallbackToDestructiveMigration()` kann Nutzerdaten bei
  Schemaänderungen löschen.

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
| Schreibende `borderline://add-snippet`-/`appendix`-Deep-Links entfernt | Beliebige Webseiten/Apps hätten sonst unbestätigt lokale Inhalte verändern können |
| AI-Studio-Metadaten, generisches README, `.aistudio`, `.env` und Signing-Artefakte nicht in die App kopiert | Generierter Ballast und Secret-Risiko |
| Screenshot-/App-Namens-Tests auf AXOLOTL angepasst | Alte Tests referenzierten eine nicht vorhandene `Greeting`-Composable bzw. „My Application“ |

Das unveränderte ZIP bleibt ausschließlich als Provenienzreferenz im Repository;
es ist nicht Teil des Android-Builds.

## Offene Risiken, priorisiert

### P0 – vor Verteilung

1. Room verschlüsseln oder Clipboard-History standardmäßig deaktivieren; klare
   Retention und echtes Löschen ergänzen.
2. `READ_LOGS` und `QUERY_ALL_PACKAGES` auf Produktnotwendigkeit/Play-Policy
   prüfen; wenn möglich durch engere APIs ersetzen.
3. Foreground-Service-Typ, Notification-Permission und Startrestriktionen auf
   Android 13–15 auf Geräten testen.
4. Explizite Room-Migrationen statt destruktivem Fallback schreiben.
5. Deep Links mit Android App Links oder bestätigender UI absichern.

### P1 – bevor weitere Features andocken

1. Borderline-Zustand aus Activity/Service in gemeinsame Core-Repositories
   verschieben und per Dependency Injection bereitstellen.
2. UI-Einstellungen entweder vollständig verdrahten oder entfernen.
3. Clipboard-Deduplizierung, Größenlimit und Hintergrundrestriktionsstatus
   implementieren.
4. Quick-Action-Erkennung als reine, umfassend getestete Domainlogik auslagern;
   IBAN korrekt validieren.
5. Service- und Receiver-Lifecycle, Prozessneustart und Emergency-Off mit
   Instrumentation-Tests absichern.

## Reproduzierbare Audit-Methode

- `unzip -l 'hyperdock (1).zip'` inventarisiert den Originalexport, ohne ihn in
  den Build zu übernehmen.
- `rg` über Imports, Manifest, Room, Intents, Berechtigungen und TODOs trennt
  tatsächlich ausführenden Code von Labels und deklarierten Dependencies.
- `scripts/verify_prototypes.py` stellt sicher, dass nur die sechs vereinbarten
  Prototype-Archive als Referenzen vorliegen.
- Gradle-Tests sind der verbindliche Buildcheck; Netzwerk-/SDK-Einschränkungen
  müssen als solche ausgewiesen und dürfen nicht als bestandener Test behauptet
  werden.
