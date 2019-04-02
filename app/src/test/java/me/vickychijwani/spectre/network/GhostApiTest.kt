package me.vickychijwani.spectre.network

import com.github.slugify.Slugify
import io.realm.RealmList
import me.vickychijwani.spectre.auth.ProductionApiProvider
import me.vickychijwani.spectre.model.entity.*
import me.vickychijwani.spectre.network.entity.*
import me.vickychijwani.spectre.testing.*
import me.vickychijwani.spectre.util.NetworkUtils
import okhttp3.logging.HttpLoggingInterceptor
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import retrofit2.*
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK
import java.util.*
import org.hamcrest.Matchers.`is` as Is

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
        @ClassRule @JvmField val deleteDefaultPostsRule = DeleteDefaultPostsRule(BLOG_URL)

        private val httpClient = ProductionHttpClientFactory().create(null)
                .newBuilder()
                .addInterceptor(HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
        private val apiProvider = ProductionApiProvider(httpClient, BLOG_URL)
        private val RETROFIT = apiProvider.retrofit
        internal val API = apiProvider.ghostApi
    }

    @Test
    fun test_getClientSecret() {
        val clientSecret = API.clientSecret     // fetch the client secret only once

        // must NOT be null since that's only possible with a very old Ghost version (< 0.7.x)
        assertThat(clientSecret, notNullValue())
        // Ghost uses a 12-character client secret, evident from the Ghost source code (1 byte can hold 2 hex chars):
        // { secret: crypto.randomBytes(6).toString('hex') }
        // file: core/server/data/migration/fixtures/004/04-update-ghost-admin-client.js
        assertThat(clientSecret.length, Is(12))
    }

    @Test
    fun test_getAuthToken_withPassword() {
        API.doWithAuthToken { token ->
            assertThat(token.tokenType, Is("Bearer"))
            assertThat(token.accessToken, notNullValue())
            assertThat(token.refreshToken, notNullValue())
            assertThat(token.expiresIn, Is(2628000))
        }
    }

    @Test
    fun test_getAuthToken_wrongEmail() {
        val clientSecret = API.clientSecret     // fetch the client secret only once
        assertThat(clientSecret, notNullValue())
        val credentials = AuthReqBody.fromPassword(clientSecret, "wrong@email.com", TEST_PWD)
        try {
            execute(API.getAuthToken(credentials))
            fail("Test did not throw exception as expected!")
        } catch (e: HttpException) {
            val apiErrors = GhostApiUtils.parseApiErrors(RETROFIT, e)
            assertThat(apiErrors, notNullValue())
            assertThat(apiErrors!!.errors.size, Is(1))
            assertThat(apiErrors.errors[0].errorType, Is("NotFoundError"))
            assertThat(apiErrors.errors[0].message, notNullValue())
            assertThat(apiErrors.errors[0].message, not(""))
        } catch (e: Exception) {
            assertThat("Test threw a different kind of exception than expected!",
                    e, instanceOf(HttpException::class.java))
        }
    }

    @Test
    fun test_getAuthToken_wrongPassword() {
        val clientSecret = API.clientSecret     // fetch the client secret only once
        assertThat(clientSecret, notNullValue())
        val credentials = AuthReqBody.fromPassword(clientSecret, TEST_USER, "wrongpassword")
        try {
            execute(API.getAuthToken(credentials))
            fail("Test did not throw exception as expected!")
        } catch (e: HttpException) {
            // Ghost returns a 422 Unprocessable Entity for an incorrect password
            assertThat("http code = ${e.code()}", NetworkUtils.isUnprocessableEntity(e), Is(true))
            val apiErrors = GhostApiUtils.parseApiErrors(RETROFIT, e)
            assertThat(apiErrors, notNullValue())
            assertThat(apiErrors!!.errors.size, Is(1))
            assertThat(apiErrors.errors[0].errorType, Is("ValidationError"))
            assertThat(apiErrors.errors[0].message, notNullValue())
            assertThat(apiErrors.errors[0].message, not(""))
        } catch (e: Exception) {
            assertThat("Test threw a different kind of exception than expected!",
                    e, instanceOf(HttpException::class.java))
        }
    }

    @Test
    fun test_getAuthToken_withRefreshToken() {
        API.doWithAuthToken { expiredToken ->
            val clientSecret = API.clientSecret     // fetch the client secret only once
            val credentials = RefreshReqBody(expiredToken.refreshToken, clientSecret)
            val refreshedToken = execute(API.refreshAuthToken(credentials))

            assertThat(refreshedToken.tokenType, Is("Bearer"))
            assertThat(refreshedToken.accessToken, notNullValue())
            assertThat(refreshedToken.refreshToken, isEmptyOrNullString())
            assertThat(refreshedToken.expiresIn, Is(2628000))

            // revoke only the access token, the refresh token is null anyway
            val reqBody = RevokeReqBody.fromAccessToken(refreshedToken.accessToken, clientSecret)
            execute(API.revokeAuthToken(refreshedToken.authHeader, reqBody))
        }
    }

    @Test
    fun test_revokeAuthToken() {
        val clientSecret = API.clientSecret     // fetch the client secret only once
        assertThat(clientSecret, notNullValue())
        val credentials = AuthReqBody.fromPassword(clientSecret, TEST_USER, TEST_PWD)
        val token = execute(API.getAuthToken(credentials))

        // revoke refresh token BEFORE access token, because the access token is needed for revocation!
        val revokeReqs = arrayOf(
                RevokeReqBody.fromRefreshToken(token.refreshToken, clientSecret),
                RevokeReqBody.fromAccessToken(token.accessToken, clientSecret))
        for (reqBody in revokeReqs) {
            val response = execute(API.revokeAuthToken(token.authHeader, reqBody))
            val jsonObj = response.asJsonObject

            assertThat(jsonObj.has("error"), Is(false))
            assertThat(jsonObj.get("token").asString, Is(reqBody.token))
        }
    }

    @Test
    fun test_getCurrentUser() {
        API.doWithAuthToken { token ->
            val response = execute(API.getCurrentUser(token.authHeader, ""))
            val user = response.body()!!.users[0]

            assertThat(response.code(), Is(HTTP_OK))
            assertThat(response.headers().get("ETag"), not(isEmptyOrNullString()))
            assertThat(user, notNullValue())
            assertThat(user.id, notNullValue())
            assertThat(user.name, notNullValue())
            assertThat(user.slug, notNullValue())
            assertThat(user.email, Is(TEST_USER))
            //assertThat(user.getImage(), anyOf(nullValue(), notNullValue())); // no-op
            //assertThat(user.getBio(), anyOf(nullValue(), notNullValue())); // no-op
            assertThat(user.roles, not(empty()))

            val role = user.roles.first()
            //assertThat(role.getId(), instanceOf(Integer.class)); // no-op, int can't be null
            assertThat(role.name, notNullValue())
            assertThat(role.description, notNullValue())
        }
    }

    @Test
    fun test_createPost() {
        API.doWithAuthToken { token ->
            API.createRandomPost(token) { expectedPost, response, createdPost ->
                assertThat(response.code(), Is(HTTP_CREATED))
                assertThat(createdPost.title, Is(expectedPost.title))
                assertThat(createdPost.slug, Is(Slugify().slugify(expectedPost.title)))
                assertThat(createdPost.status, Is(expectedPost.status))
                assertThat(createdPost.mobiledoc, Is(expectedPost.mobiledoc))
                assertThat(createdPost.html, containsString("<p>${expectedPost.markdown}</p>\n"))
                assertThat(createdPost.tags, Is(expectedPost.tags))
                assertThat(createdPost.customExcerpt, Is(expectedPost.customExcerpt))
                assertThat(createdPost.isFeatured, Is(expectedPost.isFeatured))
                assertThat(createdPost.isPage, Is(expectedPost.isPage))
            }
        }
    }

    @Test
    fun test_getPosts() {
        API.doWithAuthToken { token ->
            API.createRandomPost(token) { post1, _, _ ->
                API.createRandomPost(token) { post2, _, _ ->
                    val response = execute(API.getPosts(token.authHeader, "", null, 100))
                    val posts = response.body()!!.posts
                    assertThat(response.code(), Is(HTTP_OK))
                    assertThat(posts.size, Is(2))
                    // posts are returned in reverse-chrono order
                    // check latest post
                    assertThat(posts[0].title, Is(post2.title))
                    assertThat(posts[0].mobiledoc, Is(post2.mobiledoc))
                    // check second-last post
                    assertThat(posts[1].title, Is(post1.title))
                    assertThat(posts[1].mobiledoc, Is(post1.mobiledoc))
                }
            }
        }
    }

    @Test
    fun test_getPosts_limit() {
        // setting the limit to N should return the *latest* N posts
        API.doWithAuthToken { token ->
            API.createRandomPost(token) { _, _, _ ->
                API.createRandomPost(token) { post2, _, _ ->
                    val response = execute(API.getPosts(token.authHeader, "", null, 1))
                    val posts = response.body()!!.posts
                    assertThat(response.code(), Is(HTTP_OK))
                    assertThat(posts.size, Is(1))
                    assertThat(posts[0].title, Is(post2.title))
                    assertThat(posts[0].mobiledoc, Is(post2.mobiledoc))
                }
            }
        }
    }

    @Test
    fun test_getPost() {
        API.doWithAuthToken { token ->
            API.createRandomPost(token) { expected, _, created ->
                val response = execute(API.getPost(token.authHeader, created.id))
                val post = response.body()!!.posts[0]
                assertThat(response.code(), Is(HTTP_OK))
                assertThat(post.title, Is(expected.title))
                assertThat(post.mobiledoc, Is(expected.mobiledoc))
            }
        }
    }

    @Test
    fun test_updatePost() {
        API.doWithAuthToken { token ->
            API.createRandomPost(token) { newPost, _, created ->
                val updatedTag = Tag("updated-tag")
                val expectedPost = Post(newPost).also {
                    it.mobiledoc = GhostApiUtils.initializeMobiledoc()
                    it.mobiledoc = GhostApiUtils.insertMarkdownIntoMobiledoc("updated **markdown**", it.mobiledoc)
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

                assertThat(response.code(), Is(HTTP_OK))
                assertThat(actualPost.title, Is(expectedPost.title))
                assertThat(actualPost.mobiledoc, Is(expectedPost.mobiledoc))
                assertThat(actualPost.tags, hasSize(1))
                assertThat(actualPost.tags[0].name, Is(updatedTag.name))
                assertThat(actualPost.customExcerpt, Is(expectedPost.customExcerpt))
                assertThat(actualPost.isFeatured, Is(expectedPost.isFeatured))
                assertThat(actualPost.isPage, Is(expectedPost.isPage))
            }
        }
    }

    @Test
    fun test_customExcerptLimit() {
        API.doWithAuthToken { token ->
            val CUSTOM_EXCERPT_LIMIT = 300

            // excerpt length == allowed limit => should succeed
            API.createRandomPost(token) { newPost, _, created ->
                val expectedPost = Post(newPost)
                expectedPost.customExcerpt = getRandomString(CUSTOM_EXCERPT_LIMIT)
                val postStubs = PostStubList.from(expectedPost)

                val response = execute(API.updatePost(token.authHeader, created.id, postStubs))
                val actualPostId = response.body()!!.posts[0].id
                val actualPost = execute(API.getPost(token.authHeader, actualPostId))
                        .body()!!.posts[0]

                assertThat(response.code(), Is(HTTP_OK))
                assertThat(actualPost.customExcerpt.length, Is(expectedPost.customExcerpt.length))
            }

            // excerpt length == (allowed limit + 1) => should fail
            API.createRandomPost(token) { newPost, _, created ->
                val expectedPost = Post(newPost)
                expectedPost.customExcerpt = getRandomString(CUSTOM_EXCERPT_LIMIT + 1)
                val postStubs = PostStubList.from(expectedPost)

                val response = execute(API.updatePost(token.authHeader, created.id, postStubs))

                assertThat(response.code(), Is(422))   // 422 Unprocessable Entity
            }
        }
    }

    @Test
    fun test_deletePost() {
        API.doWithAuthToken { token ->
            var deleted: Post? = null
            API.createRandomPost(token) { _, _, created -> deleted = created }
            // post should be deleted by this point
            val response = execute(API.getPost(token.authHeader, deleted!!.id))
            assertThat(response.code(), Is(HTTP_NOT_FOUND))
        }
    }

    @Test
    fun test_getSettings() {
        API.doWithAuthToken { token ->
            val response = execute(API.getSettings(token.authHeader, ""))
            val settings = response.body()!!.settings

            assertThat(response.code(), Is(HTTP_OK))
            assertThat(response.headers().get("ETag"), not(isEmptyOrNullString()))
            assertThat(settings, notNullValue())
            // blog title
            assertThat(settings, hasItem(allOf(
                    hasProperty("key", Is("title")),
                    hasProperty("value", not(isEmptyOrNullString())))))
            // permalink format
            // UPDATE: permalink setting was removed in Ghost 2.0: https://github.com/TryGhost/Ghost/pull/9768/files
//            assertThat(settings, hasItem(allOf(
//                    hasProperty("key", Is("permalinks")),
//                    hasProperty("value", Is("/:slug/")))))
        }
    }

    @Test
    fun test_getConfiguration() {
        // NOTE: configuration (except the /configuration/about endpoint) can be queried without auth
        val config = execute(API.configuration).configuration

        assertThat(config, notNullValue())
    }

    @Test
    fun test_getConfigAbout() {
        API.doWithAuthToken { token ->
            val response = execute(API.getVersion(token.authHeader))
            val about = response.body()!!
            val version = about
                    .get("configuration").asJsonArray
                    .get(0).asJsonObject
                    .get("version").asString

            assertThat(response.code(), Is(HTTP_OK))
            assertThat(response.headers().get("ETag"), not(isEmptyOrNullString()))
            assertThat(about, notNullValue())
            assertThat(version, not(isEmptyOrNullString()))
        }
    }

}



// private helpers

private fun GhostApiService.createRandomPost(token: AuthToken, callback: (Post, Response<PostList>, Post) -> Unit) {
    val title = getRandomString(20)
    val markdown = getRandomString(100)
    val newPost = Post().also {
        it.title = title
        it.mobiledoc = GhostApiUtils.initializeMobiledoc()
        it.mobiledoc = GhostApiUtils.insertMarkdownIntoMobiledoc(markdown, it.mobiledoc)
        it.tags = RealmList()
        it.customExcerpt = markdown.substring(0, 100)
    }
    val response = execute(this.createPost(token.authHeader, PostStubList.from(newPost)))
    val createdId = response.body()!!.posts[0].id
    val created = execute(this.getPost(token.authHeader, createdId)).body()!!.posts[0]

    try {
        callback(newPost, response, created)
    } finally {
        execute(this.deletePost(token.authHeader, created.id))
    }
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
