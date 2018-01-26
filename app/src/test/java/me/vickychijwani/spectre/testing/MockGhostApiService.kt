package me.vickychijwani.spectre.testing

import com.google.gson.*
import io.reactivex.Observable
import me.vickychijwani.spectre.model.entity.*
import me.vickychijwani.spectre.network.GhostApiService
import me.vickychijwani.spectre.network.entity.*
import okhttp3.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import retrofit2.mock.*
import java.net.HttpURLConnection

class MockGhostApiService(private val mDelegate: BehaviorDelegate<GhostApiService>,
                          private val mUseGhostAuth: Boolean) : GhostApiService {

    override fun getAuthToken(@Body credentials: AuthReqBody): Observable<AuthToken> {
        if (mUseGhostAuth && credentials.authorizationCode == "auth-code" || !mUseGhostAuth && credentials.password == "password") {
            val token = AuthToken().also {
                it.accessToken = "access-token"
                it.refreshToken = "refresh-token"
                it.createdAt = System.currentTimeMillis()
                it.expiresIn = 60 * 1000
            }
            return mDelegate
                    .returningResponse(token)
                    .getAuthToken(credentials)
        } else {
            // wrong Ghost auth code or password
            // Mock Retrofit doesn't set the correct request URL, it sets just http://localhost
            val tokenUrl = "http://localhost/authentication/token/"
            val body = ResponseBody.create(MediaType.parse("application/json"),
                    "{\"errors\":[{\"message\":\"Your password is incorrect.\",\"context\":\"Your password is incorrect.\",\"errorType\":\"ValidationError\"}]}")
            val rawResponse = okhttp3.Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(Request.Builder().url(tokenUrl).build())
                    .code(HttpURLConnection.HTTP_UNAUTHORIZED).message("Unauthorized")
                    .body(body)
                    .build()
            val res = Response.error<Any>(body, rawResponse)
            return mDelegate
                    .returning(Calls.response(res))
                    .getAuthToken(credentials)
        }
    }

    override fun refreshAuthToken(@Body credentials: RefreshReqBody): Observable<AuthToken> {
        if (credentials.refreshToken == "refresh-token") {
            val token = AuthToken().also {
                it.accessToken = "refreshed-access-token"
                // it.setRefreshToken("refresh-token");      // refreshed tokens don't have a new refresh token
                it.createdAt = System.currentTimeMillis()
                it.expiresIn = 60 * 1000
            }
            return mDelegate
                    .returningResponse(token)
                    .refreshAuthToken(credentials)
        } else {
            // expired / invalid refresh token
            val body = ResponseBody.create(MediaType.parse("application/json"),
                    "{\"errors\":[{\"message\":\"Expired or invalid refresh token\",\"errorType\":\"UnauthorizedError\"}]}")
            val res = Response.error<Any>(401, body)
            return mDelegate
                    .returning(Calls.response(res))
                    .refreshAuthToken(credentials)
        }
    }

    override fun revokeAuthToken(@Header("Authorization") authHeader: String, @Body revoke: RevokeReqBody): Observable<JsonElement>? {
        return null
    }

    override fun getCurrentUser(@Header("Authorization") authHeader: String, @Header("If-None-Match") etag: String): Call<UserList>? {
        return null
    }

    override fun createPost(@Header("Authorization") authHeader: String, @Body posts: PostStubList): Call<PostList>? {
        return null
    }

    override fun getPosts(@Header("Authorization") authHeader: String, @Header("If-None-Match") etag: String, @Query("filter") filter: String, @Query("limit") numPosts: Int): Call<PostList>? {
        return null
    }

    override fun getPost(@Header("Authorization") authHeader: String, @Path("id") id: String): Call<PostList>? {
        return null
    }

    override fun updatePost(@Header("Authorization") authHeader: String, @Path("id") id: String, @Body posts: PostStubList): Call<PostList>? {
        return null
    }

    override fun deletePost(@Header("Authorization") authHeader: String, @Path("id") id: String): Call<String>? {
        return null
    }

    override fun getSettings(@Header("Authorization") authHeader: String, @Header("If-None-Match") etag: String): Call<SettingsList>? {
        return null
    }

    override fun getConfiguration(): Observable<ConfigurationList> {
        val config = if (mUseGhostAuth) {
            ConfigurationList.from(
                    ConfigurationParam("clientSecret", "client-secret"),
                    ConfigurationParam("ghostAuthId", "ghost-auth-id"),
                    ConfigurationParam("ghostAuthUrl", "ghost-auth-url"),
                    ConfigurationParam("blogUrl", "http://blog.com"))
        } else {
            ConfigurationList.from(
                    ConfigurationParam("clientSecret", "client-secret"))
        }
        return mDelegate
                .returningResponse(config)
                .configuration
    }

    override fun getVersion(@Header("Authorization") authHeader: String): Call<JsonObject>? {
        return null
    }

    override fun uploadFile(@Header("Authorization") authHeader: String, @Part file: MultipartBody.Part): Call<JsonElement>? {
        return null
    }

}
