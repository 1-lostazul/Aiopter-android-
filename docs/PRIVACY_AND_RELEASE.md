# Privacy and release gate

## Data flow

1. Text is sent only when the user taps Send.
2. Microphone audio is handled by Android speech recognition after a just-in-time permission request; AIopter does not write raw audio.
3. Screen sharing starts only after Android MediaProjection consent. The service keeps a persistent notification.
4. While screen sharing is on, Android may continuously provide frames internally. AIopter processes and sends one reduced-quality screen image only when the user asks for screen-aware help. The overlay is hidden while that image is selected, frames are not permanently stored, no screenshot is written to disk, and the encoded byte array is overwritten after request completion or cancellation.
5. The backend validates size and shape, sends the minimum prompt/image context to Nxcode AI Gateway, and does not log content.

## Before a store release

- Deploy and verify `https://api.aiopter.ai` with an actual HTTPS health check and authenticated AI request; keep mandatory authentication enabled.
- Run login, text, vision, cancellation, and maps-intent acceptance tests on real Pixel, Samsung, and Motorola devices across supported Android versions.
- Confirm current Google Play foreground-service, overlay, MediaProjection, Data safety, privacy policy, account deletion, and SDK disclosure requirements.
- Keep financial, identity, medical, password-manager, authentication-code, and other high-risk apps set to Never Allow, and verify those rules on real devices.
- Configure production monitoring that records timings and opaque request IDs only—not prompts, screenshots, audio, tokens, or response bodies.
- Complete signing, dependency review, staged rollout, incident response, and rollback procedures.

The present build deliberately does not use AccessibilityService, background microphone capture, continuous screenshots, arbitrary tool execution, or embedded AI credentials.
