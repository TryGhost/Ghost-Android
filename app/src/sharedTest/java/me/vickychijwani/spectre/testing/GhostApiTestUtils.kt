package me.vickychijwani.spectre.testing

import io.reactivex.Observable
import me.vickychijwani.spectre.model.entity.AuthToken
import me.vickychijwani.spectre.network.GhostApiService
import me.vickychijwani.spectre.network.entity.*
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import retrofit2.*

// extension functions for the GhostApiService
fun GhostApiService.deleteDefaultPosts(user: String, password: String) {
    this.doWithAuthToken(user, password) { token ->
        val posts = execute(this.getPosts(token.authHeader, "", 100)).body()!!
        // A default Ghost install has these many posts initially. If there are more than this,
        // abort. This is to avoid messing up a production blog (like my own) by mistake.
        val DEFAULT_POST_COUNT = 7
        if (posts.posts.isNotEmpty() && posts.posts.size != DEFAULT_POST_COUNT) {
            throw IllegalStateException("Aborting! Expected $DEFAULT_POST_COUNT posts, " +
                    "found ${posts.posts.size}")
        }
        for (post in posts.posts) {
            execute(this.deletePost(token.authHeader, post.id))
        }
    }
}

fun GhostApiService.doWithAuthToken(user: String, password: String, callback: (AuthToken) -> Unit) {
    val clientSecret = clientSecret     // fetch the client secret only once
    assertThat(clientSecret, notNullValue())
    val credentials = AuthReqBody.fromPassword(clientSecret, user, password)
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
