package me.vickychijwani.spectre.testing

import android.app.Activity
import android.support.annotation.NonNull
import android.support.test.espresso.IdlingResource
import java.lang.ref.WeakReference

/**
 * [IdlingResource] that is idle when the given [Activity] passes the given predicate.
 *
 * Adapted from https://gist.github.com/vaughandroid/e2fda716c7cf6853fa79
 */
open class ActivityStateIdlingResource(@NonNull activity: Activity,
                                       private val mIdlePredicate: (Activity) -> Boolean)
    : BaseIdlingResource {

    /* Hold a weak reference, so we don't leak memory even if the resource isn't unregistered. */
    private val mActivity: WeakReference<Activity> = WeakReference(activity)
    private val mName: String = "IdlingResource for state of ${activity::class.java.simpleName} " +
            "(@${System.identityHashCode(activity)})"

    private var mResourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return mName
    }

    override fun isIdleNow(): Boolean {
        val activity = mActivity.get()
        val isIdle = if (activity != null) mIdlePredicate(activity) else true
        if (isIdle) {
            if (mResourceCallback != null) {
                mResourceCallback!!.onTransitionToIdle()
            }
        } else {
            forceCheckIdleState()
        }

        return isIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        mResourceCallback = resourceCallback
    }

}
