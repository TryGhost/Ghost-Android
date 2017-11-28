package me.vickychijwani.spectre.util.log;

/**
 * Use this for code running on a JVM instead of on Android (e.g., unit tests).
 */
final class JvmLogger extends Logger {

    @Override
    public void log(int priority, String tag, String message, Object... args) {
        System.out.println(String.format(message, args));
    }

    @Override
    public void exception(Throwable error) {
        error.printStackTrace();
    }

}
