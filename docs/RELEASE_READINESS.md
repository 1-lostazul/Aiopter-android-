# AIopter release readiness

This document is a release gate, not a claim that AIopter is production-ready. A gate is complete only when linked evidence exists for the exact release candidate. Automated checks do not replace physical-device or Google Play review.

Status meanings:

- **Implemented:** present in source; still subject to CI and release-candidate validation.
- **Pending evidence:** work may exist, but required acceptance evidence is not yet recorded.
- **RELEASE BLOCKER:** public release must not proceed.

## P0 — must pass before any public release

| Gate | Status | Required evidence / remaining work |
|---|---|---|
| Official AIopter branding | Implemented | CI scan and release-candidate visual review must show no user-facing legacy branding. |
| Final applicationId | Implemented | Release manifest/package must resolve to `io.aiopter.companion`. A store identity migration must be reviewed if an older package was ever published. |
| Successful CI | Pending evidence | The pull request workflow must be green for backend and Android jobs. |
| Android lint | Pending evidence | `lintDebug` must pass in CI for the release commit. |
| Backend authentication | Implemented | Production identity forces authentication; complete a real deployed login/request acceptance test. |
| Real HTTPS production API | **RELEASE BLOCKER** | The current probe failed with `Could not resolve host: api.aiopter.ai`. Configure DNS/TLS/deployment, then pass `https://api.aiopter.ai/health` and a production-like authenticated AI request. |
| Real-device overlay test | **RELEASE BLOCKER** | Complete and retain evidence from the device checklist. |
| Real-device MediaProjection test | **RELEASE BLOCKER** | Complete consent, protected-frame, app-switch, stop, and no-persistence checks on physical devices. |
| Microphone test | **RELEASE BLOCKER** | Complete allow, deny, tap-to-talk, and kill tests on physical devices. |
| Apps & Access enforcement | **RELEASE BLOCKER** | Verify Allowed, Ask Every Time, Never Allow, unknown foreground identity, approval expiry, and app-switch behavior on physical devices. |
| STOP/KILL verification | **RELEASE BLOCKER** | Verify idle, speech, projection, and in-flight network cancellation on physical devices. |
| No committed secrets | Pending evidence | Secret scan and repository-history review must pass for the release commit. Deployment identity must be injected securely. |
| Privacy policy | **RELEASE BLOCKER** | Publish a reviewed privacy policy at a stable public URL and link it in the app/store listing. The engineering data-flow note is not a substitute. |
| Google Play Data Safety answers | **RELEASE BLOCKER** | Complete and review answers against actual authentication, speech recognition, screenshot, AI-provider, diagnostics, deletion, retention, and sharing behavior. |
| Foreground Service declaration | Implemented | Manifest declares `specialUse` and `mediaProjection`; confirm Play Console declaration matches the release behavior. |
| Special-use FGS explanation | Pending evidence | Submit and obtain acceptance for the user-enabled floating assistant explanation and required demonstration video. |
| MediaProjection disclosure | Implemented | In-app wording accurately describes continuous frame availability and one-image transmission; align store disclosure and reviewer notes. |
| Play App Signing/upload key | **RELEASE BLOCKER** | Enroll in Play App Signing and create/protect an upload key outside the repository. |
| Signed Android App Bundle | **RELEASE BLOCKER** | Build, verify, and archive a signed release AAB from the approved commit. |
| Internal Testing release | **RELEASE BLOCKER** | Publish the signed candidate to Play Internal Testing and complete the device checklist against that build. |

## P1 — must pass before broad rollout

| Gate | Status | Required evidence / remaining work |
|---|---|---|
| Durable distributed rate limiting | **RELEASE BLOCKER** | Replace the current process-local fixed-window limiter with atomic shared/durable state suitable for all production instances, then test concurrent enforcement and failure behavior. |
| Crash/ANR monitoring | **RELEASE BLOCKER** | Select and configure privacy-reviewed monitoring without prompt, image, audio, token, or response logging; validate alerts and symbol/mapping upload. |
| Multiple Android-version tests | **RELEASE BLOCKER** | Pass the full device checklist on Android 10 and at least two current Android versions across multiple vendors. |
| Accessibility review | **RELEASE BLOCKER** | Verify TalkBack labels/order, touch targets, contrast, font scaling, keyboard behavior, and overlay accessibility. |
| Battery/resource review | **RELEASE BLOCKER** | Measure foreground-service duration, projection/reader lifecycle, CPU, memory, network usage, and battery drain under realistic sessions. |
| Network retry/error review | **RELEASE BLOCKER** | Validate timeout, offline, cancellation, 401, 413, 429, 5xx, malformed stream, and recovery behavior without unsafe automatic resubmission. |
| Staged rollout plan | **RELEASE BLOCKER** | Define rollout cohorts, monitoring thresholds, pause/rollback owners, incident contacts, and rollback procedure. |

## Current security posture

- Backups are disabled and release cleartext traffic is disabled.
- Debug cleartext is confined to emulator/loopback hosts by a debug-only network policy and runtime endpoint validation.
- Activities and services are not exported except the launcher activity required by Android.
- Session tokens are encrypted with an Android Keystore AES-GCM key and are not embedded in the APK.
- Screen images are handled in memory, never written to permanent storage, and encoded JPEG bytes are overwritten after request completion or cancellation.
- Foreground-app identity failure blocks transmission; Never Allow blocks; Ask Every Time requires a fresh package-bound approval.
- STOP/KILL tears down speech recognition, active HTTP work, MediaProjection, image resources, and the overlay service.
- Provider credentials remain server-side. Production requires a securely deployed provider identity and authentication.

## Release decision

**Not ready for public release.** At minimum, the production API, distributed rate limiting, physical-device acceptance, privacy/Play declarations, signing, signed bundle, and Internal Testing gates remain blocked.
