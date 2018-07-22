package me.vickychijwani.spectre.util.log;

import com.crashlytics.android.Crashlytics;

final class ReleaseLogger extends Logger {

    @Override
    public void log(int priority, String tag, String message) {
        // log only INFO, WARN, ERROR and ASSERT levels
        if (priority >= android.util.Log.INFO) {
            Crashlytics.log(priority, tag, message);
        }
    }

    @Override
    public void exception(Throwable error) {
        // Crashlytics.logException does not print to Logcat, unlike Crashlytics.log. Not using
        // android.util.Log.e directly here because we would like caught exceptions to also show up
        // in the logs of other exceptions, as they may be correlated.
        e("Exception", android.util.Log.getStackTraceString(error));
        Crashlytics.logException(error);
    }

}
