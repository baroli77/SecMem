import { useEffect, useRef, type ReactNode } from "react";
import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import {
  Inbox,
  Library,
  MoreHorizontal,
  Plus,
  Search,
  Settings,
  SunMoon,
  Activity,
  Download,
} from "lucide-react";
import { toast, Toaster } from "sonner";
import { Button } from "@/components/ui/button";
import { CaptureDialog } from "@/components/capture/capture-dialog";
import { ApkBanner } from "@/components/apk-download";
import { useMemoryStore } from "@/lib/memory/store";
import { enrichThing } from "@/lib/memory/enrich";
import { pickPending } from "@/lib/memory/queue";
import { notificationCopy } from "@/lib/memory/resurface";
import { cn } from "@/lib/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const NAV = [
  { to: "/", label: "Today", icon: SunMoon, match: (p: string) => p === "/" },
  {
    to: "/inbox",
    label: "Inbox",
    icon: Inbox,
    match: (p: string) => p.startsWith("/inbox"),
  },
  {
    to: "/library",
    label: "Library",
    icon: Library,
    match: (p: string) => p.startsWith("/library"),
  },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const hydrated = useMemoryStore((s) => s.hydrated);
  const settings = useMemoryStore((s) => s.settings);
  const things = useMemoryStore((s) => s.things);
  const setCaptureOpen = useMemoryStore((s) => s.setCaptureOpen);
  const tickResurface = useMemoryStore((s) => s.tickResurface);
  const recordNudge = useMemoryStore((s) => s.recordNudge);
  const inboxCount = things.filter((t) => t.status === "inbox").length;
  const queueBusy = useRef(false);

  useEffect(() => {
    if (!hydrated) return;
    if (!settings.onboardingComplete && pathname !== "/onboarding" && pathname !== "/download") {
      void navigate({ to: "/onboarding" });
    }
  }, [hydrated, settings.onboardingComplete, pathname, navigate]);

  useEffect(() => {
    if (!hydrated) return;

    function nudge(surfaced: ReturnType<typeof tickResurface>) {
      const top = surfaced[0];
      if (!top) return;
      const copy = notificationCopy(top);
      toast(copy, {
        description: top.title,
        action: {
          label: "Open",
          onClick: () => {
            void navigate({ to: "/thing/$id", params: { id: top.id } });
          },
        },
      });
      if (settings.notificationsEnabled && recordNudge()) {
        try {
          if (typeof Notification !== "undefined" && Notification.permission === "granted") {
            const n = new Notification(copy, {
              body: top.title,
              tag: `sm-${top.id}`,
            });
            n.onclick = () => {
              window.focus();
              void navigate({ to: "/thing/$id", params: { id: top.id } });
            };
          }
        } catch {
          // Notifications are optional.
        }
      }
    }

    async function drainQueue() {
      if (queueBusy.current) return;
      const state = useMemoryStore.getState();
      if (!state.settings.aiEnabled || !state.settings.automaticProcessing) return;
      const pending = pickPending(state.things);
      if (pending.length === 0) return;
      queueBusy.current = true;
      try {
        for (const item of pending) {
          await enrichThing(item.id);
        }
      } finally {
        queueBusy.current = false;
      }
    }

    nudge(tickResurface());
    void drainQueue();
    const id = window.setInterval(() => {
      nudge(tickResurface());
      void drainQueue();
    }, 60_000);
    return () => window.clearInterval(id);
  }, [hydrated, tickResurface, recordNudge, settings.notificationsEnabled, navigate]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      const target = e.target as HTMLElement | null;
      const typing =
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable);
      if (typing) return;
      if (e.key === "c") {
        e.preventDefault();
        setCaptureOpen(true);
      }
      if (e.key === "/" || (e.key === "k" && (e.metaKey || e.ctrlKey))) {
        e.preventDefault();
        void navigate({ to: "/search" });
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [navigate, setCaptureOpen]);

  useEffect(() => {
    if (!hydrated || !settings.onboardingComplete) return;
    const params = new URLSearchParams(window.location.search);
    const shared = params.get("url") || params.get("text") || params.get("share");
    if (!shared) return;
    window.history.replaceState({}, "", window.location.pathname);
    const result = useMemoryStore.getState().capture({
      text: shared,
      sourceApp: "Share",
    });
    if (result.blocked === "limit") {
      toast("Active limit reached. Complete or archive something first.");
      return;
    }
    if (result.duplicate) {
      toast("Already saved — opened the existing item.");
      void navigate({ to: "/thing/$id", params: { id: result.duplicate.id } });
      return;
    }
    toast("Saved.");
    void enrichThing(result.thing.id);
    void navigate({ to: "/inbox" });
  }, [hydrated, settings.onboardingComplete, navigate]);

  if (!settings.onboardingComplete || pathname === "/onboarding" || pathname === "/download") {
    return (
      <>
        <ApkBanner />
        {children}
        <CaptureDialog />
        <Toaster richColors={false} position="bottom-center" />
      </>
    );
  }

  return (
    <div className="min-h-dvh bg-background text-foreground">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-card focus:px-3 focus:py-2"
      >
        Skip to content
      </a>
      <ApkBanner />
      <aside className="fixed inset-y-0 left-0 hidden w-60 border-r border-border bg-card/60 px-4 py-6 md:flex md:flex-col">
        <Link to="/" className="px-2">
          <p className="font-display text-2xl font-medium tracking-tight">Second Memory</p>
          <p className="mt-1 text-xs text-muted-foreground">Save it. Remember it later.</p>
        </Link>
        <nav className="mt-8 flex flex-1 flex-col gap-1">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = item.match(pathname);
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium",
                  active
                    ? "bg-muted text-foreground"
                    : "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
                )}
              >
                <Icon className="size-4" />
                {item.label}
                {item.to === "/inbox" && inboxCount > 0 ? (
                  <span className="ml-auto tabular-nums text-xs text-muted-foreground">
                    {inboxCount}
                  </span>
                ) : null}
              </Link>
            );
          })}
          <Link
            to="/search"
            className={cn(
              "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium",
              pathname.startsWith("/search")
                ? "bg-muted text-foreground"
                : "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
            )}
          >
            <Search className="size-4" />
            Search
          </Link>
          <Link
            to="/activity"
            className={cn(
              "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium",
              pathname.startsWith("/activity")
                ? "bg-muted text-foreground"
                : "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
            )}
          >
            <Activity className="size-4" />
            Activity
          </Link>
          <div className="mt-auto flex flex-col gap-1">
            <Link
              to="/download"
              className={cn(
                "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium",
                pathname.startsWith("/download")
                  ? "bg-muted text-foreground"
                  : "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
              )}
            >
              <Download className="size-4" />
              Download APK
            </Link>
            <Link
              to="/settings"
              className={cn(
                "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium",
                pathname.startsWith("/settings")
                  ? "bg-muted text-foreground"
                  : "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
              )}
            >
              <Settings className="size-4" />
              Settings
            </Link>
            <Button className="mt-2 w-full" onClick={() => setCaptureOpen(true)}>
              <Plus className="size-4" />
              Save
            </Button>
          </div>
        </nav>
      </aside>

      <header className="sticky top-0 z-30 flex items-center gap-1.5 border-b border-border bg-background/90 px-4 py-3 backdrop-blur-sm md:hidden">
        <Link to="/" className="min-w-0 flex-1">
          <p className="font-display text-lg font-medium tracking-tight">Second Memory</p>
        </Link>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label="Search"
          onClick={() => navigate({ to: "/search" })}
        >
          <Search className="size-4" />
        </Button>
        <Button asChild size="sm">
          <Link to="/download">APK</Link>
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label="More">
              <MoreHorizontal className="size-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => navigate({ to: "/download" })}>
              <Download className="size-4" />
              Download APK
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => navigate({ to: "/activity" })}>
              <Activity className="size-4" />
              Activity
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => navigate({ to: "/settings" })}>
              <Settings className="size-4" />
              Settings
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
        <Button size="sm" variant="outline" onClick={() => setCaptureOpen(true)}>
          <Plus className="size-4" />
          Save
        </Button>
      </header>

      <main
        id="main"
        className="mx-auto w-full max-w-3xl px-4 pb-28 pt-6 md:ml-60 md:max-w-3xl md:px-8 md:pb-16 md:pt-10"
      >
        {children}
      </main>

      <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-border bg-card/95 px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-1 md:hidden">
        <ul className="grid grid-cols-3">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = item.match(pathname);
            return (
              <li key={item.to}>
                <Link
                  to={item.to}
                  className={cn(
                    "flex h-12 flex-col items-center justify-center gap-0.5 text-[11px] font-medium",
                    active ? "text-foreground" : "text-muted-foreground",
                  )}
                >
                  <span className="relative">
                    <Icon className="size-5" />
                    {item.to === "/inbox" && inboxCount > 0 ? (
                      <span className="absolute -right-2 -top-1 size-1.5 rounded-full bg-primary" />
                    ) : null}
                  </span>
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      <CaptureDialog />
      <Toaster richColors={false} position="bottom-center" />
    </div>
  );
}
