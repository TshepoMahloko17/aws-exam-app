# Implementation Plan: AWS Exam Quiz App

## Overview

This plan implements a native Android quiz application for AWS Developer Associate exam practice using Kotlin, Jetpack Compose, Room, and Hilt. Tasks proceed from data models and core logic through persistence, to UI and integration, ensuring each step builds on the previous and no code is left orphaned.

## Tasks

- [x] 1. Set up project structure, dependencies, and core data models
  - [x] 1.1 Configure Gradle dependencies and project scaffolding
    - Add dependencies: Jetpack Compose, Room, Hilt, kotlinx.serialization, Jetpack Navigation Compose, JUnit 5, Kotest property testing, Turbine
    - Configure Hilt with `@HiltAndroidApp` Application class
    - Set minSdk = 26, enable Compose compiler, configure kotlinx.serialization plugin
    - Create package structure: `domain`, `data`, `ui`, `di`
    - _Requirements: 7.1_

  - [x] 1.2 Define core data model classes
    - Create `Question` data class with `@Serializable` annotation, fields: id, text, options, correctAnswers (Set<Int>), explanation; computed properties `isSingleAnswer` and `isMultipleAnswer`
    - Create `Session` data class with id, questionOrder, currentIndex, answers map, isCompleted, createdAt
    - Create `AnswerRecord` data class with selectedOptions (Set<Int>) and isCorrect
    - Create `AnswerResult` data class with isCorrect, selectedOptions, correctOptions
    - Create `ScoreResult` data class with correctCount, totalCount, percentage, passed
    - Create `ScoreHistoryRecord` data class with id, completionDate, percentage, passed
    - Create `AppError` sealed class with QuestionLoadError, SessionCorruptionError, InsufficientQuestionsError, ScoreHistoryError
    - _Requirements: 1.1, 5.1, 5.2, 5.3, 9.1_

  - [x] 1.3 Define repository interfaces
    - Create `QuestionRepository` interface with `suspend fun loadQuestions(): Result<List<Question>>`
    - Create `SessionRepository` interface with saveSession, loadIncompleteSession, deleteSession, markSessionCompleted methods
    - Create `ScoreHistoryRepository` interface with saveScore, getAllScores, getRecentScores methods
    - _Requirements: 4.1, 7.1, 9.1_

- [-] 2. Implement domain logic (pure functions)
  - [x] 2.1 Implement AnswerEvaluator
    - Create `AnswerEvaluator` object with `evaluate(question: Question, selectedOptions: Set<Int>): AnswerResult`
    - For single-answer: correct if selected set equals correctAnswers set
    - For multi-answer: correct only if selected set exactly equals correctAnswers set (set equality)
    - Return AnswerResult with correctness flag, selectedOptions, and correctOptions from question
    - _Requirements: 2.1, 2.2_

  - [x] 2.2 Write property test for AnswerEvaluator
    - **Property 1: Answer Evaluation Correctness**
    - Generate random Questions (2-6 options, 1-3 correct answers) and random selection sets
    - Assert isCorrect == true iff selectedOptions equals question.correctAnswers as sets
    - Use Kotest `forAll` with minimum 100 iterations
    - **Validates: Requirements 2.1, 2.2**

  - [x] 2.3 Implement QuestionShuffler
    - Create `QuestionShuffler` interface and `DefaultQuestionShuffler` implementation
    - `fun shuffle(questionCount: Int): List<Int>` — returns shuffled indices [0..questionCount-1]
    - Use `kotlin.random.Random` with time-based seed
    - Ensure output is a valid permutation (each index appears exactly once)
    - _Requirements: 3.1, 3.2_

  - [x] 2.4 Write property tests for QuestionShuffler
    - **Property 2: Shuffle Permutation Invariant**
    - Generate random counts 2..500, assert sorted output == [0, 1, ..., n-1]
    - **Property 3: Shuffle Non-Determinism**
    - Generate random counts 5..500, call shuffle twice, assert outputs differ (run 100+ iterations)
    - **Validates: Requirements 3.1, 3.2**

  - [x] 2.5 Implement ScoreTracker
    - Create `ScoreTracker` object with `calculateScore(answers: List<AnswerResult>, totalQuestions: Int): ScoreResult`
    - Count correct answers, compute percentage = round(correctCount / totalCount * 100), determine passed = percentage >= 72
    - Handle edge case of totalQuestions = 0 gracefully
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 2.6 Write property test for ScoreTracker
    - **Property 6: Score Calculation Correctness**
    - Generate random lists of AnswerResult (0..200 items) with random isCorrect flags
    - Assert correctCount == count of true results, percentage == round(correctCount/total*100), passed == (percentage >= 72)
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [x] 2.7 Implement ProgressTrendCalculator
    - Create `ProgressTrendCalculator` object with `calculateTrend(recentScores: List<Int>): ProgressTrend`
    - Create `ProgressTrend` enum: IMPROVING, DECLINING, STABLE
    - Compute linear regression slope over score list (indexed 0..n-1); IMPROVING if slope > 2, DECLINING if slope < -2, STABLE otherwise
    - Return STABLE if fewer than 2 scores provided
    - _Requirements: 9.3_

  - [x] 2.8 Write property test for ProgressTrendCalculator
    - **Property 13: Progress Trend Calculation**
    - Generate random lists of 0-5 percentage scores (0-100)
    - Assert trend matches expected result based on linear regression slope with ±2 threshold
    - Assert STABLE returned for lists with fewer than 2 scores
    - **Validates: Requirements 9.3**

