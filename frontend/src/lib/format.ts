/**
 * Shared display formatters (Vietnamese locale).
 * Always format with the browser's default locale? No — pin 'vi-VN'
 * so output is stable regardless of the user's system locale.
 */

const VND = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

export function formatCurrency(value: number): string {
  return VND.format(value);
}

/** 03/10/2026 */
export function formatDateDMY(iso: string): string {
  const d = new Date(iso);
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()}`;
}

/** 20:00 */
export function formatTimeHM(iso: string): string {
  const d = new Date(iso);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

/**
 * "20:00 – 21:00, 03/10/2026" when both ends share a calendar day,
 * otherwise "20:00 03/10/2026 – 08:00 04/10/2026".
 */
export function formatTimeRange(startIso: string, endIso: string): string {
  const start = new Date(startIso);
  const end = new Date(endIso);
  const sameDay =
    start.getFullYear() === end.getFullYear() &&
    start.getMonth() === end.getMonth() &&
    start.getDate() === end.getDate();

  if (sameDay) {
    return `${formatTimeHM(startIso)} – ${formatTimeHM(endIso)}, ${formatDateDMY(startIso)}`;
  }
  return `${formatTimeHM(startIso)} ${formatDateDMY(startIso)} – ${formatTimeHM(endIso)} ${formatDateDMY(endIso)}`;
}

/** "thứ Sáu, 03/10/2026" from a YYYY-MM-DD string (local calendar date, no TZ shift). */
export function formatDateLongFromYMD(ymd: string): string {
  const [y, m, d] = ymd.split('-').map(Number);
  if (!y || !m || !d) return ymd;
  const date = new Date(y, m - 1, d);
  const weekday = date.toLocaleDateString('vi-VN', { weekday: 'long' });
  return `${weekday}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
}
