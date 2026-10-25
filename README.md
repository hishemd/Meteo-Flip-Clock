# Météo Flip Clock – Style HTC Diamond / Sense

Application Android fidèle au widget météo + Flip Clock classique des téléphones HTC (Diamond, Desire, etc.).

## Fonctionnalités

- **Horloge Flip Clock** animée (style HTC)
- **Météo animée** : soleil avec rayons, pluie, nuages, neige, orage…
- **Fond dynamique** synchronisé avec la condition météo
- Interface entièrement en **français**
- **Plusieurs villes** (ajout / suppression)
- Mode démo (fonctionne sans clé API)

## Comment générer l’APK (très simple)

### 1. Prérequis
- Android Studio (Hedgehog ou plus récent recommandé)
- JDK 17

### 2. Ouvrir le projet
1. Lancez Android Studio
2. **File → Open** → sélectionnez le dossier `HTCWeatherFlipClock`
3. Attendez que Gradle synchronise (première fois peut prendre 1-2 min)

### 3. (Optionnel) Clé OpenWeatherMap
- Allez sur https://openweathermap.org/api et créez un compte gratuit
- Copiez votre clé API
- Ouvrez `app/src/main/java/com/htcweather/flipclock/data/remote/RetrofitClient.kt`
- Remplacez `YOUR_OPENWEATHERMAP_API_KEY` par votre clé

> Sans clé, l’application utilise un mode démo (météo fictive + quelques villes françaises).

### 4. Générer l’APK
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Une fois terminé, cliquez sur **locate**
3. L’APK se trouve dans :  
   `app/build/outputs/apk/debug/app-debug.apk`

Vous pouvez installer cet APK sur n’importe quel téléphone Android (autorisez les sources inconnues).

### 5. APK de release (signé)
1. **Build → Generate Signed Bundle / APK**
2. Créez un keystore (ou utilisez un existant)
3. Choisissez **APK** → release

## Structure du projet

```
app/src/main/java/com/htcweather/flipclock/
├── ui/
│   ├── MainActivity.kt          ← Écran principal (horloge + météo)
│   ├── CityListActivity.kt      ← Liste des villes
│   ├── AddCityActivity.kt       ← Recherche & ajout
│   └── FlipClockView.kt         ← Vue personnalisée Flip Clock
├── animation/
│   ├── WeatherBackgroundView.kt ← Fond animé plein écran
│   └── WeatherIconView.kt       ← Icône météo animée
├── data/
│   ├── model/                   ← Modèles (Weather, City…)
│   ├── local/                   ← Room Database
│   ├── remote/                  ← Retrofit + OpenWeatherMap
│   └── WeatherRepository.kt
└── viewmodel/
    ├── MainViewModel.kt
    └── CityViewModel.kt
```

## Personnalisation rapide

- Changer la ville par défaut : `MainViewModel.kt` (lat/lon de Paris actuellement)
- Modifier les couleurs : `res/values/colors.xml`
- Ajuster la vitesse des animations : dans les classes `WeatherBackgroundView` et `WeatherIconView`

## Notes techniques

- Min SDK 24 (Android 7.0)
- Target SDK 34
- Kotlin + ViewBinding + Room + Retrofit + Coroutines
- Pas de Google Play Services requis

Bon téléchargement et bon usage !
