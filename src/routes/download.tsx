import { createFileRoute, Link } from "@tanstack/react-router";
import { ApkInstallCard } from "@/components/apk-download";

export const Route = createFileRoute("/download")({
  component: DownloadPage,
});

function DownloadPage() {
  return (
    <div className="mx-auto flex min-h-dvh max-w-lg flex-col justify-center px-6 py-12">
      <p className="font-display text-xl font-medium tracking-tight">
        Second Memory
      </p>
      <h1 className="mt-6 font-display text-4xl font-medium tracking-tight">
        Install the Android app
      </h1>
      <p className="mt-3 text-base leading-relaxed text-muted-foreground">
        This is the native phone build. Share a link or screenshot from any
        app, keep it on the device, and get a quiet reminder when it matters.
      </p>
      <ApkInstallCard className="mt-8" />
      <p className="mt-8 text-sm text-muted-foreground">
        Want to try it in the browser first?{" "}
        <Link to="/" className="text-foreground underline underline-offset-4">
          Open Today
        </Link>
      </p>
    </div>
  );
}
