package me.vickychijwani.spectre.testing

import me.vickychijwani.spectre.event.BusProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class EventBusRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                BusProvider.setupForTesting()
                base.evaluate()
            }
        }
    }

}
