package me.vickychijwani.spectre.network

import com.github.slugify.Slugify
import io.reactivex.Observable
import io.realm.RealmList
import me.vickychijwani.spectre.model.entity.*
import me.vickychijwani.spectre.network.entity.*
import me.vickychijwani.spectre.util.NetworkUtils
import okhttp3.logging.HttpLoggingInterceptor
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpURLConnection.*
import java.util.*

/**
 * TYPE: integration-style (requires running Ghost server)
 * PURPOSE: contract tests for the latest version of the Ghost API that we support
 *
 * Run these against a live Ghost instance to detect ANY behaviour changes (breaking or
 * non-breaking) in the API when a new Ghost version comes out.
 *
 * What's NOT tested here:
 * - Ghost Auth (needs a UI)
 */

// suppress null dereference warnings because that shouldn't happen in these tests
class GhostApiTest {

    companion object {
        private val BLOG_URL = "http://localhost:2368/"
        internal val TEST_USER = "user@example.com"
        internal val TEST_PWD = "randomtestpwd"

        internal lateinit var API: GhostApiService
        private lateinit var SLUGIFY: Slugify
        private lateinit var RETROFIT: Retrofit

        @BeforeClass @JvmStatic
        fun setupApiService() {
            val httpClient = ProductionHttpClientFactory().create(null)
                    .newBuilder()
                    .addInterceptor(HttpLoggingInterceptor()
                            .setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build()
            RETROFIT = GhostApiUtils.getRetrofit(BLOG_URL, httpClient)
            API = RETROFIT.create(GhostApiService::class.java)

            doWithAuthToken({ token ->
                val posts = execute(API.getPosts(token.authHeader, "", 100)).body()!!
                // A default Ghost install has these many posts initially. If there are more than this,
                // abort. This is to avoid messing up a production blog (like my own) by mistake.
                val DEFAULT_POST_COUNT = 7
                if (posts.posts.isNotEmpty() && posts.posts.size != DEFAULT_POST_COUNT) {
                    throw IllegalStateException("Aborting! Expected $DEFAULT_POST_COUNT posts, " +
                            "found ${posts.posts.size}")
                }
                for (post in posts.posts) {
                    execute(API.deletePost(token.authHeader, post.id))
                }
            })
        }

        @BeforeClass @JvmStatic
        fun setupSlugify() {
            SLUGIFY = Slugify()
        }
    }

    @Test
    fun test_getClientSecret() {
        val clientSecret = clientSecret     // fetch the client secret only once

        // must NOT be null since that's only possible with a very old Ghost version (< 0.7.x)
        assertThat<String>(clientSecret, notNullValue())
        // Ghost uses a 12-character client secret, evident from the Ghost source code (1 byte can hold 2 hex chars):
        // { secret: crypto.randomBytes(6).toString('hex') }
        // file: core/server/data/migration/fixtures/004/04-update-ghost-admin-client.js
        assertThat(clientSecret.length, `is`(12))
    }

    @Test
    fun test_getAuthToken_withPassword() {
        doWithAuthToken({ token ->
            assertThat(token.tokenType, `is`("Bearer"))
            assertThat<String>(token.accessToken, notNullValue())
            assertThat<String>(token.refreshToken, notNullValue())
            assertThat(token.expiresIn, `is`(2628000))
        })
    }

    @Test
    fun test_getAuthToken_wrongEmail() {
        val clientSecret = clientSecret     // fetch the client secret only once
        assertThat<String>(clientSecret, notNullValue())
        val credentials = AuthReqBody.fromPassword(clientSecret, "wrong@email.com", TEST_PWD)
        try {
            execute(API.getAuthToken(credentials))
            fail("Test did not throw exception as expected!")
        } catch (e: HttpException) {
            val apiErrors = GhostApiUtils.parseApiErrors(RETROFIT, e)
            assertThat<ApiErrorList>(apiErrors, notNullValue())
            assertThat(apiErrors!!.errors.size, `is`(1))
            assertThat(apiErrors.errors[0].errorType, `is`("NotFoundError"))
            assertThat(apiErrors.errors[0].message, notNullValue())
            assertThat(apiErrors.errors[0].message, not(""))
        } catch (e: Exception) {
            assertThat("Test threw a different kind of exception than expected!",
                    e, instanceOf(HttpException::class.java))
        }
    }

    @Test
    fun test_getAuthToken_wrongPassword() {
        val clientSecret = clientSecret     // fetch the client secret only once
        assertThat<String>(clientSecret, notNullValue())
        val credentials = AuthReqBody.fromPassword(clientSecret, TEST_USER, "wrongpassword")
        try {
            execute(API.getAuthToken(credentials))
            fail("Test did not throw exception as expected!")
        } catch (e: HttpException) {
            // Ghost returns a 422 Unprocessable Entity for an incorrect password
            assertThat("http code = ${e.code()}", NetworkUtils.isUnprocessableEntity(e), `is`(true))
            val apiErrors = GhostApiUtils.parseApiErrors(RETROFIT, e)
            assertThat<ApiErrorList>(apiErrors, notNullValue())
            assertThat(apiErrors!!.errors.size, `is`(1))
            assertThat(apiErrors.errors[0].errorType, `is`("ValidationError"))
            assertThat(apiErrors.errors[0].message, notNullValue())
            assertThat(apiErrors.errors[0].message, not(""))
        } catch (e: Exception) {
            assertThat("Test threw a different kind of exception than expected!",
                    e, instanceOf(HttpException::class.java))
        }
    }

    @Test
    fun test_getAuthToken_withRefreshToken() {
        doWithAuthToken({ expiredToken ->
            val clientSecret = clientSecret     // fetch the client secret only once
            val credentials = RefreshReqBody(expiredToken.refreshToken, clientSecret)
            val refreshedToken = execute(API.refreshAuthToken(credentials))

            assertThat(refreshedToken.tokenType, `is`("Bearer"))
            assertThat(refreshedToken.accessToken, notNullValue())
            assertThat(refreshedToken.refreshToken, isEmptyOrNullString())
            assertThat(refreshedToken.expiresIn, `is`(2628000))

            // revoke only the access token, the refresh token is null anyway
            val reqBody = RevokeReqBody.fromAccessToken(refreshedToken.accessToken, clientSecret)
            execute(API.revokeAuthToken(refreshedToken.authHeader, reqBody))
        })
    }

    @Test
    fun test_revokeAuthToken() {
        val clientSecret = clientSecret     // fetch the client secret only once
        assertThat<String>(clientSecret, notNullValue())
        val credentials = AuthReqBody.fromPassword(clientSecret, TEST_USER, TEST_PWD)
        val token = execute(API.getAuthToken(credentials))

        // revoke refresh token BEFORE access token, because the access token is needed for revocation!
        val revokeReqs = arrayOf(
                RevokeReqBody.fromRefreshToken(token.refreshToken, clientSecret),
                RevokeReqBody.fromAccessToken(token.accessToken, clientSecret))
        for (reqBody in revokeReqs) {
            val response = execute(API.revokeAuthToken(token.authHeader, reqBody))
            val jsonObj = response.asJsonObject

            assertThat(jsonObj.has("error"), `is`(false))
            assertThat(jsonObj.get("token").asString, `is`(reqBody.token))
        }
    }

    @Test
    fun test_getCurrentUser() {
        doWithAuthToken({ token ->
            val response = execute(API.getCurrentUser(token.authHeader, ""))
            val user = response.body()!!.users[0]

            assertThat(response.code(), `is`(HTTP_OK))
            assertThat<String>(response.headers().get("ETag"), not(isEmptyOrNullString()))
            assertThat(user, notNullValue())
            assertThat(user.id, notNullValue())
            assertThat(user.name, notNullValue())
            assertThat(user.slug, notNullValue())
            assertThat(user.email, `is`(TEST_USER))
            //assertThat(user.getImage(), anyOf(nullValue(), notNullValue())); // no-op
            //assertThat(user.getBio(), anyOf(nullValue(), notNullValue())); // no-op
            assertThat(user.roles, not(empty()))

            val role = user.roles.first()
            //assertThat(role.getId(), instanceOf(Integer.class)); // no-op, int can't be null
            assertThat(role.name, notNullValue())
            assertThat(role.description, notNullValue())
        })
    }

    @Test
    fun test_createPost() {
        doWithAuthToken({ token ->
            createRandomPost(token, { expectedPost, response, createdPost ->
                assertThat(response.code(), `is`(HTTP_CREATED))
                assertThat<String>(createdPost.title, `is`<String>(expectedPost.title))
                assertThat(createdPost.slug, `is`(SLUGIFY.slugify(expectedPost.title)))
                assertThat<String>(createdPost.status, `is`<String>(expectedPost.status))
                assertThat<String>(createdPost.markdown, `is`<String>(expectedPost.markdown))
                assertThat(createdPost.html, `is`("<div class=\"kg-card-markdown\">" +
                        "<p>${expectedPost.markdown}</p>\n</div>"))
                assertThat<RealmList<Tag>>(createdPost.tags, `is`<RealmList<Tag>>(expectedPost.tags))
                assertThat<String>(createdPost.customExcerpt, `is`<String>(expectedPost.customExcerpt))
                assertThat<Boolean>(createdPost.isFeatured, `is`<Boolean>(expectedPost.isFeatured))
                assertThat<Boolean>(createdPost.isPage, `is`<Boolean>(expectedPost.isPage))
            })
        })
    }

    @Test
    fun test_getPosts() {
        doWithAuthToken({ token ->
            createRandomPost(token, { post1, _, _ ->
                createRandomPost(token, { post2, _, _ ->
                    val response = execute(API.getPosts(token.authHeader, "", 100))
                    val posts = response.body()!!.posts
                    assertThat(response.code(), `is`(HTTP_OK))
                    assertThat(posts.size, `is`(2))
                    // posts are returned in reverse-chrono order
                    // check latest post
                    assertThat(posts[0].title, `is`<String>(post2.title))
                    assertThat(posts[0].markdown, `is`<String>(post2.markdown))
                    // check second-last post
                    assertThat(posts[1].title, `is`<String>(post1.title))
                    assertThat(posts[1].markdown, `is`<String>(post1.markdown))
                })
            })
        })
    }

    @Test
    fun test_getPosts_limit() {
        // setting the limit to N should return the *latest* N posts
        doWithAuthToken({ token ->
            createRandomPost(token, { _, _, _ ->
                createRandomPost(token, { post2, _, _ ->
                    val response = execute(API.getPosts(token.authHeader, "", 1))
                    val posts = response.body()!!.posts
                    assertThat(response.code(), `is`(HTTP_OK))
                    assertThat(posts.size, `is`(1))
                    assertThat(posts[0].title, `is`<String>(post2.title))
                    assertThat(posts[0].markdown, `is`<String>(post2.markdown))
                })
            })
        })
    }

    @Test
    fun test_getPost() {
        doWithAuthToken({ token ->
            createRandomPost(token, { expected, _, created ->
                val response = execute(API.getPost(token.authHeader, created.id))
                val post = response.body()!!.posts[0]
                assertThat(response.code(), `is`(HTTP_OK))
                assertThat(post.title, `is`<String>(expected.title))
                assertThat(post.markdown, `is`<String>(expected.markdown))
            })
        })
    }

    @Test
    fun test_updatePost() {
        doWithAuthToken({ token ->
            createRandomPost(token, { newPost, _, created ->
                val updatedTag = Tag("updated-tag")
                val expectedPost = Post(newPost).also {
                    it.markdown = "updated **markdown**"
                    it.mobiledoc = GhostApiUtils.markdownToMobiledoc(it.markdown)
                    it.tags = RealmList(updatedTag)
                    it.customExcerpt = "updated excerpt"
                    it.isFeatured = true
                    it.isPage = true
                }
                val postStubs = PostStubList.from(expectedPost)

                val response = execute(API.updatePost(token.authHeader, created.id, postStubs))
                val actualPostId = response.body()!!.posts[0].id
                val actualPost = execute(API.getPost(token.authHeader, actualPostId))
                        .body()!!.posts[0]

                assertThat(response.code(), `is`(HTTP_OK))
                assertThat(actualPost.title, `is`(expectedPost.title))
                assertThat(actualPost.markdown, `is`(expectedPost.markdown))
                assertThat(actualPost.tags, hasSize(1))
                assertThat(actualPost.tags[0].name, `is`(updatedTag.name))
                assertThat(actualPost.customExcerpt, `is`(expectedPost.customExcerpt))
                assertThat(actualPost.isFeatured, `is`(expectedPost.isFeatured))
                assertThat(actualPost.isPage, `is`(expectedPost.isPage))
            })
        })
    }

    @Test
    fun test_customExcerptLimit() {
        doWithAuthToken({ token ->
            val CUSTOM_EXCERPT_LIMIT = 300

            // excerpt length == allowed limit => should succeed
            createRandomPost(token, { newPost, _, created ->
                val expectedPost = Post(newPost)
                expectedPost.customExcerpt = getRandomString(CUSTOM_EXCERPT_LIMIT)
                val postStubs = PostStubList.from(expectedPost)

                val response = execute(API.updatePost(token.authHeader, created.id, postStubs))
                val actualPostId = response.body()!!.posts[0].id
                val actualPost = execute(API.getPost(token.authHeader, actualPostId))
                        .body()!!.posts[0]

                assertThat(response.code(), `is`(HTTP_OK))
                assertThat(actualPost.customExcerpt.length, `is`(expectedPost.customExcerpt.length))
            })

            // excerpt length == (allowed limit + 1) => should fail
            createRandomPost(token, { newPost, _, created ->
                val expectedPost = Post(newPost)
                expectedPost.customExcerpt = getRandomString(CUSTOM_EXCERPT_LIMIT + 1)
                val postStubs = PostStubList.from(expectedPost)

                val response = execute(API.updatePost(token.authHeader, created.id, postStubs))

                assertThat(response.code(), `is`(422))   // 422 Unprocessable Entity
            })
        })
    }

    @Test
    fun test_deletePost() {
        doWithAuthToken({ token ->
            var deleted: Post? = null
            createRandomPost(token, { _, _, created -> deleted = created })
            // post should be deleted by this point
            val response = execute(API.getPost(token.authHeader, deleted!!.id))
            assertThat(response.code(), `is`(HTTP_NOT_FOUND))
        })
    }

    @Test
    fun test_getSettings() {
        doWithAuthToken({ token ->
            val response = execute(API.getSettings(token.authHeader, ""))
            val settings = response.body()!!.settings

            assertThat(response.code(), `is`(HTTP_OK))
            assertThat<String>(response.headers().get("ETag"), not(isEmptyOrNullString()))
            assertThat(settings, notNullValue())
            // blog title
            assertThat(settings, hasItem<Setting>(allOf<Any>(
                    hasProperty("key", `is`("title")),
                    hasProperty("value", not(isEmptyOrNullString())))))
            // permalink format
            assertThat(settings, hasItem<Setting>(allOf<Any>(
                    hasProperty("key", `is`("permalinks")),
                    hasProperty("value", `is`("/:slug/")))))
        })
    }

    @Test
    fun test_getConfiguration() {
        // NOTE: configuration (except the /configuration/about endpoint) can be queried without auth
        val config = execute<ConfigurationList>(API.configuration).configuration

        assertThat<List<ConfigurationParam>>(config, notNullValue())
    }

    @Test
    fun test_getConfigAbout() {
        doWithAuthToken({ token ->
            val response = execute(API.getVersion(token.authHeader))
            val about = response.body()!!
            val version = about
                    .get("configuration").asJsonArray
                    .get(0).asJsonObject
                    .get("version").asString

            assertThat(response.code(), `is`(HTTP_OK))
            assertThat<String>(response.headers().get("ETag"), not(isEmptyOrNullString()))
            assertThat(about, notNullValue())
            assertThat(version, not(isEmptyOrNullString()))
        })
    }

}



// private helpers

private fun doWithAuthToken(callback: (AuthToken) -> Unit) {
    val clientSecret = clientSecret     // fetch the client secret only once
    assertThat<String>(clientSecret, notNullValue())
    val credentials = AuthReqBody.fromPassword(clientSecret, GhostApiTest.TEST_USER, GhostApiTest.TEST_PWD)
    val token = execute(GhostApiTest.API.getAuthToken(credentials))
    try {
        callback(token)
    } finally {
        // revoke refresh token BEFORE access token, because the access token is needed for revocation!
        val revokeReqs = arrayOf(
                RevokeReqBody.fromRefreshToken(token.refreshToken, clientSecret),
                RevokeReqBody.fromAccessToken(token.accessToken, clientSecret))
        for (reqBody in revokeReqs) {
            execute(GhostApiTest.API.revokeAuthToken(token.authHeader, reqBody))
        }
    }
}

private fun createRandomPost(token: AuthToken, callback: (Post, Response<PostList>, Post) -> Unit) {
    val title = getRandomString(20)
    val markdown = getRandomString(100)
    val newPost = Post().also {
        it.title = title
        it.markdown = markdown
        it.mobiledoc = GhostApiUtils.markdownToMobiledoc(markdown)
        it.tags = RealmList()
        it.customExcerpt = markdown.substring(0, 100)
    }
    val response = execute(GhostApiTest.API.createPost(token.authHeader, PostStubList.from(newPost)))
    val createdId = response.body()!!.posts[0].id
    val created = execute(GhostApiTest.API.getPost(token.authHeader, createdId)).body()!!.posts[0]

    try {
        callback(newPost, response, created)
    } finally {
        execute(GhostApiTest.API.deletePost(token.authHeader, created.id))
    }
}

private val clientSecret: String
    get() = execute<ConfigurationList>(GhostApiTest.API.configuration).clientSecret

private fun <T> execute(call: Call<T>): Response<T> {
    return call.execute()
}

private fun <T> execute(observable: Observable<T>): T {
    return observable.blockingFirst()
}

private fun getRandomString(length: Int): String {
    val random = Random()
    val sb = StringBuilder(length)
    for (i in 0..(length-1)) {
        val c = ('a' + random.nextInt('z' - 'a'))
        sb.append(c)
    }
    return sb.toString()
}
