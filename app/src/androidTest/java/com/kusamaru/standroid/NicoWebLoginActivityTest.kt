package com.kusamaru.standroid

import android.app.Activity
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.kusamaru.standroid.activity.NicoWebLoginActivity
import com.kusamaru.standroid.nicoapi.login.NicoWebLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Opens/cancels the real window only: no credentials, OTP, or captcha interaction. */
@RunWith(AndroidJUnit4::class)
class NicoWebLoginActivityTest {
    @Test fun closingWindowReturnsNullInsteadOfHanging() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(NicoWebLoginActivity::class.java.name, null, false)
        var activity: Activity? = null
        val login = async(Dispatchers.Default) {
            NicoWebLogin.secureNicoLogin(InstrumentationRegistry.getTargetContext(), force = true)
        }
        try {
            activity = monitor.waitForActivityWithTimeout(15_000)
            assertNotNull("Login window must open", activity)
            assertFalse("Opening the window is not login completion", login.isCompleted)
            instrumentation.runOnMainSync { activity!!.finish() }
            assertNull(withTimeout(10_000) { login.await() })
        } finally {
            login.cancelAndJoin()
            instrumentation.runOnMainSync { activity?.finish() }
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test fun cancellingCallerClosesWindow() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(NicoWebLoginActivity::class.java.name, null, false)
        var activity: Activity? = null
        val login = async(Dispatchers.Default) {
            NicoWebLogin.secureNicoLogin(InstrumentationRegistry.getTargetContext(), force = true)
        }
        try {
            activity = monitor.waitForActivityWithTimeout(15_000)
            assertNotNull("Login window must open", activity)
            login.cancelAndJoin()
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertTrue("Cancelled caller must not leave a detached login window", activity!!.isFinishing)
            }
        } finally {
            login.cancelAndJoin()
            instrumentation.runOnMainSync { activity?.finish() }
            instrumentation.removeMonitor(monitor)
        }
    }
}
