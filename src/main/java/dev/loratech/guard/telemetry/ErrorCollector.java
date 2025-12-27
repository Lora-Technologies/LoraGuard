package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ErrorCollector {

    private final LoraGuard plugin;
    private final Queue<TelemetryEvent> errorQueue;
    private final Map<String, Long> errorDeduplication;
    private static final long DEDUP_WINDOW_MS = 300000;
    private static final int MAX_QUEUE_SIZE = 100;

    public ErrorCollector(LoraGuard plugin) {
        this.plugin = plugin;
        this.errorQueue = new ConcurrentLinkedQueue<>();
        this.errorDeduplication = new ConcurrentHashMap<>();
    }

    public void captureException(Throwable throwable, String context) {
        if (!plugin.getConfigManager().isTelemetryErrorsEnabled()) {
            return;
        }

        String errorKey = generateErrorKey(throwable);
        
        if (isDuplicate(errorKey)) {
            return;
        }

        if (errorQueue.size() >= MAX_QUEUE_SIZE) {
            errorQueue.poll();
        }

        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.ERROR)
            .addData("error_type", throwable.getClass().getSimpleName())
            .addData("message", throwable.getMessage() != null ? throwable.getMessage() : "No message")
            .addData("context", context)
            .addData("stack_trace", getStackTrace(throwable));

        errorQueue.offer(event);
        errorDeduplication.put(errorKey, System.currentTimeMillis());
    }

    public void captureError(String errorType, String message, String context) {
        if (!plugin.getConfigManager().isTelemetryErrorsEnabled()) {
            return;
        }

        String errorKey = errorType + ":" + message;
        
        if (isDuplicate(errorKey)) {
            return;
        }

        if (errorQueue.size() >= MAX_QUEUE_SIZE) {
            errorQueue.poll();
        }

        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.ERROR)
            .addData("error_type", errorType)
            .addData("message", message)
            .addData("context", context);

        errorQueue.offer(event);
        errorDeduplication.put(errorKey, System.currentTimeMillis());
    }

    public Queue<TelemetryEvent> drainErrors() {
        Queue<TelemetryEvent> drained = new ConcurrentLinkedQueue<>();
        TelemetryEvent event;
        while ((event = errorQueue.poll()) != null) {
            drained.offer(event);
        }
        cleanupDeduplication();
        return drained;
    }

    private boolean isDuplicate(String errorKey) {
        Long lastSeen = errorDeduplication.get(errorKey);
        if (lastSeen == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastSeen) < DEDUP_WINDOW_MS;
    }

    private void cleanupDeduplication() {
        long now = System.currentTimeMillis();
        errorDeduplication.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > DEDUP_WINDOW_MS
        );
    }

    private String generateErrorKey(Throwable throwable) {
        StackTraceElement[] stack = throwable.getStackTrace();
        String location = stack.length > 0 ? stack[0].toString() : "unknown";
        return throwable.getClass().getName() + ":" + location;
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String fullTrace = sw.toString();
        if (fullTrace.length() > 2000) {
            return fullTrace.substring(0, 2000) + "...[truncated]";
        }
        return fullTrace;
    }
}
