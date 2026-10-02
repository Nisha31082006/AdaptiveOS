const SECTIONS: [string, string][] = [
  ['Traditional fixed scheduling', 'Classic schedulers (FCFS, RR, SJF/SRTF, priority) pick one policy for the whole run. That policy never changes even if the workload\'s behavior does.'],
  ['Round Robin', 'A FIFO ready queue with a fixed time quantum. Fair and simple, but long CPU-bound jobs and short interactive jobs are treated identically.'],
  ['SRTF', 'Shortest Remaining Time First is preemptive: it always runs the ready process with the least remaining time on its CURRENT CPU burst. Great average waiting time, but it can starve long jobs and needs to know remaining time.'],
  ['Why behavior matters', 'A workload is rarely uniform: some processes are I/O-heavy and interactive, others are CPU-bound. A single fixed policy is a compromise across both.'],
  ['Runtime observation', 'AdaptiveOS never guesses ahead of time. It watches two kinds of events as they actually happen: a quantum expiring (involuntary, CPU-heavy-like) or a CPU burst finishing early to enter I/O (voluntary; classified further by anti-gaming).'],
  ['EWMA', 'An Exponentially Weighted Moving Average blends a 0-1024 signal per event into a running score: score = alpha × signal + (1-alpha) × previous. Recent behavior dominates; older behavior fades geometrically.'],
  ['Thresholds', 'Score below LOW (baseline 307, about 30% of 1024) makes RR→SRTF worth considering. Score above HIGH (baseline 716, about 70%) makes SRTF→RR worth considering. Between them, nothing happens.'],
  ['Hysteresis', 'Using two different thresholds instead of one prevents rapid oscillation when the score hovers near a single boundary.'],
  ['Cooldown', 'After any switch, a minimum number of ticks (baseline 12) must pass before another switch is allowed, even if thresholds are crossed again.'],
  ['Phase-change detection', 'A run of consecutive CPU-heavy events increments a surge counter. Reaching the surge threshold (baseline 3) is treated as EVIDENCE of a possible phase change — not an automatic switch. The controller still checks the score, the hysteresis band, the cooldown and whether another process is even waiting before it acts on that evidence.'],
  ['Anti-gaming', 'A process that yields voluntarily after using almost its entire quantum (E = executed/allocated ≥ 0.95 by default) is still classified CPU-heavy-like, not interactive, even though it "voluntarily" gave up the CPU right at the edge.'],
  ['Adaptive switching', 'The whole scheduler has ONE active policy at a time (global policy switching). When it switches, the ready queue is migrated to the new policy\'s structure; no process is ever permanently bound to one algorithm.'],
  ['Metrics', 'Average waiting, turnaround and response time; context switches; CPU utilization — computed identically for every mode from the same formulas, so comparisons are apples-to-apples.'],
  ['Experimental evaluation', 'AdaptiveOS makes no claim of being better than RR or SRTF. The Comparison page runs all three on the same workload and parameters and reports measured numbers for you to judge.'],
];

export default function HowItWorks() {
  return (
    <div className="mx-auto max-w-4xl space-y-10 px-4 py-10 sm:px-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-50">How AdaptiveOS works</h1>
        <p className="mt-2 text-slate-400">A deterministic, rule-based mechanism — no machine learning, no prediction, no per-process algorithm assignment.</p>
      </div>

      <div className="panel flex flex-wrap items-center justify-center gap-2 p-6 font-mono text-xs text-slate-300 sm:text-sm">
        {['Process Behavior', 'Runtime Observation', 'EWMA Score', 'Threshold Check', 'Adaptive Controller', 'RR ↔ SRTF'].map((s, i, arr) => (
          <span key={s} className="flex items-center gap-2">
            <span className="rounded-md border border-ink-600 bg-ink-900 px-3 py-1.5">{s}</span>
            {i < arr.length - 1 && <span className="text-adaptive">↓</span>}
          </span>
        ))}
      </div>

      <ol className="space-y-6">
        {SECTIONS.map(([title, body], i) => (
          <li key={title} className="panel p-5">
            <h2 className="flex items-baseline gap-2 text-lg font-semibold text-slate-100">
              <span className="font-mono text-sm text-adaptive">{String(i + 1).padStart(2, '0')}</span>
              {title}
            </h2>
            <p className="mt-2 leading-relaxed text-slate-400">{body}</p>
          </li>
        ))}
      </ol>
    </div>
  );
}
