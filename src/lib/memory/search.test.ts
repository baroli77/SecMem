import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { searchThings } from "./search.ts";
import type { Thing } from "./types.ts";

function thing(partial: Partial<Thing>): Thing {
  return {
    id: partial.id ?? "1",
    createdAt: 1,
    updatedAt: 2,
    originalContent: "",
    contentType: "text",
    title: "Untitled",
    category: "UNKNOWN",
    status: "active",
    priority: "normal",
    resurfaceCount: 0,
    isPinned: false,
    isFavourite: false,
    aiProcessed: false,
    processingStatus: "COMPLETE",
    tags: [],
    ...partial,
  };
}

describe("searchThings", () => {
  const corpus = [
    thing({
      id: "a",
      title: "Sony WH-1000XM7 Headphones",
      tags: ["product"],
      category: "BUY",
    }),
    thing({
      id: "b",
      title: "Send Sarah the spreadsheet",
      originalContent: "Don't forget Sarah",
      category: "DO",
    }),
    thing({
      id: "c",
      title: "Sheet-pan chickpeas",
      ocrText: "tahini lemon parsley",
      category: "RECIPE",
    }),
  ];

  it("finds by title", () => {
    assert.equal(searchThings(corpus, "headphones")[0]?.id, "a");
  });

  it("finds OCR text", () => {
    assert.equal(searchThings(corpus, "tahini")[0]?.id, "c");
  });

  it("requires every term", () => {
    assert.equal(searchThings(corpus, "sarah missingno").length, 0);
  });
});
