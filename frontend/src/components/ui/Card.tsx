import type { ReactNode } from 'react';

interface CardProps {
  children?: ReactNode;
  className?: string;
}

export function Card({ children, className = '' }: CardProps) {
  return (
    <div className={`bg-white rounded-xl border border-[var(--color-neutral-200)] shadow-[var(--shadow-sm)] ${className}`}>
      {children}
    </div>
  );
}

export function CardHeader({ children, className = '' }: CardProps) {
  return (
    <div className={`px-6 py-5 border-b border-[var(--color-neutral-200)] ${className}`}>
      {children}
    </div>
  );
}

export function CardTitle({ children, className = '' }: CardProps) {
  return (
    <h3 className={`text-[1.0625rem] font-semibold text-[var(--color-neutral-900)] leading-none ${className}`}>
      {children}
    </h3>
  );
}

export function CardContent({ children, className = '' }: CardProps) {
  return (
    <div className={`p-6 ${className}`}>
      {children}
    </div>
  );
}

export function CardFooter({ children, className = '' }: CardProps) {
  return (
    <div className={`px-6 py-4 border-t border-[var(--color-neutral-200)] bg-[var(--color-neutral-50)] rounded-b-xl flex items-center ${className}`}>
      {children}
    </div>
  );
}
