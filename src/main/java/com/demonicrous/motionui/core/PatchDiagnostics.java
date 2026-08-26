package com.demonicrous.motionui.core;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Collects the state of early bytecode patches for one startup. */
public final class PatchDiagnostics {
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/Diagnostics");
    private static final Map<String, Result> RESULTS = new LinkedHashMap<String, Result>();

    private PatchDiagnostics() {
    }

    public static synchronized void register(String patch) {
        if (!RESULTS.containsKey(patch)) {
            RESULTS.put(patch, new Result("REGISTERED", "waiting for target class"));
        }
    }

    public static synchronized void applied(String patch, String detail) {
        update(patch, "APPLIED", detail);
    }

    public static synchronized void disabled(String patch) {
        update(patch, "DISABLED", "disabled by configuration");
    }

    public static synchronized void safeMode(String patch) {
        update(patch, "SAFE_MODE", "disabled by -Dmotionui.safeMode=true");
    }

    public static synchronized void skipped(String patch, String detail) {
        update(patch, "SKIPPED", detail);
    }

    public static synchronized void failed(String patch, Throwable error) {
        update(patch, "FAILED", error.getClass().getSimpleName()
                + ": " + String.valueOf(error.getMessage()));
    }

    /** Returns an immutable-in-practice copy safe for client GUI inspection. */
    public static synchronized Map<String, Result> snapshot() {
        return new LinkedHashMap<String, Result>(RESULTS);
    }

    public static synchronized boolean hasWarnings() {
        for (Result result : RESULTS.values()) {
            if ("FAILED".equals(result.status) || "SKIPPED".equals(result.status)
                    || "SAFE_MODE".equals(result.status)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean isApplied(String patch) {
        Result result = RESULTS.get(patch);
        return result != null && "APPLIED".equals(result.status);
    }

    private static void update(String patch, String status, String detail) {
        RESULTS.put(patch, new Result(status, detail));
        if (!MotionUIEarlyConfig.isDiagnosticLoggingEnabled()) {
            return;
        }
        LOGGER.info("{}: {} ({})", patch, status, detail);
    }

    public static final class Result {
        private final String status;
        private final String detail;

        private Result(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }

        public String getStatus() {
            return status;
        }

        public String getDetail() {
            return detail;
        }
    }

    static synchronized void resetForTests() {
        RESULTS.clear();
    }
}

