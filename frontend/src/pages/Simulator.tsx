import { useMemo, useState } from 'react';
import { useWorkspace } from '../state/WorkspaceContext';
import { nextPid } from '../lib/validation';
import { snapshotAt } from '../lib/timeline';
import { usePlayback } from '../hooks/usePlayback';
import type { ExecutionMode, PolicyTransition, ProcessDef } from '../types/simulation';

import ProcessTable from '../components/ProcessTable';
import ProcessForm from '../components/ProcessForm';
import ParameterPanel from '../components/ParameterPanel';
import ErrorBanner from '../components/ErrorBanner';
import Spinner from '../components/Spinner';
import GanttChart from '../components/GanttChart';
import PolicyTimeline from '../components/PolicyTimeline';
import ScoreChart from '../components/ScoreChart';
import AdaptiveMonitor from '../components/AdaptiveMonitor';
import ProcessStatePanel from '../components/ProcessStatePanel';
import EventLog from '../components/EventLog';
import MetricsCards from '../components/MetricsCards';

const MODES: { value: ExecutionMode; label: string }[] = [
  { value: 'PURE_RR', label: 'Pure RR' },
  { value: 'PURE_SRTF', label: 'Pure SRTF' },
  { value: 'ADAPTIVE', label: 'AdaptiveOS' },
];

