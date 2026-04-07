# Implementation Plan: CrocDroid UI Improvements

## Overview

This implementation plan covers three major UI improvements to the CrocDroid Android application:
1. **QR Scanner Portrait Orientation**: Custom PortraitCaptureActivity forcing portrait mode
2. **Receive History Screen**: Dedicated screen with navigation for receive history management
3. **Relay Config Management**: Full CRUD system for relay server configuration profiles

The implementation follows a bottom-up approach, starting with data models and repository layer, then building UI components, and finally integrating navigation.

## Tasks

- [x] 1. Create data models and update existing models
  - [x] 1.1 Create RelayConfig data model
    - Create `domain/model/RelayConfig.kt` with id, name, relayAddress, relayPorts, relayPassword fields
    - Add DEFAULT companion object with croc default server configuration
    - Add @Serializable annotation for JSON persistence
    - _Requirements: 5.1, 5.2_

  - [x] 1.2 Update CrocSettings with selectedRelayConfigId field
    - Add `selectedRelayConfigId: String = "default"` field to CrocSettings data class
    - _Requirements: 7.1_

- [x] 2. Implement SettingsRepository relay config persistence
  - [x] 2.1 Add relay config state and persistence methods
    - Add `_relayConfigsState` MutableStateFlow and public `relayConfigsState` StateFlow
    - Implement `loadRelayConfigs()` with JSON deserialization and default config guarantee
    - Implement `saveRelayConfigs()` with JSON serialization to SharedPreferences
    - _Requirements: 5.3, 5.4, 5.5_

  - [ ]* 2.2 Write property test for relay config persistence round-trip
    - **Property 1: Relay Config Persistence Round-Trip**
    - **Validates: Requirements 5.3**

  - [x] 2.3 Implement relay config CRUD operations
    - Implement `addRelayConfig()` method
    - Implement `updateRelayConfig()` method
    - Implement `removeRelayConfig()` with default config protection
    - Implement `selectRelayConfig()` to update CrocSettings from selected config
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.2, 7.3_

  - [ ]* 2.4 Write property tests for relay config CRUD operations
    - **Property 2: Default Relay Config Invariant**
    - **Property 3: Add Relay Config Increases List Size**
    - **Property 4: Update Relay Config Preserves List Size**
    - **Property 5: Remove Relay Config Decreases List Size**
    - **Property 6: Relay Config Selection Updates Settings**
    - **Validates: Requirements 5.5, 6.1, 6.2, 6.3, 6.4, 7.2, 7.3, 10.3, 10.5**

  - [ ]* 2.5 Write unit tests for SettingsRepository relay config methods
    - Test selectRelayConfig updates settings correctly
    - Test removeRelayConfig does not remove default config
    - Test loadRelayConfigs ensures default config presence
    - _Requirements: 5.5, 6.4, 7.2, 7.3_

- [x] 3. Create PortraitCaptureActivity for QR scanner
  - [x] 3.1 Create PortraitCaptureActivity class
    - Create `ui/receive/PortraitCaptureActivity.kt` extending CaptureActivity
    - _Requirements: 1.1_

  - [x] 3.2 Add PortraitCaptureActivity to AndroidManifest.xml
    - Add activity declaration with screenOrientation="portrait"
    - Add stateNotLostOnOrientationChange="true"
    - Add theme="@style/zxing_CaptureTheme"
    - _Requirements: 1.2_

  - [ ]* 3.3 Write unit test for AndroidManifest configuration
    - Verify PortraitCaptureActivity is declared in manifest
    - Verify screenOrientation is set to portrait
    - _Requirements: 1.2_

  - [x] 3.4 Update ReceiveScreen to use PortraitCaptureActivity
    - Modify ScanOptions to call setCaptureActivity(PortraitCaptureActivity::class.java)
    - _Requirements: 1.3_

