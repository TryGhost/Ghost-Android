package me.vickychijwani.spectre.auth

import me.vickychijwani.spectre.error.UrlNotFoundException
import me.vickychijwani.spectre.testing.Helpers
import me.vickychijwani.spectre.testing.Helpers.execute
import me.vickychijwani.spectre.testing.urlMatches
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * TYPE: unit tests (independent of server and android)
 * PURPOSE: testing blog URL validation
 */

class NetworkBlogUrlValidatorTest {

    private val HTTP = "http://"
    private val HTTPS = "https://"

    private lateinit var server: MockWebServer

    // setup / teardown
    @Before
    fun setupMockServer() {
        server = MockWebServer().also {
            it.start()
        }
    }

    @After
    fun shutdownMockServer() {
        server.shutdown()
    }


    // actual tests
    @Test
    fun checkGhostBlog_simpleHttps() {
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false)
        server.enqueue(MockResponse())
        val blogUrl = "$HTTPS${server.hostName}:${server.port}"
        val httpClient = Helpers.prodHttpClient

        assertThat(checkGhostBlog(blogUrl, httpClient), `is`(blogUrl))
    }

    @Test
    fun checkGhostBlog_simpleHttp() {
        server.enqueue(MockResponse())
        val blogUrl = "$HTTP${server.hostName}:${server.port}"
        val httpClient = Helpers.prodHttpClient

        assertThat(checkGhostBlog(blogUrl, httpClient), `is`(blogUrl))
    }

    @Test
    fun checkGhostBlog_404() {
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false)
        server.enqueue(MockResponse().setResponseCode(404))
        val blogUrl = "$HTTPS${server.hostName}:${server.port}/THIS_DOESNT_EXIST"
        val httpClient = Helpers.prodHttpClient

        try {
            checkGhostBlog(blogUrl, httpClient)
            fail("Test did not throw exception as expected!")
        } catch (e: Exception) {
            assertThat(e, instanceOf(UrlNotFoundException::class.java))
        }
    }

    @Test
    fun checkGhostBlog_trailingSlash() {
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false)
        server.enqueue(MockResponse())
        val blogUrl = "$HTTPS${server.hostName}:${server.port}/"
        val httpClient = Helpers.prodHttpClient

        assertThat(checkGhostBlog(blogUrl, httpClient),
                isOneOf(blogUrl, blogUrl.replaceFirst("/$".toRegex(), "")))
    }

    @Test
    fun checkGhostBlog_httpToHttpsRedirect() {
        val toHttps: (HttpUrl) -> HttpUrl = { it.newBuilder().scheme("https").build() }

        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false)
        server.enqueue(MockResponse())
        val httpUrl = "$HTTP${server.hostName}:${server.port}"
        val httpsUrl = toHttps(HttpUrl.parse(httpUrl)!!).toString()
        val httpClient = Helpers.prodHttpClient.newBuilder().addInterceptor { chain ->
            if (chain.request().isHttps) {
                throw IllegalStateException("This test is supposed to make a vanilla HTTP request!")
            }
            // pretend as if the request was redirected and this response is from the redirected URL
            val httpsRequestUrl = toHttps(chain.request().url()).toString()
            Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request().newBuilder().url(httpsRequestUrl).build())
                    .code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                    .build()
        }.build()

        assertThat(checkGhostBlog(httpUrl, httpClient), urlMatches(httpsUrl))
    }

    @Test
    fun checkGhostBlog_underSubFolder() {
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false)
        server.enqueue(MockResponse())
        val blogUrl = "$HTTPS${server.hostName}:${server.port}/blog"
        val httpClient = Helpers.prodHttpClient

        assertThat(checkGhostBlog(blogUrl, httpClient), `is`(blogUrl))
    }

    @Test
    fun checkGhostBlog_underSubDomain() {
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false)
        server.enqueue(MockResponse())
        val blogUrl = "${HTTPS}blog.${server.hostName}:${server.port}"
        val httpClient = Helpers.prodHttpClient.newBuilder().addInterceptor { chain ->
            Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                    .build()
        }.build()

        assertThat(checkGhostBlog(blogUrl, httpClient), `is`(blogUrl))
    }

}

// helper methods
private fun checkGhostBlog(blogUrl: String, httpClient: OkHttpClient): String {
    return execute(NetworkBlogUrlValidator.checkGhostBlog(blogUrl, httpClient))
}
