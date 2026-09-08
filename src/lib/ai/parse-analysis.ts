import {
  isCategory,
  type AiAnalysis,
  type Category,
  type Priority,
} from "../memory/types.ts";

export function parseAnalysis(raw: string): AiAnalysis | null {
  const jsonText = extractJson(raw);
  if (!jsonText) return null;
  try {
    const data = JSON.parse(jsonText) as Record<string, unknown>;
    const categoryRaw = typeof data.category === "string" ? data.category.toUpperCase() : "";
    const category: Category | undefined = isCategory(categoryRaw)
      ? categoryRaw
      : undefined;
    const importance = asPriority(data.importance);
    const tags = Array.isArray(data.tags)
      ? data.tags.filter((t): t is string => typeof t === "string").slice(0, 8)
      : undefined;
    const confidence =
      typeof data.confidence === "number"
        ? Math.min(1, Math.max(0, data.confidence))
        : undefined;
    return {
      title: asString(data.title),
      summary: asString(data.summary),
      category,
      tags,
      detectedAction: asString(data.detectedAction),
      detectedDate: asString(data.detectedDate),
      detectedTime: asString(data.detectedTime),
      detectedLocation: asString(data.detectedLocation),
      detectedPerson: asString(data.detectedPerson),
      importance,
      confidence,
      suggestedResurfaceAt: asString(data.suggestedResurfaceAt),
      reasonForResurface: asString(data.reasonForResurface),
      estimatedReadMinutes:
        typeof data.estimatedReadMinutes === "number"
          ? Math.max(1, Math.round(data.estimatedReadMinutes))
          : undefined,
      suggestedNotificationText: asString(data.suggestedNotificationText),
      ocrText: asString(data.ocrText),
    };
  } catch {
    return null;
  }
}

function extractJson(raw: string): string | null {
  const trimmed = raw.trim();
  if (trimmed.startsWith("{")) return trimmed;
  const fence = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (fence?.[1]) return fence[1].trim();
  const start = trimmed.indexOf("{");
  const end = trimmed.lastIndexOf("}");
  if (start >= 0 && end > start) return trimmed.slice(start, end + 1);
  return null;
}

function asString(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const t = value.trim();
  if (!t || t === "null") return undefined;
  return t;
}

function asPriority(value: unknown): Priority | undefined {
  if (value === "low" || value === "normal" || value === "high") return value;
  return undefined;
}
