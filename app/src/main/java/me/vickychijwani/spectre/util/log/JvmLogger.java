package me.vickychijwani.spectre.util.log;

/**
 * Use this for code running on a JVM instead of on Android (e.g., unit tests).
 */
final class JvmLogger extends Logger {

    @Override
    public void log(int priority, String tag, String message) {
        System.out.println(message);
    }

    @Override
    public void exception(Throwable error) {
        error.printStackTrace();
    }

}
