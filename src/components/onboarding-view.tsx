import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useMemoryStore } from "@/lib/memory/store";
import { ApkDownloadButton } from "@/components/apk-download";

const SLIDES = [
  {
    kicker: "Capture",
    title: "Share anything you do not want to lose.",
    body: "A link, a screenshot, a half-formed reminder. Save it in a couple of seconds. No folders. No tags to invent.",
    art: "capture",
  },
  {
    kicker: "Understand",
    title: "It figures out what the thing is.",
    body: "Read, watch, buy, do, cook, go. Titles, dates, and a sensible time to come back are inferred so you do not organise a second inbox.",
    art: "understand",
  },
  {
    kicker: "Resurface",
    title: "It brings the thing back when it matters.",
    body: "Evening for articles. Payday for products. Tomorrow morning for the spreadsheet. You can forget on purpose.",
    art: "resurface",
  },
] as const;

export function OnboardingView() {
  const [step, setStep] = useState(0);
  const navigate = useNavigate();
  const completeOnboarding = useMemoryStore((s) => s.completeOnboarding);
  const setCaptureOpen = useMemoryStore((s) => s.setCaptureOpen);
  const slide = SLIDES[step];
  const last = step === SLIDES.length - 1;

  function next() {
    if (!last) {
      setStep((s) => s + 1);
      return;
    }
    completeOnboarding();
    void navigate({ to: "/" });
    setTimeout(() => setCaptureOpen(true), 400);
  }

  return (
    <div className="flex min-h-dvh flex-col bg-background px-6 py-10 text-foreground md:px-16">
      <p className="font-display text-xl font-medium tracking-tight">Second Memory</p>
      <div className="mx-auto flex w-full max-w-xl flex-1 flex-col justify-center py-12">
        <SlideArt kind={slide.art} />
        <p className="mt-8 text-xs font-medium uppercase tracking-[0.18em] text-primary">
          {slide.kicker}
        </p>
        <h1 className="mt-4 font-display text-4xl font-medium tracking-tight md:text-5xl">
          {slide.title}
        </h1>
        <p className="mt-5 max-w-md text-base leading-relaxed text-muted-foreground">
          {slide.body}
        </p>
        <div className="mt-10 flex flex-wrap items-center gap-3">
          <Button size="lg" onClick={next}>
            {last ? "Start saving" : "Continue"}
            <ArrowRight className="size-4" />
          </Button>
          <ApkDownloadButton size="lg" />
          {!last ? (
            <Button
              variant="ghost"
              onClick={() => {
                completeOnboarding();
                void navigate({ to: "/" });
              }}
            >
              Skip
            </Button>
          ) : null}
        </div>
        <ol className="mt-12 flex gap-2" aria-label="Onboarding progress">
          {SLIDES.map((s, i) => (
            <li
              key={s.kicker}
              className={`h-1.5 w-8 rounded-full ${i === step ? "bg-primary" : "bg-muted"}`}
            />
          ))}
        </ol>
      </div>
      <p className="text-xs text-muted-foreground">
        Save it now. Remember it when it matters.
      </p>
    </div>
  );
}

function SlideArt({ kind }: { kind: (typeof SLIDES)[number]["art"] }) {
  return (
    <div className="relative h-36 w-full max-w-sm overflow-hidden rounded-2xl bg-card shadow-(--shadow-border)">
      {kind === "capture" ? (
        <>
          <div className="absolute left-6 top-8 h-20 w-28 rotate-[-8deg] rounded-xl bg-muted" />
          <div className="absolute left-16 top-10 h-24 w-40 rounded-xl bg-primary/90 p-3 text-primary-foreground">
            <div className="h-2 w-16 rounded-full bg-primary-foreground/70" />
            <div className="mt-2 h-1.5 w-24 rounded-full bg-primary-foreground/40" />
            <div className="mt-1.5 h-1.5 w-12 rounded-full bg-primary-foreground/30" />
          </div>
        </>
      ) : null}
      {kind === "understand" ? (
        <div className="absolute inset-x-6 top-8 rounded-xl bg-muted p-4">
          <div className="h-2.5 w-32 rounded-full bg-foreground/70" />
          <div className="mt-2 h-1.5 w-full rounded-full bg-foreground/20" />
          <div className="mt-1.5 h-1.5 w-4/5 rounded-full bg-foreground/15" />
          <div className="mt-4 flex gap-2">
            <span className="rounded-full bg-primary px-2.5 py-1 text-[10px] font-medium uppercase tracking-wider text-primary-foreground">
              Read
            </span>
            <span className="rounded-full bg-card px-2.5 py-1 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
              Tonight
            </span>
          </div>
        </div>
      ) : null}
      {kind === "resurface" ? (
        <>
          <div className="absolute left-8 top-8 size-20 rounded-full border-[6px] border-muted">
            <div className="absolute left-1/2 top-1/2 h-6 w-0.5 origin-bottom -translate-x-1/2 -translate-y-full rotate-45 bg-primary" />
            <div className="absolute left-1/2 top-1/2 h-4 w-0.5 origin-bottom -translate-x-1/2 -translate-y-full rotate-[-20deg] bg-foreground" />
          </div>
          <div className="absolute right-6 top-10 w-36 rounded-xl bg-primary p-3 text-primary-foreground">
            <p className="text-[11px] font-medium leading-snug">Worth reading tonight?</p>
            <p className="mt-1 text-[10px] opacity-80">Open · Later · Done</p>
          </div>
        </>
      ) : null}
    </div>
  );
}
