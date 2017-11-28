package me.vickychijwani.spectre.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.tsengvn.typekit.TypekitContextWrapper;

import java.util.List;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.event.CredentialsExpiredEvent;
import me.vickychijwani.spectre.util.log.Log;
import me.vickychijwani.spectre.view.fragments.BaseFragment;

@SuppressWarnings("WeakerAccess")
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    private CredentialsExpiredEventHandler mCredentialsExpiredEventHandler = null;

    private final CompositeDisposable mOnPauseDisposables = new CompositeDisposable();

    protected Bus getBus() {
        return BusProvider.getBus();
    }

    protected Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    protected void disposeOnPause(Disposable d) {
        mOnPauseDisposables.add(d);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Log.Tag.LIFECYCLE, "%s#onCreate()", this.getClass().getSimpleName());
    }

    protected void setLayout(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(Log.Tag.LIFECYCLE, "%s#onStart()", this.getClass().getSimpleName());
        getBus().register(this);
        if (! (this instanceof LoginActivity)) {
            mCredentialsExpiredEventHandler = new CredentialsExpiredEventHandler(this);
            getBus().register(mCredentialsExpiredEventHandler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(Log.Tag.LIFECYCLE, "%s#onResume()", this.getClass().getSimpleName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(Log.Tag.LIFECYCLE, "%s#onPause()", this.getClass().getSimpleName());
        // clear() instead of dispose() because dispose makes it non-reusable after it is called
        mOnPauseDisposables.clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(Log.Tag.LIFECYCLE, "%s#onStop()", this.getClass().getSimpleName());
        if (mCredentialsExpiredEventHandler != null) {
            getBus().unregister(mCredentialsExpiredEventHandler);
            mCredentialsExpiredEventHandler = null;
        }
        getBus().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(Log.Tag.LIFECYCLE, "%s#onDestroy()", this.getClass().getSimpleName());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(Log.Tag.LIFECYCLE, "%s#onLowMemory()", this.getClass().getSimpleName());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.i(Log.Tag.LIFECYCLE, "%s#onTrimMemory()", this.getClass().getSimpleName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(Log.Tag.LIFECYCLE, "%s#onBackPressed()", this.getClass().getSimpleName());
        // give fragments a chance to handle back press
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (!(f instanceof BaseFragment)) {
                    continue;  // vanilla fragments don't have onBackPressed
                }

                BaseFragment bf = (BaseFragment) f;
                if (bf.onBackPressed()) {
                    return;
                }
            }
        }
        // pop back stack if any
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return;
        }
        // finally, delegate to superclass
        super.onBackPressed();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }

    protected void startBrowserActivity(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.primary));
        builder.addDefaultShareMenuItem();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    protected void credentialsExpired() {
        new CredentialsExpiredEventHandler(this)
                .onCredentialsExpiredEvent(new CredentialsExpiredEvent());
    }


    // the event handler cannot be added to BaseActivity directly because Otto doesn't look at base
    // classes when looking for subscribers, hence this little helper class
    private static class CredentialsExpiredEventHandler {
        private final Activity mActivity;

        public CredentialsExpiredEventHandler(Activity activity) {
            mActivity = activity;
        }

        @Subscribe
        public void onCredentialsExpiredEvent(CredentialsExpiredEvent event) {
            Intent intent = new Intent(mActivity, LoginActivity.class);
            // destroy all activities in this task stack
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(intent);
            Toast.makeText(mActivity, mActivity.getString(R.string.credentials_expired),
                    Toast.LENGTH_LONG).show();
            mActivity.finish();
        }
    }

}