- [x] 3. Checkpoint - Core domain logic
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement data layer (Room database and JSON parsing)
  - [x] 4.1 Set up Room database, entities, and DAOs
    - Create `SessionEntity` with @Entity annotation, fields: id (PrimaryKey), questionOrderJson, currentIndex, answersJson, isCompleted, createdAt
    - Create `ScoreHistoryEntity` with @Entity annotation, fields: id (PrimaryKey), completionDate, percentage, passed
    - Create `SessionDao` with upsertSession, getIncompleteSession, deleteSession methods
    - Create `ScoreHistoryDao` with insertScore, getAllScores, getRecentScores methods
    - Create `AppDatabase` abstract class extending RoomDatabase with version = 2
    - _Requirements: 4.1, 9.1_

  - [x] 4.2 Implement SessionRepository with Room persistence
    - Create `RoomSessionRepository` implementing `SessionRepository`
    - Implement serialization of questionOrder (List<Int>) and answers (Map<Int, AnswerRecord>) to/from JSON strings for Room storage
    - Implement saveSession: convert Session → SessionEntity, upsert to DB
    - Implement loadIncompleteSession: query DAO, convert SessionEntity → Session, return null if none found, return failure on corruption
    - Implement deleteSession and markSessionCompleted
    - _Requirements: 4.1, 4.3, 4.4, 4.5_

  - [x] 4.3 Write property test for Session persistence round-trip
    - **Property 4: Session Persistence Round-Trip**
    - Generate random Session objects with varying questionOrder, currentIndex, answers maps, and completion status
    - Save to repository, load back, assert all fields identical
    - **Property 5: Session Completion Detection**
    - Generate sessions where answers.size == questionOrder.size (completed) and answers.size < questionOrder.size (incomplete)
    - Assert completion status correctly determined
    - **Validates: Requirements 3.3, 4.1, 4.3, 4.4**

  - [x] 4.4 Implement ScoreHistoryRepository with Room persistence
    - Create `RoomScoreHistoryRepository` implementing `ScoreHistoryRepository`
    - Implement saveScore: convert ScoreHistoryRecord → ScoreHistoryEntity, insert to DB
    - Implement getAllScores: query DAO, convert entities to domain records, order by completionDate DESC
    - Implement getRecentScores(count): query DAO with limit, return in descending date order
    - _Requirements: 9.1, 9.2_

  - [x] 4.5 Write property tests for ScoreHistory persistence
    - **Property 11: Score History Persistence Round-Trip**
    - Generate random ScoreHistoryRecord objects, save and load, assert identical fields
    - **Property 12: Score History Ordering**
    - Generate random sets of records with distinct dates, save all, load all, assert descending date order
    - **Validates: Requirements 9.1, 9.2**

  - [x] 4.6 Implement QuestionRepository (JSON asset parsing)
    - Create `JsonQuestionRepository` implementing `QuestionRepository`
    - Load JSON from assets using Android's AssetManager
    - Parse using kotlinx.serialization; skip invalid entries (missing fields, empty text, zero options, out-of-range correctAnswer indices), log warnings
    - Return error result if file is missing or entirely unparseable
    - Cache parsed questions in memory after successful load
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 4.7 Write property tests for JSON parsing
    - **Property 8: Parser Resilience**
    - Generate mixed lists of valid and invalid JSON question entries
    - Assert parser returns exactly the valid entries in original relative order
    - **Property 9: Question Serialization Round-Trip**
    - Generate random valid Question objects, serialize to JSON, parse back, assert equivalence
    - **Validates: Requirements 7.3, 7.5**

