import { useRef } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  Archive,
  Check,
  Clock3,
  ExternalLink,
  MoreHorizontal,
  Pin,
  Star,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import type { Thing } from "@/lib/memory/types";
import {
  CATEGORY_LABEL,
  compactFuture,
  hostFromUrl,
  relativeAge,
  thingActionVerb,
} from "@/lib/memory/format";
import { useMemoryStore } from "@/lib/memory/store";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { CategoryMark } from "./category-mark";
import { LaterMenu } from "./later-menu";
import { SwipeRow } from "./swipe-row";

export function ThingCard({
  thing,
  showResurface = true,
  stale = false,
  selecting = false,
  selected = false,
  onToggleSelect,
  onLongPress,
}: {
  thing: Thing;
  showResurface?: boolean;
  stale?: boolean;
  selecting?: boolean;
  selected?: boolean;
  onToggleSelect?: (id: string) => void;
  onLongPress?: (id: string) => void;
}) {
  const navigate = useNavigate();
  const complete = useMemoryStore((s) => s.complete);
  const archive = useMemoryStore((s) => s.archive);
  const restore = useMemoryStore((s) => s.restore);
  const remove = useMemoryStore((s) => s.remove);
  const togglePin = useMemoryStore((s) => s.togglePin);
  const toggleFavourite = useMemoryStore((s) => s.toggleFavourite);
  const keep = useMemoryStore((s) => s.keep);
  const snooze = useMemoryStore((s) => s.snooze);

  const host = hostFromUrl(thing.sourceUrl) ?? thing.siteName ?? thing.sourceApp;
  const doneLabel = thingActionVerb(thing);
  const closed = thing.status === "completed" || thing.status === "archived";
  const longTimer = useRef(0);

  function handleLongStart() {
    if (!onLongPress) return;
    longTimer.current = window.setTimeout(() => onLongPress(thing.id), 480);
  }
  function handleLongEnd() {
    if (longTimer.current) window.clearTimeout(longTimer.current);
  }

  const card = (
    <article
      className={cn(
        "group flex gap-3 rounded-2xl bg-card p-3 shadow-(--shadow-border) transition-[box-shadow,opacity] duration-(--motion-quick) hover:shadow-(--shadow-border-hover)",
        thing.status === "completed" && "opacity-70",
        selected && "ring-2 ring-ring/70",
      )}
      onPointerDown={handleLongStart}
      onPointerUp={handleLongEnd}
      onPointerLeave={handleLongEnd}
      onPointerCancel={handleLongEnd}
    >
      {selecting ? (
        <button
          type="button"
          aria-pressed={selected}
          aria-label={selected ? "Deselect" : "Select"}
          onClick={() => onToggleSelect?.(thing.id)}
          className={cn(
            "mt-1 size-6 shrink-0 rounded-full border border-border",
            selected ? "border-primary bg-primary" : "bg-card",
          )}
        />
      ) : (
        <Link
          to="/thing/$id"
          params={{ id: thing.id }}
          className="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-muted"
          aria-hidden
          tabIndex={-1}
        >
          {thing.thumbnailUrl || thing.imageDataUrl ? (
            <img
              src={thing.thumbnailUrl || thing.imageDataUrl}
              alt=""
              className="thumb size-full object-cover"
            />
          ) : (
            <CategoryMark
              category={thing.category}
              withLabel={false}
              className="text-foreground/70"
            />
          )}
        </Link>
      )}
      <div className="min-w-0 flex-1">
        <div className="flex items-start gap-2">
          <div className="min-w-0 flex-1">
            <h3 className="truncate text-[15px] font-medium leading-snug">
              {selecting ? (
                <button
                  type="button"
                  className="text-left text-foreground"
                  onClick={() => onToggleSelect?.(thing.id)}
                >
                  {thing.title}
                </button>
              ) : (
                <Link
                  to="/thing/$id"
                  params={{ id: thing.id }}
                  className="text-foreground hover:underline"
                >
                  {thing.title}
                </Link>
              )}
            </h3>
            {thing.summary ? (
              <p className="mt-0.5 line-clamp-2 text-sm text-muted-foreground">
                {thing.summary}
              </p>
            ) : null}
          </div>
          <div className="flex shrink-0 items-center gap-0.5">
            {thing.priority === "high" ? (
              <span
                className="size-1.5 rounded-full bg-destructive"
                aria-label="High priority"
              />
            ) : null}
            {thing.isPinned ? (
              <Pin className="size-3.5 fill-current text-foreground" aria-label="Pinned" />
            ) : null}
            {thing.isFavourite ? (
              <Star className="size-3.5 fill-current text-foreground" aria-label="Favourite" />
            ) : null}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="text-muted-foreground"
                  aria-label="More actions"
                >
                  <MoreHorizontal className="size-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => togglePin(thing.id)}>
                  <Pin className="size-4" />
                  {thing.isPinned ? "Unpin" : "Pin"}
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => toggleFavourite(thing.id)}>
                  <Star className="size-4" />
                  {thing.isFavourite ? "Unfavourite" : "Favourite"}
                </DropdownMenuItem>
                {thing.sourceUrl ? (
                  <DropdownMenuItem
                    onClick={() => window.open(thing.sourceUrl, "_blank", "noopener")}
                  >
                    <ExternalLink className="size-4" />
                    Open original
                  </DropdownMenuItem>
                ) : null}
                <DropdownMenuSeparator />
                {closed ? (
                  <DropdownMenuItem onClick={() => restore(thing.id)}>
                    Restore
                  </DropdownMenuItem>
                ) : (
                  <DropdownMenuItem onClick={() => archive(thing.id)}>
                    <Archive className="size-4" />
                    Archive
                  </DropdownMenuItem>
                )}
                <DropdownMenuItem
                  destructive
                  onClick={() => {
                    remove(thing.id);
                    toast("Deleted");
                  }}
                >
                  <Trash2 className="size-4" />
                  Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] text-muted-foreground">
          <CategoryMark category={thing.category} />
          {host ? <span>{host}</span> : null}
          <span>{relativeAge(thing.createdAt)}</span>
          {showResurface && thing.resurfaceAt && thing.status !== "completed" ? (
            <span>Back {compactFuture(thing.resurfaceAt)}</span>
          ) : null}
          {thing.processingStatus === "PROCESSING" || thing.processingStatus === "QUEUED" ? (
            <span className="italic">Understanding…</span>
          ) : null}
          {thing.processingStatus === "FAILED" ? <span>Retry from the item</span> : null}
        </div>
        {stale ? (
          <p className="mt-2 text-xs text-muted-foreground">
            Not opened in a while. Keep it, or let it go.
          </p>
        ) : null}
        {!selecting && !closed ? (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {stale ? (
              <>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    keep(thing.id);
                    toast("Kept for a week");
                  }}
                >
                  Keep
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    archive(thing.id);
                    toast("Archived");
                  }}
                >
                  Archive
                </Button>
              </>
            ) : (
              <>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    complete(thing.id);
                    toast(`Marked ${doneLabel.toLowerCase()}`);
                  }}
                >
                  <Check className="size-3.5" />
                  {doneLabel}
                </Button>
                <LaterMenu
                  ids={[thing.id]}
                  trigger={
                    <Button size="sm" variant="ghost">
                      <Clock3 className="size-3.5" />
                      Later
                    </Button>
                  }
                />
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() =>
                    navigate({ to: "/thing/$id", params: { id: thing.id } })
                  }
                >
                  Open
                </Button>
              </>
            )}
          </div>
        ) : null}
      </div>
      <span className="sr-only">{CATEGORY_LABEL[thing.category]}</span>
    </article>
  );

  return (
    <SwipeRow
      enabled={!selecting && !closed}
      doneLabel={doneLabel}
      onDone={() => {
        complete(thing.id);
        toast(`Marked ${doneLabel.toLowerCase()}`);
      }}
      onLater={() => {
        snooze(thing.id, Date.now() + 60 * 60_000);
        toast("Later · 1 hour");
      }}
    >
      {card}
    </SwipeRow>
  );
}

export function ThingList({
  things,
  showResurface = true,
  stale = false,
  selecting = false,
  selectedIds,
  onToggleSelect,
  onLongPress,
}: {
  things: Thing[];
  showResurface?: boolean;
  stale?: boolean;
  selecting?: boolean;
  selectedIds?: Set<string>;
  onToggleSelect?: (id: string) => void;
  onLongPress?: (id: string) => void;
}) {
  return (
    <ul className="flex flex-col gap-2.5">
      {things.map((thing) => (
        <li key={thing.id}>
          <ThingCard
            thing={thing}
            showResurface={showResurface}
            stale={stale}
            selecting={selecting}
            selected={selectedIds?.has(thing.id)}
            onToggleSelect={onToggleSelect}
            onLongPress={onLongPress}
          />
        </li>
      ))}
    </ul>
  );
}
