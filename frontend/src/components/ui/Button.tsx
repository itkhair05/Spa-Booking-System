import { forwardRef } from 'react';
import type { ButtonHTMLAttributes } from 'react';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'danger-outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
}

const variantStyles = {
  primary: 'bg-[var(--color-brand-600)] text-white hover:bg-[var(--color-brand-700)] border border-transparent',
  secondary: 'bg-white text-[var(--color-neutral-700)] border border-[var(--color-neutral-200)] hover:bg-[var(--color-neutral-50)]',
  danger: 'bg-[var(--color-error)] text-white hover:bg-[#b91c1c] border border-transparent', // b91c1c is a darker red
  'danger-outline': 'bg-white text-[var(--color-error)] border border-[var(--color-error-border)] hover:bg-[var(--color-error-bg)]',
  ghost: 'bg-transparent text-[var(--color-neutral-700)] hover:bg-[var(--color-neutral-100)] border border-transparent',
};

const sizeStyles = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
  lg: 'px-6 py-3 text-base',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = '', variant = 'primary', size = 'md', disabled, children, ...props }, ref) => {
    const baseStyles = 'inline-flex items-center justify-center gap-1.5 font-medium rounded-lg whitespace-nowrap cursor-pointer transition-[background-color,border-color,color,transform] duration-150 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-brand-500)] focus-visible:ring-offset-1 disabled:opacity-60 disabled:pointer-events-none max-sm:min-h-11';
    const classes = `${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`;

    return (
      <button ref={ref} disabled={disabled} className={classes} {...props}>
        {children}
      </button>
    );
  }
);
Button.displayName = 'Button';
