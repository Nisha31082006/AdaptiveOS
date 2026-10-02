import { useState } from 'react';
import type { ProcessDef } from '../types/simulation';
import { validateProcess } from '../lib/validation';
import BurstEditor from './BurstEditor';

interface Props {
  initial: ProcessDef;
  otherPids: string[];
  title: string;
  onSave: (p: ProcessDef) => void;
  onCancel: () => void;
}

/** Add / edit one process: ID, arrival time and its burst sequence, with inline validation. */
export default function ProcessForm({ initial, otherPids, title, onSave, onCancel }: Props) {
  const [draft, setDraft] = useState<ProcessDef>(() => JSON.parse(JSON.stringify(initial)) as ProcessDef);
  const [showErrors, setShowErrors] = useState(false);
  const errors = validateProcess({ ...draft, pid: draft.pid.trim() }, otherPids);

  const save = () => {
    if (errors.length > 0) {
      setShowErrors(true);
      return;
    }
    onSave({ ...draft, pid: draft.pid.trim() });
  };

  return (
    <section className="panel border-rr/40 p-4" aria-label={title}>
      <h3 className="panel-title mb-3">{title}</h3>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label" htmlFor="pf-pid">Process ID</label>
          <input id="pf-pid" className="input" value={draft.pid} onChange={(e) => setDraft({ ...draft, pid: e.target.value })} placeholder="P1" />
        </div>
        <div>
          <label className="label" htmlFor="pf-arrival">Arrival time</label>
          <input
            id="pf-arrival"
            className="input"
            type="number"
            min={0}
            step={1}
            value={Number.isNaN(draft.arrivalTime) ? '' : draft.arrivalTime}
            onChange={(e) => setDraft({ ...draft, arrivalTime: e.target.value === '' ? NaN : Number(e.target.value) })}
          />
        </div>
      </div>
      <div className="mt-4">
        <p className="label">Burst sequence</p>
        <BurstEditor bursts={draft.bursts} onChange={(bursts) => setDraft({ ...draft, bursts })} />
      </div>
      {showErrors && errors.length > 0 && (
        <ul role="alert" className="mt-3 list-disc space-y-1 pl-5 text-sm text-red-300">
          {errors.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
      <div className="mt-4 flex gap-2">
        <button type="button" className="btn btn-primary" onClick={save}>
          Save process
        </button>
        <button type="button" className="btn" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </section>
  );
}
