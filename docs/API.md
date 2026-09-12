# Mobile API v1

## Health

`GET /health` returns service identity and version.

## Chat stream

`POST /api/v1/chat/stream` accepts JSON with up to 20 `user`/`assistant` messages. The final message must be from the user. An optional image contains a JPEG or PNG MIME type and base64 data. An optional UUID `requestId` is echoed in the `X-Request-Id` response header for content-free diagnostics; the server creates one when omitted. Encoded requests are capped before model dispatch.

The response is `text/event-stream`. Events are JSON objects with one of these types:

- `delta`: a text fragment
- `action`: a non-executed, risk-classified proposal with label and Android-safe URI
- `done`: successful end of response
- `error`: safe user-facing failure

Authentication is required by default. Send the Nxcode bearer token; the service forwards it to the Nxcode AI gateway for verification before an answer can be produced. Clients should cancel the HTTP call when the user taps Kill; the service propagates that disconnect to in-flight model work, and consequential actions must never be retried automatically.
