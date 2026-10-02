package com.adaptiveos.engine.model;

import java.util.List;

/**
 * Process Control Block (PCB): everything the simulated OS knows about one process.
 * <p>
 * Static description (never changes): pid, name, arrivalTime, bursts, totalCpuTime, totalIoTime.<br>
 * Dynamic state (changes during the simulation): everything else.
 * <p>
 * Instances are mutable and belong to exactly ONE simulation run. Fresh copies are created from a
 * {@link com.adaptiveos.engine.workload.Workload} for every run so that runs never influence each other.
 */
public final class ProcessControlBlock {

    // ------------------------------------------------------------------ static description
    /** Unique process id (1, 2, 3 ...). Used as the last tie-breaker so ordering is deterministic. */
    private final int pid;
    /** Human readable name such as "P1". */
    private final String name;
    /** Tick at which the process enters the system. */
    private final int arrivalTime;
    /** Full burst list, e.g. CPU 3 -> IO 4 -> CPU 2. Never modified; we move an index instead. */
    private final List<Burst> bursts;
    /** Sum of all CPU bursts (used for waiting-time formula). */
    private final int totalCpuTime;
    /** Sum of all I/O bursts (used for waiting-time formula). */
    private final int totalIoTime;

    // ------------------------------------------------------------------ dynamic scheduling state
    /** Lifecycle state (NEW, READY, RUNNING, WAITING, TERMINATED). */
    private ProcessState state = ProcessState.NEW;
    /** Index into {@link #bursts} of the burst the process is currently in. Avoids removing list elements. */
    private int currentBurstIndex = 0;
    /**
     * Remaining ticks of the CURRENT CPU burst. SRTF compares this value (not total lifetime work).
     * It is 0 while the process is inside an I/O burst.
     */
    private int remainingTime;
    /** Remaining ticks of the current I/O burst (only meaningful in state WAITING). */
    private int ioRemaining;
    /**
     * Policy that was active the last time this process was queued/dispatched. INFORMATIONAL ONLY:
     * the policy is global, so this is never a permanent per-process assignment.
     */
    private SchedulingPolicy currentPolicy;

    // ------------------------------------------------------------------ per-turn tracking (adaptive input)
    /** Length (in ticks) of the time slice granted at the start of the current CPU "turn" (the quantum). */
    private int allocatedQuantumTurn;
    /** Ticks actually executed during the current turn. E = executedBurst / allocatedQuantumTurn. */
    private int executedBurst;

    // ------------------------------------------------------------------ per-process behaviour telemetry
    /**
     * This process's own EWMA interactivity score (0..1024). Purely informational / for debugging.
     * The score that DRIVES decisions is the global one inside AdaptiveController.
     */
    private double interactivityScore;
    /** Consecutive CPU-heavy events seen from this process (informational; reset by an interactive event). */
    private int surgeCounter;
    /** Snapshot of the controller's cooldown expiry tick when this process last produced an event (informational). */
    private long safeUntil;

    // ------------------------------------------------------------------ metrics
    /** Tick of first CPU dispatch (-1 = never ran yet). */
    private long startTime = -1;
    /** Tick at which the last burst finished (-1 = not finished). */
    private long completionTime = -1;
    /** waitingTime = turnaround - totalCpuTime - totalIoTime (filled in at termination). */
    private long waitingTime;
    /** responseTime = startTime - arrivalTime (filled in at first dispatch). */
    private long responseTime = -1;
    /** Cross-check: number of ticks actually spent sitting in a ready queue. Must equal waitingTime. */
    private long readyWaitTicks;
    /** How many times the CPU switched TO this process from a different process. */
    private int contextSwitches;

    public ProcessControlBlock(int pid, int arrivalTime, List<Burst> bursts, double initialScore) {
        if (bursts == null || bursts.isEmpty()) {
            throw new IllegalArgumentException("Process P" + pid + " must have at least one burst");
        }
        if (bursts.get(0).type() != BurstType.CPU) {
            throw new IllegalArgumentException("Process P" + pid + " must start with a CPU burst");
        }
        this.pid = pid;
        this.name = "P" + pid;
        this.arrivalTime = arrivalTime;
        this.bursts = List.copyOf(bursts);
        this.totalCpuTime = sumOf(BurstType.CPU);
        this.totalIoTime = sumOf(BurstType.IO);
        this.interactivityScore = initialScore;
        this.remainingTime = this.bursts.get(0).duration();
    }

