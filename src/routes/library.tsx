import { useMemo, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";
import { ThingList } from "@/components/things/thing-card";
import { CATEGORIES } from "@/lib/memory/types";
import { CATEGORY_LABEL } from "@/lib/memory/format";
import { useMemoryStore } from "@/lib/memory/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/library")({
  component: LibraryPage,
});

type Filter =
  | "all"
  | "favourites"
  | "completed"
  | "archived"
  | (typeof CATEGORIES)[number];

function LibraryPage() {
  const things = useMemoryStore((s) => s.things);
  const setCaptureOpen = useMemoryStore((s) => s.setCaptureOpen);
  const [filter, setFilter] = useState<Filter>("all");

  const visible = useMemo(() => {
    return things
      .filter((t) => {
        if (filter === "all") return t.status !== "archived" && t.status !== "completed";
        if (filter === "favourites") return t.isFavourite && t.status !== "archived";
        if (filter === "completed") return t.status === "completed";
        if (filter === "archived") return t.status === "archived";
        return t.category === filter && t.status !== "archived";
      })
      .sort((a, b) => Number(b.isPinned) - Number(a.isPinned) || b.updatedAt - a.updatedAt);
  }, [things, filter]);

  const chips: { id: Filter; label: string }[] = [
    { id: "all", label: "All" },
    { id: "favourites", label: "Favourites" },
    { id: "completed", label: "Completed" },
    { id: "archived", label: "Archived" },
    ...CATEGORIES.filter((c) => c !== "UNKNOWN").map((c) => ({
      id: c,
      label: CATEGORY_LABEL[c],
    })),
  ];

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
            Library
          </p>
          <h1 className="mt-1 font-display text-4xl font-medium tracking-tight">
            Everything saved
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            <span className="tabular-nums">{visible.length}</span>{" "}
            {visible.length === 1 ? "thing" : "things"}
          </p>
        </div>
        <Link
          to="/search"
          className="text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
        >
          Search
        </Link>
      </header>

      <div className="-mx-4 mb-6 flex gap-1.5 overflow-x-auto px-4 pb-1">
        {chips.map((chip) => (
          <button
            key={chip.id}
            type="button"
            onClick={() => setFilter(chip.id)}
            className={cn(
              "h-9 shrink-0 rounded-full px-3 text-sm",
              filter === chip.id
                ? "bg-foreground text-background"
                : "bg-muted text-muted-foreground hover:text-foreground",
            )}
          >
            {chip.label}
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <EmptyState
          title="Things you save will live here."
          body="No folders. Filter by what something is, or search across titles, notes, and extracted text."
          action={
            <Button onClick={() => setCaptureOpen(true)}>Save something</Button>
          }
        />
      ) : (
        <ThingList things={visible} />
      )}
    </div>
  );
}
