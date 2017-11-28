package me.vickychijwani.spectre.auth

import com.squareup.otto.Subscribe
import io.reactivex.Observable
import me.vickychijwani.spectre.auth.LoginOrchestrator.HACKListener
import me.vickychijwani.spectre.auth.LoginOrchestrator.Listener
import me.vickychijwani.spectre.event.*
import me.vickychijwani.spectre.event.BusProvider.getBus
import me.vickychijwani.spectre.network.*
import me.vickychijwani.spectre.network.entity.AuthReqBody
import me.vickychijwani.spectre.testing.*
import me.vickychijwani.spectre.util.Pair
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.everyItem
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.*
import org.junit.Assert.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.hamcrest.MockitoHamcrest.argThat
import retrofit2.Retrofit
import retrofit2.mock.NetworkBehavior
import java.util.*

/**
 * TYPE: unit tests
 * PURPOSE: testing overall logic flow for login process. This does NOT include logic for refreshing
 * an expired token, etc - only the initial user login flow is covered. The refresh logic is tested
 * in [AuthServiceTest].
 */

class LoginOrchestratorTest {

    companion object {
        @ClassRule @JvmField var rxSchedulersRule = RxSchedulersRule()
        @ClassRule @JvmField var loggingRule = JvmLoggingRule()
        @ClassRule @JvmField var eventBusRule = EventBusRule()

        private val BLOG_URL_WITHOUT_PROTOCOL = "blog.example.com"
        private val BLOG_URL = "http://$BLOG_URL_WITHOUT_PROTOCOL"

        private fun makeOrchestrator(credSource: CredentialSource,
                                     credSink: CredentialSink,
                                     useGhostAuth: Boolean,
                                     networkBehavior: NetworkBehavior = Helpers.idealNetworkBehavior)
                : LoginOrchestrator {
            val blogUrlValidator = BlogUrlValidator { Observable.just("http://$it") }
            val hackListener = mock(HACKListener::class.java)
            val apiProviderFactory = MockApiProviderFactory(useGhostAuth, networkBehavior)
            return LoginOrchestrator(blogUrlValidator, apiProviderFactory, credSource, credSink,
                    hackListener)
        }
    }

    private lateinit var credSource: CredentialSource
    private lateinit var credSink: CredentialSink
    private lateinit var listener: Listener

    @Before
    fun setupMocks() {
        credSource = mock(CredentialSource::class.java)
        credSink = mock(CredentialSink::class.java)
        listener = mock(Listener::class.java)
    }


    // tests
    @Test
    fun ghostAuth_success() {
        val orchestrator = makeOrchestrator(credSource, credSink, true)
        `when`(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"))
        orchestrator.listen(listener)

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL)

        verify<Listener>(listener).onStartWaiting()
        verify<CredentialSource>(credSource).getGhostAuthCode(any<GhostAuth.Params>())
        verify<CredentialSink>(credSink).saveCredentials(argThat(`is`(BLOG_URL)), any<AuthReqBody>())
        verify<CredentialSink>(credSink, never()).deleteCredentials(BLOG_URL)
        verify<Listener>(listener).onLoginDone()
    }

    @Test
    fun ghostAuth_networkFailure() {
        val failingNetworkBehavior = Helpers.failingNetworkBehaviour
        val orchestrator = makeOrchestrator(credSource, credSink, true, failingNetworkBehavior)
        `when`(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"))
        orchestrator.listen(listener)

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL)

        verify<Listener>(listener).onNetworkError(any<LoginOrchestrator.ErrorType>(),
                argThat(sameInstance(failingNetworkBehavior.failureException())))
    }

    @Test
    fun passwordAuth_success() {
        val orchestrator = makeOrchestrator(credSource, credSink, false)
        `when`(credSource.getEmailAndPassword(any()))
                .thenReturn(Observable.just(Pair("email", "password")))
        orchestrator.listen(listener)

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL)

