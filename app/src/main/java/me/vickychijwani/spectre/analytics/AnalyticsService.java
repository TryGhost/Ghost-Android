package me.vickychijwani.spectre.analytics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import me.vickychijwani.spectre.event.FileUploadedEvent;
import me.vickychijwani.spectre.event.GhostVersionLoadedEvent;
import me.vickychijwani.spectre.event.LoadGhostVersionEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LogoutStatusEvent;
import me.vickychijwani.spectre.util.BundleBuilder;
import me.vickychijwani.spectre.util.log.Log;

public class AnalyticsService {

    private static final String TAG = AnalyticsService.class.getSimpleName();

    private static FirebaseAnalytics sFirebaseAnalytics;

    private static Bus sEventBus;

    private static Listener sEventBusListener;

    private AnalyticsService() {}

    /**
     * Ideally the `context` parameter should be an Activity context in order for page views to be
     * recorded automatically, but we don't care about that particular feature, and initializing
     * with the app context is certainly more convenient.
     * @param context - to initialize Firebase Analytics. NOTE: NO REFERENCE to the object is stored
     * @param eventBus - to listen for analytics triggers and respond
     */
    public static void start(Context context, Bus eventBus) {
        sFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        sEventBus = eventBus;
        sEventBusListener = new Listener(sEventBus);

        sEventBus.register(sEventBusListener);
        sEventBus.post(new LoadGhostVersionEvent(true));
    }

    public static void stop() {
        sEventBus.unregister(sEventBusListener);
    }

    private static void logGhostVersion(@Nullable String ghostVersion) {
        if (ghostVersion == null) {
            ghostVersion = "Unknown";
        }
        Log.i(TAG, "GHOST VERSION = %s", ghostVersion);
        Answers.getInstance().logCustom(new CustomEvent("Ghost Version")
                .putCustomAttribute("version", ghostVersion));
        sFirebaseAnalytics.logEvent("ghost_version", new BundleBuilder()
                .put("version", ghostVersion)
                .build());
        sFirebaseAnalytics.setUserProperty("ghost_version", ghostVersion);
    }

