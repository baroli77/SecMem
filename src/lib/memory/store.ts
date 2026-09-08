import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type {
  ActivityEvent,
  ActivityType,
  CaptureInput,
  Settings,
  Thing,
  ThingStatus,
} from "./types";
import { DEFAULT_SETTINGS, FREE_ACTIVE_LIMIT, activeCount } from "./types";
import { findDuplicate, parseCaptureInput } from "./heuristics";
import {
  formatDay,
  notificationCopy,
  shouldNudge,
  staleUnopened,
  suggestResurfaceAt,
} from "./resurface";
import { makeSeed } from "./seed";
import type { AiAnalysis } from "./types";

export interface CaptureResult {
  thing: Thing;
  duplicate?: Thing;
  blocked?: "limit";
}

interface MemoryState {
  things: Thing[];
  activities: ActivityEvent[];
  settings: Settings;
  hydrated: boolean;
  captureOpen: boolean;
  setCaptureOpen: (open: boolean) => void;
  setHydrated: () => void;
  capture: (input: CaptureInput) => CaptureResult;
  updateThing: (id: string, patch: Partial<Thing>) => void;
  setStatus: (id: string, status: ThingStatus) => void;
  complete: (id: string) => void;
  completeMany: (ids: string[]) => void;
  snooze: (id: string, until: number) => void;
  snoozeMany: (ids: string[], until: number) => void;
  archive: (id: string) => void;
  archiveMany: (ids: string[]) => void;
  restore: (id: string) => void;
  keep: (id: string) => void;
  remove: (id: string) => void;
  togglePin: (id: string) => void;
  toggleFavourite: (id: string) => void;
  openThing: (id: string) => void;
  applyAi: (id: string, result: AiAnalysis, provider?: string, model?: string) => void;
  markProcessing: (id: string, status: Thing["processingStatus"], error?: string) => void;
  logActivity: (
    type: ActivityType,
    thing?: Pick<Thing, "id" | "title">,
    detail?: string,
  ) => void;
  patchSettings: (patch: Partial<Settings>) => void;
  completeOnboarding: () => void;
  loadExamples: () => void;
  tickResurface: (now?: number) => Thing[];
  recordNudge: (now?: Date) => boolean;
  importSnapshot: (data: { things: Thing[]; activities?: ActivityEvent[]; settings?: Partial<Settings> }) => void;
  resetAll: () => void;
  bumpAiUsage: () => void;
}

function nid(prefix: string): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function capActivities(events: ActivityEvent[]): ActivityEvent[] {
  return events.slice(0, 400);
}