    private int sumOf(BurstType type) {
        int sum = 0;
        for (Burst b : bursts) {
            if (b.type() == type) {
                sum += b.duration();
            }
        }
        return sum;
    }

    // ------------------------------------------------------------------ burst navigation
    /** True while the process still has a current burst (false once every burst has been finished). */
    public boolean hasCurrentBurst() {
        return currentBurstIndex < bursts.size();
    }

    public Burst currentBurst() {
        return bursts.get(currentBurstIndex);
    }

    /** Moves to the next burst (index + 1). */
    public void moveToNextBurst() {
        currentBurstIndex++;
    }

    /** Loads the current (CPU) burst duration into {@link #remainingTime}. */
    public void loadCpuBurst() {
        remainingTime = currentBurst().duration();
    }

    /** Starts the current (I/O) burst: no CPU work left, ioRemaining set to its duration. */
    public void startIo() {
        remainingTime = 0;
        ioRemaining = currentBurst().duration();
    }

    // ------------------------------------------------------------------ execution
    /** Executes ONE tick on the CPU. */
    public void runOneTick() {
        remainingTime--;
        executedBurst++;
    }

    public boolean isCpuBurstFinished() {
        return remainingTime == 0;
    }

    /** Advances I/O by one tick. */
    public void tickIo() {
        ioRemaining--;
    }

    public boolean isIoFinished() {
        return ioRemaining == 0;
    }

    /** Begins a new turn (time slice) of the given length. */
    public void beginTurn(int quantum) {
        allocatedQuantumTurn = quantum;
        executedBurst = 0;
    }

    /** E = executedBurst / allocatedQuantumTurn, the fraction of the slice this turn actually used. */
    public double quantumUsageRatio() {
        return allocatedQuantumTurn == 0 ? 0.0 : (double) executedBurst / allocatedQuantumTurn;
    }

    // ------------------------------------------------------------------ telemetry
    /** Updates this process's own EWMA score. */
    public void updateOwnScore(double signal, double alpha, boolean interactive, long controllerSafeUntil) {
        interactivityScore = alpha * signal + (1.0 - alpha) * interactivityScore;
        surgeCounter = interactive ? 0 : surgeCounter + 1;
        safeUntil = controllerSafeUntil;
    }

    // ------------------------------------------------------------------ metrics helpers
    public void markFirstDispatch(long now) {
        if (startTime < 0) {
            startTime = now;
            responseTime = now - arrivalTime;
        }
    }

    public void incrementReadyWait() {
        readyWaitTicks++;
    }

    public void incrementContextSwitches() {
        contextSwitches++;
    }

    /** Called when the last CPU burst finishes at time {@code now}. */
    public void markCompleted(long now) {
        completionTime = now;
        waitingTime = getTurnaroundTime() - totalCpuTime - totalIoTime;
        state = ProcessState.TERMINATED;
    }

    public long getTurnaroundTime() {
        return completionTime - arrivalTime;
    }

    // ------------------------------------------------------------------ plain accessors
    public int getPid() { return pid; }
    public String getName() { return name; }
    public int getArrivalTime() { return arrivalTime; }
    public List<Burst> getBursts() { return bursts; }
    public int getTotalCpuTime() { return totalCpuTime; }
    public int getTotalIoTime() { return totalIoTime; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getCurrentBurstIndex() { return currentBurstIndex; }
    public int getRemainingTime() { return remainingTime; }
    public int getIoRemaining() { return ioRemaining; }
    public SchedulingPolicy getCurrentPolicy() { return currentPolicy; }
    public void setCurrentPolicy(SchedulingPolicy currentPolicy) { this.currentPolicy = currentPolicy; }
    public int getAllocatedQuantumTurn() { return allocatedQuantumTurn; }
    public int getExecutedBurst() { return executedBurst; }
    public double getInteractivityScore() { return interactivityScore; }
    public int getSurgeCounter() { return surgeCounter; }
    public long getSafeUntil() { return safeUntil; }
    public long getStartTime() { return startTime; }
    public long getCompletionTime() { return completionTime; }
    public long getWaitingTime() { return waitingTime; }
    public long getResponseTime() { return responseTime; }
    public long getReadyWaitTicks() { return readyWaitTicks; }
    public int getContextSwitches() { return contextSwitches; }

    @Override
    public String toString() {
        return name + "[" + state + ", burst " + currentBurstIndex + ", remaining " + remainingTime + "]";
    }
}
