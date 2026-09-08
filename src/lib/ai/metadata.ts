import { createServerFn } from "@tanstack/react-start";
import type { UrlMetadata } from "@/lib/memory/types";
import { parseOpenGraph } from "./parse-og";

export type MetadataResult =
  | { ok: true; metadata: UrlMetadata }
  | { ok: false; error: string };

export { parseOpenGraph };

export const fetchUrlMetadataFn = createServerFn({ method: "POST" })
  .validator((input: { url: string }) => input)
  .handler(async ({ data }): Promise<MetadataResult> => {
    let url: URL;
    try {
      url = new URL(data.url);
    } catch {
      return { ok: false, error: "Invalid URL" };
    }
    if (url.protocol !== "http:" && url.protocol !== "https:") {
      return { ok: false, error: "Unsupported protocol" };
    }

    if (/(?:youtube\.com|youtu\.be)/i.test(url.hostname)) {
      const oembed = await fetchJson(
        `https://www.youtube.com/oembed?url=${encodeURIComponent(url.toString())}&format=json`,
        7000,
      );
      if (oembed && typeof oembed.title === "string") {
        return {
          ok: true,
          metadata: {
            title: oembed.title,
            description:
              typeof oembed.author_name === "string"
                ? oembed.author_name
                : undefined,
            image: typeof oembed.thumbnail_url === "string" ? oembed.thumbnail_url : undefined,
            siteName: "YouTube",
            canonicalUrl: url.toString(),
          },
        };
      }
    }

    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 8000);
      const res = await fetch(url.toString(), {
        signal: controller.signal,
        redirect: "follow",
        headers: {
          "User-Agent":
            "SecondMemory/1.0 (metadata; +https://secondmemory.app)",
          Accept: "text/html,application/xhtml+xml",
        },
      });
      clearTimeout(timer);
      if (!res.ok) {
        return { ok: false, error: `Fetch failed (${res.status})` };
      }
      const contentType = res.headers.get("content-type") ?? "";
      if (!contentType.includes("html") && !contentType.includes("xml")) {
        return {
          ok: true,
          metadata: {
            title: url.hostname.replace(/^www\./, ""),
            canonicalUrl: res.url || url.toString(),
            siteName: url.hostname.replace(/^www\./, ""),
          },
        };
      }
      const html = (await res.text()).slice(0, 180_000);
      const metadata = parseOpenGraph(html, new URL(res.url || url.toString()));
      return { ok: true, metadata };
    } catch (err) {
      const message = err instanceof Error ? err.message : "Network error";
      return { ok: false, error: message };
    }
  });

async function fetchJson(
  url: string,
  timeoutMs: number,
): Promise<Record<string, unknown> | null> {
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timer);
    if (!res.ok) return null;
    return (await res.json()) as Record<string, unknown>;
  } catch {
    return null;
  }
}
