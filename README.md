# LockedIn

An NFC-powered app blocker for Android that helps you achieve digital wellbeing through physical commitment devices.

## Overview

LockedIn is a focus app with a unique approach: you tap an NFC tag to activate blocking, and **the only way to unblock is to tap the tag again**. This physical friction prevents impulse app usage and helps build healthier digital habits.

## Features

- **NFC-Activated Blocking** - Tap your registered NFC tag to start a focus session
- **App Blocking with Full-Screen Overlay** - Blocked apps trigger an undismissable lock screen with countdown timer
- **Schedule Configuration** - Set custom time windows for your blocking sessions
- **Streak Tracking** - Track consecutive days of completed focus sessions with milestone celebrations (7, 30, 100 days)
- **Session Statistics** - View blocked app attempts, session duration, and time saved
- **Persistent Notifications** - Always know how much time remains in your focus session
- **Boot Reset Protection** - Blocking state clears on device restart for safety
- **Double-Tap Confirmation** - Prevents accidental session endings

## Screenshots

*Coming soon*

## Requirements

- Android 10 (API 29) or higher
- NFC-capable device
- Accessibility Service permission (required for app detection)

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Clean Architecture principles
- **Database**: Room (v2.6.1)
- **Async**: Kotlin Coroutines & Flow
- **Navigation**: Navigation Compose

## Project Structure

```
app/src/main/java/com/lockedin/
├── MainActivity.kt                 # App entry point
├── service/                        # Business logic & system integration
│   ├── NfcHandler.kt              # NFC tag detection & management
│   ├── AppBlockerAccessibilityService.kt  # Monitors foreground apps
│   ├── BlockingStateManager.kt    # Centralized blocking state
│   ├── BlockingForegroundService.kt  # Maintains blocking & notifications
│   └── BootReceiver.kt            # Clears blocking on device boot
├── data/                          # Data layer
│   ├── AppDatabase.kt             # Room database
│   ├── dao/                       # Data Access Objects
│   ├── entity/                    # Database entities
│   └── repository/                # Data access abstraction
└── ui/                            # Presentation layer (Compose)
    ├── home/                      # Main screen
    ├── setup/                     # Initial setup wizard
    ├── appselection/              # Choose apps to block
    ├── schedule/                  # Configure blocking schedule
    ├── statistics/                # View session & streak data
    ├── settings/                  # App settings
    ├── overlay/                   # Lock screen overlay
    └── theme/                     # Material 3 dark theme
```

## Building

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 35
- JDK 11 or higher

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Setup Flow

1. **Launch the app** - First-time users are guided through a setup wizard
2. **Grant Accessibility Service** - Required for detecting blocked apps
3. **Select apps to block** - Choose which apps should be blocked during focus sessions
4. **Configure schedule** - Set your preferred blocking time window
5. **Register NFC tag** - Tap your NFC tag to register it with the app
6. **Start focusing** - Tap your tag anytime to begin a focus session

## How It Works

1. **Tap your NFC tag** to start a focus session
2. The app activates blocking based on your configured schedule
3. If you try to open a blocked app, a full-screen overlay appears
4. The overlay shows remaining time and cannot be dismissed
5. **Tap your NFC tag again** to end the session (with double-tap confirmation)
6. Your streak updates and statistics are recorded

## Permissions

| Permission | Purpose |
|------------|---------|
| `NFC` | Read NFC tags for activation/deactivation |
| `FOREGROUND_SERVICE` | Maintain blocking state when app is backgrounded |
| `POST_NOTIFICATIONS` | Show persistent countdown notification |
| `RECEIVE_BOOT_COMPLETED` | Clear blocking state on device restart |
| `QUERY_ALL_PACKAGES` | List installed apps for selection |
| `VIBRATE` | Haptic feedback on NFC tap |
| `ACCESSIBILITY_SERVICE` | Detect when blocked apps are launched |

## Architecture

The app follows MVVM architecture with clear separation of concerns:

- **Data Layer**: Room database with DAOs and repositories for persistence
- **Service Layer**: NFC handling, accessibility monitoring, and foreground services
- **Presentation Layer**: Compose UI with ViewModels managing UI state via StateFlow

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

*License information to be added*

## Acknowledgments

Built with modern Android development practices using Jetpack Compose, Kotlin Coroutines, and Material Design 3.
