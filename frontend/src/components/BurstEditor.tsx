import type { Burst, BurstType } from '../types/simulation';

interface Props {
  bursts: Burst[];
  onChange: (bursts: Burst[]) => void;
}

/** Edits an ordered burst sequence: [CPU|3] [IO|4] [CPU|2]. Supports add, delete and reorder. */
export default function BurstEditor({ bursts, onChange }: Props) {
  const update = (i: number, patch: Partial<Burst>) => onChange(bursts.map((b, j) => (j === i ? { ...b, ...patch } : b)));
  const remove = (i: number) => onChange(bursts.filter((_, j) => j !== i));
  const move = (i: number, d: -1 | 1) => {
    const j = i + d;
    if (j < 0 || j >= bursts.length) return;
    const next = [...bursts];
    [next[i], next[j]] = [next[j], next[i]];
    onChange(next);
  };
  const add = (type: BurstType) => onChange([...bursts, { type, duration: type === 'CPU' ? 4 : 3 }]);

  return (
    <div>
      <ol className="space-y-2" aria-label="Burst sequence">
        {bursts.map((b, i) => (
          <li key={i} className="flex flex-wrap items-center gap-2">
            <span className="w-5 text-right font-mono text-xs text-slate-500">{i + 1}</span>
            <select
              aria-label={`Burst ${i + 1} type`}
              className="input w-20"
              value={b.type}
              onChange={(e) => update(i, { type: e.target.value as BurstType })}
            >
              <option value="CPU">CPU</option>
              <option value="IO">IO</option>
            </select>
            <input
              aria-label={`Burst ${i + 1} duration`}
              className="input w-24"
              type="number"
              min={1}
              step={1}
              value={Number.isNaN(b.duration) ? '' : b.duration}
              onChange={(e) => update(i, { duration: e.target.value === '' ? NaN : Number(e.target.value) })}
            />
            <span className="text-xs text-slate-500">ticks</span>
            <button type="button" className="btn btn-ghost px-2" onClick={() => move(i, -1)} disabled={i === 0} aria-label={`Move burst ${i + 1} up`}>
              ↑
            </button>
            <button type="button" className="btn btn-ghost px-2" onClick={() => move(i, 1)} disabled={i === bursts.length - 1} aria-label={`Move burst ${i + 1} down`}>
              ↓
            </button>
            <button type="button" className="btn btn-ghost px-2 text-red-300" onClick={() => remove(i)} aria-label={`Delete burst ${i + 1}`}>
              ✕
            </button>
          </li>
        ))}
      </ol>
      <div className="mt-3 flex gap-2">
        <button type="button" className="btn" onClick={() => add('CPU')}>
          + CPU burst
        </button>
        <button type="button" className="btn" onClick={() => add('IO')}>
          + I/O burst
        </button>
      </div>
    </div>
  );
}
