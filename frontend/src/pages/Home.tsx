import { Link } from 'react-router-dom';

const FLOW = ['Processes', 'Runtime Observation', 'Adaptive Controller', 'RR ↔ SRTF', 'Scheduling Metrics'];

const FEATURES = [
  ['Runtime behavior observation', 'The engine classifies each CPU-burst completion or quantum expiry as it happens, from real execution events.'],
  ['EWMA interactivity estimation', 'An exponentially weighted moving average (0-1024) tracks recent tendency toward interactive or CPU-heavy behavior.'],
  ['Global policy switching', 'The whole scheduler has one active policy at a time; it is never assigned permanently per process.'],
  ['Hysteresis', 'Separate low/high thresholds for switching in each direction reduce oscillation near a single boundary.'],
  ['Cooldown', 'A minimum number of ticks after a switch during which another switch is blocked.'],
  ['Phase-change detection', 'Repeated CPU-heavy events are evidence of a possible phase change, not an automatic trigger by themselves.'],
  ['Anti-gaming heuristic', 'A voluntary yield after using almost the whole quantum is still treated as CPU-heavy-like behavior.'],
  ['Gantt visualization', 'An interactive timeline shows exactly which process ran when, and under which policy.'],
  ['Metrics comparison', 'Pure RR, Pure SRTF and AdaptiveOS are run on identical workloads and compared without declaring a winner.'],
];

export default function Home() {
  return (
    <div>
      <section className="mx-auto max-w-5xl px-4 pb-10 pt-16 text-center sm:px-6 sm:pt-24">
        <p className="font-mono text-xs uppercase tracking-[0.3em] text-adaptive">Runtime-Adaptive CPU Scheduling Simulator</p>
        <h1 className="mt-4 text-4xl font-bold tracking-tight text-slate-50 sm:text-6xl">AdaptiveOS</h1>
        <p className="mx-auto mt-6 max-w-2xl text-balance text-lg text-slate-400">
          Observe process behavior. Adapt scheduling policy at runtime. Compare RR, SRTF, and AdaptiveOS.
        </p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link to="/simulator" className="btn btn-adaptive px-5 py-2 text-base">Launch Simulator</Link>
          <Link to="/how-it-works" className="btn px-5 py-2 text-base">Explore How It Works</Link>
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-4 pb-16 sm:px-6" aria-label="Conceptual flow">
        <div className="panel flex flex-col items-center gap-3 p-6 sm:flex-row sm:justify-between sm:gap-2">
          {FLOW.map((step, i) => (
            <div key={step} className="flex items-center gap-2 sm:contents">
              <div className="flow-step rounded-md border border-ink-600 bg-ink-900 px-3 py-2 text-center font-mono text-xs text-slate-300 sm:text-sm">{step}</div>
              {i < FLOW.length - 1 && <span className="text-adaptive sm:mx-1" aria-hidden>→</span>}
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-4xl px-4 pb-16 sm:px-6">
        <h2 className="text-xl font-semibold text-slate-100">Why AdaptiveOS?</h2>
        <p className="mt-3 leading-relaxed text-slate-400">
          Traditional scheduling policies typically remain fixed during a scheduling run. AdaptiveOS explores whether lightweight
          runtime behavior analysis can dynamically switch between Round Robin and SRTF when workload behavior changes. It is a
          deterministic, rule-based simulator — not machine learning, and not a guarantee of better performance than either
          fixed policy. Results are measured experimentally, workload by workload.
        </p>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-24 sm:px-6">
        <h2 className="mb-6 text-xl font-semibold text-slate-100">Core features</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(([title, body]) => (
            <div key={title} className="panel p-4">
              <h3 className="font-medium text-slate-100">{title}</h3>
              <p className="mt-1.5 text-sm text-slate-400">{body}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
