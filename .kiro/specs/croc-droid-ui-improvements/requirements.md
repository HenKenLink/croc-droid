# Requirements Document

## Introduction

This document specifies requirements for three UI improvements to the CrocDroid Android application: forcing portrait orientation for the QR scanner, moving receive history to a dedicated screen, and implementing relay configuration management. These improvements enhance usability and provide better control over relay server configurations.

## Glossary

- **QR_Scanner**: The barcode scanning interface provided by zxing-android-embedded library
- **ReceiveScreen**: The main screen where users enter transfer codes to receive files
- **ReceiveHistoryScreen**: A dedicated screen displaying the history of received files
- **RelayConfig**: A configuration profile containing relay server connection details (address, ports, password)
- **SettingsRepository**: The data persistence layer managing application settings and configurations
- **CrocSettings**: The application settings data model
- **RelayScreen**: The screen for managing local relay server operations
- **RelayConfigScreen**: The screen for managing relay configuration profiles
- **PortraitCaptureActivity**: A custom activity that extends CaptureActivity to enforce portrait orientation
- **Navigation_Route**: A serializable navigation destination in the Compose navigation graph

## Requirements

### Requirement 1: QR Scanner Portrait Orientation

**User Story:** As a user, I want the QR scanner to always launch in portrait mode, so that I have a consistent scanning experience regardless of device orientation.

#### Acceptance Criteria

1. THE PortraitCaptureActivity SHALL extend CaptureActivity from zxing-android-embedded library
2. THE PortraitCaptureActivity SHALL be declared in AndroidManifest.xml with screenOrientation set to portrait
3. WHEN the QR scanner is launched from ReceiveScreen, THE QR_Scanner SHALL use PortraitCaptureActivity
4. WHEN the device is in landscape orientation, THE QR_Scanner SHALL display in portrait orientation
5. WHEN the user rotates the device during scanning, THE QR_Scanner SHALL remain in portrait orientation

### Requirement 2: Receive History Navigation

**User Story:** As a user, I want to access my receive history from a dedicated screen, so that I can view and manage received files without cluttering the main receive interface.

#### Acceptance Criteria

1. THE Navigation_Route SHALL include a ReceiveHistoryRoute destination
2. WHEN the user is on ReceiveScreen in idle state, THE ReceiveScreen SHALL display a history icon button
3. WHEN the user taps the history icon button, THE Application SHALL navigate to ReceiveHistoryScreen
4. THE ReceiveScreen SHALL NOT display inline receive history items
5. WHEN the user navigates to ReceiveHistoryScreen, THE Application SHALL hide the bottom navigation bar

### Requirement 3: Receive History Screen Display

**User Story:** As a user, I want to view my complete receive history on a dedicated screen, so that I can easily browse and manage all received files.

#### Acceptance Criteria

1. THE ReceiveHistoryScreen SHALL display a TopAppBar with a back button and "Clear All" action
2. WHEN receive history is empty, THE ReceiveHistoryScreen SHALL display an empty state with icon and message
3. WHEN receive history contains entries, THE ReceiveHistoryScreen SHALL display each entry as a card showing filename, date, size, and file count
4. FOR EACH history entry where files exist, THE ReceiveHistoryScreen SHALL display open, share, and delete action buttons
5. FOR EACH history entry where all files are missing, THE ReceiveHistoryScreen SHALL display "File missing or deleted" message and only show delete action
6. WHEN the user taps "Clear All", THE ReceiveHistoryScreen SHALL remove all history entries and delete associated files from app directories

### Requirement 4: Receive History View Model

**User Story:** As a developer, I want a dedicated ViewModel for receive history, so that history management logic is separated from transfer operations.

#### Acceptance Criteria

1. THE ReceiveHistoryViewModel SHALL expose receiveHistoryState from SettingsRepository
2. THE ReceiveHistoryViewModel SHALL provide deleteHistoryEntry function that removes a single entry
3. THE ReceiveHistoryViewModel SHALL provide clearHistory function that removes all entries
4. THE ReceiveHistoryViewModel SHALL provide openHistoryFile function that opens a file using system intent
5. THE ReceiveHistoryViewModel SHALL provide shareHistoryFile function that shares a file using system share sheet

### Requirement 5: Relay Configuration Data Model

**User Story:** As a user, I want to save multiple relay server configurations, so that I can quickly switch between different relay servers.

#### Acceptance Criteria

1. THE RelayConfig SHALL contain id, name, relayAddress, relayPorts, and relayPassword fields
2. THE SettingsRepository SHALL maintain a default RelayConfig with name "Default (croc)", address "croc.schollz.com", ports "9009,9010,9011,9012,9013", and password "pass123"
3. THE SettingsRepository SHALL persist relay configurations using SharedPreferences as JSON
4. THE SettingsRepository SHALL expose relayConfigsState as StateFlow<List<RelayConfig>>
5. THE SettingsRepository SHALL ensure the default RelayConfig is always present in relayConfigsState

### Requirement 6: Relay Configuration CRUD Operations

**User Story:** As a user, I want to create, edit, and delete relay configurations, so that I can manage my relay server profiles.

#### Acceptance Criteria

