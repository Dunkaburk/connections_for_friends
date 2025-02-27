# CLAUDE.md - Agent Guide for connections_for_friends

## Build Commands
- Build project: `./gradlew build`
- Build app module: `./gradlew :app:build`
- Run app: `./gradlew installDebug`
- Clean build: `./gradlew clean`

## Test & Lint Commands
- Run all tests: `./gradlew test`
- Run unit tests: `./gradlew testDebugUnitTest`
- Run single test: `./gradlew :app:testDebugUnitTest --tests "com.example.connections_for_friends.TestClass.testMethod"`
- Android instrumentation tests: `./gradlew connectedAndroidTest`
- Run lint: `./gradlew lint`

## Code Style Guidelines
- Use Kotlin style guide (https://developer.android.com/kotlin/style-guide)
- Imports: Group and order by package (Android/Androidx first, then alphabetically)
- Naming: camelCase for variables/functions, PascalCase for classes/interfaces
- Composables: PascalCase for @Composable functions
- Annotations: Place on separate lines above declaration
- Types: Always specify explicit types for public APIs
- Error handling: Use Kotlin's Result type for operations that can fail
- Strings: Extract to strings.xml for user-facing text
- Compose theme: Follow Material3 design system