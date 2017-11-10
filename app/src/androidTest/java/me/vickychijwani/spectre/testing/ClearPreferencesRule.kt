package me.vickychijwani.spectre.testing

import android.content.SharedPreferences
import android.support.annotation.NonNull
import android.support.test.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.*


/**
 * This rule clears all app's SharedPreferences before running each test
 */
class ClearPreferencesRule : TestRule {

    private val allPreferencesFiles: List<SharedPreferences>
        @NonNull
        get() {
            val context = InstrumentationRegistry.getTargetContext().applicationContext
            val prefsFolder = File("${context.applicationInfo.dataDir}/shared_prefs")
            val prefsFiles = prefsFolder.list() ?: return Collections.emptyList()
            return prefsFiles
                    .map {
                        val prefs = when {
                            it.endsWith(".xml") -> it.substring(0, it.indexOf(".xml"))
                            else -> it
                        }
                        context.getSharedPreferences(prefs, 0)
                    }
        }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                // clear data before and after the test(s)
                clearData()
                try {
                    base.evaluate()
                } finally {
                    clearData()
                }
            }
        }
    }

    private fun clearData() {
        allPreferencesFiles.forEach { it.edit().clear().apply() }
    }

}
