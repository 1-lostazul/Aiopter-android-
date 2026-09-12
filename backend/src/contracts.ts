import { z } from 'zod'

export const ChatMessageSchema = z.object({
  role: z.enum(['user', 'assistant']),
  content: z.string().trim().min(1).max(12_000),
})

export const ChatRequestSchema = z.object({
  messages: z.array(ChatMessageSchema).min(1).max(20),
  image: z.object({
    mimeType: z.enum(['image/jpeg', 'image/png']),
    data: z.string().max(4_000_000),
  }).optional(),
}).superRefine((input, context) => {
  if (input.messages.at(-1)?.role !== 'user') context.addIssue({ code: z.ZodIssueCode.custom, message: 'The final message must be from the user.' })
})

export type ChatRequest = z.infer<typeof ChatRequestSchema>

export type SafeAction = { id: string; label: string; uri: string; risk: 1 }

export function deriveSafeAction(prompt: string): SafeAction | null {
  const match = prompt.match(/(?:navigate|directions|map|take me)\s+(?:to\s+)?(.{2,120})/i)
  if (!match?.[1]) return null
  const destination = match[1].replace(/[\r\n]/g, ' ').trim()
  return { id: crypto.randomUUID(), label: `Open directions to ${destination}`, uri: `geo:0,0?q=${encodeURIComponent(destination)}`, risk: 1 }
}
