package me.vickychijwani.spectre.testing

import android.support.test.espresso.*
import me.vickychijwani.spectre.SpectreApplication
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule to make UI tests automatically wait for network calls using an Espresso [IdlingResource]
 */
class OkHttpIdlingResourceRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // register the IdlingResource
                val client = SpectreApplication.getInstance().okHttpClient
                val r = OkHttpIdlingResource.create("OkHttp", client)
                IdlingRegistry.getInstance().register(r)

                try {
                    // run test
                    base.evaluate()
                } finally {
                    // clean up
                    IdlingRegistry.getInstance().unregister(r)
                }
            }
        }
    }

}
