# Requirements Document

## Introduction

This document defines the requirements for an Android application designed to help users practice for the AWS Certified Developer Associate exam. The app presents multiple-choice questions covering various AWS services (RDS, DynamoDB, SQS, SNS, EC2, ELB, CloudFormation, Elastic Beanstalk, IAM, S3, VPC, EBS, AutoScaling, SWF, etc.). Questions are loaded in random order each session, and users can save their progress to continue later. The app supports both single-answer and multiple-answer question types, displays explanations after answering, and tracks user performance.

## Glossary

- **Quiz_App**: The native Android application for AWS exam practice
- **Question_Bank**: The complete set of exam questions stored locally within the application
- **Session**: A single quiz attempt containing a randomized subset or full set of questions from the Question_Bank
- **Question**: An individual exam item consisting of question text, multiple choice options, a correct answer, and an explanation
- **Single_Answer_Question**: A Question type where exactly one option is the correct answer
- **Multiple_Answer_Question**: A Question type where two or more options form the correct answer
- **Session_Store**: The local persistence layer that saves and retrieves Session progress
- **Score_Tracker**: The component that calculates and displays the user's performance metrics
- **Question_Shuffler**: The component responsible for randomizing question order within a Session
- **Score_History**: The persistent collection of all completed Session score records used for progress tracking

## Requirements

### Requirement 1: Question Display

**User Story:** As a user, I want to view exam questions with their multiple choice options clearly displayed, so that I can practice answering AWS certification questions.

#### Acceptance Criteria

1. WHEN a Question is presented, THE Quiz_App SHALL display the question text and all associated answer options in the order defined in the Question_Bank, with each option labeled by a letter identifier starting from A
2. WHEN a Single_Answer_Question is presented, THE Quiz_App SHALL display radio buttons for option selection with no option pre-selected, allowing exactly one selection at a time
3. WHEN a Multiple_Answer_Question is presented, THE Quiz_App SHALL display checkboxes for option selection with no option pre-selected, allowing the user to select one or more options without restricting the maximum number of selections
4. WHEN a Multiple_Answer_Question is presented, THE Quiz_App SHALL indicate the number of correct answers expected in the question prompt

### Requirement 2: Answer Submission and Feedback

**User Story:** As a user, I want to submit my answer and receive immediate feedback with explanations, so that I can learn from my mistakes.

#### Acceptance Criteria

1. WHEN the user submits an answer to a Single_Answer_Question, THE Quiz_App SHALL compare the selected option against the correct answer and determine the submission as correct only if the selected option matches the correct answer exactly
2. WHEN the user submits an answer to a Multiple_Answer_Question, THE Quiz_App SHALL compare the selected options against the correct answers and determine the submission as correct only if all correct options are selected and no incorrect options are selected
3. WHEN the user submits a correct answer, THE Quiz_App SHALL display a visually distinct success indicator and the explanation text
4. WHEN the user submits an incorrect answer to a Single_Answer_Question, THE Quiz_App SHALL highlight the user's incorrect selection, indicate the correct option, and show the explanation text
5. WHEN the user submits an incorrect answer to a Multiple_Answer_Question, THE Quiz_App SHALL highlight the user's incorrect selections, indicate all correct options, and show the explanation text
6. WHILE no option is selected for the current Question, THE Quiz_App SHALL disable the submit button
7. WHEN the user has submitted an answer for the current Question, THE Quiz_App SHALL disable the submit button and prevent modification of the selected options

### Requirement 3: Question Randomization

**User Story:** As a user, I want questions to appear in a random order each time I start a new session, so that I do not memorize answers by position.

#### Acceptance Criteria

1. WHEN a new Session is started, THE Question_Shuffler SHALL produce a permutation of all questions from the Question_Bank such that each question appears exactly once and no question is omitted
2. WHEN two consecutive new Sessions are started from the same Question_Bank containing 5 or more questions, THE Question_Shuffler SHALL produce a different question order with probability of at least 99 percent
3. WHEN a Session is resumed, THE Quiz_App SHALL preserve the original shuffled order assigned to that Session so that questions appear in the same sequence as when the Session was first created
4. IF the Question_Bank contains fewer than 2 questions when a new Session is started, THEN THE Quiz_App SHALL display an error message indicating that insufficient questions are available and SHALL not create the Session

### Requirement 4: Session Persistence

**User Story:** As a user, I want my quiz progress to be saved automatically, so that I can close the app and continue where I left off.

#### Acceptance Criteria

