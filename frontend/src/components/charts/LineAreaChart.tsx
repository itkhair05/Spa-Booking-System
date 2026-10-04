import { useId } from 'react';

export interface LineAreaChartPoint {
  /** Short x-axis label, e.g. "04/10". */
  label: string;
  value: number;
  /** Exact tooltip text; falls back to "{label}: {value}". */
  title?: string;
}

interface LineAreaChartProps {
  data: LineAreaChartPoint[];
  color?: string;
  ariaLabel: string;
}

const W = 560; // 4:1 wide aspect ratio for balanced dashboard fit
const H = 140;
const PAD = { top: 18, right: 20, bottom: 28, left: 32 };

export function LineAreaChart({ data, color = '#465d4c', ariaLabel }: LineAreaChartProps) {
  const gradientId = useId();
  if (data.length === 0) return null;

  const max = Math.max(...data.map((d) => d.value), 1);
  const innerW = W - PAD.left - PAD.right;
  const innerH = H - PAD.top - PAD.bottom;
  const x = (i: number) => PAD.left + (i / Math.max(data.length - 1, 1)) * innerW;
  const y = (v: number) => PAD.top + (1 - v / max) * innerH;

  const line = data
    .map((d, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(d.value).toFixed(1)}`)
    .join(' ');
  const area = `${line} L${x(data.length - 1).toFixed(1)},${(H - PAD.bottom).toFixed(1)} L${PAD.left},${(H - PAD.bottom).toFixed(1)} Z`;

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-full block" role="img" aria-label={ariaLabel}>
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.22" />
          <stop offset="100%" stopColor={color} stopOpacity="0.02" />
        </linearGradient>
      </defs>
      <title>{ariaLabel}</title>
      {[0, 0.5, 1].map((t) => {
        const gy = y(max * t);
        return (
          <g key={t}>
            <line
              x1={PAD.left}
              x2={W - PAD.right}
              y1={gy}
              y2={gy}
              stroke="#e7e5e4"
              strokeDasharray={t === 0 ? undefined : '3 4'}
            />
            <text x={PAD.left - 6} y={gy + 3} fontSize={9} fill="#a8a29e" textAnchor="end">
              {Math.round(max * t)}
            </text>
          </g>
        );
      })}
      <path d={area} fill={`url(#${gradientId})`} />
      <path
        d={line}
        fill="none"
        stroke={color}
        strokeWidth={2}
        strokeLinejoin="round"
        strokeLinecap="round"
      />
      {data.map((d, i) => (
        <g key={`${d.label}-${i}`}>
          <circle cx={x(i)} cy={y(d.value)} r={10} fill="transparent" className="cursor-pointer">
            <title>{d.title ?? `${d.label}: ${d.value}`}</title>
          </circle>
          <circle cx={x(i)} cy={y(d.value)} r={3.5} fill="#fff" stroke={color} strokeWidth={2}>
            <title>{d.title ?? `${d.label}: ${d.value}`}</title>
          </circle>
          <text x={x(i)} y={H - 8} fontSize={9.5} fill="#78716c" textAnchor="middle">
            {d.label}
          </text>
        </g>
      ))}
    </svg>
  );
}