- [x] 4. Checkpoint - Verify QR scanner and data layer
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Create ReceiveHistoryViewModel
  - [x] 5.1 Create ReceiveHistoryViewModel class
    - Create `ui/receive/ReceiveHistoryViewModel.kt` with SettingsRepository and Context dependencies
    - Expose `receiveHistoryState` from SettingsRepository
    - Implement `deleteHistoryEntry()` method
    - Implement `clearHistory()` method
    - Implement `openHistoryFile()` with ACTION_VIEW intent
    - Implement `shareHistoryFile()` with ACTION_SEND intent
    - Add ViewModelProvider.Factory companion method
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 5.2 Write property tests for history operations
    - **Property 7: Delete History Entry Removes Single Entry**
    - **Property 8: Clear History Removes All Entries**
    - **Validates: Requirements 4.2, 4.3, 3.6**

  - [ ]* 5.3 Write unit tests for ReceiveHistoryViewModel
    - Test deleteHistoryEntry removes from repository
    - Test clearHistory removes all entries
    - Test openHistoryFile creates correct intent
    - Test shareHistoryFile creates correct intent
    - _Requirements: 4.2, 4.3, 4.4, 4.5_

- [x] 6. Create ReceiveHistoryScreen
  - [x] 6.1 Create ReceiveHistoryScreen composable
    - Create `ui/receive/ReceiveHistoryScreen.kt` with ViewModel and onNavigateBack parameters
    - Implement TopAppBar with back button and "Clear All" action
    - Implement empty state with icon and message
    - Implement LazyColumn of history entry cards
    - Display filename, formatted date, formatted size, file count for each entry
    - Show open, share, delete buttons for entries with existing files
    - Show "File missing or deleted" message and delete-only button for missing files
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [ ]* 6.2 Write property test for history entry rendering
    - **Property 9: History Entry Rendering Completeness**
    - **Validates: Requirements 3.3, 3.4, 3.5**

  - [ ]* 6.3 Write UI tests for ReceiveHistoryScreen
    - Test empty state displays when no history
    - Test history entries display correctly
    - Test open/share/delete buttons appear for existing files
    - Test missing file state displays correctly
    - _Requirements: 3.2, 3.3, 3.4, 3.5_

- [x] 7. Update ReceiveScreen for history navigation
  - [x] 7.1 Modify ReceiveScreen to add history navigation
    - Add `onNavigateToHistory: () -> Unit` parameter to ReceiveScreen
    - Remove inline history section (lines 176-267)
    - Add history icon button in idle state that calls onNavigateToHistory
    - _Requirements: 2.2, 2.4_

  - [ ]* 7.2 Write UI test for ReceiveScreen history button
    - Test history button appears in idle state
    - Test history button triggers navigation callback
    - _Requirements: 2.2_

- [x] 8. Create RelayConfigViewModel
  - [x] 8.1 Create RelayConfigViewModel class
    - Create `ui/relay/RelayConfigViewModel.kt` with SettingsRepository dependency
    - Expose `relayConfigsState` from SettingsRepository
    - Implement `addConfig()` method creating RelayConfig and calling repository
    - Implement `updateConfig()` method creating RelayConfig and calling repository
    - Implement `deleteConfig()` method calling repository
    - Add ViewModelProvider.Factory companion method
    - _Requirements: 6.1, 6.2, 6.3_

  - [ ]* 8.2 Write unit tests for RelayConfigViewModel
    - Test addConfig persists to repository
    - Test updateConfig modifies existing config
    - Test deleteConfig removes from repository
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 9. Create RelayConfigScreen
  - [x] 9.1 Create RelayConfigScreen composable
    - Create `ui/relay/RelayConfigScreen.kt` with ViewModel and onNavigateBack parameters
    - Implement TopAppBar with "Relay Configs" title and back button
    - Implement LazyColumn of relay config cards
    - Display name, address, ports, password (masked) for each config
    - Show edit button for all configs
    - Show delete button only for non-default configs
    - Implement FAB for adding new config
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [x] 9.2 Implement add/edit dialog for relay configs
    - Create dialog with name, relay address, relay ports, relay password fields
    - Wire save button to call addConfig or updateConfig on ViewModel
    - Wire cancel button to dismiss dialog without saving
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ]* 9.3 Write property test for relay config rendering
    - **Property 10: Relay Config Rendering Completeness**
    - **Validates: Requirements 8.3, 8.4, 8.5**

  - [ ]* 9.4 Write property test for dialog cancellation
    - **Property 11: Dialog Cancellation Preserves State**
    - **Validates: Requirements 9.6**

  - [ ]* 9.5 Write UI tests for RelayConfigScreen
    - Test default config has no delete button
    - Test non-default configs have delete button
    - Test edit button opens dialog
    - Test FAB opens add dialog
    - Test dialog save creates/updates config
    - Test dialog cancel preserves state
    - _Requirements: 8.4, 8.5, 8.6, 9.1, 9.2, 9.4, 9.5, 9.6_

