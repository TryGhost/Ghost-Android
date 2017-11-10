package me.vickychijwani.spectre.testing

import me.vickychijwani.spectre.auth.ProductionApiProvider
import me.vickychijwani.spectre.network.ProductionHttpClientFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DeleteDefaultPostsRule(private val blogUrl: String) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val httpClient = ProductionHttpClientFactory().create(null)
                val apiProvider = ProductionApiProvider(httpClient, blogUrl)
                apiProvider.ghostApi.deleteDefaultPosts()
                base.evaluate()
            }
        }
    }

}
