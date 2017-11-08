package me.vickychijwani.spectre.view

import android.support.test.espresso.*
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import me.vickychijwani.spectre.R
import me.vickychijwani.spectre.testing.*
import org.junit.*
import org.junit.runner.RunWith


// Tests follow Jake Wharton's Robot pattern explained here:
// https://academy.realm.io/posts/kau-jake-wharton-testing-robots/

@RunWith(AndroidJUnit4::class) @LargeTest
class LoginTest {

    @Rule @JvmField
    val mActivityRule = IntentsTestRule(LoginActivity::class.java)

    @Rule @JvmField
    val mPrefsRule = ClearPreferencesRule()

    private lateinit var mProgressBarIdlingResource: IdlingResource

    @Before
    fun setup() {
        mProgressBarIdlingResource = ViewNotVisibleIdlingResource(mActivityRule.activity, R.id.progress)
        IdlingRegistry.getInstance().register(mProgressBarIdlingResource)
    }

    @After
    fun teardown() {
        IdlingRegistry.getInstance().unregister(mProgressBarIdlingResource)
    }

    @Test
    fun successfulLogin() {
        startLogin {
            blogAddress(TEST_BLOG)
        }.connectToBlog {
            email(TEST_USER)
            password(TEST_PWD)
        }.login {
            isLoggedIn()
        }
    }

    @Test
    fun nonExistentBlog() {
        startLogin {
            blogAddress("nonexistent_blog")
        }.connectToBlog {
            errorMatching("There is no Ghost admin")
        }
    }

    @Test
    fun invalidEmail() {
        startLogin {
            blogAddress(TEST_BLOG)
        }.connectToBlog {
            email("invalid_email")
        }.login {
            errorMatching("This email address is invalid")
        }
    }

    @Test
    fun nonExistentUser() {
        startLogin {
            blogAddress(TEST_BLOG)
        }.connectToBlog {
            email("nonexistent@example.com")
            password("doesnt_matter")
        }.login {
            errorMatching("There is no user with that email address")
        }
    }

    @Test
    fun wrongPassword() {
        startLogin {
            blogAddress(TEST_BLOG)
        }.connectToBlog {
            email(TEST_USER)
            password("wrongpassword")
        }.login {
            errorMatching("Your password is incorrect")
        }
    }

}