export const useMemoryStore = create<MemoryState>()(
  persist(
    (set, get) => ({
      things: [],
      activities: [],
      settings: { ...DEFAULT_SETTINGS },
      hydrated: false,
      captureOpen: false,
      setCaptureOpen: (open) => set({ captureOpen: open }),
      setHydrated: () => set({ hydrated: true }),

      capture: (input) => {
        const state = get();
        const parsed = parseCaptureInput(input);
        if (!input.forceDuplicate) {
          const duplicate = findDuplicate(state.things, parsed.sourceUrl);
          if (duplicate) {
            return {
              thing: duplicate,
              duplicate,
            };
          }
        }
        if (
          !state.settings.isPro &&
          activeCount(state.things) >= FREE_ACTIVE_LIMIT
        ) {
          return {
            thing: state.things[0]!,
            blocked: "limit",
          };
        }

        const now = Date.now();
        const draft: Thing = {
          id: nid("th"),
          createdAt: now,
          updatedAt: now,
          originalContent: parsed.originalContent,
          contentType: parsed.contentType,
          sourceUrl: parsed.sourceUrl,
          sourceApp: parsed.sourceApp ?? "Second Memory",
          title: parsed.title,
          summary: parsed.summary,
          imageDataUrl: parsed.imageDataUrl,
          mimeType: parsed.mimeType,
          category: parsed.category,
          status: "inbox",
          priority: parsed.dueAt ? "high" : "normal",
          dueAt: parsed.dueAt,
          resurfaceCount: 0,
          isPinned: false,
          isFavourite: false,
          aiProcessed: false,
          processingStatus: state.settings.automaticProcessing && state.settings.aiEnabled
            ? "QUEUED"
            : "NONE",
          enrichAttempts: 0,
          tags: parsed.tags,
          detectedDate: parsed.detectedDate,
          detectedTime: parsed.detectedTime,
          detectedPerson: parsed.detectedPerson,
        };
        const suggestion = suggestResurfaceAt(draft, state.settings, new Date(now));
        draft.resurfaceAt = suggestion.at;
        draft.reasonForResurface = suggestion.reason;

        set({
          things: [draft, ...state.things],
          activities: capActivities([
            {
              id: nid("ev"),
              at: now,
              type: "captured",
              thingId: draft.id,
              title: draft.title,
            },
            ...state.activities,
          ]),
        });
        return { thing: draft };
      },

      updateThing: (id, patch) => {
        const now = Date.now();
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id ? { ...t, ...patch, updatedAt: now } : t,
          ),
        }));
      },

      setStatus: (id, status) => {
        const now = Date.now();
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id
              ? {
                  ...t,
                  status,
                  updatedAt: now,
                  completedAt: status === "completed" ? now : t.completedAt,
                  archivedAt: status === "archived" ? now : t.archivedAt,
                }
              : t,
          ),
        }));
      },

      complete: (id) => {
        get().completeMany([id]);
      },

      completeMany: (ids) => {
        if (ids.length === 0) return;
        const now = Date.now();
        const setIds = new Set(ids);
        set((s) => ({
          things: s.things.map((t) =>
            setIds.has(t.id)
              ? {
                  ...t,
                  status: "completed" as const,
                  completedAt: now,
                  updatedAt: now,
                  resurfaceAt: undefined,
                }
              : t,
          ),
          activities: capActivities([
            ...ids.map((id) => ({
              id: nid("ev"),
              at: now,
              type: "completed" as const,
              thingId: id,
              title: s.things.find((t) => t.id === id)?.title,
            })),
            ...s.activities,
          ]),
        }));
      },

      snooze: (id, until) => {
        get().snoozeMany([id], until);
      },

      snoozeMany: (ids, until) => {
        if (ids.length === 0) return;
        const now = Date.now();
        const setIds = new Set(ids);
        set((s) => ({
          things: s.things.map((t) =>
            setIds.has(t.id)
              ? {
                  ...t,
                  status: t.status === "inbox" ? "active" : t.status,
                  resurfaceAt: until,
                  reasonForResurface: "Snoozed",
                  updatedAt: now,
                }
              : t,
          ),
          activities: capActivities([
            ...ids.map((id) => ({
              id: nid("ev"),
              at: now,
              type: "snoozed" as const,
              thingId: id,
              title: s.things.find((t) => t.id === id)?.title,
            })),
            ...s.activities,
          ]),
        }));
      },

      archive: (id) => {
        get().archiveMany([id]);
      },

      archiveMany: (ids) => {
        if (ids.length === 0) return;
        const now = Date.now();
        const setIds = new Set(ids);
        set((s) => ({
          things: s.things.map((t) =>
            setIds.has(t.id)
              ? {
                  ...t,
                  status: "archived" as const,
                  archivedAt: now,
                  updatedAt: now,
                  resurfaceAt: undefined,
                }
              : t,
          ),
          activities: capActivities([
            ...ids.map((id) => ({
              id: nid("ev"),
              at: now,
              type: "archived" as const,
              thingId: id,
              title: s.things.find((t) => t.id === id)?.title,
            })),
            ...s.activities,
          ]),
        }));
      },

      restore: (id) => {
        const now = Date.now();
        set((s) => ({
          things: s.things.map((t) => {
            if (t.id !== id) return t;
            const suggestion = suggestResurfaceAt(t, s.settings, new Date(now));
            return {
              ...t,
              status: "active",
              archivedAt: undefined,
              completedAt: undefined,
              resurfaceAt: suggestion.at,
              reasonForResurface: suggestion.reason,
              updatedAt: now,
            };
          }),
        }));
      },

      keep: (id) => {
        const now = Date.now();
        const until = now + 7 * 24 * 3600_000;
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id
              ? {
                  ...t,
                  lastOpenedAt: now,
                  status: t.status === "inbox" ? "active" : t.status,
                  resurfaceAt: until,
                  reasonForResurface: "Kept for later",
                  updatedAt: now,
                }
              : t,
          ),
        }));
      },

      remove: (id) => {
        const thing = get().things.find((t) => t.id === id);
        const now = Date.now();
        set((s) => ({
          things: s.things.filter((t) => t.id !== id),
          activities: capActivities([
            {
              id: nid("ev"),
              at: now,
              type: "deleted",
              thingId: id,
              title: thing?.title,
            },
            ...s.activities,
          ]),
        }));
      },

      togglePin: (id) => {
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id
              ? { ...t, isPinned: !t.isPinned, updatedAt: Date.now() }
              : t,
          ),
        }));
      },

      toggleFavourite: (id) => {
        const thing = get().things.find((t) => t.id === id);
        const now = Date.now();
        const turningOn = !thing?.isFavourite;
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id
              ? { ...t, isFavourite: !t.isFavourite, updatedAt: now }
              : t,
          ),
          activities: turningOn
            ? capActivities([
                {
                  id: nid("ev"),
                  at: now,
                  type: "favourited",
                  thingId: id,
                  title: thing?.title,
                },
                ...s.activities,
              ])
            : s.activities,
        }));
      },

      openThing: (id) => {
        const thing = get().things.find((t) => t.id === id);
        const now = Date.now();
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id
              ? {
                  ...t,
                  lastOpenedAt: now,
                  status: t.status === "inbox" ? "active" : t.status,
                  updatedAt: now,
                }
              : t,
          ),
          activities: capActivities([
            {
              id: nid("ev"),
              at: now,
              type: "opened",
              thingId: id,
              title: thing?.title,
            },
            ...s.activities,
          ]),
        }));
      },

      applyAi: (id, result, provider = "xai", model = "grok-4.5") => {
        const now = Date.now();
        set((s) => ({
          things: s.things.map((t) => {
            if (t.id !== id) return t;
            const category = result.category ?? t.category;
            const next: Thing = {
              ...t,
              title: result.title?.trim() || t.title,
              summary: result.summary ?? t.summary,
              category,
              tags: result.tags?.length ? result.tags.slice(0, 8) : t.tags,
              detectedAction: result.detectedAction ?? t.detectedAction,
              detectedDate: result.detectedDate ?? t.detectedDate,
              detectedTime: result.detectedTime ?? t.detectedTime,
              detectedLocation: result.detectedLocation ?? t.detectedLocation,
              detectedPerson: result.detectedPerson ?? t.detectedPerson,
              priority: result.importance ?? t.priority,
              aiProcessed: true,
              aiConfidence: result.confidence,
              aiProvider: provider,
              aiModel: model,
              processingStatus: "COMPLETE",
              processingError: undefined,
              estimatedReadMinutes: result.estimatedReadMinutes,
              suggestedNotificationText: result.suggestedNotificationText,
              ocrText: result.ocrText ?? t.ocrText,
              updatedAt: now,
            };
            if (result.detectedDate && !next.dueAt) {
              const iso = result.detectedDate;
              const time = result.detectedTime ?? "09:00";
              const due = Date.parse(`${iso}T${time}:00`);
              if (!Number.isNaN(due)) next.dueAt = due;
            }
            if (result.suggestedResurfaceAt) {
              const at = Date.parse(result.suggestedResurfaceAt);
              if (!Number.isNaN(at) && at > now) {
                next.resurfaceAt = at;
                next.reasonForResurface = result.reasonForResurface ?? t.reasonForResurface;
              }
            } else if (category !== t.category || next.dueAt !== t.dueAt) {
              const suggestion = suggestResurfaceAt(next, s.settings, new Date(now));
              next.resurfaceAt = suggestion.at;
              next.reasonForResurface = result.reasonForResurface ?? suggestion.reason;
            } else if (result.reasonForResurface) {
              next.reasonForResurface = result.reasonForResurface;
            }
            return next;
          }),
          activities: capActivities([
            {
              id: nid("ev"),
              at: now,
              type: "processed",
              thingId: id,
              title: result.title,
              detail: result.category,
            },
            ...s.activities,
          ]),
        }));
      },

      markProcessing: (id, status, error) => {
        set((s) => ({
          things: s.things.map((t) =>
            t.id === id
              ? {
                  ...t,
                  processingStatus: status,
                  processingError: error,
                  lastEnrichAttempt:
                    status === "PROCESSING" ? Date.now() : t.lastEnrichAttempt,
                  enrichAttempts:
                    status === "PROCESSING"
                      ? (t.enrichAttempts ?? 0) + 1
                      : t.enrichAttempts,
                  updatedAt: Date.now(),
                }
              : t,
          ),
          activities:
            status === "FAILED"
              ? capActivities([
                  {
                    id: nid("ev"),
                    at: Date.now(),
                    type: "processing_failed",
                    thingId: id,
                    title: s.things.find((x) => x.id === id)?.title,
                    detail: error,
                  },
                  ...s.activities,
                ])
              : s.activities,
        }));
      },

      logActivity: (type, thing, detail) => {
        set((s) => ({
          activities: capActivities([
            {
              id: nid("ev"),
              at: Date.now(),
              type,
              thingId: thing?.id,
              title: thing?.title,
              detail,
            },
            ...s.activities,
          ]),
        }));
      },

      patchSettings: (patch) => {
        set((s) => ({ settings: { ...s.settings, ...patch } }));
      },

      completeOnboarding: () => {
        set((s) => ({
          settings: { ...s.settings, onboardingComplete: true },
        }));
      },

      loadExamples: () => {
        const seeded = makeSeed();
        set((s) => ({
          things: [...seeded.things, ...s.things],
          activities: capActivities([...seeded.activities, ...s.activities]),
        }));
      },

      tickResurface: (nowMs) => {
        const now = nowMs ?? Date.now();
        const dayStart = new Date(now);
        dayStart.setHours(0, 0, 0, 0);
        const surfaced: Thing[] = [];
        set((s) => {
          if (!s.settings.resurfaceEnabled) return s;
          const things = s.things.map((t) => {
            if (t.status === "completed" || t.status === "archived") return t;
            const due =
              (t.resurfaceAt !== undefined && t.resurfaceAt <= now) ||
              staleUnopened(t, now);
            if (!due) return t;
            if (t.lastResurfacedAt && t.lastResurfacedAt >= dayStart.getTime()) {
              return t;
            }
            const next = {
              ...t,
              lastResurfacedAt: now,
              resurfaceCount: t.resurfaceCount + 1,
              updatedAt: now,
            };
            surfaced.push(next);
            return next;
          });
          if (surfaced.length === 0) return s;
          const events: ActivityEvent[] = surfaced.map((t) => ({
            id: nid("ev"),
            at: now,
            type: "resurfaced" as const,
            thingId: t.id,
            title: t.title,
            detail: notificationCopy(t),
          }));
          return {
            things,
            activities: capActivities([...events, ...s.activities]),
          };
        });
        return surfaced;
      },

      recordNudge: (now = new Date()) => {
        const gate = shouldNudge(get().settings, now);
        if (!gate.allowed) return false;
        const day = formatDay(now);
        set((s) => ({
          settings: {
            ...s.settings,
            nudgesOn: day,
            nudgesToday: s.settings.nudgesOn === day ? s.settings.nudgesToday + 1 : 1,
          },
        }));
        return true;
      },

      importSnapshot: (data) => {
        set((s) => ({
          things: data.things,
          activities: data.activities ?? s.activities,
          settings: { ...s.settings, ...data.settings },
        }));
      },

      resetAll: () => {
        set({
          things: [],
          activities: [],
          settings: { ...DEFAULT_SETTINGS, onboardingComplete: true },
        });
      },

      bumpAiUsage: () => {
        set((s) => ({
          settings: { ...s.settings, aiUsageCount: s.settings.aiUsageCount + 1 },
        }));
      },
    }),
    {
      name: "second-memory",
      storage: createJSONStorage(() => localStorage),
      skipHydration: true,
      partialize: (s) => ({
        things: s.things,
        activities: s.activities,
        settings: s.settings,
      }),
    },
  ),
);

export function exportSnapshot(): string {
  const { things, activities, settings } = useMemoryStore.getState();
  return JSON.stringify({ things, activities, settings, exportedAt: Date.now() }, null, 2);
}
