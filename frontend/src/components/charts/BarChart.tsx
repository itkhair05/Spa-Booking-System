export interface BarChartItem {
  label: string;
  value: number;
  /** Exact tooltip text; falls back to "{label}: {value}". */
  title?: string;
}

interface BarChartProps {
  data: BarChartItem[];
  color?: string;
  ariaLabel: string;
}

export function BarChart({ data, color = '#465d4c', ariaLabel }: BarChartProps) {
  if (data.length === 0) return null;
  const max = Math.max(...data.map((d) => d.value), 1);

  return (
    <div className="space-y-3.5" role="img" aria-label={ariaLabel}>
      {data.map((d) => (
        <div key={d.label}>
          <div className="flex items-baseline justify-between gap-3 text-xs mb-1">
            <span className="text-stone-600 truncate" title={d.label}>
              {d.label}
            </span>
            <span className="font-mono font-semibold text-stone-900 shrink-0 tabular-nums">
              {d.value}
            </span>
          </div>
          <div className="h-2.5 rounded-full bg-stone-100 overflow-hidden">
            <div
              className="h-full rounded-full"
              style={{ width: `${Math.max((d.value / max) * 100, d.value > 0 ? 3 : 0)}%`, backgroundColor: color }}
              title={d.title ?? `${d.label}: ${d.value}`}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
