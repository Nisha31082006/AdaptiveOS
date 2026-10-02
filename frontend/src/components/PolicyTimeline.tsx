import { useState } from 'react';
import type { PolicySegment, PolicyTransition } from '../types/simulation';
import { POLICY_COLOR } from '../lib/timeline';

interface Props {
  segments: PolicySegment[];
  transitions: PolicyTransition[];
  totalTime: number;
  selected?: PolicyTransition | null;
  onSelect?: (t: PolicyTransition | null) => void;
}

/** Proportional bar of which policy was active when, with clickable switch markers and a detail panel. */
export default function PolicyTimeline({ segments, transitions, totalTime, selected: controlled, onSelect }: Props) {
  const [internal, setInternal] = useState<PolicyTransition | null>(null);
  const selected = controlled !== undefined ? controlled : internal;
  const select = (t: PolicyTransition | null) => (onSelect ? onSelect(t) : setInternal(t));
  const pct = (t: number) => `${(t / Math.max(totalTime, 1)) * 100}%`;

  return (
    <div>
      <h3 className="panel-title mb-3">Policy timeline</h3>
      <div className="relative h-14 px-1">
        <div className="absolute inset-x-1 top-6 flex h-6 overflow-hidden rounded" role="img" aria-label="Active policy over time">
          {segments.map((s, i) => (
            <div
              key={i}
              className="flex items-center justify-center font-mono text-[11px] font-bold text-ink-950"
              style={{ width: `${((s.endTime - s.startTime) / Math.max(totalTime, 1)) * 100}%`, background: POLICY_COLOR[s.policy] }}
              title={`${s.policy}: t=${s.startTime}–${s.endTime}`}
            >
              {(s.endTime - s.startTime) / Math.max(totalTime, 1) > 0.08 ? s.policy : ''}
            </div>
          ))}
        </div>
        <div className="absolute inset-x-1 top-0">
          {transitions.map((t, i) => (
            <button
              key={i}
              type="button"
              onClick={() => select(selected === t ? null : t)}
              aria-label={`Policy switch at t=${t.time}: ${t.fromPolicy} to ${t.toPolicy}`}
              aria-pressed={selected === t}
              className="absolute -translate-x-1/2 rounded px-1 font-mono text-[10px] text-adaptive hover:bg-ink-700 focus-visible:ring-2 focus-visible:ring-adaptive"
              style={{ left: pct(t.time) }}
            >
              ▼ t={t.time}
            </button>
          ))}
        </div>
        <div className="absolute inset-x-1 bottom-0 flex justify-between font-mono text-[10px] text-slate-500">
          <span>0</span>
          <span>{totalTime}</span>
        </div>
      </div>
      {transitions.length === 0 && (
        <p className="mt-2 text-xs text-slate-500">No policy switch happened in this run: one policy stayed active the whole time.</p>
      )}
      {selected && (
        <dl className="mt-3 grid grid-cols-2 gap-x-6 gap-y-1 rounded-md border border-ink-600 bg-ink-950/60 p-3 font-mono text-xs sm:grid-cols-4" aria-live="polite">
          <div><dt className="text-slate-500">Time</dt><dd>t={selected.time}</dd></div>
          <div><dt className="text-slate-500">Switch</dt><dd>{selected.fromPolicy} → {selected.toPolicy}</dd></div>
          <div><dt className="text-slate-500">Score</dt><dd>{selected.interactivityScore.toFixed(1)}</dd></div>
          <div><dt className="text-slate-500">Surge counter</dt><dd>{selected.surgeCounter}</dd></div>
          <div className="col-span-2 sm:col-span-3"><dt className="text-slate-500">Reason (from the engine)</dt><dd>{selected.reason}</dd></div>
          <div><dt className="text-slate-500">Cooldown until</dt><dd>t={selected.cooldownUntil}</dd></div>
        </dl>
      )}
    </div>
  );
}
