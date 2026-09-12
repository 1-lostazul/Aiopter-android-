import { serve } from '@hono/node-server'
import { createApp, type Env } from './app.js'

const port = Number(process.env.PORT || 3001)
const env: Env = { THREAD_ID: process.env.THREAD_ID, NXCODE_APP_ID: process.env.NXCODE_APP_ID, AUTH_REQUIRED: process.env.AUTH_REQUIRED }
const app = createApp()
serve({ fetch: request => app.fetch(request, env), port, hostname: '0.0.0.0' }, info => console.log(`Alopter API listening on http://0.0.0.0:${info.port}`))
