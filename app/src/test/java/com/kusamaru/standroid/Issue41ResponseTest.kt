package com.kusamaru.standroid

import com.kusamaru.standroid.nicoapi.login.findLoginCookie
import com.kusamaru.standroid.nicoapi.nicolive.NicoLiveProgram
import com.kusamaru.standroid.nicoapi.nicolive.parseCurrentProgramList
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoWatchResponse
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Synthetic contracts, not captured service responses. No credentials/network required. */
class Issue41ResponseTest {
    // RFC822 offset for the existing JVM SimpleDateFormat; Android date rendering is a device check.
    private fun v4(): JSONObject = JSONObject("""
        {"responseType":"watch", "client":{"watchId":"sm9","watchTrackId":"track"},
         "video":{"id":"sm9","title":"test","description":"description","registeredAt":"2007-03-06T00:33:00+0900",
          "duration":60,"thumbnail":{"normal":"https://example.com/thumb"},"isLikedByViewer":true,
          "count":{"view":1,"comment":2,"mylist":3,"like":4}},
         "tags":{"items":[{"name":"tag","isLocked":true}]},
         "media":{"accessRightKey":"key","encryption":null,
          "contents":{"videos":[{"id":"video-low","isAvailable":true}],"audios":[{"id":"audio","isAvailable":true}]}},
         "payment":{"billingType":"free"}, "viewer":null,
         "comment":{"nvComment":{"server":"https://example.com/comment","params":{},"threadKey":"thread"}},
         "lazy":{"authKey":"lazy-key"}, "metadata":{}}
    """)

    private fun wrap(data: JSONObject): JSONObject = JSONObject().put("\$watchV4", JSONObject().put("data", data))

    private fun lazy(): JSONObject = JSONObject("""
        {"owner":{"type":"user","visibility":"visible","id":123,"nickname":"uploader",
          "icon":{"url":"https://example.com/icon"}},"series":null}
    """)

    private fun reject(block: () -> Unit) {
        try {
            block()
            fail("An unsupported/incomplete response must not reach consumers or overwrite a cache")
        } catch (_: Exception) {
            // Assertions are Errors, so a successful block still fails this test.
        }
    }

    @Test fun legacyResponseIsUnchangedIncludingDmcAndSeries() {
        val legacy = JSONObject("""{"video":{"id":"sm9"},"media":{"delivery":{"movie":{}}},"series":{"id":1}}""")
        val before = legacy.toString()
        assertSame(legacy, NicoVideoWatchResponse.normalize(legacy))
        assertEquals(before, legacy.toString())
    }

    @Test fun v4GeneralVideoWorksWithExistingReadersWithoutMutatingInput() = runBlocking {
        val source = wrap(v4())
        val extra = lazy()
        val before = source.toString()
        val extraBefore = extra.toString()
        val result = NicoVideoWatchResponse.normalize(source, extra)
        val reader = NicoVideoHTML()
        val video = reader.createNicoVideoData(result)
        assertEquals("sm9", video.videoId)
        assertEquals("uploader", video.uploaderName)
        assertEquals("https://example.com/thumb", video.thum)
        assertEquals(60L, video.duration)
        assertEquals("123", reader.parseUserData(result)!!.userId)
        assertTrue(reader.isDomandOnly(result))
        assertFalse(reader.isEncryption(result.toString()))
        assertFalse(reader.isLiked(result)) // guest
        assertNull(reader.getSeriesData(result))
        assertEquals("video-low", result.getJSONObject("media").getJSONObject("domand").getJSONArray("videos").getJSONObject(0).getString("id"))
        assertFalse(reader.parseTagDataList(result).single().hasNicoPedia)
        assertFalse(result.getJSONObject("tag").getJSONArray("items").getJSONObject(0).has("isNicodicArticleExists"))
        assertEquals(before, source.toString())
        assertEquals(extraBefore, extra.toString())
    }

    @Test fun viewerLikeAndPremiumArePreservedOnlyWhenProvided() {
        val data = v4().put("viewer", JSONObject("""{"id":456,"isPremium":true}"""))
        val result = NicoVideoWatchResponse.normalize(wrap(data), lazy())
        assertTrue(NicoVideoHTML().isLiked(result))
        assertTrue(result.getJSONObject("viewer").getBoolean("isPremium"))
        data.getJSONObject("viewer").remove("isPremium")
        reject { NicoVideoWatchResponse.normalize(wrap(data), lazy()) }
    }

    @Test fun isoTimestampIsPreservedWithoutDateOrTimezoneConversion() {
        val data = v4()
        data.getJSONObject("video").put("registeredAt", "2007-03-06T00:33:00+09:00")
        val result = NicoVideoWatchResponse.normalize(wrap(data), lazy())
        assertEquals("2007-03-06T00:33:00+09:00", result.getJSONObject("video").getString("registeredAt"))
    }

    @Test fun paidVideoCannotBecomeFreeThroughConversion() {
        val data = v4()
        data.getJSONObject("payment").put("billingType", "paid")
        val result = NicoVideoWatchResponse.normalize(wrap(data), lazy())
        assertTrue(NicoVideoHTML().isEncryption(result.toString()))
    }

