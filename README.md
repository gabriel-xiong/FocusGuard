# FocusGuard
Demo Link: https://youtube.com/shorts/b8om0cERWRE?si=qW4sHb--sjr7N6NJ


FocusGuard is an Android study app that helps students protect focus time. The app runs on-device computer vision during a focus session, estimates whether the user is still studying, pauses the session when attention is lost, and blocks distracting apps until the user returns to FocusGuard.

The project was built for the Qualcomm x Google LiteRT Hackathon and targets a Samsung Galaxy S25 Ultra with Snapdragon 8 Elite / Hexagon NPU acceleration.

## Team

| Name | Email |
| --- | --- |
| Gabriel Xiong | gpdxiong@gmail.com |
| Rayan Ahmed | rayan.ahmed3638@gmail.com |
| Sanjiv Saravanan | sanjivps17@gmail.com |
| Sual Harun | sualharun.w@gmail.com |

## Application Description

FocusGuard turns focused study into a protected, rewarded session:

- The student selects a study duration.
- The student selects distracting apps to block.
- The front camera activates only during the active focus session.
- Three LiteRT models run locally to detect face presence, face landmarks, eye openness, and gaze direction.
- The app calibrates to the student's normal study posture for the first 45 detected-face frames.
- If the student keeps studying, the timer counts down normally.
- If the student looks away, leaves frame, closes their eyes, or produces low-confidence vision signals, the timer pauses.
- If the student opens a blacklisted app, an Accessibility Service immediately returns them to EarnedIt.
- Completing focus time unlocks a reward window and feeds a pet/progression system.

No camera frame needs to leave the device for attention detection. Optional social features use Supabase only for profile/friend data when configured.

## Features And Functionality

### Focus Sessions

- Duration-based focus sessions.
- Session phases: `Idle`, `FocusActive`, `RewardActive`, `Paused`, `Failed`, and `Complete`.
- Per-session focus score from `0.0` to `1.0`.
- Focused seconds and distracted seconds tracking.
- Timer freezes during calibration and pauses while distracted.
- Reward window after successful focus completion.

### On-Device ML Attention Detection

The ML pipeline is in `app/src/main/java/com/focusguard/ml`.

```text
CameraX front camera
    -> rotate and mirror frame
    -> face_detector.tflite
    -> face crop
    -> face_landmark_detector.tflite
    -> eye landmarks, eye aspect ratio, eye crop
    -> eyegaze.tflite
    -> pitch/yaw in radians
    -> pitch/yaw in degrees
    -> calibration baseline subtraction
    -> AttentionSignal
    -> AttentionScorer
    -> SessionManager
    -> Compose UI and app-blocking service
```

The app ships three frozen `.tflite` models:

- `face_detector.tflite`: detects the face and produces bounding boxes/confidence.
- `face_landmark_detector.tflite`: extracts 468 facial landmarks.
- `eyegaze.tflite`: estimates gaze pitch and yaw from the eye crop.

Model execution uses Google LiteRT `CompiledModel`. `FallbackCompiledModel` tries accelerators in this order:

```text
NPU -> GPU -> CPU
```

The fallback applies to model creation and runtime inference. If an accelerator fails during `run(...)`, the wrapper recreates model buffers on the next accelerator, rewrites the input, and retries.

### Calibration

When a focus session starts:

- the first 45 detected-face frames are used for calibration,
- raw yaw and pitch are averaged,
- the focus timer is frozen,
- later yaw/pitch values are baseline-subtracted.

Calibration answers "what does normal study posture look like for this user and phone position?" before scoring begins.

### Attention Scoring

The scorer lives in `app/src/main/java/com/focusguard/session/AttentionScorer.kt`.

A signal is distracted when:

- no face is detected,
- face confidence or landmark/eye confidence is too low,
- calibrated yaw/pitch is outside the tuned focus zone,
- eye aspect ratio indicates eyes closed.

The tuned focus zone is:

```kotlin
yaw in -25.0..20.0 && pitch in -15.0..18.0
```

This range is asymmetric by design. Calibration recenters the user's neutral posture, while threshold tuning defines how much natural movement is acceptable around that center. Labeled data showed that real focused study behavior included more downward reading posture and slight leftward glances than a symmetric threshold allowed.

