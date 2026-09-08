import { addDays, addHours, subDays, subHours } from "date-fns";
import type { ActivityEvent, Thing } from "./types";

function id(prefix: string): string {
  return `${prefix}-${Math.random().toString(36).slice(2, 8)}`;
}

export function makeSeed(now = new Date()): {
  things: Thing[];
  activities: ActivityEvent[];
} {
  const n = now.getTime();
  const evening = new Date(now);
  evening.setHours(19, 30, 0, 0);
  if (evening.getTime() <= n) evening.setDate(evening.getDate() + 1);

  const things: Thing[] = [
    {
      id: id("th"),
      createdAt: subHours(now, 3).getTime(),
      updatedAt: subHours(now, 3).getTime(),
      originalContent:
        "Don't forget to send Sarah the spreadsheet tomorrow.",
      contentType: "text",
      sourceApp: "Messages",
      title: "Send Sarah the spreadsheet",
      summary: "Follow up with Sarah and send the spreadsheet tomorrow.",
      category: "DO",
      status: "inbox",
      priority: "high",
      dueAt: addDays(new Date(now.getFullYear(), now.getMonth(), now.getDate(), 9, 0), 1).getTime(),
      resurfaceAt: addHours(now, 1).getTime(),
      resurfaceCount: 0,
      isPinned: false,
      isFavourite: false,
      aiProcessed: true,
      aiConfidence: 0.92,
      aiProvider: "heuristic",
      processingStatus: "COMPLETE",
      tags: ["person", "dated"],
      detectedAction: "Send Sarah the spreadsheet",
      detectedPerson: "Sarah",
      detectedDate: isoDate(addDays(now, 1)),
      suggestedNotificationText: "Send Sarah the spreadsheet.",
      reasonForResurface: "On the detected date",
    },
    {
      id: id("th"),
      createdAt: subHours(now, 26).getTime(),
      updatedAt: subHours(now, 2).getTime(),
      originalContent:
        "https://waitbutwhy.com/2013/10/why-procrastinators-procrastinate.html",
      contentType: "url",
      sourceUrl:
        "https://waitbutwhy.com/2013/10/why-procrastinators-procrastinate.html",
      sourceApp: "Chrome",
      title: "Why Procrastinators Procrastinate",
      summary:
        "A long, vivid look at the Instant Gratification Monkey and why important work keeps losing to easy now.",
      category: "READ",
      status: "active",
      priority: "normal",
      resurfaceAt: subMinutes(n, 40),
      lastResurfacedAt: subMinutes(n, 40),
      resurfaceCount: 1,
      isPinned: false,
      isFavourite: true,
      aiProcessed: true,
      aiConfidence: 0.88,
      processingStatus: "COMPLETE",
      tags: ["article", "deep-work"],
      estimatedReadMinutes: 18,
      suggestedNotificationText: "Worth reading tonight?",
      reasonForResurface: "Evening reading window",
      siteName: "Wait But Why",
    },
    {
      id: id("th"),
      createdAt: subDays(now, 2).getTime(),
      updatedAt: subDays(now, 2).getTime(),
      originalContent: "https://www.youtube.com/watch?v=jNQXAC9IVRw",
      contentType: "url",
      sourceUrl: "https://www.youtube.com/watch?v=jNQXAC9IVRw",
      sourceApp: "YouTube",
      title: "Me at the zoo",
      summary: "The first video uploaded to YouTube — a 19-second clip at the zoo.",
      category: "WATCH",
      status: "active",
      priority: "low",
      resurfaceAt: subMinutes(n, 12),
      lastResurfacedAt: subMinutes(n, 12),
      resurfaceCount: 1,
      isPinned: false,
      isFavourite: false,
      aiProcessed: true,
      aiConfidence: 0.8,
      processingStatus: "COMPLETE",
      tags: ["video"],
      estimatedReadMinutes: 1,
      suggestedNotificationText: "You saved this to watch.",
      reasonForResurface: "Leisure period",
      siteName: "YouTube",
    },
    {
      id: id("th"),
      createdAt: subDays(now, 6).getTime(),
      updatedAt: subDays(now, 6).getTime(),
      originalContent:
        "Sony WH-1000XM7 wireless noise-cancelling headphones\nhttps://www.amazon.com/dp/B0C33XXS56",
      contentType: "url",
      sourceUrl: "https://www.amazon.com/dp/B0C33XXS56",
      sourceApp: "Amazon",
      title: "Sony WH-1000XM7 Headphones",
      summary: "Sony wireless noise-cancelling headphones. Consider on payday.",
      category: "BUY",
      status: "active",
      priority: "normal",
      resurfaceAt: paydaySoon(now).getTime(),
      resurfaceCount: 0,
      isPinned: false,
      isFavourite: false,
      aiProcessed: true,
      aiConfidence: 0.86,
      processingStatus: "COMPLETE",
      tags: ["product"],
      detectedAction: "Consider purchase",
      suggestedNotificationText: "Still want these headphones?",
      reasonForResurface: "Around payday",
      siteName: "Amazon",
    },
    {
      id: id("th"),
      createdAt: subHours(now, 8).getTime(),
      updatedAt: subHours(now, 8).getTime(),
      originalContent: "Dentist appointment\n14 October\n10:40",
      contentType: "text",
      sourceApp: "Screenshot",
      title: "Dentist, 14 October at 10:40",
      summary: "Dentist appointment on 14 October at 10:40.",
      category: "EVENT",
      status: "inbox",
      priority: "high",
      dueAt: dentistDate(now).getTime(),
      resurfaceAt: dentistDate(now).getTime() - 16 * 3600_000,
      resurfaceCount: 0,
      isPinned: true,
      isFavourite: false,
      aiProcessed: true,
      aiConfidence: 0.9,
      processingStatus: "COMPLETE",
      tags: ["dated", "health"],
      detectedDate: isoDate(dentistDate(now)),
      detectedTime: "10:40",
      suggestedNotificationText: "Dentist tomorrow at 10:40.",
      reasonForResurface: "Ahead of the event",
      ocrText: "Dentist appointment\n14 October\n10:40",
    },
    {
      id: id("th"),
      createdAt: subDays(now, 1).getTime(),
      updatedAt: subDays(now, 1).getTime(),
      originalContent:
        "Crispy chickpeas, tahini, lemon, parsley. Sheet-pan dinner for two. Ingredients: chickpeas, olive oil, tahini, garlic.",
      contentType: "text",
      sourceApp: "Notes",
      title: "Sheet-pan tahini chickpeas",
      summary: "A fast weekend dinner: roasted chickpeas with tahini and lemon.",
      category: "RECIPE",
      status: "active",
      priority: "normal",
      resurfaceAt: nextSaturdayTen(now).getTime(),
      resurfaceCount: 0,
      isPinned: false,
      isFavourite: false,
      aiProcessed: true,
      aiConfidence: 0.84,
      processingStatus: "COMPLETE",
      tags: ["cooking"],
      reasonForResurface: "Weekend meal planning",
      estimatedReadMinutes: 4,
    },
    {
      id: id("th"),
      createdAt: subDays(now, 4).getTime(),
      updatedAt: subDays(now, 3).getTime(),
      originalContent:
        "Rewrite the onboarding so capture is the first thing people feel, not a feature tour.",
      contentType: "text",
      sourceApp: "Second Memory",
      title: "Capture-first onboarding",
      summary:
        "Product note: the first session should prove that sharing something is enough.",
      category: "WORK",
      status: "completed",
      priority: "normal",
      completedAt: subDays(now, 3).getTime(),
      lastOpenedAt: subDays(now, 3).getTime(),
      resurfaceCount: 1,
      lastResurfacedAt: subDays(now, 3).getTime(),
      isPinned: false,
      isFavourite: false,
      aiProcessed: true,
      processingStatus: "COMPLETE",
      tags: ["product"],
      detectedAction: "Rewrite onboarding",
    },
    {
      id: id("th"),
      createdAt: subDays(now, 9).getTime(),
      updatedAt: subDays(now, 9).getTime(),
      originalContent:
        "https://www.theverge.com/24028211/ai-memory-personal-knowledge",
      contentType: "url",
      sourceUrl: "https://www.theverge.com/24028211/ai-memory-personal-knowledge",
      sourceApp: "Chrome",
      title: "Personal knowledge, without another graveyard",
      summary:
        "A piece on why saving is easy and remembering is the actual product.",
      category: "READ",
      status: "archived",
      priority: "low",
      archivedAt: subDays(now, 2).getTime(),
      lastOpenedAt: subDays(now, 5).getTime(),
      resurfaceCount: 2,
      isPinned: false,
      isFavourite: false,
      aiProcessed: true,
      processingStatus: "COMPLETE",
      tags: ["article"],
      siteName: "The Verge",
      estimatedReadMinutes: 8,
    },
  ];

  const activities: ActivityEvent[] = [];
  for (const t of things) {
    activities.push({
      id: id("ev"),
      at: t.createdAt,
      type: "captured",
      thingId: t.id,
      title: t.title,
    });
    if (t.aiProcessed) {
      activities.push({
        id: id("ev"),
        at: t.createdAt + 20_000,
        type: "processed",
        thingId: t.id,
        title: t.title,
        detail: t.category,
      });
    }
    if (t.lastResurfacedAt) {
      activities.push({
        id: id("ev"),
        at: t.lastResurfacedAt,
        type: "resurfaced",
        thingId: t.id,
        title: t.title,
      });
    }
    if (t.completedAt) {
      activities.push({
        id: id("ev"),
        at: t.completedAt,
        type: "completed",
        thingId: t.id,
        title: t.title,
      });
    }
    if (t.archivedAt) {
      activities.push({
        id: id("ev"),
        at: t.archivedAt,
        type: "archived",
        thingId: t.id,
        title: t.title,
      });
    }
  }

  return { things, activities: activities.sort((a, b) => b.at - a.at) };
}

function isoDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
function pad(n: number): string {
  return n.toString().padStart(2, "0");
}
function subMinutes(n: number, m: number): number {
  return n - m * 60_000;
}
function paydaySoon(now: Date): Date {
  const day = 28;
  const d = new Date(now.getFullYear(), now.getMonth(), day, 10, 0, 0, 0);
  if (d.getTime() <= now.getTime()) {
    return new Date(now.getFullYear(), now.getMonth() + 1, day, 10, 0, 0, 0);
  }
  return d;
}
function dentistDate(now: Date): Date {
  const d = new Date(now.getFullYear(), 9, 14, 10, 40, 0, 0);
  if (d.getTime() < now.getTime()) d.setFullYear(d.getFullYear() + 1);
  return d;
}
function nextSaturdayTen(now: Date): Date {
  const d = new Date(now);
  const diff = (6 - d.getDay() + 7) % 7 || 7;
  d.setDate(d.getDate() + diff);
  d.setHours(10, 0, 0, 0);
  return d;
}
