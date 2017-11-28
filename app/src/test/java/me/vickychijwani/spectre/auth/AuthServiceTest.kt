package me.vickychijwani.spectre.auth

import com.squareup.otto.Subscribe
import io.reactivex.Observable
import me.vickychijwani.spectre.auth.AuthService.Listener
import me.vickychijwani.spectre.event.BusProvider.getBus
import me.vickychijwani.spectre.event.CredentialsExpiredEvent
import me.vickychijwani.spectre.model.entity.AuthToken
import me.vickychijwani.spectre.network.*
import me.vickychijwani.spectre.testing.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.hasProperty
import org.junit.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.hamcrest.MockitoHamcrest.argThat

/**
 * TYPE: unit tests
 * PURPOSE: testing logic for token / credential expiry scenarios
 */

class AuthServiceTest {

    companion object {
        @ClassRule @JvmField val rxSchedulersRule = RxSchedulersRule()
        @ClassRule @JvmField val loggingRule = JvmLoggingRule()
        @ClassRule @JvmField val eventBusRule = EventBusRule()

        private val BLOG_URL = "https://blog.example.com"
    }

    private lateinit var credSource: CredentialSource
    private lateinit var credSink: CredentialSink
    private lateinit var listener: Listener

    @Before
    fun setupMocks() {
        // source must be == sink because of the limitation in AuthService#loginAgain
        val credSourceAndSink = mock(AuthStore::class.java)
        credSource = credSourceAndSink
        credSink = credSourceAndSink
        listener = mock(Listener::class.java)
    }


    // tests
    @Test
    fun refreshToken_expiredAccessToken() {
        refreshToken("expired-access-token", "refresh-token", "auth-code")

        verify(credSink).setLoggedIn(BLOG_URL, true)
        verify(listener).onNewAuthToken(argThat(hasProperty("accessToken",
                `is`("refreshed-access-token"))))
        verify(listener).onNewAuthToken(argThat(hasProperty("refreshToken",
                `is`("refresh-token"))))
    }

    @Test
    fun refreshToken_expiredAccessAndRefreshToken() {
        refreshToken("expired-access-token", "expired-refresh-token", "auth-code")

        verify(credSink).setLoggedIn(BLOG_URL, true)
        verify(listener).onNewAuthToken(argThat(hasProperty("accessToken",
                `is`("access-token"))))
        verify(listener).onNewAuthToken(argThat(hasProperty("refreshToken",
                `is`("refresh-token"))))
    }

    @Test
    fun refreshToken_expiredTokensAndAuthCode() {
        val spy = spy(CredentialsExpiredEventListener())
        getBus().register(spy)

        refreshToken("expired-access-token", "expired-refresh-token", "expired-auth-code")

        verify(credSink).deleteCredentials(BLOG_URL)
        verify(spy).onCredentialsExpiredEvent(any())
        getBus().unregister(spy)
    }


    // helpers
    private fun refreshToken(accessToken: String, refreshToken: String, authCode: String) {
        val api = GhostApiUtils.getRetrofit(BLOG_URL, Helpers.prodHttpClient).let {
            Helpers.getMockRetrofit(it, Helpers.idealNetworkBehavior).let {
                MockGhostApiService(it.create(GhostApiService::class.java), true)
            }
        }

        `when`(credSource.getGhostAuthCode(any())).thenReturn(Observable.just(authCode))

        val token = AuthToken()
        token.accessToken = accessToken
        token.refreshToken = refreshToken

        val authService = AuthService(BLOG_URL, api, credSource, credSink)
        authService.listen(listener)
        authService.refreshToken(token)
    }

}

private open class CredentialsExpiredEventListener {
    @Subscribe
    open fun onCredentialsExpiredEvent(event: CredentialsExpiredEvent?) {
        // no-op
    }
}
