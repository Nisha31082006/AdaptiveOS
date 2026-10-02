import { useMemo, useState } from 'react';
import type { EngineEvent } from '../types/simulation';

interface Props {
  events: EngineEvent[];
  /** Current playback time; events after it are hidden when "up to current time" is on. */
  time?: number;
}

/** Scrollable, filterable engine event log (process, event type, policy, adaptive-only). */
export default function EventLog({ events, time }: Props) {
  const [process, setProcess] = useState('ALL');
  const [type, setType] = useState('ALL');
  const [policy, setPolicy] = useState('ALL');
  const [adaptiveOnly, setAdaptiveOnly] = useState(false);
  const [upToNow, setUpToNow] = useState(time !== undefined);

  const processes = useMemo(() => Array.from(new Set(events.map((e) => e.processId).filter((p): p is string => !!p))).sort((a, b) => Number(a.slice(1)) - Number(b.slice(1))), [events]);
  const types = useMemo(() => Array.from(new Set(events.map((e) => e.type))).sort(), [events]);

  const shown = events.filter(
    (e) =>
      (process === 'ALL' || e.processId === process) &&
      (type === 'ALL' || e.type === type) &&
      (policy === 'ALL' || e.policy === policy) &&
      (!adaptiveOnly || e.adaptive) &&
      (!upToNow || time === undefined || e.time <= time),
  );

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
        <h3 className="panel-title">Event log <span className="font-normal normal-case text-slate-500">({shown.length} of {events.length})</span></h3>
        <div className="flex flex-wrap items-end gap-3 text-xs">
          <label>
            <span className="label">Process</span>
            <select className="input py-1" value={process} onChange={(e) => setProcess(e.target.value)}>
              <option value="ALL">All</option>
              {processes.map((p) => <option key={p}>{p}</option>)}
            </select>
          </label>
          <label>
            <span className="label">Event type</span>
            <select className="input py-1" value={type} onChange={(e) => setType(e.target.value)}>
              <option value="ALL">All</option>
              {types.map((t) => <option key={t}>{t}</option>)}
            </select>
          </label>
          <label>
            <span className="label">Policy</span>
            <select className="input py-1" value={policy} onChange={(e) => setPolicy(e.target.value)}>
              <option value="ALL">All</option>
              <option>RR</option>
              <option>SRTF</option>
            </select>
          </label>
          <label className="flex items-center gap-1.5 pb-1.5"><input type="checkbox" checked={adaptiveOnly} onChange={(e) => setAdaptiveOnly(e.target.checked)} /> Adaptive events only</label>
          {time !== undefined && (
            <label className="flex items-center gap-1.5 pb-1.5"><input type="checkbox" checked={upToNow} onChange={(e) => setUpToNow(e.target.checked)} /> Up to current time</label>
          )}
        </div>
      </div>
      {shown.length === 0 ? (
        <p className="rounded-md border border-dashed border-ink-600 p-6 text-center text-sm text-slate-500">No events match the current filters.</p>
      ) : (
        <ol className="max-h-72 overflow-y-auto rounded-md border border-ink-700 bg-ink-950/70 p-2 font-mono text-[12.5px] leading-relaxed" aria-label="Simulation events">
          {shown.map((e, i) => (
            <li key={i} className={`flex gap-2 px-1 ${e.type === 'POLICY_SWITCH' ? 'rounded bg-emerald-900/30 text-emerald-200' : e.adaptive ? 'text-emerald-300/80' : 'text-slate-300'}`}>
              <span className="w-12 shrink-0 text-slate-500">[t={e.time}]</span>
              <span className={`w-8 shrink-0 ${e.policy === 'RR' ? 'text-rr' : 'text-srtf'}`}>{e.policy}</span>
              <span>{e.message}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
