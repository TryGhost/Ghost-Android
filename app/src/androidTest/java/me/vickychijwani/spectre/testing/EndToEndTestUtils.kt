package me.vickychijwani.spectre.testing

import android.app.Activity
import android.content.*
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.intent.matcher.IntentMatchers
import org.hamcrest.Matcher
import kotlin.reflect.KClass


fun <T : Activity> hasActivity(cls: KClass<T>): Matcher<Intent> {
    return IntentMatchers.hasComponent(ComponentName(InstrumentationRegistry.getTargetContext(), cls.java.name))
}
