# One UI Weather Icon Provider

A Breezy Weather compatible icon provider built from the current One UI weather artwork extracted from Samsung Weather 1.7.30.50.

It is also directly compatible with Pixel AOD for OPlus because the module consumes Breezy Weather `DRAWABLE_FILTER` providers.

## Source APK used for this build

- File: `Samsung Weather_1.7.30.50_apkcombo.com_anti.apk`
- SHA-256: `4E370655378BB39A3FCD391EC9B1587A77648295075E89B14103975D93E5DBF2`
- Static source artwork: `res/drawable-nodpi` and `res/drawable-night-nodpi`
- The APK also contains 280x280 Lottie weather animations under `assets/white` and `assets/dark`; this provider intentionally uses static PNG drawables because that is the common Breezy/Pixel AOD provider surface.

## Mapping

| Breezy condition | Samsung Weather artwork |
| --- | --- |
| clear day | `sunny.png` |
| clear night | `clear.png` |
| partly cloudy day | `partly_cloud.png` |
| partly cloudy night | `partly_cloud_night.png` |
| cloudy | `cloudy.png` |
| rain | `rain.png` |
| snow | `snow.png` |
| wind | `wind.png` |
| fog | `fog.png` |
| haze / dust | `sand_storm.png` |
| sleet | `rain_and_sleet.png` |
| hail | `hail.png` |
| thunder | `thunderstorm.png` |
| thunderstorm | `rain_and_thunder.png` |

Both normal and Android `night` resource variants are copied so Samsung's own light/dark palette selection is preserved.

## Build

```powershell
$env:ANDROID_HOME = 'D:\Android\Sdk'
.\gradlew.bat :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Asset licensing

The provider code is based on the LGPL-3.0 Breezy Weather One UI 2 provider structure. Samsung Weather artwork is proprietary/non-free and is not relicensed by this project. Keep extracted assets for personal/local use unless you have permission to redistribute them.
