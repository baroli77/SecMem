import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { nextRetryDelayMs, shouldRetryEnrichment } from "./queue.ts";
import type { Thing } from "./types.ts";

function thing(partial: Partial<Thing>): Thing {
  return {
    id: "x",
    createdAt: 1,
    updatedAt: 1,
    originalContent: "x",
    contentType: "text",
    title: "Item",
    category: "UNKNOWN",
    status: "inbox",
    priority: "normal",
    resurfaceCount: 0,
    isPinned: false,
    isFavourite: false,
    aiProcessed: false,
    processingStatus: "QUEUED",
    tags: [],
    ...partial,
  };
}

describe("enrichment retry", () => {
  it("retries queued items immediately", () => {
    assert.equal(shouldRetryEnrichment(thing({ processingStatus: "QUEUED" })), true);
  });

  it("does not retry while processing or complete", () => {
    assert.equal(
      shouldRetryEnrichment(thing({ processingStatus: "PROCESSING" })),
      false,
    );
    assert.equal(
      shouldRetryEnrichment(thing({ processingStatus: "COMPLETE" })),
      false,
    );
  });

  it("backs off failed attempts", () => {
    const now = 1_000_000;
    const failed = thing({
      processingStatus: "FAILED",
      enrichAttempts: 2,
      lastEnrichAttempt: now - 60_000,
    });
    assert.equal(shouldRetryEnrichment(failed, now), false);
    assert.equal(nextRetryDelayMs(2), 4 * 60_000);
    assert.equal(
      shouldRetryEnrichment(failed, now + 4 * 60_000),
      true,
    );
  });

  it("stops after the attempt cap", () => {
    assert.equal(
      shouldRetryEnrichment(
        thing({
          processingStatus: "FAILED",
          enrichAttempts: 4,
          lastEnrichAttempt: 0,
        }),
      ),
      false,
    );
  });
});
