package me.vickychijwani.spectre.util.log;

public abstract class Log {

    // common log tags for easy filtering to colocate events of the same type over long periods of time
    public static final class Tag {
        public static final String LIFECYCLE = "Lifecycle";
    }

    public enum Environment {
        DEBUG,
        RELEASE,
        JVM,
    }

    private static Environment environment = Environment.DEBUG;
    private static Logger logger = new DebugLogger();

    public static void useEnvironment(Environment env) {
        if (environment == env) {
            return;
        }
        environment = env;
        switch (environment) {
            case DEBUG:
                logger = new DebugLogger();
                break;
            case RELEASE:
                logger = new ReleaseLogger();
                break;
            case JVM:
                logger = new JvmLogger();
                break;
            default:
                throw new IllegalArgumentException("Unexpected logging environment passed");
        }
    }

    public static void v(String tag, String message, Object... args) {
        logger.v(tag, message, args);
    }

    public static void d(String tag, String message, Object... args) {
        logger.d(tag, message, args);
    }

    public static void i(String tag, String message, Object... args) {
        logger.i(tag, message, args);
    }

    public static void w(String tag, String message, Object... args) {
        logger.w(tag, message, args);
    }

    public static void e(String tag, String message, Object... args) {
        logger.e(tag, message, args);
    }

    public static void wtf(String message, Object... args) {
        logger.wtf(message, args);
    }

    public static void exception(Throwable error) {
        logger.exception(error);
    }

}
