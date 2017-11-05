package me.vickychijwani.spectre.auth

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import me.vickychijwani.spectre.R
import me.vickychijwani.spectre.testing.ClearPreferencesRule
import me.vickychijwani.spectre.view.LoginActivity
import me.vickychijwani.spectre.view.PostListActivity
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass


// Tests follow the Jake Wharton's Robot pattern explained here:
// https://academy.realm.io/posts/kau-jake-wharton-testing-robots/

@RunWith(AndroidJUnit4::class) @LargeTest
class LoginTest {

    @Rule @JvmField
    val mActivityRule = IntentsTestRule(LoginActivity::class.java)

    @Rule @JvmField
    val mPrefsRule = ClearPreferencesRule()

    @Test
    fun successfulLogin() {
        startLogin {
            blogAddress("10.0.2.2:2368")
        } connectToBlog {
            email("user@example.com")
            password("randomtestpwd")
        } login {
            isLoggedIn()
        }
    }

    @Test
    fun nonExistentBlog() {
        startLogin {
            blogAddress("nonexistent_blog")
        } connectToBlog {
            errorMatching("There is no Ghost admin")
        }
    }

    @Test
    fun invalidEmail() {
        startLogin {
            blogAddress("10.0.2.2:2368")
        } connectToBlog {
            email("invalid_email")
        } login {
            errorMatching("This email address is invalid")
        }
    }

    @Test
    fun nonExistentUser() {
        startLogin {
            blogAddress("10.0.2.2:2368")
        } connectToBlog {
            email("nonexistent@example.com")
            password("doesnt_matter")
        } login {
            errorMatching("There is no user with that email address")
        }
    }

    @Test
    fun wrongPassword() {
        startLogin {
            blogAddress("10.0.2.2:2368")
        } connectToBlog {
            email("user@example.com")
            password("wrongpassword")
        } login {
            errorMatching("Your password is incorrect")
        }
    }

}

private fun <T : Activity> hasActivity(cls: KClass<T>): Matcher<Intent> {
    return hasComponent(ComponentName(getTargetContext(), cls.java.name))
}

private fun startLogin(func: BlogAddressRobot.() -> Unit) = BlogAddressRobot().apply { func() }

private class BlogAddressRobot {
    fun blogAddress(address: String) {
        onView(withId(R.id.blog_url))
                .perform(typeText(address))
    }

    infix fun connectToBlog(func: ConnectResultRobot.() -> Unit): ConnectResultRobot {
        onView(withId(R.id.next_btn))
                .perform(click())
        return ConnectResultRobot().apply { func() }
    }
}

private interface ErrorRobot {
    fun errorMatching(partialMessage: String) {
        onView(withText(containsString(partialMessage)))
                .check(matches(isDisplayed()))
    }
}

private class ConnectResultRobot : ErrorRobot {
    fun email(email: String) {
        onView(withId(R.id.email))
                .perform(typeText(email))
    }

    fun password(password: String) {
        onView(withId(R.id.password))
                .perform(typeText(password))
    }

    infix fun login(func: LoginResultRobot.() -> Unit): LoginResultRobot {
        onView(withId(R.id.sign_in_btn))
                .perform(click())
        return LoginResultRobot().apply{ func() }
    }
}

private class LoginResultRobot : ErrorRobot {
    fun isLoggedIn() {
        intended(hasActivity(PostListActivity::class))
    }
}
