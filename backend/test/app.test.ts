import { describe, expect, it } from 'vitest'
import { createApp } from '../src/app.js'
import { deriveSafeAction } from '../src/contracts.js'

const provider = async () => 'I can help with that safely.'
const env = { THREAD_ID: 'test-thread', AUTH_REQUIRED: 'false' }

describe('AIopter API', () => {
  it('reports health without exposing internals', async () => {
    const response = await createApp(provider).request('/health', {}, env)
    expect(response.status).toBe(200)
    expect(await response.json()).toEqual({ ok: true, service: 'aiopter-api', version: 'v1' })
  })
  it('fails health and chat safely when gateway identity is missing', async () => {
    const app = createApp(provider)
    const health = await app.request('/health', {}, {})
    expect(health.status).toBe(503)
    expect(await health.json()).toEqual({ ok: false, service: 'aiopter-api', error: 'Service identity is not configured.' })
    const chat = await app.request('/api/v1/chat/stream', {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: 'Bearer test-session' },
      body: JSON.stringify({ messages: [{ role: 'user', content: 'Hello' }] }),
    }, {})
    expect(chat.status).toBe(503)
  })
  it('cannot disable authentication when a production app identity is configured', async () => {
    const response = await createApp(provider).request('/api/v1/chat/stream', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ messages: [{ role: 'user', content: 'Hello' }] }),
    }, { NXCODE_APP_ID: 'production-app', AUTH_REQUIRED: 'false' })
    expect(response.status).toBe(401)
  })
  it('rejects invalid chat payloads', async () => {
    const response = await createApp(provider).request('/api/v1/chat/stream', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ messages: [] }) }, env)
    expect(response.status).toBe(400)
  })
  it('streams text and completion with the client request ID', async () => {
    const requestId = 'd9afad2e-c6f2-4c2b-917a-4d7ddfdc8e1d'
    const response = await createApp(provider).request('/api/v1/chat/stream', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ requestId, messages: [{ role: 'user', content: 'Hello' }] }) }, env)
    expect(response.status).toBe(200)
    expect(response.headers.get('x-request-id')).toBe(requestId)
    const body = await response.text()
    expect(body).toContain('"type":"delta"')
    expect(body).toContain('"type":"done"')
  })
  it('rejects malformed request IDs', async () => {
    const response = await createApp(provider).request('/api/v1/chat/stream', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ requestId: 'device-name-or-other-pii', messages: [{ role: 'user', content: 'Hello' }] }) }, env)
    expect(response.status).toBe(400)
  })
  it('aborts model work when the mobile request disconnects', async () => {
    let markStarted: (() => void) | undefined
    const started = new Promise<void>(resolve => { markStarted = resolve })
    let upstreamAborted = false
    const waitingProvider = async (_input: unknown, _env: unknown, _authorization?: string, signal?: AbortSignal) =>
      new Promise<string>((_resolve, reject) => {
        signal?.addEventListener('abort', () => {
          upstreamAborted = true
          reject(new Error('aborted'))
        }, { once: true })
        markStarted?.()
      })
    const controller = new AbortController()
    const response = createApp(waitingProvider as typeof provider).request('/api/v1/chat/stream', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ messages: [{ role: 'user', content: 'Hello' }] }),
      signal: controller.signal,
    }, env)
    await started
    controller.abort()
    await response
    expect(upstreamAborted).toBe(true)
  })
  it('requires login by default', async () => {
    const response = await createApp(provider).request('/api/v1/chat/stream', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ messages: [{ role: 'user', content: 'Hello' }] }) }, { THREAD_ID: 'test-thread' })
    expect(response.status).toBe(401)
  })
  it('rejects malformed bearer credentials', async () => {
    const response = await createApp(provider).request('/api/v1/chat/stream', { method: 'POST', headers: { 'content-type': 'application/json', authorization: 'Bearer' }, body: JSON.stringify({ messages: [{ role: 'user', content: 'Hello' }] }) }, { THREAD_ID: 'test-thread' })
    expect(response.status).toBe(401)
  })
  it('accepts a bearer credential for gateway verification', async () => {
    let forwarded: string | undefined
    const inspectingProvider = async (_input: unknown, _env: unknown, authorization?: string) => { forwarded = authorization; return 'Verified upstream.' }
    const response = await createApp(inspectingProvider as typeof provider).request('/api/v1/chat/stream', { method: 'POST', headers: { 'content-type': 'application/json', authorization: 'Bearer test-session' }, body: JSON.stringify({ messages: [{ role: 'user', content: 'Hello' }] }) }, { THREAD_ID: 'test-thread' })
    expect(response.status).toBe(200)
    expect(forwarded).toBe('Bearer test-session')
  })
})

describe('safe actions', () => {
  it('creates an encoded maps intent instead of executing it', () => {
    const action = deriveSafeAction('Navigate to Union Square, New York')
    expect(action?.uri).toBe('geo:0,0?q=Union%20Square%2C%20New%20York')
    expect(action?.risk).toBe(1)
  })
  it('does not invent actions for ordinary chat', () => expect(deriveSafeAction('Explain this screen')).toBeNull())
})