    private static void logLogin(@Nullable String blogUrl, boolean success) {
        if (blogUrl == null) {
            blogUrl = "Unknown";
        }
        String successStr = success ? "SUCCEEDED" : "FAILED";
        Log.i(TAG, "LOGIN %s, BLOG URL = %s", successStr, blogUrl);
        Answers.getInstance().logLogin(new LoginEvent()
                .putCustomAttribute("URL", blogUrl)
                .putSuccess(success));
        sFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, new BundleBuilder()
                .put("url", blogUrl)
                // Param.SUCCESS expects a long value: https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Param#SUCCESS
                .put(FirebaseAnalytics.Param.SUCCESS, success ? 1L : 0L)
                .build());
    }

    private static void logLogout(boolean logoutSucceeded) {
        if (logoutSucceeded) {
            Log.i(TAG, "LOGOUT SUCCEEDED");
            Answers.getInstance().logCustom(new CustomEvent("Logout"));
            sFirebaseAnalytics.logEvent("logout", new BundleBuilder().build());
        }
    }

    public static void logGhostV0Error() {
        Log.i(TAG, "GHOST VERSION 0.x ERROR - UPGRADE REQUIRED");
        Answers.getInstance().logCustom(new CustomEvent("Ghost v0.x error"));
        sFirebaseAnalytics.logEvent("ghost_v0_error", new BundleBuilder().build());
    }

    public static void logMetadataDbSchemaVersion(@NonNull String metadataDbSchemaVersion) {
        Log.i(TAG, "METADATA DB SCHEMA VERSION = %s", metadataDbSchemaVersion);
        Answers.getInstance().logCustom(new CustomEvent("Metadata DB Schema Version")
                .putCustomAttribute("version", metadataDbSchemaVersion));
        sFirebaseAnalytics.logEvent("metadata_db", new BundleBuilder()
                .put("schema_version", metadataDbSchemaVersion)
                .build());
        sFirebaseAnalytics.setUserProperty("metadata_db_version", metadataDbSchemaVersion);
    }

    public static void logDbSchemaVersion(@NonNull String dbSchemaVersion) {
        Log.i(TAG, "DB SCHEMA VERSION = %s", dbSchemaVersion);
        Answers.getInstance().logCustom(new CustomEvent("DB Schema Version")
                .putCustomAttribute("version", dbSchemaVersion));
        sFirebaseAnalytics.logEvent("data_db", new BundleBuilder()
                .put("schema_version", dbSchemaVersion)
                .build());
        sFirebaseAnalytics.setUserProperty("blog_db_schema_version", dbSchemaVersion);
    }


    // post actions
    public static void logNewDraftUploaded() {
        logPostAction("New draft uploaded", null);
    }

    public static void logDraftPublished(String postUrl) {
        logPostAction("Published draft", postUrl);
    }

    public static void logScheduledPostUpdated(String postUrl) {
        logPostAction("Scheduled post updated", postUrl);
    }

    public static void logPublishedPostUpdated(String postUrl) {
        logPostAction("Published post updated", postUrl);
    }

    public static void logPostUnpublished() {
        logPostAction("Unpublished post", null);
    }

    public static void logDraftAutoSaved() {
        logPostAction("Auto-saved draft", null);
    }

    public static void logDraftSavedExplicitly() {
        logPostAction("Explicitly saved draft", null);
    }

    public static void logPostSavedInUnknownScenario() {
        logPostAction("Unknown scenario", null);
    }

    public static void logPublishedPostAutoSavedLocally() {
        logPostAction("Auto-saved edits to published post", null);
    }

    public static void logScheduledPostAutoSavedLocally() {
        logPostAction("Auto-saved edits to scheduled post", null);
    }

    public static void logDraftDeleted() {
        logPostAction("Deleted draft", null);
    }

    public static void logConflictFound() {
        logPostAction("Conflict found", null);
    }

    public static void logConflictResolved() {
        logPostAction("Conflict resolved", null);
    }

    private static void logPostAction(@NonNull String postAction, @Nullable String postUrl) {
        CustomEvent postActionEvent = new CustomEvent("Post Actions")
                .putCustomAttribute("Scenario", postAction);
        BundleBuilder postActionBundle = new BundleBuilder()
                .put("scenario", postAction);
        if (postUrl != null) {
            // FIXME this is a huge hack, also Fabric only shows 10 of these per day
            postActionEvent.putCustomAttribute("URL", postUrl);
            postActionBundle.put("url", postUrl);
        }
        Log.i(TAG, "POST ACTION: %s", postAction);
        Answers.getInstance().logCustom(postActionEvent);

        sFirebaseAnalytics.logEvent("post_action", postActionBundle.build());
    }


    private static class Listener {
        private final Bus mEventBus;

        Listener(Bus eventBus) {
            mEventBus = eventBus;
        }

        @Subscribe
        public void onLoginDoneEvent(LoginDoneEvent event) {
            logLogin(event.blogUrl, true);

            // user just logged in, now's a good time to check this
            mEventBus.post(new LoadGhostVersionEvent(true));
        }

        @Subscribe
        public void onLoginErrorEvent(LoginErrorEvent event) {
            logLogin(event.blogUrl, false);
        }

        @Subscribe
        public void onGhostVersionLoadedEvent(GhostVersionLoadedEvent event) {
            logGhostVersion(event.version);
        }

        @Subscribe
        public void onLogoutStatusEvent(LogoutStatusEvent logoutEvent) {
            logLogout(logoutEvent.succeeded);
        }

        @Subscribe
        public void onFileUploadedEvent(FileUploadedEvent event) {
            logPostAction("Image uploaded", null);
        }
    }

}