- [x] 5. Checkpoint - Data layer complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement Hilt dependency injection module
  - [x] 6.1 Create DI module for repositories and database
    - Create `@Module` with `@InstallIn(SingletonComponent::class)`
    - Provide Room database instance (singleton)
    - Provide SessionDao and ScoreHistoryDao from database
    - Bind `RoomSessionRepository` to `SessionRepository`
    - Bind `RoomScoreHistoryRepository` to `ScoreHistoryRepository`
    - Bind `JsonQuestionRepository` to `QuestionRepository`
    - Provide `DefaultQuestionShuffler` bound to `QuestionShuffler` interface
    - _Requirements: 7.1, 4.1, 9.1_

- [x] 7. Implement ViewModels
  - [x] 7.1 Implement HomeViewModel
    - Inject SessionRepository and QuestionRepository via Hilt
    - On init: check for incomplete session, expose UI state (hasIncompleteSession, canStartNew)
    - Handle "start new session": if incomplete session exists, trigger confirmation dialog state; on confirm, delete old session and create new one with shuffled order
    - Handle "resume session": load incomplete session, navigate to quiz
    - Handle question load error: expose error state for ErrorScreen
    - Validate minimum 2 questions available; expose InsufficientQuestionsError if not met
    - _Requirements: 3.4, 4.2, 8.1, 8.2, 8.3, 8.4_

  - [x] 7.2 Implement QuizViewModel
    - Inject SessionRepository, QuestionRepository, AnswerEvaluator, ScoreTracker, ScoreHistoryRepository
    - Manage quiz state: current question, selected options, submitted answers, navigation position
    - Submit answer: evaluate using AnswerEvaluator, record in session, persist session state immediately
    - Navigation: expose previous/next availability based on current index, navigate without restrictions on answered status
    - On all questions answered: mark session completed, calculate score, save score history record, expose result state
    - Expose progress indicator (current question number / total)
    - Handle session corruption: expose error state
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 4.1, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4_

  - [x] 7.3 Write unit tests for QuizViewModel
    - Test initial state: no options selected, submit button disabled
    - Test answer submission: correct/incorrect feedback displayed
    - Test navigation: previous hidden on first, next hidden on last
    - Test answered question revisit: read-only state with feedback shown
    - Test session completion: score calculated and displayed
    - _Requirements: 1.2, 1.3, 2.3, 2.4, 2.5, 2.6, 2.7, 5.4, 6.1, 6.2, 6.3, 6.4_

  - [x] 7.4 Implement HistoryViewModel
    - Inject ScoreHistoryRepository and ProgressTrendCalculator
    - On init: load all scores, compute progress trend from most recent 5 scores
    - Expose UI state: list of ScoreHistoryRecord, ProgressTrend, empty state flag
    - Handle load failure: expose error state with retry capability
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 7.5 Write unit tests for HistoryViewModel
    - Test empty history: shows "no past attempts" message state
    - Test loaded history: scores in descending date order
    - Test trend calculation: improving, declining, stable scenarios
    - Test error state: load failure shows error with retry
    - _Requirements: 9.2, 9.3, 9.4_

  - [x] 7.6 Write property test for navigation logic
    - **Property 7: Navigation Button Visibility**
    - Generate random currentIndex and totalCount pairs where 0 <= currentIndex < totalCount
    - Assert previous visible iff currentIndex > 0, next visible iff currentIndex < totalCount - 1
    - **Validates: Requirements 6.1, 6.2**

  - [x] 7.7 Write property test for multi-answer count indicator
    - **Property 10: Multi-Answer Count Indicator**
    - Generate random multi-answer Questions, assert displayed count equals correctAnswers.size
    - **Validates: Requirements 1.4**

