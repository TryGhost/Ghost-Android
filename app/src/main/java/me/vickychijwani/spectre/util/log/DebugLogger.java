package me.vickychijwani.spectre.util.log;

final class DebugLogger extends Logger {

    @Override
    public void log(int priority, String tag, String message) {
        android.util.Log.println(priority, tag, message);
    }

    @Override
    public void exception(Throwable error) {
        android.util.Log.e("Exception", android.util.Log.getStackTraceString(error));
    }

}
