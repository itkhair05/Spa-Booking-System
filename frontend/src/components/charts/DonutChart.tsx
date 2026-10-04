export interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

interface DonutChartProps {
  segments: DonutSegment[];
  ariaLabel: string;
  /** Label rendered under the total in the center of the ring. */
  centerLabel?: string;
}

const SIZE = 140;
const CENTER = SIZE / 2;
const R = 44;
const STROKE = 18;
const CIRCUMFERENCE = 2 * Math.PI * R;

export function DonutChart({ segments, ariaLabel, centerLabel }: DonutChartProps) {
  const total = segments.reduce((sum, s) => sum + s.value, 0);
  if (total <= 0) return null;

  const visible = segments.filter((s) => s.value > 0);
  const dashes = visible.map((s) => (s.value / total) * CIRCUMFERENCE);
  const offsets = dashes.map((_, i) => dashes.slice(0, i).reduce((a, b) => a + b, 0));

  return (
    <svg viewBox={`0 0 ${SIZE} ${SIZE}`} className="w-36 h-36 shrink-0" role="img" aria-label={ariaLabel}>
      <title>{ariaLabel}</title>
      {visible.map((s, i) => {
        const fraction = s.value / total;
        return (
          <circle
            key={s.label}
            cx={CENTER}
            cy={CENTER}
            r={R}
            fill="none"
            stroke={s.color}
            strokeWidth={STROKE}
            strokeDasharray={`${dashes[i]} ${CIRCUMFERENCE - dashes[i]}`}
            strokeDashoffset={-offsets[i]}
            transform={`rotate(-90 ${CENTER} ${CENTER})`}
          >
            <title>{`${s.label}: ${s.value} (${Math.round(fraction * 100)}%)`}</title>
          </circle>
        );
      })}
      <text x={CENTER} y={CENTER - 1} textAnchor="middle" fontSize={20} fontWeight={700} fill="#44403c" fontFamily="ui-monospace, monospace">
        {total}
      </text>
      {centerLabel && (
        <text x={CENTER} y={CENTER + 13} textAnchor="middle" fontSize={8.5} fill="#a8a29e">
          {centerLabel}
        </text>
      )}
    </svg>
  );
}
