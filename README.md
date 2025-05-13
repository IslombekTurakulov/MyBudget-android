# MyBudget Android

MyBudget is a modern Android application for personal finance management, built with Kotlin and following Android best practices.

## Features

- Personal finance tracking and management
- Transaction history and categorization
- Data visualization with charts and graphs
- OCR functionality for receipt scanning
- Export data to CSV and PDF formats
- Push notifications
- Secure data storage with encryption
- Material Design 3 UI components
- Deep linking support for invites
- File sharing capabilities
- Camera integration for receipt scanning

## Technical Stack

- **Language**: Kotlin
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Clean Architecture
- **Key Libraries**:
  - AndroidX Core KTX
  - Material Design 3
  - Navigation Component
  - Room Database
  - Hilt for Dependency Injection
  - Retrofit for Networking
  - Coroutines for Asynchronous Programming
  - ML Kit for OCR
  - MPAndroidChart for Data Visualization
  - Firebase (Analytics, Crashlytics, Messaging)
  - Glide for Image Loading
  - WorkManager for Background Tasks
  - DataStore for Preferences
  - Ktor HTTP Client
  - FileProvider for file sharing
  - PhotoView for image viewing

## Project Structure

```
app/src/main/java/ru/iuturakulov/mybudget/
├── auth/           # Authentication related code
├── core/           # Core functionality and utilities
├── data/           # Data layer (repositories, data sources)
├── di/             # Dependency injection modules
├── domain/         # Domain layer (use cases, models)
├── firebase/       # Firebase related code
└── ui/             # UI layer (activities, fragments, viewmodels)
```

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 1.8 or newer
- Android SDK 34
- Google Play Services
- Firebase project setup

### Required Permissions

The app requires the following permissions:
- Internet access
- Camera access
- Storage access (for saving receipts and exports)
- Notification permissions

### Installation

1. Clone the repository:
```bash
git clone https://github.com/IslombekTurakulov/MyBudget-android.git
```

2. Open the project in Android Studio

3. Set up Firebase:
   - Create a new Firebase project
   - Add your `google-services.json` to the app directory
   - Enable Firebase Analytics, Crashlytics, and Cloud Messaging

4. Sync the project with Gradle files

5. Build and run the application

## Building the Project

The project uses Gradle for building. You can build the project using:

```bash
./gradlew build
```

For a release build:

```bash
./gradlew assembleRelease
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contact

Islombek Turakulov
- Email: me@turakulov.ru
- LinkedIn: [@iuturakulov](https://linkedin.com/in/iuturakulov)
