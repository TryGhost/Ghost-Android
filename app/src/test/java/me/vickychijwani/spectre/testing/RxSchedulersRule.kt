package me.vickychijwani.spectre.testing

import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.internal.schedulers.TrampolineScheduler
import io.reactivex.plugins.RxJavaPlugins
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.Callable

class RxSchedulersRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val scheduler = TrampolineScheduler.instance()
                val schedulerFn = { _: Callable<Scheduler> -> scheduler }
                RxJavaPlugins.reset()
                RxJavaPlugins.setInitIoSchedulerHandler(schedulerFn)
                RxJavaPlugins.setInitNewThreadSchedulerHandler(schedulerFn)
                RxAndroidPlugins.reset()
                RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerFn)

                try {
                    base.evaluate()
                } finally {
                    RxJavaPlugins.reset()
                    RxAndroidPlugins.reset()
                }
            }
        }
    }

}
