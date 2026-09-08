import type { Thing } from "./types";

export function searchThings(things: Thing[], query: string): Thing[] {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  const terms = q.split(/\s+/).filter(Boolean);
  const scored = things
    .map((t) => ({ t, score: scoreThing(t, terms, q) }))
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score || b.t.updatedAt - a.t.updatedAt);
  return scored.map((x) => x.t);
}

function scoreThing(t: Thing, terms: string[], raw: string): number {
  const hay = [
    t.title,
    t.summary,
    t.originalContent,
    t.notes,
    t.ocrText,
    t.sourceUrl,
    t.siteName,
    t.detectedPerson,
    t.detectedAction,
    t.category,
    t.tags.join(" "),
  ]
    .filter(Boolean)
    .join("\n")
    .toLowerCase();

  if (raw.length >= 3 && hay.includes(raw)) return 50 + Math.min(20, raw.length);
  let hits = 0;
  for (const term of terms) {
    if (!hay.includes(term)) return 0;
    if (t.title.toLowerCase().includes(term)) hits += 8;
    else if (t.tags.some((tag) => tag.toLowerCase().includes(term))) hits += 5;
    else hits += 2;
  }
  return hits;
}
