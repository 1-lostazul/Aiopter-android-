from pathlib import Path
from datetime import datetime, timezone
from zipfile import ZipFile, ZIP_DEFLATED
import subprocess

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor

ROOT = Path('/workspace')
OUT = ROOT / 'exports'
OUT.mkdir(parents=True, exist_ok=True)

try:
    revision = subprocess.check_output(['git', 'rev-parse', '--short', 'HEAD'], cwd=ROOT, text=True).strip()
except Exception:
    revision = 'unavailable'

generated = datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')

transcript = r'''ALOPTER PROJECT — USER-FACING CHAT TRANSCRIPT
Generated: {generated}
Project revision at export: {revision}

SCOPE NOTE
This is a copy/paste transcript of the user-facing dialogue retained in the current conversation context. It excludes hidden system instructions, internal execution logs, tool payloads, and secrets. Repeated “accept delivery” interface actions are represented as user actions.

USER: Continue

NXCODE: Alopter now supports per-app screen rules, defaults new apps to “Ask Every Time,” and blocks unknown, switched, protected, or stale-approved screens before transmission. The Kill control now cancels active network work, while privacy-safe request IDs improve diagnostics without logging content. Next, test the flow on an Android device and enable App Awareness when prompted; browser preview is unavailable because this workspace has no external port mapping.

USER: So what needs to be done next

NXCODE: Next steps:

1. Run the app on a real Android device and test overlay permission, App Awareness, screen sharing, all three per-app rules, app switching, protected screens, and Kill.
2. Run a full APK build in the x86 Linux CI environment or Android Studio.
3. Deploy the backend, then complete a production-like sign-in and AI screenshot request; live Nxcode authentication and AI calls remain unverified until that succeeds.

USER: So how do we do that

NXCODE:
- Local Android test: Open the project in Android Studio, create an API 29+ emulator, run the debug app, enable Overlay and App Awareness, then test Allowed, Ask Every Time, Never Allow, app switching, protected screens, and Kill.
- Live sign-in and AI: tell me “Deploy the backend”; I’ll deploy it, connect the Android app to its HTTPS address, and run the real authentication and screenshot-analysis acceptance test.
- Installable APK: connect this repository to GitHub and push it so the existing CI workflow builds the APK, or build it from Android Studio on an x86 computer.

USER: What do u think is the next best step

NXCODE: The best next step is to deploy the backend. That gives the Android app a real HTTPS endpoint and lets us validate sign-in, AI responses, and screenshot analysis on a physical device—say “Deploy the backend” and I’ll handle it.

USER: Deploy the backend and handle it

NXCODE: The backend is ready, but deployment is blocked because the Cloudflare account has reached its 500-Worker limit. Remove an unused Worker or increase the account quota, then tell me “retry deployment” and I’ll deploy it, connect the Android app, and run the live acceptance checks.

USER: How do I remove worker

NXCODE: If you control the Cloudflare account:

1. Open Cloudflare Dashboard → Workers & Pages and select an unused Worker.
2. Open Settings → Delete Worker, enter its name, and confirm—do not delete an active or unfamiliar Worker.
3. If this is Nxcode’s managed Cloudflare account and you cannot access that page, delete an old deployment from your Nxcode workspace or contact Nxcode support to free a Worker slot.

The failed Alopter deployment was not created, so deleting it will not help; once a slot is available, tell me “retry deployment.”

USER: How will I know when a slot is opened

NXCODE: You’ll know a slot is available when either:

- Cloudflare shows fewer than 500 Workers, or
- Nxcode confirms an old deployment was removed.

After deleting one, the slot should become available almost immediately. Tell me “retry deployment”, and I’ll perform one deployment attempt to confirm it.

USER: How do I access cloudflar

NXCODE: Go to https://dash.cloudflare.com/ and sign in with the Cloudflare account connected to your Nxcode workspace, then open Workers & Pages.

However, this deployment appears to use an Nxcode-managed Cloudflare account, so you may not have direct access. If the account is not visible, contact Nxcode support and ask them to free a Worker slot for your workspace; afterward, tell me “retry deployment.”

USER: This my first time using nxcode do I need to sign up to cloudflar

NXCODE: No—you should not need a separate Cloudflare account when deploying through Nxcode. The limit belongs to Nxcode’s managed deployment account, so contact Nxcode support and report that your first deployment failed because the managed account reached its Worker limit; once they free capacity, tell me “retry deployment.”

USER: I accessed cloudflar through github from in nxcode

NXCODE: If Cloudflare opened successfully through your GitHub login, you already have a Cloudflare account—no additional signup is needed. Open Workers & Pages: if it is empty or shows only a few Workers, the 500-Worker limit is on Nxcode’s managed deployment account rather than yours, so Nxcode support must resolve it. Do not delete anything unless you recognize it as your own unused deployment.

USER: Not touching anything and I sent nxcode an email with what told me to tell them

NXCODE: That was the right choice—don’t delete anything you don’t recognize. Once Nxcode confirms capacity is available, tell me “retry deployment”; meanwhile, we can continue testing the app locally in the Android emulator if you want.

USER: Ok

NXCODE: Great. Send me Nxcode’s reply when it arrives, and I’ll take the next step from there.

USER: No reply can u check on it

NXCODE: I can’t access Nxcode’s support inbox or view your email status. Once they reply, paste their message here; until then, I shouldn’t retry deployment because the confirmed account-capacity issue remains unresolved.

USER: Can u try again to deploy

NXCODE: The retry is still blocked by the same Nxcode-managed Cloudflare account limit, and no Alopter deployment was created. We need Nxcode support to free capacity or raise the limit before another attempt will work.

USER ACTION: accept_delivery

NXCODE: The project changes are safely checkpointed. Deployment remains pending until Nxcode resolves the Cloudflare Worker capacity limit.

USER: Try again

NXCODE: Deployment is still blocked by the same 500-Worker limit on Nxcode’s managed Cloudflare account. No app changes are needed; Nxcode support must free capacity before deployment can proceed.

USER: What should I do to get this problem remedied

NXCODE: Reply to your Nxcode support email with this exact message:

“My first backend deployment is blocked by the 500-Worker limit on Nxcode’s managed Cloudflare account. The deployment target is developer-code-sandbox-alopter-api, and Cloudflare returns error 10037. Please free a Worker slot, increase the account limit, or move my workspace to an account with available capacity. Please confirm when I can retry.”

Also include a screenshot of the deployment error if possible. You do not need to modify your personal Cloudflare account; after Nxcode confirms the fix, return here and say “retry deployment.”

USER: Can u break it down to smaller steps

NXCODE:
1. Open your email app.
2. Find the email you sent to Nxcode support.
3. Tap Reply.
4. Paste this message:

“My first backend deployment is blocked by the 500-Worker limit on Nxcode’s managed Cloudflare account. Cloudflare error: 10037. Please free a Worker slot and tell me when I can retry.”

5. Send the reply.
6. Wait for Nxcode support to confirm it is fixed.
7. Return here and say “retry deployment.”

Do not delete anything in your personal Cloudflare account.

USER ACTION: accept_delivery

NXCODE: Your project is saved and ready. Once Nxcode support confirms the Worker capacity issue is fixed, return and say “retry deployment.”

USER ACTION: accept_delivery

NXCODE: Everything is safely saved. The only remaining blocker is Nxcode’s Cloudflare capacity; once support confirms it is resolved, tell me “retry deployment.”

USER ACTION: accept_delivery

NXCODE: No further action is needed here until support replies. When they confirm capacity is available, send “retry deployment.”

USER ACTION: accept_delivery

NXCODE: The project remains saved and ready. Return with Nxcode support’s reply, or say “retry deployment” after they confirm the capacity issue is fixed.

USER ACTION: accept_delivery

NXCODE: Everything is saved. We’re waiting only for Nxcode support to free deployment capacity.

USER: How do i export to github.com

NXCODE:
1. Go to https://github.com/new and sign in.
2. Enter a repository name, such as alopter.
3. Choose Private or Public.
4. Leave README, .gitignore, and license unchecked.
5. Click Create repository.
6. Copy the repository’s HTTPS address.
7. Paste that address here and tell me “push to this repository”—I’ll connect and upload the project. Do not send your GitHub password or access token in chat.

USER ACTION: accept_delivery

NXCODE: Create the empty GitHub repository, then paste its HTTPS address here. I’ll connect and push the saved project for you.

USER: Create me a copy and paste for this entire chat plus a docx file of the exact code specs with a download link
'''.format(generated=generated, revision=revision)

