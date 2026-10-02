import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, ApiError } from '../services/api';
import { DEFAULT_PARAMS } from '../lib/defaults';
import { validateParams, validateWorkload } from '../lib/validation';
import type {
  CompareResponse,
  ExecutionMode,
  Parameters,
  ProcessDef,
  SimulationResponse,
  WorkloadInfo,
} from '../types/simulation';

type Loading = 'idle' | 'simulate' | 'compare';
type Backend = 'checking' | 'online' | 'offline';

interface Workspace {
  processes: ProcessDef[];
  setProcesses: (p: ProcessDef[]) => void;
  params: Parameters;
  setParams: (p: Parameters) => void;
  mode: ExecutionMode;
  setMode: (m: ExecutionMode) => void;
  workloads: WorkloadInfo[];
  selectedWorkloadId: string;
  loadWorkload: (id: string) => void;
  backend: Backend;
  result: SimulationResponse | null;
  compare: CompareResponse | null;
  loading: Loading;
  errors: string[];
  clearErrors: () => void;
  stale: boolean;
  compareStale: boolean;
  run: () => Promise<boolean>;
  runCompare: () => Promise<boolean>;
  reset: () => void;
}

const Ctx = createContext<Workspace | null>(null);

const clone = <T,>(v: T): T => JSON.parse(JSON.stringify(v)) as T;

/**
 * Holds the user's workload, parameters and the LAST results returned by the backend.
 * It never computes scheduling results itself: it only calls the API.
 */
export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const [processes, setProcesses] = useState<ProcessDef[]>([]);
  const [params, setParams] = useState<Parameters>(DEFAULT_PARAMS);
  const [mode, setMode] = useState<ExecutionMode>('ADAPTIVE');
  const [workloads, setWorkloads] = useState<WorkloadInfo[]>([]);
  const [selectedWorkloadId, setSelectedWorkloadId] = useState('baseline');
  const [backend, setBackend] = useState<Backend>('checking');
  const [result, setResult] = useState<SimulationResponse | null>(null);
  const [compare, setCompare] = useState<CompareResponse | null>(null);
  const [loading, setLoading] = useState<Loading>('idle');
  const [errors, setErrors] = useState<string[]>([]);
  const [resultKey, setResultKey] = useState('');
  const [compareKey, setCompareKey] = useState('');

  // Load predefined workloads from the backend once; preload the first (baseline) into the editor.
  useEffect(() => {
    let cancelled = false;
    api
      .workloads()
      .then((list) => {
        if (cancelled) return;
        setWorkloads(list);
        setBackend('online');
        const first = list.find((w) => w.id === 'baseline') ?? list[0];
        if (first) {
          setSelectedWorkloadId(first.id);
          setProcesses(clone(first.processes));
        }
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setBackend('offline');
        setErrors([e instanceof ApiError ? e.message : 'Could not load the example workloads.']);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const inputKey = useMemo(() => JSON.stringify({ processes, params }), [processes, params]);
  const stale = (result !== null && resultKey !== inputKey + mode) || false;

  const loadWorkload = useCallback(
    (id: string) => {
      const w = workloads.find((x) => x.id === id);
      if (!w) return;
      setSelectedWorkloadId(id);
      setProcesses(clone(w.processes));
      setErrors([]);
    },
    [workloads],
  );

  const validate = useCallback((): string[] => {
    const problems = [...validateWorkload(processes)];
    Object.values(validateParams(params)).forEach((m) => m && problems.push(m));
    return problems;
  }, [processes, params]);

  const run = useCallback(async () => {
    const problems = validate();
    if (problems.length) {
      setErrors(problems);
      return false;
    }
    setErrors([]);
    setLoading('simulate');
    try {
      const response = await api.simulate({ ...params, mode, processes });
      setResult(response);
      setResultKey(inputKey + mode);
      return true;
    } catch (e) {
      setErrors([e instanceof ApiError ? e.message : 'The simulation failed unexpectedly.']);
      return false;
    } finally {
      setLoading('idle');
    }
  }, [validate, params, mode, processes, inputKey]);

  const runCompare = useCallback(async () => {
    const problems = validate();
    if (problems.length) {
      setErrors(problems);
      return false;
    }
    setErrors([]);
    setLoading('compare');
    try {
      const response = await api.compare({ ...params, processes });
      setCompare(response);
      setCompareKey(inputKey);
      return true;
    } catch (e) {
      setErrors([e instanceof ApiError ? e.message : 'The comparison failed unexpectedly.']);
      return false;
    } finally {
      setLoading('idle');
    }
  }, [validate, params, processes, inputKey]);

  const reset = useCallback(() => {
    setResult(null);
    setCompare(null);
    setErrors([]);
    setParams(DEFAULT_PARAMS);
    const w = workloads.find((x) => x.id === selectedWorkloadId);
    setProcesses(w ? clone(w.processes) : []);
  }, [workloads, selectedWorkloadId]);

  const value: Workspace = {
    processes,
    setProcesses,
    params,
    setParams,
    mode,
    setMode,
    workloads,
    selectedWorkloadId,
    loadWorkload,
    backend,
    result,
    compare,
    compareStale: compare !== null && compareKey !== inputKey,
    loading,
    errors,
    clearErrors: () => setErrors([]),
    stale,
    run,
    runCompare,
    reset,
  };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useWorkspace(): Workspace {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useWorkspace must be used inside <WorkspaceProvider>');
  return ctx;
}

/** True when the comparison shown no longer matches the current workload/parameters. */
export function useCompareStale(): boolean {
  const { compare, processes, params } = useWorkspace();
  const [key, setKey] = useState<string | null>(null);
  const current = JSON.stringify({ processes, params });
  useEffect(() => {
    if (compare) setKey(current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [compare]);
  return compare !== null && key !== null && key !== current;
}
