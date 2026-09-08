import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";
import { ThingList } from "@/components/things/thing-card";
import { LaterMenu } from "@/components/things/later-menu";
import { useMemoryStore } from "@/lib/memory/store";

export const Route = createFileRoute("/inbox")({
  component: InboxPage,
});

function InboxPage() {
  const things = useMemoryStore((s) => s.things);
  const setCaptureOpen = useMemoryStore((s) => s.setCaptureOpen);
  const completeMany = useMemoryStore((s) => s.completeMany);
  const archiveMany = useMemoryStore((s) => s.archiveMany);
  const inbox = things
    .filter((t) => t.status === "inbox")
    .sort((a, b) => b.createdAt - a.createdAt);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const selecting = selected.size > 0;

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function enterSelect(id: string) {
    setSelected((prev) => new Set(prev).add(id));
  }

  const ids = [...selected];

  return (
    <div>
      <header className="mb-8">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
          Inbox
        </p>
        <h1 className="mt-1 font-display text-4xl font-medium tracking-tight">
          Recently saved
        </h1>
        <p className="mt-2 max-w-lg text-sm text-muted-foreground">
          Glance, then done or later. Swipe right to finish, left to snooze.
          Opening an item files it in the library.
        </p>
      </header>

      {selecting ? (
        <div className="mb-4 flex flex-wrap items-center gap-2 rounded-2xl bg-card px-3 py-2 shadow-(--shadow-border)">
          <p className="mr-auto text-sm">
            <span className="tabular-nums">{selected.size}</span> selected
          </p>
          <Button
            size="sm"
            onClick={() => {
              completeMany(ids);
              toast("Marked done");
              setSelected(new Set());
            }}
          >
            Done
          </Button>
          <LaterMenu
            ids={ids}
            onPicked={() => setSelected(new Set())}
            trigger={
              <Button size="sm" variant="secondary">
                Later
              </Button>
            }
          />
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              archiveMany(ids);
              toast("Archived");
              setSelected(new Set());
            }}
          >
            Archive
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setSelected(new Set())}>
            Cancel
          </Button>
        </div>
      ) : null}

      {inbox.length === 0 ? (
        <EmptyState
          title="You're clear. Share something you don't want to forget."
          body="Inbox is for new captures that have not been opened yet."
          action={
            <Button onClick={() => setCaptureOpen(true)}>Save something</Button>
          }
        />
      ) : (
        <ThingList
          things={inbox}
          selecting={selecting}
          selectedIds={selected}
          onToggleSelect={toggle}
          onLongPress={enterSelect}
        />
      )}
    </div>
  );
}
