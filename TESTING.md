## Testing Guide

### How to run
- Unit tests (JVM):
  - `./gradlew testDebugUnitTest`
- Instrumented tests (device/emulator):
  - `./gradlew connectedAndroidTest`
  - Requires an Android emulator or a physical device connected and authorized (USB debugging on).

### What’s covered
- Unit tests
  - Utils: `DataRefreshNotifier`, `ImageUtils`, `DataValidator`
  - Repositories: `VehicleRepository`, `FleetRepository`, `HomeRepository`, `AuthRepository`
- Instrumented tests (androidTest)
  - Room DAOs: `VehicleDao`, `FuelRecordDao`, `ActivityRecordDao`, `MaintenanceRecordDao`, `OrganizationDao`, `NotificationDao`, `DiaryEntryDao`
  - UI (Jetpack Compose): `HomeScreen`, `FleetScreen`, `DiaryScreen`, `VehicleActivityScreen`, `VehicleFuelScreen`, `VehicleMaintenanceScreen`, `ConnectivityScreen`
  - UI components: `AppHeader`, `VehicleListItem`

### Notes
- Instrumented tests run on-device and may take longer on first run due to install/build steps.
- If you want to run a single test class:
  - Unit: `./gradlew :app:testDebugUnitTest --tests "dev.barreto.fleetctrl.*.YourTestClass"`
  - Instrumented: `./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.barreto.fleetctrl.package.YourTestClass`

### Troubleshooting
- If `connectedAndroidTest` reports no devices: start an emulator from Android Studio (AVD Manager) or connect a device with USB debugging enabled.
- If you see Firebase-related warnings during unit tests, they are mocked; tests should still pass.
