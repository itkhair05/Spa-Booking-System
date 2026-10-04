import { forwardRef, useId } from 'react';
import type { InputHTMLAttributes } from 'react';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  /** Helper text below the field. Hidden while an error is shown. */
  hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className = '', label, error, hint, id, ...props }, ref) => {
    const generatedId = useId();
    const inputId = id || generatedId;
    const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;

    const baseStyles = 'flex w-full rounded-lg border bg-white px-3 py-2 text-sm text-[var(--color-neutral-900)] placeholder:text-[var(--color-neutral-400)] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-brand-200)] focus-visible:border-[var(--color-brand-500)] disabled:cursor-not-allowed disabled:bg-[var(--color-neutral-100)] disabled:text-[var(--color-neutral-500)] max-sm:min-h-11';
    const errorStyles = error ? 'border-[var(--color-error-border)] focus-visible:ring-[var(--color-error-bg)] focus-visible:border-[var(--color-error)]' : 'border-[var(--color-neutral-200)]';
    
    return (
      <div className="flex flex-col gap-1.5 w-full">
        {label && (
          <label htmlFor={inputId} className="text-[0.8125rem] font-semibold text-[var(--color-neutral-700)]">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={`${baseStyles} ${errorStyles} ${className}`}
          aria-invalid={!!error}
          aria-describedby={describedBy}
          {...props}
        />
        {error && (
          <span id={`${inputId}-error`} className="text-xs font-medium text-[var(--color-error)]">
            {error}
          </span>
        )}
        {!error && hint && (
          <span id={`${inputId}-hint`} className="text-xs text-[var(--color-neutral-500)]">
            {hint}
          </span>
        )}
      </div>
    );
  }
);
Input.displayName = 'Input';
