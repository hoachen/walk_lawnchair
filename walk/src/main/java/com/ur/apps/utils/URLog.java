package com.ur.apps.utils;

import android.util.Log;

import com.ur.apps.walk.BuildConfig;

import java.util.Arrays;

/**
 * URLog - 统一的 Android 日志工具类
 * 修复了格式化字符串问题，提供更稳定的日志输出
 */
public class URLog {

    // MARK: - 常量定义

    private static boolean isLoggingEnabled = BuildConfig.DEBUG;
    private static final String LOG_PREFIX = "[WalkApp]";
    private static final int MAX_LOG_LENGTH = 4000;

    // MARK: - 日志开关控制

    public static void setLoggingEnabled(boolean enabled) {
        isLoggingEnabled = enabled || BuildConfig.DEBUG;
    }

    public static boolean getLoggingEnabled() {
        return isLoggingEnabled;
    }

    // MARK: - Debug 级别日志方法

    public static void d(String message) {
        log(Log.DEBUG, message);
    }

    public static void d(String tag, String message) {
        log(Log.DEBUG, buildMessage(tag, message));
    }

    public static void d(String tag, Object... args) {
        log(Log.DEBUG, buildMessage(tag, args));
    }

    public static void debug(String message) {
        log(Log.DEBUG, message);
    }

    public static void debug(String tag, String message) {
        log(Log.DEBUG, buildMessage(tag, message));
    }

    public static void debug(String tag, Object... args) {
        log(Log.DEBUG, buildMessage(tag, args));
    }

    // MARK: - Info 级别日志方法


    public static void i(String message) {
        log(Log.INFO, message);
    }

    public static void i(String tag, String message) {
        log(Log.INFO, buildMessage(tag, message));
    }

    public static void i(String tag, Object... args) {
        log(Log.INFO, buildMessage(tag, args));
    }

    public static void info(String message) {
        log(Log.INFO, message);
    }

    public static void info(String tag, String message) {
        log(Log.INFO, buildMessage(tag, message));
    }

    public static void info(String tag, Object... args) {
        log(Log.INFO, buildMessage(tag, args));
    }

    // MARK: - Warning 级别日志方法
    public static void w(String message) {
        log(Log.WARN, message);
    }

    public static void w(String tag, String message) {
        log(Log.WARN, buildMessage(tag, message));
    }

    public static void w(String tag, Object... args) {
        log(Log.WARN, buildMessage(tag, args));
    }

