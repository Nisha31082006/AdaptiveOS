import { useMemo, useRef, useState } from 'react';
import type { GanttEntry, PolicySegment, PolicyTransition } from '../types/simulation';
import { colorForProcess, niceTickStep, POLICY_COLOR, processIdsIn } from '../lib/timeline';

interface Props {
  title?: string;
  entries: GanttEntry[];
  policySegments: PolicySegment[];
  transitions: PolicyTransition[];
  totalTime: number;
  currentTime?: number;
  onSeek?: (time: number) => void;
  onSelectTransition?: (t: PolicyTransition) => void;
  /** Smaller rows for the side-by-side comparison view. */
  compact?: boolean;
  /** Shared zoom (px per tick) so several charts can use the same scale. */
  pxPerTick?: number;
}

const LEFT = 64;
const RIGHT = 24;
const BAND_H = 20;
const AXIS_H = 26;

/**
 * Interactive Gantt chart drawn as SVG from the entries returned by the engine.
 * One lane per process, an "IDLE" lane when the CPU was idle, a policy band on top, dashed markers at
 * policy transitions, hover/focus tooltips, zoom, and a playback cursor.
 */
export default function GanttChart({
  title = 'Gantt chart',
  entries,
  policySegments,
  transitions,
  totalTime,
  currentTime,
  onSeek,
  onSelectTransition,
  compact = false,
  pxPerTick: fixedPx,
}: Props) {
  const [zoom, setZoom] = useState(24);
  const px = fixedPx ?? zoom;
  const rowH = compact ? 22 : 30;
  const scrollRef = useRef<HTMLDivElement>(null);
  const [tip, setTip] = useState<{ x: number; y: number; entry: GanttEntry } | null>(null);

  const ids = useMemo(() => processIdsIn(entries), [entries]);
  const hasIdle = entries.some((e) => e.idle);
  const lanes = [...ids, ...(hasIdle ? ['IDLE'] : [])];
  const width = LEFT + totalTime * px + RIGHT;
  const height = BAND_H + AXIS_H + lanes.length * rowH + 8;
  const laneY = (lane: string) => BAND_H + AXIS_H + lanes.indexOf(lane) * rowH;
  const step = niceTickStep(px);
  const ticks: number[] = [];
  for (let t = 0; t <= totalTime; t += step) ticks.push(t);
  if (ticks[ticks.length - 1] !== totalTime) ticks.push(totalTime);

  const fit = () => {
    const avail = (scrollRef.current?.clientWidth ?? 900) - LEFT - RIGHT - 8;
    setZoom(Math.max(4, Math.min(60, Math.floor(avail / Math.max(totalTime, 1)))));
  };

  const seekFromClick = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!onSeek) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const t = Math.round((e.clientX - rect.left - LEFT) / px);
    if (t >= 0 && t <= totalTime) onSeek(t);
  };

  const showTip = (e: React.MouseEvent | React.FocusEvent, entry: GanttEntry) => {
    const box = scrollRef.current?.getBoundingClientRect();
    if (!box) return;
    if ('clientX' in e) setTip({ x: e.clientX - box.left + (scrollRef.current?.scrollLeft ?? 0) + 12, y: e.clientY - box.top + 12, entry });
    else {
      const r = (e.currentTarget as Element).getBoundingClientRect();
      setTip({ x: r.left - box.left + (scrollRef.current?.scrollLeft ?? 0), y: r.bottom - box.top + 4, entry });
    }
  };

  const describe = (g: GanttEntry) =>
    `${g.idle ? 'CPU idle' : g.processId}, time ${g.startTime} to ${g.endTime}, policy ${g.policy}, ${g.endTime - g.startTime} ticks`;

  return (
    <div>
      <div className="mb-2 flex flex-wrap items-center justify-between gap-3">
        <h3 className="panel-title">{title}</h3>
        <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400">
          <span className="flex items-center gap-1"><i className="inline-block h-2.5 w-2.5 rounded-sm" style={{ background: POLICY_COLOR.RR }} /> RR</span>
          <span className="flex items-center gap-1"><i className="inline-block h-2.5 w-2.5 rounded-sm" style={{ background: POLICY_COLOR.SRTF }} /> SRTF</span>
          {fixedPx === undefined && (
            <>
              <label className="flex items-center gap-2">
                Zoom
                <input type="range" min={4} max={60} value={zoom} onChange={(e) => setZoom(Number(e.target.value))} aria-label="Gantt zoom" />
              </label>
              <button type="button" className="btn btn-ghost px-2 py-0.5 text-xs" onClick={fit}>Fit</button>
            </>
          )}
        </div>
      </div>

      <div ref={scrollRef} className="relative overflow-x-auto rounded-md border border-ink-700 bg-ink-950/60" onMouseLeave={() => setTip(null)}>
        <svg
          width={width}
          height={height}
          role="img"
          aria-label={`${title}: ${entries.length} blocks over ${totalTime} ticks`}
          onClick={seekFromClick}
          className={onSeek ? 'cursor-crosshair' : undefined}
        >
          <defs>
            <pattern id="idle-hatch" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
              <rect width="6" height="6" fill="#141b24" />
              <line x1="0" y1="0" x2="0" y2="6" stroke="#475569" strokeWidth="2" />
            </pattern>
          </defs>

          {/* policy band */}
          {policySegments.map((s, i) => (
            <g key={i}>
              <rect x={LEFT + s.startTime * px} y={2} width={Math.max((s.endTime - s.startTime) * px, 1)} height={BAND_H - 4} fill={POLICY_COLOR[s.policy]} opacity={0.85} rx={2} />
              {(s.endTime - s.startTime) * px > 34 && (
                <text x={LEFT + s.startTime * px + 6} y={BAND_H - 6} fontSize="10" fontWeight="700" fill="#0b0f14" className="font-mono">{s.policy}</text>
              )}
            </g>
          ))}
          <text x={LEFT - 8} y={BAND_H - 6} textAnchor="end" fontSize="10" fill="#94a3b8">policy</text>

          {/* axis */}
          <g transform={`translate(0 ${BAND_H})`}>
            {ticks.map((t) => (
              <g key={t}>
                <line x1={LEFT + t * px} x2={LEFT + t * px} y1={AXIS_H - 6} y2={height} stroke="#1c2632" />
                <text x={LEFT + t * px} y={AXIS_H - 10} textAnchor="middle" fontSize="10" fill="#94a3b8" className="font-mono">{t}</text>
              </g>
            ))}
          </g>

          {/* lane labels + guides */}
          {lanes.map((lane) => (
            <g key={lane}>
              <text x={LEFT - 8} y={laneY(lane) + rowH / 2 + 4} textAnchor="end" fontSize="12" className="font-mono" fill={lane === 'IDLE' ? '#94a3b8' : colorForProcess(lane)} fontWeight="600">
                {lane}
              </text>
              <line x1={LEFT} x2={LEFT + totalTime * px} y1={laneY(lane) + rowH} y2={laneY(lane) + rowH} stroke="#141b24" />
            </g>
          ))}

          {/* execution blocks */}
          {entries.map((g, i) => {
            const lane = g.idle ? 'IDLE' : g.processId;
            const x = LEFT + g.startTime * px;
            const w = Math.max((g.endTime - g.startTime) * px, 1);
            const y = laneY(lane) + 3;
            const h = rowH - 6;
            return (
              <g
                key={i}
                tabIndex={0}
                role="img"
                aria-label={describe(g)}
                onMouseMove={(e) => showTip(e, g)}
                onFocus={(e) => showTip(e, g)}
                onBlur={() => setTip(null)}
                className="outline-none focus-visible:[&>rect]:stroke-white"
              >
                <rect x={x} y={y} width={w} height={h} rx={3} fill={g.idle ? 'url(#idle-hatch)' : colorForProcess(g.processId)} stroke={g.idle ? '#475569' : '#0b0f14'} strokeWidth={1.5} />
                <rect x={x} y={y + h - 3} width={w} height={3} fill={POLICY_COLOR[g.policy]} rx={1} />
                {w > 26 && !g.idle && !compact && (
                  <text x={x + w / 2} y={y + h / 2 + 3} textAnchor="middle" fontSize="10" fontWeight="700" fill="#0b0f14" className="font-mono">{g.endTime - g.startTime}</text>
                )}
              </g>
            );
          })}

          {/* policy transition markers */}
          {transitions.map((t, i) => (
            <g
              key={i}
              role="button"
              tabIndex={0}
              aria-label={`Policy switch at t=${t.time}: ${t.fromPolicy} to ${t.toPolicy}`}
              className="cursor-pointer outline-none focus-visible:[&>polygon]:stroke-white"
              onClick={(e) => { e.stopPropagation(); onSelectTransition?.(t); }}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSelectTransition?.(t); } }}
            >
              <line x1={LEFT + t.time * px} x2={LEFT + t.time * px} y1={0} y2={height} stroke="#34d399" strokeDasharray="4 3" strokeWidth={1.5} />
              <polygon points={`${LEFT + t.time * px - 5},0 ${LEFT + t.time * px + 5},0 ${LEFT + t.time * px},8`} fill="#34d399" />
              <title>{`t=${t.time}: ${t.fromPolicy} → ${t.toPolicy}`}</title>
            </g>
          ))}

          {/* playback cursor */}
          {currentTime !== undefined && (
            <line x1={LEFT + currentTime * px} x2={LEFT + currentTime * px} y1={BAND_H} y2={height} stroke="#f8fafc" strokeWidth={1.5} opacity={0.85} pointerEvents="none" />
          )}
        </svg>

        {tip && (
          <div role="status" className="pointer-events-none absolute z-20 rounded-md border border-ink-600 bg-ink-950 px-2.5 py-1.5 font-mono text-xs shadow-lg" style={{ left: tip.x, top: tip.y }}>
            <div className="font-semibold" style={{ color: tip.entry.idle ? '#94a3b8' : colorForProcess(tip.entry.processId) }}>
              {tip.entry.idle ? 'CPU idle' : tip.entry.processId}
            </div>
            <div>Time: {tip.entry.startTime}–{tip.entry.endTime}</div>
            <div>Policy: {tip.entry.policy}</div>
            <div>Duration: {tip.entry.endTime - tip.entry.startTime} ticks</div>
          </div>
        )}
      </div>

      {/* Text alternative for screen readers */}
      <table className="sr-only-table">
        <caption>{title} as a table</caption>
        <thead><tr><th>Process</th><th>Start</th><th>End</th><th>Policy</th></tr></thead>
        <tbody>
          {entries.map((g, i) => (
            <tr key={i}><td>{g.idle ? 'IDLE' : g.processId}</td><td>{g.startTime}</td><td>{g.endTime}</td><td>{g.policy}</td></tr>
          ))}
        </tbody>
      </table>
      {onSeek && <p className="kbd-hint mt-1">Click the chart to move the time cursor. Click a green marker for policy-switch details.</p>}
    </div>
  );
}
