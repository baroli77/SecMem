import { useRef, useState, type ReactNode, type PointerEvent } from "react";
import { Check, Clock3 } from "lucide-react";
import { cn } from "@/lib/utils";

const THRESHOLD = 88;

export function SwipeRow({
  children,
  onDone,
  onLater,
  enabled = true,
  doneLabel = "Done",
}: {
  children: ReactNode;
  onDone: () => void;
  onLater: () => void;
  enabled?: boolean;
  doneLabel?: string;
}) {
  const startX = useRef(0);
  const startY = useRef(0);
  const dx = useRef(0);
  const tracking = useRef(false);
  const axis = useRef<"undecided" | "x" | "y">("undecided");
  const suppressClick = useRef(false);
  const [x, setX] = useState(0);
  const [dragging, setDragging] = useState(false);

  if (!enabled) return <>{children}</>;

  function reset() {
    tracking.current = false;
    axis.current = "undecided";
    dx.current = 0;
    setX(0);
    setDragging(false);
  }

  function onPointerDown(e: PointerEvent<HTMLDivElement>) {
    if (e.pointerType === "mouse") return;
    const target = e.target as HTMLElement;
    if (target.closest("button, a, input, textarea, [role='menuitem'], [role='menu']")) {
      return;
    }
    tracking.current = true;
    axis.current = "undecided";
    startX.current = e.clientX;
    startY.current = e.clientY;
    dx.current = 0;
  }

  function onPointerMove(e: PointerEvent<HTMLDivElement>) {
    if (!tracking.current) return;
    const mx = e.clientX - startX.current;
    const my = e.clientY - startY.current;
    if (axis.current === "undecided") {
      if (Math.abs(mx) < 8 && Math.abs(my) < 8) return;
      axis.current = Math.abs(mx) > Math.abs(my) * 1.2 ? "x" : "y";
      if (axis.current === "y") {
        tracking.current = false;
        return;
      }
      setDragging(true);
      try {
        e.currentTarget.setPointerCapture(e.pointerId);
      } catch {
        // Capture is optional.
      }
    }
    if (axis.current !== "x") return;
    dx.current = mx;
    setX(mx);
  }

  function onPointerUp() {
    if (!tracking.current) return;
    const final = dx.current;
    if (final > THRESHOLD) {
      suppressClick.current = true;
      onDone();
      if (typeof navigator !== "undefined" && navigator.vibrate) navigator.vibrate(12);
    } else if (final < -THRESHOLD) {
      suppressClick.current = true;
      onLater();
      if (typeof navigator !== "undefined" && navigator.vibrate) navigator.vibrate(12);
    }
    reset();
  }

  return (
    <div className="relative overflow-hidden rounded-2xl shadow-(--shadow-border) transition-[box-shadow] duration-(--motion-quick) hover:shadow-(--shadow-border-hover)">
      <div className="pointer-events-none absolute inset-0 flex" aria-hidden>
        <div
          className={cn(
            "flex flex-1 items-center gap-2 bg-primary px-4 text-sm font-medium text-primary-foreground",
            x <= 8 && "opacity-0",
          )}
        >
          <Check className="size-4" />
          {doneLabel}
        </div>
        <div
          className={cn(
            "flex flex-1 items-center justify-end gap-2 bg-muted px-4 text-sm font-medium text-foreground",
            x >= -8 && "opacity-0",
          )}
        >
          Later
          <Clock3 className="size-4" />
        </div>
      </div>
      <div
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={reset}
        onClickCapture={(e) => {
          if (!suppressClick.current) return;
          e.preventDefault();
          e.stopPropagation();
          suppressClick.current = false;
        }}
        className="relative touch-pan-y"
        style={{
          transform: `translateX(${x}px)`,
          transition: dragging ? "none" : "transform 180ms cubic-bezier(0.22, 1, 0.36, 1)",
        }}
      >
        {children}
      </div>
    </div>
  );
}
