import { useState, type ReactNode } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { snoozeOptions } from "@/lib/memory/resurface";
import { useMemoryStore } from "@/lib/memory/store";

export function LaterMenu({
  ids,
  trigger,
  onPicked,
}: {
  ids: string[];
  trigger: ReactNode;
  onPicked?: () => void;
}) {
  const settings = useMemoryStore((s) => s.settings);
  const snoozeMany = useMemoryStore((s) => s.snoozeMany);
  const later = snoozeOptions(new Date(), settings);
  const [customOpen, setCustomOpen] = useState(false);
  const [customValue, setCustomValue] = useState("");

  function apply(at: number, label: string) {
    snoozeMany(ids, at);
    toast(`Later · ${label}`);
    onPicked?.();
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>{trigger}</DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          {later.map((opt) => (
            <DropdownMenuItem key={opt.id} onClick={() => apply(opt.at, opt.label)}>
              {opt.label}
            </DropdownMenuItem>
          ))}
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onSelect={() => {
              const d = new Date(Date.now() + 3600_000);
              const pad = (n: number) => n.toString().padStart(2, "0");
              setCustomValue(
                `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`,
              );
              setCustomOpen(true);
            }}
          >
            Pick a time
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <Dialog open={customOpen} onOpenChange={setCustomOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Bring it back</DialogTitle>
          </DialogHeader>
          <div>
            <Label htmlFor="custom-snooze">When</Label>
            <Input
              id="custom-snooze"
              type="datetime-local"
              className="mt-1.5"
              value={customValue}
              onChange={(e) => setCustomValue(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setCustomOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                if (!customValue) return;
                const at = new Date(customValue).getTime();
                if (Number.isNaN(at) || at <= Date.now()) {
                  toast("Pick a time in the future");
                  return;
                }
                apply(at, "custom time");
                setCustomOpen(false);
              }}
            >
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