transcript_path = OUT / 'alopter-chat-transcript.txt'
transcript_path.write_text(transcript, encoding='utf-8')

# ---------- Word specification ----------
doc = Document()
section = doc.sections[0]
section.top_margin = Inches(0.65)
section.bottom_margin = Inches(0.65)
section.left_margin = Inches(0.72)
section.right_margin = Inches(0.72)

styles = doc.styles
styles['Normal'].font.name = 'Aptos'
styles['Normal'].font.size = Pt(10)
styles['Title'].font.name = 'Aptos Display'
styles['Title'].font.size = Pt(28)
styles['Title'].font.color.rgb = RGBColor(10, 45, 78)
for name, size, color in [('Heading 1', 19, RGBColor(10, 69, 110)), ('Heading 2', 14, RGBColor(21, 96, 128)), ('Heading 3', 11, RGBColor(40, 80, 100))]:
    styles[name].font.name = 'Aptos Display'
    styles[name].font.size = Pt(size)
    styles[name].font.color.rgb = color

# Header/footer
header = section.header.paragraphs[0]
header.text = 'ALOPTER  |  IMPLEMENTATION SPECIFICATION'
header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
for r in header.runs:
    r.font.size = Pt(8)
    r.font.color.rgb = RGBColor(95, 110, 120)
