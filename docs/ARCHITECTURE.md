# Architecture

```
React Frontend (Vite, TypeScript, Tailwind, Recharts)
        │  fetch() JSON over HTTP
        ▼
Spring Boot REST API  (com.adaptiveos.controller / dto / service / config)
        │  plain Java method calls (no network, no serialization)
        ▼
AdaptiveOS Java Engine (com.adaptiveos.engine.*)
        │
   ┌────┼─────┐
   ▼    ▼     ▼
  RR  SRTF  AdaptiveController
```

## Layers

**Engine (`backend/src/main/java/com/adaptiveos/engine/...`)**
The original, framework-free AdaptiveOS simulator: `model`, `scheduler`, `simulation`, `metrics`,
`workload`, `logging`. It has no Spring annotations and no HTTP awareness. It is the single source of
truth for scheduling behaviour; it was relocated into an `engine` sub-package but its logic,
package-internal structure and public API were preserved. Two additions were made purely to expose more
of what the engine already computes:
- `logging.EventType` / `logging.SimEvent`: the existing free-text log lines are now also emitted as
  structured `(time, type, processId, policy, message)` records, so the web UI can filter them.
- `simulation.ProcessSnapshot` / `simulation.TickSnapshot`: the engine now records, once per tick, which
  process is running, the active policy, and (for ADAPTIVE) the controller's score/surge/cooldown — all
  values it was already computing internally.
- `AdaptiveController.PolicySwitch` gained `score`, `surgeCounter` and `cooldownUntil` fields, again
  values the controller already had at the moment of the switch.

No scheduling decision, threshold, formula or ordering rule was changed.

**Service (`backend/src/main/java/com/adaptiveos/service/SimulationService.java`)**
The only bridge between REST and the engine. It validates the untrusted request, builds an
`engine.workload.Workload` and `engine.config.SchedulerConfig`, runs the real
`engine.simulation.SimulationEngine`, and maps the resulting `SimulationResult` into DTOs. It contains
**no scheduling logic** — every number in a response is read from the engine's output. It has no Spring
dependency, so it is unit-tested directly and is also reusable outside the web app.

**DTOs (`backend/src/main/java/com/adaptiveos/dto`)**
Plain Java records mirroring the JSON shapes in [API.md](API.md).

**Controllers (`backend/src/main/java/com/adaptiveos/controller`)**
Thin `@RestController`s that call `SimulationService` and return DTOs directly (Spring's Jackson
integration serializes them). `GlobalExceptionHandler` turns every failure into the consistent
`{timestamp, status, message, path}` shape and never leaks a stack trace to the client.

**Frontend (`frontend/src`)**
- `services/api.ts` — the only place that calls `fetch`; every other file goes through it.
- `state/WorkspaceContext.tsx` — holds the current workload, parameters, mode and the last
  simulate/compare result; performs no scheduling computation itself.
- `components/` — presentational + interactive pieces (Gantt chart, policy timeline, score chart,
  adaptive monitor, event log, process states, metrics cards/comparison, process/burst editors).
- `pages/` — Home, Simulator, Comparison, How It Works, About.

## Why the engine was not duplicated

The Spring Boot module depends on the engine as ordinary Java source (same Maven module, `engine`
sub-package) and calls `new SimulationEngine(...).run()` directly. There is exactly one implementation
of Round Robin, SRTF and the adaptive controller; the frontend never computes scheduling results.
