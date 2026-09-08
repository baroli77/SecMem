import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { parseOpenGraph } from "./parse-og.ts";

describe("parseOpenGraph", () => {
  it("reads og tags and title", () => {
    const html = `
      <html><head>
        <title>Fallback</title>
        <meta property="og:title" content="Why Procrastinators Procrastinate" />
        <meta property="og:description" content="The monkey." />
        <meta property="og:image" content="/img.png" />
        <meta property="og:site_name" content="Wait But Why" />
        <link rel="canonical" href="https://waitbutwhy.com/post" />
      </head></html>
    `;
    const meta = parseOpenGraph(html, new URL("https://waitbutwhy.com/post"));
    assert.equal(meta.title, "Why Procrastinators Procrastinate");
    assert.equal(meta.description, "The monkey.");
    assert.equal(meta.siteName, "Wait But Why");
    assert.equal(meta.image, "https://waitbutwhy.com/img.png");
    assert.equal(meta.canonicalUrl, "https://waitbutwhy.com/post");
  });
});
