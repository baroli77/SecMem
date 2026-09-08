import type { CaptureInput, Category, ContentType, Thing } from "./types";

const URL_RE = /https?:\/\/[^\s<>"')\]]+/i;

const YOUTUBE_RE = /(?:youtube\.com|youtu\.be)/i;
const AMAZON_RE = /(?:amazon\.|amzn\.|amznto\.)/i;
const REDDIT_RE = /(?:reddit\.com|redd\.it)/i;
const RECIPE_HOST_RE =
  /(?:allrecipes|seriouseats|nytcooking|bbcgoodfood|simplyrecipes|bonappetit|epicurious)/i;
const NEWS_RE =
  /(?:nytimes|bbc\.|theguardian|washingtonpost|theverge|arstechnica|wired\.|medium\.|substack|waitbutwhy|newyorker|economist)/i;
const MAPS_RE = /(?:maps\.google|google\.com\/maps|openstreetmap|what3words)/i;
const SHOP_RE = /(?:ebay\.|etsy\.|ikea\.|apple\.com\/.*shop|store\.)/i;

const TASK_RE =
  /\b(don't forget|do not forget|remind me|remember to|need to|have to|must |todo|to-do|follow up|send .+ the|call |email |pay |book |schedule )\b/i;
const RECIPE_TEXT_RE =
  /\b(ingredients|preheat|tbsp|tablespoon|bake for|serves \d|recipe)\b/i;
const EVENT_RE =
  /\b(appointment|meeting|dentist|doctor|flight|reservation|booking|interview|birthday|wedding)\b/i;
const WATCH_TEXT_RE = /\b(watch this|youtube|video essay|trailer)\b/i;
const BUY_TEXT_RE = /\b(buy|purchase|wishlist|add to cart|on sale)\b/i;
const IDEA_RE = /\b(idea:|what if|maybe we should|note to self)\b/i;

const WEEKDAYS = [
  "sunday",
  "monday",
  "tuesday",
  "wednesday",
  "thursday",
  "friday",
  "saturday",
] as const;

export interface ParsedCapture {
  originalContent: string;
  contentType: ContentType;
  sourceUrl?: string;
  title: string;
  summary?: string;
  category: Category;
  tags: string[];
  detectedDate?: string;
  detectedTime?: string;
  detectedPerson?: string;
  dueAt?: number;
  imageDataUrl?: string;
  mimeType?: string;
  sourceApp?: string;
}

export function extractFirstUrl(text: string): string | undefined {
  const match = text.match(URL_RE);
  if (!match) return undefined;
  return match[0].replace(/[.,;:]+$/, "");
}

export function parseCaptureInput(input: CaptureInput): ParsedCapture {
  const rawText = (input.text ?? "").trim();
  const explicitUrl = input.url?.trim() || extractFirstUrl(rawText);
  const hasImage = Boolean(input.imageDataUrl);

  let contentType: ContentType = "text";
  if (hasImage) contentType = "image";
  else if (explicitUrl && (!rawText || rawText === explicitUrl)) contentType = "url";
  else if (explicitUrl) contentType = "url";
  else if (/<\/?[a-z][\s\S]*>/i.test(rawText)) contentType = "html";

  const originalContent =
    rawText || explicitUrl || (hasImage ? "Image captured" : "");

  const extracted = extractSignals(originalContent, explicitUrl);
  const title = inferTitle(originalContent, explicitUrl, hasImage);

  return {
    originalContent,
    contentType,
    sourceUrl: explicitUrl,
    title,
    summary: extracted.summary,
    category: extracted.category,
    tags: extracted.tags,
    detectedDate: extracted.detectedDate,
    detectedTime: extracted.detectedTime,
    detectedPerson: extracted.detectedPerson,
    dueAt: extracted.dueAt,
    imageDataUrl: input.imageDataUrl,
    mimeType: input.mimeType,
    sourceApp: input.sourceApp,
  };
}

export function inferTitle(
  content: string,
  url?: string,
  isImage = false,
): string {
  const firstLine = content
    .split(/\n+/)
    .map((l) => l.trim())
    .find((l) => l && l !== url);

  if (firstLine && firstLine.length > 0 && firstLine !== url) {
    const cleaned = firstLine.replace(URL_RE, "").trim();
    if (cleaned.length >= 3) return truncate(cleaned, 90);
  }

  if (url) {
    try {
      const u = new URL(url);
      const host = u.hostname.replace(/^www\./, "");
      const last = u.pathname
        .split("/")
        .filter(Boolean)
        .pop()
        ?.replace(/[-_]/g, " ")
        .replace(/\.\w+$/, "");
      if (last && last.length > 2 && !/^[a-z0-9]{8,}$/i.test(last)) {
        return truncate(titleCase(last), 90);
      }
      return host;
    } catch {
      return truncate(url, 90);
    }
  }

  if (isImage) return "Screenshot";
  return truncate(content, 90) || "Untitled";
}

export interface ExtractedSignals {
  category: Category;
  tags: string[];
  summary?: string;
  detectedDate?: string;
  detectedTime?: string;
  detectedPerson?: string;
  dueAt?: number;
}

export function extractSignals(
  content: string,
  url?: string,
  now = new Date(),
): ExtractedSignals {
  const text = content.toLowerCase();
  let category: Category = "UNKNOWN";
  const tags: string[] = [];

  if (url && YOUTUBE_RE.test(url)) {
    category = "WATCH";
    tags.push("video");
  } else if (url && (AMAZON_RE.test(url) || SHOP_RE.test(url))) {
    category = "BUY";
    tags.push("product");
  } else if (url && REDDIT_RE.test(url)) {
    category = "READ";
    tags.push("reddit");
  } else if (url && RECIPE_HOST_RE.test(url)) {
    category = "RECIPE";
    tags.push("cooking");
  } else if (url && MAPS_RE.test(url)) {
    category = "PLACE";
  } else if (url && NEWS_RE.test(url)) {
    category = "READ";
    tags.push("article");
  } else if (RECIPE_TEXT_RE.test(text)) {
    category = "RECIPE";
  } else if (EVENT_RE.test(text) && hasDateCue(text)) {
    category = "EVENT";
  } else if (TASK_RE.test(text)) {
    category = "DO";
  } else if (WATCH_TEXT_RE.test(text)) {
    category = "WATCH";
  } else if (BUY_TEXT_RE.test(text)) {
    category = "BUY";
  } else if (IDEA_RE.test(text)) {
    category = "IDEA";
  } else if (url) {
    category = "READ";
    tags.push("link");
  } else if (EVENT_RE.test(text)) {
    category = "EVENT";
  }

  if (/\b(work|slack|jira|standup|office)\b/i.test(content) && category === "DO") {
    category = "WORK";
  }

  const when = extractDateTime(content, now);
  const person = extractPerson(content);

  if (when.isoDate) tags.push("dated");
  if (person) tags.push("person");

  const firstSentence = content
    .replace(URL_RE, "")
    .split(/(?<=[.!?])\s+/)
    .map((s) => s.trim())
    .find((s) => s.length > 20);

  return {
    category,
    tags: unique(tags).slice(0, 6),
    summary: firstSentence ? truncate(firstSentence, 180) : undefined,
    detectedDate: when.isoDate,
    detectedTime: when.time,
    detectedPerson: person,
    dueAt: when.dueAt,
  };
}

export function extractDateTime(
  content: string,
  now = new Date(),
): { isoDate?: string; time?: string; dueAt?: number } {
  const text = content.toLowerCase();
  let date = new Date(now);
  let foundDate = false;
  let time: string | undefined;
  let hour: number | undefined;
  let minute = 0;

  const timeMatch = content.match(
    /\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b/i,
  );
  const explicitTime = content.match(/\b([01]?\d|2[0-3]):([0-5]\d)\b/);
  if (explicitTime) {
    hour = Number(explicitTime[1]);
    minute = Number(explicitTime[2]);
    time = `${pad(hour)}:${pad(minute)}`;
  } else if (timeMatch && timeMatch[3]) {
    hour = Number(timeMatch[1]);
    minute = Number(timeMatch[2] ?? 0);
    const mer = timeMatch[3].toLowerCase();
    if (mer === "pm" && hour < 12) hour += 12;
    if (mer === "am" && hour === 12) hour = 0;
    time = `${pad(hour)}:${pad(minute)}`;
  }

  if (/\btoday\b/.test(text) || /\btonight\b/.test(text)) {
    foundDate = true;
    date = new Date(now);
  } else if (/\btomorrow\b/.test(text)) {
    foundDate = true;
    date = addDays(now, 1);
  } else if (/\bthis weekend\b/.test(text)) {
    foundDate = true;
    date = nextWeekday(now, 6);
  } else if (/\bnext week\b/.test(text)) {
    foundDate = true;
    date = addDays(now, 7);
  } else {
    for (let i = 0; i < WEEKDAYS.length; i += 1) {
      const name = WEEKDAYS[i];
      const re = new RegExp(`\\b(next\\s+)?${name}\\b`);
      if (re.test(text)) {
        foundDate = true;
        date = nextWeekday(now, i);
        if (/next\s+/.test(text) && date.getDay() === now.getDay()) {
          date = addDays(date, 7);
        }
        break;
      }
    }
  }

  const iso = content.match(/\b(20\d{2}-\d{2}-\d{2})\b/);
  if (iso) {
    foundDate = true;
    date = new Date(`${iso[1]}T00:00:00`);
  }

  const dayMonth = content.match(
    /\b(\d{1,2})(?:st|nd|rd|th)?\s+(january|february|march|april|may|june|july|august|september|october|november|december)\b/i,
  );
  const monthDay = content.match(
    /\b(january|february|march|april|may|june|july|august|september|october|november|december)\s+(\d{1,2})(?:st|nd|rd|th)?\b/i,
  );
  if (dayMonth || monthDay) {
    const months = [
      "january",
      "february",
      "march",
      "april",
      "may",
      "june",
      "july",
      "august",
      "september",
      "october",
      "november",
      "december",
    ];
    const monthName = (dayMonth?.[2] ?? monthDay?.[1] ?? "").toLowerCase();
    const dayNum = Number(dayMonth?.[1] ?? monthDay?.[2]);
    const mi = months.indexOf(monthName);
    if (mi >= 0 && dayNum >= 1 && dayNum <= 31) {
      foundDate = true;
      date = new Date(now.getFullYear(), mi, dayNum);
      if (date.getTime() < now.getTime() - 12 * 3600_000) {
        date = new Date(now.getFullYear() + 1, mi, dayNum);
      }
    }
  }

  if (!foundDate && !time) return {};

  if (hour !== undefined) {
    date.setHours(hour, minute, 0, 0);
  } else if (/\btonight\b/.test(text)) {
    date.setHours(20, 0, 0, 0);
  } else if (foundDate) {
    date.setHours(9, 0, 0, 0);
  }

  const isoDate = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  return {
    isoDate: foundDate ? isoDate : undefined,
    time,
    dueAt: foundDate || time ? date.getTime() : undefined,
  };
}

export function extractPerson(content: string): string | undefined {
  const match = content.match(
    /\b(?:send|tell|call|email|meet|ask|ping|text|message)\s+([A-Z][a-z]+)\b/,
  );
  if (match?.[1] && !["This", "The", "A", "An", "My"].includes(match[1])) {
    return match[1];
  }
  return undefined;
}

export function findDuplicate(
  things: Thing[],
  sourceUrl?: string,
): Thing | undefined {
  if (!sourceUrl) return undefined;
  const normalized = normalizeUrl(sourceUrl);
  return things
    .filter(
      (t) =>
        t.sourceUrl &&
        normalizeUrl(t.sourceUrl) === normalized &&
        t.status !== "archived",
    )
    .sort((a, b) => b.createdAt - a.createdAt)[0];
}

export function normalizeUrl(url: string): string {
  try {
    const u = new URL(url);
    u.hash = "";
    u.hostname = u.hostname.replace(/^www\./, "").toLowerCase();
    if (u.pathname.endsWith("/") && u.pathname.length > 1) {
      u.pathname = u.pathname.slice(0, -1);
    }
    u.searchParams.delete("utm_source");
    u.searchParams.delete("utm_medium");
    u.searchParams.delete("utm_campaign");
    u.searchParams.delete("utm_content");
    u.searchParams.delete("utm_term");
    return u.toString();
  } catch {
    return url.trim().toLowerCase();
  }
}

function hasDateCue(text: string): boolean {
  return /\b(today|tomorrow|tonight|monday|tuesday|wednesday|thursday|friday|saturday|sunday|january|february|march|april|may|june|july|august|september|october|november|december|\d{1,2}:\d{2}|20\d{2}-\d{2}-\d{2})\b/.test(
    text,
  );
}

function addDays(from: Date, n: number): Date {
  const d = new Date(from);
  d.setDate(d.getDate() + n);
  return d;
}

function nextWeekday(from: Date, weekday: number): Date {
  const d = new Date(from);
  const diff = (weekday - d.getDay() + 7) % 7;
  d.setDate(d.getDate() + (diff === 0 ? 7 : diff));
  return d;
}

function pad(n: number): string {
  return n.toString().padStart(2, "0");
}

function titleCase(s: string): string {
  return s
    .split(/\s+/)
    .map((w) => (w ? w[0].toUpperCase() + w.slice(1).toLowerCase() : w))
    .join(" ");
}

function truncate(s: string, n: number): string {
  const t = s.replace(/\s+/g, " ").trim();
  if (t.length <= n) return t;
  return `${t.slice(0, n - 1).trimEnd()}…`;
}

function unique(items: string[]): string[] {
  return [...new Set(items)];
}
