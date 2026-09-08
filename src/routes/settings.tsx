import { useRef, type ReactNode } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
import { exportSnapshot, useMemoryStore } from "@/lib/memory/store";
import { FREE_ACTIVE_LIMIT, activeCount } from "@/lib/memory/types";
import type { Appearance } from "@/lib/memory/types";
import { ApkDownloadButton } from "@/components/apk-download";

export const Route = createFileRoute("/settings")({
  component: SettingsPage,
});

function SettingsPage() {
  const settings = useMemoryStore((s) => s.settings);
  const patch = useMemoryStore((s) => s.patchSettings);
  const things = useMemoryStore((s) => s.things);
  const resetAll = useMemoryStore((s) => s.resetAll);
  const importSnapshot = useMemoryStore((s) => s.importSnapshot);
  const loadExamples = useMemoryStore((s) => s.loadExamples);
  const fileRef = useRef<HTMLInputElement>(null);
  const active = activeCount(things);

  async function enableNotifications() {
    if (typeof Notification === "undefined") {
      toast("Notifications are not available in this browser");
      return;
    }
    const perm = await Notification.requestPermission();
    patch({ notificationsEnabled: perm === "granted" });
    if (perm !== "granted") toast("Permission was not granted");
  }

  return (
    <div className="max-w-xl">
      <header className="mb-8">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
          Settings
        </p>
        <h1 className="mt-1 font-display text-4xl font-medium tracking-tight">
          How it behaves
        </h1>
      </header>

      <Section title="Appearance">
        <div className="flex gap-2">
          {(["system", "light", "dark"] as Appearance[]).map((mode) => (
            <Button
              key={mode}
              size="sm"
              variant={settings.appearance === mode ? "default" : "outline"}
              onClick={() => patch({ appearance: mode })}
            >
              {mode[0].toUpperCase() + mode.slice(1)}
            </Button>
          ))}
        </div>
      </Section>

      <Section title="Understanding">
        <Row
          label="Allow enrichment"
          hint="A compact extract of an item can be sent to the model. Capture still works if this is off."
        >
          <Switch
            checked={settings.aiEnabled}
            onCheckedChange={(v) => patch({ aiEnabled: v })}
            aria-label="Allow enrichment"
          />
        </Row>
        <Row
          label="Automatic after capture"
          hint="Infer title, category, dates, and when to come back. Turn off to keep only what you typed."
        >
          <Switch
            checked={settings.automaticProcessing}
            onCheckedChange={(v) => patch({ automaticProcessing: v })}
            disabled={!settings.aiEnabled}
            aria-label="Automatic enrichment"
          />
        </Row>
        <p className="text-xs text-muted-foreground">
          {settings.aiUsageCount} enrichments run on this device.
        </p>
      </Section>

      <Section title="Smart resurface">
        <Row label="Bring things back" hint="Rule-based, conservative, never spam.">
          <Switch
            checked={settings.resurfaceEnabled}
            onCheckedChange={(v) => patch({ resurfaceEnabled: v })}
            aria-label="Smart resurface"
          />
        </Row>
        <Field label="Max nudges per day">
          <Input
            type="number"
            min={0}
            max={20}
            value={settings.maxNudgesPerDay}
            onChange={(e) =>
              patch({ maxNudgesPerDay: Math.max(0, Number(e.target.value) || 0) })
            }
          />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Quiet hours start">
            <Input
              type="time"
              value={settings.quietHoursStart}
              onChange={(e) => patch({ quietHoursStart: e.target.value })}
            />
          </Field>
          <Field label="Quiet hours end">
            <Input
              type="time"
              value={settings.quietHoursEnd}
              onChange={(e) => patch({ quietHoursEnd: e.target.value })}
            />
          </Field>
          <Field label="Work starts">
            <Input
              type="time"
              value={settings.workHoursStart}
              onChange={(e) => patch({ workHoursStart: e.target.value })}
            />
          </Field>
          <Field label="Work ends">
            <Input
              type="time"
              value={settings.workHoursEnd}
              onChange={(e) => patch({ workHoursEnd: e.target.value })}
            />
          </Field>
          <Field label="Reading time">
            <Input
              type="time"
              value={settings.readingTime}
              onChange={(e) => patch({ readingTime: e.target.value })}
            />
          </Field>
          <Field label="Leisure time">
            <Input
              type="time"
              value={settings.leisureTime}
              onChange={(e) => patch({ leisureTime: e.target.value })}
            />
          </Field>
        </div>
        <Field label="Payday (day of month)">
          <Input
            type="number"
            min={1}
            max={28}
            value={settings.payday}
            onChange={(e) =>
              patch({ payday: Math.min(28, Math.max(1, Number(e.target.value) || 1)) })
            }
          />
        </Field>
      </Section>

      <Section title="Android app">
        <p className="text-sm text-muted-foreground">
          Native Second Memory for Android 8 and up. Share a link or screenshot
          from any app — it saves immediately and comes back later.
        </p>
        <Button asChild variant="outline">
          <a href="/second-memory.apk" download="SecondMemory.apk">
            Download APK
          </a>
        </Button>
      </Section>

      <Section title="Notifications">
        <Row
          label="Browser nudges"
          hint="Optional. Dismissing a notification never deletes the item."
        >
          <Switch
            checked={settings.notificationsEnabled}
            onCheckedChange={(v) => {
              if (v) void enableNotifications();
              else patch({ notificationsEnabled: false });
            }}
            aria-label="Browser notifications"
          />
        </Row>
      </Section>

      <Section title="Shortcuts">
        <p className="text-sm text-muted-foreground">
          <kbd className="rounded-md bg-muted px-1.5 py-0.5 font-mono text-xs">C</kbd> save
          something ·{" "}
          <kbd className="rounded-md bg-muted px-1.5 py-0.5 font-mono text-xs">/</kbd> search
          ·{" "}
          <kbd className="rounded-md bg-muted px-1.5 py-0.5 font-mono text-xs">⌘</kbd>
          <kbd className="rounded-md bg-muted px-1.5 py-0.5 font-mono text-xs">Enter</kbd> in
          the save box to store immediately.
        </p>
      </Section>

      <Section title="Plan">
        <p className="text-sm text-muted-foreground">
          Free keeps {FREE_ACTIVE_LIMIT} active things. Completed and archived
          do not count. Existing items are never hidden.
        </p>
        <p className="text-sm">
          Active now: <span className="tabular-nums">{active}</span>
          {settings.isPro ? " · Pro" : ""}
        </p>
        <Row label="Pro (preview)" hint="Unlimited active items. Billing is not connected yet.">
          <Switch
            checked={settings.isPro}
            onCheckedChange={(v) => patch({ isPro: v })}
            aria-label="Pro entitlement"
          />
        </Row>
      </Section>

      <Section title="Data">
        <p className="text-sm text-muted-foreground">
          Everything lives on this device. Export is a JSON snapshot you can keep.
        </p>
        <div className="flex flex-wrap gap-2">
          <Button
            variant="outline"
            onClick={() => {
              const blob = new Blob([exportSnapshot()], { type: "application/json" });
              const url = URL.createObjectURL(blob);
              const a = document.createElement("a");
              a.href = url;
              a.download = "second-memory.json";
              a.click();
              URL.revokeObjectURL(url);
            }}
          >
            Export
          </Button>
          <Button variant="outline" onClick={() => fileRef.current?.click()}>
            Import
          </Button>
          <input
            ref={fileRef}
            type="file"
            accept="application/json"
            className="hidden"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              try {
                const parsed = JSON.parse(await file.text()) as {
                  things?: unknown;
                  activities?: unknown;
                  settings?: unknown;
                };
                if (!Array.isArray(parsed.things)) throw new Error("Invalid file");
                importSnapshot({
                  things: parsed.things as never,
                  activities: parsed.activities as never,
                  settings: parsed.settings as never,
                });
                toast("Imported");
              } catch {
                toast("Could not import that file");
              } finally {
                e.target.value = "";
              }
            }}
          />
          <Button
            variant="outline"
            onClick={() => {
              loadExamples();
              toast("Example memories added");
            }}
          >
            Load examples
          </Button>
          <Button
            variant="outline"
            onClick={() => {
              if (confirm("Remove everything saved on this device?")) {
                resetAll();
                toast("Cleared");
              }
            }}
          >
            Clear this device
          </Button>
        </div>
      </Section>

      <Section title="Privacy">
        <p className="text-sm leading-relaxed text-muted-foreground">
          Capture never depends on a network. If enrichment is enabled, a
          compact extract of the item — not your whole library — is sent to
          the model so it can title, classify, and date it. Turn enrichment
          off and the rest of the app keeps working.
        </p>
      </Section>

      <Section title="Android app">
        <p className="text-sm text-muted-foreground">
          Native build for Android 8+. Share from any app, keep memories on
          the phone, and get quiet reminders. This is a signed sideload APK,
          not a Play Store listing.
        </p>
        <ApkDownloadButton />
        <p className="text-xs text-muted-foreground">
          Open the file on your phone, allow installs from this source when
          Android asks, then Share to Second Memory from Chrome, YouTube,
          Photos, or Messages.
        </p>
      </Section>

      <Section title="About">
        <p className="text-sm text-muted-foreground">
          Second Memory is a personal attention system. It is not a notes app,
          a todo list, or a chatbot. Save it now. Remember it when it matters.
        </p>
      </Section>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mb-10">
      <h2 className="mb-4 font-display text-xl font-medium tracking-tight">{title}</h2>
      <div className="flex flex-col gap-4">{children}</div>
      <Separator className="mt-8" />
    </section>
  );
}

function Row({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <p className="text-sm font-medium">{label}</p>
        {hint ? <p className="mt-1 text-xs text-muted-foreground">{hint}</p> : null}
      </div>
      {children}
    </div>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