1. THE SettingsRepository SHALL provide addRelayConfig function that adds a new configuration
2. THE SettingsRepository SHALL provide updateRelayConfig function that modifies an existing configuration
3. THE SettingsRepository SHALL provide removeRelayConfig function that deletes a configuration
4. WHEN removeRelayConfig is called with the default config id, THE SettingsRepository SHALL NOT remove the default configuration
5. WHEN a relay configuration is added or updated, THE SettingsRepository SHALL persist changes to SharedPreferences

### Requirement 7: Relay Configuration Selection

**User Story:** As a user, I want to select which relay configuration to use, so that my transfers use the correct relay server.

#### Acceptance Criteria

1. THE CrocSettings SHALL include a selectedRelayConfigId field with default value "default"
2. WHEN a relay configuration is selected, THE Application SHALL update CrocSettings.selectedRelayConfigId
3. WHEN a relay configuration is selected, THE Application SHALL populate CrocSettings.relayAddress, relayPorts, and relayPassword from the selected RelayConfig
4. THE RelayScreen SHALL display a relay config selector showing the currently selected configuration name
5. WHEN the user selects a different relay config from RelayScreen, THE Application SHALL update the selected configuration

### Requirement 8: Relay Configuration Management Screen

**User Story:** As a user, I want a dedicated screen to manage my relay configurations, so that I can view, add, edit, and delete relay profiles.

#### Acceptance Criteria

1. THE Navigation_Route SHALL include a RelayConfigRoute destination
2. THE RelayConfigScreen SHALL display a TopAppBar with title "Relay Configs" and back button
3. THE RelayConfigScreen SHALL display each relay configuration as a card showing name, address, ports, and password
4. FOR EACH relay configuration, THE RelayConfigScreen SHALL display an edit button that opens an edit dialog
5. FOR EACH non-default relay configuration, THE RelayConfigScreen SHALL display a delete button
6. FOR THE default relay configuration, THE RelayConfigScreen SHALL NOT display a delete button
7. THE RelayConfigScreen SHALL display a FAB that opens an add configuration dialog
8. WHEN the user navigates to RelayConfigScreen, THE Application SHALL hide the bottom navigation bar

### Requirement 9: Relay Configuration Dialog

**User Story:** As a user, I want to add or edit relay configurations through a dialog, so that I can input relay server details.

#### Acceptance Criteria

1. THE RelayConfigScreen SHALL display a dialog for adding new configurations
2. THE RelayConfigScreen SHALL display a dialog for editing existing configurations
3. THE configuration dialog SHALL include input fields for name, relay address, relay ports, and relay password
4. WHEN the user saves a new configuration, THE RelayConfigViewModel SHALL call addConfig with the input values
5. WHEN the user saves an edited configuration, THE RelayConfigViewModel SHALL call updateConfig with the modified values
6. WHEN the user cancels the dialog, THE RelayConfigScreen SHALL dismiss the dialog without saving changes

### Requirement 10: Relay Screen Configuration Integration

**User Story:** As a user, I want to see and select relay configurations from the relay screen, so that I can choose which relay server to run.

#### Acceptance Criteria

1. THE RelayScreen SHALL display the currently selected relay configuration name
2. THE RelayScreen SHALL display a "Manage Configs" button that navigates to RelayConfigRoute
3. WHEN a relay configuration is selected on RelayScreen, THE RelayScreen SHALL auto-fill host, port, and password fields from the selected configuration
4. THE RelayViewModel SHALL expose relayConfigs and selectedRelayConfig from SettingsRepository
5. THE RelayViewModel SHALL provide a method to select a relay configuration that updates CrocSettings

### Requirement 11: Settings Screen Relay Section Simplification

**User Story:** As a user, I want the settings screen to show which relay configuration is active, so that I understand which relay server will be used for transfers.

#### Acceptance Criteria

1. THE SettingsScreen relay section SHALL display the currently selected relay configuration name in a read-only field
2. THE SettingsScreen relay section SHALL display a "Change" button that navigates to RelayConfigRoute
3. THE SettingsScreen relay section SHALL continue to display the "Peer IP (Direct Connect)" field as editable
4. THE SettingsScreen relay section SHALL NOT display editable fields for relay address, ports, or password
5. WHEN the user taps "Change", THE Application SHALL navigate to RelayConfigScreen

### Requirement 12: Navigation Integration

**User Story:** As a developer, I want navigation routes properly integrated, so that users can navigate between all screens.

#### Acceptance Criteria

1. THE MainActivity NavHost SHALL include a composable for ReceiveHistoryRoute
2. THE MainActivity NavHost SHALL include a composable for RelayConfigRoute
3. WHEN ReceiveScreen is displayed, THE ReceiveScreen SHALL receive an onNavigateToHistory callback
4. WHEN RelayScreen is displayed, THE RelayScreen SHALL receive an onNavigateToRelayConfig callback
5. WHEN SettingsScreen is displayed, THE SettingsScreen SHALL receive an onNavigateToRelayConfig callback
6. WHEN the user is on ReceiveHistoryRoute or RelayConfigRoute, THE MainActivity SHALL hide the bottom navigation bar
