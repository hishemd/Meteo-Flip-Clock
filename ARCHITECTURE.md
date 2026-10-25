# HTC Diamond Weather Clock – Architecture

```
app/
 ├── data/
 │    ├── remote/     (API – OpenWeatherMap)
 │    ├── local/      (Room database)
 │    ├── model/
 │    └── WeatherRepository.kt
 │
 ├── domain/          (business helpers – prefs, location)
 │
 ├── ui/
 │    ├── MainActivity, Settings, Cities
 │    ├── FlipClockView (clock)
 │    ├── animation/ (weather background + icons)
 │    └── widget/ (4 home screen widgets)
 │
 ├── service/         (Live Wallpaper)
 │
 └── viewmodel/
```

## Specs
- Name: HTC Diamond Weather Clock
- Languages: Français / English / العربية
- Min SDK: 26
- Target SDK: 35
- Size target: 10–20 MB

## Flip Digit (app)
Custom View with smooth vertical flip animation (not possible inside App Widgets).

## Widgets (4)
1. Flip Clock + Weather (large)
2. Flip Clock only (extra large)
3. Compact weather
4. Tall vertical