1. WHEN the user answers a Question, THE Session_Store SHALL persist the current Session state including the current question index, all answers given, and the shuffled question order before any other user interaction is accepted
2. WHEN the app is launched and an incomplete Session exists, THE Quiz_App SHALL offer the user a choice to continue the existing Session or start a new Session
3. WHEN the user chooses to continue, THE Quiz_App SHALL restore the Session to the state defined by the persisted data: the same shuffled question order, all previously submitted answers, and the question index where the user left off
4. WHEN the user has submitted an answer for every Question in a Session, THE Session_Store SHALL mark the Session as completed
5. IF the Session_Store fails to read persisted Session data due to corruption or an unreadable format, THEN THE Quiz_App SHALL discard the corrupted Session, present an error message indicating the saved progress could not be recovered, and allow the user to start a new Session

### Requirement 5: Score Tracking and Results

**User Story:** As a user, I want to see my score and performance summary at the end of a session, so that I can gauge my readiness for the exam.

#### Acceptance Criteria

1. WHEN a Session is completed, THE Score_Tracker SHALL display the total number of correct answers out of total questions, where a Question is scored as correct only if all selected options exactly match the full set of correct options defined for that Question
2. WHEN a Session is completed, THE Score_Tracker SHALL display the percentage score rounded to the nearest whole number
3. WHEN a Session is completed, THE Score_Tracker SHALL display a pass indicator if the percentage score is greater than or equal to 72 percent, or a fail indicator if the percentage score is below 72 percent
4. WHILE a Session is in progress, THE Quiz_App SHALL display the current question number and total question count in the Session

### Requirement 6: Question Navigation

**User Story:** As a user, I want to navigate between questions during my session, so that I can review or skip questions as needed.

#### Acceptance Criteria

1. WHEN the user is on a Question that is not the first Question, THE Quiz_App SHALL display a visible previous-navigation button, and WHEN the user is on the first Question, THE Quiz_App SHALL hide the previous-navigation button
2. WHEN the user is on a Question that is not the last Question, THE Quiz_App SHALL display a visible next-navigation button regardless of whether the current Question has been answered, and WHEN the user is on the last Question, THE Quiz_App SHALL hide the next-navigation button
3. WHEN the user navigates to a previously answered Question, THE Quiz_App SHALL display the previously selected answer and the feedback that was shown, with answer selections in a read-only state that prevents modification
4. WHEN the user navigates to a Question that has not yet been answered, THE Quiz_App SHALL display the question text and answer options in their default unselected state with the submit button available

### Requirement 7: Question Bank Storage

**User Story:** As a developer, I want the exam questions stored locally in a structured format, so that the app functions without network connectivity.

#### Acceptance Criteria

1. THE Quiz_App SHALL store all questions from the Question_Bank in a local JSON data file bundled with the application, containing at least 50 Question entries
2. WHEN the application starts up, THE Quiz_App SHALL parse the JSON data file into Question objects within 3 seconds
3. WHEN the JSON data file contains an entry that is missing any required field (question text, at least 2 answer options, at least one correct answer indicator, or explanation text) or contains malformed values (empty question text, zero-length options, correct answer index out of range), THE Quiz_App SHALL skip that entry, log a warning identifying the skipped entry, and continue parsing remaining entries
4. IF the JSON data file is missing or entirely unparseable, THEN THE Quiz_App SHALL display an error message indicating that questions could not be loaded and SHALL prevent Session creation
5. THE Quiz_App SHALL produce an equivalent Question object (identical question text, identical options in the same order, identical correct answer indicators, and identical explanation text) when a valid Question is parsed from JSON, serialized back to JSON, and parsed again

### Requirement 8: New Session Creation

**User Story:** As a user, I want to start a fresh quiz session at any time, so that I can practice multiple times with different question orders.

#### Acceptance Criteria

1. WHEN the user selects the new session option, THE Quiz_App SHALL create a new Session with a freshly randomized question order, starting at question 1 with no recorded answers
2. WHILE an incomplete Session exists, WHEN the user selects the new session option, THE Quiz_App SHALL prompt the user to confirm abandoning the existing Session before creating a new one
3. WHEN the user confirms abandoning the existing Session, THE Session_Store SHALL delete the incomplete Session data and create the new Session
4. WHEN the user declines to abandon the existing Session, THE Quiz_App SHALL return the user to the existing Session at the question where they left off

### Requirement 9: Score History and Progress Tracking

**User Story:** As a user, I want to see the scores of all my previous exam attempts, so that I can track whether my performance is improving over time.

#### Acceptance Criteria

1. WHEN a Session is completed, THE Session_Store SHALL persist a history record containing the session completion date, the percentage score, and the pass or fail result
2. WHEN the user navigates to the score history view, THE Quiz_App SHALL display a list of all past completed Session records ordered by completion date from most recent to oldest, showing the date, percentage score, and pass or fail indicator for each entry
3. WHEN the score history contains 2 or more entries, THE Quiz_App SHALL display a progress trend indicator showing whether the user's scores are improving, declining, or stable based on comparison of the most recent 5 session scores
4. IF no completed Sessions exist in the score history, THEN THE Quiz_App SHALL display a message indicating that no past attempts are available
