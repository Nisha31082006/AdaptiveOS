import type { Metrics } from '../types/simulation';
import { fmt } from '../lib/timeline';

interface Props {
  metrics: Metrics;
}

const CARDS: { key: keyof Metrics; label: string; unit: string; digits?: number }[] = [
  { key: 'averageWaitingTime', label: 'Avg Waiting Time', unit: 'ticks' },
  { key: 'averageTurnaroundTime', label: 'Avg Turnaround', unit: 'ticks' },
  { key: 'averageResponseTime', label: 'Avg Response Time', unit: 'ticks' },
  { key: 'contextSwitches', label: 'Context Switches', unit: '', digits: 0 },
  { key: 'cpuUtilization', label: 'CPU Utilization', unit: '%' },
  { key: 'maxWaitingTime', label: 'Max Waiting (1 process)', unit: 'ticks', digits: 0 },
];

/** Headline metrics for one run, straight from MetricsCalculator via the API. No ranking or "best" label. */
export default function MetricsCards({ metrics }: Props) {
  return (
    <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {CARDS.map((c) => (
        <div key={c.key} className="panel p-3">
          <dt className="text-[11px] uppercase tracking-wide text-slate-500">{c.label}</dt>
          <dd className="mt-1 font-mono text-xl font-semibold text-slate-100">
            {fmt(metrics[c.key] as number, c.digits ?? 2)}
            <span className="ml-1 text-xs font-normal text-slate-500">{c.unit}</span>
          </dd>
        </div>
      ))}
      <div className="panel col-span-2 p-3 sm:col-span-3 lg:col-span-6">
        <dt className="text-[11px] uppercase tracking-wide text-slate-500">Policy usage (this run)</dt>
        <dd className="mt-1 font-mono text-sm">
          <span className="text-rr">RR: {metrics.rrDuration} ticks</span>
          <span className="mx-2 text-slate-600">·</span>
          <span className="text-srtf">SRTF: {metrics.srtfDuration} ticks</span>
          <span className="mx-2 text-slate-600">·</span>
          <span className="text-slate-300">{metrics.policySwitches} switch{metrics.policySwitches === 1 ? '' : 'es'}</span>
        </dd>
      </div>
    </dl>
  );
}
