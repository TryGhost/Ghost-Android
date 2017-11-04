package me.vickychijwani.spectre.testing

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import timber.log.Timber

class LoggingRule : TestRule {

    private val tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
            message?.let {
                println(message)
            }
            t?.printStackTrace()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Timber.plant(tree)
                base.evaluate()
            }
        }
    }

}
