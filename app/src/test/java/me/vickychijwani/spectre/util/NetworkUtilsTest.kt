package me.vickychijwani.spectre.util

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * TYPE: unit tests (independent of server and android)
 * PURPOSE: testing networking-related utilities
 */

@RunWith(Enclosed::class)
class NetworkUtilsTest {

    @RunWith(Parameterized::class)
    class AbsoluteUrlTest(private val baseUrl: String, private val relativePath: String,
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


    @RunWith(Parameterized::class)
    class PicassoUrlTest(private val baseUrl: String, private val relativePath: String,
                          private val result: String) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun data() = listOf(
                    arrayOf("https://foo.com", "/image.jpg", "https://foo.com/image.jpg"),
                    arrayOf("https://foo.com/", "image.jpg", "https://foo.com/image.jpg"),
                    arrayOf("https://foo.com", "/image.jpg", "https://foo.com/image.jpg"),
                    arrayOf("https://foo.com/", "/image.jpg", "https://foo.com/image.jpg"),
                    // if relative path is already absolute
                    arrayOf("https://foo.com/", "https://bar.com/image.jpg", "https://bar.com/image.jpg"),
                    // protocol-relative URL
                    arrayOf("//foo.com", "/image.jpg", "http://foo.com/image.jpg"),
                    arrayOf("http://foo.com/", "//bar.com/image.jpg", "http://bar.com/image.jpg"),
                    arrayOf("https://foo.com/", "//bar.com/image.jpg", "https://bar.com/image.jpg")
            )
        }

        @Test
        fun makeAbsoluteUrl() {
            assertThat(NetworkUtils.makePicassoUrl(baseUrl, relativePath), `is`(result))
        }
    }

}
