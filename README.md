# GAIME - Game-Aware AI Micro-Experience

A Kotlin Multiplatform waitroom for AI work.

## Architecture

GAIME is a Compose Multiplatform application targeting Android and Desktop (JVM). It implements a state-machine-driven "waitroom" where players interact while AI processes run in the background.

### Core Concepts

- **GameStateMachine** - Manages game session lifecycle through states: Idle, Thinking, ReadyButPlaying, RevealReady, Closed, Failed
- **AiSignalSource** - Interface for AI signal providers (manual or automated)
- **WaitroomScreen** - Compose UI showing current state and manual control buttons

### Project Structure

```
composeApp/
  src/
    commonMain/kotlin/
      core/        - State machine and game logic
      signals/     - AI signal source interfaces
      ui/          - Compose UI screens
      app/         - Top-level App composable
    androidMain/   - Android Activity entry point
    desktopMain/   - Desktop window entry point
    commonTest/    - Unit tests
```

## Build Instructions

### Prerequisites

- JDK 17+ (JDK 25 recommended)
- Gradle 8.14+

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew allTests
```

### Run Desktop

```bash
./gradlew :composeApp:run
```

### Build Android APK

```bash
./gradlew :composeApp:assembleDebug
```
