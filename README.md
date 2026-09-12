# Alopter

Native Android companion and AI orchestration service for **SEE. ASK. DO. ANYWHERE**.

## What is implemented

- Kotlin/Jetpack Compose Android home app targeting API 36 with a real `TYPE_APPLICATION_OVERLAY` floating rotor.
- Draggable edge snapping, persistent placement, animated brand treatment, compact dark assistant panel, and immediate Kill control.
- Progressive overlay, notification, microphone, and MediaProjection consent.
- Tap-to-talk through Android speech recognition.
- Request-driven screen understanding: one downscaled JPEG per explicit screen-aware prompt, no file persistence, and prompt buffers cleared after use.
- Text and image chat through a Hono service and Nxcode AI Gateway, with Server-Sent Events to the Android client.
- Nxcode SDK sign-in in an origin-restricted WebView; session tokens are encrypted using Android Keystore.
- Deterministic, user-approved maps intents. The model never executes arbitrary Android actions.
- Request schemas, payload limits, rate limiting, normalized errors, cancellation, security headers, and no prompt/image logging.

## Local development

### Backend

Use Node 20 or newer. Install dependencies in the backend directory, then start its development script. The service listens on all interfaces at port 3001; the Android debug build reaches it from the standard emulator bridge.

The Nxcode workspace supplies its development identity. Every AI request requires a signed-in user by default; anonymous access must be enabled explicitly only for isolated local contract tests. No provider API key belongs in this repository or the APK.

### Android

Open the repository root in Android Studio, select the Android app configuration, and run on Android 10 or newer. The project requires JDK 17 and Android SDK 36. For a physical device, provide the HTTPS backend URL as the debug API Gradle property rather than using a browser-facing localhost address.

The release build defaults to the production API host and rejects cleartext traffic. Replace that host at build/release time only after the backend is deployed and its health and AI request paths have been tested from the app.

## Safety model

Screen sharing and microphone access are off by default. Android owns each permission prompt, screen sharing has a persistent notification and stop action, and Kill cancels listening, capture, AI work, temporary buffers, and the overlay itself. Navigation is proposal-only until the user taps Review and confirms the external app launch.

## Current acceptance boundary

Backend behavior is covered by automated contract tests. A full native APK build is enforced in x86 Linux CI; this workspace is ARM64 while Google's Linux AAPT2 binary is x86-64-only. Live Nxcode login and AI calls require a platform-authenticated preview or deployed app and must be exercised there before release.
