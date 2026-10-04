import { AlertCircle } from 'lucide-react';
import { Button } from './Button';

interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}

/** Consistent page-level error state with a retry action. */
export const ErrorState = ({
  title = 'Không thể tải dữ liệu',
  message,
  onRetry,
  retryLabel = 'Thử lại',
}: ErrorStateProps) => {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-[var(--color-error-border)] bg-[var(--color-error-bg)] px-6 py-12 text-center">
      <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-white text-[var(--color-error)]">
        <AlertCircle aria-hidden="true" size={24} />
      </div>
      <p className="text-base font-semibold text-[var(--color-neutral-900)]">{title}</p>
      <p className="mt-1 max-w-md text-sm leading-relaxed text-[var(--color-neutral-600)]">{message}</p>
      {onRetry && (
        <Button type="button" variant="secondary" className="mt-5" onClick={onRetry}>
          {retryLabel}
        </Button>
      )}
    </div>
  );
};
