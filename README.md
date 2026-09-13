# AIopter

Native Android companion and AI orchestration service for **SEE. ASK. DO. ANYWHERE**.

## What is implemented

- Kotlin/Jetpack Compose Android home app targeting API 36 with a real `TYPE_APPLICATION_OVERLAY` floating rotor.
- Draggable edge snapping, persistent placement, animated brand treatment, compact dark assistant panel, and immediate Kill control.
- Progressive overlay, notification, microphone, and MediaProjection consent.
- Tap-to-talk through Android speech recognition.
- While screen sharing is on, Android may continuously provide frames internally. AIopter processes and sends one reduced-quality screen image only for an explicit screen-aware prompt; frames are not permanently stored and temporary image bytes are cleared after use.
- Per-app screen rules with **Allowed**, **Ask Every Time**, and **Never Allow** choices. New apps ask by default; unknown foreground apps, expired approvals, app switches, and protected or blank frames are blocked before encoding or transmission.
- Text and image chat through a Hono service and Nxcode AI Gateway, with Server-Sent Events to the Android client.
- Nxcode SDK sign-in in an origin-restricted WebView; session tokens are encrypted using Android Keystore.
- Deterministic, user-approved maps intents. The model never executes arbitrary Android actions.
- Request schemas, payload limits, normalized errors, cancellation, security headers, and no prompt/image logging. The current process-local rate limiter is for development only; shared durable rate limiting remains a release blocker.

## Local development

### Backend

Use Node 20 or newer. Install dependencies in the backend directory, then start its development script. The service listens on all interfaces at port 3001; the Android debug build reaches it from the standard emulator bridge.

The Nxcode workspace supplies its development identity. Every AI request requires a signed-in user by default; anonymous access must be enabled explicitly only for isolated local contract tests. No provider API key belongs in this repository or the APK.

### Android

Open the repository root in Android Studio, select the Android app configuration, and run on Android 10 or newer. The project requires JDK 17 and Android SDK 36. For a physical device, provide the HTTPS backend URL as the debug API Gradle property rather than using a browser-facing localhost address.

The release build is locked to `https://api.aiopter.ai` and rejects cleartext traffic. That hostname is the intended production API; it must not be described as live until its HTTPS health endpoint and an authenticated AI request pass against the deployed service.

## Safety model

Screen sharing and microphone access are off by default. Android owns each permission prompt, screen sharing has a persistent notification and stop action, and Kill cancels listening, capture, active network work, temporary buffers, and the overlay itself. App awareness uses Android’s special usage access only to identify the foreground package at send time; the app list is limited to launchable apps and no broad package permission is requested. Navigation is proposal-only until the user taps Review and confirms the external app launch.

## Current acceptance boundary

Backend behavior and device-independent Android policy/configuration logic are covered by automated tests. CI enforces policy tests, Android unit tests and lint, debug/release APK builds, and a required debug APK artifact. Live sign-in, AI calls, overlay, MediaProjection, speech recognition, and STOP/KILL behavior must still pass the real-device checklist before release.
