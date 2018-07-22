package me.vickychijwani.spectre.view

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.*
import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.web.assertion.WebViewAssertions.webMatches
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.support.test.espresso.web.webdriver.DriverAtoms.getText
import android.support.test.espresso.web.webdriver.Locator
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.view.Gravity
import me.vickychijwani.spectre.R
import me.vickychijwani.spectre.model.entity.Post
import me.vickychijwani.spectre.testing.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.anyOf
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) @LargeTest
class PostTest {

    @Rule @JvmField val mActivityRule = IntentsTestRule(LoginActivity::class.java)
    @Rule @JvmField val mDeleteDefaultPostsRule = DeleteDefaultPostsRule(TEST_BLOG_WITH_PROTOCOL)
    @Rule @JvmField val mPrefsRule = ClearPreferencesRule()
    @Rule @JvmField val mOkHttpIdlingResourceRule = OkHttpIdlingResourceRule()

    private lateinit var mSyncPendingIdlingResource: IdlingResource

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

        // consider the app idle when post status != "waiting to go online", i.e., when sync is done
        mSyncPendingIdlingResource = ViewDoesntMatchTextIdlingResource(mActivityRule.activity,
                R.id.post_status_text, getTargetContext().getString(R.string.status_offline_changes))
        IdlingRegistry.getInstance().register(mSyncPendingIdlingResource)
    }

    @After
    fun teardown() {
        IdlingRegistry.getInstance().unregister(mSyncPendingIdlingResource)
    }

    @Test
    fun createAndDeleteDraft() {
        newPost {
        }.backToPostList {
            hasDraft(Post.DEFAULT_TITLE, position = 0)
        }.openPost(position = 0).doWithPost {
            hasEditorTitle("")
            preview()
            hasPreviewTitle(Post.DEFAULT_TITLE)
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
        }
    }

    // KNOWN BUG
    @Ignore
    @Test
    fun editAndDeleteDraft() {
        newPost {
            editorTitle("Some title")
            editorBody("Some body text")
        }.deleteDraft {
            doesNotHavePost("Some title", position = 0)
        }
    }

    @Test
    fun setTags() {
        newPost {
            openPostSettings()
            tags("tag1", "tag2")
            closePostSettings()
        }.backToPostList {
            // the first tag is always displayed
            hasPost(tagSubstring = "tag1", position = 0)
        }
    }

    @Test
    fun publishAndUnpublish() {
        newPost {
            editorTitle("Publish & unpublish")
            publish()
        }.backToPostList {
            hasPublishedPost("Publish & unpublish", position = 0)
        }.openPost(0).doWithPost {
            unpublish()
        }.backToPostList {
            hasDraft("Publish & unpublish", position = 0)
        }
    }

    @Test
    fun publishThenEditAndRepublish() {
        newPost {
            editorTitle("Title 1")
            publish()
            editorTitle("Title 2")
        }.backToPostList {
            hasPost("Title 2", statusSubstring = "not published", position = 0)
        }.openPost(position = 0).doWithPost {
            publish()
        }.backToPostList {
            hasPublishedPost("Title 2", position = 0)
        }
    }
}

private fun newPost(func: PostViewRobot.() -> Unit): PostViewRobot {
    onView(withId(R.id.new_post_btn))
            .perform(click())
    return PostViewRobot().apply { func() }
}

private class PostListRobot {
    fun hasDraft(title: String, position: Int) {
        hasPost(title, statusSubstring = "Draft", position = position)
    }

    fun hasPublishedPost(title: String, position: Int) {
        hasPost(title, statusSubstring = "Published", position = position)
    }

    fun hasPost(title: String? = null, statusSubstring: String? = null,
                tagSubstring: String? = null, position: Int = 0) {
        title?.let {
            onView(withRecyclerView(R.id.post_list).atPositionOnView(position, R.id.post_title))
                    .check(matches(withText(title)))
        }
        statusSubstring?.let {
            onView(withRecyclerView(R.id.post_list).atPositionOnView(position, R.id.post_status_text))
                    .check(matches(withText(containsString(statusSubstring))))
        }
        tagSubstring?.let {
            onView(withRecyclerView(R.id.post_list).atPositionOnView(position, R.id.post_tags))
                    .check(matches(withText(containsString(tagSubstring))))
        }
    }

    fun doesNotHavePost(title: String, position: Int) {
        try {
            onView(withRecyclerView(R.id.post_list).atPositionOnView(position, R.id.post_title))
                    .check(matches(not(withText(title))))
        } catch (_: NoMatchingViewException) {
            // if the view at position was not found (e.g., in an empty RecyclerView), then
            // the assertion "doesNotHavePost" still holds
        }
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
        // scroll up to make the Toolbar and menu visible
        onView(withId(R.id.view_pager))
                .perform(swipeDown())
    }

    fun doWithPost(func: PostViewRobot.() -> Unit): PostViewRobot {
        return this.apply { func() }
    }

    fun preview() {
        onView(withText("Preview")).perform(click())
    }

    fun edit() {
        onView(withText("Edit")).perform(click())
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

    fun publish() {
        onView(withMenuIdOrAnyOfText(R.id.action_publish, R.string.publish, R.string.update_post))
                .perform(click())
        // "OK" when publishing a new post and "Yes" when updating an existing post
        onView(anyOf(withText("OK"), withText(containsString("Yes"))))
                .inRoot(isDialog())
                .perform(click())
    }

    fun unpublish() {
        onView(withMenuIdOrAnyOfText(R.id.action_unpublish, R.string.unpublish))
                .perform(click())
        onView(withText("OK"))
                .inRoot(isDialog())
                .perform(click())
    }

    fun openPostSettings() {
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open(Gravity.END))
    }

    fun closePostSettings() {
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.close(Gravity.END))
    }

    fun tags(vararg tags: String) {
        onView(withId(R.id.post_tags_edit))
                .perform(replaceText(tags.joinToString(",")))
    }

    fun deleteDraft(func: PostListRobot.() -> Unit): PostListRobot {
        onView(withMenuIdOrAnyOfText(R.id.action_delete, R.string.delete_draft))
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
