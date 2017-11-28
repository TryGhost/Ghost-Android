package me.vickychijwani.spectre.testing

import me.vickychijwani.spectre.util.log.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// uses normal JVM methods for logging, instead of depending on Android or Crashlytics
class JvmLoggingRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Log.useEnvironment(Log.Environment.JVM)
                base.evaluate()
            }
        }
    }

}
