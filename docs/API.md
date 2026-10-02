# API Reference

Base URL: `http://localhost:8080/api` (configurable on the frontend via `VITE_API_BASE_URL`).
All responses are JSON. All errors use the shape described in [Errors](#errors).

## POST /api/simulate

Runs one simulation.

### Request body
```jsonc
{
  "mode": "ADAPTIVE",           // required: "PURE_RR" | "PURE_SRTF" | "ADAPTIVE"
  "quantum": 4,                 // optional, all parameters default to the baseline values below
  "alpha": 0.25,
  "initialScore": 512,
  "lowThreshold": 307,
  "highThreshold": 716,
  "cooldownTicks": 12,
  "surgeThreshold": 3,
  "antiGamingThreshold": 0.95,
  "processes": [
    { "pid": "P1", "arrivalTime": 0, "bursts": [{ "type": "CPU", "duration": 10 }] },
    { "pid": "P2", "arrivalTime": 1, "bursts": [
        { "type": "CPU", "duration": 3 }, { "type": "IO", "duration": 4 }, { "type": "CPU", "duration": 2 }
    ]},
    { "pid": "P3", "arrivalTime": 2, "bursts": [{ "type": "CPU", "duration": 6 }] }
  ]
}
```

### Response body (shape; all values come from the engine)
```jsonc
{
  "mode": "ADAPTIVE",
  "parameters": { "quantum": 4, "alpha": 0.25, "...": "...", "maxScore": 1024 },
  "metrics": {
    "averageWaitingTime": 9.67, "averageTurnaroundTime": 18.0, "averageResponseTime": 2.67,
    "contextSwitches": 5, "cpuUtilization": 100.0, "maxWaitingTime": 13,
    "totalTime": 21, "busyTime": 21, "policySwitches": 1, "rrDuration": 15, "srtfDuration": 6
  },
  "processResults": [ { "pid": "P1", "arrivalTime": 0, "startTime": 0, "completionTime": 21, "...": "..." } ],
  "ganttEntries": [ { "processId": "P1", "startTime": 0, "endTime": 4, "policy": "RR", "idle": false } ],
  "policySegments": [ { "policy": "RR", "startTime": 0, "endTime": 15 }, { "policy": "SRTF", "startTime": 15, "endTime": 21 } ],
  "policyTransitions": [
    { "time": 15, "fromPolicy": "RR", "toPolicy": "SRTF", "reason": "score 306.0 < LOW 307",
      "interactivityScore": 306.0, "surgeCounter": 2, "cooldownUntil": 27 }
  ],
  "events": [ { "time": 4, "type": "QUANTUM_EXPIRED", "processId": "P1", "policy": "RR", "message": "P1 quantum expired", "adaptive": false } ],
  "snapshots": [ { "time": 0, "runningProcessId": "P1", "policy": "RR", "interactivityScore": 512.0, "surgeCounter": 0, "cooldownUntil": 0, "cooldownActive": false, "classification": "Neutral (inside hysteresis band)", "processes": [ { "pid": "P1", "state": "RUNNING", "burstType": "CPU", "remaining": 9 } ] } ],
  "finalPolicy": "SRTF",
  "totalTime": 21
}
```

`ganttEntries` are the engine's execution blocks, split at policy-switch boundaries so every entry has
exactly one `policy` (splitting only — it never invents or moves execution time). `snapshots` has one
entry per tick plus a final one at `totalTime`. `events[].adaptive` is true for events produced by the
`AdaptiveController` (EWMA update, surge, anti-gaming, cooldown, policy switch) as opposed to the plain
scheduler (arrival, dispatch, quantum expiry, I/O, termination).

## POST /api/compare

Same request body as `/simulate` but `mode` is ignored. Runs `PURE_RR`, `PURE_SRTF` and `ADAPTIVE` on
identical copies of the workload and parameters.

```jsonc
{ "parameters": { "...": "..." }, "pureRr": { /* SimulationResponse */ }, "pureSrtf": { "...": "..." }, "adaptive": { "...": "..." } }
```

## GET /api/workloads

Returns every predefined workload:
```jsonc
[ { "id": "baseline", "name": "Basic Mixed", "description": "...", "category": "core", "processes": [ "..." ] }, "..." ]
```
`category` is `"core"` (Basic Mixed, CPU Heavy, Interactive, Mixed, Phase Change) or `"edge"` (the
remaining edge-case workloads: single process, all-arrive-at-zero, burst-equals-quantum, SRTF ties, etc).

## GET /api/workloads/{id}

Returns one workload by id (404 if unknown).

## GET /api/health

`{ "status": "UP" }` — used by the frontend status indicator and by Docker healthchecks.

## Errors

Every non-2xx response has this shape:
```json
{ "timestamp": "2026-01-01T12:00:00Z", "status": 400, "message": "EWMA alpha must be in (0, 1], but was 3.0", "path": "/api/simulate" }
```
`400` — invalid request (bad JSON, invalid parameter, invalid process, duplicate PID, empty workload, ...).
`404` — unknown workload id. `405` / `415` — wrong method / content type. `500` — unexpected server error
(logged server-side; the client never receives a stack trace).
