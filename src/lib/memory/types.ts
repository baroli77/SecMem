export const CATEGORIES = [
  "READ",
  "WATCH",
  "BUY",
  "DO",
  "WORK",
  "PERSONAL",
  "PLACE",
  "IDEA",
  "REFERENCE",
  "EVENT",
  "RECIPE",
  "UNKNOWN",
] as const;

export type Category = (typeof CATEGORIES)[number];

export const STATUSES = ["inbox", "active", "completed", "archived"] as const;
export type ThingStatus = (typeof STATUSES)[number];

export const PROCESSING = [
  "NONE",
  "QUEUED",
  "PROCESSING",
  "COMPLETE",
  "FAILED",
] as const;
export type ProcessingStatus = (typeof PROCESSING)[number];

export type ContentType = "text" | "url" | "image" | "html";
export type Priority = "low" | "normal" | "high";
export type Appearance = "system" | "light" | "dark";

export interface Thing {
  id: string;
  createdAt: number;
  updatedAt: number;
  originalContent: string;
  contentType: ContentType;
  sourceUrl?: string;
  sourceApp?: string;
  title: string;
  summary?: string;
  notes?: string;
  imageDataUrl?: string;
  thumbnailUrl?: string;
  mimeType?: string;
  category: Category;
  status: ThingStatus;
  priority: Priority;
  dueAt?: number;
  resurfaceAt?: number;
  completedAt?: number;
  archivedAt?: number;
  lastOpenedAt?: number;
  lastResurfacedAt?: number;
  resurfaceCount: number;
  isPinned: boolean;
  isFavourite: boolean;
  aiProcessed: boolean;
  aiConfidence?: number;
  aiProvider?: string;
  aiModel?: string;
  processingStatus: ProcessingStatus;
  processingError?: string;
  enrichAttempts?: number;
  lastEnrichAttempt?: number;
  tags: string[];
  detectedAction?: string;
  detectedDate?: string;
  detectedTime?: string;
  detectedLocation?: string;
  detectedPerson?: string;
  estimatedReadMinutes?: number;
  suggestedNotificationText?: string;
  reasonForResurface?: string;
  ocrText?: string;
  siteName?: string;
}

export type ActivityType =
  | "captured"
  | "processed"
  | "processing_failed"
  | "resurfaced"
  | "opened"
  | "completed"
  | "snoozed"
  | "archived"
  | "deleted"
  | "favourited"
  | "search"
  | "edited";

export interface ActivityEvent {
  id: string;
  at: number;
  type: ActivityType;
  thingId?: string;
  title?: string;
  detail?: string;
}

export interface Settings {
  appearance: Appearance;
  aiEnabled: boolean;
  automaticProcessing: boolean;
  resurfaceEnabled: boolean;
  maxNudgesPerDay: number;
  quietHoursStart: string;
  quietHoursEnd: string;
  workHoursStart: string;
  workHoursEnd: string;
  readingTime: string;
  leisureTime: string;
  payday: number;
  notificationsEnabled: boolean;
  onboardingComplete: boolean;
  isPro: boolean;
  aiUsageCount: number;
  nudgesOn: string;
  nudgesToday: number;
}

export const FREE_ACTIVE_LIMIT = 40;
export const MAX_ENRICH_ATTEMPTS = 4;

export const DEFAULT_SETTINGS: Settings = {
  appearance: "system",
  aiEnabled: true,
  automaticProcessing: true,
  resurfaceEnabled: true,
  maxNudgesPerDay: 5,
  quietHoursStart: "22:00",
  quietHoursEnd: "07:00",
  workHoursStart: "09:00",
  workHoursEnd: "18:00",
  readingTime: "19:30",
  leisureTime: "20:30",
  payday: 28,
  notificationsEnabled: false,
  onboardingComplete: false,
  isPro: false,
  aiUsageCount: 0,
  nudgesOn: "",
  nudgesToday: 0,
};

export interface CaptureInput {
  text?: string;
  url?: string;
  imageDataUrl?: string;
  mimeType?: string;
  sourceApp?: string;
  forceDuplicate?: boolean;
}

export interface AiAnalysis {
  title?: string;
  summary?: string;
  category?: Category;
  tags?: string[];
  detectedAction?: string;
  detectedDate?: string;
  detectedTime?: string;
  detectedLocation?: string;
  detectedPerson?: string;
  importance?: Priority;
  confidence?: number;
  suggestedResurfaceAt?: string;
  reasonForResurface?: string;
  estimatedReadMinutes?: number;
  suggestedNotificationText?: string;
  ocrText?: string;
}

export interface UrlMetadata {
  title?: string;
  description?: string;
  image?: string;
  siteName?: string;
  canonicalUrl?: string;
}

export function isCategory(value: string): value is Category {
  return (CATEGORIES as readonly string[]).includes(value);
}

export function activeCount(things: Thing[]): number {
  return things.filter(
    (t) => t.status === "inbox" || t.status === "active",
  ).length;
}
