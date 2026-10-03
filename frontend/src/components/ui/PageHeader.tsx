import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

export function PageHeader({ title, description, action, className = '' }: PageHeaderProps) {
  return (
    <div className={`flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8 ${className}`}>
      <div>
        <h2 className="text-[1.5rem] font-bold text-[var(--color-neutral-900)] tracking-tight">
          {title}
        </h2>
        {description && (
          <p className="text-[0.9375rem] text-[var(--color-neutral-500)] mt-1">
            {description}
          </p>
        )}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}
