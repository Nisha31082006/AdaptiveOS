import { useWorkspace, useCompareStale } from '../state/WorkspaceContext';
import ErrorBanner from '../components/ErrorBanner';
import Spinner from '../components/Spinner';
import MetricsComparison from '../components/MetricsComparison';
import GanttChart from '../components/GanttChart';
import { modeLabel } from '../lib/timeline';

export default function Comparison() {
  const { compare, loading, errors, clearErrors, runCompare, processes, params } = useWorkspace();
  const stale = useCompareStale();
  const maxTime = compare ? Math.max(compare.pureRr.totalTime, compare.pureSrtf.totalTime, compare.adaptive.totalTime) : 0;

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-4 py-8 sm:px-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-50">Comparison</h1>
        <p className="mt-1 text-sm text-slate-400">
          Runs Pure RR, Pure SRTF and AdaptiveOS on the exact same {processes.length}-process workload and the same parameters ({Object.entries(params).length} settings), using the real engine for each mode.
        </p>
      </div>

      <ErrorBanner errors={errors} onDismiss={clearErrors} />

      <div className="flex flex-wrap items-center gap-3">
        <button type="button" className="btn btn-adaptive" onClick={() => runCompare()} disabled={loading !== 'idle'}>
          {loading === 'compare' ? <Spinner label="Comparing…" /> : compare ? 'Compare Again' : 'Compare All'}
        </button>
        {stale && compare && <p className="text-xs text-amber-300">Inputs changed since this comparison ran — click Compare Again to refresh.</p>}
      </div>

      {!compare && loading === 'idle' && (
        <div className="rounded-lg border border-dashed border-ink-600 p-10 text-center text-slate-500">
          No comparison yet. Edit the workload on the <a className="underline hover:text-slate-200" href="/simulator">Simulator</a> page (or use the default), then click Compare All.
        </div>
      )}

      {compare && (
        <>
          <section className="panel p-5">
            <h2 className="panel-title mb-4">Comparison results</h2>
            <MetricsComparison compare={compare} />
          </section>

          <section className="panel space-y-6 p-5">
            <h2 className="panel-title">Gantt charts (same scale)</h2>
            {(['pureRr', 'pureSrtf', 'adaptive'] as const).map((k) => (
              <GanttChart
                key={k}
                title={modeLabel(compare[k].mode)}
                entries={compare[k].ganttEntries}
                policySegments={compare[k].policySegments}
                transitions={compare[k].policyTransitions}
                totalTime={maxTime}
                pxPerTick={Math.max(4, Math.min(24, Math.floor(900 / Math.max(maxTime, 1))))}
                compact
              />
            ))}
          </section>
        </>
      )}
    </div>
  );
}
