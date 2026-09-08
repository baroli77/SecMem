import { createServerFn } from "@tanstack/react-start";
import { CATEGORIES, type AiAnalysis } from "@/lib/memory/types";
import { parseAnalysis } from "./parse-analysis";

export interface AnalyzeInput {
  originalContent: string;
  sourceUrl?: string;
  title?: string;
  siteName?: string;
  pageDescription?: string;
  ocrHint?: string;
  imageDataUrl?: string;
  nowISO: string;
  timezone: string;
}

export type AnalyzeResult =
  | { ok: true; analysis: AiAnalysis; provider: string; model: string }
  | { ok: false; error: string };

export { parseAnalysis };

const SYSTEM = `You are the silent intelligence layer of Second Memory, a personal attention app.
The user saved something so they can forget it. Infer what it is and when it should come back.
Return ONLY JSON with this schema:
{
  "title": string (concise, human, no quotes around the whole title),
  "summary": string (1-2 sentences, useful, no marketing),
  "category": one of ${CATEGORIES.join("|")},
  "tags": string[] (0-6 short tags),
  "detectedAction": string | null,
  "detectedDate": string | null (YYYY-MM-DD in the user's timezone if a date is present),
  "detectedTime": string | null (HH:mm 24h if a time is present),
  "detectedLocation": string | null,
  "detectedPerson": string | null,
  "importance": "low" | "normal" | "high",
  "confidence": number (0-1),
  "suggestedResurfaceAt": string | null (ISO-8601 datetime),
  "reasonForResurface": string | null (short, plain),
  "estimatedReadMinutes": number | null,
  "suggestedNotificationText": string | null (one short line, human, never mention AI),
  "ocrText": string | null
}
Rules:
- Do not invent facts that are not in the content.
- Prefer READ for articles, WATCH for video, BUY for products, DO for tasks, EVENT for appointments, RECIPE for cooking.
- Notification copy should sound like a person, e.g. "Worth reading tonight?" or "Send Sarah the spreadsheet."
- If unsure, use UNKNOWN and lower confidence.
- Dates: resolve relative words like tomorrow using nowISO and timezone.`;

export const analyzeThingFn = createServerFn({ method: "POST" })
  .validator((input: AnalyzeInput) => input)
  .handler(async ({ data }): Promise<AnalyzeResult> => {
    const apiKey = process.env.XAI_API_KEY;
    if (!apiKey) {
      return { ok: false, error: "AI is not available" };
    }

    const userParts: unknown[] = [];
    const textBlock = [
      `Now: ${data.nowISO} (${data.timezone})`,
      data.title ? `Current title: ${data.title}` : null,
      data.sourceUrl ? `URL: ${data.sourceUrl}` : null,
      data.siteName ? `Site: ${data.siteName}` : null,
      data.pageDescription ? `Page description: ${data.pageDescription}` : null,
      data.ocrHint ? `OCR/extracted text: ${data.ocrHint}` : null,
      `Content:\n${data.originalContent.slice(0, 4000)}`,
    ]
      .filter(Boolean)
      .join("\n");

    userParts.push({ type: "text", text: textBlock });

    if (data.imageDataUrl && data.imageDataUrl.length < 280_000) {
      userParts.push({
        type: "image_url",
        image_url: { url: data.imageDataUrl },
      });
    }

    const userContent =
      data.imageDataUrl && data.imageDataUrl.length < 280_000
        ? userParts
        : textBlock;

    try {
      const res = await fetch("https://api.x.ai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${apiKey}`,
        },
        body: JSON.stringify({
          model: "grok-4.5",
          temperature: 0.2,
          max_tokens: 700,
          response_format: { type: "json_object" },
          messages: [
            { role: "system", content: SYSTEM },
            { role: "user", content: userContent },
          ],
        }),
      });

      if (!res.ok) {
        const body = await res.text().catch(() => "");
        return {
          ok: false,
          error: `xAI API error ${res.status}${body ? `: ${body.slice(0, 180)}` : ""}`,
        };
      }

      const payload = (await res.json()) as {
        choices?: { message?: { content?: string } }[];
      };
      const raw = payload.choices?.[0]?.message?.content ?? "";
      const analysis = parseAnalysis(raw);
      if (!analysis) {
        return { ok: false, error: "Malformed AI response" };
      }
      return {
        ok: true,
        analysis,
        provider: "xai",
        model: "grok-4.5",
      };
    } catch (err) {
      const message = err instanceof Error ? err.message : "Network error";
      return { ok: false, error: message };
    }
  });
