# GEMINI.md — Croc Droid Project Specification

## Overview
Android GUI client for [croc](https://github.com/schollz/croc) (Go-based, end-to-end encrypted file transfer). The Go engine is compiled to `.aar` via `gomobile` and bridged to Kotlin.

---

## Tech Stack
| Component | Choice |
|---|---|
| Language | Kotlin |
| Build | Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`) |
| UI | Jetpack Compose + **Material 3 only** |
| Architecture | MVVM + UDF (`StateFlow`, no `LiveData`) |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose (type-safe `@Serializable` routes only) |
| Serialization | `kotlinx.serialization` |

---

## Hard Rules (AI must enforce these)

### Forbidden
- ❌ `LiveData` → use `StateFlow`
- ❌ `mutableStateOf` in ViewModel → use `MutableStateFlow`
- ❌ `AppCompatActivity` → use `ComponentActivity`
- ❌ Material 2 (`androidx.compose.material.*`) → use Material 3
- ❌ String-based navigation routes → use `@Serializable` data objects/classes
- ❌ XML layouts / `findViewById`
- ❌ `composeOptions { kotlinCompilerExtensionVersion }` (managed by BOM)
- ❌ `kotlinOptions { jvmTarget }` → use `kotlin { compilerOptions { jvmTarget.set(...) } }`
- ❌ Groovy build scripts → use `.kts`
- ❌ Hardcoded dependency versions → declare in `libs.versions.toml`
- ❌ Blocking calls on main thread for croc operations → use coroutines/background dispatcher

### Required
- ✅ All Composables accept `modifier: Modifier = Modifier`
- ✅ State hoisting; no state held inside Composables
- ✅ `LaunchedEffect`/`SideEffect` for side effects
- ✅ `enableEdgeToEdge()` in `MainActivity`
- ✅ Compose BOM for all Compose versions
- ✅ `data class` for data holders, `sealed interface` for UI state, `data object` for singletons
- ✅ Prefer `val`, immutable collections, `?.let`/`?: run` over `!!`

---

## Architecture
```
UI (Composable) → Event → ViewModel (StateFlow<UiState>) → Repository → DataSource
```
- **UI**: render + emit events only
- **ViewModel**: business logic, exposes `StateFlow<*UiState>`, use `sealed interface` for state
- **Repository**: single data entry point
- **DataSource**: network / disk / croc engine calls

---

## Project Structure

```
/                           Root
├── go/crocbridge/          Go ↔ Kotlin bridge (gomobile)
│   ├── bridge.go           Exported funcs: ExecuteSend, ExecuteReceive, etc.
│   ├── crocbridge.aar      Build artifact (copied to app/libs/)
│   └── go.mod              replace → ../../external/croc
├── app/                    Android module
│   ├── libs/               crocbridge.aar + sources jar
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/henkenlink/crocdroid/
│       ├── CrocDroidApp.kt         Application class
│       ├── MainActivity.kt         Single Activity, enableEdgeToEdge
│       ├── navigation/
│       │   └── CrocDroidNavHost.kt Type-safe NavHost (@Serializable routes)
│       ├── ui/
│       │   ├── theme/              Color.kt, Theme.kt, Type.kt (M3)
│       │   ├── send/               SendScreen.kt, SendViewModel.kt
│       │   ├── receive/            ReceiveScreen.kt, ReceiveViewModel.kt
│       │   ├── relay/              RelayScreen.kt, RelayViewModel.kt
│       │   └── settings/           SettingsScreen.kt, SettingsViewModel.kt
│       ├── domain/model/           CrocSettings.kt, TransferState.kt
│       └── data/
│           ├── croc/CrocEngine.kt  Go library wrapper; emits Flow<TransferProgress>
│           └── settings/SettingsRepository.kt  SharedPreferences/DataStore persistence
├── external/croc/          Full croc Go source (for reference & custom builds)
├── gradle/libs.versions.toml
├── build_project.sh        Build Go bridge → build Android APK
└── setup_sdk.sh            Setup Android SDK/NDK (Codespace)
```

### Naming
| Type | Pattern | Example |
|---|---|---|
| Screen | `*Screen` | `SendScreen` |
| ViewModel | `*ViewModel` | `SendViewModel` |
| UiState | `*UiState` | `SendUiState` |
| Repository interface | `*Repository` | `SettingsRepository` |
| Repository impl | `*RepositoryImpl` | `SettingsRepositoryImpl` |
| Common component | Descriptive | `CodeInputField`, `CrocButton` |

---

## croc Engine Notes
- `external/croc/src/croc/croc.go` — core logic (`Options`, `Client`, `Send`, `Receive`)
- All engine calls run on background coroutines; progress exposed as `Flow<TransferProgress>`
- `go/crocbridge/go.mod` must use `replace` pointing to `../../external/croc`
