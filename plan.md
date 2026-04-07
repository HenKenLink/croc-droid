# Three Feature Improvements: QR Scanner Orientation, Receive History Page, Relay Config Management

## 1. QR Scanner Landscape Orientation Issue

### Analysis

The project uses `com.journeyapps:zxing-android-embedded:4.3.0` for QR scanning. In `ReceiveScreen.kt` (line 148), `setOrientationLocked(true)` is already called — this locks the orientation to the **current** Activity orientation at scan time, rather than forcing portrait.

The `zxing-android-embedded` library launches its own `Activity` (`CaptureActivity`) for scanning. By default it inherits the system orientation or follows the user's device settings. The key issue is that `setOrientationLocked(true)` locks to "current" — if the device or manifest doesn't force portrait, the scan activity may launch in landscape.

**Root Cause**: Indeed a library behavior issue. The `zxing-android-embedded` `CaptureActivity` does not force portrait mode by default. To fix this, we can set `setCaptureActivity` to a custom Activity that forces portrait orientation, **or** set `setOrientationLocked(false)` and declare a custom portrait-only capture activity.

> [!IMPORTANT]
> **Recommended Fix**: Create a `PortraitCaptureActivity` that extends `CaptureActivity` and enforces `screenOrientation = portrait` in the manifest. Then set it via `ScanOptions.setCaptureActivity(PortraitCaptureActivity::class.java)`.

---

## 2. Receive History → Separate Page

Move the inline receive history from `ReceiveScreen.kt` to a new dedicated `ReceiveHistoryScreen` accessible via a history icon button in the Receive page top bar area.

### Proposed Changes

#### Navigation

##### [MODIFY] [CrocDroidNavHost.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/navigation/CrocDroidNavHost.kt)
- Add `@Serializable data object ReceiveHistoryRoute`

##### [MODIFY] [MainActivity.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/MainActivity.kt)
- Add `composable<ReceiveHistoryRoute>` to the NavHost
- Wire up `ReceiveHistoryScreen` with a `ReceiveHistoryViewModel`
- Pass navigation callbacks (`onNavigateBack`)
- Hide bottom bar when on ReceiveHistoryRoute

#### Receive Screen

##### [MODIFY] [ReceiveScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveScreen.kt)
- Remove the inline history section (lines 176–267)
- Add a history icon button near the hero section / top area that calls `onNavigateToHistory()`
- Add `onNavigateToHistory: () -> Unit` parameter

#### New Receive History Screen

##### [NEW] [ReceiveHistoryScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveHistoryScreen.kt)
- Full-page screen with `TopAppBar` (back button + "Clear All" action)
- Display the receive history items (migrated from ReceiveScreen.kt)
- Same design: file icons, date, size, open/share/delete actions
- Empty state with icon and text

##### [NEW] [ReceiveHistoryViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveHistoryViewModel.kt)
- Wraps `SettingsRepository.receiveHistoryState`
- Functions: `deleteHistoryEntry`, `clearHistory`, `openHistoryFile`, `shareHistoryFile`
- Extracted from `ReceiveViewModel` (keeps ReceiveViewModel cleaner)

---

## 3. Relay Config Management

### Data Model

##### [NEW] [RelayConfig.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/domain/model/RelayConfig.kt)
```kotlin
@Serializable
data class RelayConfig(
    val id: String,
    val name: String,
    val relayAddress: String,
    val relayPorts: String,
    val relayPassword: String,
)
```

Default profile (croc default server):
- name: "Default (croc)"
- address: "croc.schollz.com"
- ports: "9009,9010,9011,9012,9013"
- password: "pass123"

### Persistence

##### [MODIFY] [SettingsRepository.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/data/settings/SettingsRepository.kt)
- Add `relayConfigsState: StateFlow<List<RelayConfig>>` backed by SharedPreferences
- CRUD methods: `addRelayConfig`, `updateRelayConfig`, `removeRelayConfig`
- Ensure default config is always present and cannot be deleted

##### [MODIFY] [CrocSettings.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/domain/model/CrocSettings.kt)
- Add `selectedRelayConfigId: String = "default"` field
- When a relay config is selected, `relayAddress`, `relayPorts`, `relayPassword` are populated from it

### Relay Screen Changes

##### [MODIFY] [RelayScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/relay/RelayScreen.kt)
- Add a "Relay Config" dropdown/selector section showing the currently selected relay config
- Add a "Manage Configs" button (gear icon) that navigates to the Relay Config management page
- When relay config is selected, auto-fill host/port/password from the config

##### [MODIFY] [RelayViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/relay/RelayViewModel.kt)
- Accept `SettingsRepository` as a dependency
- Expose `relayConfigs` and `selectedRelayConfig`
- Method to select a relay config → updates CrocSettings

### New Relay Config Management Page

##### [NEW] [RelayConfigScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/relay/RelayConfigScreen.kt)
- Full-page with TopAppBar ("Relay Configs" + back button)
- List of relay configs as cards, each showing:
  - Name, address, ports, password
  - Edit button → opens edit dialog
  - Delete button (disabled for default config)
- FAB to add new config → opens add dialog
- Dialog with fields: Name, Relay Address, Relay Ports, Relay Password

##### [NEW] [RelayConfigViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/relay/RelayConfigViewModel.kt)
- Wraps SettingsRepository relay config CRUD operations
- Exposes `relayConfigsState: StateFlow<List<RelayConfig>>`
- Functions: `addConfig`, `updateConfig`, `deleteConfig`

### Navigation

##### [MODIFY] [CrocDroidNavHost.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/navigation/CrocDroidNavHost.kt)
- Add `@Serializable data object RelayConfigRoute`

##### [MODIFY] [MainActivity.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/MainActivity.kt)
- Add `composable<RelayConfigRoute>` to NavHost
- Wire up `RelayConfigScreen` + `RelayConfigViewModel`
- Pass `onNavigateToRelayConfig` to `RelayScreen`
- Hide bottom bar on RelayConfigRoute

### Settings Screen (Relay section simplification)

##### [MODIFY] [SettingsScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/settings/SettingsScreen.kt)
- Replace the manual relay address/port/password text fields with a read-only display showing the currently selected relay config name
- Add a "Change" button that navigates to Relay Config management
- Still keep the "Peer IP" field as-is

---

## Open Questions

> [!IMPORTANT]
> 1. **QR Scanner fix**: Should I create a custom `PortraitCaptureActivity` that forces portrait in the manifest? This is the standard recommended approach for `zxing-android-embedded`. Or would you prefer a different approach?
> 2. **Settings page relay section**: Once relay configs are managed from the Relay page, should the Settings "Relay & Connection" section simply show which config is selected and link to the management page, or should it remain fully editable?

---

## Verification Plan

### Automated
- `./gradlew assembleDebug` — verify project compiles

### Manual
- Verify QR scanner launches in portrait mode
- Verify receive history page loads from history icon on Receive screen
- Verify relay config CRUD (add/edit/delete) works on the management page
- Verify selecting a relay config applies to both the relay server and transfer settings
