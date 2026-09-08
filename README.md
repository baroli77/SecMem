# Second Memory

A personal attention system: **save it now, remember it when it matters.**

This is a local-first web app. The original product spec targeted Android (share sheet, WorkManager, Play Billing). This build keeps that capture → understand → resurface loop in the browser so it can be used immediately.

It is not a notes app, a todo list, a bookmark manager, or a chatbot.

## What it does

1. Capture a URL, some text, or an image in a couple of seconds.
2. Persist it on this device before any network call.
3. Optionally enrich it (title, summary, category, dates, when to bring it back).
4. Surface it later according to conservative rules (reading window, payday, weekend meal planning, detected due dates).
5. Act with Done / Later / Open. Search the library offline.

## Architecture

| Layer | Location | Notes |
| --- | --- | --- |
| UI | `src/routes/*`, `src/components/*` | TanStack Router screens (Today, Inbox, Library, Detail, Search, Activity, Settings, Onboarding) |
| Domain | `src/lib/memory/*` | Thing model, heuristics, resurface engine, search, seed |
| Persistence | Zustand + `localStorage` (`second-memory`) | Source of truth is local. Capture never waits on a server. |
| AI | `src/lib/ai/analyze.ts` | Provider-shaped server function. xAI `grok-4.5`, JSON schema. Optional. |
| Metadata | `src/lib/ai/metadata.ts` | Open Graph / YouTube oEmbed with short timeouts |
| Entitlements | `settings.isPro` | Free: 40 active items. Completed/archived do not count. Existing items are never hidden. |

AI is provider-independent at the result layer (`AiAnalysis`). The first implementation talks to xAI because that key is present in this environment. Keys are never hardcoded.

## Module map (web)

```
src/lib/memory    domain + store + resurface + heuristics + enrichment queue
src/lib/ai        analyze + URL metadata (server functions)
src/components    capture, thing cards, shell
src/routes        Today, Inbox, Library, Search, Activity, Settings, Detail, Onboarding
```

## Build

```
npm install
npm run dev
npm run typecheck
npm test
npm run build
```

No `.env` file. If `XAI_API_KEY` is injected, enrichment is live. If it is missing, capture, search, resurface, and edit still work; enrichment is marked failed and can be retried with backoff.

## AI provider setup

- Server-only: `process.env.XAI_API_KEY`
- Model: `grok-4.5`
- Structured JSON only. Failures never delete a captured item.
- Automatic enrichment can be turned off in Settings. Manual capture continues. Manual retry still works if enrichment is allowed.

## Testing

```
npm test
```

Coverage is around the parts that must not be vague:

- capture parsing and classification
- date / person extraction
- duplicate URL canonicalisation
- resurface policies, quiet hours, throttle
- snooze options
- FTS-style local search
- AI JSON parsing
- Open Graph parsing
- enrichment retry backoff

## Major decisions

- **Web, not Android**, in this environment. Share sheet is modeled as paste / drop / `?url=` / `?text=`.
- **Local-first.** Room’s role is `localStorage` here. Cloud sync is not required for v1.
- **Deterministic resurface first**, AI suggestions as an enhancement. Notifications are throttled and respect quiet hours.
- **No chatbot.** Intelligence is invisible: title, category, date, when to return.
- **Play Billing** is abstracted as an entitlement flag. There is no store connection in this web build.
- **OCR** for screenshots uses the vision path of the same analysis call when an image is attached.
- **Onboarding does not seed fake memories.** Settings can load examples for a demo.

## Known limitations

- Not a native Android package (no WorkManager, no system share target, no Play Billing).
- Enrichment requires a network and an API key; heuristics cover the offline case.
- Search is local substring/FTS-style, not embeddings.
- Image attachments are compressed and stored as data URLs; very large libraries of photos will pressure `localStorage`.
- Browser notifications are optional and must be granted in Settings.

## Product loop

Share → stored instantly → understood in the background → forgotten → brought back at a useful time → Done / Later / Open.
