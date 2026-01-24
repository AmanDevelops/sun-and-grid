# SolaX Pocket WiFi Monitor

A minimal, modern Flutter application for monitoring SolaX Solar Inverters via the local Pocket WiFi interface.

This application leverages the local vulnerability/feature described in [CVE-2023-35835](https://app.opencve.io/cve/CVE-2023-35835) to fetch real-time telemetry directly from the inverter without cloud dependency.

## Features

- **Real-time Monitoring**: View Solar, Grid, and Yield data instantly.
- **Visual Topology**: animated flow diagram showing energy transfer from Solar to Grid.
- **Local Connection**: Direct connection to the inverter's WiFi AP (typically `5.8.8.8` or `192.168.10.10`).
- **Data Parsing**: Decodes the raw array data from the SolaX API into human-readable metrics.
- **Customizable**: Configure IP and Password/Serial Number dynamically.

## Prerequisites

- **Flutter SDK**: [Install Flutter](https://docs.flutter.dev/get-started/install) (Version 3.0+)
- **Android SDK**: For building Android APKs.
- **SolaX Pocket WiFi**: A compatible SolaX inverter with Pocket WiFi v3.

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd solax_monitor
```

### 2. Install Dependencies

```bash
flutter pub get
```

### 3. Run Locally (Development)

To run the app on a connected Android device or emulator:

```bash
flutter run
```

#### Configuring the Default Password

The application defaults to a placeholder Serial Number/Password. You can override this at build time using the `DEFAULT_SOLAX_PWD` environment variable:

```bash
flutter run --dart-define=DEFAULT_SOLAX_PWD=YOUR_ACTUAL_SN
```

*Note: You can also change this in the App's "Settings" dialog at runtime.*

### 4. Build for Release (Android)

To generate a production-ready APK:

```bash
flutter build apk --release --dart-define=DEFAULT_SOLAX_PWD=YOUR_ACTUAL_SN
```

The output APK will be located at: `build/app/outputs/flutter-apk/app-release.apk`

## Architecture

- **`lib/main.dart`**: Application entry point and theme configuration.
- **`lib/screens/overview_screen.dart`**: Main dashboard featuring the animated flow chart and key metrics.
- **`lib/screens/data_screen.dart`**: Detailed list view of all parsed telemetry data.
- **`lib/providers/data_provider.dart`**: State management (Provider) handling API calls and data state.
- **`lib/services/api_service.dart`**: HTTP service handling the CVE-2023-35835 payload construction.

## Mock Server (Testing)

A Python mock server is included to simulate a SolaX inverter for testing UI and parsing logic without being connected to physical hardware.

1. Navigate to the mock server directory:
   ```bash
   cd mock_server
   ```
2. Run the server:
   ```bash
   python3 solax_mock.py
   ```
3. In the App Settings, set the IP to: `<YOUR_COMPUTER_IP>:8000`

## License

This project is for educational and personal monitoring purposes.