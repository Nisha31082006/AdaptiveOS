import type { ProcessDef } from '../types/simulation';
import { colorForProcess } from '../lib/timeline';

interface Props {
  processes: ProcessDef[];
  onAdd: () => void;
  onEdit: (index: number) => void;
  onDelete: (index: number) => void;
}

/** Lists the workload. Bursts are shown as chips: CPU in slate, I/O striped in violet. */
export default function ProcessTable({ processes, onAdd, onEdit, onDelete }: Props) {
  return (
    <div>
      {processes.length === 0 ? (
        <div className="rounded-md border border-dashed border-ink-600 p-8 text-center text-sm text-slate-400">
          <p className="font-medium text-slate-300">The workload is empty.</p>
          <p className="mt-1">Add a process below, or load one of the example workloads.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <caption className="sr-only">Processes in the workload</caption>
            <thead className="text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="py-2 pr-4 font-medium">PID</th>
                <th className="py-2 pr-4 font-medium">Arrival</th>
                <th className="py-2 pr-4 font-medium">Burst sequence</th>
                <th className="py-2 pr-4 font-medium">CPU / IO</th>
                <th className="py-2 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ink-700">
              {processes.map((p, i) => {
                const cpu = p.bursts.filter((b) => b.type === 'CPU').reduce((s, b) => s + b.duration, 0);
                const io = p.bursts.filter((b) => b.type === 'IO').reduce((s, b) => s + b.duration, 0);
                return (
                  <tr key={p.pid + i}>
                    <td className="py-2 pr-4 font-mono font-semibold" style={{ color: colorForProcess(p.pid) }}>{p.pid}</td>
                    <td className="py-2 pr-4 font-mono">{p.arrivalTime}</td>
                    <td className="py-2 pr-4">
                      <div className="flex flex-wrap gap-1">
                        {p.bursts.map((b, j) => (
                          <span
                            key={j}
                            className={`badge ${b.type === 'CPU' ? 'bg-ink-700 text-slate-200' : 'bg-violet-900/60 text-violet-200'}`}
                            title={`${b.type} burst, ${b.duration} ticks`}
                          >
                            {b.type} {b.duration}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-2 pr-4 font-mono text-xs text-slate-400">{cpu} / {io}</td>
                    <td className="py-2 text-right">
                      <button type="button" className="btn btn-ghost px-2" onClick={() => onEdit(i)} aria-label={`Edit ${p.pid}`}>
                        Edit
                      </button>
                      <button type="button" className="btn btn-ghost px-2 text-red-300" onClick={() => onDelete(i)} aria-label={`Delete ${p.pid}`}>
                        Delete
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      <button type="button" className="btn mt-3" onClick={onAdd}>
        + Add process
      </button>
    </div>
  );
}
