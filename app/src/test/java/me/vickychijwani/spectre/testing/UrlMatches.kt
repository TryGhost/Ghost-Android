package me.vickychijwani.spectre.testing

import org.hamcrest.Description
import org.hamcrest.Factory
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Custom Hamcrest matcher that matches 2 URLs ignoring ONLY trailing slashes. This can't handle
 * stuff like port 80/443 being the default for HTTP/HTTPS, or query params being unordered, or even
 * URLs being case-insensitive.
 */
class UrlMatches(private val expected: String) : TypeSafeMatcher<String>() {

    override fun matchesSafely(actual: String): Boolean {
        return matchUrls(actual, expected)
    }

    override fun describeTo(description: Description) {
        description.appendText("a URL string that exactly matches ")
                .appendValue(expected)
                .appendText(" ignoring trailing slashes")
    }

    private fun matchUrls(actual: String, expected: String): Boolean {
        val normalizedActual = actual.replaceFirst("/$".toRegex(), "")
        val normalizedExpected = expected.replaceFirst("/$".toRegex(), "")
        return normalizedActual == normalizedExpected
    }

}

@Factory
fun urlMatches(expectedUrl: String): Matcher<String> {
    return UrlMatches(expectedUrl)
}
