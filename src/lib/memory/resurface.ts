import {
  addDays,
  nextSaturday,
  setHours,
  setMilliseconds,
  setMinutes,
  setSeconds,
} from "date-fns";
import type { Settings, Thing } from "./types";

export interface SnoozeOption {
  id: string;
  label: string;
  at: number;
}

export function parseHm(hm: string): { h: number; m: number } {
  const [h, m] = hm.split(":").map((x) => Number(x));
  return { h: Number.isFinite(h) ? h : 9, m: Number.isFinite(m) ? m : 0 };
}

export function atTime(base: Date, hm: string): Date {
  const { h, m } = parseHm(hm);
  return setMilliseconds(setSeconds(setMinutes(setHours(base, h), m), 0), 0);
}

export function isQuietHours(now: Date, settings: Settings): boolean {
  const cur = now.getHours() * 60 + now.getMinutes();
  const start = toMinutes(settings.quietHoursStart);
  const end = toMinutes(settings.quietHoursEnd);
  if (start === end) return false;
  if (start < end) return cur >= start && cur < end;
  return cur >= start || cur < end;
}

export function isWorkHours(now: Date, settings: Settings): boolean {
  const day = now.getDay();
  if (day === 0 || day === 6) return false;
  const cur = now.getHours() * 60 + now.getMinutes();
  const start = toMinutes(settings.workHoursStart);
  const end = toMinutes(settings.workHoursEnd);
  return cur >= start && cur < end;
}

function toMinutes(hm: string): number {
  const { h, m } = parseHm(hm);
  return h * 60 + m;
}

export function nextWeekdayMorning(from: Date, hm: string): Date {
  let d = atTime(from, hm);
  if (d.getTime() <= from.getTime()) d = addDays(d, 1);
  while (d.getDay() === 0 || d.getDay() === 6) d = addDays(d, 1);
  return d;
}

export function nextPayday(from: Date, payday: number, hm = "10:00"): Date {
  const day = Math.min(28, Math.max(1, payday));
  let candidate = atTime(
    new Date(from.getFullYear(), from.getMonth(), day),
    hm,
  );
  if (candidate.getTime() <= from.getTime()) {
    candidate = atTime(
      new Date(from.getFullYear(), from.getMonth() + 1, day),
      hm,
    );
  }
  return candidate;
}

export function nextOccurrence(from: Date, hm: string): Date {
  let d = atTime(from, hm);
  if (d.getTime() <= from.getTime() + 30 * 60_000) d = addDays(d, 1);
  return d;
}

export function suggestResurfaceAt(
  thing: Pick<
    Thing,
    | "category"
    | "dueAt"
    | "detectedDate"
    | "createdAt"
    | "isFavourite"
    | "isPinned"
    | "priority"
  >,
  settings: Settings,
  now = new Date(),
): { at: number; reason: string } {
  if (thing.dueAt) {
    const due = new Date(thing.dueAt);
    if (thing.category === "EVENT") {
      const ahead = new Date(due.getTime() - 16 * 3600_000);
      if (ahead.getTime() > now.getTime() + 20 * 60_000) {
        return { at: ahead.getTime(), reason: "Ahead of the event" };
      }
      const soon = new Date(due.getTime() - 90 * 60_000);
      return {
        at: Math.max(soon.getTime(), now.getTime() + 15 * 60_000),
        reason: "Shortly before the event",
      };
    }
    if (due.getTime() > now.getTime() + 20 * 60_000) {
      return { at: due.getTime(), reason: "On the detected date" };
    }
  }

  switch (thing.category) {
    case "READ": {
      const at = nextOccurrence(now, settings.readingTime);
      return { at: at.getTime(), reason: "Evening reading window" };
    }
    case "WATCH": {
      const leisure = nextOccurrence(now, settings.leisureTime);
      if (now.getDay() === 5 || now.getDay() === 6 || now.getDay() === 0) {
        return { at: leisure.getTime(), reason: "Leisure period" };
      }
      const sat = atTime(nextSaturday(now), settings.leisureTime);
      const pick = leisure.getDay() === 6 || leisure.getDay() === 0 ? leisure : sat;
      return { at: pick.getTime(), reason: "Evening or weekend leisure" };
    }
    case "BUY": {
      const at = nextPayday(now, settings.payday);
      return { at: at.getTime(), reason: "Around payday" };
    }
    case "RECIPE": {
      let sat = atTime(nextSaturday(now), "10:00");
      if (now.getDay() === 6 && now.getHours() < 12) sat = atTime(now, "10:00");
      return { at: sat.getTime(), reason: "Weekend meal planning" };
    }
    case "WORK": {
      const at = nextWeekdayMorning(now, settings.workHoursStart);
      return { at: at.getTime(), reason: "Next work morning" };
    }
    case "DO": {
      const at = nextWeekdayMorning(now, settings.workHoursStart);
      return { at: at.getTime(), reason: "Next useful working window" };
    }
    case "EVENT": {
      const at = nextOccurrence(now, "18:00");
      return { at: at.getTime(), reason: "Ahead of the day" };
    }
    case "PLACE": {
      return {
        at: addDays(atTime(now, settings.leisureTime), 2).getTime(),
        reason: "When there is time to go",
      };
    }
    case "IDEA":
    case "REFERENCE": {
      return {
        at: addDays(atTime(now, settings.readingTime), 3).getTime(),
        reason: "Give it a few days, then review",
      };
    }
    case "PERSONAL": {
      return {
        at: nextOccurrence(now, settings.leisureTime).getTime(),
        reason: "Personal time",
      };
    }
    default: {
      const days = thing.isPinned || thing.isFavourite ? 2 : 4;
      return {
        at: addDays(atTime(now, settings.readingTime), days).getTime(),
        reason: "A later reminder",
      };
    }
  }
}

