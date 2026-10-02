import { describe, expect, it } from 'vitest';
import { nextPid, validateParams, validateProcess, validateWorkload } from '../validation';
import { DEFAULT_PARAMS } from '../defaults';
import type { ProcessDef } from '../../types/simulation';

describe('validateParams', () => {
  it('accepts the baseline defaults', () => {
    expect(validateParams(DEFAULT_PARAMS)).toEqual({});
  });

  it('rejects alpha outside (0, 1]', () => {
    expect(validateParams({ ...DEFAULT_PARAMS, alpha: 0 }).alpha).toBeDefined();
    expect(validateParams({ ...DEFAULT_PARAMS, alpha: 1.5 }).alpha).toBeDefined();
    expect(validateParams({ ...DEFAULT_PARAMS, alpha: 1 }).alpha).toBeUndefined();
  });

  it('requires low threshold strictly below high threshold', () => {
    expect(validateParams({ ...DEFAULT_PARAMS, lowThreshold: 800, highThreshold: 700 }).lowThreshold).toBeDefined();
    expect(validateParams({ ...DEFAULT_PARAMS, lowThreshold: 700, highThreshold: 700 }).lowThreshold).toBeDefined();
  });

  it('rejects a non-integer or too-small quantum', () => {
    expect(validateParams({ ...DEFAULT_PARAMS, quantum: 0 }).quantum).toBeDefined();
    expect(validateParams({ ...DEFAULT_PARAMS, quantum: 2.5 }).quantum).toBeDefined();
  });

  it('rejects NaN values from empty number inputs', () => {
    expect(validateParams({ ...DEFAULT_PARAMS, cooldownTicks: NaN }).cooldownTicks).toBeDefined();
  });
});

const cpu = (d: number) => ({ type: 'CPU' as const, duration: d });
const io = (d: number) => ({ type: 'IO' as const, duration: d });
const proc = (pid: string, arrival: number, bursts: ProcessDef['bursts']): ProcessDef => ({ pid, arrivalTime: arrival, bursts });

describe('validateProcess', () => {
  it('accepts a normal process', () => {
    expect(validateProcess(proc('P2', 1, [cpu(3), io(4), cpu(2)]))).toEqual([]);
  });

  it('flags a bad pid format', () => {
    expect(validateProcess(proc('X1', 0, [cpu(1)]))).not.toHaveLength(0);
    expect(validateProcess(proc('P0', 0, [cpu(1)]))).not.toHaveLength(0);
  });

  it('flags a duplicate pid against the others', () => {
    expect(validateProcess(proc('P1', 0, [cpu(1)]), ['P1', 'P2'])).toContain('Process ID P1 is already used.');
  });

  it('flags negative or non-integer arrival time', () => {
    expect(validateProcess(proc('P1', -1, [cpu(1)]))).not.toHaveLength(0);
    expect(validateProcess(proc('P1', 1.5, [cpu(1)]))).not.toHaveLength(0);
  });

  it('flags an empty burst list', () => {
    expect(validateProcess(proc('P1', 0, []))).toContain('Add at least one burst.');
  });

  it('flags non-positive burst duration', () => {
    const errors = validateProcess(proc('P1', 0, [cpu(0)]));
    expect(errors.some((e) => e.includes('duration'))).toBe(true);
  });

  it('requires the first and last burst to be CPU', () => {
    expect(validateProcess(proc('P1', 0, [io(2), cpu(3)]))).toContain('The first burst must be CPU.');
    expect(validateProcess(proc('P1', 0, [cpu(2), io(3)]))).toContain('The last burst must be CPU (a process cannot end while in I/O).');
  });

  it('flags two consecutive I/O bursts', () => {
    const errors = validateProcess(proc('P1', 0, [cpu(1), io(1), io(1), cpu(1)]));
    expect(errors.some((e) => e.includes('I/O'))).toBe(true);
  });
});

describe('validateWorkload', () => {
  it('flags an empty workload', () => {
    expect(validateWorkload([])[0]).toMatch(/empty/i);
  });

  it('is valid for the baseline-like workload', () => {
    expect(validateWorkload([proc('P1', 0, [cpu(10)]), proc('P2', 1, [cpu(3), io(4), cpu(2)]), proc('P3', 2, [cpu(6)])])).toEqual([]);
  });

  it('prefixes each error with the offending process id', () => {
    const errors = validateWorkload([proc('P1', -1, [cpu(1)])]);
    expect(errors[0].startsWith('P1:')).toBe(true);
  });
});

describe('nextPid', () => {
  it('starts at P1 for an empty workload', () => {
    expect(nextPid([])).toBe('P1');
  });

  it('continues after the highest existing pid', () => {
    expect(nextPid([proc('P1', 0, [cpu(1)]), proc('P3', 0, [cpu(1)])])).toBe('P4');
  });
});
