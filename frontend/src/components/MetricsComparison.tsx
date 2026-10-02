import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { CompareResponse } from '../types/simulation';
import { fmt } from '../lib/timeline';

interface Props {
  compare: CompareResponse;
}

const ROWS: { key: 'averageWaitingTime' | 'averageTurnaroundTime' | 'averageResponseTime' | 'contextSwitches'; label: string }[] = [
  { key: 'averageWaitingTime', label: 'Avg Waiting' },
  { key: 'averageTurnaroundTime', label: 'Avg Turnaround' },
  { key: 'averageResponseTime', label: 'Avg Response' },
  { key: 'contextSwitches', label: 'Context Switches' },
];

const COLORS = { RR: '#38bdf8', SRTF: '#f59e0b', ADAPTIVE: '#34d399' };

/**
 * Side-by-side RR / SRTF / AdaptiveOS metrics: a plain table plus bar charts and a CPU-utilization
 * comparison. Deliberately presents numbers without labeling any policy "best" or "winner".
 */
export default function MetricsComparison({ compare }: Props) {
  const { pureRr, pureSrtf, adaptive } = compare;
  const table = ROWS.map((r) => ({ label: r.label, RR: pureRr.metrics[r.key], SRTF: pureSrtf.metrics[r.key], ADAPTIVE: adaptive.metrics[r.key] }));
  const util = [
    { name: 'Pure RR', value: pureRr.metrics.cpuUtilization, fill: COLORS.RR },
    { name: 'Pure SRTF', value: pureSrtf.metrics.cpuUtilization, fill: COLORS.SRTF },
    { name: 'AdaptiveOS', value: adaptive.metrics.cpuUtilization, fill: COLORS.ADAPTIVE },
  ];

  return (
    <div className="space-y-6">
      <p className="text-sm text-slate-400">These results are measured from the same workload under three scheduling modes with identical parameters.</p>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="text-xs uppercase tracking-wider text-slate-500">
            <tr>
              <th className="py-2 pr-4 font-medium"> </th>
              <th className="py-2 pr-4 font-medium text-rr">Pure RR</th>
              <th className="py-2 pr-4 font-medium text-srtf">Pure SRTF</th>
              <th className="py-2 pr-4 font-medium text-adaptive">AdaptiveOS</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-700 font-mono">
            {table.map((row) => (
              <tr key={row.label}>
                <td className="py-2 pr-4 font-sans text-slate-300">{row.label}</td>
                <td className="py-2 pr-4">{fmt(row.RR)}</td>
                <td className="py-2 pr-4">{fmt(row.SRTF)}</td>
                <td className="py-2 pr-4">{fmt(row.ADAPTIVE)}</td>
              </tr>
            ))}
            <tr>
              <td className="py-2 pr-4 font-sans text-slate-300">CPU Utilization</td>
              <td className="py-2 pr-4">{fmt(pureRr.metrics.cpuUtilization)}%</td>
              <td className="py-2 pr-4">{fmt(pureSrtf.metrics.cpuUtilization)}%</td>
              <td className="py-2 pr-4">{fmt(adaptive.metrics.cpuUtilization)}%</td>
            </tr>
            <tr>
              <td className="py-2 pr-4 font-sans text-slate-300">Policy Switches</td>
              <td className="py-2 pr-4">-</td>
              <td className="py-2 pr-4">-</td>
              <td className="py-2 pr-4">{adaptive.metrics.policySwitches}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {(['averageWaitingTime', 'averageTurnaroundTime', 'averageResponseTime', 'contextSwitches'] as const).map((key) => (
          <div key={key} className="h-56">
            <p className="mb-1 text-xs font-medium text-slate-400">{ROWS.find((r) => r.key === key)?.label}</p>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={[{ name: ROWS.find((r) => r.key === key)?.label, RR: pureRr.metrics[key], SRTF: pureSrtf.metrics[key], ADAPTIVE: adaptive.metrics[key] }]} margin={{ top: 4, right: 8, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="#1c2632" vertical={false} />
                <XAxis dataKey="name" tick={false} stroke="#2a3745" />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} stroke="#2a3745" width={36} />
                <Tooltip contentStyle={{ background: '#080b10', border: '1px solid #2a3745', fontSize: 12 }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="RR" fill={COLORS.RR} name="Pure RR" radius={[3, 3, 0, 0]} />
                <Bar dataKey="SRTF" fill={COLORS.SRTF} name="Pure SRTF" radius={[3, 3, 0, 0]} />
                <Bar dataKey="ADAPTIVE" fill={COLORS.ADAPTIVE} name="AdaptiveOS" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        ))}
        <div className="h-56">
          <p className="mb-1 text-xs font-medium text-slate-400">CPU Utilization</p>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={util} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 8 }}>
              <CartesianGrid stroke="#1c2632" horizontal={false} />
              <XAxis type="number" domain={[0, 100]} tick={{ fill: '#94a3b8', fontSize: 11 }} stroke="#2a3745" unit="%" />
              <YAxis type="category" dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 11 }} stroke="#2a3745" width={80} />
              <Tooltip contentStyle={{ background: '#080b10', border: '1px solid #2a3745', fontSize: 12 }} formatter={(v: number) => [`${v.toFixed(2)}%`, 'utilization']} />
              <Bar dataKey="value" radius={[0, 3, 3, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
