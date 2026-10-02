export default function About() {
  return (
    <div className="mx-auto max-w-3xl space-y-8 px-4 py-10 sm:px-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-50">AdaptiveOS</h1>
        <p className="text-slate-400">Runtime-Adaptive CPU Scheduling Simulator</p>
      </div>

      <p className="leading-relaxed text-slate-300">
        AdaptiveOS is an academic / educational project exploring runtime-adaptive scheduling: whether lightweight, rule-based
        analysis of process behavior can usefully switch a scheduler between Round Robin and SRTF while it runs. It is a
        simulator, not a modification of any real operating system kernel, and it makes no claim to be superior to either
        fixed policy — that is exactly what the Comparison page is for.
      </p>

      <section>
        <h2 className="text-lg font-semibold text-slate-100">Technology stack</h2>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-slate-400">
          <li>Engine and API: Java 17, Spring Boot, Maven</li>
          <li>Frontend: React, TypeScript, Vite, Tailwind CSS, Recharts</li>
          <li>No database; no external services required to run locally</li>
        </ul>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-slate-100">Algorithms</h2>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-slate-400">
          <li>Round Robin (fixed quantum, FIFO ready queue)</li>
          <li>Shortest Remaining Time First (preemptive, deterministic tie-breaking)</li>
          <li>AdaptiveOS: global policy switching between the two above, driven by an EWMA interactivity score</li>
        </ul>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-slate-100">Adaptive mechanism</h2>
        <p className="mt-2 text-slate-400">
          Event classification → EWMA score update → threshold check with hysteresis → cooldown check → phase-change evidence
          → anti-gaming check → possible global RR ↔ SRTF switch. See the <a className="underline hover:text-slate-200" href="/how-it-works">How It Works</a> page for details.
        </p>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-slate-100">Limitations</h2>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-slate-400">
          <li>Only RR and SRTF are supported policies.</li>
          <li>All thresholds, the quantum, alpha, cooldown, surge threshold and anti-gaming ratio are baseline experimental parameters, not proven optimal values.</li>
          <li>The mechanism reacts to observed behavior; it does not predict future bursts.</li>
          <li>No guarantee is made that AdaptiveOS outperforms Pure RR or Pure SRTF on any given workload.</li>
          <li>This is a simulator: it does not modify a real kernel or scheduler.</li>
          <li>No machine learning or AI is used anywhere in the scheduling logic.</li>
        </ul>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-slate-100">Future work</h2>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-slate-400">
          <li>Additional policies (FCFS, SJF, Priority, MLFQ) and additional adaptive strategies</li>
          <li>Persisted experiment history and CSV export of results</li>
          <li>Live/streamed simulation instead of replaying a completed run</li>
        </ul>
      </section>
    </div>
  );
}
