import { Hono } from 'hono'
import { secureHeaders } from 'hono/secure-headers'
import { ChatRequestSchema, deriveSafeAction } from './contracts.js'
import { generateAssistantReply, GatewayError } from './gateway.js'
import { FixedWindowLimiter } from './rate-limit.js'
import { mobileAuthPage } from './auth-page.js'

export type Env = { THREAD_ID?: string; NXCODE_APP_ID?: string; AUTH_REQUIRED?: string }
type Provider = typeof generateAssistantReply

export function createApp(provider: Provider = generateAssistantReply) {
  const app = new Hono<{ Bindings: Env }>()
  const limiter = new FixedWindowLimiter(24, 60_000)
  app.use('/api/*', secureHeaders())

  app.get('/health', c => hasGatewayIdentity(c.env)
    ? c.json({ ok: true, service: 'aiopter-api', version: 'v1' })
    : c.json({ ok: false, service: 'aiopter-api', error: 'Service identity is not configured.' }, 503))
  app.get('/auth/mobile', c => c.html(mobileAuthPage, 200, { 'Cache-Control': 'no-store', 'X-Frame-Options': 'SAMEORIGIN', 'Referrer-Policy': 'no-referrer' }))

  app.post('/api/v1/chat/stream', async c => {
    if (!hasGatewayIdentity(c.env)) return c.json({ error: 'The assistant service is not configured.' }, 503)
    const authorization = c.req.header('Authorization')
    const bearer = authorization?.match(/^Bearer\s+(\S+)$/i)?.[1]
    const isProduction = Boolean(c.env.NXCODE_APP_ID?.trim())
    const authRequired = isProduction || c.env.AUTH_REQUIRED !== 'false'
    if (authRequired && !bearer) return c.json({ error: 'Sign in is required.' }, 401)
    const identity = bearer ? `user:${hash(bearer)}` : `ip:${c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local'}`
    const rate = limiter.consume(identity)
    c.header('X-RateLimit-Remaining', String(rate.remaining))
    if (!rate.allowed) { c.header('Retry-After', String(rate.retryAfterSeconds)); return c.json({ error: 'Too many requests. Please wait a moment.' }, 429) }

    const contentLength = Number(c.req.header('content-length') || 0)
    if (contentLength > 4_500_000) return c.json({ error: 'Request is too large.' }, 413)
    const raw = await c.req.json().catch(() => null)
    const parsed = ChatRequestSchema.safeParse(raw)
    if (!parsed.success) return c.json({ error: 'Invalid chat request.', issues: parsed.error.issues.map(i => ({ path: i.path.join('.'), message: i.message })) }, 400)

    const requestId = parsed.data.requestId ?? crypto.randomUUID()
    c.header('X-Request-Id', requestId)
    const aborter = new AbortController()
    const cancelUpstream = () => aborter.abort()
    c.req.raw.signal.addEventListener('abort', cancelUpstream, { once: true })
    const reply = await provider(parsed.data, c.env, bearer ? `Bearer ${bearer}` : undefined, aborter.signal).catch(error => {
      if (error instanceof GatewayError) return { error: error.message, status: error.status } as const
      return { error: 'The assistant is temporarily unavailable.', status: 503 } as const
    }).finally(() => c.req.raw.signal.removeEventListener('abort', cancelUpstream))
    if (typeof reply !== 'string') return c.json({ error: reply.error }, reply.status as 429 | 500 | 502 | 503)

    const action = deriveSafeAction(parsed.data.messages.at(-1)!.content)
    const encoder = new TextEncoder()
    const stream = new ReadableStream({
      async start(controller) {
        try {
          for (const chunk of chunkText(reply, 44)) {
            controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'delta', text: chunk })}\n\n`))
            await delay(14)
          }
          if (action) controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'action', ...action })}\n\n`))
          controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'done' })}\n\n`))
        } finally { controller.close() }
      },
      cancel() { aborter.abort() },
    })
    return new Response(stream, { headers: { 'Content-Type': 'text/event-stream; charset=utf-8', 'Cache-Control': 'no-store, no-transform', 'X-Accel-Buffering': 'no', 'X-Request-Id': requestId } })
  })
  app.notFound(c => c.json({ error: 'Not found' }, 404))
  app.onError((error, c) => { console.error('request_failed', { name: error.name }); return c.json({ error: 'Unexpected server error.' }, 500) })
  return app
}

function chunkText(text: string, target: number): string[] {
  const words = text.split(/(\s+)/); const chunks: string[] = []; let current = ''
  for (const word of words) { if (current.length + word.length > target && current) { chunks.push(current); current = '' }; current += word }
  if (current) chunks.push(current); return chunks
}
function delay(ms: number) { return new Promise(resolve => setTimeout(resolve, ms)) }
function hasGatewayIdentity(env: Env): boolean { return Boolean(env.NXCODE_APP_ID?.trim() || env.THREAD_ID?.trim()) }
function hash(value: string) { let result = 2166136261; for (let i = 0; i < value.length; i++) result = Math.imul(result ^ value.charCodeAt(i), 16777619); return (result >>> 0).toString(16) }

export default createApp()
