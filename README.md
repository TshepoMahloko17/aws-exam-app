# AWS Exam Quiz App

A native Android application for practicing AWS Certified Developer Associate exam questions. Works entirely offline with locally bundled questions.

## Features

- 50+ multiple-choice questions covering core AWS services (RDS, DynamoDB, SQS, SNS, EC2, IAM, S3, VPC, CloudFormation, and more)
- Single-answer and multiple-answer question types
- Randomized question order each session
- Immediate feedback with explanations after each answer
- Session persistence — close the app and resume where you left off
- Score tracking with pass/fail indicator (72% threshold)
- Score history with progress trend analysis
- Full navigation between questions (review previous answers)

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository Pattern |
| Navigation | Jetpack Navigation Compose |
| DI | Hilt |
| Persistence | Room |
| JSON Parsing | kotlinx.serialization |
| Testing | JUnit 5, Kotest (property-based), Turbine |
| Min SDK | 26 (Android 8.0) |

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API 34

### Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install on connected device/emulator
./gradlew installDebug
```

## Project Structure

```
app/src/main/java/com/awsquiz/app/
├── domain/          # Pure business logic (evaluator, shuffler, score tracker)
├── data/            # Room database, repositories, JSON parsing
├── di/              # Hilt dependency injection module
└── ui/
    ├── home/        # Home screen (start/resume/history)
    ├── quiz/        # Quiz screen (questions, answers, navigation)
    ├── result/      # Result screen (score, pass/fail)
    ├── history/     # Score history with trend indicator
    ├── error/       # Error state screens
    ├── navigation/  # Navigation graph
    └── theme/       # Material 3 theme
```

## Testing

The project uses a dual testing strategy:

- **Property-based tests** (Kotest) — verify universal correctness properties across randomized inputs
- **Unit tests** (JUnit 5) — verify specific examples and edge cases
- **Integration tests** — verify full session lifecycle flows

Run all tests:

```bash
./gradlew testDebugUnitTest
```

## License

This project is for educational purposes.
