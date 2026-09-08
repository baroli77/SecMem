import type { UrlMetadata } from "../memory/types.ts";

const ATTR = "property|name|itemprop";

export function parseOpenGraph(html: string, pageUrl: URL): UrlMetadata {
  const get = (keys: string[]): string | undefined => {
    for (const key of keys) {
      const re = new RegExp(
        `<meta[^>]+(?:${ATTR})=["']${escapeRe(key)}["'][^>]*content=["']([^"']+)["'][^>]*>`,
        "i",
      );
      const re2 = new RegExp(
        `<meta[^>]+content=["']([^"']+)["'][^>]*(?:${ATTR})=["']${escapeRe(key)}["'][^>]*>`,
        "i",
      );
      const m = html.match(re) || html.match(re2);
      if (m?.[1]) return decode(m[1]);
    }
    return undefined;
  };

  const titleTag = html.match(/<title[^>]*>([^<]+)<\/title>/i)?.[1];
  const canonical = html.match(
    /<link[^>]+rel=["']canonical["'][^>]+href=["']([^"']+)["']/i,
  )?.[1];

  const image = get(["og:image", "twitter:image", "og:image:url"]);
  return {
    title: get(["og:title", "twitter:title"]) || (titleTag ? decode(titleTag) : undefined),
    description: get(["og:description", "description", "twitter:description"]),
    image: image ? absolutize(image, pageUrl) : undefined,
    siteName: get(["og:site_name"]) || pageUrl.hostname.replace(/^www\./, ""),
    canonicalUrl: canonical ? absolutize(decode(canonical), pageUrl) : pageUrl.toString(),
  };
}

function escapeRe(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function decode(s: string): string {
  return s
    .replace(/&/g, "&")
    .replace(/</g, "<")
    .replace(/>/g, ">")
    .replace(/"/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, " ")
    .trim();
}

function absolutize(maybe: string, base: URL): string {
  try {
    return new URL(maybe, base).toString();
  } catch {
    return maybe;
  }
}
