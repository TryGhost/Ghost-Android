package me.vickychijwani.spectre.util

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * TYPE: unit tests (independent of server and android)
 * PURPOSE: testing networking-related utilities
 */

@RunWith(Parameterized::class)
class NetworkUtilsTest(private val baseUrl: String, private val relativePath: String,
                       private val result: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
                arrayOf("https://foo.com", "login", "https://foo.com/login"),
                arrayOf("https://foo.com/", "login", "https://foo.com/login"),
                arrayOf("https://foo.com", "/login", "https://foo.com/login"),
                arrayOf("https://foo.com/", "/login", "https://foo.com/login"),
                // protocol-relative URL
                arrayOf("//foo.com/", "login", "//foo.com/login"),
                // if relative path is already absolute
                arrayOf("https://foo.com/", "https://bar.com/login", "https://bar.com/login"),
                arrayOf("https://foo.com/", "//bar.com/login", "//bar.com/login")
        )
    }

    @Test
    fun makeAbsoluteUrl() {
        assertThat(NetworkUtils.makeAbsoluteUrl(baseUrl, relativePath), `is`(result))
    }

}
