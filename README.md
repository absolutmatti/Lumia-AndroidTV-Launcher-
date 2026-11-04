# Lumia Launcher 🌌

> **Ein ruhiger, moderner Android-TV-Launcher mit klarem Fokus auf Performance, Eleganz und perfekte D-Pad-Bedienung.**

Lumia Launcher setzt auf ein konsequentes **Ein-Seiten-Prinzip**: Keine verschachtelten Tabs, keine überladenen Menüs und keine ablenkende Werbung. Auf einen Blick stehen die wichtigsten Favoriten und darunter eine aufgeräumte, alphabetisch sortierte 16:9-Kartenwand aller installierten Anwendungen zur Verfügung.

---

## ✨ Highlights

* **🎯 Ein-Seiten-Prinzip:**
  * **Statusleiste:** Immer sichtbare Statusleiste mit Schnellzugriff auf Einstellungen (Zahnrad), WLAN-Statusanzeige, formatiertem Datum und präziser Uhrzeit.
  * **Favoriten-Panel:** Genau 6 Plätze für die meistgenutzten Lieblings-Apps direkt im Sichtfeld.
  * **App-Grid:** 6-spaltige, nach unten scrollbare 16:9-Kartenwand aller weiteren Apps – streng alphabetisch sortiert.
* **⚡ 100% Offline & Privat:**
  * Keinerlei Netzwerkberechtigung für Launcher-Funktionen erforderlich (nur `ACCESS_NETWORK_STATE` für das WLAN-Symbol).
  * Keine Analyse-Tracker, keine Cloud-Abhängigkeit, keine Benutzerkonten.
* **🏎️ Maximale Performance (60 FPS auf TV-Hardware):**
  * Hintergrund-Caching aller App-Icons und Leanback-Banner als skalierte Bitmaps.
  * Kein Ruckeln beim Scrollen: Keine schweren IPC-Aufrufe an den `PackageManager` während des Bildlaufs.
  * Sanfte, organische Animationen (Spring-Physik beim Fokuswechsel, weiche Überblendungen).
* **🎨 Ruhiges, stimmiges Design:**
  * Konstantes Hintergrundbild (mitgeliefertes Standardbild oder eigenes Foto) mit dynamischer Scrim-Abdunklung (25 % im oberen Bereich, 55 % beim Eintauchen ins Grid).
  * Kein störender Blur-Effekt – klare Kontraste und hervorragende Lesbarkeit auf jeder Displaygröße (1080p und 4K).
  * Automatische Extraktion harmonischer Farbpaletten aus den App-Logos (oder umschaltbar auf einheitlich edles Dunkelgrau).
* **🎮 Perfekte Fernbedienungs-Steuerung (D-Pad):**
  * Entwickelt für Android TV / Google TV Fernbedienungen.
  * Vollständig per D-Pad navigierbar mit klarem Fokus-Indikator (3 dp weißer Glow-Border & Skalierung).
  * Kontextmenü per langem Druck auf die OK-Taste oder Menü-Taste.
  * Zuverlässiges HOME- und Zurück-Tasten-Verhalten.

---

## 🎮 Steuerung mit der Fernbedienung

| Taste / Aktion | Funktion |
|---|---|
| **D-Pad (Navigation)** | Wechselt den Fokus zwischen Statusleiste, Favoriten und App-Karten. |
| **OK (kurz)** | Startet die fokussierte Anwendung oder öffnet den Menüpunkt. |
| **OK (lang gehalten, ≥ 500 ms)** | Öffnet das **Kontextmenü** für die fokussierte App. |
| **Menü-Taste (`KEYCODE_MENU`)** | Öffnet ebenfalls direkt das Kontextmenü der fokussierten App. |
| **Zurück (`BACK`)** | Schließt geöffnete Overlays (Einstellungen/Kontextmenü). Befindet man sich im App-Grid, springt die Ansicht sanft nach oben auf die Favoriten. |
| **Home (`HOME`)** | Schließt alle Overlays und kehrt sofort zur Startansicht zurück. |

### Kontextmenü-Aktionen
* ⭐ **Zu Favoriten hinzufügen / Aus Favoriten entfernen**
* ℹ️ **App-Info** (öffnet die Android-Systemdetails zur App)
* 🗑️ **Deinstallieren** (nur bei Drittanbieter-Apps)
* 👁️‍🗨️ **Ausblenden** (entfernt Apps aus dem Grid; kann in den Einstellungen jederzeit rückgängig gemacht werden)

---

## ⚙️ Einstellungs-Overlay

Über das Zahnrad-Symbol in der Statusleiste erreichbar:

1. **Allgemein:**
   * **Zeitformat:** Systemstandard, 12-Stunden oder 24-Stunden.
   * **Datum anzeigen:** Schalter zum Ein- oder Ausblenden der Datumsanzeige.
   * **Startfokus (HOME-Verhalten):** Immer zu Favoriten springen oder letzte Position beibehalten.
2. **Darstellung:**
   * **Hintergrundbild:** Eigenes Bild über System-Dateiwähler oder integrierten Bild-Picker auswählen bzw. auf Standard zurücksetzen.
   * **Kartenfarben:** *Dynamisch* (aus Logo-Palette berechnet) oder *Einheitlich dunkel*.
3. **Apps:**
   * **Ausgeblendete Apps:** Übersicht aller versteckten Apps mit 1-Klick-Wiedereinblenden.
   * **Favoriten zurücksetzen:** Setzt die 6 Favoriten-Plätze mit Sicherheitsabfrage zurück.
4. **System:**
   * Schneller Direktzugriff auf System-WLAN, Bildschirmeinstellungen, Ton, Haupteinstellungen und Standard-Launcher-Auswahl.
5. **Über:**
   * Versionsinformationen und Open-Source-Lizenzen.

---

## 📥 Installation

Die vorkompilierte, installationsfertige APK befindet sich direkt im Hauptverzeichnis des Repositories:

### 1. Installation per ADB (Android Debug Bridge)
Stelle sicher, dass USB-Debugging bzw. Netzwerk-Debugging auf deinem Android TV / Fire TV aktiviert ist:

```bash
# Per WLAN verbinden (Beispiel-IP deines Fernsehers)
adb connect 192.168.1.100:5555

# Lumia Launcher installieren
adb install -r lumia-launcher.apk
```

### 2. Installation per USB-Stick oder Dateimanager
1. Lade `lumia-launcher.apk` auf einen USB-Stick herunter.
2. Stecke den Stick in deinen Android TV / TV-Box.
3. Öffne einen Dateimanager auf dem TV (z. B. *X-plore* oder *File Commander*) und installiere die APK.

---

## 🛠️ Technologie & Architektur

* **UI & Framework:** 100% Jetpack Compose (Material 3 for TV)
* **Sprache:** Kotlin (Coroutines, StateFlow)
* **Architektur:** MVVM (Model-View-ViewModel) mit unidirektionalem Datenfluss
* **Datenbank:** Android Jetpack Room (Persistenz für Favoriten und ausgeblendete Apps)
* **Präferenzen:** Jetpack DataStore Preferences
* **Bilder & Farbanalyse:** Coil Image Loader & `androidx.palette`
* **TV-Features:** Leanback Launcher Intent Filter, TV-Banner, Hardware-Feature `android.software.leanback`

---

## 📄 Lizenz

Dieses Projekt ist unter der **Apache 2.0 Lizenz** veröffentlicht.
