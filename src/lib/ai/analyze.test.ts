import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { parseAnalysis } from "./parse-analysis.ts";

describe("parseAnalysis", () => {
  it("accepts a clean JSON object", () => {
    const parsed = parseAnalysis(
      JSON.stringify({
        title: "Sony WH-1000XM7 Headphones",
        summary: "Wireless noise-cancelling headphones.",
        category: "BUY",
        tags: ["product"],
        confidence: 0.9,
        suggestedNotificationText: "Still want these headphones?",
      }),
    );
    assert.ok(parsed);
    assert.equal(parsed?.category, "BUY");
    assert.equal(parsed?.title, "Sony WH-1000XM7 Headphones");
  });

  it("rejects unknown categories instead of crashing", () => {
    const parsed = parseAnalysis(
      JSON.stringify({ title: "X", category: "BANANA", confidence: 2 }),
    );
    assert.ok(parsed);
    assert.equal(parsed?.category, undefined);
    assert.equal(parsed?.confidence, 1);
  });

  it("pulls JSON out of a fenced block", () => {
    const parsed = parseAnalysis("```json\n{\"title\":\"Hi\",\"category\":\"DO\"}\n```");
    assert.equal(parsed?.title, "Hi");
    assert.equal(parsed?.category, "DO");
  });

  it("returns null on garbage", () => {
    assert.equal(parseAnalysis("not json at all"), null);
  });
});
