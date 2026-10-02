# Manual End-to-End Testing Checklist

Automated coverage: 63 engine tests + 81 service tests (JUnit 5, run with `mvn test`), 45 frontend
tests (Vitest + Testing Library, run with `npm test`), and — during development of this project —
a real headless-Chromium walkthrough of the built frontend against a running backend. Use this
checklist for a manual pass after any change, or before a demo.

## Setup
- [ ] `cd backend && mvn spring-boot:run` — server starts on :8080, `GET /api/health` returns `{"status":"UP"}`
- [ ] `cd frontend && npm install && npm run dev` — opens on :5173, navbar shows "Backend online"
- [ ] Stop the backend: navbar switches to "Backend unreachable" and a red banner appears (no crash)

## Workloads (Simulator page)
For each of **Basic Mixed, CPU Heavy, Interactive, Phase Change, and one Edge Case** (e.g. "SRTF ties"):
- [ ] Loading the example populates the process table correctly
- [ ] Run under Pure RR, Pure SRTF, and AdaptiveOS — each completes without error
- [ ] Gantt chart blocks are contiguous, add up to the total time, and match the metrics table
- [ ] For AdaptiveOS: the score chart, policy timeline and event log all show the same switch time(s)
- [ ] Metrics shown on screen match `mvn test` / the CLI (`docs/ARCHITECTURE.md` describes how to
      cross-check against the standalone engine)

## Process builder
- [ ] Add a process, add/reorder/delete bursts, save — appears correctly in the table
- [ ] Edit an existing process; cancel discards changes
- [ ] Delete a process
- [ ] Validation: empty PID, duplicate PID, negative arrival, zero/negative duration, first burst not
      CPU, last burst not CPU, two consecutive I/O bursts — each shows a clear inline message and blocks
      the run

## Parameters
- [ ] Change quantum, alpha, thresholds, cooldown, surge, anti-gaming — Run reflects the new values
- [ ] Invalid values (alpha=0, alpha=2, low ≥ high, quantum=0) show inline errors and are rejected by
      the backend too if bypassed
- [ ] "Restore baseline parameters" resets all eight fields

## Gantt chart & timeline
- [ ] Hover a block shows process/time/policy/duration tooltip
- [ ] Click the chart moves the time cursor; Play/Pause/step buttons work; speed selector changes rate
- [ ] Clicking a green policy-switch marker shows time/score/surge/reason/cooldown
- [ ] Zoom slider and "Fit" work without breaking the layout

## Comparison page
- [ ] "Compare All" runs all three modes on the current workload/parameters
- [ ] Table + bar charts + CPU utilization chart all show consistent numbers
- [ ] No policy is labeled "best" or "winner" anywhere on the page
- [ ] Changing the workload and returning shows a "stale" notice until Compare Again is clicked

## Error handling
- [ ] Stop the backend mid-session, click Run — friendly network error, no stack trace, app stays usable
- [ ] Malformed manual request (e.g. via curl) returns the standard `{timestamp,status,message,path}` body

## Accessibility / responsiveness
- [ ] Tab through the Simulator page — every control is reachable and has a visible focus ring
- [ ] Screen-reader table fallback exists for the Gantt chart (inspect the DOM: `.sr-only-table`)
- [ ] Resize to tablet width (≈768px) — layout reflows without breaking; desktop remains the primary target