    public static void w(String message, Throwable throwable) {
        log(Log.WARN, message);
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.WARN, throwable);
        }
    }

    public static void w(String tag, String message, Throwable throwable) {
        log(Log.WARN, buildMessage(tag, message));
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.WARN, throwable, tag);
        }
    }
    public static void warning(String message) {
        log(Log.WARN, message);
    }

    public static void warning(String tag, String message) {
        log(Log.WARN, buildMessage(tag, message));
    }

    public static void warning(String tag, Object... args) {
        log(Log.WARN, buildMessage(tag, args));
    }

    public static void warning(String message, Throwable throwable) {
        log(Log.WARN, message);
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.WARN, throwable);
        }
    }

    public static void warning(String tag, String message, Throwable throwable) {
        log(Log.WARN, buildMessage(tag, message));
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.WARN, throwable, tag);
        }
    }

    // MARK: - Error 级别日志方法
    public static void e(String message) {
        log(Log.ERROR, message);
    }

    public static void e(String tag, String message) {
        log(Log.ERROR, buildMessage(tag, message));
    }

    public static void e(String tag, Object... args) {
        log(Log.ERROR, buildMessage(tag, args));
    }

    public static void e(String message, Throwable throwable) {
        log(Log.ERROR, message);
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.ERROR, throwable);
        }
    }

    public static void e(String tag, String message, Throwable throwable) {
        log(Log.ERROR, buildMessage(tag, message));
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.ERROR, throwable, tag);
        }
    }
    public static void error(String message) {
        log(Log.ERROR, message);
    }

    public static void error(String tag, String message) {
        log(Log.ERROR, buildMessage(tag, message));
    }

    public static void error(String tag, Object... args) {
        log(Log.ERROR, buildMessage(tag, args));
    }

    public static void error(String message, Throwable throwable) {
        log(Log.ERROR, message);
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.ERROR, throwable);
        }
    }

    public static void error(String tag, String message, Throwable throwable) {
        log(Log.ERROR, buildMessage(tag, message));
        if (isLoggingEnabled && throwable != null) {
            logThrowable(Log.ERROR, throwable, tag);
        }
    }

    // MARK: - Verbose 级别日志方法

    public static void v(String message) {
        if (!isLoggingEnabled) return;
        logLongMessage(Log.VERBOSE, LOG_PREFIX, message);
    }

    public static void v(String tag, String message) {
        if (!isLoggingEnabled) return;
        logLongMessage(Log.VERBOSE, buildTag(tag), message);
    }

    public static void v(String tag, Object... args) {
        if (!isLoggingEnabled) return;
        logLongMessage(Log.VERBOSE, buildTag(tag), buildContent(args));
    }
    public static void verbose(String message) {
        if (!isLoggingEnabled) return;
        logLongMessage(Log.VERBOSE, LOG_PREFIX, message);
    }

    public static void verbose(String tag, String message) {
        if (!isLoggingEnabled) return;
        logLongMessage(Log.VERBOSE, buildTag(tag), message);
    }

    public static void verbose(String tag, Object... args) {
        if (!isLoggingEnabled) return;
        logLongMessage(Log.VERBOSE, buildTag(tag), buildContent(args));
    }

    // MARK: - 高级功能方法

    public static void dump(Object obj) {
        if (!isLoggingEnabled) return;

        if (obj == null) {
            debug("Object dump: null");
            return;
        }

        debug("Object dump: " + safeToString(obj));
        debug("Class: " + obj.getClass().getSimpleName());

        // 数组类型特殊处理
        if (obj.getClass().isArray()) {
            debug("Array content: " + arrayToString(obj));
        }
    }

    public static void trace(String methodName) {
        if (!isLoggingEnabled) return;

        StackTraceElement caller = getCallerStackTrace();
        if (caller != null) {
            String className = getSimpleClassName(caller.getClassName());
            debug("TRACE", "%s.%s() -> %s", className, caller.getMethodName(), methodName);
        } else {
            debug("TRACE", methodName);
        }
    }

    public static void trace() {
        if (!isLoggingEnabled) return;

        StackTraceElement caller = getCallerStackTrace();
        if (caller != null) {
            String className = getSimpleClassName(caller.getClassName());
            debug("TRACE", "%s.%s()", className, caller.getMethodName());
        }
    }

    public static void performance(String tag, long startTime) {
        if (!isLoggingEnabled) return;

        long duration = System.currentTimeMillis() - startTime;
        debug("PERFORMANCE", "%s took %d ms", tag, duration);
    }

    public static Timer startTimer(String tag) {
        return new Timer(tag);
    }

    public static void printStackTrace() {
        if (!isLoggingEnabled) return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        debug("STACK_TRACE", "Current call stack:");
        for (int i = 3; i < Math.min(stackTrace.length, 10); i++) {
            StackTraceElement element = stackTrace[i];
            String className = getSimpleClassName(element.getClassName());
            debug("STACK_TRACE", "  at %s.%s(%s:%d)",
                    className, element.getMethodName(),
                    element.getFileName(), element.getLineNumber());
        }
    }

    // MARK: - 私有辅助方法

    /**
     * 安全的日志记录方法，避免格式化异常
     */
    private static void log(int level, String message) {
        if (!isLoggingEnabled) return;
        logLongMessage(level, LOG_PREFIX, message);
    }

    /**
     * 处理超长日志分割
     */
    private static void logLongMessage(int priority, String tag, String message) {
        if (message == null) {
            Log.println(priority, tag, "null");
            return;
        }

        if (message.length() < MAX_LOG_LENGTH) {
            Log.println(priority, tag, message);
            return;
        }

        // 分割超长日志
        for (int i = 0; i <= message.length() / MAX_LOG_LENGTH; i++) {
            int start = i * MAX_LOG_LENGTH;
            int end = Math.min((i + 1) * MAX_LOG_LENGTH, message.length());
            String part = message.substring(start, end);
            if (i == 0) {
                Log.println(priority, tag, part);
            } else {
                Log.println(priority, tag, "└─ " + part);
            }
        }
    }

    /**
     * 记录异常信息
     */
    private static void logThrowable(int level, Throwable throwable) {
        logThrowable(level, throwable, null);
    }

    private static void logThrowable(int level, Throwable throwable, String tag) {
        if (throwable == null) return;

        String logTag = tag != null ? buildTag(tag) : LOG_PREFIX;
        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.toString();

        logLongMessage(level, logTag, "Exception: " + message);
        logLongMessage(level, logTag, "Stack trace:");

        // 打印堆栈跟踪
        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = getSimpleClassName(element.getClassName());
            logLongMessage(level, logTag,
                    String.format("    at %s.%s(%s:%d)",
                            className, element.getMethodName(),
                            element.getFileName(), element.getLineNumber()));
        }
    }

    /**
     * 构建带标签的消息（安全方式）
     */
    private static String buildMessage(String tag, String message) {
        return safeFormat("%s: %s", tag, message);
    }

    private static String buildMessage(String tag, Object... args) {
        if (args == null || args.length == 0) {
            return tag;
        }

        String content = buildContent(args);
        return safeFormat("%s: %s", tag, content);
    }

    /**
     * 构建内容（安全方式）
     */
    private static String buildContent(Object... args) {
        if (args == null) return "null";
        if (args.length == 0) return "";

        try {
            // 如果只有一个参数，直接返回其字符串表示
            if (args.length == 1) {
                return safeToString(args[0]);
            }

            // 多个参数，使用数组格式
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(safeToString(args[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error building content: " + e.getMessage();
        }
    }

    /**
     * 安全的字符串格式化，避免 Formatter 异常
     */
    private static String safeFormat(String format, Object... args) {
        try {
            // 检查格式字符串中的格式说明符数量
            int formatSpecifierCount = countFormatSpecifiers(format);
            if (formatSpecifierCount != args.length) {
                // 如果不匹配，使用安全的方式拼接
                return format + " " + Arrays.toString(args);
            }
            return String.format(format, args);
        } catch (Exception e) {
            // 如果格式化失败，回退到安全方式
            return format + " " + Arrays.toString(args);
        }
    }

    /**
     * 计算格式字符串中的格式说明符数量
     */
    private static int countFormatSpecifiers(String format) {
        if (format == null) return 0;

        int count = 0;
        boolean inFormat = false;

        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);

            if (c == '%') {
                if (i + 1 < format.length()) {
                    char next = format.charAt(i + 1);
                    if (next == '%') {
                        i++; // 跳过转义的 %
                    } else {
                        count++;
                        inFormat = true;
                    }
                }
            } else if (inFormat) {
                // 检查格式说明符结束
                if (Character.isLetter(c)) {
                    inFormat = false;
                }
            }
        }

        return count;
    }

    /**
     * 安全的对象转字符串
     */
    private static String safeToString(Object obj) {
        if (obj == null) return "null";

        try {
            return String.valueOf(obj);
        } catch (Exception e) {
            return "toString failed: " + e.getMessage();
        }
    }

    /**
     * 数组转字符串
     */
    private static String arrayToString(Object array) {
        if (array == null) return "null";

        try {
            if (array instanceof Object[]) {
                return Arrays.toString((Object[]) array);
            } else if (array instanceof int[]) {
                return Arrays.toString((int[]) array);
            } else if (array instanceof byte[]) {
                return Arrays.toString((byte[]) array);
            } else if (array instanceof char[]) {
                return Arrays.toString((char[]) array);
            } else if (array instanceof boolean[]) {
                return Arrays.toString((boolean[]) array);
            } else if (array instanceof long[]) {
                return Arrays.toString((long[]) array);
            } else if (array instanceof float[]) {
                return Arrays.toString((float[]) array);
            } else if (array instanceof double[]) {
                return Arrays.toString((double[]) array);
            } else {
                return "Unsupported array type";
            }
        } catch (Exception e) {
            return "Array toString failed: " + e.getMessage();
        }
    }

    /**
     * 构建标签
     */
    private static String buildTag(String tag) {
        return LOG_PREFIX + ":" + tag;
    }

    /**
     * 获取简单类名
     */
    private static String getSimpleClassName(String className) {
        if (className == null) return "Unknown";
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * 获取调用者堆栈信息
     */
    private static StackTraceElement getCallerStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 4) {
            return stackTrace[4]; // 0: getStackTrace, 1: getCallerStackTrace, 2: trace方法, 3: 调用trace的方法, 4: 实际调用者
        }
        return null;
    }

    // MARK: - 内部类

    public static class Timer {
        private final String tag;
        private final long startTime;

        private Timer(String tag) {
            this.tag = tag;
            this.startTime = System.currentTimeMillis();
        }

        public void end() {
            long duration = System.currentTimeMillis() - startTime;
            debug("TIMER", "%s completed in %d ms", tag, duration);
        }

        public long endAndGetDuration() {
            long duration = System.currentTimeMillis() - startTime;
            debug("TIMER", "%s completed in %d ms", tag, duration);
            return duration;
        }
    }
}