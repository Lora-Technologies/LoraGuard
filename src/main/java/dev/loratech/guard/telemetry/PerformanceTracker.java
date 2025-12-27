package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class PerformanceTracker {

    private final LoraGuard plugin;
    private final Queue<TelemetryEvent> performanceQueue;
    private final Map<String, OperationStats> operationStats;
    private static final int MAX_QUEUE_SIZE = 50;

    public PerformanceTracker(LoraGuard plugin) {
        this.plugin = plugin;
        this.performanceQueue = new ConcurrentLinkedQueue<>();
        this.operationStats = new ConcurrentHashMap<>();
    }

    public TimingContext startTiming(String operationName) {
        if (!plugin.getConfigManager().isTelemetryPerformanceEnabled()) {
            return new TimingContext(operationName, 0, false);
        }
        return new TimingContext(operationName, System.nanoTime(), true);
    }

    public void recordTiming(TimingContext context) {
        if (!context.isEnabled()) {
            return;
        }

        long durationNanos = System.nanoTime() - context.getStartTime();
        long durationMs = durationNanos / 1_000_000;

        operationStats.computeIfAbsent(context.getOperationName(), k -> new OperationStats())
            .record(durationMs);
    }

    public void recordSlowOperation(String operationName, long durationMs, String details) {
        if (!plugin.getConfigManager().isTelemetryPerformanceEnabled()) {
            return;
        }

        if (performanceQueue.size() >= MAX_QUEUE_SIZE) {
            performanceQueue.poll();
        }

        long threshold = plugin.getConfigManager().getTelemetrySlowThresholdMs();
        if (durationMs >= threshold) {
            TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.PERFORMANCE)
                .addData("operation", operationName)
                .addData("duration_ms", durationMs)
                .addData("details", details)
                .addData("threshold_ms", threshold);

            performanceQueue.offer(event);
        }
    }

    public Queue<TelemetryEvent> drainPerformanceEvents() {
        Queue<TelemetryEvent> drained = new ConcurrentLinkedQueue<>();
        
        TelemetryEvent event;
        while ((event = performanceQueue.poll()) != null) {
            drained.offer(event);
        }

        for (Map.Entry<String, OperationStats> entry : operationStats.entrySet()) {
            OperationStats stats = entry.getValue();
            if (stats.getCount() > 0) {
                TelemetryEvent statsEvent = new TelemetryEvent(TelemetryEvent.EventType.PERFORMANCE)
                    .addData("operation", entry.getKey())
                    .addData("count", stats.getCount())
                    .addData("avg_ms", stats.getAverage())
                    .addData("max_ms", stats.getMax())
                    .addData("min_ms", stats.getMin());
                drained.offer(statsEvent);
            }
        }

        operationStats.clear();
        return drained;
    }

    public static class TimingContext {
        private final String operationName;
        private final long startTime;
        private final boolean enabled;

        public TimingContext(String operationName, long startTime, boolean enabled) {
            this.operationName = operationName;
            this.startTime = startTime;
            this.enabled = enabled;
        }

        public String getOperationName() {
            return operationName;
        }

        public long getStartTime() {
            return startTime;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    private static class OperationStats {
        private final LongAdder totalTime = new LongAdder();
        private final LongAdder count = new LongAdder();
        private final AtomicLong max = new AtomicLong(0);
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

        public void record(long durationMs) {
            totalTime.add(durationMs);
            count.increment();
            max.updateAndGet(current -> Math.max(current, durationMs));
            min.updateAndGet(current -> Math.min(current, durationMs));
        }

        public long getCount() {
            return count.sum();
        }

        public double getAverage() {
            long c = count.sum();
            return c > 0 ? (double) totalTime.sum() / c : 0;
        }

        public long getMax() {
            return max.get();
        }

        public long getMin() {
            long m = min.get();
            return m == Long.MAX_VALUE ? 0 : m;
        }
    }
}
