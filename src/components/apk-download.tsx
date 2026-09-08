import type { ReactNode } from "react";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export const APK_HREF = "/second-memory.apk";
export const APK_FILENAME = "SecondMemory-1.1.2.apk";

export function ApkAnchor({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  return (
    <a
      href={APK_HREF}
      download={APK_FILENAME}
      target="_blank"
      rel="noreferrer"
      className={className}
    >
      {children}
    </a>
  );
}

export function ApkBanner() {
  return (
    <ApkAnchor className="block bg-primary px-4 py-2.5 text-center text-sm font-medium text-primary-foreground hover:opacity-95">
      Download the Android APK · 12 MB — tap here
    </ApkAnchor>
  );
}

export function ApkDownloadButton({
  size = "lg",
  className,
}: {
  size?: "default" | "sm" | "lg";
  className?: string;
}) {
  return (
    <Button asChild size={size} className={className}>
      <a
        href={APK_HREF}
        download={APK_FILENAME}
        target="_blank"
        rel="noreferrer"
      >
        <Download className="size-4" />
        Download APK
      </a>
    </Button>
  );
}

export function ApkInstallCard({ className }: { className?: string }) {
  return (
    <aside
      className={cn(
        "rounded-2xl border border-border bg-card p-5 shadow-sm",
        className,
      )}
    >
      <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
        Android
      </p>
      <h2 className="mt-1 font-display text-2xl font-medium tracking-tight">
        Get it on your phone
      </h2>
      <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
        Native build 1.1.0 for Android 8+. Share from any app. Install this
        file — it is not on the Play Store.
      </p>
      <ApkDownloadButton className="mt-4" />
      <ol className="mt-4 list-decimal space-y-1 pl-4 text-xs text-muted-foreground">
        <li>Tap Download APK.</li>
        <li>Open the file on the phone.</li>
        <li>Allow installs from this source if Android asks.</li>
      </ol>
    </aside>
  );
}
