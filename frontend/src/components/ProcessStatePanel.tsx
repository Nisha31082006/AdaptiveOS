import { useState } from 'react';
import type { ProcessResult, ProcessState, Snapshot } from '../types/simulation';
import { colorForProcess } from '../lib/timeline';

const STATE_STYLE: Record<ProcessState, string> = {
  NEW: 'bg-ink-700 text-slate-300',
  READY: 'bg-sky-900/70 text-sky-200',
  RUNNING: 'bg-emerald-600 text-white',
  WAITING: 'bg-violet-900/70 text-violet-200',
  TERMINATED: 'bg-ink-800 text-slate-500 line-through',
};

interface Props {
  snapshot: Snapshot | undefined;
  results: ProcessResult[];
}

/** Each process's state at the current playback time. Click a process for details. */
export default function ProcessStatePanel({ snapshot, results }: Props) {
  const [selected, setSelected] = useState<string | null>(null);
  if (!snapshot) return null;
  const detail = selected ? snapshot.processes.find((p) => p.pid === selected) : undefined;
  const final = selected ? results.find((r) => r.pid === selected) : undefined;

  return (
    <div>
      <h3 className="panel-title mb-3">Process states <span className="font-normal normal-case text-slate-500">at t={snapshot.time}</span></h3>
      <ul className="space-y-1.5">
        {snapshot.processes.map((p) => (
          <li key={p.pid}>
            <button
              type="button"
              onClick={() => setSelected(selected === p.pid ? null : p.pid)}
              aria-pressed={selected === p.pid}
              className={`flex w-full items-center justify-between rounded-md border px-3 py-1.5 text-left text-sm transition-colors hover:bg-ink-800 ${selected === p.pid ? 'border-rr/60 bg-ink-800' : 'border-ink-700'}`}
            >
              <span className="font-mono font-semibold" style={{ color: colorForProcess(p.pid) }}>{p.pid}</span>
              <span className="flex items-center gap-2">
                {p.state !== 'TERMINATED' && p.state !== 'NEW' && (
                  <span className="font-mono text-[11px] text-slate-500">{p.burstType} · {p.remaining} left</span>
                )}
                <span className={`badge ${STATE_STYLE[p.state]}`}>{p.state}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
      {detail && final && (
        <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 rounded-md border border-ink-600 bg-ink-950/60 p-3 font-mono text-xs" aria-live="polite">
          <div><dt className="text-slate-500">PID</dt><dd>{detail.pid}</dd></div>
          <div><dt className="text-slate-500">Arrival</dt><dd>{final.arrivalTime}</dd></div>
          <div><dt className="text-slate-500">State now</dt><dd>{detail.state}</dd></div>
          <div><dt className="text-slate-500">Current burst</dt><dd>{detail.burstType === 'NONE' ? '—' : `${detail.burstType} (${detail.remaining} left)`}</dd></div>
          <div><dt className="text-slate-500">Start (final)</dt><dd>{final.startTime}</dd></div>
          <div><dt className="text-slate-500">Completion (final)</dt><dd>{final.completionTime}</dd></div>
          <div><dt className="text-slate-500">Waiting (final)</dt><dd>{final.waitingTime}</dd></div>
          <div><dt className="text-slate-500">Response (final)</dt><dd>{final.responseTime}</dd></div>
        </dl>
      )}
    </div>
  );
}