export function snoozeOptions(now = new Date(), settings?: Settings): SnoozeOption[] {
  const reading = settings?.readingTime ?? "19:30";
  const work = settings?.workHoursStart ?? "09:00";
  const hour = addDays(now, 0);
  const inHour = new Date(now.getTime() + 60 * 60_000);
  let tonight = atTime(hour, reading);
  if (tonight.getTime() <= now.getTime() + 20 * 60_000) tonight = addDays(tonight, 1);
  const tomorrow = atTime(addDays(now, 1), work);
  let weekend = atTime(nextSaturday(now), "10:00");
  if (now.getDay() === 6 && now.getHours() < 10) weekend = atTime(now, "10:00");
  return [
    { id: "1h", label: "1 hour", at: inHour.getTime() },
    { id: "tonight", label: "Tonight", at: tonight.getTime() },
    { id: "tomorrow", label: "Tomorrow", at: tomorrow.getTime() },
    { id: "weekend", label: "Weekend", at: weekend.getTime() },
  ];
}

export function notificationCopy(thing: Thing): string {
  if (thing.suggestedNotificationText) return thing.suggestedNotificationText;
  const title = thing.title || "this";
  switch (thing.category) {
    case "READ":
      return "Worth reading tonight?";
    case "WATCH":
      return "You saved this to watch.";
    case "BUY":
      return `Still want ${truncate(title, 42)}?`;
    case "DO":
    case "WORK":
      return thing.detectedAction || title;
    case "EVENT":
      return title;
    case "RECIPE":
      return "Planning meals? You saved this recipe.";
    case "IDEA":
      return "That idea is still here.";
    default:
      return "You saved this for later.";
  }
}

export function staleUnopened(thing: Thing, now = Date.now()): boolean {
  if (thing.status === "completed" || thing.status === "archived") return false;
  if (thing.lastOpenedAt) return now - thing.lastOpenedAt > 30 * 24 * 3600_000;
  return now - thing.createdAt > 30 * 24 * 3600_000;
}

export function shouldNudge(
  settings: Settings,
  now = new Date(),
): { allowed: boolean; reason?: string } {
  if (!settings.resurfaceEnabled) return { allowed: false, reason: "disabled" };
  if (isQuietHours(now, settings)) return { allowed: false, reason: "quiet" };
  const day = formatDay(now);
  const used = settings.nudgesOn === day ? settings.nudgesToday : 0;
  if (used >= settings.maxNudgesPerDay) return { allowed: false, reason: "throttle" };
  return { allowed: true };
}

export function formatDay(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function pad(n: number): string {
  return n.toString().padStart(2, "0");
}

function truncate(s: string, n: number): string {
  if (s.length <= n) return s;
  return `${s.slice(0, n - 1).trimEnd()}…`;
}

export function todayBuckets(things: Thing[], now = Date.now()) {
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  const end = new Date(now);
  end.setHours(23, 59, 59, 999);
  const startMs = start.getTime();
  const endMs = end.getTime();

  const open = things.filter(
    (t) => t.status !== "completed" && t.status !== "archived",
  );

  const due = open
    .filter((t) => t.dueAt && t.dueAt >= startMs && t.dueAt <= endMs)
    .sort((a, b) => (a.dueAt ?? 0) - (b.dueAt ?? 0));

  const dueIds = new Set(due.map((t) => t.id));

  const resurfaced = open
    .filter(
      (t) =>
        !dueIds.has(t.id) &&
        t.resurfaceAt !== undefined &&
        t.resurfaceAt <= now,
    )
    .sort((a, b) => (a.resurfaceAt ?? 0) - (b.resurfaceAt ?? 0));

  const resurfacedIds = new Set(resurfaced.map((t) => t.id));

  const laterToday = open
    .filter(
      (t) =>
        !dueIds.has(t.id) &&
        !resurfacedIds.has(t.id) &&
        t.resurfaceAt !== undefined &&
        t.resurfaceAt > now &&
        t.resurfaceAt <= endMs,
    )
    .sort((a, b) => (a.resurfaceAt ?? 0) - (b.resurfaceAt ?? 0));

  const laterIds = new Set(laterToday.map((t) => t.id));

  const stillWant = open
    .filter(
      (t) =>
        !dueIds.has(t.id) &&
        !resurfacedIds.has(t.id) &&
        !laterIds.has(t.id) &&
        staleUnopened(t, now),
    )
    .sort((a, b) => a.createdAt - b.createdAt)
    .slice(0, 4);

  const stillIds = new Set(stillWant.map((t) => t.id));

  const suggested = open
    .filter(
      (t) =>
        !dueIds.has(t.id) &&
        !resurfacedIds.has(t.id) &&
        !laterIds.has(t.id) &&
        !stillIds.has(t.id) &&
        (t.isPinned || t.isFavourite),
    )
    .sort((a, b) => Number(b.isPinned) - Number(a.isPinned) || b.updatedAt - a.updatedAt)
    .slice(0, 4);

  return { due, resurfaced, laterToday, suggested, stillWant };
}
