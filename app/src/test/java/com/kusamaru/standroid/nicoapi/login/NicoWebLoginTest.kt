package com.kusamaru.standroid.nicoapi.login

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class NicoWebLoginTest {
    @Test fun automaticConfirmationAcceptsServicePagesButNeverPasswordMfaOrChallengePages() {
        for (url in listOf("https://www.nicovideo.jp/", "https://nicovideo.jp/", "https://sp.nicovideo.jp/", "https://live.nicovideo.jp/")) {
            assertTrue(isWebLoginReturnUrl(url))
        }
        for (url in listOf(null, "https://account.nicovideo.jp/spa/login/index.html",
            "https://account.nicovideo.jp/spa/mfa/index.html", "https://challenges.cloudflare.com/",
            "https://nicovideo.jp.evil.example/", "http://www.nicovideo.jp/")) {
            assertFalse(isWebLoginReturnUrl(url))
        }
    }

    @Test fun waitsForWindowAndFinalAuthenticationNotJustWindowOpening() = runBlocking {
        val request = WebLoginRequest()
        val waiting = async { request.awaitStarted(); request.await() }
        yield()
        assertFalse(waiting.isCompleted)
        request.markStarted()
        yield()
        assertFalse(waiting.isCompleted) // MFA can still be in progress.
        assertTrue(request.complete(true))
        assertTrue(waiting.await())
    }

    @Test fun cancelledWindowReturnsFailure() = runBlocking {
        val request = WebLoginRequest()
        request.markStarted()
        assertTrue(request.complete(false))
        assertFalse(request.await())
    }

    @Test fun callerCancellationRejectsLateWindowAndSuccess() = runBlocking {
        val request = WebLoginRequest()
        val waiting = async { request.awaitStarted(); request.await() }
        yield()
        request.cancel()
        assertFalse(request.isActive)
        assertFalse(request.markStarted())
        assertFalse(request.complete(true))
        try {
            waiting.await()
            fail("Cancelled login must not resume")
        } catch (_: CancellationException) {
        }
    }

    @Test fun duplicateResultDoesNotChangeSuccessfulLogin() = runBlocking {
        val request = WebLoginRequest()
        assertTrue(request.complete(true))
        assertFalse(request.complete(false))
        assertTrue(request.await())
    }

    @Test fun cookieHeaderUsesExactNameAndPreservesEqualsInValue() {
        assertEquals("value=part", webLoginSession("nicosid=other; user_session_secure=wrong; user_session=value=part"))
        assertEquals("valid", webLoginSession("user_session=deleted; user_session=valid"))
        assertEquals("valid", webLoginSession("unrelated=日本語; user_session=valid"))
    }

    @Test fun missingDeletedEmptyAndMalformedCookiesAreNotSessions() {
        for (header in listOf(null, "", "user_session_secure=wrong", "user_session=deleted", "user_session=", "user_session=bad\r\nInjected=1")) {
            assertNull(webLoginSession(header))
        }
    }

    @Test fun requiresServiceSuccessAndPositiveAuthenticatedUserId() {
        assertTrue(isAuthenticatedUserResponse("""{"meta":{"status":200},"data":{"user":{"id":123}}}"""))
        assertTrue(isAuthenticatedUserResponse("""{"meta":{"status":200},"data":{"user":{"id":"123"}}}"""))
        for (body in listOf(null, "", "<html>Error</html>", "{}",
            """{"meta":{"status":401},"data":{"user":{"id":123}}}""",
            """{"meta":{"status":200},"data":{"user":{"id":0}}}""",
            """{"meta":{"status":200},"data":{"user":{"id":"unknown"}}}""",
            """{"meta":{"status":200},"data":{"user":null}}""")) {
            assertFalse(isAuthenticatedUserResponse(body))
        }
    }

    @Test fun allowsOfficialHttpsAndTurnstileOnly() {
        for (url in listOf("https://account.nicovideo.jp/spa/login/index.html", "https://www.nicovideo.jp/", "https://live2.nicovideo.jp/", "https://challenges.cloudflare.com/")) {
            assertTrue(isWebLoginUrl(url))
        }
        for (url in listOf(null, "http://account.nicovideo.jp/", "https://nicovideo.jp.evil.example/",
            "https://evilnicovideo.jp/", "https://user:pass@account.nicovideo.jp/",
            "https://account.nicovideo.jp:8443/", "https://accounts.google.com/",
            "file:///tmp/login.html", "intent://login", "javascript:alert(1)")) {
            assertFalse(isWebLoginUrl(url))
        }
    }
}
