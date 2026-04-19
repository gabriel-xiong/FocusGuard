# Accessibility Service Manual QA

Issue: EAR-21

This checklist verifies the FocusGuard Accessibility Service app-blocking behavior for the hackathon demo.

## Test Setup

- Build variant: `debug`
- App package: `com.focusguard`
- Service: `com.focusguard.service.FocusAccessibilityService`
- Demo blocked app: Chrome (`com.android.chrome`) or another installed non-critical app
- Always-allowed packages:
  - FocusGuard (`com.focusguard`)
  - Android Settings (`com.android.settings`)
  - System UI (`com.android.systemui`)
  - Emergency dialer (`com.android.dialer`)

## Run Log

Fill this in during the device run.

| Field | Value |
| --- | --- |
| Date | 2026-04-30 |
| Tester | Sanjiv |
| Device | TBD |
| Android version | TBD |
| App build | debug |
| Blocked test package | TBD |
| Result | Not run: waiting for connected Android device |

## Preflight

- [ ] Install the debug build on the device.
- [ ] Open FocusGuard once so `SessionManager.initialize(...)` runs.
- [ ] Confirm the test blocked app is installed.
- [ ] Add the test app package to the FocusGuard blacklist.
- [ ] Start a focus session with reward mode inactive.
- [ ] Open Android Settings > Accessibility > FocusGuard App Blocking.
- [ ] Enable the Accessibility Service and accept Android's warning.
- [ ] Return to FocusGuard and confirm the app still opens normally.

## Test Cases

### 1. Service Disabled Flow

1. Disable FocusGuard App Blocking in Android Accessibility settings.
2. Start or keep a focus session active in FocusGuard.
3. Open the blocked test app.

Expected:

- The blocked app opens normally.
- FocusGuard does not bounce to the foreground.
- No blocked attempt is recorded in the visible/debug session state.

### 2. Service Enabled Flow

1. Enable FocusGuard App Blocking in Android Accessibility settings.
2. Return to FocusGuard.
3. Keep the same focus session active.

Expected:

- Android shows FocusGuard App Blocking as enabled.
- FocusGuard remains usable after returning from Settings.
- Settings itself is not bounced or blocked.

### 3. Blacklisted App During Focus Mode

1. Confirm focus mode is active and reward mode is inactive.
2. Open the blacklisted test app from launcher or recents.

Expected:

- FocusGuard returns to the foreground quickly.
- `SessionManager.blockedPackageName` is set to the blocked package.
- The recent blocked attempts list includes the blocked package and a timestamp.
- The focus session remains active.

### 4. Blacklisted App During Reward Mode

1. Complete or force the focus session into reward mode.
2. Open the same blacklisted test app.

Expected:

- The app opens normally during the reward window.
- FocusGuard does not bounce to the foreground.
- No new blocked attempt is recorded for this reward-mode open.

### 5. Non-Blacklisted App

1. Keep focus mode active.
2. Open an app that is not in the blacklist.

Expected:

- The non-blacklisted app opens normally.
- FocusGuard does not bounce to the foreground.
- No blocked attempt is recorded.

### 6. Repeated Blocked App Opens

1. Keep focus mode active.
2. Open the blacklisted test app.
3. Immediately try opening it again several times.

Expected:

- The first open bounces back to FocusGuard.
- Repeated Accessibility events for the same package do not create a rapid launch loop.
- After the debounce window, a new intentional attempt can be recorded.

### 7. FocusGuard, Settings, and System Safety

1. Keep focus mode active.
2. Open FocusGuard.
3. Open Android Settings.
4. Pull down notification shade / interact with System UI.
5. Open Phone / emergency dialer if available.

Expected:

- FocusGuard never blocks itself.
- Android Settings remains accessible for permission recovery.
- System UI remains usable.
- Emergency dialer is never blocked.

## Local Verification

Completed before device run:

- [x] Confirmed `FocusAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOWS_CHANGED`.
- [x] Confirmed service ignores FocusGuard, Settings, System UI, and emergency dialer packages.
- [x] Confirmed service checks reward mode before enforcing blacklist.
- [x] Confirmed service records blocked attempts through `SessionManager.onBlockedAppOpened(...)`.
- [x] Confirmed service debounces repeated blocked package events.
- [ ] Device run completed.
