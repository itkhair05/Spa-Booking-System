import api from './axios';

export type ExportKind = 'bookings' | 'customers' | 'revenue';

const FALLBACK_NAMES: Record<ExportKind, string> = {
  bookings: 'lich-hen',
  customers: 'khach-hang',
  revenue: 'doanh-thu',
};

/**
 * Downloads an OWNER-only .xlsx report. Tenant context comes from the
 * authenticated session server-side; no tenant id is sent from the client.
 */
export async function downloadExport(kind: ExportKind): Promise<void> {
  const response = await api.get(`/exports/${kind}`, { responseType: 'blob' });

  const disposition: string | undefined = response.headers?.['content-disposition'];
  const match = disposition?.match(/filename="?([^";]+)"?/);
  const filename = match?.[1] ?? `${FALLBACK_NAMES[kind]}.xlsx`;

  const url = URL.createObjectURL(response.data as Blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
