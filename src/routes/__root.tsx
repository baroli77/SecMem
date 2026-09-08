import { useEffect } from "react";
import {
  createRootRoute,
  HeadContent,
  Outlet,
  Scripts,
} from "@tanstack/react-router";
import { AuthProvider } from "@/lib/auth/provider";
import { PreviewHostBridge } from "@/components/preview-host-bridge";
import { AppShell } from "@/components/layout/app-shell";
import { useMemoryStore } from "@/lib/memory/store";
import appCss from "../styles.css?url";

const APP_NAME = "Second Memory";

function applyTheme(appearance: "system" | "light" | "dark") {
  const dark =
    appearance === "dark" ||
    (appearance !== "light" &&
      window.matchMedia("(prefers-color-scheme: dark)").matches);
  document.documentElement.classList.toggle("dark", dark);
}

function RootProviders() {
  const appearance = useMemoryStore((s) => s.settings.appearance);
  const hydrated = useMemoryStore((s) => s.hydrated);

  useEffect(() => {
    const result = useMemoryStore.persist.rehydrate();
    void Promise.resolve(result).then(() => {
      useMemoryStore.getState().setHydrated();
    });
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    applyTheme(appearance);
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => applyTheme(useMemoryStore.getState().settings.appearance);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, [appearance, hydrated]);

  return (
    <AuthProvider>
      <AppShell>
        <Outlet />
      </AppShell>
    </AuthProvider>
  );
}

export const Route = createRootRoute({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { title: APP_NAME },
      {
        name: "description",
        content: "Save it now. Remember it when it matters.",
      },
      { name: "theme-color", content: "#2c5c4f" },
    ],
    links: [
      { rel: "icon", type: "image/svg+xml", href: "/favicon.svg" },
      { rel: "stylesheet", href: appCss },
      { rel: "manifest", href: "/__grok/manifest.webmanifest" },
      { rel: "apple-touch-icon", href: "/__grok/icon-180.png" },
      { rel: "preconnect", href: "https://fonts.googleapis.com" },
      {
        rel: "preconnect",
        href: "https://fonts.gstatic.com",
        crossOrigin: "anonymous",
      },
      {
        rel: "stylesheet",
        href: "https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,500;0,6..72,600;1,6..72,400&family=Source+Sans+3:ital,wght@0,400;0,500;0,600;0,700;1,400&display=swap",
      },
    ],
  }),
  component: () => (
    <html lang="en" className="antialiased" suppressHydrationWarning>
      <head>
        <HeadContent />
      </head>
      <body>
        <PreviewHostBridge />
        <script
          dangerouslySetInnerHTML={{
            __html: `try{var raw=localStorage.getItem('second-memory');var a='system';if(raw){var p=JSON.parse(raw);a=(p.state&&p.state.settings&&p.state.settings.appearance)||'system';}var d=a==='dark'||(a!=='light'&&matchMedia('(prefers-color-scheme: dark)').matches);document.documentElement.classList.toggle('dark',d);}catch(e){}`,
          }}
        />
        <RootProviders />
        <Scripts />
      </body>
    </html>
  ),
});
