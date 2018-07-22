package me.vickychijwani.spectre.testing

import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.closeSoftKeyboard
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import me.vickychijwani.spectre.R
import me.vickychijwani.spectre.view.*
import org.hamcrest.CoreMatchers.containsString


fun startLogin(func: BlogAddressRobot.() -> Unit) = BlogAddressRobot().apply { func() }

class BlogAddressRobot {
    fun blogAddress(address: String) {
        onView(withId(R.id.blog_url))
                .perform(replaceText(address), closeSoftKeyboard())
    }

    fun connectToBlog(func: ConnectResultRobot.() -> Unit): ConnectResultRobot {
        onView(withId(R.id.next_btn))
                .perform(click())
        return ConnectResultRobot().apply { func() }
    }

    fun isLoggedOut() {
        intended(hasActivity(LoginActivity::class))
    }
}

interface ErrorRobot {
    fun errorMatching(partialMessage: String) {
        onView(withText(containsString(partialMessage)))
                .check(matches(isDisplayed()))
    }
}

class ConnectResultRobot : ErrorRobot {
    fun email(email: String) {
        onView(withId(R.id.email))
                .perform(replaceText(email))
    }

    fun password(password: String) {
        onView(withId(R.id.password))
                .perform(replaceText(password))
    }

    fun login(func: LoginResultRobot.() -> Unit): LoginResultRobot {
        onView(withId(R.id.sign_in_btn))
                .perform(click())
        return LoginResultRobot().apply { func() }
    }
}

class LoginResultRobot : ErrorRobot {
    fun isLoggedIn() {
        intended(hasActivity(PostListActivity::class))
    }

    fun logout(func: BlogAddressRobot.() -> Unit) : BlogAddressRobot {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText("Logout"))
                .perform(click())
        return BlogAddressRobot().apply { func() }
    }
}