### App Blocking

`FocusAccessibilityService` watches foreground-window changes while a session is active. If the foreground app is blacklisted, EarnedIt is relaunched and the blocked attempt is recorded.

The service intentionally ignores:

- EarnedIt itself,
- Android/system UI/settings packages,
- the emergency dialer,
- reward windows,
- duplicate rapid-fire events from the same app.

### Pet, Rewards, Insights, And Social

The product layer includes:

- onboarding and pet selection,
- home dashboard with pet progress,
- pet evolution and cosmetics,
- feeding/play/encouragement interactions,
- session results,
- insights screens,
- optional social profiles and friend requests through Supabase.

Local app state is handled by `EarnedItStore`. Supabase schema is in `supabase/social_schema.sql`.

### Debug And Quality Tools

Instrumentation lives in `app/src/main/java/com/focusguard/instrumentation`.

- `BenchmarkRegistry`: per-stage timing.
- `BenchmarkReporter`: JSON benchmark reports with p50, p95, p99, mean, samples, memory, FPS, and thermal status.
- `SystemMetricsSampler`: memory, GC pressure, frame count, FPS, and thermal tracking.
- `QualityCaptureManager`: opt-in recording mode that saves every fifth frame as JPEG plus sidecar JSON.
- `DebugInstrumentationState`: debug switches for recording mode and frame stamping.

Captured sidecars include face confidence, bounding box, landmark score, raw yaw/pitch, calibration baseline, baseline-subtracted yaw/pitch, eye landmarks, focused-zone decision, session score, timestamp, and a label field for offline annotation.

## Fine-Tuning Process

We started with an off-the-shelf head-pose-based focus detector that produced too many false negatives: the timer paused unfairly during normal studying. We did not retrain any model. All three models stayed frozen; only the decision rule on top of model outputs was tuned.

Original rule:

```kotlin
abs(yaw) < 20.0 && abs(pitch) < 15.0
```

Training session:

- 6 minutes,
- scripted focused and distracted segments,
- 664 captured frames,
- human frame labels,
- 0.5-second transition buffer around segment boundaries.

Original-rule performance:

| Metric | Original rule |
| --- | ---: |
| Overall agreement | 79.3% |
| Focused recall | 73.4% |
| Distraction recall | 96.6% |
| False negatives | 116 |

Error analysis showed that 79% of false negatives failed only the pitch threshold. Median false-negative pitch was 17.3 degrees, which matched natural downward reading posture.

Final tuned rule:

```kotlin
yaw in -25.0..20.0 && pitch in -15.0..18.0
```

Training-session improvement:

| Metric | Old rule | Tuned rule |
| --- | ---: | ---: |
| Overall agreement | 79.3% | 88.4% |
| Focused recall | 73.4% | 86.9% |
| Distraction recall | 96.6% | 92.6% |
| False negatives | 116 | 57 |

Held-out validation:

- frame-level agreement: 84.5%,
- rolling-window agreement: 92.7%,
- clean rolling-window agreement after removing one setup-geometry confound: 93.6%,
- distraction recall for look-down and look-right: 98.4%,
- reaction time: under one captured frame.

The validation set now gives us a way to evaluate future optimizations such as quantization, frame skipping, AOT compilation, or accelerator changes without guessing whether focus quality regressed.

## Repository Structure

```text
focus-guard/
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      assets/
        face_detector.tflite
        face_landmark_detector.tflite
        eyegaze.tflite
      java/com/focusguard/
        MainActivity.kt
        instrumentation/
        ml/
        pet/
        service/
        session/
        social/
        state/
        ui/
      res/
  docs/
    accessibility-service-qa.md
  supabase/
    social_schema.sql
  build.gradle.kts
  settings.gradle.kts
  gradlew
  gradlew.bat
```

## Dependencies

Install these before building from scratch:

- Git
- Android Studio Ladybug or newer, or Android command-line tools with SDK support
- JDK 17
- Android SDK 35
- Android device or emulator with camera support
- Recommended physical device: Samsung Galaxy S25 Ultra or another modern Snapdragon Android device

Gradle dependencies are declared in `app/build.gradle.kts` and are downloaded by the Gradle wrapper:

