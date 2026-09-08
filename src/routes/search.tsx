import { useMemo, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/empty-state";
import { ThingList } from "@/components/things/thing-card";
import { searchThings } from "@/lib/memory/search";
import { useMemoryStore } from "@/lib/memory/store";

export const Route = createFileRoute("/search")({
  component: SearchPage,
});

function SearchPage() {
  const things = useMemoryStore((s) => s.things);
  const logActivity = useMemoryStore((s) => s.logActivity);
  const [query, setQuery] = useState("");
  const results = useMemo(() => searchThings(things, query), [things, query]);

  return (
    <div>
      <header className="mb-6">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
          Search
        </p>
        <h1 className="mt-1 font-display text-4xl font-medium tracking-tight">
          Find it again
        </h1>
      </header>
      <Input
        autoFocus
        value={query}
        placeholder="Titles, notes, URLs, tags, OCR…"
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") logActivity("search", undefined, query);
        }}
        aria-label="Search saved things"
      />
      <div className="mt-6">
        {!query.trim() ? (
          <p className="text-sm text-muted-foreground">
            Search stays on this device. Nothing is sent to a server.
          </p>
        ) : results.length === 0 ? (
          <EmptyState
            title="No matches."
            body="Try a title fragment, a site name, or a person."
          />
        ) : (
          <ThingList things={results} />
        )}
      </div>
    </div>
  );
}
