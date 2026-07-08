# AntSKIP

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-active-blue)
![License](https://img.shields.io/badge/license-private-lightgrey)

AntSKIP is a lightweight Android accessibility app that automatically taps
supported "skip" buttons in selected streaming apps. The first supported profile
is Crunchyroll, with Netflix and Prime Video prepared as disabled provider
profiles for future app-specific tuning.

## Goals

- Skip intros, recaps and credits with minimal overhead.
- Avoid overlays, screen capture, timers and background polling.
- Keep all behavior configurable from the app UI.
- Make provider support easy to extend without changing the accessibility core.

## Technologies

- Kotlin
- Android Accessibility Service
- Native Android Views
- Gradle Android Plugin
- No runtime third-party dependencies

## Current Support

| Provider | Android package | Default | Notes |
| --- | --- | --- | --- |
| Crunchyroll | `com.crunchyroll.crunchyroid` | Enabled | Primary target |
| Netflix | `com.netflix.mediaclient` | Disabled | Profile prepared |
| Prime Video | `com.amazon.avod.thirdpartyclient` | Disabled | Profile prepared |

## Skip Actions

| Action | Default | Examples |
| --- | --- | --- |
| Intro/opening | Enabled | `Pular abertura`, `Skip Intro`, `Skip Opening` |
| Recap/summary | Enabled | `Pular resumo`, `Skip Recap` |
| Credits/ending | Enabled | `Pular creditos`, `Skip Credits`, `Skip Ending` |
| Preview | Disabled | `Skip Preview` |
| Next episode | Disabled | `Next Episode`, `Proximo episodio` |

## Architecture

```text
app/src/main/kotlin/com/artur/antskip
├── accessibility/
│   └── AntSkipAccessibilityService.kt
├── data/
│   └── PreferenceStore.kt
├── domain/
│   ├── SkipAction.kt
│   └── StreamingProvider.kt
├── matcher/
│   ├── NodeText.kt
│   ├── SkipMatcher.kt
│   ├── SkipPhraseBank.kt
│   └── TextNormalization.kt
└── ui/
    └── MainActivity.kt
```

### Responsibility Split

- `accessibility`: receives Android accessibility events and delegates matching.
- `data`: persists user settings in `SharedPreferences`.
- `domain`: defines providers and skip action types.
- `matcher`: extracts accessible node text, normalizes labels and finds clickable nodes.
- `ui`: native settings screen for providers and skip actions.

## Performance Model

AntSKIP is intentionally event-driven:

- No screen recording or screenshot analysis.
- No floating overlay.
- No continuous polling loop.
- Only receives events from configured streaming package names.
- Scans at most 300 accessibility nodes per event.
- Uses a 3 second cooldown after a successful tap.

## Build

Open the project directory in Android Studio:

```powershell
cd C:\Users\artur\AntSKIP
```

Then run:

```powershell
gradle :app:assembleDebug
```

If using Android Studio, select `app` and run the debug build from the IDE.

## Install And Enable

1. Build and install the debug APK on the Android phone.
2. Open AntSKIP.
3. Tap `Abrir acessibilidade`.
4. Enable the `AntSKIP` accessibility service.
5. Keep Crunchyroll enabled and choose the skip actions you want.

Android requires accessibility permission to be enabled manually by the user.

## Extending Providers

To add or tune a provider:

1. Add the Android package to `StreamingProvider.kt`.
2. Add the package to `res/xml/ant_skip_accessibility_service.xml`.
3. Add provider-specific labels or view IDs to `SkipPhraseBank.kt`.
4. Keep risky actions disabled by default until tested on-device.

## Repository Workflow

Recommended commit style:

```text
feat: add provider profile
refactor: isolate skip matcher
docs: document setup and provider model
```

## Safety Notes

AntSKIP only automates taps on accessibility nodes exposed by selected apps. It
does not modify streaming apps, bypass DRM, inspect video frames or interact with
network traffic.
