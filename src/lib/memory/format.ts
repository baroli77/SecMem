import {
  differenceInCalendarDays,
  format,
  formatDistanceToNowStrict,
  isThisWeek,
  isToday,
  isTomorrow,
  isYesterday,
} from "date-fns";
import type { Category, Thing } from "./types";

export const CATEGORY_LABEL: Record<Category, string> = {
  READ: "Read",
  WATCH: "Watch",
  BUY: "Buy",
  DO: "Do",
  WORK: "Work",
  PERSONAL: "Personal",
  PLACE: "Place",
  IDEA: "Idea",
  REFERENCE: "Reference",
  EVENT: "Event",
  RECIPE: "Recipe",
  UNKNOWN: "Saved",
};

export function relativeAge(ts: number, now = Date.now()): string {
  const date = new Date(ts);
  const mins = Math.round((now - ts) / 60_000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  if (isToday(date)) return format(date, "HH:mm");
  if (isYesterday(date)) return "yesterday";
  if (isThisWeek(date, { weekStartsOn: 1 })) return format(date, "EEE");
  const days = differenceInCalendarDays(now, date);
  if (days < 30) return `${days}d ago`;
  return format(date, "d MMM");
}

export function relativeFuture(ts: number, now = Date.now()): string {
  const date = new Date(ts);
  if (ts <= now) return "now";
  if (isToday(date)) return `today ${format(date, "HH:mm")}`;
  if (isTomorrow(date)) return `tomorrow ${format(date, "HH:mm")}`;
  return format(date, "EEE d MMM, HH:mm");
}

export function compactFuture(ts: number, now = Date.now()): string {
  const date = new Date(ts);
  if (ts <= now + 5 * 60_000) return "now";
  if (isToday(date)) return format(date, "HH:mm");
  if (isTomorrow(date)) return "tomorrow";
  return format(date, "EEE d MMM");
}

export function formatWhen(ts: number): string {
  return format(new Date(ts), "d MMM yyyy, HH:mm");
}

export function hostFromUrl(url?: string): string | undefined {
  if (!url) return undefined;
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return undefined;
  }
}

export function snoozeLabel(until: number, now = Date.now()): string {
  return `Until ${relativeFuture(until, now)}`;
}

export function distanceShort(ts: number, now = Date.now()): string {
  return formatDistanceToNowStrict(ts, { addSuffix: true })
    .replace(" seconds", "s")
    .replace(" second", "s")
    .replace(" minutes", "m")
    .replace(" minute", "m")
    .replace(" hours", "h")
    .replace(" hour", "h")
    .replace(" days", "d")
    .replace(" day", "d");
}

export function thingActionVerb(thing: Thing): string {
  switch (thing.category) {
    case "READ":
      return "Read";
    case "WATCH":
      return "Watch";
    case "BUY":
      return "Bought";
    case "DO":
    case "WORK":
    case "EVENT":
      return "Done";
    case "RECIPE":
      return "Cooked";
    default:
      return "Done";
  }
}
