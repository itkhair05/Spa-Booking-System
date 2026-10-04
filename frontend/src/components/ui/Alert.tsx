import type { ReactNode } from 'react';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

export type AlertTone = 'error' | 'success' | 'info';

const TONE_CLASSES: Record<AlertTone, string> = {
  error: 'bg-[var(--color-error-bg)] border-[var(--color-error-border)] text-[var(--color-error)]',
  success: 'bg-[var(--color-success-bg)] border-[var(--color-success-border)] text-[var(--color-success)]',
  info: 'bg-[var(--color-brand-50)] border-[var(--color-brand-200)] text-[var(--color-brand-700)]',
};

interface AlertProps {
  tone?: AlertTone;
  children: ReactNode;
  /** Extra actions (e.g. a retry button) rendered below the message. */
  actions?: ReactNode;
  className?: string;
}

/**
 * Inline page/section message banner. Always rendered with role="alert"
 * so screen readers announce it when it appears.
 */
export const Alert = ({ tone = 'error', children, actions, className = '' }: AlertProps) => {
  const Icon = tone === 'success' ? CheckCircle2 : AlertCircle;
  return (
    <div role="alert" className={`rounded-xl border p-4 text-sm ${TONE_CLASSES[tone]} ${className}`}>
      <div className="flex items-start gap-2.5">
        <Icon aria-hidden="true" size={18} className="mt-0.5 shrink-0" />
        <div className="min-w-0 flex-1 font-medium">{children}</div>
      </div>
      {actions && <div className="mt-3 pl-7">{actions}</div>}
    </div>
  );
};
