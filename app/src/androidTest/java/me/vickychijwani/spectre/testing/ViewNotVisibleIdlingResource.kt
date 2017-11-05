package me.vickychijwani.spectre.testing

import android.app.Activity
import android.support.annotation.IdRes
import android.support.annotation.NonNull
import android.support.test.espresso.IdlingResource
import android.view.View

/**
 * [IdlingResource] which is idle when a [View] with the given ID is NOT [View.VISIBLE]
 * in the given activity.
 */
class ViewNotVisibleIdlingResource(@NonNull activity: Activity,
                                   @IdRes private val mViewId: Int)
    : ActivityStateIdlingResource(activity,
        { act ->
            val view: View? = act.findViewById(mViewId)
            view == null || view.visibility != View.VISIBLE
        }
)