- [x] 8. Checkpoint - ViewModels complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement UI screens with Jetpack Compose
  - [x] 9.1 Implement HomeScreen
    - Display "Start New Session" button (always visible)
    - Display "Continue Session" button only when incomplete session exists
    - Display "View History" button for navigation to history
    - Show confirmation dialog when starting new session while incomplete session exists
    - Show error state when questions cannot be loaded or fewer than 2 available
    - Wire to HomeViewModel state and event handlers
    - _Requirements: 4.2, 8.1, 8.2, 8.3, 8.4, 3.4, 7.4_

  - [x] 9.2 Implement QuizScreen
    - Display question text with letter-labeled options (A, B, C, D...)
    - For single-answer questions: render radio buttons, allow exactly one selection
    - For multi-answer questions: render checkboxes, display "Select N answers" hint where N = correctAnswers.size
    - Submit button: disabled when no option selected or after submission
    - After submission: show success/failure indicator, highlight correct/incorrect options, display explanation text
    - Navigation: previous button (hidden on first question), next button (hidden on last question)
    - Progress indicator: "Question X of Y"
    - Previously answered questions: display in read-only state with prior feedback
    - Wire to QuizViewModel state and event handlers
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 5.4, 6.1, 6.2, 6.3, 6.4_

  - [x] 9.3 Implement ResultScreen
    - Display correct answers count out of total (e.g., "35/50")
    - Display percentage score rounded to nearest whole number
    - Display pass indicator (green, "PASSED") if percentage >= 72, fail indicator (red, "FAILED") otherwise
    - Display "Return to Home" button
    - Wire to score data passed from QuizViewModel
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 9.4 Implement HistoryScreen
    - Display list of past sessions: date, percentage, pass/fail badge for each entry
    - Order by completion date descending (most recent first)
    - Display progress trend indicator (arrow up / arrow down / flat line) when 2+ entries exist
    - Display "No past attempts" message when history is empty
    - Wire to HistoryViewModel state
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 9.5 Implement ErrorScreen
    - Display error message text
    - Display "Start New Session" or "Return to Home" button depending on error context
    - Handle: missing question file, unparseable file, corrupt session, insufficient questions
    - _Requirements: 3.4, 4.5, 7.4_

- [x] 10. Set up navigation and wire screens together
  - [x] 10.1 Configure Jetpack Navigation Compose
    - Define navigation graph with routes: home, quiz, result, history, error
    - Pass score data to ResultScreen via navigation arguments
    - Handle back navigation appropriately (prevent returning to quiz after completion)
    - Wire HomeScreen buttons to navigation actions
    - Integrate Hilt with navigation (hiltViewModel() in composables)
    - _Requirements: 4.2, 8.1, 9.2_

- [x] 11. Create question bank JSON asset file
  - [x] 11.1 Create questions.json with full question data
    - Create `assets/questions.json` file with at least 50 question entries covering AWS services (RDS, DynamoDB, SQS, SNS, EC2, ELB, CloudFormation, Elastic Beanstalk, IAM, S3, VPC, EBS, AutoScaling, SWF)
    - Include mix of single-answer and multiple-answer questions
    - Each entry: id, text, options (at least 2), correctAnswers (indices), explanation
    - Validate JSON structure matches the schema defined in design
    - _Requirements: 7.1_

- [x] 12. Checkpoint - Full app integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Final integration and edge case handling
  - [x] 13.1 Implement edge case handling and error paths
    - Handle question bank with 0 or 1 valid questions (show error, prevent session)
    - Handle session save failure (retry once, then show error)
    - Handle score history save failure (log error, show toast, don't block result display)
    - Handle score history read failure (show error on history screen with retry button)
    - Ensure JSON parsing completes within 3 seconds for 80+ questions (optimize if needed)
    - _Requirements: 3.4, 4.5, 7.2, 7.4, 9.4_

  - [x] 13.2 Write integration tests for full session lifecycle
    - Test: create session → answer all questions → complete → verify result screen data → verify score history record saved
    - Test: create session → answer partial → close → resume → verify state preserved
    - Test: complete multiple sessions → verify history accumulates and ordering correct
    - Test: JSON load performance within 3 seconds
    - _Requirements: 4.1, 4.3, 5.1, 7.2, 9.1_

- [x] 14. Final checkpoint - All features complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The app uses Kotlin with Jetpack Compose, Room, Hilt, and kotlinx.serialization
- All domain logic (AnswerEvaluator, ScoreTracker, QuestionShuffler, ProgressTrendCalculator) is pure and testable without Android dependencies
- Room persistence tests (Properties 4, 5, 11, 12) require Android instrumented test context or in-memory Room database

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "2.3", "2.5", "2.7"] },
    { "id": 3, "tasks": ["2.2", "2.4", "2.6", "2.8", "4.1"] },
    { "id": 4, "tasks": ["4.2", "4.4", "4.6"] },
    { "id": 5, "tasks": ["4.3", "4.5", "4.7", "6.1"] },
    { "id": 6, "tasks": ["7.1", "7.2", "7.4", "11.1"] },
    { "id": 7, "tasks": ["7.3", "7.5", "7.6", "7.7"] },
    { "id": 8, "tasks": ["9.1", "9.2", "9.3", "9.4", "9.5"] },
    { "id": 9, "tasks": ["10.1"] },
    { "id": 10, "tasks": ["13.1"] },
    { "id": 11, "tasks": ["13.2"] }
  ]
}
```
