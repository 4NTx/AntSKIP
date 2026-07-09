# AntSKIP

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-active-blue)
![License](https://img.shields.io/badge/license-private-lightgrey)

AntSKIP is a lightweight Android accessibility app that automatically taps
visible skip buttons in supported streaming apps. It is designed for the native
Android apps, not for browser extensions or modified streaming clients.

The app does not record the screen, inspect video frames, bypass DRM, inject
code into streaming apps, or interact with network traffic. It only reads
accessibility text exposed by selected apps and performs a normal accessibility
click when a matching button appears.

## Features

- Automatically skips intros/openings, recaps, credits/endings, previews, and
  next-episode prompts.
- Shows an on-screen confirmation such as `Pulando abertura` after a successful
  automatic tap.
- Lets users enable or disable each streaming app independently.
- Lets users enable or disable each skip action independently.
- Supports per-app action rules. For example, `Next episode` can be enabled for
  Netflix and disabled for Prime Video.
- Supports a per-app bedtime window for `Next episode`, so automatic episode
  advance can stop during selected hours.
- Supports one-time `Next episode` pauses per app, such as pausing for 1, 2, or
  4 hours, or until a selected time.
- Supports custom phrases per action, so users can teach the app labels from
  any language or app version.
- Normalizes accents and casing before matching. For example, `Próximo`,
  `proximo`, and `PROXIMO` are treated the same.
- Shows the installed version and checks GitHub Releases for updates.
- Includes copyable diagnostic logs for accessibility matching and click
  troubleshooting.
- Uses a cooldown after each tap to avoid repeated clicks.
- Runs event-driven through Android Accessibility instead of polling the screen.

## Supported Apps

| App | Android package | Default | Status |
| --- | --- | --- | --- |
| Crunchyroll | `com.crunchyroll.crunchyroid` | Enabled | Primary support |
| Netflix | `com.netflix.mediaclient` | Enabled | Primary support |
| Prime Video | `com.amazon.avod.thirdpartyclient` | Disabled | Restricted support |
| Disney+ | `com.disney.disneyplus` | Disabled | Experimental |
| Max | `com.wbd.stream` | Disabled | Experimental |
| Paramount+ | `com.cbs.app` | Disabled | Experimental |

Restricted support means the app uses provider-specific labels and extra click
target checks, but the provider still needs to expose accessible button text.

Crunchyroll uses stricter matching than the other providers. It only reacts to
explicit visible labels such as `PULAR INTRODUCAO`, `PULAR ABERTURA`,
`Skip Intro`, `Skip Opening`, or `Next Episode`, and rejects large clickable
containers so it does not tap anime list rows or full-screen player areas.

Netflix uses the normal phrase bank plus Android accessibility click fallbacks,
because Netflix can expose the button action on an ancestor node instead of the
text node itself.

Prime Video uses a stricter provider-specific matcher instead of the generic
phrase bank. It supports intro, recap, credits, and preview labels by default.
Next-episode prompts are available in per-app rules, but disabled by default for
Prime Video because they are easier to confuse with credits or player controls.

## Supported Skip Actions

| Action | Default | Example labels |
| --- | --- | --- |
| Intros/openings | Enabled | `Pular abertura`, `Skip Intro`, `Skip Opening` |
| Recaps/summaries | Enabled | `Pular resumo`, `Pular recapitulacao`, `Skip Recap` |
| Credits/endings | Enabled | `Pular creditos`, `Skip Credits`, `Skip Ending` |
| Previews | Disabled | `Pular previa`, `Skip Preview`, `Skip Trailer` |
| Next episode | Enabled | `Proximo`, `Proximo episodio`, `Next Episode`, `Play Next` |

The built-in phrase bank includes common labels in Portuguese, English, Spanish,
French, German, Italian, Dutch, Polish, Japanese, Korean, Chinese, and Turkish.
Because streaming apps frequently change their UI text, the in-app custom phrase
editor is the recommended fallback for missing languages or regional variants.

## Install And Enable

For manual installation from GitHub, download the signed APK from the latest
release:

```text
AntSKIP-v1.19-test-signed.apk
```

Do not install `app-release-unsigned.apk` directly. It is not signed and Android
will reject it.

1. Download the signed APK on the Android phone.
2. Open the downloaded APK and allow installation from unknown sources if Android asks.
3. Open AntSKIP.
4. Tap `Ativar acessibilidade`.
5. Enable the `AntSKIP` accessibility service.
6. Return to AntSKIP and choose the apps and skip actions you want.
7. Open a supported streaming app and play an episode with a skip button.

When AntSKIP successfully taps a button, Android shows a short message on the
screen, such as `Pulando abertura`.

## Recommended First Test

For the first test, use the safest configuration:

1. Keep `Automation` enabled.
2. Keep only `Netflix`, `Crunchyroll`, or `Prime Video` enabled.
3. Enable only `Intros/openings` while testing.
4. Play an episode that shows a visible skip-intro button.
5. Confirm that Android shows `Pulando abertura`.

After that works, enable `Recaps/summaries`, `Credits/endings`, or
`Next episode` one at a time. `Next episode` is intentionally the riskiest
action because some apps use generic labels such as `Next` or `Proximo`.

## Restricted Settings

Recent Android versions can block accessibility access for sideloaded apps with
a message similar to:

```text
Restricted settings
```

or, in Portuguese:

```text
Controlada pelas configuracoes restritas
```

To unlock the accessibility service:

1. Open Android `Settings`.
2. Go to `Apps`.
3. Select `AntSKIP`.
4. Open the three-dot menu in the top-right corner.
5. Tap `Allow restricted settings`.
6. Confirm with PIN, password, or biometrics if Android asks.
7. Return to `Settings > Accessibility`.
8. Enable the `AntSKIP` accessibility service.

Some Android skins translate or move this option, but it is usually on the app
info screen for sideloaded apps.

## Updates

The main screen shows the installed version and checks the latest GitHub Release
when the app opens. If a newer signed APK is available, AntSKIP shows an
`Atualizacao disponivel` status with a button to open the download page.

This check only contacts:

```text
https://api.github.com/repos/4NTx/AntSKIP/releases/latest
```

## Custom Phrases

If a button is visible but AntSKIP does not tap it:

1. Open AntSKIP.
2. Go to `Ensinar frases`.
3. Tap the action type, for example `Editar frases: Aberturas e intros`.
4. Add the exact text shown by the streaming app, one phrase per line.
5. Save and test the episode again.

Examples:

```text
Pular abertura
Proximo
Skip Intro
Next Episode
```

Custom phrases are stored locally in Android `SharedPreferences`.

If the same phrase is saved more than once, AntSKIP keeps only one copy. This is
intentional and does not create duplicate clicks.

## Per-App Rules

Global actions are only the default behavior. AntSKIP can also configure every
action separately for every streaming app.

Use `Regras por app` in the app UI to decide which actions are allowed for each
provider. This is the safest way to reduce false positives on providers other
than Crunchyroll.

Crunchyroll intentionally ignores custom phrases in the matcher. It follows the
per-app action rules and built-in strict labels for intros, recaps, credits, and
next episode, because generic phrases caused false taps while browsing the anime
list.

Examples:

- Enable `Next episode` for Netflix.
- Disable `Next episode` for Prime Video.
- Enable only `Intros/openings` while testing a new provider.
- Keep experimental providers disabled until you confirm their labels.

If a per-app rule is changed, it overrides the global default for that provider.

## Troubleshooting

### The accessibility service is blocked

If Android says the service is controlled by restricted settings, unlock it from
the AntSKIP app info screen:

```text
Settings > Apps > AntSKIP > three-dot menu > Allow restricted settings
```

Then return to:

```text
Settings > Accessibility > AntSKIP
```

### The button appears but AntSKIP does not tap it

- Confirm the streaming app is enabled in AntSKIP.
- Confirm the action is enabled globally. For Netflix, Prime Video, and
  experimental providers, also confirm it in `Per-app rules`.
- Add the exact visible label in `Custom phrases`.
- For Crunchyroll, the strict matcher supports built-in intro, recap, and
  credits labels and does not use custom phrases.

### AntSKIP taps the wrong button

- Disable that action for the affected app in `Per-app rules`.
- Avoid generic custom phrases such as `Next` unless they are scoped to a trusted
  provider through per-app rules.

## Known Limits

- AntSKIP can only tap buttons that the streaming app exposes to Android
  Accessibility.
- If a streaming app hides the button from accessibility, no phrase can match it.
- Streaming apps can change labels, package names, or accessibility behavior at
  any time.
- Next-episode prompts can behave differently for movies, specials, and final
  episodes. Disable `Next episode` for that app if you prefer to manually watch endings.
- Very generic phrases such as `Next` or `Proximo` can be risky. Prefer per-app
  rules and blocklist phrases when testing.
- Some providers have package aliases for mobile and TV builds, but forks or
  region-specific variants may still need another package entry.

## Build

Open the project directory:

```powershell
cd C:\Users\artur\AntSKIP
```

Build debug and release APKs:

```powershell
gradle :app:assembleDebug :app:assembleRelease
```

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

## Safety Model

- No screenshots.
- No overlays.
- No background screen recording.
- No DRM bypass.
- No modification of streaming app files.
- No traffic inspection.
- Accessibility events are limited to configured streaming package names.
