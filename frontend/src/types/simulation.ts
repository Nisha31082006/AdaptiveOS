// TypeScript mirror of the backend DTOs (com.adaptiveos.dto). Every field is produced by the Java engine.

export type ExecutionMode = 'PURE_RR' | 'PURE_SRTF' | 'ADAPTIVE';
export type Policy = 'RR' | 'SRTF';
export type BurstType = 'CPU' | 'IO';
export type ProcessState = 'NEW' | 'READY' | 'RUNNING' | 'WAITING' | 'TERMINATED';

export interface Burst {
  type: BurstType;
  duration: number;
}

export interface ProcessDef {
  pid: string;
  arrivalTime: number;
  bursts: Burst[];
}

export interface Parameters {
  quantum: number;
  alpha: number;
  initialScore: number;
  lowThreshold: number;
  highThreshold: number;
  cooldownTicks: number;
  surgeThreshold: number;
  antiGamingThreshold: number;
}

export interface ParametersResponse extends Parameters {
  maxScore: number;
}

export interface SimulationRequest extends Parameters {
  mode?: ExecutionMode;
  processes: ProcessDef[];
}

export interface Metrics {
  averageWaitingTime: number;
  averageTurnaroundTime: number;
  averageResponseTime: number;
  contextSwitches: number;
  cpuUtilization: number;
  maxWaitingTime: number;
  totalTime: number;
  busyTime: number;
  policySwitches: number;
  rrDuration: number;
  srtfDuration: number;
}

export interface ProcessResult {
  pid: string;
  arrivalTime: number;
  startTime: number;
  completionTime: number;
  turnaroundTime: number;
  waitingTime: number;
  responseTime: number;
  contextSwitches: number;
  totalCpuTime: number;
  totalIoTime: number;
  bursts: Burst[];
}

export interface GanttEntry {
  processId: string;
  startTime: number;
  endTime: number;
  policy: Policy;
  idle: boolean;
}

export interface PolicySegment {
  policy: Policy;
  startTime: number;
  endTime: number;
}

export interface PolicyTransition {
  time: number;
  fromPolicy: Policy;
  toPolicy: Policy;
  reason: string;
  interactivityScore: number;
  surgeCounter: number;
  cooldownUntil: number;
}

export interface EngineEvent {
  time: number;
  type: string;
  processId: string | null;
  policy: Policy;
  message: string;
  adaptive: boolean;
}

export interface ProcessSnapshot {
  pid: string;
  state: ProcessState;
  burstType: 'CPU' | 'IO' | 'NONE';
  remaining: number;
}

export interface Snapshot {
  time: number;
  runningProcessId: string | null;
  policy: Policy;
  interactivityScore: number | null;
  surgeCounter: number | null;
  cooldownUntil: number | null;
  cooldownActive: boolean;
  classification: string | null;
  processes: ProcessSnapshot[];
}

export interface SimulationResponse {
  mode: ExecutionMode;
  parameters: ParametersResponse;
  metrics: Metrics;
  processResults: ProcessResult[];
  ganttEntries: GanttEntry[];
  policySegments: PolicySegment[];
  policyTransitions: PolicyTransition[];
  events: EngineEvent[];
  snapshots: Snapshot[];
  finalPolicy: Policy;
  totalTime: number;
}

export interface CompareResponse {
  parameters: ParametersResponse;
  pureRr: SimulationResponse;
  pureSrtf: SimulationResponse;
  adaptive: SimulationResponse;
}

export interface WorkloadInfo {
  id: string;
  name: string;
  description: string;
  category: 'core' | 'edge';
  processes: ProcessDef[];
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  message: string;
  path: string;
}
