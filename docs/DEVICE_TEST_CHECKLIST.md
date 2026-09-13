# AIopter real-device test checklist

Run this checklist on a production-representative physical phone before any public release. Record device model, Android version, build identifier, backend environment, tester, date, and evidence for every result. Do not mark a result passed from an emulator alone.

## Test record

- Device/model:
- Android version and security patch:
- AIopter version/build:
- Backend environment and host:
- Tester/date:
- Network types tested:

## Installation and first launch

- [ ] **First launch:** Install with no prior AIopter data. Confirm the AIopter name/icon appear, screen sharing is off, microphone is idle, overlay is off, and no capture/listening starts automatically.
- [ ] **Overlay permission:** Start the floating assistant, deny overlay permission, and confirm no bubble appears. Grant it from Android Settings and confirm the flow resumes only after returning to AIopter.
- [ ] **Notification permission:** On Android 13+, deny notification permission and confirm the app remains understandable and does not silently bypass Android’s permission model. Grant it and confirm foreground-service notifications are visible.
- [ ] **Microphone allow/deny:** Tap Mic before permission is granted. Test both denial and approval; denial must not start listening, while approval may start only the user-requested tap-to-talk session.
- [ ] **Android process restart:** Force-stop AIopter, relaunch it, and confirm no screen-sharing grant, microphone session, active request, or overlay is silently restored.

## Floating native assistant

- [ ] **Floating propeller:** Confirm the real `TYPE_APPLICATION_OVERLAY` propeller appears above another app after explicit permission and has a persistent foreground notification.
- [ ] **Dragging and snap behavior:** Drag to each side and near top/bottom limits. Confirm movement stays on screen, snaps to the nearest edge, and its vertical position is retained after collapsing/restarting the overlay.
- [ ] **Open/collapse panel:** Tap the propeller to open the native panel and use the collapse control to return to the bubble without losing control of the foreground app.
- [ ] **Thinking animation:** Send a question and confirm the propeller indicates active AI work, then returns to a non-busy state when the request completes or fails.
- [ ] **Orientation change:** Rotate between portrait and landscape with bubble and panel open. Confirm controls remain reachable and the overlay does not become stranded off-screen.
- [ ] **Background/foreground:** Move AIopter and the foreground app between background and foreground. Confirm the overlay state remains explicit and Android keeps required service notifications visible.

## Text, speech, and network

- [ ] **Typed AI question:** Submit a typed question without screen sharing. Confirm exactly one request is sent, cancellation remains available, and no screenshot is attached.
- [ ] **Online AI response:** While authenticated against the production-representative backend, confirm a real streamed response appears and no demo/fallback text is substituted.
- [ ] **Tap-to-talk:** Tap Mic, speak once, and confirm Android speech recognition fills the text field. Confirm AIopter does not submit or execute the text without the user’s send action.
- [ ] **Network loss:** Disconnect networking before and during a request. Confirm a useful error appears, the request ends, and no endless busy/listening state remains.
- [ ] **Backend failure:** Point a debug build at a controlled failing backend. Confirm authentication/configuration/server failures are surfaced without token, prompt, screenshot, or credential details.

## Screen sharing and app access

- [ ] **Screen-sharing consent:** Enable sharing and confirm AIopter shows its disclosure before Android’s MediaProjection prompt. Cancel once and confirm sharing remains off; approve once and confirm the persistent notification and stop action appear.
- [ ] **Screen-aware question:** With sharing on, submit one screen-aware question. Confirm Android may supply frames internally but AIopter processes/sends only one reduced-quality image for that explicit question, does not save it, and hides its overlay from the image.
- [ ] **Allowed rule:** Mark a non-sensitive test app Allowed, switch to it, and confirm an explicitly submitted screen-aware question proceeds without a second AIopter approval prompt.
- [ ] **Ask Every Time rule:** Mark the test app Ask Every Time. Confirm every screen-aware question requires a new one-time approval and that a previous approval cannot be reused.
- [ ] **Ask Every Time app switch:** Open the approval prompt, switch foreground apps before approving, then approve. Confirm transmission is blocked because the package changed.
- [ ] **Never Allow rule:** Mark the test app Never Allow and confirm screen content is never transmitted, even with screen sharing active.
- [ ] **Unknown foreground identity:** Revoke Usage Access or create a state where the foreground package cannot be resolved. Confirm AIopter blocks screen transmission and directs the user to Apps & Access.
- [ ] **Protected/black screen:** Test a `FLAG_SECURE` or otherwise protected screen and a controlled black screen. Confirm AIopter reports it as protected/blank and sends no image.
- [ ] **Banking/password/private app protection:** Test representative banking, password-manager, authentication-code, identity, health, and private-content apps with Never Allow. Confirm each is blocked; do not use real credentials or account data in test evidence.
- [ ] **Stop sharing notification action:** Use Android’s screen-sharing notification stop action and confirm projection ends immediately and the UI returns to sharing off.

## External actions

- [ ] **Maps approval and launch:** Ask for navigation. Confirm AIopter shows the proposed destination first, launches an external maps handler only after explicit approval, and does nothing when Cancel is selected.
- [ ] **No arbitrary execution:** Ask the model to open an unsupported app or perform another device action. Confirm no activity, intent, or setting launches without a deterministic supported action and approval.

## STOP/KILL

- [ ] **STOP/KILL while idle:** Use STOP from the home screen and KILL from the panel. Confirm overlay and both foreground-service notifications disappear.
- [ ] **STOP/KILL during microphone:** Kill while listening/transcribing. Confirm speech recognition ends immediately, no transcript is submitted, and the microphone indicator clears.
- [ ] **STOP/KILL during screen sharing:** Kill while MediaProjection is active. Confirm Android’s sharing indicator/notification ends immediately and no later frame is sent.
- [ ] **STOP/KILL during AI network request:** Kill during a slow streamed response. Confirm the HTTP request is cancelled, response updates stop, temporary image bytes are discarded when applicable, and the overlay closes.

## Exit criteria

All items must pass on the minimum supported Android version and at least two currently supported Android versions from different device vendors. Attach redacted screenshots/video and backend request IDs only; never attach prompts, captured screens, tokens, credentials, or personal data.
