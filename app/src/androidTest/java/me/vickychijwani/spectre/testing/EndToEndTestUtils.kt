package me.vickychijwani.spectre.testing

import android.app.Activity
import android.content.*
import android.support.annotation.*
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.view.View
import org.hamcrest.*
import org.hamcrest.Matchers.anyOf
import kotlin.reflect.KClass


fun <T : Activity> hasActivity(cls: KClass<T>): Matcher<Intent> {
    return IntentMatchers.hasComponent(ComponentName(getTargetContext(), cls.java.name))
}

/**
 * Matches a menu item with the given id OR one with text matching ANY OF the given strings
 */
fun withMenuIdOrAnyOfText(@IdRes id: Int, @StringRes vararg menuStrings: Int): Matcher<View> {
    return try {
        val matcher = withId(id)
        onView(matcher).check(matches(isDisplayed()))
        matcher
    } catch (NoMatchingViewException: Exception) {
        val targetContext = getTargetContext()
        openActionBarOverflowOrOptionsMenu(targetContext)
        val textMatchers = menuStrings.map { Matchers.equalTo(targetContext.getString(it)) }
        withText(anyOf(textMatchers))
    }
}
