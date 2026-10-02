import type { EngineEvent, ParametersResponse, PolicyTransition, Snapshot } from '../types/simulation';
import { POLICY_COLOR } from '../lib/timeline';

interface Props {
  snapshot: Snapshot | undefined;
  parameters: ParametersResponse;
  transitions: PolicyTransition[];
  events: EngineEvent[];
  time: number;
  adaptiveMode: boolean;
}

/** Live view of the AdaptiveController state at the current playback time (all values from the engine). */
export default function AdaptiveMonitor({ snapshot, parameters, transitions, events, time, adaptiveMode }: Props) {
  if (!snapshot) return null;
  if (!adaptiveMode || snapshot.interactivityScore === null) {
    return (
      <div>
        <h3 className="panel-title mb-3">Adaptive monitor</h3>
        <p className="text-sm text-slate-400">
          This run uses a fixed policy (<span className="font-mono text-slate-200">{snapshot.policy}</span>), so the adaptive controller is not active.
          Select <span className="font-semibold">AdaptiveOS</span> as the mode to see the interactivity score, surge counter and cooldown.
        </p>
      </div>
    );
  }
  const score = snapshot.interactivityScore;
  const switchesSoFar = transitions.filter((t) => t.time <= time).length;
  const lastAdaptive = [...events].reverse().find((e) => e.time <= time && ['POLICY_SWITCH', 'SIGNAL', 'COOLDOWN_BLOCKED'].includes(e.type));
  const pct = (v: number) => `${(v / parameters.maxScore) * 100}%`;

  return (
    <div>
      <h3 className="panel-title mb-3">Adaptive monitor</h3>
      <dl className="space-y-3 text-sm">
        <div className="flex items-center justify-between">
          <dt className="text-slate-400">Current policy</dt>
          <dd className="badge text-ink-950" style={{ background: POLICY_COLOR[snapshot.policy] }}>{snapshot.policy}</dd>
        </div>
        <div>
          <div className="flex items-center justify-between">
            <dt className="text-slate-400">Interactivity score</dt>
            <dd className="font-mono">{score.toFixed(1)} <span className="text-slate-500">/ {parameters.maxScore}</span></dd>
          </div>
          <div className="relative mt-2 h-3 rounded bg-ink-950" role="meter" aria-valuemin={0} aria-valuemax={parameters.maxScore} aria-valuenow={score} aria-label="Interactivity score">
            <div className="absolute inset-y-0 left-0 rounded bg-adaptive/80" style={{ width: pct(score) }} />
            <div className="absolute inset-y-[-3px] w-px bg-rr" style={{ left: pct(parameters.lowThreshold) }} title={`LOW ${parameters.lowThreshold}`} />
            <div className="absolute inset-y-[-3px] w-px bg-srtf" style={{ left: pct(parameters.highThreshold) }} title={`HIGH ${parameters.highThreshold}`} />
          </div>
          <div className="relative mt-1 h-3 font-mono text-[10px] text-slate-500">
            <span className="absolute -translate-x-1/2 text-rr" style={{ left: pct(parameters.lowThreshold) }}>LOW {parameters.lowThreshold}</span>
            <span className="absolute -translate-x-1/2 text-srtf" style={{ left: pct(parameters.highThreshold) }}>HIGH {parameters.highThreshold}</span>
          </div>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-slate-400">Classification</dt>
          <dd className="font-medium">{snapshot.classification}</dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-slate-400">Surge counter</dt>
          <dd className="font-mono">{snapshot.surgeCounter} <span className="text-slate-500">/ evidence at {parameters.surgeThreshold}</span></dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-slate-400">Cooldown</dt>
          <dd className="font-mono">{snapshot.cooldownActive ? `Active until t=${snapshot.cooldownUntil}` : 'Inactive'}</dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-slate-400">Policy switches so far</dt>
          <dd className="font-mono">{switchesSoFar}</dd>
        </div>
        <div>
          <dt className="text-slate-400">Last adaptive event</dt>
          <dd className="mt-1 rounded bg-ink-950 px-2 py-1.5 font-mono text-xs">{lastAdaptive ? `[t=${lastAdaptive.time}] ${lastAdaptive.message}` : 'None yet'}</dd>
        </div>
      </dl>
      <p className="kbd-hint mt-3">Classification rule: score &lt; LOW → CPU-heavy tendency; score &gt; HIGH → interactive tendency; otherwise neutral.</p>
    </div>
  );
}
