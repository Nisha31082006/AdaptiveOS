import type { EngineEvent, GanttEntry, Snapshot } from '../types/simulation';

/** The snapshot describing the system during tick {@code time} (clamped to the available range). */
export function snapshotAt(snapshots: Snapshot[], time: number): Snapshot | undefined {
  if (snapshots.length === 0) return undefined;
  const t = Math.max(0, Math.min(time, snapshots[snapshots.length - 1].time));
  return snapshots.find((s) => s.time === t) ?? snapshots[snapshots.length - 1];
}

/** Events that happened at or before {@code time}. */
export function eventsUpTo(events: EngineEvent[], time: number): EngineEvent[] {
  return events.filter((e) => e.time <= time);
}

/** Choose an axis tick step so labels are at least {@code minPx} apart. */
export function niceTickStep(pxPerTick: number, minPx = 38): number {
  const steps = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000];
  return steps.find((s) => s * pxPerTick >= minPx) ?? 1000;
}

/** Ordered list of distinct process ids that appear in the Gantt data (natural order P1, P2, P10). */
export function processIdsIn(entries: GanttEntry[]): string[] {
  const ids = Array.from(new Set(entries.filter((e) => !e.idle).map((e) => e.processId)));
  return ids.sort((a, b) => Number(a.slice(1)) - Number(b.slice(1)));
}

const PALETTE = ['#60a5fa', '#f472b6', '#a78bfa', '#2dd4bf', '#fb923c', '#facc15', '#4ade80', '#f87171', '#22d3ee', '#c084fc'];

/** Stable colour per process id (P1 always gets the same colour). */
export function colorForProcess(pid: string): string {
  const n = Number(pid.slice(1));
  return PALETTE[(Number.isFinite(n) ? Math.max(n - 1, 0) : 0) % PALETTE.length];
}

export const POLICY_COLOR = { RR: '#38bdf8', SRTF: '#f59e0b' } as const;

export function fmt(n: number, digits = 2): string {
  return Number.isInteger(n) ? String(n) : n.toFixed(digits);
}

/** Human readable mode name. */
export function modeLabel(mode: string): string {
  return mode === 'PURE_RR' ? 'Pure RR' : mode === 'PURE_SRTF' ? 'Pure SRTF' : 'AdaptiveOS';
}
