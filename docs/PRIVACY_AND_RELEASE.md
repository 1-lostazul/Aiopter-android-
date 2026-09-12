# Privacy and release gate

## Data flow

1. Text is sent only when the user taps Send.
2. Microphone audio is handled by Android speech recognition after a just-in-time permission request; Alopter does not write raw audio.
3. Screen sharing starts only after Android MediaProjection consent. The service keeps a persistent notification.
4. While sharing is active, a reduced frame is produced only for a user-submitted question. The overlay is hidden during capture, no screenshot is written to disk, and the encoded byte array is cleared after the request.
5. The backend validates size and shape, sends the minimum prompt/image context to Nxcode AI Gateway, and does not log content.

## Before a store release

- Replace and verify the production API hostname, and keep mandatory authentication enabled.
- Run login, text, vision, cancellation, and maps-intent acceptance tests on real Pixel, Samsung, and Motorola devices across supported Android versions.
- Confirm current Google Play foreground-service, overlay, MediaProjection, Data safety, privacy policy, account deletion, and SDK disclosure requirements.
- Add per-app sensitive-screen rules before allowing screen sharing in financial, identity, medical, password-manager, or other high-risk apps.
- Configure production monitoring that records timings and opaque request IDs only—not prompts, screenshots, audio, tokens, or response bodies.
- Complete signing, dependency review, staged rollout, incident response, and rollback procedures.

The present build deliberately does not use AccessibilityService, background microphone capture, continuous screenshots, arbitrary tool execution, or embedded AI credentials.
