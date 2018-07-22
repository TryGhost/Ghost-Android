package me.vickychijwani.spectre.testing

import android.app.Activity
import android.support.annotation.*
import android.support.test.espresso.IdlingResource
import android.widget.TextView

/**
 * [IdlingResource] which is idle when a [TextView] with the given ID is present
 * AND does NOT match the given text in the given activity.
 */
class ViewDoesntMatchTextIdlingResource(@NonNull activity: Activity,
                                        @IdRes private val mViewId: Int,
                                        private val textToMatch: String)
    : ActivityStateIdlingResource(activity,
        { act ->
            val view = act.findViewById(mViewId) as TextView?
            view != null && view.text != textToMatch
        }
)
