import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  extractDateTime,
  extractFirstUrl,
  extractPerson,
  extractSignals,
  findDuplicate,
  inferTitle,
  normalizeUrl,
  parseCaptureInput,
} from "./heuristics.ts";
import type { Thing } from "./types.ts";

describe("parseCaptureInput", () => {
  it("extracts a URL and classifies YouTube as WATCH", () => {
    const parsed = parseCaptureInput({
      text: "https://www.youtube.com/watch?v=jNQXAC9IVRw",
    });
    assert.equal(parsed.category, "WATCH");
    assert.equal(parsed.contentType, "url");
    assert.ok(parsed.sourceUrl?.includes("youtube.com"));
  });

  it("classifies Amazon as BUY", () => {
    const parsed = parseCaptureInput({
      text: "https://www.amazon.com/dp/B09XS7J49B",
    });
    assert.equal(parsed.category, "BUY");
  });

  it("classifies a reminder as DO and finds Sarah", () => {
    const parsed = parseCaptureInput({
      text: "Don't forget to send Sarah the spreadsheet tomorrow.",
    });
    assert.equal(parsed.category, "DO");
    assert.equal(parsed.detectedPerson, "Sarah");
    assert.ok(parsed.detectedDate);
    assert.ok(parsed.dueAt);
  });

  it("classifies appointment text as EVENT when a date is present", () => {
    const parsed = parseCaptureInput({
      text: "Dentist appointment 14 October 10:40",
    });
    assert.equal(parsed.category, "EVENT");
    assert.equal(parsed.detectedTime, "10:40");
  });
});

describe("extractDateTime", () => {
  const now = new Date("2026-09-06T10:00:00");

  it("resolves tomorrow", () => {
    const r = extractDateTime("call them tomorrow", now);
    assert.equal(r.isoDate, "2026-09-07");
  });

  it("resolves an explicit date", () => {
    const r = extractDateTime("Dentist 14 October 10:40", now);
    assert.equal(r.isoDate, "2026-10-14");
    assert.equal(r.time, "10:40");
  });
});

describe("extractPerson", () => {
  it("finds a capitalized name after send", () => {
    assert.equal(
      extractPerson("Don't forget to send Sarah the spreadsheet"),
      "Sarah",
    );
  });
});

describe("duplicate URLs", () => {
  it("matches canonicalised URLs", () => {
    const a = normalizeUrl("https://www.Example.com/path/?utm_source=x");
    const b = normalizeUrl("https://example.com/path");
    assert.equal(a, b);
  });

  it("finds an existing thing", () => {
    const thing = {
      id: "1",
      sourceUrl: "https://example.com/a",
      status: "active",
      createdAt: 1,
    } as Thing;
    const found = findDuplicate([thing], "https://www.example.com/a/");
    assert.equal(found?.id, "1");
  });
});

describe("inferTitle", () => {
  it("uses the first line when present", () => {
    assert.equal(inferTitle("Sony headphones\nhttps://amazon.com/x"), "Sony headphones");
  });
});

describe("extractSignals", () => {
  it("marks news hosts as READ", () => {
    const s = extractSignals(
      "https://www.theverge.com/2024/ai",
      "https://www.theverge.com/2024/ai",
    );
    assert.equal(s.category, "READ");
  });
});

describe("extractFirstUrl", () => {
  it("strips trailing punctuation", () => {
    assert.equal(
      extractFirstUrl("see https://example.com/a."),
      "https://example.com/a",
    );
  });
});
