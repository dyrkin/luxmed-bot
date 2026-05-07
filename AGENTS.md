# Agent Guidelines

## After Implementing a Feature

Always run the full test suite after finishing any feature implementation:

```bash
./gradlew test
```

## Before Starting Any Task

Compare the Java version specified in `build.gradle` with the Java version currently active in the console.

1. Check the version in `build.gradle`:
   - Look at `sourceCompatibility` / `targetCompatibility` under the `java` block (e.g., `JavaVersion.VERSION_25` → Java 25).

2. Check the active Java version in the console:
   ```bash
   java -version
   ```

3. If they differ, switch the console Java version to match `build.gradle` using `sdk`:
   ```bash
   sdk use java <version>
   ```
   For example, if `build.gradle` specifies Java 25:
   ```bash
   sdk use java 25-open
   ```

Always ensure the console Java version matches `build.gradle` before proceeding with any task.
