import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
}

/** Consistent empty state: explains why the area is empty and offers the next step. */
export const EmptyState = ({ icon, title, description, action }: EmptyStateProps) => {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-[var(--color-neutral-200)] bg-white px-6 py-14 text-center">
      {icon && (
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-brand-50)] text-[var(--color-brand-600)]">
          {icon}
        </div>
      )}
      <p className="text-base font-semibold text-[var(--color-neutral-800)]">{title}</p>
      <p className="mt-1 max-w-md text-sm leading-relaxed text-[var(--color-neutral-500)]">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
};