    @Test fun explicitAbsentOwnerIsDifferentFromMissingLazyData() {
        val extra = lazy().put("owner", JSONObject.NULL)
        assertNull(NicoVideoHTML().parseUserData(NicoVideoWatchResponse.normalize(wrap(v4()), extra)))
        extra.remove("owner")
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), extra) }
        reject { NicoVideoWatchResponse.normalize(wrap(v4())) }
    }

    @Test fun unsupportedSeriesChannelAndVisibilityFailRatherThanFabricatingData() {
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), lazy().put("series", JSONObject().put("id", 1))) }
        val extra = lazy()
        extra.getJSONObject("owner").put("type", "channel")
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), extra) }
        extra.getJSONObject("owner").put("type", "user").put("visibility", "unknown")
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), extra) }
    }

    // Shape observed from the v4 lazy API for a user series (values synthetic).
    // RFC822 offset only because the JVM SimpleDateFormat rejects "+09:00"; Android is a device check.
    private fun seriesItem(id: String) = """
        {"type":"essential","id":"$id","title":"title $id","registeredAt":"2020-08-25T07:33:02+0900",
         "count":{"view":10,"comment":2,"mylist":3,"like":4},"duration":685,
         "thumbnail":{"url":"https://example.com/s","largeUrl":"https://example.com/l"},
         "owner":{"type":"user","id":"1","name":"uploader"}}
    """

    private fun series(prev: String? = seriesItem("sm1"), next: String? = seriesItem("sm3")): JSONObject = JSONObject("""
        {"id":157715,"title":"series","description":"d","thumbnailUrl":"https://example.com/series",
         "video":{"prev":${prev ?: "null"},"next":${next ?: "null"},"first":${seriesItem("sm1")}}}
    """)

    @Test fun v4SeriesWorksWithExistingSeriesReaders() = runBlocking {
        val source = lazy().put("series", series())
        val before = source.toString()
        val result = NicoVideoWatchResponse.normalize(wrap(v4()), source)
        val reader = NicoVideoHTML()
        val data = reader.getSeriesData(result)!!
        assertEquals("157715", data.seriesId)
        assertEquals("series", data.title)
        val html = reader.getSeriesHTMLData(result)!!
        assertEquals("sm1", html.prevVideoData!!.videoId)
        assertEquals("sm3", html.nextVideoData!!.videoId)
        assertEquals("sm1", html.firstVideoData!!.videoId)
        assertEquals("10", html.nextVideoData!!.viewCount)
        assertEquals(before, source.toString())
    }

    @Test fun v4SeriesEndsAreNullButIncompleteSeriesStillFails() = runBlocking {
        val last = NicoVideoWatchResponse.normalize(wrap(v4()), lazy().put("series", series(next = null)))
        assertNull(NicoVideoHTML().getSeriesHTMLData(last)!!.nextVideoData)
        val noTitle = series().apply { remove("title") }
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), lazy().put("series", noTitle)) }
        val noVideo = series().apply { remove("video") }
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), lazy().put("series", noVideo)) }
        val badItem = series().apply { getJSONObject("video").getJSONObject("next").getJSONObject("count").remove("view") }
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), lazy().put("series", badItem)) }
        reject { NicoVideoWatchResponse.normalize(wrap(v4()), lazy().put("series", "not-an-object")) }
    }

    @Test fun incompleteOrEncryptedV4DoesNotReachConsumers() {
        val data = v4()
        data.getJSONObject("comment").getJSONObject("nvComment").remove("threadKey")
        reject { NicoVideoWatchResponse.normalize(wrap(data), lazy()) }
        val encrypted = v4()
        encrypted.getJSONObject("media").put("encryption", JSONObject().put("type", "drm"))
        reject { NicoVideoWatchResponse.normalize(wrap(encrypted), lazy()) }
    }

    @Test fun serviceErrorAndEmptyBodyAreNotSuccess() {
        reject { NicoVideoWatchResponse.unwrap(null) }
        reject { NicoVideoWatchResponse.unwrap("""{"meta":{"status":403},"data":{"response":{}}}""") }
        reject { NicoVideoWatchResponse.normalize(wrap(v4().put("error", JSONObject())), lazy()) }
    }

    @Test fun errorMessageIncludesReasonOnlyWhenReadable() {
        val reason = """{"meta":{"status":400},"data":{"response":{"reason":"nvpcClientVersion is missing or does not match."}}}"""
        assertEquals("動画情報の取得に失敗しました (400): nvpcClientVersion is missing or does not match.", NicoVideoWatchResponse.errorMessage(400, reason))
        val plain = "動画情報の取得に失敗しました (400)"
        assertEquals(plain, NicoVideoWatchResponse.errorMessage(400, null))
        assertEquals(plain, NicoVideoWatchResponse.errorMessage(400, "<html></html>"))
        assertEquals(plain, NicoVideoWatchResponse.errorMessage(400, """{"data":{"response":{}}}"""))
        // meta.status が失敗の本文でも reason を添える
        try {
            NicoVideoWatchResponse.unwrap(reason)
            fail("expected failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.endsWith("does not match."))
        }
    }

    private fun live(section: String, items: String = "[]", hasError: Boolean = false): JSONObject =
        JSONObject("""{"props":{"view":{"$section":{"hasError":$hasError,"items":$items}}}}""")

    private val seed = """
        {"type":"seed","value":{"nicoliveProgramId":"lv123","title":"live","listingThumbnail":"https://example.com/live",
         "providerType":"official","socialGroup":{"name":"group"},"beginTime":1700000000,"endTime":1700003600,"status":"RELEASED"}}
    """

    @Test fun allFourLiveSectionsAcceptExplicitEmptyLists() {
        val sections = mapOf(
            NicoLiveProgram.FAVOURITE_PROGRAM to "favoriteProgramListSectionState",
            NicoLiveProgram.RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM to "organizationProgramListSectionState",
            NicoLiveProgram.POPULAR_BEFORE_OPEN_BROADCAST_STATUS_PROGRAM to "popularBeforeOpenBroadcastStatusProgramListSectionState",
            NicoLiveProgram.ROOKIE_PROGRAM to "rookieProgramListSectionState",
        )
        for ((category, section) in sections) assertTrue(parseCurrentProgramList(live(section), category).isEmpty())
    }

    @Test fun liveSeedUsesMillisecondsAndPreservesOfficialStatus() {
        val program = parseCurrentProgramList(live("organizationProgramListSectionState", "[$seed]"), NicoLiveProgram.RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM).single()
        assertEquals("lv123", program.programId)
        assertEquals("1700000000000", program.beginAt)
        assertEquals("1700003600000", program.endAt)
        assertEquals("group", program.communityName)
        assertEquals("RELEASED", program.lifeCycle)
        assertTrue(program.isOfficial)
    }

    @Test fun rookieSupplierAndOnAirStatusArePreserved() {
        val item = JSONObject(seed)
        item.getJSONObject("value").remove("socialGroup")
        item.getJSONObject("value").put("supplier", JSONObject().put("name", "rookie"))
            .put("providerType", "community").put("status", "ON_AIR")
        val program = parseCurrentProgramList(live("rookieProgramListSectionState", "[$item]"), NicoLiveProgram.ROOKIE_PROGRAM).single()
        assertEquals("rookie", program.communityName)
        assertEquals("ON_AIR", program.lifeCycle)
        assertFalse(program.isOfficial)
    }

    @Test fun liveMissingSectionErrorAndUnknownItemAreNotEmptySuccess() {
        reject { parseCurrentProgramList(live("favoriteProgramListSectionState", hasError = true), NicoLiveProgram.FAVOURITE_PROGRAM) }
        reject { parseCurrentProgramList(JSONObject("""{"props":{"view":{}}}"""), NicoLiveProgram.FAVOURITE_PROGRAM) }
        reject { parseCurrentProgramList(live("favoriteProgramListSectionState", "[{\"type\":\"unknown\"}]"), NicoLiveProgram.FAVOURITE_PROGRAM) }
    }

    @Test fun liveHtmlSupportsBothSelectorsAndEntityDecoding() = runBlocking {
        val parser = NicoLiveProgram()
        val current = live("favoriteProgramListSectionState").toString().replace("\"", "&quot;")
        assertTrue(parser.parseJSON("<script id='DAT-csr-data' data-value='$current'></script>", NicoLiveProgram.FAVOURITE_PROGRAM).isEmpty())
        assertTrue(parser.parseJSON("<script id='embedded-data' data-props='{\"view\":{\"favoriteProgramListState\":{\"programList\":[]}}}'></script>", NicoLiveProgram.FAVOURITE_PROGRAM).isEmpty())
        reject { runBlocking { parser.parseJSON("<html></html>", NicoLiveProgram.FAVOURITE_PROGRAM) } }
    }

    @Test fun loginCookiesAreSelectedByNameNotPosition() {
        val url = "https://account.nicovideo.jp/login".toHttpUrl()
        val headers = Headers.Builder()
            .add("set-cookie", "user_session_secure=wrong; Path=/")
            .add("set-cookie", "nicosid=other; Path=/")
            .add("set-cookie", "user_session=correct; Path=/; HttpOnly")
            .add("set-cookie", "mfa_trusted_device_token=trust; Path=/")
            .build()
        assertEquals("correct", findLoginCookie(headers, url, "user_session")!!.value)
        assertEquals("trust", findLoginCookie(headers, url, "mfa_trusted_device_token")!!.value)
        val twoCookies = Headers.Builder().add("Set-Cookie", "nicosid=other").add("Set-Cookie", "user_session=correct").build()
        assertEquals("correct", findLoginCookie(twoCookies, url, "user_session")!!.value)
    }

    @Test fun missingDeletedAndExpiredCookiesAreNotLoginSuccess() {
        val url = "https://account.nicovideo.jp/login".toHttpUrl()
        for (cookie in listOf("user_session_secure=wrong", "user_session=", "user_session=deleted", "user_session=expired; Max-Age=0")) {
            assertNull(findLoginCookie(Headers.Builder().add("Set-Cookie", cookie).build(), url, "user_session"))
        }
    }
}
