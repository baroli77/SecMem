import { analyzeThingFn } from "@/lib/ai/analyze";
import { fetchUrlMetadataFn } from "@/lib/ai/metadata";
import { useMemoryStore } from "./store";
import type { Thing } from "./types";

export async function enrichThing(id: string): Promise<void> {
  const state = useMemoryStore.getState();
  const thing = state.things.find((t) => t.id === id);
  if (!thing) return;
  if (thing.processingStatus === "PROCESSING") return;
  if (!state.settings.aiEnabled) {
    state.markProcessing(id, "NONE");
    return;
  }

  state.markProcessing(id, "PROCESSING");

  let working: Thing = { ...thing };

  if (working.sourceUrl) {
    try {
      const meta = await fetchUrlMetadataFn({ data: { url: working.sourceUrl } });
      if (meta.ok) {
        const patch: Partial<Thing> = {};
        if (meta.metadata.title && isGenericTitle(working.title, working.sourceUrl)) {
          patch.title = meta.metadata.title;
        }
        if (meta.metadata.description && !working.summary) {
          patch.summary = meta.metadata.description.slice(0, 240);
        }
        if (meta.metadata.image) patch.thumbnailUrl = meta.metadata.image;
        if (meta.metadata.siteName) patch.siteName = meta.metadata.siteName;
        if (meta.metadata.canonicalUrl) patch.sourceUrl = meta.metadata.canonicalUrl;
        if (Object.keys(patch).length > 0) {
          state.updateThing(id, patch);
          working = { ...working, ...patch };
        }
      }
    } catch {
      // Metadata is optional; capture already persisted.
    }
  }

  try {
    const result = await analyzeThingFn({
      data: {
        originalContent: working.originalContent,
        sourceUrl: working.sourceUrl,
        title: working.title,
        siteName: working.siteName,
        pageDescription: working.summary,
        ocrHint: working.ocrText,
        imageDataUrl: working.imageDataUrl,
        nowISO: new Date().toISOString(),
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      },
    });
    if (!result.ok) {
      state.markProcessing(id, "FAILED", result.error);
      return;
    }
    state.applyAi(id, result.analysis, result.provider, result.model);
    state.bumpAiUsage();
  } catch (err) {
    const message = err instanceof Error ? err.message : "Enrichment failed";
    state.markProcessing(id, "FAILED", message);
  }
}

function isGenericTitle(title: string, url?: string): boolean {
  if (!title || title === "Untitled" || title === "Screenshot") return true;
  if (!url) return false;
  try {
    const host = new URL(url).hostname.replace(/^www\./, "");
    return title === host || title === url;
  } catch {
    return false;
  }
}

export async function compressImageFile(file: File): Promise<{
  dataUrl: string;
  mimeType: string;
}> {
  const dataUrl = await readFile(file);
  const img = await loadImage(dataUrl);
  const max = 1280;
  const scale = Math.min(1, max / Math.max(img.width, img.height));
  const w = Math.max(1, Math.round(img.width * scale));
  const h = Math.max(1, Math.round(img.height * scale));
  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d");
  if (!ctx) return { dataUrl, mimeType: file.type || "image/jpeg" };
  ctx.drawImage(img, 0, 0, w, h);
  const mime = "image/jpeg";
  let quality = 0.82;
  let out = canvas.toDataURL(mime, quality);
  while (out.length > 350_000 && quality > 0.45) {
    quality -= 0.1;
    out = canvas.toDataURL(mime, quality);
  }
  return { dataUrl: out, mimeType: mime };
}

function readFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Could not read image"));
    reader.onload = () => resolve(String(reader.result));
    reader.readAsDataURL(file);
  });
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("Could not decode image"));
    img.src = src;
  });
}