        verify<Listener>(listener).onStartWaiting()
        verify<CredentialSource>(credSource).getEmailAndPassword(any<PasswordAuth.Params>())
        verify<CredentialSink>(credSink).saveCredentials(argThat(`is`(BLOG_URL)), any<AuthReqBody>())
        verify<CredentialSink>(credSink, never()).deleteCredentials(BLOG_URL)
        verify<Listener>(listener).onLoginDone()
    }

    @Test
    fun passwordAuth_wrongPassword() {
        val orchestrator = makeOrchestrator(credSource, credSink, false)
        orchestrator.listen(listener)
        // simulate entering the wrong password once, followed by the right password
        val retrying = booleanArrayOf(false)
        val retried = booleanArrayOf(false)
        `when`(credSource.getEmailAndPassword(any())).thenReturn(Observable.fromCallable Observable@ {
            if (!retrying[0]) {
                retrying[0] = true
                return@Observable Pair<String, String>("email", "wrong-password")
            } else {
                retried[0] = true
                return@Observable Pair<String, String>("email", "password")
            }
        })

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL)

        verify<Listener>(listener).onStartWaiting()
        verify<CredentialSource>(credSource).getEmailAndPassword(any<PasswordAuth.Params>())
        verify<CredentialSink>(credSink).saveCredentials(argThat(`is`(BLOG_URL)), any<AuthReqBody>())
        verify<CredentialSink>(credSink, never()).deleteCredentials(BLOG_URL)
        verify<Listener>(listener).onLoginDone()
        // this throws an NPE for no apparent reason - wtf? hence the ugly "retried" flag
        //verify(listener).onApiError(any(), any());
        assertThat(retried[0], `is`(true))
    }

    @Test
    fun loginSucceededEvent() {
        val spy = spy(LoginStatusEventListener())
        getBus().register(spy)
        val orchestrator = makeOrchestrator(credSource, credSink, true)
        `when`(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"))
        orchestrator.listen(listener)

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL)

        verify(spy).onLoginDoneEvent(any())
        getBus().unregister(spy)
    }

    @Test
    fun loginFailureEvent() {
        val spy = spy(LoginStatusEventListener())
        getBus().register(spy)

        val orchestrator = makeOrchestrator(credSource, credSink, true)
        `when`(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("wrong-auth-code"))
        orchestrator.listen(listener)

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL)

        // atLeastOnce() because the operation gets retried automatically
        verify(spy, atLeastOnce()).onLoginErrorEvent(any())
        getBus().unregister(spy)
    }

    @Test
    fun normalizeBlogUrl_shouldTrimWhitespaceAndTrailingGhostPath() {
        assertThat(Arrays.asList(
                LoginOrchestrator.normalizeBlogUrl("  https://my-blog.com         "),
                LoginOrchestrator.normalizeBlogUrl("  https://my-blog.com/ghost   "),
                LoginOrchestrator.normalizeBlogUrl("  https://my-blog.com/ghost/  ")
        ), everyItem(`is`("https://my-blog.com")))
    }

    private class MockApiProviderFactory(private val mUseGhostAuth: Boolean,
                                         private val mNetworkBehavior: NetworkBehavior)
        : ApiProviderFactory {

        override fun create(blogUrl: String): ApiProvider {
            val retrofit = GhostApiUtils.getRetrofit(blogUrl, Helpers.prodHttpClient)
            return object : ApiProvider {
                override fun getRetrofit(): Retrofit {
                    return retrofit
                }

                override fun getGhostApi(): GhostApiService {
                    val mockRetrofit = Helpers.getMockRetrofit(retrofit, mNetworkBehavior)
                    val delegate = mockRetrofit.create(GhostApiService::class.java)
                    return MockGhostApiService(delegate, mUseGhostAuth)
                }
            }
        }
    }

}

private open class LoginStatusEventListener {
    @Subscribe
    open fun onLoginDoneEvent(event: LoginDoneEvent?) {
        // no-op
    }

    @Subscribe
    open fun onLoginErrorEvent(event: LoginErrorEvent?) {
        // no-op
    }
}