- [x] 10. Checkpoint - Verify new screens and ViewModels
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Update RelayViewModel for config selection
  - [x] 11.1 Add SettingsRepository dependency to RelayViewModel
    - Update RelayViewModel constructor to accept SettingsRepository
    - Update ViewModelProvider.Factory to pass SettingsRepository
    - _Requirements: 10.4_

  - [x] 11.2 Expose relay config state in RelayViewModel
    - Expose `relayConfigsState` from SettingsRepository
    - Create `selectedRelayConfig` StateFlow derived from settingsState and relayConfigsState
    - Implement `selectRelayConfig()` method calling repository
    - _Requirements: 10.4, 10.5_

  - [ ]* 11.3 Write unit tests for RelayViewModel config selection
    - Test selectRelayConfig updates settings
    - Test selectedRelayConfig reflects current selection
    - _Requirements: 10.5_

- [x] 12. Update RelayScreen for config selection
  - [x] 12.1 Add relay config selector to RelayScreen
    - Add `onNavigateToRelayConfig: () -> Unit` parameter
    - Add relay configuration card section before existing fields
    - Display currently selected config name
    - Add "Manage Configs" button with settings icon navigating to RelayConfigRoute
    - Add ExposedDropdownMenuBox for selecting relay config
    - Wire dropdown selection to call viewModel.selectRelayConfig()
    - _Requirements: 7.4, 7.5, 10.1, 10.2, 10.3_

  - [ ]* 12.2 Write UI tests for RelayScreen config selection
    - Test config selector displays current config
    - Test manage configs button triggers navigation
    - Test dropdown selection updates config
    - Test fields auto-fill from selected config
    - _Requirements: 10.1, 10.2, 10.3_

- [x] 13. Update SettingsScreen relay section
  - [x] 13.1 Simplify SettingsScreen relay section
    - Add `onNavigateToRelayConfig: () -> Unit` parameter
    - Replace editable relay address/ports/password fields with read-only selected config display
    - Add "Change" button with edit icon navigating to RelayConfigRoute
    - Keep "Peer IP (Direct Connect)" field as editable
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [ ]* 13.2 Write UI tests for SettingsScreen relay section
    - Test selected config name displays
    - Test change button triggers navigation
    - Test peer IP field remains editable
    - Test relay address/ports/password fields removed
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [x] 14. Add navigation routes and integration
  - [x] 14.1 Add new routes to CrocDroidNavHost
    - Add `@Serializable data object ReceiveHistoryRoute` to navigation routes
    - Add `@Serializable data object RelayConfigRoute` to navigation routes
    - _Requirements: 2.1, 8.1_

  - [x] 14.2 Update MainActivity NavHost with new composables
    - Add composable<ReceiveHistoryRoute> with ReceiveHistoryViewModel and ReceiveHistoryScreen
    - Add composable<RelayConfigRoute> with RelayConfigViewModel and RelayConfigScreen
    - Pass onNavigateBack callbacks using navController.popBackStack()
    - _Requirements: 12.1, 12.2_

  - [x] 14.3 Update existing route composables with navigation callbacks
    - Update ReceiveRoute composable to pass onNavigateToHistory callback
    - Update RelayRoute composable to pass onNavigateToRelayConfig callback and SettingsRepository
    - Update SettingsRoute composable to pass onNavigateToRelayConfig callback
    - _Requirements: 12.3, 12.4, 12.5_

  - [x] 14.4 Update bottom bar visibility logic
    - Modify showBottomBar logic to hide on ReceiveHistoryRoute and RelayConfigRoute
    - _Requirements: 2.5, 8.8, 12.6_

  - [ ]* 14.5 Write navigation integration tests
    - Test ReceiveHistoryRoute hides bottom bar
    - Test RelayConfigRoute hides bottom bar
    - Test navigation callbacks work correctly
    - _Requirements: 2.5, 8.8, 12.6_

- [x] 15. Final checkpoint - Integration and end-to-end verification
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at logical breakpoints
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation uses Kotlin with Jetpack Compose and follows Android best practices
- All persistence uses SharedPreferences with kotlinx.serialization JSON encoding
