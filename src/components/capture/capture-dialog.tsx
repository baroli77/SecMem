import { useEffect, useRef, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { Clipboard, ImagePlus, Link2, LoaderCircle } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { extractFirstUrl, findDuplicate } from "@/lib/memory/heuristics";
import { compressImageFile, enrichThing } from "@/lib/memory/enrich";
import { useMemoryStore } from "@/lib/memory/store";
import { FREE_ACTIVE_LIMIT, activeCount } from "@/lib/memory/types";
import { relativeAge } from "@/lib/memory/format";

const SAMPLES = [
  {
    label: "Article",
    text: "https://waitbutwhy.com/2015/01/artificial-intelligence-revolution-1.html",
  },
  {
    label: "Task",
    text: "Remind me to book the train tickets next Tuesday.",
  },
  {
    label: "Product",
    text: "Sony WH-1000XM7 Headphones\nhttps://www.amazon.com/dp/B09XS7J49B",
  },
  {
    label: "Video",
    text: "https://www.youtube.com/watch?v=jNQXAC9IVRw",
  },
];

export function CaptureDialog() {
  const open = useMemoryStore((s) => s.captureOpen);
  const setOpen = useMemoryStore((s) => s.setCaptureOpen);
  const capture = useMemoryStore((s) => s.capture);
  const things = useMemoryStore((s) => s.things);
  const settings = useMemoryStore((s) => s.settings);
  const navigate = useNavigate();
  const [text, setText] = useState("");
  const [image, setImage] = useState<{ dataUrl: string; mimeType: string } | null>(
    null,
  );
  const [busy, setBusy] = useState(false);
  const [duplicateId, setDuplicateId] = useState<string | null>(null);
  const areaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (open) {
      setTimeout(() => areaRef.current?.focus(), 40);
    } else {
      setText("");
      setImage(null);
      setDuplicateId(null);
      setBusy(false);
    }
  }, [open]);

  const detectedUrl = extractFirstUrl(text);
  const overLimit =
    !settings.isPro && activeCount(things) >= FREE_ACTIVE_LIMIT;

  async function save(force = false) {
    if (overLimit && !force) {
      toast("Free plan holds 40 active things. Complete or archive some, or turn on Pro in Settings.");
      return;
    }
    if (!text.trim() && !image) return;
    setBusy(true);
    const result = capture({
      text: text.trim() || undefined,
      url: detectedUrl,
      imageDataUrl: image?.dataUrl,
      mimeType: image?.mimeType,
      sourceApp: "Capture",
      forceDuplicate: force,
    });
    if (result.blocked === "limit") {
      setBusy(false);
      toast("Active limit reached");
      return;
    }
    if (result.duplicate && !force) {
      setDuplicateId(result.duplicate.id);
      setBusy(false);
      return;
    }
    setOpen(false);
    toast("Saved.", {
      action: {
        label: "Open",
        onClick: () => {
          void navigate({ to: "/thing/$id", params: { id: result.thing.id } });
        },
      },
    });
    void enrichThing(result.thing.id);
    void navigate({ to: "/inbox" });
  }

  async function onFiles(files: FileList | null) {
    const file = files?.[0];
    if (!file || !file.type.startsWith("image/")) return;
    setBusy(true);
    try {
      const compressed = await compressImageFile(file);
      setImage(compressed);
    } catch {
      toast("Could not read that image");
    } finally {
      setBusy(false);
    }
  }

  async function pasteClipboard() {
    try {
      if (navigator.clipboard?.read) {
        const items = await navigator.clipboard.read();
        for (const item of items) {
          const imageType = item.types.find((t) => t.startsWith("image/"));
          if (imageType) {
            const blob = await item.getType(imageType);
            const file = new File([blob], "clipboard.png", { type: imageType });
            const compressed = await compressImageFile(file);
            setImage(compressed);
            return;
          }
          if (item.types.includes("text/plain")) {
            const blob = await item.getType("text/plain");
            const pasted = (await blob.text()).trim();
            if (pasted) setText(pasted);
            return;
          }
        }
      }
      const pasted = (await navigator.clipboard.readText()).trim();
      if (pasted) setText(pasted);
      else toast("Clipboard is empty");
    } catch {
      toast("Clipboard is not available here");
    }
  }

  const duplicate = duplicateId
    ? things.find((t) => t.id === duplicateId)
    : undefined;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="max-w-xl gap-4 sm:p-6">
        <DialogHeader>
          <DialogTitle>Save something</DialogTitle>
          <DialogDescription>
            Paste a link, drop a screenshot, or write a thought. It is stored
            immediately. Understanding happens after.
          </DialogDescription>
        </DialogHeader>

        {duplicate ? (
          <div className="rounded-xl border border-border bg-muted/60 p-4">
            <p className="text-sm font-medium">Already saved {relativeAge(duplicate.createdAt)}</p>
            <p className="mt-1 text-sm text-muted-foreground">{duplicate.title}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button
                size="sm"
                onClick={() => {
                  setOpen(false);
                  void navigate({ to: "/thing/$id", params: { id: duplicate.id } });
                }}
              >
                Open existing
              </Button>
              <Button size="sm" variant="outline" onClick={() => void save(true)}>
                Save another copy
              </Button>
            </div>
          </div>
        ) : (
          <>
            <Textarea
              ref={areaRef}
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="A URL, a reminder, a recipe, a thought…"
              className="min-h-36 font-sans"
              onKeyDown={(e) => {
                if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
                  e.preventDefault();
                  void save();
                }
              }}
              onPaste={(e) => {
                const file = [...e.clipboardData.files].find((f) =>
                  f.type.startsWith("image/"),
                );
                if (file) {
                  e.preventDefault();
                  void compressImageFile(file)
                    .then(setImage)
                    .catch(() => toast("Could not read that image"));
                }
              }}
            />
            <div className="flex flex-wrap gap-2">
              <Button type="button" size="sm" variant="outline" onClick={() => void pasteClipboard()}>
                <Clipboard className="size-3.5" />
                Paste clipboard
              </Button>
            </div>
            <label
              className="flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-muted/40 px-3 py-4 text-sm text-muted-foreground transition-colors hover:bg-muted"
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                void onFiles(e.dataTransfer.files);
              }}
            >
              <ImagePlus className="size-4" />
              Drop an image, or tap to attach a screenshot
              <input
                type="file"
                accept="image/*"
                className="sr-only"
                onChange={(e) => void onFiles(e.target.files)}
              />
            </label>
            {image ? (
              <img
                src={image.dataUrl}
                alt="Attached"
                className="thumb max-h-40 w-full rounded-xl object-cover"
              />
            ) : null}
            {detectedUrl ? (
              <p className="flex items-center gap-2 text-xs text-muted-foreground">
                <Link2 className="size-3.5" />
                {detectedUrl}
              </p>
            ) : null}
            {findDuplicate(things, detectedUrl) && !duplicate ? (
              <p className="text-xs text-muted-foreground">
                A copy of this link may already be saved.
              </p>
            ) : null}
            <div className="flex flex-wrap gap-1.5">
              {SAMPLES.map((sample) => (
                <button
                  key={sample.label}
                  type="button"
                  className="rounded-full border border-border bg-card px-3 py-1 text-xs text-muted-foreground hover:text-foreground"
                  onClick={() => setText(sample.text)}
                >
                  Try {sample.label.toLowerCase()}
                </button>
              ))}
            </div>
          </>
        )}

        <DialogFooter>
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          {!duplicate ? (
            <Button onClick={() => void save()} disabled={busy || (!text.trim() && !image)}>
              {busy ? <LoaderCircle className="size-4 animate-spin" /> : null}
              Save
            </Button>
          ) : null}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
