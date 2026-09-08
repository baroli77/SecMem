import { createFileRoute } from "@tanstack/react-router";
import { OnboardingView } from "@/components/onboarding-view";

export const Route = createFileRoute("/onboarding")({
  component: OnboardingView,
});
