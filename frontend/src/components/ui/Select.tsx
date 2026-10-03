import { forwardRef, useId } from 'react';
import type { SelectHTMLAttributes } from 'react';

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: { value: string | number; label: string }[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className = '', label, error, id, options, ...props }, ref) => {
    const generatedId = useId();
    const selectId = id || generatedId;
    
    const baseStyles = 'flex w-full rounded-lg border bg-white px-3 py-2 text-sm text-[var(--color-neutral-900)] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-brand-100)] focus-visible:border-[var(--color-brand-500)] disabled:cursor-not-allowed disabled:bg-[var(--color-neutral-100)] disabled:text-[var(--color-neutral-500)]';
    const errorStyles = error ? 'border-[var(--color-error-border)] focus-visible:ring-[var(--color-error-bg)] focus-visible:border-[var(--color-error)]' : 'border-[var(--color-neutral-200)]';
    
    return (
      <div className="flex flex-col gap-1.5 w-full">
        {label && (
          <label htmlFor={selectId} className="text-[0.8125rem] font-semibold text-[var(--color-neutral-700)]">
            {label}
          </label>
        )}
        <select
          ref={ref}
          id={selectId}
          className={`${baseStyles} ${errorStyles} ${className}`}
          aria-invalid={!!error}
          aria-describedby={error ? `${selectId}-error` : undefined}
          {...props}
        >
          <option value="" disabled>Select an option</option>
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        {error && (
          <span id={`${selectId}-error`} className="text-xs font-medium text-[var(--color-error)]">
            {error}
          </span>
        )}
      </div>
    );
  }
);
Select.displayName = 'Select';
