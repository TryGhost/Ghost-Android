package me.vickychijwani.spectre.util.log;

abstract class Logger {

    public abstract void log(int priority, String tag, String message);

    public abstract void exception(Throwable error);

    public void v(String tag, String message, Object... args) {
        log(android.util.Log.VERBOSE, tag, format(message, args));
    }

    public void d(String tag, String message, Object... args) {
        log(android.util.Log.DEBUG, tag, format(message, args));
    }

    public void i(String tag, String message, Object... args) {
        log(android.util.Log.INFO, tag, format(message, args));
    }

    public void w(String tag, String message, Object... args) {
        log(android.util.Log.WARN, tag, format(message, args));
    }

    public void e(String tag, String message, Object... args) {
        log(android.util.Log.ERROR, tag, format(message, args));
    }

    public void wtf(String message, Object... args) {
        throw new AssertionError(format(message, args));
    }

    private String format(String message, Object... args) {
        // if no args are passed, don't interpret '%' chars as format specifiers
        // this makes it possible to log strings with the actual '%' char in them
        return (args.length > 0) ? String.format(message, args) : message;
    }

}
