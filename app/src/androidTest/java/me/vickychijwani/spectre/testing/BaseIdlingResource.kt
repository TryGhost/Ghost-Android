package me.vickychijwani.spectre.testing

import android.os.Handler
import android.support.test.espresso.IdlingResource

interface BaseIdlingResource : IdlingResource {

    companion object {
        private val IDLE_POLL_DELAY_MILLIS = 100L
    }

    fun forceCheckIdleState() {
        /* Force a re-check of the idle state in a little while. If isIdleNow() returns false,
         * Espresso only polls it every few seconds which can slow down our tests. */
        Handler().postDelayed({ isIdleNow }, IDLE_POLL_DELAY_MILLIS)
    }

}
