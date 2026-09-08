import {
  Bookmark,
  BookOpen,
  Briefcase,
  Calendar,
  CheckSquare,
  CircleDashed,
  Lightbulb,
  MapPin,
  Play,
  ShoppingBag,
  User,
  UtensilsCrossed,
} from "lucide-react";
import type { Category } from "@/lib/memory/types";
import { CATEGORY_LABEL } from "@/lib/memory/format";
import { cn } from "@/lib/utils";

const ICONS: Record<Category, typeof BookOpen> = {
  READ: BookOpen,
  WATCH: Play,
  BUY: ShoppingBag,
  DO: CheckSquare,
  WORK: Briefcase,
  PERSONAL: User,
  PLACE: MapPin,
  IDEA: Lightbulb,
  REFERENCE: Bookmark,
  EVENT: Calendar,
  RECIPE: UtensilsCrossed,
  UNKNOWN: CircleDashed,
};

export function CategoryIcon({
  category,
  className,
}: {
  category: Category;
  className?: string;
}) {
  const Icon = ICONS[category] ?? CircleDashed;
  return <Icon className={cn("size-4", className)} aria-hidden />;
}

export function CategoryMark({
  category,
  withLabel = true,
  className,
}: {
  category: Category;
  withLabel?: boolean;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 text-[11px] font-medium uppercase tracking-wider text-muted-foreground",
        className,
      )}
    >
      <CategoryIcon category={category} className="size-3.5" />
      {withLabel ? CATEGORY_LABEL[category] : null}
    </span>
  );
}
