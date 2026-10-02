import type { Parameters } from '../types/simulation';

/** Baseline experimental parameters (NOT claimed to be optimal). They match the Java engine defaults. */
export const DEFAULT_PARAMS: Parameters = {
  quantum: 4,
  alpha: 0.25,
  initialScore: 512,
  lowThreshold: 307,
  highThreshold: 716,
  cooldownTicks: 12,
  surgeThreshold: 3,
  antiGamingThreshold: 0.95,
};

export const MAX_SCORE = 1024;