footer = section.footer.paragraphs[0]
footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
footer.add_run('Alopter specification  •  ')
field = OxmlElement('w:fldSimple')
field.set(qn('w:instr'), 'PAGE')
footer._p.append(field)

def shade(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = tcPr.find(qn('w:shd'))
    if shd is None:
        shd = OxmlElement('w:shd')
        tcPr.append(shd)
    shd.set(qn('w:fill'), fill)

def set_cell_text(cell, text, bold=False, color=None, size=9):
    cell.text = ''
    p = cell.paragraphs[0]
    r = p.add_run(str(text))
    r.bold = bold
    r.font.size = Pt(size)
    if color:
        r.font.color.rgb = color
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER

def table(headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.style = 'Table Grid'
    for i, h in enumerate(headers):
        set_cell_text(t.rows[0].cells[i], h, True, RGBColor(255,255,255), 9)
        shade(t.rows[0].cells[i], '0A456E')
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            set_cell_text(cells[i], val)
            if len(t.rows) % 2 == 1:
                shade(cells[i], 'EDF4F7')
    if widths:
        for row in t.rows:
            for i, width in enumerate(widths):
                row.cells[i].width = Inches(width)
    doc.add_paragraph()
    return t

def bullet(text, level=0):
    style = 'List Bullet' if level == 0 else 'List Bullet 2'
    p = doc.add_paragraph(style=style)
    p.add_run(text)
    return p

def number(text, level=0):
    style = 'List Number' if level == 0 else 'List Number 2'
    p = doc.add_paragraph(style=style)
    p.add_run(text)
    return p

def code(text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Inches(0.25)
    p.paragraph_format.right_indent = Inches(0.15)
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(6)
    pPr = p._p.get_or_add_pPr()
    shd = OxmlElement('w:shd'); shd.set(qn('w:fill'), 'F1F4F6'); pPr.append(shd)
    r = p.add_run(text)
    r.font.name = 'Consolas'; r.font.size = Pt(8.5)
    return p

def status_box(title, text, fill='FFF4CE'):
    t = doc.add_table(rows=1, cols=1)
    t.style = 'Table Grid'
    shade(t.cell(0,0), fill)
    p = t.cell(0,0).paragraphs[0]
    r = p.add_run(title + ': '); r.bold = True
    p.add_run(text)
    doc.add_paragraph()

# Cover
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.add_run('\n\n')
r = p.add_run('ALOPTER')
r.bold = True; r.font.size = Pt(38); r.font.color.rgb = RGBColor(0, 116, 166)
r = p.add_run('\nExact Code & Product Specification')
r.font.size = Pt(22); r.font.color.rgb = RGBColor(17, 55, 80)
r = p.add_run('\nNative Android Companion + AI Orchestration Backend')
r.font.size = Pt(13); r.font.color.rgb = RGBColor(82, 100, 114)

doc.add_paragraph('\n')
table(['Document field', 'Value'], [
    ('Specification status', 'As implemented in the current repository'),
    ('Project revision', revision),
    ('Generated', generated),
    ('Application ID', 'io.alopter.companion (release); io.alopter.companion.debug (debug)'),
    ('Deployment status', 'Not deployed; blocked by managed Cloudflare Worker capacity'),
    ('Live integration status', 'Nxcode login and AI screenshot flow remain unverified in production'),
], [2.0, 4.8])
status_box('Important', 'This document records the implemented source contract. The repository remains the ultimate source of truth. No secret values, session tokens, prompts, screenshots, or internal credentials are included.', 'DDEBF7')
doc.add_page_break()

# Executive summary
doc.add_heading('1. Executive Summary', level=1)
doc.add_paragraph('Alopter is a native Android companion designed around the phrase “SEE. ASK. DO. ANYWHERE.” It provides a user-controlled floating assistant, explicit microphone and MediaProjection consent, request-driven single-frame screen understanding, per-app screen access policies, Nxcode-backed AI responses, and proposal-only external actions.')
bullet('Screen and microphone capabilities are off by default and require explicit user action.')
bullet('One reduced-quality frame is captured only for an explicit screen-aware request; frames are never intentionally persisted.')
bullet('Every frame is gated by foreground-app identification and a locally stored per-app rule before image encoding or network transmission.')
bullet('New apps default to Ask Every Time. One-time approvals expire after 30 seconds and are invalidated by foreground-app changes.')
bullet('Protected or nearly black frames are rejected locally.')
bullet('The Kill action cancels speech recognition, capture, active network work, and the overlay.')
bullet('Model-proposed navigation remains non-executing until a separate user confirmation opens an Android-safe URI.')

status_box('Current release boundary', 'Source compilation, policy tests, backend type checks, backend build, ten backend contract tests, and dependency audit passed in the workspace. A complete APK must still be produced by x86 Linux CI or Android Studio, and the deployed login/AI flow must still pass on a real device.', 'FFF4CE')

# Architecture
doc.add_heading('2. System Architecture', level=1)
table(['Layer', 'Technology', 'Responsibility'], [
    ('Android application', 'Kotlin 2.1.20, Jetpack Compose, API 29–36', 'Consent, overlay UI, speech input, screen capture, local app rules, secure token storage, SSE client'),
    ('Shared policy module', 'Kotlin/JVM 17', 'Pure access decisions, one-time approval validity, protected-frame classification, trusted-origin matching'),
    ('Backend API', 'TypeScript 5.8, Hono, Node development / Cloudflare Worker production', 'Authentication boundary, schema validation, rate limiting, AI gateway orchestration, SSE output, safe action proposal'),
    ('AI provider', 'Nxcode AI Gateway, model alias fast', 'Text and multimodal response generation under a fixed safety instruction'),
    ('CI', 'GitHub Actions, Ubuntu, Node 20, JDK 17, Android SDK 36', 'Backend checks and complete debug APK/test build on x86 Linux'),
], [1.4, 2.0, 3.4])

doc.add_heading('2.1 Runtime flow', level=2)
for item in [
    'The user starts the floating assistant and grants Android overlay/notification consent where required.',
    'The user separately enables screen sharing through an explanatory disclosure followed by Android MediaProjection consent.',
    'For a screen-aware prompt, the overlay hides briefly and requests one frame from the capture service.',
    'The capture service identifies the foreground package through Usage Access. If unavailable, transmission is blocked.',
    'The service loads the local app rule: Allowed, Ask Every Time, or Never Allow.',
    'Ask Every Time closes the acquired image and asks for one-time approval. Approval is accepted only if fresh and the foreground package still matches.',
    'An allowed frame is copied to an in-memory bitmap, cropped, sampled for protected/blank content, JPEG-compressed at quality 74, and sent to the backend.',
    'The backend validates the request, invokes the Nxcode AI Gateway, and returns SSE events to Android.',
    'Temporary JPEG bytes are overwritten after the request completes; capture images and bitmaps are closed or recycled.',
]: number(item)

# Android specs
doc.add_heading('3. Android Application Specification', level=1)
table(['Setting', 'Exact value'], [
    ('Namespace / release application ID', 'io.alopter.companion'),
    ('Debug application ID', 'io.alopter.companion.debug'),
    ('Minimum Android version', 'API 29 (Android 10)'),
    ('Target / compile SDK', 'API 36'),
    ('Release version', '1.0.0, versionCode 1'),
    ('Java/Kotlin target', 'JVM 17'),
    ('Production API base URL', 'https://api.alopter.app (configured but not live-verified)'),
    ('Emulator debug API base URL', 'http://10.0.2.2:3001'),
    ('Physical-device debug API', 'Must be supplied as an HTTPS Gradle property'),
], [2.5, 4.2])

# dependencies
doc.add_heading('3.1 Android dependency versions', level=2)
table(['Component', 'Version'], [
    ('Android Gradle Plugin', '8.10.1'), ('Kotlin', '2.1.20'), ('AndroidX Core KTX', '1.16.0'),
    ('Activity Compose', '1.10.1'), ('Compose BOM', '2025.05.01'), ('Lifecycle', '2.9.0'),
    ('DataStore Preferences', '1.1.7'), ('OkHttp', '4.12.0'), ('Kotlin Coroutines', '1.10.2'), ('JUnit', '4.13.2'),
], [3.2, 2.0])

# permissions
doc.add_heading('3.2 Permissions and package visibility', level=2)
table(['Android capability', 'Purpose / constraint'], [
    ('INTERNET', 'Backend API and Nxcode login/AI communication.'),
    ('SYSTEM_ALERT_WINDOW', 'User-enabled TYPE_APPLICATION_OVERLAY assistant bubble and panel.'),
    ('POST_NOTIFICATIONS', 'Visible foreground-service notifications on Android 13+.'),
    ('RECORD_AUDIO', 'Tap-to-talk speech recognition only.'),
    ('PACKAGE_USAGE_STATS', 'Special Usage Access used to identify the foreground package at send time; frames are blocked without it.'),
    ('FOREGROUND_SERVICE_SPECIAL_USE', 'Floating assistant foreground service.'),
    ('FOREGROUND_SERVICE_MEDIA_PROJECTION', 'MediaProjection foreground capture service.'),
    ('Launcher query only', 'Package visibility is restricted to ACTION_MAIN + CATEGORY_LAUNCHER; QUERY_ALL_PACKAGES is not requested.'),
], [2.2, 4.5])

# UI
doc.add_heading('3.3 Home screen and overlay', level=2)
bullet('Home screen displays floating overlay, screen sharing, microphone, and app-awareness status.')
bullet('Primary controls: start/stop floating assistant, enable screen sharing, Manage Apps & Access, sign in/sign out.')
bullet('Overlay bubble is draggable, snaps to a screen edge, stores vertical placement, and opens a dark compact panel.')
bullet('Panel controls: Mic, See ON/OFF, Send, minimize, and KILL.')
bullet('Overlay exposes AI answer text and a separate Review action button for model-proposed navigation.')
bullet('Speech recognition requests RECORD_AUDIO only after disclosure and is tap initiated.')

doc.add_heading('3.4 Session state model', level=2)
table(['State group', 'Exact enum values'], [
    ('OverlayState', 'DISABLED, PERMISSION_MISSING, READY, BUBBLE, PANEL'),
    ('ScreenState', 'OFF, REQUESTING_PERMISSION, STARTING, SHARING, BLOCKED_FOR_APP, STOPPING, ERROR'),
    ('MicState', 'IDLE, REQUESTING_PERMISSION, LISTENING, TRANSCRIBING, ERROR'),
    ('AiState', 'IDLE, SENDING, STREAMING, COMPLETE, CANCELLED, ERROR'),
], [1.7, 5.0])
doc.add_paragraph('ScreenState is considered session-active for both SHARING and BLOCKED_FOR_APP. This distinction lets the user stop a live MediaProjection session even while policy blocks the current app.')

# Policy
doc.add_heading('4. Per-App Access Policy', level=1)
table(['Rule', 'Decision', 'Behavior'], [
    ('Allowed', 'ALLOW', 'A fresh frame may proceed when foreground context is known.'),
    ('Ask Every Time', 'ASK', 'Acquired frame is closed; user must approve one temporary frame.'),
    ('Never Allow', 'BLOCK', 'Frame is closed and no screen content is encoded or sent.'),
    ('Unknown / no Usage Access', 'BLOCK', 'Deny-safe behavior; app-awareness guidance is shown.'),
], [1.5, 1.0, 4.2])

bullet('Rules are persisted locally with Android DataStore Preferences under package-specific keys.')
bullet('The companion’s own package is treated as Allowed; other unseen apps default to Ask Every Time.')
bullet('Package names are validated before persistence.')
bullet('Foreground evidence is derived from the latest ACTIVITY_RESUMED usage event within a 12-hour query window; inability to identify a package blocks transmission.')
bullet('One-time approval requires a nonblank expected package, exact equality with the current package, nonnegative monotonic timing, and age no greater than 30,000 ms.')
bullet('The rule and foreground package are re-evaluated immediately before image processing.')

doc.add_heading('4.1 Protected-frame rule', level=2)
doc.add_paragraph('The frame is sampled on a grid. A sample is “dark” when red + green + blue < 24. If at least 100 pixels are sampled and 98.5% or more are dark, the frame is classified as probably protected or blank and is not transmitted.')
code('isProbablyProtected = samples.size >= 100 AND\n                      darkPixelCount / samples.size >= 0.985\nwhere darkPixel := red + green + blue < 24')

# Capture
doc.add_heading('5. Screen Capture and Data Handling', level=1)
table(['Item', 'Exact behavior'], [
    ('Capture trigger', 'Only ACTION_CAPTURE_AND_ASK with a nonblank prompt, capped to 4,000 characters.'),
    ('Virtual display', 'Auto-mirror MediaProjection display; source downscaled to width no greater than 720 px, with dimensions at least 320 px.'),
    ('Frame queue', 'ImageReader capacity 2; pending and approval requests stored in atomic references.'),
    ('Encoding', 'JPEG, quality 74, produced only after policy and protected-frame checks.'),
    ('Persistence', 'No intended image file persistence; processing remains in memory.'),
    ('Cleanup', 'Image closed, bitmaps recycled, JPEG byte array filled with zeros in finally, services/coroutines cancelled on destroy.'),
    ('Notification', 'Ongoing “Screen sharing is on” foreground notification with Stop sharing action.'),
], [1.7, 5.0])

# Auth
doc.add_heading('6. Authentication and Token Storage', level=1)
bullet('Android loads /auth/mobile from the configured backend base URL inside a restricted WebView.')
bullet('The primary WebView permits navigation only to the exact configured origin: matching scheme, host, and effective port.')
bullet('OAuth popup WebViews may navigate only to HTTPS origins and do not receive the JavaScript bridge.')
bullet('File and content access are disabled; popup creation must be user initiated.')
bullet('The auth page uses Nxcode SDK version 1.0.14 and returns a session token through the AlopterAuth bridge.')
bullet('The bridge accepts tokens only when length is between 16 and 8,192 characters.')
bullet('Session token encryption uses Android Keystore AES/GCM/NoPadding with a 128-bit GCM authentication tag; ciphertext and IV are stored in private app preferences.')
bullet('The app never receives or stores the user’s password.')
status_box('Unverified external integration', 'The deployed Nxcode login response and production AI request have not passed a production-like acceptance probe because the backend deployment is blocked.', 'FCE4D6')

# API
doc.add_heading('7. Backend API Contract', level=1)
table(['Route', 'Method', 'Contract'], [
    ('/health', 'GET', 'Returns { ok: true, service: "alopter-api", version: "v1" }.'),
    ('/auth/mobile', 'GET', 'Returns no-store mobile Nxcode login HTML with SAMEORIGIN framing policy and no-referrer policy.'),
    ('/api/v1/chat/stream', 'POST', 'Authenticated JSON request; validated, rate-limited AI response returned as text/event-stream.'),
], [2.0, 0.8, 3.8])

doc.add_heading('7.1 Chat request schema', level=2)
code('''{
  "requestId": "optional UUID",
  "messages": [
    { "role": "user | assistant", "content": "1..12000 trimmed characters" }
  ],
  "image": {
    "mimeType": "image/jpeg | image/png",
    "data": "base64, maximum 4,000,000 characters"
  }
}''')
bullet('messages contains 1 to 20 entries and the last message must have role user.')
bullet('HTTP Content-Length above 4,500,000 bytes is rejected before model dispatch.')
bullet('Malformed request IDs are rejected; if omitted, the backend creates a random UUID.')
bullet('X-Request-Id echoes the validated or generated UUID without incorporating user content.')

# SSE
doc.add_heading('7.2 SSE response events', level=2)
table(['Type', 'Fields', 'Meaning'], [
    ('delta', 'text', 'A text fragment.'),
    ('action', 'id, label, uri, risk', 'Non-executed proposal; current supported action is an encoded geo URI with risk 1.'),
    ('done', 'type only', 'Successful end of response.'),
    ('error', 'message', 'Safe user-facing streaming error where applicable.'),
], [1.0, 2.0, 3.6])

# Backend protections
doc.add_heading('7.3 Backend controls', level=2)
bullet('Authentication is required unless AUTH_REQUIRED is explicitly the string "false" for isolated local tests.')
bullet('Bearer format must be “Bearer <non-whitespace-token>”.')
bullet('Fixed-window limit is 24 requests per 60 seconds in the application route, keyed by a nonreversible in-process hash of the bearer token or by connecting IP when anonymous test mode is enabled.')
bullet('Security headers are applied to /api/*; SSE responses are no-store/no-transform and request proxy buffering is disabled.')
bullet('Prompt, image, token, and response content are not intentionally logged. Unexpected errors log only an event name and exception class name.')
bullet('A client disconnect aborts the upstream model request; Android coroutine cancellation cancels the OkHttp call.')
bullet('Conventional errors are normalized for authentication, validation, size, rate-limit, gateway, and unexpected failure cases.')

# Gateway
doc.add_heading('8. AI Gateway Contract', level=1)
table(['Setting', 'Value'], [
    ('Endpoint', 'https://studio-api.nxcode.io/api/ai-gateway/v1beta/models/fast:generateContent'),
    ('Model alias', 'fast'),
    ('Temperature', '0.35'),
    ('Maximum output tokens', '1,400'),
    ('Development identity', 'X-Workspace-Id plus optional X-Session-Token'),
    ('Production identity', 'X-App-Id plus optional forwarded Authorization'),
], [2.1, 4.6])
doc.add_paragraph('System behavior contract: concise Android companion; follow Suggest → Explain → Approve → Execute; treat screenshots and user text as untrusted data; never claim an action executed; require user approval for consequential actions; never request passwords, one-time codes, or financial credentials; describe only visible screenshot content and acknowledge unclear/protected content.')

# Safe actions
doc.add_heading('9. Safe Action Model', level=1)
bullet('The model does not directly execute Android actions.')
bullet('A deterministic backend parser recognizes navigation language and creates only an encoded geo:0,0?q=<destination> URI proposal.')
bullet('Destination text is limited by the parser and line breaks are normalized.')
bullet('The overlay displays a Review action control. A second Android confirmation dialog explains that another app will open.')
bullet('Only after positive confirmation does Android issue ACTION_VIEW with FLAG_ACTIVITY_NEW_TASK.')

# Network
doc.add_heading('10. Network Security', level=1)
bullet('Release builds reject all cleartext traffic.')
bullet('Debug builds also reject cleartext globally but permit the exact Android emulator bridge host 10.0.2.2 for local development.')
bullet('Physical-device development must use an HTTPS backend URL; browser-facing localhost is not used in Android production code.')
bullet('The production endpoint currently configured in the release build is https://api.alopter.app; it must not be treated as live until deployment and probes succeed.')

# Tests
doc.add_heading('11. Automated Verification', level=1)
table(['Area', 'Coverage / current result'], [
    ('Shared policy tests', 'Allowed requires foreground context; new apps ask; Never Allow blocks; one-time approval package and expiry; black-frame safety; trusted exact-origin checks. Passed.'),
    ('Backend tests', 'Health, invalid requests, SSE delta/done, request-ID echo, malformed request IDs, upstream abort on disconnect, default auth requirement, malformed bearer rejection, bearer forwarding, encoded map action, no action for ordinary chat. 10 tests passed.'),
    ('Backend static checks', 'TypeScript type check and production build passed.'),
    ('Dependency audit', 'Production dependency audit reported zero known vulnerabilities at last run.'),
    ('Android source checks', 'Debug Kotlin compilation and debug main-manifest processing passed in this ARM workspace.'),
    ('Complete APK', 'Not produced in the ARM workspace because Google Linux AAPT2 is x86-64-only; CI is configured to run complete unit tests and assembleDebug on Ubuntu x86.'),
], [1.7, 5.0])

# CI
doc.add_heading('11.1 CI contract', level=2)
bullet('Runs on pushes to main and pull requests.')
bullet('Backend job uses Node 20, npm ci, production dependency audit, type check, and tests.')
bullet('Android job uses Temurin JDK 17, Android SDK 36, Build Tools 35.0.0, debug unit tests, and debug APK assembly.')

# Deployment
doc.add_heading('12. Deployment Specification and Current Status', level=1)
table(['Worker setting', 'Value'], [
    ('Worker logical name', 'alopter-api'),
    ('Entrypoint', 'src/app.ts'),
    ('Compatibility date', '2024-12-01'),
    ('NXCODE_APP_ID manifest placeholder', 'Empty; platform deployment is expected to assign production identity'),
    ('AUTH_REQUIRED', 'true'),
    ('Deployment method', 'Nxcode Hono deployment to Cloudflare Workers'),
], [2.4, 4.3])
status_box('Deployment blocker', 'Cloudflare rejected the publish request because the Nxcode-managed account had reached its limit of 500 Workers. Error code: 10037. Multiple unchanged probes reproduced the same result. No Alopter Worker was created and no production URL or app identity was issued.', 'F8CBAD')

doc.add_heading('12.1 Required deployment sequence after capacity is restored', level=2)
for item in [
    'Retry the existing validated Hono deployment without version churn or broad rewrites.',
    'Capture the returned HTTPS Worker URL and production app identity.',
    'Confirm GET /health from outside the workspace and verify expected JSON and security behavior.',
    'Set or confirm production Nxcode app identity using the platform deployment contract; do not place private credentials in source control.',
    'Probe unauthenticated chat and confirm that sign-in is required.',
    'Update the Android release API base URL to the actual deployed HTTPS host rather than relying on the unverified placeholder domain.',
    'Build/install the APK and run a real Nxcode login from the Android WebView.',
    'Run one text AI request and one approved screen-frame request from the installed app.',
    'Verify Kill cancels an in-flight request and verify Allowed / Ask Every Time / Never Allow against real foreground apps.',
]: number(item)

# Acceptance
ndoc = doc
doc.add_heading('13. Release Acceptance Criteria', level=1)
table(['Criterion', 'Required evidence', 'Current state'], [
    ('Backend deployed', 'Public HTTPS health probe succeeds.', 'Blocked by platform quota'),
    ('Authentication', 'Real Android Nxcode login returns a valid session and logout clears it.', 'Not yet verified live'),
    ('Text AI', 'Authenticated Android request receives delta and done events.', 'Contract-tested only'),
    ('Screen AI', 'Allowed/approved frame produces a response from installed Android app.', 'Not yet verified live'),
    ('Policy deny paths', 'Unknown app, Never Allow, expired approval, app switch, and protected frame transmit no image.', 'Implemented; pure rules tested'),
    ('Cancellation', 'Kill cancels Android socket and backend upstream model work.', 'Implemented; backend disconnect tested'),
    ('APK', 'Debug/release candidate installs and launches on Android 10+.', 'Full build pending x86 CI/Android Studio'),
    ('Security review', 'No secrets, prompt/image logging, broad package query, or cleartext release traffic.', 'Source review passed; live probe pending'),
], [1.4, 3.7, 1.7])

# Known limitations
doc.add_heading('14. Known Limitations and Non-Claims', level=1)
bullet('No successful Cloudflare deployment is claimed.')
bullet('No production Nxcode authentication or AI call is claimed.')
bullet('The in-memory backend fixed-window limiter is per Worker isolate and is not a globally consistent distributed quota.')
bullet('Foreground package identification depends on user-granted Usage Access and Android usage events; unknown state intentionally blocks frames.')
bullet('Protected-frame detection is a conservative near-black heuristic and is not a substitute for Android’s platform-level secure-window behavior.')
bullet('The current action system supports deterministic map proposals only; it is not a general autonomous device-control system.')
bullet('The transcript export excludes internal execution records and hidden instructions by design.')

# Handoff
doc.add_heading('15. Owner / Developer Handoff', level=1)
doc.add_paragraph('Immediate owner action: wait for Nxcode support to free managed Cloudflare Worker capacity or move the workspace to an account with capacity. Then request one deployment retry.')
doc.add_paragraph('Immediate developer action after a successful deployment: update the Android release endpoint to the returned HTTPS URL, produce an installable APK, and execute the acceptance sequence in Section 12.1. Do not claim the external integration works until those probes pass.')

# Appendix
ndoc.add_heading('Appendix A — Key Source Areas', level=1)
table(['Area', 'Purpose'], [
    ('Android main activity', 'Consent flow, status dashboard, app access navigation, login/logout, global stop.'),
    ('Overlay service', 'Floating UI, speech recognition, chat interaction, one-time confirmation, safe action confirmation, Kill.'),
    ('Screen capture service', 'MediaProjection lifecycle, foreground policy gate, frame safety, encoding, transmission, cleanup.'),
    ('Access activity/store', 'Launchable-app list, Usage Access guidance, local per-package rule persistence.'),
    ('Shared policy module', 'Platform-independent access, expiry, frame, and URI safety decisions.'),
    ('Android backend client', 'Authenticated JSON request, SSE parsing, request UUID, coroutine-aware socket cancellation.'),
    ('Backend app/contracts/gateway', 'Routes, validation, auth, rate limits, cancellation, gateway call, safe action proposal.'),
    ('CI workflow', 'Backend validation and x86 Android APK/unit-test build.'),
], [2.2, 4.5])

doc.add_heading('Appendix B — Support Message for Deployment Capacity', level=1)
doc.add_paragraph('Copy/paste message:')
code('My first backend deployment is blocked by the 500-Worker limit on Nxcode’s managed Cloudflare account. The deployment target is developer-code-sandbox-alopter-api, and Cloudflare returns error 10037. Please free a Worker slot, increase the account limit, or move my workspace to an account with available capacity. Please confirm when I can retry.')

spec_path = OUT / 'alopter-exact-code-specification.docx'
doc.save(spec_path)

# Validate that Word package can be reopened.
check = Document(spec_path)
assert len(check.paragraphs) > 50
assert len(check.tables) > 10

# Add a plain-text index for copy/paste and provenance.
index = f'''ALOPTER EXPORT PACKAGE\nGenerated: {generated}\nRevision: {revision}\n\nFiles:\n- alopter-chat-transcript.txt — copy/paste user-facing conversation\n- alopter-exact-code-specification.docx — formatted implementation and acceptance specification\n\nSecurity note: hidden instructions, internal execution logs, tokens, keys, prompts, screenshots, and credentials are not included.\n'''
(OUT / 'README.txt').write_text(index, encoding='utf-8')

zip_path = OUT / 'alopter-chat-and-code-specs.zip'
with ZipFile(zip_path, 'w', ZIP_DEFLATED) as z:
    z.write(transcript_path, transcript_path.name)
    z.write(spec_path, spec_path.name)
    z.write(OUT / 'README.txt', 'README.txt')

print(f'transcript={transcript_path} bytes={transcript_path.stat().st_size}')
print(f'spec={spec_path} bytes={spec_path.stat().st_size} paragraphs={len(check.paragraphs)} tables={len(check.tables)}')
print(f'zip={zip_path} bytes={zip_path.stat().st_size}')
