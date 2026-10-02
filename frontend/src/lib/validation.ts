import type { Parameters, ProcessDef } from '../types/simulation';
import { MAX_SCORE } from './defaults';

// These limits mirror the backend (SimulationService). The backend re-validates everything.
export const LIMITS = {
  maxProcesses: 50,
  maxBursts: 101,
  maxBurstDuration: 1000,
  maxArrival: 10_000,
  maxQuantum: 1000,
  maxCooldown: 100_000,
  maxSurge: 1000,
} as const;

export type ParamErrors = Partial<Record<keyof Parameters, string>>;

const isWhole = (n: number) => Number.isInteger(n);

export function validateParams(p: Parameters): ParamErrors {
  const e: ParamErrors = {};
  if (!Number.isFinite(p.quantum) || !isWhole(p.quantum) || p.quantum < 1 || p.quantum > LIMITS.maxQuantum)
    e.quantum = `Quantum must be a whole number between 1 and ${LIMITS.maxQuantum}.`;
  if (!Number.isFinite(p.alpha) || p.alpha <= 0 || p.alpha > 1) e.alpha = 'Alpha must be greater than 0 and at most 1.';
  if (!Number.isFinite(p.initialScore) || p.initialScore < 0 || p.initialScore > MAX_SCORE)
    e.initialScore = `Initial score must be between 0 and ${MAX_SCORE}.`;
  if (!Number.isFinite(p.lowThreshold) || p.lowThreshold < 0 || p.lowThreshold > MAX_SCORE)
    e.lowThreshold = `Low threshold must be between 0 and ${MAX_SCORE}.`;
  if (!Number.isFinite(p.highThreshold) || p.highThreshold < 0 || p.highThreshold > MAX_SCORE)
    e.highThreshold = `High threshold must be between 0 and ${MAX_SCORE}.`;
  if (!e.lowThreshold && !e.highThreshold && p.lowThreshold >= p.highThreshold)
    e.lowThreshold = 'Low threshold must be smaller than the high threshold (they form the hysteresis band).';
  if (!Number.isFinite(p.cooldownTicks) || !isWhole(p.cooldownTicks) || p.cooldownTicks < 0 || p.cooldownTicks > LIMITS.maxCooldown)
    e.cooldownTicks = 'Cooldown must be a whole number of ticks (0 or more).';
  if (!Number.isFinite(p.surgeThreshold) || !isWhole(p.surgeThreshold) || p.surgeThreshold < 1 || p.surgeThreshold > LIMITS.maxSurge)
    e.surgeThreshold = 'Surge threshold must be a whole number of at least 1.';
  if (!Number.isFinite(p.antiGamingThreshold) || p.antiGamingThreshold <= 0 || p.antiGamingThreshold > 1)
    e.antiGamingThreshold = 'Anti-gaming threshold must be greater than 0 and at most 1.';
  return e;
}

const PID_RE = /^P[1-9]\d{0,5}$/;

/** Problems with ONE process. {@code otherPids} are the PIDs of the other processes (for duplicate detection). */
export function validateProcess(p: ProcessDef, otherPids: string[] = []): string[] {
  const errors: string[] = [];
  const pid = p.pid.trim();
  if (!PID_RE.test(pid)) errors.push('Process ID must look like P1, P2, ... (P followed by a positive number).');
  else if (otherPids.includes(pid)) errors.push(`Process ID ${pid} is already used.`);
  if (!Number.isFinite(p.arrivalTime) || !isWhole(p.arrivalTime) || p.arrivalTime < 0 || p.arrivalTime > LIMITS.maxArrival)
    errors.push(`Arrival time must be a whole number between 0 and ${LIMITS.maxArrival}.`);
  if (p.bursts.length === 0) errors.push('Add at least one burst.');
  if (p.bursts.length > LIMITS.maxBursts) errors.push(`At most ${LIMITS.maxBursts} bursts are allowed.`);
  p.bursts.forEach((b, i) => {
    if (!Number.isFinite(b.duration) || !isWhole(b.duration) || b.duration <= 0)
      errors.push(`Burst ${i + 1}: duration must be a positive whole number.`);
    else if (b.duration > LIMITS.maxBurstDuration) errors.push(`Burst ${i + 1}: duration must be at most ${LIMITS.maxBurstDuration}.`);
  });
  if (p.bursts.length > 0) {
    if (p.bursts[0].type !== 'CPU') errors.push('The first burst must be CPU.');
    if (p.bursts[p.bursts.length - 1].type !== 'CPU') errors.push('The last burst must be CPU (a process cannot end while in I/O).');
    for (let i = 1; i < p.bursts.length; i++)
      if (p.bursts[i].type === 'IO' && p.bursts[i - 1].type === 'IO') {
        errors.push('Two I/O bursts in a row: merge them into one.');
        break;
      }
  }
  return errors;
}

/** Problems with the whole workload (empty, too big, duplicate PIDs, or any invalid process). */
export function validateWorkload(processes: ProcessDef[]): string[] {
  if (processes.length === 0) return ['The workload is empty. Add at least one process or load an example.'];
  if (processes.length > LIMITS.maxProcesses) return [`At most ${LIMITS.maxProcesses} processes are allowed.`];
  const errors: string[] = [];
  processes.forEach((p, i) => {
    const others = processes.filter((_, j) => j !== i).map((q) => q.pid.trim());
    validateProcess(p, others).forEach((m) => errors.push(`${p.pid || 'Process ' + (i + 1)}: ${m}`));
  });
  return errors;
}

export function nextPid(processes: ProcessDef[]): string {
  const used = processes.map((p) => Number(p.pid.replace(/^P/i, ''))).filter((n) => Number.isFinite(n));
  return `P${(used.length ? Math.max(...used) : 0) + 1}`;
}
