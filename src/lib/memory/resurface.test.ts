import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  isQuietHours,
  notificationCopy,
  shouldNudge,
  snoozeOptions,
  suggestResurfaceAt,
  todayBuckets,
} from "./resurface.ts";
import { DEFAULT_SETTINGS, type Thing } from "./types.ts";

const settings = { ...DEFAULT_SETTINGS };

function thing(partial: Partial<Thing>): Thing {
  return {
    id: "x",
    createdAt: Date.now(),
    updatedAt: Date.now(),
    originalContent: "x",
    contentType: "text",
    title: "Item",
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

describe("suggestResurfaceAt", () => {
  const mondayMorning = new Date("2026-09-07T08:00:00");

  it("schedules READ into the evening window", () => {
    const r = suggestResurfaceAt(thing({ category: "READ" }), settings, mondayMorning);
    const d = new Date(r.at);
    assert.equal(d.getHours(), 19);
    assert.equal(d.getMinutes(), 30);
    assert.match(r.reason, /reading/i);
  });

  it("schedules BUY around payday", () => {
    const r = suggestResurfaceAt(thing({ category: "BUY" }), settings, mondayMorning);
    assert.equal(new Date(r.at).getDate(), settings.payday);
  });

  it("schedules RECIPE on a Saturday", () => {
    const r = suggestResurfaceAt(thing({ category: "RECIPE" }), settings, mondayMorning);
    assert.equal(new Date(r.at).getDay(), 6);
  });

  it("uses the event due date when present", () => {
    const due = new Date("2026-10-14T10:40:00").getTime();
    const r = suggestResurfaceAt(
      thing({ category: "EVENT", dueAt: due }),
      settings,
      mondayMorning,
    );
    assert.ok(r.at < due);
    assert.ok(r.at > mondayMorning.getTime());
  });
});

describe("quiet hours", () => {
  it("treats 23:00 as quiet when quiet is 22–07", () => {
    assert.equal(isQuietHours(new Date("2026-09-06T23:10:00"), settings), true);
  });
  it("treats 09:00 as not quiet", () => {
    assert.equal(isQuietHours(new Date("2026-09-06T09:00:00"), settings), false);
  });
});

describe("throttling", () => {
  it("blocks when the daily cap is reached", () => {
    const gated = shouldNudge(
      { ...settings, nudgesOn: "2026-09-06", nudgesToday: 5, maxNudgesPerDay: 5 },
      new Date("2026-09-06T12:00:00"),
    );
    assert.equal(gated.allowed, false);
    assert.equal(gated.reason, "throttle");
  });
});

describe("snooze options", () => {
  it("returns four standard options", () => {
    const opts = snoozeOptions(new Date("2026-09-06T12:00:00"), settings);
    assert.deepEqual(
      opts.map((o) => o.id),
      ["1h", "tonight", "tomorrow", "weekend"],
    );
    assert.ok(opts[0]!.at > Date.parse("2026-09-06T12:00:00"));
  });
});

describe("notification copy", () => {
  it("is human, not AI-flavoured", () => {
    const copy = notificationCopy(thing({ category: "READ", title: "Essay" }));
    assert.equal(copy.includes("AI"), false);
    assert.ok(copy.length < 80);
  });
});

describe("today buckets", () => {
  it("splits due, resurfaced, and later today", () => {
    const now = Date.parse("2026-09-06T15:00:00");
    const due = thing({
      id: "due",
      dueAt: Date.parse("2026-09-06T16:00:00"),
      title: "Due",
    });
    const resurfaced = thing({
      id: "res",
      resurfaceAt: Date.parse("2026-09-06T14:00:00"),
      title: "Res",
    });
    const later = thing({
      id: "lat",
      resurfaceAt: Date.parse("2026-09-06T20:00:00"),
      title: "Later",
    });
    const buckets = todayBuckets([due, resurfaced, later], now);
    assert.equal(buckets.due[0]?.id, "due");
    assert.equal(buckets.resurfaced[0]?.id, "res");
    assert.equal(buckets.laterToday[0]?.id, "lat");
  });

  it("asks about items unopened for a month", () => {
    const now = Date.parse("2026-09-06T15:00:00");
    const stale = thing({
      id: "old",
      createdAt: now - 40 * 24 * 3600_000,
      resurfaceAt: now + 5 * 24 * 3600_000,
    });
    const buckets = todayBuckets([stale], now);
    assert.equal(buckets.stillWant[0]?.id, "old");
  });
});
