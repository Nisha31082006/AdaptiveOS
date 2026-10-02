import type { Parameters } from '../types/simulation';
import { DEFAULT_PARAMS } from '../lib/defaults';
import { validateParams } from '../lib/validation';

interface Props {
  params: Parameters;
  onChange: (p: Parameters) => void;
}

interface Field {
  key: keyof Parameters;
  label: string;
  step: number;
  help: string;
}

const FIELDS: Field[] = [
  { key: 'quantum', label: 'RR Quantum', step: 1, help: 'Length (ticks) of a Round Robin time slice. Baseline 4; a tunable experimental value, not claimed optimal.' },
  { key: 'alpha', label: 'EWMA Alpha', step: 0.05, help: 'EWMA Alpha controls how strongly recent behavior influences the interactivity score (0 to 1).' },
  { key: 'initialScore', label: 'Initial Score', step: 1, help: 'Starting interactivity score (0-1024). 512 is neutral: neither interactive nor CPU-heavy.' },
  { key: 'lowThreshold', label: 'Low Threshold', step: 1, help: 'RR to SRTF is considered only when the score falls below this value (about 30% of 1024).' },
  { key: 'highThreshold', label: 'High Threshold', step: 1, help: 'SRTF to RR is considered only when the score rises above this value (about 70% of 1024). Together with Low it forms the hysteresis band.' },
  { key: 'cooldownTicks', label: 'Cooldown (ticks)', step: 1, help: 'After a policy switch, another switch is blocked for this many ticks.' },
  { key: 'surgeThreshold', label: 'Surge Threshold', step: 1, help: 'Number of consecutive CPU-heavy events that count as phase-change evidence. Evidence alone does not force a switch.' },
  { key: 'antiGamingThreshold', label: 'Anti-Gaming', step: 0.01, help: 'If a process yields after using at least this fraction of its slice, the yield is treated as CPU-heavy rather than interactive.' },
];

/** Inputs for all baseline experimental parameters, with validation and hover/focus explanations. */
export default function ParameterPanel({ params, onChange }: Props) {
  const errors = validateParams(params);
  return (
    <div>
      <div className="grid gap-x-4 gap-y-3 sm:grid-cols-2">
        {FIELDS.map((f) => {
          const id = `param-${f.key}`;
          const value = params[f.key];
          return (
            <div key={f.key} className="group relative">
              <label htmlFor={id} className="label">
                {f.label}
              </label>
              <input
                id={id}
                className={`input ${errors[f.key] ? 'input-error' : ''}`}
                type="number"
                step={f.step}
                value={Number.isNaN(value) ? '' : value}
                aria-invalid={errors[f.key] ? true : undefined}
                aria-describedby={`${id}-help`}
                onChange={(e) => onChange({ ...params, [f.key]: e.target.value === '' ? NaN : Number(e.target.value) })}
              />
              {errors[f.key] && <p className="mt-1 text-xs text-red-300">{errors[f.key]}</p>}
              <p
                id={`${id}-help`}
                role="tooltip"
                className="pointer-events-none absolute left-0 top-full z-20 mt-1 hidden w-64 rounded-md border border-ink-600 bg-ink-950 p-2 text-xs text-slate-300 shadow-lg group-focus-within:block group-hover:block"
              >
                {f.help}
              </p>
            </div>
          );
        })}
      </div>
      <p className="kbd-hint mt-3">Baseline experimental values are pre-filled. Hover or focus a field for an explanation.</p>
      <button type="button" className="btn btn-ghost mt-2 px-0 text-xs text-slate-400 hover:text-white" onClick={() => onChange({ ...DEFAULT_PARAMS })}>
        Restore baseline parameters
      </button>
    </div>
  );
}
