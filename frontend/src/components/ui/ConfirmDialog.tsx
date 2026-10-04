import { useEffect, useId, useRef, type ReactNode } from 'react';
import { Button } from './Button';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  /** Disables both actions and shows a busy label while the action runs. */
  busy?: boolean;
  /** Error message shown inside the dialog when the action fails. */
  error?: string | null;
  tone?: 'danger' | 'primary';
}

/**
 * Accessible confirmation dialog: focus moves to the safest action,
 * Escape/backdrop cancel, Tab is trapped inside while open.
 */
export const ConfirmDialog = ({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel = 'Hủy',
  onConfirm,
  onCancel,
  busy = false,
  error = null,
  tone = 'danger',
}: ConfirmDialogProps) => {
  const titleId = useId();
  const descId = useId();
  const cancelRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    // Focus the cancel (safe) action when the dialog opens.
    cancelRef.current?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !busy) {
        e.preventDefault();
        onCancel();
        return;
      }
      if (e.key !== 'Tab') return;

      // Light focus trap across the dialog's focusable elements.
      const panel = panelRef.current;
      if (!panel) return;
      const focusable = panel.querySelectorAll<HTMLElement>(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement as HTMLElement | null;

      if (e.shiftKey && (active === first || !panel.contains(active))) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && active === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, busy, onCancel]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descId}
    >
      <div
        className="absolute inset-0 bg-[var(--color-neutral-900)]/40"
        aria-hidden="true"
        onClick={busy ? undefined : onCancel}
      />
      <div ref={panelRef} className="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-[var(--shadow-md)]">
        <h3 id={titleId} className="text-lg font-semibold text-[var(--color-neutral-900)]">
          {title}
        </h3>
        <div id={descId} className="mt-2 text-sm leading-relaxed text-[var(--color-neutral-600)]">
          {description}
        </div>

        {error && (
          <p role="alert" className="mt-4 rounded-lg border border-[var(--color-error-border)] bg-[var(--color-error-bg)] p-3 text-sm font-medium text-[var(--color-error)]">
            {error}
          </p>
        )}

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:gap-3">
          <Button ref={cancelRef} type="button" variant="secondary" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button
            type="button"
            variant={tone === 'danger' ? 'danger' : 'primary'}
            onClick={onConfirm}
            disabled={busy}
            aria-busy={busy}
          >
            {busy ? 'Đang xử lý...' : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
};
