import { createFileRoute, Link } from "@tanstack/react-router";
import { isThisWeek, isToday, startOfDay, startOfWeek } from "date-fns";
import { EmptyState } from "@/components/empty-state";
import { relativeAge } from "@/lib/memory/format";
import { useMemoryStore } from "@/lib/memory/store";
import type { ActivityEvent } from "@/lib/memory/types";

export const Route = createFileRoute("/activity")({
  component: ActivityPage,
});

const LABELS: Record<ActivityEvent["type"], string> = {
  captured: "Captured",
  processed: "Understood",
  processing_failed: "Could not enrich",
  resurfaced: "Brought back",
  opened: "Opened",
  completed: "Done",
  snoozed: "Later",
  archived: "Archived",
  deleted: "Deleted",
  favourited: "Favourited",
  search: "Searched",
  edited: "Edited",
};

function ActivityPage() {
  const activities = useMemoryStore((s) => s.activities);
  const things = useMemoryStore((s) => s.things);
  const now = new Date();
  const weekStart = startOfWeek(now, { weekStartsOn: 1 }).getTime();
  const todayStart = startOfDay(now).getTime();

  const todayEvents = activities.filter((e) => e.at >= todayStart);
  const weekEvents = activities.filter((e) => e.at >= weekStart);

  const count = (list: ActivityEvent[], type: ActivityEvent["type"]) =>
    list.filter((e) => e.type === type).length;

  const weekCompletedIds = new Set(
    weekEvents.filter((e) => e.type === "completed" && e.thingId).map((e) => e.thingId),
  );
  const weekRead = things.filter(
    (t) => weekCompletedIds.has(t.id) && t.category === "READ",
  ).length;
  const weekBought = things.filter(
    (t) => weekCompletedIds.has(t.id) && t.category === "BUY",
  ).length;

  return (
    <div>
      <header className="mb-8">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
          Activity
        </p>
        <h1 className="mt-1 font-display text-4xl font-medium tracking-tight">
          What happened
        </h1>
      </header>

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Stat label="Today captured" value={count(todayEvents, "captured")} />
        <Stat label="Today done" value={count(todayEvents, "completed")} />
        <Stat label="Today resurfaced" value={count(todayEvents, "resurfaced")} />
        <Stat label="Week done" value={count(weekEvents, "completed")} />
      </div>
      <p className="mb-10 text-sm text-muted-foreground">
        This week: {count(weekEvents, "captured")} captured
        {weekRead ? ` · ${weekRead} articles read` : ""}
        {weekBought ? ` · ${weekBought} purchases reviewed` : ""}
        {count(weekEvents, "archived")
          ? ` · ${count(weekEvents, "archived")} archived`
          : ""}
        .
      </p>

      {activities.length === 0 ? (
        <EmptyState
          title="No history yet."
          body="Captures, completions, and resurfacing will appear here."
        />
      ) : (
        <ol className="flex flex-col">
          {activities.slice(0, 80).map((event, i) => {
            const showDay =
              i === 0 ||
              startOfDay(event.at).getTime() !==
                startOfDay(activities[i - 1]!.at).getTime();
            return (
              <li key={event.id}>
                {showDay ? (
                  <p className="mb-2 mt-6 text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground first:mt-0">
                    {isToday(event.at)
                      ? "Today"
                      : isThisWeek(event.at, { weekStartsOn: 1 })
                        ? relativeAge(event.at)
                        : relativeAge(event.at)}
                  </p>
                ) : null}
                <div className="flex items-baseline justify-between gap-3 border-b border-border py-3">
                  <div className="min-w-0">
                    <p className="text-sm text-foreground">
                      <span className="text-muted-foreground">
                        {LABELS[event.type]}
                      </span>
                      {event.title ? (
                        event.thingId ? (
                          <>
                            {" · "}
                            <Link
                              to="/thing/$id"
                              params={{ id: event.thingId }}
                              className="hover:underline"
                            >
                              {event.title}
                            </Link>
                          </>
                        ) : (
                          <> · {event.title}</>
                        )
                      ) : null}
                    </p>
                    {event.detail ? (
                      <p className="text-xs text-muted-foreground">{event.detail}</p>
                    ) : null}
                  </div>
                  <time className="shrink-0 tabular-nums text-xs text-muted-foreground">
                    {relativeAge(event.at)}
                  </time>
                </div>
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl bg-card p-4 shadow-(--shadow-border)">
      <p className="tabular-nums font-display text-3xl font-medium tracking-tight">
        {value}
      </p>
      <p className="mt-1 text-xs text-muted-foreground">{label}</p>
    </div>
  );
}
