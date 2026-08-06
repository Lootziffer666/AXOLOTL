# Optionale AXOLOTL-Module

AXOLOTL setzt ausschließlich den Borderline-Rahmen voraus. Alle anderen
Funktionen werden zur Laufzeit als Android-Activities entdeckt. Ein Modul kann
im selben APK, in einem Dynamic-Feature-Build oder in einem separat installierten
APK liegen; die Shell hat keine Klassenreferenz auf seine Activity.

## Discovery-Vertrag

Ein Modul veröffentlicht eine Activity mit der Action
`app.axolotl.action.MODULE` und Metadaten:

```xml
<activity android:name=".MyModuleActivity" android:exported="true">
    <intent-filter>
        <action android:name="app.axolotl.action.MODULE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <meta-data android:name="app.axolotl.module.ID" android:value="my-module" />
    <meta-data android:name="app.axolotl.module.TITLE" android:value="My Module" />
    <meta-data android:name="app.axolotl.module.DESCRIPTION" android:value="What it really does" />
    <meta-data android:name="app.axolotl.module.VERSION" android:value="1" />
    <meta-data android:name="app.axolotl.module.ICON" android:value="AUTOMATE" />
    <meta-data android:name="app.axolotl.module.CAPABILITIES" android:value="GENERATIVE_UI" />
</activity>
```

Beim Start fragt `InstalledModuleDiscovery` alle passenden Activities über den
PackageManager ab. Metadaten werden in ein `AxolotlModule` übersetzt; die
explizite `ComponentName` wird separat gehalten. Die Shell startet das Modul nur
über diese aufgelöste Komponente. Wird das APK deinstalliert oder die Activity
aus dem Manifest entfernt, erscheint das Modul beim nächsten Prozessstart nicht
mehr. Unbekannte Module sind daher kein Buildfehler und keine Voraussetzung.

## Kopieren oder separat installieren

- **Kopieren:** Activity und Manifest-Block in ein Android-Projekt übernehmen;
  eine eindeutige Modul-ID vergeben.
- **Separates APK:** denselben Intent-/Metadaten-Vertrag veröffentlichen. AXOLOTL
  entdeckt das installierte APK ohne Codeabhängigkeit.
- **Bundled:** Files, Browser, AI und Automate nutzen derzeit denselben Vertrag
  innerhalb der Shell-APK.
- **Standalone:** Apps und PWA Studio liegen als unabhängige Gradle-Projekte
  unter `modules/apps` und `modules/pwa`. Beide erzeugen eigene APKs und sind
  keine Build-Abhängigkeiten von `:app`. Nicht installierte Module erscheinen
  nicht in der Shell.

## Grenzen

Discovery erlaubt nur das Öffnen einer vom Modul exportierten Oberfläche. Sie
übergibt weder Datenbankzugriff noch privilegierte Borderline-Actions. Tieferer
Datenaustausch benötigt zukünftig versionierte, permission-geschützte
ContentProvider-/Binder-Verträge. Eine Modul-ID darf nur Kleinbuchstaben,
Ziffern und Bindestriche enthalten; doppelte IDs werden nicht registriert.

## Kompilieren

Der Hauptworkflow `.github/workflows/android.yml` führt zwei getrennte Gates
aus:

1. `verify_android_modules.py` validiert Discovery-Action, Export-Status,
   Pflichtmetadaten, eindeutige IDs und vorhandene Activity-Quellen.
2. Gradle kompiliert `:app:assembleDebug`. Weil die aktuell gebündelten Module
   Android-Quellen desselben APKs sind, werden sie dabei gemeinsam typgeprüft
   und in `app-debug.apk` paketiert. `:module-apps:assembleDebug` und
   `:module-pwa:assembleDebug` kompilieren zusätzlich die unabhängigen Modul-
   APKs. Anschließend laufen die App- und PWA-Sandbox-Tests.

## PWA Studio

Das optionale PWA-Studio-APK erstellt benannte lokale Webmodule, speichert deren
HTML ausschließlich im privaten App-Verzeichnis und führt sie unter dem
HTTPS-Ursprung `appassets.androidplatform.net` aus. JavaScript ist innerhalb
des Moduls erlaubt; eine Content-Security-Policy blockiert Netzwerkzugriffe,
externe Frames, Plugins und fremde Ressourcen. Es gibt keine JavaScript-Bridge
in native AXOLOTL-Funktionen, keinen Datei-/Content-Zugriff und kein Mixed
Content. Module können geladen, bearbeitet, ausgeführt und gelöscht werden.

Das ist bewusst ein lokaler PWA-/Webmodul-Runner. Service-Worker-Hintergrund-
ausführung und ungeprüfte Remote-Abhängigkeiten sind noch nicht freigeschaltet.

Für ein in ein eigenes Repository/APK ausgelagertes Modul steht
`.github/workflows/build-module.yml` als `workflow_call` bereit. Der aufrufende
Workflow übergibt seine Assemble-Task und optional den APK-Pfad:

```yaml
jobs:
  build:
    uses: owner/axolotl/.github/workflows/build-module.yml@main
    with:
      gradle-task: :module:assembleDebug
      artifact-path: module/build/outputs/apk/debug/*.apk
```

Beide Workflows verwenden JDK 17 und Gradle 8.9 und brechen ab, wenn kein APK
erzeugt wurde. Der Hauptworkflow lehnt außerdem binäre Bildressourcen unter
`app/src` und `modules` ab.
