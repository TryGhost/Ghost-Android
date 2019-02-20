package me.vickychijwani.spectre.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import io.reactivex.Single;
import me.vickychijwani.spectre.error.UrlNotFoundException;
import okhttp3.Call;
import retrofit2.HttpException;
import retrofit2.Response;

public class NetworkUtils {

    /**
     * Check whether there is any network with a usable connection.
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isUnauthorized(@Nullable Response response) {
        if (response == null) {
            return false;
        }
        // Ghost returns 403 Forbidden in some cases, inappropriately
        // see this for what 401 vs 403 should mean: http://stackoverflow.com/a/3297081/504611
        return response.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                || response.code() == HttpURLConnection.HTTP_FORBIDDEN;
    }

    public static boolean isUnauthorized(@Nullable Throwable e) {
        if (e == null || !(e instanceof HttpException)) {
            return false;
        }
        HttpException httpEx = (HttpException) e;
        // Ghost returns 403 Forbidden in some cases, inappropriately
        // see this for what 401 vs 403 should mean: http://stackoverflow.com/a/3297081/504611
        return httpEx.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                || httpEx.code() == HttpURLConnection.HTTP_FORBIDDEN;
    }

    public static boolean isNotModified(@Nullable Response response) {
        //noinspection SimplifiableIfStatement
        if (response == null) {
            return false;
        }
        return response.code() == HttpURLConnection.HTTP_NOT_MODIFIED;
    }

    public static boolean isNotFound(@Nullable Throwable e) {
        if (e == null || !(e instanceof HttpException)) {
            return false;
        }
        HttpException httpEx = (HttpException) e;
        return httpEx.code() == HttpURLConnection.HTTP_NOT_FOUND;
    }

    public static boolean isUnprocessableEntity(@Nullable Throwable e) {
        if (e == null || !(e instanceof HttpException)) {
            return false;
        }
        HttpException httpEx = (HttpException) e;
        return httpEx.code() == 422;
    }

    public static boolean isTooManyRequests(@Nullable Throwable e) {
        if (e == null || !(e instanceof HttpException)) {
            return false;
        }
        HttpException httpEx = (HttpException) e;
        return httpEx.code() == 429;  // too many requests
    }

    public static boolean isUnrecoverableError(@Nullable Response response) {
        if (response == null) {
            return false;
        }
        return response.code() >= 400 && isUnauthorized(response);
    }

    public static boolean isConnectionError(Throwable error) {
        return error instanceof ConnectException || error instanceof SocketTimeoutException;
    }

    public static boolean isUserNetworkError(Throwable error) {
        // user provided a malformed / non-existent URL
        return error instanceof UnknownHostException || error instanceof MalformedURLException
                || error instanceof UrlNotFoundException;
    }

    public static boolean isSslError(Throwable error) {
        return error instanceof SSLHandshakeException;
    }

    // Picasso cannot handle protocol-relative URLs
    public static String makePicassoUrl(@NonNull String baseUrl,
                                        @NonNull String relativeOrProtocolRelativeUrl) {
        String maybeProtocolRelativeUrl = makeAbsoluteUrl(baseUrl, relativeOrProtocolRelativeUrl);
        String protocol = baseUrl.startsWith("https") ? "https" : "http";
        return resolveProtocolRelativeUrl(protocol, maybeProtocolRelativeUrl);
    }

    private static String resolveProtocolRelativeUrl(@NonNull String protocol,
                                                     @NonNull String protocolRelativeUrl) {
        if (! protocolRelativeUrl.startsWith("//")) {
            // URL is not protocol-relative, return it as-is
            return protocolRelativeUrl;
        }

        return protocol + ":" + protocolRelativeUrl;
    }

    public static String makeAbsoluteUrl(@NonNull String baseUrl, @NonNull String relativePath) {
        // maybe relativePath is already absolute
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")
                // protocol-relative URLs (can't remember which scenario actually produces these)
                || relativePath.startsWith("//")) {
            return relativePath;
        }

        boolean baseHasSlash = baseUrl.endsWith("/");
        boolean relHasSlash = relativePath.startsWith("/");
        if (baseHasSlash && relHasSlash) {
            return baseUrl + relativePath.substring(1);
        } else if (baseHasSlash ^ relHasSlash) {
            return baseUrl + relativePath;
        } else {
            return baseUrl + "/" + relativePath;
        }
    }

    public static Single<okhttp3.Response> networkCall(@NonNull Call call) {
        return Single
                .fromCallable(call::execute)
                .doOnDispose(call::cancel);
    }

}
