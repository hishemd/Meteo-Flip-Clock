# HTC Diamond Weather Clock

Application Android (Kotlin) inspirée de **HTC Sense / Diamond** :
horloge Flip Clock, météo animée, widgets, fond d’écran live.

## Fonctionnalités

- Flip Clock mécanique (Blanc nacré / Noir carbone / Givré 3D)
- Fonds animés : soleil, pluie, orage, neige, nuages, brume (jour & nuit)
- Live Wallpaper synchronisé avec la météo
- 4 widgets écran d’accueil
- GPS + multi-villes
- Prévisions quotidiennes & horaires
- Paramètres complets (°C/°F, GPS, son/vibration, thèmes)
- Langues : Français, English, العربية
- Mode hors-ligne pour tester tous les fonds

## Build

```bash
./gradlew assembleDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`

Ou via GitHub Actions → artifact **Meteo-Flip-Clock-APK**.

## Configuration

Clé OpenWeatherMap déjà intégrée (modifiable dans Paramètres).

## Structure

Voir `ARCHITECTURE.md`.

Min SDK 26 · Target SDK 35 · Kotlin · ViewBinding · Room · Retrofit
