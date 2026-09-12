type Bucket = { count: number; resetAt: number }

export class FixedWindowLimiter {
  private readonly buckets = new Map<string, Bucket>()
  constructor(private readonly limit = 30, private readonly windowMs = 60_000) {}

  consume(key: string, now = Date.now()): { allowed: boolean; remaining: number; retryAfterSeconds: number } {
    let bucket = this.buckets.get(key)
    if (!bucket || bucket.resetAt <= now) {
      bucket = { count: 0, resetAt: now + this.windowMs }
      this.buckets.set(key, bucket)
    }
    bucket.count++
    if (this.buckets.size > 10_000) this.prune(now)
    return { allowed: bucket.count <= this.limit, remaining: Math.max(0, this.limit - bucket.count), retryAfterSeconds: Math.max(1, Math.ceil((bucket.resetAt - now) / 1000)) }
  }

  private prune(now: number) { for (const [key, value] of this.buckets) if (value.resetAt <= now) this.buckets.delete(key) }
}
