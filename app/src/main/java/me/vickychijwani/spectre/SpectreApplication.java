package me.vickychijwani.spectre;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.otto.DeadEvent;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.tsengvn.typekit.Typekit;

import java.io.File;

import io.fabric.sdk.android.Fabric;
import io.reactivex.plugins.RxJavaPlugins;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import me.vickychijwani.spectre.analytics.AnalyticsService;
import me.vickychijwani.spectre.auth.LoginOrchestrator;
import me.vickychijwani.spectre.error.UncaughtRxException;
import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.model.BlogMetadataDBMigration;
import me.vickychijwani.spectre.model.BlogMetadataModule;
import me.vickychijwani.spectre.network.NetworkService;
import me.vickychijwani.spectre.network.ProductionHttpClientFactory;
import me.vickychijwani.spectre.util.log.Log;
import okhttp3.OkHttpClient;
import retrofit2.Response;

import static me.vickychijwani.spectre.model.DBConfiguration.METADATA_DB_SCHEMA_VERSION;

public class SpectreApplication extends Application {

    private static final String TAG = "SpectreApplication";
    private static SpectreApplication sInstance;

    // this is named "images" but it actually caches all HTTP responses
    private static final String HTTP_CACHE_PATH = "images";

    protected OkHttpClient mOkHttpClient = null;
    protected Picasso mPicasso = null;

    @SuppressWarnings("FieldCanBeLocal")
    private AnalyticsService mAnalyticsService = null;

    // FIXME hacks
    private LoginOrchestrator.HACKListener mHACKListener;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics(), new Answers());
        Log.useEnvironment(BuildConfig.DEBUG ? Log.Environment.DEBUG : Log.Environment.RELEASE);
        Log.i(TAG, "APP LAUNCHED");

        BusProvider.getBus().register(this);
        sInstance = this;

        RxJavaPlugins.setErrorHandler(this::uncaughtRxException);

        setupMetadataRealm();
        setupFonts();
        initOkHttpClient();
        initPicasso();

        NetworkService networkService = new NetworkService();
        mHACKListener = networkService;
        networkService.start(mOkHttpClient);

        mAnalyticsService = new AnalyticsService(BusProvider.getBus());
        mAnalyticsService.start();
    }

    private void setupMetadataRealm() {
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .modules(new BlogMetadataModule())
                .schemaVersion(METADATA_DB_SCHEMA_VERSION)
                .migration(new BlogMetadataDBMigration())
                .build();
        Realm.setDefaultConfiguration(config);
        AnalyticsService.logMetadataDbSchemaVersion(String.valueOf(METADATA_DB_SCHEMA_VERSION));
    }

    private void setupFonts() {
        Typekit.getInstance()
                .addNormal(Typekit.createFromAsset(this, "fonts/OpenSans-Regular.ttf"))
                .addItalic(Typekit.createFromAsset(this, "fonts/OpenSans-Italic.ttf"))
                .addBold(Typekit.createFromAsset(this, "fonts/OpenSans-Bold.ttf"))
                .addBoldItalic(Typekit.createFromAsset(this, "fonts/OpenSans-BoldItalic.ttf"))
                .add("narrow-bold", Typekit.createFromAsset(this, "fonts/OpenSans-CondBold.ttf"));
    }

    public static SpectreApplication getInstance() {
        return sInstance;
    }

    protected void initOkHttpClient() {
        if (mOkHttpClient != null) {
            return;
        }
        File cacheDir = createCacheDir(this);
        mOkHttpClient = new ProductionHttpClientFactory().create(cacheDir);
    }

    @SuppressWarnings("WeakerAccess")
    protected void initPicasso() {
        if (mPicasso != null) {
            return;
        }
        mPicasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(mOkHttpClient))
                .listener((picasso, uri, exception) -> {
                    Log.e("Picasso", "Failed to load image: %s", uri);
                    Log.exception(exception);
                })
                .build();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public Picasso getPicasso() {
        return mPicasso;
    }

    public LoginOrchestrator.HACKListener getHACKListener() {
        return mHACKListener;
    }

    public void addDebugDrawer(@NonNull Activity activity) {
        // no-op, overridden in debug build
    }

    @Nullable protected static File createCacheDir(Context context) {
        File cacheDir = context.getApplicationContext().getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getApplicationContext().getCacheDir();
        }

        File cache = new File(cacheDir, HTTP_CACHE_PATH);
        if (cache.exists() || cache.mkdirs()) {
            return cache;
        } else {
            return null;
        }
    }

    @Subscribe
    public void onApiErrorEvent(ApiErrorEvent event) {
        Response errorResponse = event.apiFailure.response;
        Throwable error = event.apiFailure.error;
        if (errorResponse != null) {
            try {
                String responseString = errorResponse.errorBody().string();
                Log.e(TAG, responseString);
            } catch (Exception e) {
                Log.e(TAG, "[onApiErrorEvent] Error while parsing response error body!");
            }
        }
        if (error != null) {
            Log.exception(error);
        }
    }

    @Subscribe
    public void onDeadEvent(DeadEvent event) {
        Log.w(TAG, "Dead event ignored: %s", event.event.getClass().getName());
    }

    private void uncaughtRxException(Throwable e) {
        Log.exception(new UncaughtRxException(e));
    }

}
