package me.vickychijwani.spectre.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.util.log.Log;

@SuppressWarnings("WeakerAccess")
public abstract class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";

    private final String mClassName;
    private Unbinder mUnbinder = null;

    protected Bus getBus() {
        return BusProvider.getBus();
    }

    public BaseFragment() {
        super();
        mClassName = this.getClass().getSimpleName();
    }

    @SuppressWarnings("unused")
    protected Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    protected void bindView(@NonNull View view) {
        // Unbinding is needed for derived classes 2 or more levels down the inheritance chain,
        // because each class in the chain may call bindView() independently. Moreover, unbinding
        // and re-binding does not pose a problem because ButterKnife also binds base class fields.
        unbindView();
        mUnbinder = ButterKnife.bind(this, view);
    }

    private void unbindView() {
        if (mUnbinder != null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Log.Tag.LIFECYCLE, "%s#onCreateView()", mClassName);
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(Log.Tag.LIFECYCLE, "%s#onStart()", mClassName);
        getBus().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUnbinder == null) {
            throw new IllegalStateException("You forgot to call bindView() in " + mClassName +
                    "#onCreateView(). This is required in order to unbind Fragment views. See " +
                    "http://jakewharton.github.io/butterknife/#reset");
        }
        Log.i(Log.Tag.LIFECYCLE, "%s#onResume()", mClassName);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(Log.Tag.LIFECYCLE, "%s#onAttach()", mClassName);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(Log.Tag.LIFECYCLE, "%s#onDetach()", mClassName);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(Log.Tag.LIFECYCLE, "%s#onPause()", mClassName);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(Log.Tag.LIFECYCLE, "%s#onStop()", mClassName);
        getBus().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(Log.Tag.LIFECYCLE, "%s#onDestroyView()", mClassName);
        unbindView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(Log.Tag.LIFECYCLE, "%s#onDestroy()", mClassName);
    }

    /**
     * Called by the hosting {@link me.vickychijwani.spectre.view.BaseActivity} to give the Fragment
     * a chance to handle the back press event. The Fragment must return true in order to prevent
     * the default action: {@link android.app.Activity#finish}.
     *
     * @return true if this Fragment has handled the event, false otherwise
     */
    public boolean onBackPressed() {
        return false;
    }

}
