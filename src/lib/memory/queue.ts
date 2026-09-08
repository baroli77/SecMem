import { MAX_ENRICH_ATTEMPTS, type Thing } from "./types.ts";

export function nextRetryDelayMs(attempts: number): number {
  const n = Math.max(0, attempts);
  const minutes = Math.min(60, 2 ** n);
  return minutes * 60_000;
}

export function shouldRetryEnrichment(thing: Thing, now = Date.now()): boolean {
  if (thing.status === "completed" || thing.status === "archived") return false;
  if (thing.processingStatus === "QUEUED") return true;
  if (thing.processingStatus !== "FAILED") return false;
  const attempts = thing.enrichAttempts ?? 0;
  if (attempts >= MAX_ENRICH_ATTEMPTS) return false;
  const last = thing.lastEnrichAttempt ?? 0;
  return now - last >= nextRetryDelayMs(attempts);
}

export function pickPending(things: Thing[], now = Date.now(), limit = 3): Thing[] {
  return things.filter((t) => shouldRetryEnrichment(t, now)).slice(0, limit);
}
