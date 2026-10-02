import { describe, expect, it } from 'vitest';
import { colorForProcess, eventsUpTo, fmt, modeLabel, niceTickStep, processIdsIn, snapshotAt } from '../timeline';
import type { EngineEvent, GanttEntry, Snapshot } from '../../types/simulation';

describe('snapshotAt', () => {
  const snapshots: Snapshot[] = [0, 1, 2].map((t) => ({
    time: t, runningProcessId: `P${t}`, policy: 'RR', interactivityScore: null, surgeCounter: null,
    cooldownUntil: null, cooldownActive: false, classification: null, processes: [],
  }));

  it('returns the snapshot at an exact time', () => {
    expect(snapshotAt(snapshots, 1)?.runningProcessId).toBe('P1');
  });

  it('clamps to the last snapshot when time exceeds the range', () => {
    expect(snapshotAt(snapshots, 99)?.time).toBe(2);
  });

  it('returns undefined for an empty list', () => {
    expect(snapshotAt([], 0)).toBeUndefined();
  });
});

describe('eventsUpTo', () => {
  const events: EngineEvent[] = [0, 4, 8].map((t) => ({ time: t, type: 'X', processId: null, policy: 'RR', message: '', adaptive: false }));
  it('keeps only events at or before the given time', () => {
    expect(eventsUpTo(events, 4)).toHaveLength(2);
  });
});

describe('niceTickStep', () => {
  it('picks a small step when zoomed in', () => {
    expect(niceTickStep(40)).toBe(1);
  });
  it('picks a larger step when zoomed out', () => {
    expect(niceTickStep(1)).toBeGreaterThanOrEqual(20);
  });
});

describe('processIdsIn', () => {
  it('sorts numerically and excludes idle entries', () => {
    const entries: GanttEntry[] = [
      { processId: 'P10', startTime: 0, endTime: 1, policy: 'RR', idle: false },
      { processId: 'P2', startTime: 1, endTime: 2, policy: 'RR', idle: false },
      { processId: 'IDLE', startTime: 2, endTime: 3, policy: 'RR', idle: true },
    ];
    expect(processIdsIn(entries)).toEqual(['P2', 'P10']);
  });
});

describe('colorForProcess', () => {
  it('is stable for the same pid', () => {
    expect(colorForProcess('P3')).toBe(colorForProcess('P3'));
  });
});

describe('fmt / modeLabel', () => {
  it('shows integers without decimals', () => {
    expect(fmt(4)).toBe('4');
    expect(fmt(4.5)).toBe('4.50');
  });
  it('gives human labels for engine modes', () => {
    expect(modeLabel('PURE_RR')).toBe('Pure RR');
    expect(modeLabel('ADAPTIVE')).toBe('AdaptiveOS');
  });
});
