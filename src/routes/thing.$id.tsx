import { useEffect, useRef, type ReactNode } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import {
  Archive,
  ArrowLeft,
  Check,
  Clock3,
  ExternalLink,
  Pin,
  RotateCcw,
  Star,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { CategoryMark } from "@/components/things/category-mark";
import { LaterMenu } from "@/components/things/later-menu";
import { CATEGORIES } from "@/lib/memory/types";
import type { Priority } from "@/lib/memory/types";
import { CATEGORY_LABEL, formatWhen, hostFromUrl, thingActionVerb } from "@/lib/memory/format";
import { enrichThing } from "@/lib/memory/enrich";
import { useMemoryStore } from "@/lib/memory/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/thing/$id")({
  component: ThingDetailPage,
});

function ThingDetailPage() {
  const { id } = Route.useParams();
  const navigate = useNavigate();
  const thing = useMemoryStore((s) => s.things.find((t) => t.id === id));
  const updateThing = useMemoryStore((s) => s.updateThing);
  const complete = useMemoryStore((s) => s.complete);
  const archive = useMemoryStore((s) => s.archive);
  const restore = useMemoryStore((s) => s.restore);
  const remove = useMemoryStore((s) => s.remove);
  const togglePin = useMemoryStore((s) => s.togglePin);
  const toggleFavourite = useMemoryStore((s) => s.toggleFavourite);
  const openThing = useMemoryStore((s) => s.openThing);
  const didOpen = useRef<string | null>(null);

  useEffect(() => {
    if (thing && didOpen.current !== thing.id) {
      didOpen.current = thing.id;
      openThing(thing.id);
    }
  }, [thing, openThing]);

  if (!thing) {
    return (
      <div className="py-16">
        <h1 className="font-display text-3xl">This thing is gone</h1>
        <p className="mt-2 text-sm text-muted-foreground">It may have been deleted.</p>
        <Button className="mt-6" variant="outline" asChild>
          <Link to="/">Back to Today</Link>
        </Button>
      </div>
    );
  }

  const host = hostFromUrl(thing.sourceUrl) ?? thing.siteName;
  const doneLabel = thingActionVerb(thing);
  const closed = thing.status === "completed" || thing.status === "archived";

  return (
    <article className="pb-8">
      <div className="mb-6 flex items-center gap-2">
        <Button variant="ghost" size="icon-sm" asChild aria-label="Back">
          <Link to="/inbox">
            <ArrowLeft className="size-4" />
          </Link>
        </Button>
        <span className="text-sm text-muted-foreground">Item</span>
      </div>

      {(thing.imageDataUrl || thing.thumbnailUrl) && (
        <img
          src={thing.imageDataUrl || thing.thumbnailUrl}
          alt=""
          className="thumb mb-6 max-h-64 w-full rounded-2xl object-cover"
        />
      )}

      <input
        aria-label="Title"
        className="w-full bg-transparent font-display text-3xl font-medium leading-tight tracking-tight text-foreground outline-none placeholder:text-muted-foreground"
        value={thing.title}
        onChange={(e) => updateThing(thing.id, { title: e.target.value })}
      />

      <textarea
        aria-label="Summary"
        className="mt-3 min-h-16 w-full resize-none bg-transparent text-base leading-relaxed text-muted-foreground outline-none"
        value={thing.summary ?? ""}
        placeholder="A short summary"
        onChange={(e) => updateThing(thing.id, { summary: e.target.value })}
      />

      <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
        <CategoryMark category={thing.category} />
        {host ? <span>{host}</span> : null}
        <span>Saved {formatWhen(thing.createdAt)}</span>
      </div>

      {thing.processingStatus === "PROCESSING" || thing.processingStatus === "QUEUED" ? (
        <p className="mt-3 text-sm italic text-muted-foreground">Understanding…</p>
      ) : null}
      {thing.processingStatus === "FAILED" ? (
        <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
          <span>{thing.processingError ?? "Could not enrich this yet."}</span>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              void enrichThing(thing.id);
              toast("Trying again");
            }}
          >
            <RotateCcw className="size-3.5" />
            Retry
          </Button>
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap gap-2">
        {!closed ? (
          <>
            <Button
              onClick={() => {
                complete(thing.id);
                toast(`Marked ${doneLabel.toLowerCase()}`);
                void navigate({ to: "/" });
              }}
            >
              <Check className="size-4" />
              {doneLabel}
            </Button>
            <LaterMenu
              ids={[thing.id]}
              trigger={
                <Button variant="secondary">
                  <Clock3 className="size-4" />
                  Later
                </Button>
              }
            />
          </>
        ) : (
          <Button variant="secondary" onClick={() => restore(thing.id)}>
            Restore
          </Button>
        )}
        <Button variant="outline" onClick={() => togglePin(thing.id)}>
          <Pin className="size-4" />
          {thing.isPinned ? "Unpin" : "Pin"}
        </Button>
        <Button variant="outline" onClick={() => toggleFavourite(thing.id)}>
          <Star className="size-4" />
          {thing.isFavourite ? "Unfavourite" : "Favourite"}
        </Button>
        {thing.sourceUrl ? (
          <Button variant="outline" asChild>
            <a href={thing.sourceUrl} target="_blank" rel="noreferrer">
              <ExternalLink className="size-4" />
              Open original
            </a>
          </Button>
        ) : null}
      </div>

      <section className="mt-10">
        <h2 className="mb-3 text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
          Adjust
        </h2>
        <p className="mb-2 text-sm font-medium">Category</p>
        <div className="flex flex-wrap gap-1.5">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => updateThing(thing.id, { category: c })}
              className={cn(
                "rounded-full px-3 py-1.5 text-xs font-medium",
                thing.category === c
                  ? "bg-foreground text-background"
                  : "bg-muted text-muted-foreground",
              )}
            >
              {CATEGORY_LABEL[c]}
            </button>
          ))}
        </div>

        <p className="mb-2 mt-5 text-sm font-medium">Priority</p>
        <div className="flex flex-wrap gap-1.5">
          {(["low", "normal", "high"] as Priority[]).map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => updateThing(thing.id, { priority: p })}
              className={cn(
                "rounded-full px-3 py-1.5 text-xs font-medium capitalize",
                thing.priority === p
                  ? "bg-foreground text-background"
                  : "bg-muted text-muted-foreground",
              )}
            >
              {p}
            </button>
          ))}
        </div>

        <div className="mt-5">
          <Label htmlFor="tags">Tags</Label>
          <Input
            id="tags"
            className="mt-1.5"
            value={thing.tags.join(", ")}
            onChange={(e) =>
              updateThing(thing.id, {
                tags: e.target.value
                  .split(",")
                  .map((t) => t.trim())
                  .filter(Boolean),
              })
            }
            placeholder="comma separated"
          />
        </div>

        <div className="mt-5">
          <Label htmlFor="notes">Notes</Label>
          <Textarea
            id="notes"
            className="mt-1.5"
            value={thing.notes ?? ""}
            onChange={(e) => updateThing(thing.id, { notes: e.target.value })}
            placeholder="Anything you want to add"
          />
        </div>

        <div className="mt-5 grid gap-3 sm:grid-cols-2">
          <div>
            <Label htmlFor="due">Due</Label>
            <Input
              id="due"
              type="datetime-local"
              className="mt-1.5"
              value={toLocalInput(thing.dueAt)}
              onChange={(e) =>
                updateThing(thing.id, {
                  dueAt: e.target.value ? new Date(e.target.value).getTime() : undefined,
                })
              }
            />
          </div>
          <div>
            <Label htmlFor="resurface">Bring back</Label>
            <Input
              id="resurface"
              type="datetime-local"
              className="mt-1.5"
              value={toLocalInput(thing.resurfaceAt)}
              onChange={(e) =>
                updateThing(thing.id, {
                  resurfaceAt: e.target.value
                    ? new Date(e.target.value).getTime()
                    : undefined,
                  reasonForResurface: "Set manually",
                })
              }
            />
          </div>
        </div>
      </section>

      <dl className="mt-8 grid gap-2 text-sm">
        {thing.reasonForResurface ? (
          <Meta label="Why later" value={thing.reasonForResurface} />
        ) : null}
        {thing.detectedPerson ? <Meta label="Person" value={thing.detectedPerson} /> : null}
        {thing.detectedAction ? <Meta label="Action" value={thing.detectedAction} /> : null}
        {thing.detectedLocation ? (
          <Meta label="Place" value={thing.detectedLocation} />
        ) : null}
        {thing.estimatedReadMinutes ? (
          <Meta label="Time" value={`${thing.estimatedReadMinutes} min`} />
        ) : null}
        {thing.aiConfidence !== undefined ? (
          <Meta
            label="Confidence"
            value={`${Math.round(thing.aiConfidence * 100)}%`}
          />
        ) : null}
      </dl>

      {thing.originalContent && thing.originalContent !== thing.title ? (
        <div className="mt-8">
          <h2 className="text-sm font-medium">Original</h2>
          <pre className="mt-2 whitespace-pre-wrap rounded-xl bg-muted px-4 py-3 font-sans text-sm text-muted-foreground">
            {thing.originalContent}
          </pre>
        </div>
      ) : null}

      {thing.ocrText ? (
        <div className="mt-6">
          <h2 className="text-sm font-medium">Extracted text</h2>
          <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">
            {thing.ocrText}
          </p>
        </div>
      ) : null}

      <div className="mt-10 flex flex-wrap gap-2">
        {!closed ? (
          <Button variant="outline" onClick={() => archive(thing.id)}>
            <Archive className="size-4" />
            Archive
          </Button>
        ) : null}
        {thing.processingStatus === "NONE" ? (
          <Button
            variant="outline"
            onClick={() => {
              void enrichThing(thing.id);
              toast("Understanding…");
            }}
          >
            <RotateCcw className="size-4" />
            Understand
          </Button>
        ) : null}
        <Button
          variant="ghost"
          className="text-destructive"
          onClick={() => {
            if (confirm("Delete this permanently from this device?")) {
              remove(thing.id);
              void navigate({ to: "/library" });
            }
          }}
        >
          <Trash2 className="size-4" />
          Delete
        </Button>
      </div>
    </article>
  );
}

function Meta({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="grid grid-cols-[7rem_1fr] gap-3">
      <dt className="text-muted-foreground">{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function toLocalInput(ts?: number): string {
  if (!ts) return "";
  const d = new Date(ts);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
