package me.vickychijwani.spectre.testing

import io.reactivex.Observable
import me.vickychijwani.spectre.model.entity.AuthToken
import me.vickychijwani.spectre.network.GhostApiService
import me.vickychijwani.spectre.network.entity.*
import org.hamcrest.Matchers
import org.junit.Assert
import retrofit2.*

const val TEST_BLOG = "10.0.2.2:2368"
const val TEST_BLOG_WITH_PROTOCOL = "http://$TEST_BLOG"
const val TEST_USER = "user@example.com"
const val TEST_PWD = "randomtestpwd"

// extension functions for the GhostApiService
fun GhostApiService.deleteDefaultPosts() {
    this.doWithAuthToken { token ->
        val posts = execute(this.getPosts(token.authHeader, "", null, 100)).body()!!
        // A default Ghost install has 8 posts initially. If there are more than this, or the blog
        // address is not localhost, abort. This is to avoid messing up a production blog by mistake.
        val MAX_EXPECTED_POSTS = 8
        if (posts.posts.size > MAX_EXPECTED_POSTS || TEST_BLOG_WITH_PROTOCOL != "http://10.0.2.2:2368") {
            throw IllegalStateException("Aborting! Expected max $MAX_EXPECTED_POSTS posts, " +
                    "found ${posts.posts.size}")
        }
        for (post in posts.posts) {
            execute(this.deletePost(token.authHeader, post.id))
        }
    }
}

fun GhostApiService.doWithAuthToken(callback: (AuthToken) -> Unit) {
    val clientSecret = clientSecret     // fetch the client secret only once
    Assert.assertThat(clientSecret, Matchers.notNullValue())
    val credentials = AuthReqBody.fromPassword(clientSecret, TEST_USER, TEST_PWD)
    val token = execute(this.getAuthToken(credentials))
    try {
        callback(token)
    } finally {
        // revoke refresh token BEFORE access token, because the access token is needed for revocation!
        val revokeReqs = arrayOf(
                RevokeReqBody.fromRefreshToken(token.refreshToken, clientSecret),
                RevokeReqBody.fromAccessToken(token.accessToken, clientSecret))
        for (reqBody in revokeReqs) {
            execute(this.revokeAuthToken(token.authHeader, reqBody))
        }
    }
}

val GhostApiService.clientSecret: String
    get() = execute(this.configuration).clientSecret

fun <T> execute(call: Call<T>): Response<T> {
    return call.execute()
}

fun <T> execute(observable: Observable<T>): T {
    return observable.blockingFirst()
}
