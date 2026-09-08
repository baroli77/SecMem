import { createFileRoute } from "@tanstack/react-router";
import { format } from "date-fns";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";
import { ThingList } from "@/components/things/thing-card";
import { todayBuckets } from "@/lib/memory/resurface";
import { useMemoryStore } from "@/lib/memory/store";
import { OnboardingView } from "@/components/onboarding-view";
import { ApkInstallCard } from "@/components/apk-download";

export const Route = createFileRoute("/")({
  component: TodayPage,
});

function TodayPage() {
  const onboardingComplete = useMemoryStore((s) => s.settings.onboardingComplete);
  const things = useMemoryStore((s) => s.things);
  const setCaptureOpen = useMemoryStore((s) => s.setCaptureOpen);
  const { due, resurfaced, laterToday, suggested, stillWant } = todayBuckets(things);

  if (!onboardingComplete) {
    return <OnboardingView />;
  }

  const empty =
    due.length +
      resurfaced.length +
      laterToday.length +
      suggested.length +
      stillWant.length ===
    0;

  return (
    <div>
      <header className="mb-8">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
          {format(new Date(), "EEEE d MMMM")}
        </p>
        <h1 className="mt-1 font-display text-4xl font-medium tracking-tight">
          Worth your attention
        </h1>
        <p className="mt-2 max-w-lg text-sm text-muted-foreground">
          Not a task list. The few things that should come back now.
        </p>
      </header>

      <ApkInstallCard className="mb-8" />

      {empty ? (
        <EmptyState
          title="Nothing needs your attention right now."
          body="Share a link, a screenshot, or a thought. It will wait until it is useful."
          action={
            <Button onClick={() => setCaptureOpen(true)}>Save something</Button>
          }
        />
      ) : (
        <div className="flex flex-col gap-9">
          <Section title="Due" items={due} />
          <Section title="Resurfaced" items={resurfaced} />
          <Section title="Suggested" items={suggested} />
          <Section title="Later today" items={laterToday} />
          <Section
            title="Still want this?"
            items={stillWant}
            stale
            hint="Not opened in a month."
          />
        </div>
      )}
    </div>
  );
}

function Section({
  title,
  items,
  stale = false,
  hint,
}: {
  title: string;
  items: ReturnType<typeof todayBuckets>["due"];
  stale?: boolean;
  hint?: string;
}) {
  if (items.length === 0) return null;
  return (
    <section>
      <h2 className="mb-1 text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
        {title}
        <span className="ml-2 tabular-nums">{items.length}</span>
      </h2>
      {hint ? <p className="mb-3 text-xs text-muted-foreground">{hint}</p> : <div className="mb-3" />}
      <ThingList things={items} stale={stale} />
    </section>
  );
}