export default function Simulator() {
  const ws = useWorkspace();
  const { processes, setProcesses, params, setParams, mode, setMode, workloads, selectedWorkloadId, loadWorkload, result, loading, errors, clearErrors, stale } = ws;
  const [editing, setEditing] = useState<'new' | number | null>(null);
  const [selectedTransition, setSelectedTransition] = useState<PolicyTransition | null>(null);

  const playback = usePlayback(result?.totalTime ?? 0);
  const snapshot = useMemo(() => (result ? snapshotAt(result.snapshots, playback.time) : undefined), [result, playback.time]);

  const editingProcess: ProcessDef =
    editing === 'new' ? { pid: nextPid(processes), arrivalTime: 0, bursts: [{ type: 'CPU', duration: 4 }] } : editing !== null ? processes[editing] : ({ pid: '', arrivalTime: 0, bursts: [] } as ProcessDef);
  const otherPids = editing === 'new' ? processes.map((p) => p.pid) : processes.filter((_, i) => i !== editing).map((p) => p.pid);

  const saveProcess = (p: ProcessDef) => {
    if (editing === 'new') setProcesses([...processes, p]);
    else if (typeof editing === 'number') setProcesses(processes.map((x, i) => (i === editing ? p : x)));
    setEditing(null);
  };

  const runNow = async () => {
    setSelectedTransition(null);
    await ws.run();
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-4 py-8 sm:px-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-50">AdaptiveOS Simulator</h1>
        <p className="mt-1 text-sm text-slate-400">Build a workload, choose a mode and parameters, then run it against the real Java scheduling engine.</p>
      </div>

      <ErrorBanner errors={errors} onDismiss={clearErrors} />

      <section className="panel p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="panel-title">Workload configuration</h2>
          <label className="flex items-center gap-2 text-sm">
            <span className="text-slate-400">Example workload</span>
            <select className="input w-56 py-1.5" value={selectedWorkloadId} onChange={(e) => loadWorkload(e.target.value)}>
              <optgroup label="Core">
                {workloads.filter((w) => w.category === 'core').map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
              </optgroup>
              <optgroup label="Edge cases">
                {workloads.filter((w) => w.category === 'edge').map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
              </optgroup>
            </select>
          </label>
        </div>
        <ProcessTable processes={processes} onAdd={() => setEditing('new')} onEdit={(i) => setEditing(i)} onDelete={(i) => setProcesses(processes.filter((_, j) => j !== i))} />
        {editing !== null && (
          <div className="mt-4">
            <ProcessForm
              key={editing}
              title={editing === 'new' ? 'Add process' : `Edit ${editingProcess.pid}`}
              initial={editingProcess}
              otherPids={otherPids}
              onSave={saveProcess}
              onCancel={() => setEditing(null)}
            />
          </div>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="panel p-5">
          <h2 className="panel-title mb-4">Parameters</h2>
          <ParameterPanel params={params} onChange={setParams} />
        </section>
        <section className="panel p-5">
          <h2 className="panel-title mb-4">Simulation controls</h2>
          <fieldset className="mb-4">
            <legend className="label">Mode</legend>
            <div className="flex flex-wrap gap-2">
              {MODES.map((m) => (
                <label key={m.value} className={`btn cursor-pointer ${mode === m.value ? 'btn-primary' : ''}`}>
                  <input type="radio" name="mode" className="sr-only" value={m.value} checked={mode === m.value} onChange={() => setMode(m.value)} />
                  {m.label}
                </label>
              ))}
            </div>
          </fieldset>
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn btn-primary" onClick={runNow} disabled={loading !== 'idle'}>
              {loading === 'simulate' ? <Spinner label="Running…" /> : result ? 'Run Again' : 'Run Simulation'}
            </button>
            <button type="button" className="btn btn-adaptive" onClick={() => ws.runCompare()} disabled={loading !== 'idle'}>
              {loading === 'compare' ? <Spinner label="Comparing…" /> : 'Compare All'}
            </button>
            <button type="button" className="btn" onClick={ws.reset} disabled={loading !== 'idle'}>Reset</button>
          </div>
          {stale && result && <p className="mt-3 text-xs text-amber-300">Inputs changed since the last run — results below are from the previous run. Click Run to refresh.</p>}
          {ws.compare && <p className="mt-3 text-xs text-slate-400">A full comparison was also computed — see the <a className="underline hover:text-slate-200" href="/comparison">Comparison</a> page.</p>}
        </section>
      </div>

      {result && snapshot && (
        <>
          <section className="panel p-5">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
              <h2 className="panel-title">Current simulation</h2>
              <div className="flex items-center gap-2">
                <button type="button" className="btn btn-ghost px-2" onClick={() => playback.stepBy(-1)} aria-label="Step back">⏮</button>
                <button type="button" className="btn" onClick={playback.toggle}>{playback.playing ? 'Pause' : 'Play'}</button>
                <button type="button" className="btn btn-ghost px-2" onClick={() => playback.stepBy(1)} aria-label="Step forward">⏭</button>
                <label className="flex items-center gap-1 text-xs text-slate-400">
                  Speed
                  <select className="input w-16 py-1" value={playback.speed} onChange={(e) => playback.setSpeed(Number(e.target.value))}>
                    {[0.5, 1, 2, 4].map((s) => <option key={s} value={s}>{s}×</option>)}
                  </select>
                </label>
              </div>
            </div>
            <input
              type="range" min={0} max={result.totalTime} value={playback.time}
              onChange={(e) => playback.setTime(Number(e.target.value))} className="w-full accent-adaptive" aria-label="Timeline scrubber"
            />
            <div className="mt-3 grid grid-cols-3 divide-x divide-ink-700 rounded-md border border-ink-700 text-center">
              <div className="p-3"><p className="text-xs uppercase text-slate-500">Current time</p><p className="font-mono text-xl">{snapshot.time}</p></div>
              <div className="p-3"><p className="text-xs uppercase text-slate-500">Running process</p><p className="font-mono text-xl">{snapshot.runningProcessId ?? 'idle'}</p></div>
              <div className="p-3"><p className="text-xs uppercase text-slate-500">Current policy</p><p className={`font-mono text-xl ${snapshot.policy === 'RR' ? 'text-rr' : 'text-srtf'}`}>{snapshot.policy}</p></div>
            </div>
          </section>

          <section className="panel p-5">
            <GanttChart
              entries={result.ganttEntries}
              policySegments={result.policySegments}
              transitions={result.policyTransitions}
              totalTime={result.totalTime}
              currentTime={playback.time}
              onSeek={playback.setTime}
              onSelectTransition={setSelectedTransition}
            />
          </section>

          <section className="panel p-5">
            <PolicyTimeline segments={result.policySegments} transitions={result.policyTransitions} totalTime={result.totalTime} selected={selectedTransition} onSelect={setSelectedTransition} />
          </section>

          {mode === 'ADAPTIVE' && (
            <section className="panel p-5">
              <ScoreChart snapshots={result.snapshots} parameters={result.parameters} transitions={result.policyTransitions} currentTime={playback.time} />
            </section>
          )}

          <div className="grid gap-6 lg:grid-cols-2">
            <section className="panel p-5"><ProcessStatePanel snapshot={snapshot} results={result.processResults} /></section>
            <section className="panel p-5"><AdaptiveMonitor snapshot={snapshot} parameters={result.parameters} transitions={result.policyTransitions} events={result.events} time={playback.time} adaptiveMode={mode === 'ADAPTIVE'} /></section>
          </div>

          <section className="panel p-5">
            <EventLog events={result.events} time={playback.time} />
          </section>

          <section className="panel p-5">
            <h2 className="panel-title mb-3">Metrics</h2>
            <MetricsCards metrics={result.metrics} />
          </section>
        </>
      )}

      {!result && loading === 'idle' && (
        <div className="rounded-lg border border-dashed border-ink-600 p-10 text-center text-slate-500">
          No simulation has been run yet. Configure a workload above and click <span className="font-semibold text-slate-300">Run Simulation</span>.
        </div>
      )}
    </div>
  );
}
