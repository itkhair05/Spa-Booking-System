import type { ReactNode } from 'react';
import AppShell from './AppShell';

interface RestrictedAccessProps {
  shellTitle: string;
  icon: ReactNode;
  message: string;
}

/**
 * Restricted-access state shared by all OWNER-only management areas so STAFF
 * receives one consistent screen (same pattern as /feedback, /customers).
 */
const RestrictedAccess = ({ shellTitle, icon, message }: RestrictedAccessProps) => (
  <AppShell title={shellTitle}>
    <div className="max-w-2xl mx-auto py-16 text-center px-4">
      <div className="w-14 h-14 rounded-full bg-amber-50 text-amber-700 flex items-center justify-center mx-auto mb-4 border border-amber-200">
        {icon}
      </div>
      <h2 className="text-xl font-serif-title font-semibold text-stone-900 mb-2">Quyền truy cập hạn chế</h2>
      <p className="text-stone-600 text-sm leading-relaxed mb-6">{message}</p>
    </div>
  </AppShell>
);

export default RestrictedAccess;
