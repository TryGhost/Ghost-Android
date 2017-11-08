package me.vickychijwani.spectre.testing

/*
 * Copyright (C) 2016 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.support.annotation.CheckResult
import android.support.test.espresso.IdlingResource
import okhttp3.*

/**
 * An [IdlingResource] for [OkHttpClient].
 *
 * This was taken from https://github.com/JakeWharton/okhttp-idling-resource
 * with this patch applied: https://github.com/JakeWharton/okhttp-idling-resource/issues/10#issue-223458172
 */
class OkHttpIdlingResource private constructor(private val name: String,
                                               private val dispatcher: Dispatcher)
    : IdlingResource {
    @Volatile private var callback: IdlingResource.ResourceCallback? = null

    init {
        dispatcher.setIdleCallback {
            this.callback?.onTransitionToIdle()
        }
    }

    override fun getName(): String {
        return name
    }

    override fun isIdleNow(): Boolean {
        val idle = dispatcher.runningCallsCount() == 0
        if (idle && callback != null) {
            callback!!.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    companion object {
        /**
         * Create a new [IdlingResource] from `client` as `name`. You must register
         * this instance using `Espresso.registerIdlingResources`.
         */
        @CheckResult
        fun create(name: String, client: OkHttpClient): OkHttpIdlingResource {
            return OkHttpIdlingResource(name, client.dispatcher())
        }
    }
}
