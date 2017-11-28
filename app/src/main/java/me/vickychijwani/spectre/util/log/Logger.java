package me.vickychijwani.spectre.util.log;

abstract class Logger {

    public abstract void log(int priority, String tag, String message, Object... args);

    public abstract void exception(Throwable error);

    public void v(String tag, String message, Object... args) {
        log(android.util.Log.VERBOSE, tag, message, args);
    }

    public void d(String tag, String message, Object... args) {
        log(android.util.Log.DEBUG, tag, message, args);
    }

    public void i(String tag, String message, Object... args) {
        log(android.util.Log.INFO, tag, message, args);
    }

    public void w(String tag, String message, Object... args) {
        log(android.util.Log.WARN, tag, message, args);
    }

    public void e(String tag, String message, Object... args) {
        log(android.util.Log.ERROR, tag, message, args);
    }

    public void wtf(String message, Object... args) {
        throw new AssertionError(String.format(message, args));
    }

}