- Kotlin Android plugin
- Jetpack Compose and Material 3
- CameraX
- AndroidX lifecycle/navigation/datastore
- Kotlin coroutines
- Google LiteRT `com.google.ai.edge.litert:litert:2.1.0`

Optional social features require a Supabase project and anon key.

## Setup From Scratch

1. Clone the repository.

```powershell
git clone https://github.com/sualharun/Earned.git
cd Earned\focus-guard
```

2. Open the `focus-guard` folder in Android Studio.

3. Confirm Android Studio is using JDK 17.

4. Install Android SDK 35 if prompted.

5. Sync Gradle.

6. Optional: create `focus-guard/local.properties` for Supabase social features.

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

If this file is omitted, the core focus app still works. Social actions show a configuration-needed state when the anon key is missing.

7. Connect a physical Android device and enable developer options/USB debugging.

8. Build the debug APK.

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS/Linux:

```bash
./gradlew :app:assembleDebug
```

## Run Instructions

From Android Studio:

1. Select the `app` run configuration.
2. Select a connected Android device.
3. Press Run.
4. Grant camera permission when prompted.

From the command line:

```powershell
.\gradlew.bat :app:installDebug
```

Then open EarnedIt from the launcher.

## Usage Instructions

1. Complete onboarding and choose a pet.
2. Open the focus setup screen.
3. Select a focus duration.
4. Select distracting apps to block.
5. Enable the accessibility service when prompted or manually:
   - Android Settings
   - Accessibility
   - EarnedIt App Blocking
   - Turn on
6. Start a focus session.
7. Hold the phone near the study target so the camera sees your face.
8. Stay naturally focused while calibration collects 45 detected-face frames.
9. Study normally.
10. Look away, leave frame, or close your eyes to see the session pause.
11. Try opening a blacklisted app to confirm EarnedIt bounces you back.
12. Finish the focus timer to enter the reward window.

Best results:

- Put the phone close to the laptop/book/notebook you are actually using.
- Keep your face visible during calibration.
- Avoid calibrating while looking far away from the main study material.

## Testing Instructions

Compile check:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Full debug build:

```powershell
.\gradlew.bat :app:assembleDebug
```

Accessibility-service manual QA:

```text
docs/accessibility-service-qa.md
```

Suggested device QA:

1. Install the debug build on a physical device.
2. Grant camera permission.
3. Enable the Accessibility Service.
4. Start a short focus session.
5. Verify calibration completes.
6. Verify timer counts down while focused.
7. Verify timer pauses when face is missing or gaze is outside the tuned zone.
8. Verify a blacklisted app returns the user to EarnedIt.
9. Verify `com.android.dialer` is not blocked.
10. Verify the reward window does not block selected apps.

Suggested instrumentation QA:

1. Start a focus session.
2. Enable recording mode from debug controls.
3. Run a short scripted focused/distracted sequence.
4. Pull files from app external storage.
5. Inspect `quality_capture/<session_id>/manifest.json`.
6. Inspect frame sidecars for yaw, pitch, confidence, and focused-zone values.
7. Inspect benchmark JSON under `benchmarks/`.

## Notes

- Camera-based focus detection is sensitive to phone placement. The best setup is a phone position near the study material.
- The tuned threshold was validated for the captured study setup. Different desk geometry can require future calibration or threshold work.
- The package name remains `com.focusguard`, while the product UI/README uses EarnedIt.
- The app uses NPU first but can fall back to GPU or CPU for reliability.
- Social features are optional; focus detection and app blocking do not require network access.
- The current app icon is Android's built-in compass icon.

## References

- Google AI Edge LiteRT documentation: https://ai.google.dev/edge/litert
- LiteRT Android/Kotlin APIs: https://ai.google.dev/edge/litert/android
- Qualcomm AI Hub MediaPipe Face model family: https://aihub.qualcomm.com/models/mediapipe_face
- Android CameraX documentation: https://developer.android.com/media/camera/camerax
- Android Accessibility Service documentation: https://developer.android.com/guide/topics/ui/accessibility/service
- Jetpack Compose documentation: https://developer.android.com/compose
- Supabase REST/API documentation: https://supabase.com/docs
