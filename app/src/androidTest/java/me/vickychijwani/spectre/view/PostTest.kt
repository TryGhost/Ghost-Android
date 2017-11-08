package me.vickychijwani.spectre.view

import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.action.ViewActions.swipeLeft
import android.support.test.espresso.action.ViewActions.swipeRight
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.hasDescendant
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.web.assertion.WebViewAssertions.webMatches
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.support.test.espresso.web.webdriver.DriverAtoms.getText
import android.support.test.espresso.web.webdriver.Locator
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import me.vickychijwani.spectre.R
import me.vickychijwani.spectre.model.entity.Post
import me.vickychijwani.spectre.testing.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) @LargeTest
class PostTest {

    @Rule @JvmField
    val mActivityRule = IntentsTestRule(LoginActivity::class.java)

    @Rule @JvmField
    val mPrefsRule = ClearPreferencesRule()

    @Rule @JvmField
    val mOkHttpIdlingResourceRule = OkHttpIdlingResourceRule()

    // login before each test; no need to log out because preferences are cleared after each test
    @Before
    fun setup() {
        val r = ViewNotVisibleIdlingResource(mActivityRule.activity, R.id.progress)
        IdlingRegistry.getInstance().register(r)
        startLogin {
            blogAddress(TEST_BLOG)
        }.connectToBlog {
            email(TEST_USER)
            password(TEST_PWD)
        }.login {}
        IdlingRegistry.getInstance().unregister(r)
    }

    @Test
    fun createAndDeleteDraft() {
        newPost {
        }.backToPostList {
            hasDraft(Post.DEFAULT_TITLE, position = 0)
        }.openPost(position = 0).doWithPost {
            hasEditorTitle(Post.DEFAULT_TITLE)
        }.deleteDraft {
            doesNotHavePost(Post.DEFAULT_TITLE, position = 0)
        }
    }

    @Test
    fun previewDraft() {
        newPost {
            editorTitle("New post")
            editorBody("**Bold** _Italic_ [Link](https://ghost.org)")
            preview()
            hasPreviewTitle("New post")
            // espresso-web doesn't allow extracting the HTML source, so this is the best we can do
            hasPreviewBody("Bold Italic Link")
        }.backToPostList {
            hasDraft("New post", position = 0)
        }.openPost(position = 0).deleteDraft {}
    }

}

private fun newPost(func: PostViewRobot.() -> Unit): PostViewRobot {
    onView(withId(R.id.new_post_btn))
            .perform(click())
    return PostViewRobot().apply { func() }
}

private class PostListRobot {
    fun hasDraft(title: String, position: Int) {
        if (title == "Draft") {
            throw IllegalArgumentException("This method cannot handle a post with title = $title, " +
                    "see the implementation for why")
        }
        onView(withRecyclerView(R.id.post_list).atPosition(position))
                .check(matches(hasDescendant(withText(title))))
                .check(matches(hasDescendant(withText("Draft"))))
    }

    fun doesNotHavePost(title: String, position: Int) {
        onView(withRecyclerView(R.id.post_list).atPosition(position))
                .check(matches(not(hasDescendant(withText(title)))))
    }

    fun openPost(position: Int): PostViewRobot {
        onView(withRecyclerView(R.id.post_list).atPosition(position))
                .perform(click())
        return PostViewRobot()
    }
}

private class PostViewRobot {
    init {
        // Close soft keyboard to make the view pager fully-visible. Otherwise you may get an error when swiping it, like this:
        // espresso.PerformException: Error performing 'fast swipe' on view 'with id: org.ghost.android.debug:id/view_pager'.
        // Caused by: java.lang.RuntimeException: Action will not be performed because the target
        // view does not match one or more of the following constraints:
        //   at least 90 percent of the view's area is displayed to the user.
        closeSoftKeyboard()
    }

    fun doWithPost(func: PostViewRobot.() -> Unit): PostViewRobot {
        return this.apply { func() }
    }

    fun preview() {
        onView(withId(R.id.view_pager))
                .perform(swipeRight())
    }

    fun edit() {
        onView(withId(R.id.view_pager))
                .perform(swipeLeft())
    }

    fun editorTitle(text: String) {
        onView(withId(R.id.post_title_edit))
                .perform(replaceText(text))
        // Close soft keyboard to make the view pager fully-visible. Otherwise you may get an error when swiping it, like this:
        // espresso.PerformException: Error performing 'fast swipe' on view 'with id: org.ghost.android.debug:id/view_pager'.
        // Caused by: java.lang.RuntimeException: Action will not be performed because the target
        // view does not match one or more of the following constraints:
        //   at least 90 percent of the view's area is displayed to the user.
        closeSoftKeyboard()
    }

    fun editorBody(text: String) {
        onView(withId(R.id.post_markdown))
                .perform(replaceText(text))
        // Close soft keyboard to make the view pager fully-visible. Otherwise you may get an error when swiping it, like this:
        // espresso.PerformException: Error performing 'fast swipe' on view 'with id: org.ghost.android.debug:id/view_pager'.
        // Caused by: java.lang.RuntimeException: Action will not be performed because the target
        // view does not match one or more of the following constraints:
        //   at least 90 percent of the view's area is displayed to the user.
        closeSoftKeyboard()
    }

    fun hasPreviewTitle(text: String) {
        onWebView()
                .withElement(findElement(Locator.CSS_SELECTOR, ".post-title"))
                .check(webMatches(getText(), `is`(text)))
    }

    fun hasPreviewBody(text: String) {
        onWebView()
                .withElement(findElement(Locator.CSS_SELECTOR, ".post-content"))
                .check(webMatches(getText(), `is`(text)))
    }

    fun hasEditorTitle(text: String) {
        onView(withId(R.id.post_title_edit))
                .check(matches(withText(text)))
    }

    fun hasEditorBody(text: String) {
        onView(withId(R.id.post_markdown))
                .check(matches(withText(text)))
    }

    fun deleteDraft(func: PostListRobot.() -> Unit): PostListRobot {
        // scroll up to make the Toolbar and menu visible
        onView(withId(R.id.view_pager))
                .perform(swipeDown())
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText("Delete draft"))
                .perform(click())
        onView(withText("Delete"))
                .inRoot(isDialog())
                .perform(click())
        // deleting takes us back to the post list
        return PostListRobot().apply { func() }
    }

    fun backToPostList(func: PostListRobot.() -> Unit): PostListRobot {
        closeSoftKeyboard()    // to avoid Back press from being wasted in closing the keyboard
        pressBack()
        return PostListRobot().apply { func() }
    }
}
