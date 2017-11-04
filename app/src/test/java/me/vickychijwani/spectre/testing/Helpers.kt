package me.vickychijwani.spectre.testing

import io.reactivex.Single
import me.vickychijwani.spectre.network.ProductionHttpClientFactory
import okhttp3.OkHttpClient
import okhttp3.internal.tls.SslClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

object Helpers {

    val LOCALHOST_SOCKET_FACTORY = SslClient.localhost().socketFactory!!
    private val LOCALHOST_TRUST_MANAGER = SslClient.localhost().trustManager!!

    // helpers
    val prodHttpClient: OkHttpClient
        get() {
            val httpClient = ProductionHttpClientFactory().create(null)

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            return httpClient.newBuilder()
                    .sslSocketFactory(LOCALHOST_SOCKET_FACTORY, LOCALHOST_TRUST_MANAGER)
                    .addInterceptor(loggingInterceptor)
                    .build()
        }

    val idealNetworkBehavior: NetworkBehavior
        get() = getNetworkBehavior(0, 0, 0, 0)

    val failingNetworkBehaviour: NetworkBehavior
        get() = getNetworkBehavior(0, 0, 100, 0)

    fun getMockRetrofit(retrofit: Retrofit, networkBehavior: NetworkBehavior): MockRetrofit {
        return MockRetrofit.Builder(retrofit)
                .networkBehavior(networkBehavior)
                .build()
    }

    fun <T> execute(single: Single<T>): T {
        return single.blockingGet()
    }


    // private methods
    private fun getNetworkBehavior(delayMsec: Int, delayVariance: Int,
                                   failurePercent: Int, errorPercent: Int): NetworkBehavior {
        return NetworkBehavior.create().also {
            it.setDelay(delayMsec.toLong(), TimeUnit.MILLISECONDS)
            it.setVariancePercent(delayVariance)
            // "failure" means network layer failure
            it.setFailurePercent(failurePercent)
            // "error" means HTTP error
            it.setErrorPercent(errorPercent)
        }
    }

}
