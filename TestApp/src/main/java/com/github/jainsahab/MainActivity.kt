package com.github.jainsahab

import androidx.activity.addCallback
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.appcompat.app.AppCompatActivity
import com.github.jainsahab.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException
import java.util.Date

class MainActivity : AppCompatActivity() {

private lateinit var binding:  
        ActivityMainBinding  
        
private var liveUrlInputText =
    ""

// =====================================
// BROWSER HISTORY / BOOKMARKS / MENU IDS
// =====================================

private val browserHistoryPrefsName =
    "gel_browser_history"

private val browserHistoryKey =
    "history_json"

private val browserHistoryMaxItems =
    100

private val browserBookmarksPrefsName =
    "gel_browser_bookmarks"

private val browserBookmarksKey =
    "bookmarks_json"

private val browserBookmarksMaxItems =
    200

private val menuHistoryId =
    91001

private val menuBookmarksId =
    91002

private val menuAddBookmarkId =
    91003

private val menuSavedChannelsId =
    91004

private val menuDetectedStreamsId =
    91005

private val menuCopyDetectedStreamsId =
    91006

private val menuExportM3uId =
    91007

private val menuSharePageId =
    91008

private val menuCopyPageUrlId =
    91009

private val menuFindInPageId =
    91010

private val menuReloadId =
    91011

private val menuStopLoadingId =
    91012

private val menuOpenChromeId =
    91013

private val menuDesktopModeId =
    91014

private val menuMobileModeId =
    91015

private val menuClearBrowserDataId =
    91016

private val menuOpenNewTabId =
    91017
    
private val menuGoForwardId =
    91024

private val menuTabsId =
    91018

private val menuTranslatePageId =
    91019

private val menuAddToHomeScreenId =
    91020

private val menuOpenPlayerId =
    91021
    
private val menuScanChannelCandidatesId =
    91022
    
private val menuAutoScanChannelsId =
    91023
    

private val desktopUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/137.0.0.0 Safari/537.36"

private val mobileUserAgent =
    "Mozilla/5.0 (Linux; Android 13) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/137.0.0.0 Mobile Safari/537.36"

private val systemWebViewUserAgent: String by lazy {

    try {

        WebSettings.getDefaultUserAgent(
            this
        )

    } catch (_: Throwable) {

        mobileUserAgent
    }
}

// =====================================
// RESULTS PANEL STATE
// 0 = collapsed, 1 = normal, 2 = expanded
// =====================================

private var resultsPanelState =
    1

private val resultsPanelCollapsedHeightDp =
    0

private val resultsPanelNormalHeightDp =
    90
    
private var autoScanStopButton: Button? =
    null


// =====================================
// AUTO CHANNEL SCANNER STATE
// =====================================

data class AutoScanCandidate(
    val title: String,
    val href: String
)

data class AutoScanPage(
    val title: String,
    val href: String
)

private var autoScanRunning =
    false

private var autoScanIndex =
    0

private val autoScanCandidates =
    mutableListOf<AutoScanCandidate>()

private val autoScanResults =
    linkedMapOf<String, MutableList<String>>()

private val autoScanKnownBefore =
    mutableSetOf<String>()

private val autoScanMaxCandidates =
    Int.MAX_VALUE

// =====================================
// AUTOMATIC COOKIE CONSENT RECOVERY
// OneTrust / cookie banner safe reload
// =====================================

private val cookieConsentReloadedUrls =
    mutableSetOf<String>()

private var cookieConsentReloadInProgress =
    false

data class BrowserHistoryEntry(
    val title: String,
    val url: String,
    val timestamp: Long
)

private val browserHistory =
    mutableListOf<BrowserHistoryEntry>()

data class BrowserBookmarkEntry(
    val title: String,
    val url: String,
    val timestamp: Long
)

private val browserBookmarks =
    mutableListOf<BrowserBookmarkEntry>()

// =====================================
// BROWSER TABS — SESSION ONLY
// =====================================

data class BrowserTabEntry(
    val title: String,
    val url: String,
    val timestamp: Long
)

private val browserTabs =
    mutableListOf<BrowserTabEntry>()

private var currentBrowserTabIndex =
    -1
    
 private var protectedFallbackShown =
    false

private var refererRetryDone =
    false

// =====================================
// CHANNEL CANDIDATE STATE
// Used to suppress false protected warning
// =====================================

private var lastChannelCandidateCount =
    0
    
private var popupWebView: WebView? =
    null
    
// =====================================
// WEBVIEW USER INTERACTION GUARD
// =====================================

private var webUserInteracting =
    false
    
// =====================================
// CLEAN STREAM FILTER STATE
// .ts appears only when no playlist exists
// =====================================

private var pagePlaylistFound =
    false

private val pendingTsFallback =
    java.util.concurrent.CopyOnWriteArraySet<String>()

// =====================================
// CLOUDFLARE / HUMAN VERIFY GUARD
// Pauses heavy scanners during browser verification
// =====================================

private var cloudflareChallengeActive =
    false

private var lastCloudflareChallengeTime =
    0L

private val cloudflareChallengeRecheckRunnable =
    object : Runnable {

        override fun run() {

            try {

                if (!cloudflareChallengeActive) {
                    return
                }

                val activeWebView =
                    popupWebView
                        ?: binding.contentMain.webview

                activeWebView.evaluateJavascript(
                    """
(function() {

    try {

        var title =
            String(document.title || "").toLowerCase();

        var body =
            String(
                document.body
                    ? document.body.innerText || ""
                    : ""
            ).toLowerCase();

        var html =
            String(
                document.documentElement
                    ? document.documentElement.outerHTML || ""
                    : ""
            ).toLowerCase();

        var explicitMarker =
            html.indexOf("cf-chl-") >= 0 ||
            html.indexOf("challenge-platform") >= 0 ||
            html.indexOf("cf-turnstile") >= 0 ||
            html.indexOf("challenges.cloudflare.com") >= 0 ||
            html.indexOf("cf_clearance") >= 0;

        var humanChallenge =
            (
                body.indexOf("verify you are human") >= 0 ||
                body.indexOf("checking your browser") >= 0 ||
                body.indexOf("performing security verification") >= 0
            ) &&
            (
                html.indexOf("cloudflare") >= 0 ||
                html.indexOf("turnstile") >= 0 ||
                explicitMarker
            );

        var waitingPage =
            title.indexOf("just a moment") >= 0 &&
            (
                html.indexOf("cloudflare") >= 0 ||
                explicitMarker
            );

        return (
            explicitMarker ||
            humanChallenge ||
            waitingPage
        )
            ? "challenge"
            : "clear";

    } catch(e) {

        return "challenge";
    }
})();
                    """.trimIndent()
                ) { result ->

                    try {

                        val stillChallenge =
                            (
                                result?.contains(
                                    "challenge",
                                    true
                                ) == true
                            )

                        if (stillChallenge) {

                            activeWebView.removeCallbacks(
                                this
                            )

                            activeWebView.postDelayed(
                                this,
                                2000L
                            )

                        } else {

                            setCloudflareChallengeMode(
                                false
                            )

                            activeWebView.postDelayed(
                                {

                                    try {

                                        if (
                                            !webUserInteracting &&
                                            !cloudflareChallengeActive
                                        ) {

                                            runDeepMediaScan(
                                                activeWebView
                                            )
                                        }

                                    } catch (_: Throwable) {}
                                },
                                500L
                            )
                        }

                    } catch (_: Throwable) {

                        activeWebView.postDelayed(
                            this,
                            2000L
                        )
                    }
                }

            } catch (_: Throwable) {

                try {

                    binding.contentMain.webview.postDelayed(
                        this,
                        2000L
                    )

                } catch (_: Throwable) {}
            }
        }
    }
    
// =====================================
// CLEAR WEB INTERACTION FLAG
// =====================================

private val clearWebInteractionRunnable =
    Runnable {

        webUserInteracting =
            false
    }

// =====================================  
// UNIQUE URL CACHE  
// =====================================  

private val detectedStreams =
    java.util.concurrent.CopyOnWriteArraySet<String>()

private val detectedVideos =
    java.util.concurrent.CopyOnWriteArraySet<String>()

private val detectedImages =
    java.util.concurrent.CopyOnWriteArraySet<String>()

private val detectedAudio =
    java.util.concurrent.CopyOnWriteArraySet<String>()

private val detectedMasterStreams =
    java.util.concurrent.CopyOnWriteArraySet<String>()

private val detectedChannels =
    java.util.concurrent.ConcurrentHashMap<String, String>()

private val streamHeaders =
    java.util.concurrent.ConcurrentHashMap<
        String,
        MutableMap<String, String>
    >()
    
// =====================================
// DETECTED M3U LISTS
// Country/channel list files, not HLS streams
// =====================================

private val detectedM3uLists =
    java.util.concurrent.CopyOnWriteArraySet<String>()
  
private var lastSelectedUrl =
    ""
    
private val hlsVerdicts =
    mutableMapOf<String, String>()
    
// =====================================
// LIVE / DASH STATE
// =====================================

private var bestLiveUrl =
    ""

private var bestLiveScore =
    0

private val dashVideoMap =
    mutableMapOf<String, String>()

private val dashAudioMap =
    mutableMapOf<String, String>()
    
// =====================================
// STREAM META
// =====================================

private val streamResolution =
    linkedMapOf<String, String>()

private val streamBandwidth =
    linkedMapOf<String, String>()

private val streamCodec =
    linkedMapOf<String, String>()
    
private var bestVideoItag = 0
private var bestAudioItag = 0

private var liveLocked = false
private var lockedStreamId = ""

// =====================================
// SAVED CHANNELS / FAVORITES
// =====================================

data class SavedChannel(
    val name: String,
    val url: String,
    val logo: String,
    val group: String
)

private val savedChannels =
    mutableListOf<SavedChannel>()

private val savedChannelsPrefsName =
    "gel_saved_channels"

private val savedChannelsKey =
    "channels_json"

// =====================================
// LOAD SAVED CHANNELS
// =====================================

private fun loadSavedChannels() {

    try {

        savedChannels.clear()

        val prefs =
            getSharedPreferences(
                savedChannelsPrefsName,
                MODE_PRIVATE
            )

        val raw =
            prefs.getString(
                savedChannelsKey,
                "[]"
            )
                ?.trim()
                ?: "[]"

        if (raw.isBlank()) {
            return
        }

        val array =
            org.json.JSONArray(
                raw
            )

        for (i in 0 until array.length()) {

            try {

                val obj =
                    array.optJSONObject(
                        i
                    ) ?: continue

                val name =
                    obj.optString(
                        "name",
                        ""
                    ).trim()

                val url =
                    obj.optString(
                        "url",
                        ""
                    ).trim()

                val logo =
                    obj.optString(
                        "logo",
                        "noimage.png"
                    ).trim()
                        .ifBlank {
                            "noimage.png"
                        }

                val group =
                    obj.optString(
                        "group",
                        "Live Streams"
                    ).trim()
                        .ifBlank {
                            "Live Streams"
                        }

                if (
                    url.isBlank() ||
                    !isExportableStream(url)
                ) {
                    continue
                }

                val exists =
                    savedChannels.any { channel ->

                        channel.url == url
                    }

                if (exists) {
                    continue
                }

                savedChannels.add(
                    SavedChannel(
                        name =
                            name.ifBlank {
                                buildChannelName(url)
                            },
                        url =
                            url,
                        logo =
                            logo,
                        group =
                            group
                    )
                )

            } catch (_: Throwable) {}
        }

    } catch (t: Throwable) {

        Log.e(
            "SAVED_CHANNELS",
            "load failed",
            t
        )

        savedChannels.clear()
    }
}

// =====================================
// SAVE SAVED CHANNELS
// =====================================

private fun persistSavedChannels() {

    try {

        val array =
            org.json.JSONArray()

        savedChannels.forEach { channel ->

            try {

                val obj =
                    org.json.JSONObject()

                obj.put(
                    "name",
                    channel.name
                )

                obj.put(
                    "url",
                    channel.url
                )

                obj.put(
                    "logo",
                    channel.logo
                )

                obj.put(
                    "group",
                    channel.group
                )

                array.put(
                    obj
                )

            } catch (_: Throwable) {}
        }

        val ok =
            getSharedPreferences(
                savedChannelsPrefsName,
                MODE_PRIVATE
            )
                .edit()
                .putString(
                    savedChannelsKey,
                    array.toString()
                )
                .commit()

        if (!ok) {

            Log.e(
                "SAVED_CHANNELS",
                "persist commit failed"
            )
        }

    } catch (t: Throwable) {

        Log.e(
            "SAVED_CHANNELS",
            "persist failed",
            t
        )
    }
}

// =====================================
// NORMALIZE SAVED CHANNEL URL
// Never save temporary googlevideo URLs
// =====================================

private fun normalizeSavedChannelUrl(
    rawUrl: String
): String {

    return try {

        val cleanUrl =
            rawUrl
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim()

        val lower =
            cleanUrl.lowercase()

        if (
            lower.contains("googlevideo.com") ||
            lower.contains("videoplayback")
        ) {

            val uri =
                Uri.parse(cleanUrl)

            val id =
                uri.getQueryParameter("id")
                    ?.substringBefore(".")
                    ?.trim()
                    .orEmpty()

            if (
                id.matches(
                    Regex("^[A-Za-z0-9_-]{11}$")
                )
            ) {

                return "https://www.youtube.com/watch?v=$id"
            }

            return ""
        }

        cleanUrl

    } catch (_: Throwable) {

        rawUrl.trim()
    }
}

// =====================================
// ADD SAVED CHANNEL
// =====================================

private fun addSavedChannel(
    url: String
) {

    try {

        // =====================================
        // LOAD EXISTING SAVED CHANNELS FIRST
        // CRITICAL: prevents overwrite of old saved list
        // =====================================

        loadSavedChannels()

        val cleanUrl =
            normalizeSavedChannelUrl(
                url
            )

        if (
            cleanUrl.isBlank() ||
            !isExportableStream(cleanUrl)
        ) {
            return
        }

        val exists =
            savedChannels.any { channel ->

                channel.url == cleanUrl
            }

        if (exists) {

            Toast.makeText(
                this,
                "Channel already saved",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val channelName =
            buildChannelName(
                cleanUrl
            )

        val logoUrl =
            buildLogoUrl(
                cleanUrl
            )

        val lower =
            cleanUrl.lowercase()

        val group =
            when {

                lower.contains(".mp3") ||
                    lower.contains(".m4a") ||
                    lower.contains(".aac") ||
                    lower.contains(".opus") ||
                    lower.contains(".wav") ||
                    lower.contains(".ogg") ||
                    lower.contains(".flac") ->
                    "Audio"

                lower.contains(".mp4") ||
                    lower.contains(".webm") ||
                    lower.contains(".mkv") ||
                    lower.contains(".mov") ||
                    lower.contains(".avi") ||
                    lower.contains(".3gp") ->
                    "Static Videos"

                else ->
                    "Live Streams"
            }

        savedChannels.add(
            SavedChannel(
                name = channelName,
                url = cleanUrl,
                logo = logoUrl,
                group = group
            )
        )

        persistSavedChannels()

        Toast.makeText(
            this,
            "Channel saved",
            Toast.LENGTH_SHORT
        ).show()

    } catch (_: Throwable) {

        Toast.makeText(
            this,
            "Save failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHOW SAVED CHANNELS DIALOG
// Full selectable manager: OPEN / COPY / SHARE / DELETE
// =====================================

private fun showSavedChannelsDialog() {

    try {

        loadSavedChannels()

        if (savedChannels.isEmpty()) {

            Toast.makeText(
                this,
                "No saved channels",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val labels =
            savedChannels.map { channel ->

                "${channel.name}\n${channel.url}"
            }

        val urls =
            savedChannels.map { channel ->

                channel.url
            }

        showSelectableUrlsDialog(
            title = "Saved Channels",
            labels = labels,
            urls = urls,
            allowSave = false,
            allowDelete = true,
            onDeleteSelected = { selectedUrls ->

                try {

                    loadSavedChannels()

                    savedChannels.removeAll { channel ->

                        selectedUrls.contains(
                            channel.url
                        )
                    }

                    persistSavedChannels()

                    Toast.makeText(
                        this,
                        "Deleted ${selectedUrls.size} channel(s)",
                        Toast.LENGTH_SHORT
                    ).show()

                    showSavedChannelsDialog()

                } catch (_: Throwable) {}
            }
        )

    } catch (t: Throwable) {

        Log.e(
            "SAVED_CHANNELS_DIALOG",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Saved channels failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// STREAM INFO SNAPSHOTS
// =====================================

data class StreamInfoSnapshot(
    val url: String,
    val badge: String,
    val quality: String,
    val cdn: String,
    val security: String,
    val segment: String,
    val forensic: String,
    val youtubeWatch: String,
    val dashVideo: String,
    val dashAudio: String,
    val dashVideoItag: String,
    val dashAudioItag: String,
    val bestStream: String,
    val bestLive: String
)

private val streamInfoSnapshots =
    mutableMapOf<String, StreamInfoSnapshot>()
    
// =====================================
// LIVE HEARTBEAT MAP
// =====================================

private val liveHeartbeatMap =
    mutableMapOf<String, Long>()
    
// =====================================
// STREAM HIT COUNTER
// =====================================

private val streamHitCounter =
    mutableMapOf<String, Int>()
    
// =====================================
// UI REFRESH DEBOUNCE
// =====================================

private val uiHandler =
    android.os.Handler(
        android.os.Looper.getMainLooper()
    )

private val refreshRunnable =
    Runnable {

        try {

            showAllMedia()

        } catch (_: Throwable) {}
    }
    
// =====================================
// BEST STREAM TRACKER
// =====================================

private var bestStreamUrl =
    ""

private var bestStreamScore =
    0
    
// =====================================
// YOUTUBE DASH TRACKER
// =====================================

private var youtubeDashVideoUrl =
    ""

private var youtubeDashAudioUrl =
    ""

private var youtubeDashVideoItag =
    ""

private var youtubeDashAudioItag =
    ""
    
// =====================================
// YOUTUBE EMBED / WATCH TRACKER
// =====================================

private var youtubeEmbedUrl =
    ""

private var youtubeWatchUrl =
    ""

// =====================================
// STREAM VALIDATION CACHE
// =====================================

private val streamValidation =
    linkedMapOf<String, String>()
  
private var monitorRunning =
    false

private var autoRefreshEnabled =
    false

// =====================================
// DEEP SCAN TIMER
// =====================================

private var lastDeepScanTime =
    0L

private var lastTouchX = 0f
private var lastTouchY = 0f

private val streamScores =
    mutableMapOf<String, Int>()
    
// =====================================
// FORENSIC SOURCE CACHE
// =====================================

private val streamSources =
    java.util.concurrent.ConcurrentHashMap<String, String>()
    
// =====================================
// BLOB CORRELATION
// =====================================

private val blobRelations =
    java.util.concurrent.ConcurrentHashMap<
        String,
        String
    >()
    
// =====================================
// MANIFEST RELATIONS
// =====================================

private val manifestRelations =
    java.util.concurrent.ConcurrentHashMap<
        String,
        String
    >()

private fun markStreamSource(
    url: String,
    source: String
) {

    try {

        if (
            !streamSources.containsKey(url)
        ) {

            streamSources[url] =
                source
        }

    } catch (_: Throwable) {}
}

// =====================================
// TOKEN INTELLIGENCE
// =====================================

private val streamTokens =
    java.util.concurrent.ConcurrentHashMap<
        String,
        MutableList<String>
    >()
      
// =====================================  
// FULLSCREEN VIDEO  
// =====================================  

private var customView: View? = null  

private var customViewCallback:  
        WebChromeClient.CustomViewCallback? = null  

// =====================================  
// REQUEST BODY  
// =====================================  

private val requestBody: JSONObject  
    get() {  

        return JSONObject().apply {  

            put(  
                "name",  
                "test${Date().time}"  
            )  

            put(  
                "job",  
                "Investment manager"  
            )  

            put(  
                "age",  
                "23"  
            )  
        }  
    }  

// =====================================  
// OKHTTP  
// =====================================  

private val okHttpClient: OkHttpClient
    get() {

        val builder =  
            OkHttpClient.Builder()  

        builder.networkInterceptors().add(  
            object : Interceptor {  

                @Throws(IOException::class)  
                override fun intercept(  
                    chain: Interceptor.Chain  
                ): Response {  

                    val response =  
                        chain.proceed(  
                            chain.request()  
                        )  
                        
                    val url =
                        response.request.url.toString()

                    val body =  
                        response.body  
                            ?.string()  
                            .orEmpty()  
                            
// =====================================
// DRM DETECTION
// =====================================

try {

    val lowerBody =
        body.lowercase()

    if (

        lowerBody.contains("widevine") ||
        lowerBody.contains("fairplay") ||
        lowerBody.contains("playready") ||
        lowerBody.contains("drm") ||
        lowerBody.contains("license") ||
        lowerBody.contains("clearkey")

    ) {

        streamValidation[url] =
            "🔐 DRM"

        Log.e(
            "DRM_DETECTED",
            url
        )
    }

} catch (_: Throwable) {}                

                    return response.newBuilder()  
                        .removeHeader(  
                            "Content-type"  
                        )  
                        .addHeader(  
                            "Content-type",  
                            "application/json; charset=utf-8"  
                        )  
                        .body(  
                            body.toResponseBody(  
                                response.body  
                                    ?.contentType()  
                            )  
                        )  
                        .build()  
                }  
            }  
        )  

        return builder.build()  
    }  



// =====================================
// PREVIEW IMAGE IN APP
// Shows image large without opening source page
// Fixed: explicit preview height, no blank dialog
// =====================================

private fun showImagePreviewDialog(
    imageUrl: String
) {

    try {

        val cleanUrl =
            imageUrl
                .trim()

        if (cleanUrl.isBlank()) {

            Toast.makeText(
                this,
                "No image URL",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val safeUrl =
            cleanUrl
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")

        val dialogWidth =
            (resources.displayMetrics.widthPixels * 0.96f).toInt()

        val dialogHeight =
            (resources.displayMetrics.heightPixels * 0.88f).toInt()

        val previewHeight =
            (resources.displayMetrics.heightPixels * 0.68f).toInt()

        val previewWebView =
            WebView(this)

        previewWebView.settings.apply {

            javaScriptEnabled =
                false

            domStorageEnabled =
                true

            loadsImagesAutomatically =
                true

            blockNetworkImage =
                false

            blockNetworkLoads =
                false

            useWideViewPort =
                true

            loadWithOverviewMode =
                true

            setSupportZoom(
                true
            )

            builtInZoomControls =
                true

            displayZoomControls =
                false

            mixedContentMode =
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            cacheMode =
                WebSettings.LOAD_DEFAULT
        }

        try {

            val cookieManager =
                CookieManager.getInstance()

            cookieManager.setAcceptCookie(
                true
            )

            cookieManager.setAcceptThirdPartyCookies(
                previewWebView,
                true
            )

            cookieManager.flush()

        } catch (_: Throwable) {}

        previewWebView.setBackgroundColor(
            android.graphics.Color.BLACK
        )

        val html =
            """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=6.0, user-scalable=yes">
<style>
html, body {
    margin: 0;
    padding: 0;
    width: 100%;
    min-height: 100%;
    background: #000000;
    overflow: auto;
}
body {
    display: flex;
    align-items: center;
    justify-content: center;
}
img {
    display: block;
    max-width: 100vw;
    max-height: 100vh;
    width: auto;
    height: auto;
    object-fit: contain;
}
</style>
</head>
<body>
<img src="$safeUrl" />
</body>
</html>
            """.trimIndent()

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )

                minimumHeight =
                    dialogHeight
            }

        root.addView(
            previewWebView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                previewHeight
            )
        )

        val buttonRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    10,
                    10,
                    10,
                    10
                )
            }

        val copyButton =
            Button(this).apply {

                text =
                    "COPY URL"

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {

                        setMargins(
                            0,
                            0,
                            6,
                            0
                        )
                    }
            }

        val openButton =
            Button(this).apply {

                text =
                    "OPEN"

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {

                        setMargins(
                            6,
                            0,
                            0,
                            0
                        )
                    }
            }

        buttonRow.addView(
            copyButton
        )

        buttonRow.addView(
            openButton
        )

        root.addView(
            buttonRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog =
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Image Preview")
                .setView(root)
                .setNegativeButton(
                    "CLOSE",
                    null
                )
                .create()

        copyButton.setOnClickListener {

            try {

                val clipboard =
                    getSystemService(
                        CLIPBOARD_SERVICE
                    ) as ClipboardManager

                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "image_url",
                        cleanUrl
                    )
                )

                Toast.makeText(
                    this,
                    "Image URL copied",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (_: Throwable) {}
        }

        openButton.setOnClickListener {

            try {

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(cleanUrl)
                    ).apply {

                        addCategory(
                            Intent.CATEGORY_BROWSABLE
                        )
                    }

                startActivity(
                    Intent.createChooser(
                        intent,
                        "Open Image With"
                    )
                )

            } catch (_: Throwable) {

                Toast.makeText(
                    this,
                    "Cannot open image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.setOnShowListener {

            try {

                dialog.window?.setLayout(
                    dialogWidth,
                    dialogHeight
                )

                previewWebView.postDelayed(
                    {

                        try {

                            previewWebView.loadDataWithBaseURL(
                                cleanUrl,
                                html,
                                "text/html",
                                "UTF-8",
                                null
                            )

                        } catch (_: Throwable) {}
                    },
                    150
                )

            } catch (_: Throwable) {}
        }

        dialog.setOnDismissListener {

            try {

                previewWebView.stopLoading()
                previewWebView.loadUrl("about:blank")
                previewWebView.destroy()

            } catch (_: Throwable) {}
        }

        dialog.show()

    } catch (t: Throwable) {

        Log.e(
            "IMAGE_PREVIEW",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Image preview failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}


// =====================================
// COOKIE CONSENT URL KEY
// =====================================

private fun buildCookieConsentUrlKey(
    rawUrl: String
): String {

    return try {

        val uri =
            Uri.parse(
                rawUrl
            )

        val scheme =
            uri.scheme
                ?: "https"

        val host =
            uri.host
                ?: rawUrl

        val path =
            uri.path
                ?: ""

        "$scheme://$host$path"

    } catch (_: Throwable) {

        rawUrl.substringBefore("?")
    }
}

// =====================================
// COOKIE CONSENT BRIDGE
// Called from WebView JavaScript after user clicks
// Allow / Reject / Confirm / Save choices
// =====================================

inner class CookieConsentBridge {

    @JavascriptInterface
    fun onCookieConsentAction(
        pageUrl: String?
    ) {

        try {

            handleCookieConsentAction(
                pageUrl.orEmpty()
            )

        } catch (_: Throwable) {}
    }
}

// =====================================
// HANDLE COOKIE CONSENT ACTION
// Flush cookies and reload current page once
// =====================================

private fun handleCookieConsentAction(
    pageUrl: String
) {

    try {

        runOnUiThread {

            try {

                val activeWebView =
                    popupWebView
                        ?: binding.contentMain.webview

                val currentUrl =
                    pageUrl
                        .takeIf { it.isNotBlank() }
                        ?: activeWebView.url.orEmpty()

                if (currentUrl.isBlank()) {
                    return@runOnUiThread
                }

                val key =
                    buildCookieConsentUrlKey(
                        currentUrl
                    )

                if (
                    cookieConsentReloadInProgress ||
                    cookieConsentReloadedUrls.contains(key)
                ) {
                    return@runOnUiThread
                }

                cookieConsentReloadInProgress =
                    true

                cookieConsentReloadedUrls.add(
                    key
                )

                webUserInteracting =
                    true

                try {

                    CookieManager
                        .getInstance()
                        .flush()

                } catch (_: Throwable) {}

                activeWebView.postDelayed(
                    {

                        try {

                            activeWebView.stopLoading()
                            activeWebView.reload()

                        } catch (_: Throwable) {}

                        activeWebView.postDelayed(
                            {

                                cookieConsentReloadInProgress =
                                    false

                                webUserInteracting =
                                    false
                            },
                            1800
                        )
                    },
                    700
                )

            } catch (_: Throwable) {

                cookieConsentReloadInProgress =
                    false

                webUserInteracting =
                    false
            }
        }

    } catch (_: Throwable) {}
}

// =====================================
// INSTALL COOKIE CONSENT WATCHER
// Detect user clicks on cookie banners without auto-clicking
// =====================================

private fun installCookieConsentWatcher(
    view: WebView?,
    url: String?
) {

    try {

        if (
            view == null ||
            url.isNullOrBlank() ||
            url.equals(
                "about:blank",
                true
            ) ||
            cloudflareChallengeActive ||
            isCloudflareChallengeRequestUrl(
                url
            )
        ) {
            return
        }

        view.evaluateJavascript(
            """
(function() {

    try {

        if (window.__gelCookieConsentWatcherInstalled) {
            return "already-installed";
        }

        window.__gelCookieConsentWatcherInstalled = true;

        function gelTextOf(el) {

            try {

                if (!el) {
                    return "";
                }

                var text = "";

                text += " " + (el.id || "");
                text += " " + (el.className || "");
                text += " " + (el.name || "");
                text += " " + (el.value || "");
                text += " " + (el.getAttribute("aria-label") || "");
                text += " " + (el.getAttribute("title") || "");
                text += " " + (el.innerText || "");
                text += " " + (el.textContent || "");

                return String(text).toLowerCase();

            } catch(e) {

                return "";
            }
        }

        function gelIsCookieConsentElement(el) {

            try {

                var node = el;
                var depth = 0;

                while (node && depth < 6) {

                    var t = gelTextOf(node);

                    var hasCookieSystem =
                        t.indexOf("onetrust") >= 0 ||
                        t.indexOf("ot-sdk") >= 0 ||
                        t.indexOf("cookie") >= 0 ||
                        t.indexOf("privacy") >= 0 ||
                        t.indexOf("consent") >= 0 ||
                        t.indexOf("gdpr") >= 0 ||
                        t.indexOf("cmp") >= 0;

                    var hasAction =
                        t.indexOf("allow all") >= 0 ||
                        t.indexOf("accept all") >= 0 ||
                        t.indexOf("reject all") >= 0 ||
                        t.indexOf("confirm my choices") >= 0 ||
                        t.indexOf("confirm choices") >= 0 ||
                        t.indexOf("save choices") >= 0 ||
                        t.indexOf("save preference") >= 0 ||
                        t.indexOf("accept recommended") >= 0 ||
                        t.indexOf("agree") >= 0 ||
                        t.indexOf("i accept") >= 0 ||
                        t.indexOf("got it") >= 0;

                    if (hasCookieSystem && hasAction) {
                        return true;
                    }

                    if (
                        t.indexOf("onetrust-accept-btn-handler") >= 0 ||
                        t.indexOf("onetrust-reject-all-handler") >= 0 ||
                        t.indexOf("accept-recommended-btn-handler") >= 0 ||
                        t.indexOf("save-preference-btn-handler") >= 0 ||
                        t.indexOf("ot-pc-refuse-all-handler") >= 0
                    ) {
                        return true;
                    }

                    node = node.parentElement;
                    depth++;
                }

                return false;

            } catch(e) {

                return false;
            }
        }

        document.addEventListener(
            "click",
            function(ev) {

                try {

                    var target = ev.target;

                    if (!target) {
                        return;
                    }

                    if (!gelIsCookieConsentElement(target)) {
                        return;
                    }

                    setTimeout(
                        function() {

                            try {

                                if (
                                    window.GELCookieBridge &&
                                    window.GELCookieBridge.onCookieConsentAction
                                ) {

                                    window.GELCookieBridge.onCookieConsentAction(
                                        window.location.href || ""
                                    );
                                }

                            } catch(e) {}
                        },
                        350
                    );

                } catch(e) {}
            },
            true
        );

        return "installed";

    } catch(e) {

        return "cookie-watcher-error:" + e;
    }
})();
            """.trimIndent(),
            null
        )

    } catch (_: Throwable) {}
}


// =====================================
// RESULTS PANEL HELPERS
// Collapsed / Normal / Expanded URL output panel
// =====================================

private fun dpToPx(
    dp: Int
): Int {

    return try {

        (
            dp *
                resources.displayMetrics.density
            ).toInt()

    } catch (_: Throwable) {

        dp
    }
}

private fun setupResultsPanel() {

    try {

        binding.contentMain.btnResultsToggle.setOnClickListener {

            toggleResultsPanel()
        }

        binding.contentMain.resultPanelHeader.setOnClickListener {

            toggleResultsPanel()
        }

        binding.contentMain.resultPanelHeader.setOnLongClickListener {

            setResultsPanelState(
                0
            )

            true
        }

        binding.contentMain.btnResultCopy.setOnClickListener {

            copyResultsPanelText()
        }

        binding.contentMain.btnResultClear.setOnClickListener {

            try {

                binding.contentMain.btnClear.performClick()

            } catch (_: Throwable) {

                clearResultsPanelOnly()
            }
        }

        setResultsPanelState(
            resultsPanelState
        )

    } catch (t: Throwable) {

        Log.e(
            "RESULTS_PANEL",
            "setup failed",
            t
        )
    }
}

private fun toggleResultsPanel() {

    val nextState =
        when (resultsPanelState) {

            0 -> 1
            1 -> 2
            else -> 0
        }

    setResultsPanelState(
        nextState
    )
}

private fun setResultsPanelState(
    state: Int
) {

    try {

        resultsPanelState =
            when (state) {

                0 -> 0
                2 -> 2
                else -> 1
            }

        val targetHeight =
            when (resultsPanelState) {

                0 ->
                    dpToPx(
                        resultsPanelCollapsedHeightDp
                    )

                2 ->
                    (
                        resources.displayMetrics.heightPixels *
                            0.42f
                        ).toInt()

                else ->
                    dpToPx(
                        resultsPanelNormalHeightDp
                    )
            }

        val params =
            binding.contentMain.resultScroll.layoutParams

        params.height =
            targetHeight

        binding.contentMain.resultScroll.layoutParams =
            params

        binding.contentMain.resultScroll.visibility =
            if (resultsPanelState == 0) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.contentMain.btnResultsToggle.text =
            when (resultsPanelState) {

                0 -> "RESULTS ▲"
                2 -> "RESULTS ▼"
                else -> "RESULTS ▲"
            }

        binding.contentMain.resultPanelHeader.alpha =
            if (resultsPanelState == 0) {
                0.88f
            } else {
                1.0f
            }

    } catch (t: Throwable) {

        Log.e(
            "RESULTS_PANEL",
            "state failed",
            t
        )
    }
}

private fun copyResultsPanelText() {

    try {

        val text =
            binding.contentMain.result
                .text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (text.isBlank()) {

            Toast.makeText(
                this,
                "No results to copy",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "gel_results",
                text
            )
        )

        Toast.makeText(
            this,
            "Results copied",
            Toast.LENGTH_SHORT
        ).show()

    } catch (t: Throwable) {

        Log.e(
            "RESULTS_PANEL",
            "copy failed",
            t
        )

        Toast.makeText(
            this,
            "Copy failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun clearResultsPanelOnly() {

    try {

        binding.contentMain.result.text =
            ""

        Toast.makeText(
            this,
            "Results cleared",
            Toast.LENGTH_SHORT
        ).show()

    } catch (_: Throwable) {}
}

// =====================================  
// ON CREATE  
// =====================================  

override fun onCreate(  
    savedInstanceState: Bundle?  
) {  

    super.onCreate(  
        savedInstanceState  
    )  

    binding =  
        ActivityMainBinding.inflate(  
            layoutInflater  
        )  

    setContentView(  
binding.root

)

// =====================================
// GEL BUTTON OUTLINE COLORS
// Green outline for actions / Red outline for CLEAR
// =====================================

try {

    val green =
        android.graphics.Color.rgb(
            0,
            170,
            80
        )

    val red =
        android.graphics.Color.rgb(
            210,
            0,
            0
        )

    // TOP BUTTONS — GREEN
    applyButtonOutline(
        binding.contentMain.openBrowser,
        green
    )

    applyButtonOutline(
        binding.contentMain.openChrome,
        green
    )

    applyButtonOutline(
        binding.contentMain.btnAll,
        green
    )

    applyButtonOutline(
        binding.contentMain.btnVideos,
        green
    )

    applyButtonOutline(
        binding.contentMain.btnAudio,
        green
    )

    // TOP CLEAR — RED
    applyButtonOutline(
        binding.contentMain.btnClear,
        red
    )

    // RESULT PANEL BUTTONS — GREEN
    applyButtonOutline(
        binding.contentMain.btnResultsToggle,
        green
    )

    applyButtonOutline(
        binding.contentMain.btnResultCopy,
        green
    )

    // RESULT CLEAR — RED
    applyButtonOutline(
        binding.contentMain.btnResultClear,
        red
    )

} catch (_: Throwable) {}

loadSavedChannels()

window.setSoftInputMode(
    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
)

binding.contentMain.result.isVerticalScrollBarEnabled = true

binding.contentMain.result.movementMethod =
    android.text.method.ScrollingMovementMethod()

setupResultsPanel()

// =====================================
// LIVE URL INPUT TRACKER
// =====================================

try {

    liveUrlInputText =
        binding.contentMain.urlInput
            .text
            ?.toString()
            ?.trim()
            .orEmpty()

    binding.contentMain.urlInput.addTextChangedListener(
        object : android.text.TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                liveUrlInputText =
                    s
                        ?.toString()
                        ?.trim()
                        .orEmpty()
            }

            override fun afterTextChanged(
                s: android.text.Editable?
            ) {

                liveUrlInputText =
                    s
                        ?.toString()
                        ?.trim()
                        .orEmpty()
            }
        }
    )

} catch (_: Throwable) {}

setSupportActionBar(
    binding.toolbar
)

onBackPressedDispatcher.addCallback(this) {

    if (popupWebView != null) {

        closePopupWebView()

        return@addCallback
    }

    when {

        customView != null -> {

            customView?.visibility =
                View.GONE

        (customView?.parent as? ViewGroup)  
            ?.removeView(customView)  

        customView = null  

        binding.contentMain.webview.visibility =  
            View.VISIBLE  

        window.decorView.systemUiVisibility =  
            View.SYSTEM_UI_FLAG_VISIBLE  

        customViewCallback  
            ?.onCustomViewHidden()  
    }  

    binding.contentMain.webview.canGoBack() -> {  

        binding.contentMain.webview.goBack()  
    }  

    else -> {  

        finish()  
    }  
}

}

// =====================================
// WEBVIEW SETTINGS
// Full browser-mode analyzer profile
// =====================================

binding.contentMain.webview.settings.apply {

    // =====================================
    // JAVASCRIPT / STORAGE
    // =====================================

    javaScriptEnabled =
        true

    javaScriptCanOpenWindowsAutomatically =
        true

    domStorageEnabled =
        true

    databaseEnabled =
        true

    // =====================================
    // NETWORK / MEDIA
    // =====================================

    loadsImagesAutomatically =
        true

    blockNetworkImage =
        false

    blockNetworkLoads =
        false

    mediaPlaybackRequiresUserGesture =
        false

    mixedContentMode =
        WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

    cacheMode =
        WebSettings.LOAD_DEFAULT

    // =====================================
    // VIEWPORT / DESKTOP-LIKE BEHAVIOR
    // =====================================

    useWideViewPort =
        true

    loadWithOverviewMode =
        true

    setSupportZoom(
        true
    )

    builtInZoomControls =
        true

    displayZoomControls =
        false

    // =====================================
    // WINDOWS / POPUPS
    // =====================================

    setSupportMultipleWindows(
        true
    )

    // =====================================
    // ACCESS
    // Keep safe for Play Store
    // =====================================

    allowFileAccess =
        true

    allowContentAccess =
        true

    allowFileAccessFromFileURLs =
        false

    allowUniversalAccessFromFileURLs =
        false

    // =====================================
    // USER AGENT
    // Stable desktop Chrome identity
    // =====================================

    userAgentString =
        systemWebViewUserAgent
}

// =====================================
// WEBVIEW TOUCH = PAUSE SCANNER + SCROLL FIX
// Prevent ANR while user taps / scrolls / opens images
// =====================================

binding.contentMain.webview.setOnTouchListener { v, _ ->

    try {

        webUserInteracting =
            true

        binding.contentMain.webview.removeCallbacks(
            clearWebInteractionRunnable
        )

        binding.contentMain.webview.postDelayed(
            clearWebInteractionRunnable,
            3500
        )

        v.parent?.requestDisallowInterceptTouchEvent(
            true
        )

    } catch (_: Throwable) {}

    false
}

// =====================================
// WEBVIEW SAFE BROWSING
// =====================================

try {

    if (
        android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.O
    ) {

        binding.contentMain.webview.settings.safeBrowsingEnabled =
            true
    }

} catch (_: Throwable) {}

// =====================================
// WEBVIEW COOKIES
// =====================================

try {

    val cookieManager =
        CookieManager.getInstance()

    cookieManager.setAcceptCookie(
        true
    )

    cookieManager.setAcceptThirdPartyCookies(
        binding.contentMain.webview,
        true
    )

    cookieManager.flush()

} catch (_: Throwable) {}

// =====================================
// COOKIE CONSENT JAVASCRIPT BRIDGE
// Automatic cookie banner recovery
// =====================================

try {

    binding.contentMain.webview.addJavascriptInterface(
        CookieConsentBridge(),
        "GELCookieBridge"
    )

} catch (_: Throwable) {}

// =====================================
// WEBVIEW SCROLL FIX
// =====================================

binding.contentMain.webview.isVerticalScrollBarEnabled =
    true

binding.contentMain.webview.isHorizontalScrollBarEnabled =
    true

binding.contentMain.webview.isScrollbarFadingEnabled =
    false

binding.contentMain.webview.overScrollMode =
    View.OVER_SCROLL_ALWAYS

// =====================================
// WEBVIEW RENDERING LAYER FIX
// Prevent blank / invisible WebView rendering
// =====================================

try {

    binding.contentMain.webview.setBackgroundColor(
        android.graphics.Color.WHITE
    )

    binding.contentMain.webview.setLayerType(
        View.LAYER_TYPE_HARDWARE,
        null
    )

    binding.contentMain.webview.isFocusable =
        true

    binding.contentMain.webview.isFocusableInTouchMode =
        true

    binding.contentMain.webview.requestFocus()

    if (
        android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.M
    ) {

        binding.contentMain.webview.settings.offscreenPreRaster =
            true
    }

} catch (_: Throwable) {}

// =====================================
// WEBVIEW LONG PRESS MENU
// IMAGE = CUSTOM POPUP
// TEXT  = DEFAULT WEBVIEW SELECTION
// =====================================

binding.contentMain.webview.isLongClickable =
    true

binding.contentMain.webview.setOnLongClickListener { view ->

    try {

        val webView =
            view as? WebView
                ?: return@setOnLongClickListener false

        val hit =
            webView.hitTestResult

        val imageUrl =
            when (hit.type) {

                WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {

                    hit.extra
                        ?.trim()
                        .orEmpty()
                }

                else -> {
                    ""
                }
            }

        // =====================================
        // NOT IMAGE → LET WEBVIEW SELECT TEXT
        // =====================================

        if (imageUrl.isBlank()) {
            return@setOnLongClickListener false
        }

        // =====================================
        // IMAGE → CUSTOM IMAGE POPUP
        // =====================================

        val popup =
            PopupMenu(
                this,
                webView
            )

        popup.menu.add(
            "COPY IMAGE URL"
        )

        popup.menu.add(
            "PREVIEW IMAGE"
        )

        popup.menu.add(
            "OPEN IMAGE"
        )

        popup.menu.add(
            "SHARE IMAGE URL"
        )

        popup.setOnMenuItemClickListener { item ->

            when (item.title.toString()) {

                "COPY IMAGE URL" -> {

                    try {

                        val clipboard =
                            getSystemService(
                                CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "image_url",
                                imageUrl
                            )
                        )

                        Toast.makeText(
                            this,
                            "Image URL copied",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Throwable) {}

                    true
                }

                "PREVIEW IMAGE" -> {

                    try {

                        showImagePreviewDialog(
                            imageUrl
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Image preview failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    true
                }

                "OPEN IMAGE" -> {

                    try {

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(imageUrl)
                            ).apply {

                                addCategory(
                                    Intent.CATEGORY_BROWSABLE
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                intent,
                                "Open Image With"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Cannot open image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    true
                }

                "SHARE IMAGE URL" -> {

                    try {

                        val shareIntent =
                            Intent(
                                Intent.ACTION_SEND
                            ).apply {

                                type =
                                    "text/plain"

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    imageUrl
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share Image URL"
                            )
                        )

                    } catch (_: Throwable) {}

                    true
                }

                else -> false
            }
        }

        popup.show()

        true

    } catch (_: Throwable) {

        false
    }
}

// =====================================
// COOKIES
// =====================================

CookieManager
.getInstance()
.setAcceptCookie(true)

CookieManager
.getInstance()
.setAcceptThirdPartyCookies(
binding.contentMain.webview,
true
)

// =====================================
// RESULT TEXT SELECTABLE
// =====================================

binding.contentMain.result.setTextIsSelectable(true)

// =====================================
// WEBVIEW CLIENT
// Safe navigation + URL interception only
// =====================================

binding.contentMain.webview.webViewClient =
    object : WebViewClient() {

        override fun onPageStarted(
    view: WebView?,
    url: String?,
    favicon: android.graphics.Bitmap?
) {

    super.onPageStarted(
        view,
        url,
        favicon
    )

    try {

        if (!url.isNullOrBlank()) {

            if (
                isCloudflareChallengeRequestUrl(
                    url
                )
            ) {

                setCloudflareChallengeMode(
                    true
                )
            }

            binding.contentMain.urlInput.setText(
                url
            )

            binding.contentMain.urlInput.setSelection(
                0
            )

            liveUrlInputText =
                url
        }

    } catch (_: Throwable) {}
}

override fun onPageFinished(
    view: WebView?,
    url: String?
) {

    super.onPageFinished(
        view,
        url
    )

    try {

        if (!url.isNullOrBlank()) {

            if (
                url.equals(
                    "about:blank",
                    true
                )
            ) {
                return
            }

            // =====================================
            // UPDATE URL BAR
            // Show URL from beginning
            // =====================================

            try {

                binding.contentMain.urlInput.setText(
                    url
                )

                binding.contentMain.urlInput.setSelection(
                    0
                )

                liveUrlInputText =
                    url

            } catch (_: Throwable) {}

            // =====================================
            // SAVE PAGE TO HISTORY
            // =====================================

            try {

                addBrowserHistory(
                    url,
                    view?.title
                )

            } catch (_: Throwable) {}

            // =====================================
            // COOKIE CONSENT WATCHER
            // Auto reload after user confirms/rejects cookies
            // =====================================

            try {

                installCookieConsentWatcher(
                    view,
                    url
                )

            } catch (_: Throwable) {}

            // =====================================
            // FORCE WEBVIEW RENDER REFRESH
            // =====================================

            try {

                view?.postDelayed(
                    {

                        try {

                            view.setBackgroundColor(
                                android.graphics.Color.WHITE
                            )

                            view.requestLayout()

                            view.invalidate()

                            view.requestFocus()

                        } catch (_: Throwable) {}
                    },
                    300
                )

            } catch (_: Throwable) {}

            // =====================================
            // PAGE DOM DIAGNOSTIC
            // Detect blank / protected / WebView-blocked pages
            // No debug output in production
            // =====================================

            try {

                view?.evaluateJavascript(
                    """
(function() {

    try {

        var title =
            document.title || "";

        var ready =
            document.readyState || "";

        var bodyText =
            document.body
                ? document.body.innerText || ""
                : "";

        var html =
            document.documentElement
                ? document.documentElement.outerHTML || ""
                : "";

        var htmlLength =
            html.length;

        var bodyLength =
            bodyText.length;

        var hasVideo =
            document.querySelectorAll("video").length;

        var hasIframe =
            document.querySelectorAll("iframe").length;

        var hasScript =
            document.querySelectorAll("script").length;

        var lowerText =
            bodyText.toLowerCase();

        var hasProtectedWords =
            (
                lowerText.indexOf("protected") >= 0 ||
                lowerText.indexOf("drm") >= 0 ||
                lowerText.indexOf("geoblock") >= 0 ||
                lowerText.indexOf("geo") >= 0 ||
                lowerText.indexOf("content protected") >= 0 ||
                lowerText.indexOf("contenido protegido") >= 0 ||
                lowerText.indexOf("acceso a contenido protegido") >= 0
            );

        var verdict =
            "PAGE HAS DOM CONTENT";

        if (
            bodyLength < 30 &&
            htmlLength < 5000
        ) {

            verdict =
                "BLANK / BLOCKED / PROTECTED PAGE";

        } else if (
            hasProtectedWords
        ) {

            verdict =
                "PROTECTED / DRM / GEO MESSAGE DETECTED";

        } else if (
            hasVideo === 0 &&
            hasIframe === 0 &&
            bodyLength < 200
        ) {

            verdict =
                "NO VISIBLE MEDIA DOM";
        }

        return (
            "DOM TITLE: " + title + "\n" +
            "READY STATE: " + ready + "\n" +
            "BODY TEXT LENGTH: " + bodyLength + "\n" +
            "HTML LENGTH: " + htmlLength + "\n" +
            "VIDEO TAGS: " + hasVideo + "\n" +
            "IFRAME TAGS: " + hasIframe + "\n" +
            "SCRIPT TAGS: " + hasScript + "\n" +
            "VERDICT: " + verdict
        );

    } catch(e) {

        return "DOM DIAGNOSTIC ERROR: " + e;
    }

})();
                    """.trimIndent()
                    
                ) { jsResult ->

    try {

        if (
            url.equals(
                "about:blank",
                true
            ) ||
            url.contains(
                "google.com/search",
                true
            ) ||
            (
                !url.startsWith(
                    "http://",
                    true
                ) &&
                !url.startsWith(
                    "https://",
                    true
                )
            )
        ) {
            return@evaluateJavascript
        }

        val cleanResult =
            jsResult
                .removePrefix("\"")
                .removeSuffix("\"")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")

val isCloudflarePage =
    isCloudflareLikePage(
        url,
        cleanResult
    )

if (isCloudflarePage) {

    setCloudflareChallengeMode(
        true
    )

    Log.e(
        "CLOUDFLARE_GUARD",
        "Challenge detected -> scanner paused"
    )

    return@evaluateJavascript
}

val isBlockedPage =
    cleanResult.contains(
        "BLANK / BLOCKED / PROTECTED PAGE",
        true
    ) ||
        cleanResult.contains(
            "PROTECTED / DRM / GEO",
            true
        )

if (isBlockedPage) {

    val alreadyDetectedPlayable =
        detectedStreams.isNotEmpty() ||
            detectedVideos.isNotEmpty() ||
            detectedAudio.isNotEmpty() ||
            detectedMasterStreams.isNotEmpty() ||
            streamInfoSnapshots.isNotEmpty() ||
            bestStreamUrl.isNotBlank() ||
            bestLiveUrl.isNotBlank() ||
            youtubeWatchUrl.isNotBlank() ||
            youtubeDashVideoUrl.isNotBlank() ||
            youtubeDashAudioUrl.isNotBlank()

    if (alreadyDetectedPlayable) {

        Log.e(
            "PROTECTED_SKIPPED",
            "Streams already detected, fallback suppressed"
        )

        return@evaluateJavascript
    }

    val retried =
        retryWithRefererOnce(
            url
        )

    if (!retried) {

        binding.contentMain.webview.postDelayed(
    {

        try {

            val hasUsefulEvidence =
                detectedStreams.isNotEmpty() ||
                    detectedVideos.isNotEmpty() ||
                    detectedAudio.isNotEmpty() ||
                    detectedMasterStreams.isNotEmpty() ||
                    streamInfoSnapshots.isNotEmpty() ||
                    lastChannelCandidateCount > 0 ||
                    bestStreamUrl.isNotBlank() ||
                    bestLiveUrl.isNotBlank()

            if (hasUsefulEvidence) {
                return@postDelayed
            }

            runDeepMediaScan(
                view
            )

            binding.contentMain.webview.postDelayed(
                {

                    val hasLateEvidence =
                        detectedStreams.isNotEmpty() ||
                            detectedVideos.isNotEmpty() ||
                            detectedAudio.isNotEmpty() ||
                            detectedMasterStreams.isNotEmpty() ||
                            streamInfoSnapshots.isNotEmpty() ||
                            lastChannelCandidateCount > 0 ||
                            bestStreamUrl.isNotBlank() ||
                            bestLiveUrl.isNotBlank()

                    if (!hasLateEvidence) {

                        showProtectedPageFallback(
                            url,
                            cleanResult
                        )
                    }

                },
                2500
            )

        } catch (_: Throwable) {}
    },
    3500
)
    }
}

                    } catch (_: Throwable) {}
                }

            } catch (_: Throwable) {}

            // =====================================
            // DETECT PAGE URL
            // =====================================

            if (
                !cloudflareChallengeActive &&
                !isCloudflareChallengeRequestUrl(
                    url
                )
            ) {

                handleInterceptedMediaUrl(
                    url,
                    null
                )

                detectAndSaveUrl(
                    url
                )
            }

            // =====================================
            // ENABLE PAGE TEXT SELECTION
            // =====================================

            enablePageTextSelection(
                view
            )

            // =====================================
            // DEEP MEDIA SCAN
            // =====================================

           if (
    !webUserInteracting &&
    !cloudflareChallengeActive
) {

    runDeepMediaScan(
        view
    )
}

            // =====================================
            // DELAYED RESCAN 1
            // =====================================

            binding.contentMain.webview.postDelayed(
                {
                    try {

                        if (
                            view != null &&
                            !isFinishing &&
                            !isDestroyed
                        ) {

                            lastDeepScanTime =
                                0L

                            if (
    !webUserInteracting &&
    !cloudflareChallengeActive
) {

    runDeepMediaScan(
        view
    )
}
                        }

                    } catch (_: Throwable) {}
                },
                2500
            )

            // =====================================
            // DELAYED RESCAN 2
            // =====================================

            binding.contentMain.webview.postDelayed(
                {
                    try {

                        if (
                            view != null &&
                            !isFinishing &&
                            !isDestroyed
                        ) {

                            if (
    !webUserInteracting &&
    !cloudflareChallengeActive
) {

    runDeepMediaScan(
        view
    )
}
                        }

                    } catch (_: Throwable) {}
                },
                6000
            )
        }

    } catch (_: Throwable) {}
}

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {

            val url =
                request
                    ?.url
                    ?.toString()
                    ?: ""

            return try {

                if (
                    url.isNotBlank() &&
                    !cloudflareChallengeActive &&
                    !isCloudflareChallengeRequestUrl(
                        url
                    )
                ) {

                    handleInterceptedMediaUrl(
                        url,
                        request
                    )
                }

                false

            } catch (_: Throwable) {

                false
            }
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {

            val url =
                request
                    ?.url
                    ?.toString()
                    ?: ""

            try {

                if (
                    url.isNotBlank() &&
                    !cloudflareChallengeActive &&
                    !isCloudflareChallengeRequestUrl(
                        url
                    )
                ) {

                    handleInterceptedMediaUrl(
                        url,
                        request
                    )
                }

            } catch (_: Throwable) {}

            return null
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {

            super.onReceivedError(
                view,
                request,
                error
            )

            try {

                if (request?.isForMainFrame == true) {

                    binding.contentMain.result.append(
                        """

WEBVIEW ERROR:
${request.url}

${error?.description ?: "Unknown error"}

────────────────────

                        """.trimIndent()
                    )
                }

            } catch (_: Throwable) {}
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {

            super.onReceivedHttpError(
                view,
                request,
                errorResponse
            )

            try {

                if (request?.isForMainFrame == true) {

                    binding.contentMain.result.append(
                        """

HTTP ERROR:
${request.url}

STATUS:
${errorResponse?.statusCode ?: 0}

REASON:
${errorResponse?.reasonPhrase ?: ""}

────────────────────

                        """.trimIndent()
                    )
                }

            } catch (_: Throwable) {}
        }
    }

// =====================================
// WEB CHROME CLIENT (FULLSCREEN)
// =====================================

binding.contentMain.webview.webChromeClient =
    object : WebChromeClient() {
    
    override fun onProgressChanged(
    view: WebView?,
    newProgress: Int
) {

    super.onProgressChanged(
        view,
        newProgress
    )

    try {

        if (newProgress >= 100) {

            view?.postDelayed(
                {

                    try {

                        view.requestLayout()

                        view.invalidate()

                    } catch (_: Throwable) {}
                },
                300
            )
        }

    } catch (_: Throwable) {}
}

        override fun onCreateWindow(
    view: WebView?,
    isDialog: Boolean,
    isUserGesture: Boolean,
    resultMsg: android.os.Message?
): Boolean {

    return try {

        closePopupWebView()

        val childWebView =
            WebView(this@MainActivity)

        popupWebView =
            childWebView

        childWebView.layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        childWebView.settings.apply {

            javaScriptEnabled =
                true

            domStorageEnabled =
                true

            databaseEnabled =
                true

            allowFileAccess =
                true

            allowContentAccess =
                true

            mediaPlaybackRequiresUserGesture =
                false

            loadsImagesAutomatically =
                true

            blockNetworkImage =
                false

            blockNetworkLoads =
                false

            useWideViewPort =
                true

            loadWithOverviewMode =
                true

            setSupportZoom(
                true
            )

            builtInZoomControls =
                true

            displayZoomControls =
                false

            javaScriptCanOpenWindowsAutomatically =
                true

            setSupportMultipleWindows(
                false
            )

            mixedContentMode =
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            cacheMode =
                WebSettings.LOAD_DEFAULT

            userAgentString =
                binding.contentMain.webview.settings.userAgentString
        }

        try {

            val cookieManager =
                CookieManager.getInstance()

            cookieManager.setAcceptCookie(
                true
            )

            cookieManager.setAcceptThirdPartyCookies(
                childWebView,
                true
            )

            cookieManager.flush()

        } catch (_: Throwable) {}

        try {

            childWebView.addJavascriptInterface(
                CookieConsentBridge(),
                "GELCookieBridge"
            )

        } catch (_: Throwable) {}

        childWebView.webViewClient =
            object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {

                    val popupUrl =
                        request
                            ?.url
                            ?.toString()
                            .orEmpty()

                    return try {

                        if (popupUrl.isNotBlank()) {

                            binding.contentMain.urlInput.setText(
                                popupUrl
                            )

                            binding.contentMain.urlInput.setSelection(
                                0
                            )

                            liveUrlInputText =
                                popupUrl

                            if (
                                !cloudflareChallengeActive &&
                                !isCloudflareChallengeRequestUrl(
                                    popupUrl
                                )
                            ) {

                                handleInterceptedMediaUrl(
                                    popupUrl,
                                    request
                                )
                            }
                        }

                        false

                    } catch (_: Throwable) {

                        false
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {

                    val popupUrl =
                        request
                            ?.url
                            ?.toString()
                            .orEmpty()

                    try {

                        if (
                            popupUrl.isNotBlank() &&
                            !cloudflareChallengeActive &&
                            !isCloudflareChallengeRequestUrl(
                                popupUrl
                            )
                        ) {

                            handleInterceptedMediaUrl(
                                popupUrl,
                                request
                            )
                        }

                    } catch (_: Throwable) {}

                    return null
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    try {

                        if (!url.isNullOrBlank()) {

                            if (
                                isCloudflareChallengeRequestUrl(
                                    url
                                )
                            ) {

                                setCloudflareChallengeMode(
                                    true
                                )
                            }

                            binding.contentMain.urlInput.setText(
                                url
                            )

                            binding.contentMain.urlInput.setSelection(
                                0
                            )

                            liveUrlInputText =
                                url

                            // =====================================
                            // SAVE POPUP PAGE TO HISTORY
                            // =====================================

                            try {

                                addBrowserHistory(
                                    url,
                                    view?.title
                                )

                            } catch (_: Throwable) {}

                            try {

                                installCookieConsentWatcher(
                                    view,
                                    url
                                )

                            } catch (_: Throwable) {}

                            if (
                                !cloudflareChallengeActive &&
                                !isCloudflareChallengeRequestUrl(
                                    url
                                )
                            ) {

                                handleInterceptedMediaUrl(
                                    url,
                                    null
                                )

                                detectAndSaveUrl(
                                    url
                                )
                            }

                            if (
    !webUserInteracting &&
    !cloudflareChallengeActive
) {

    runDeepMediaScan(
        view
    )
}
                        }

                    } catch (_: Throwable) {}
                }
            }

        childWebView.webChromeClient =
            object : WebChromeClient() {

                override fun onCloseWindow(
                    window: WebView?
                ) {

                    closePopupWebView()
                }
            }

        val root =
            window.decorView
                .rootView as ViewGroup

        root.addView(
            childWebView
        )

        val transport =
            resultMsg?.obj
                as? WebView.WebViewTransport

        transport?.webView =
            childWebView

        resultMsg?.sendToTarget()

        true

    } catch (t: Throwable) {

        Log.e(
            "POPUP_WEBVIEW",
            "create popup failed",
            t
        )

        false
    }
}

        override fun onShowCustomView(
            view: View?,
            callback: CustomViewCallback?
        ) {

            if (customView != null) {

                callback?.onCustomViewHidden()
                return
            }

            customView =
                view

            customViewCallback =
                callback

            window.decorView.systemUiVisibility =
                (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )

            addContentView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            binding.contentMain.webview.visibility =
                View.GONE
        }

        override fun onHideCustomView() {

            customView?.visibility =
                View.GONE

            (customView?.parent as? ViewGroup)
                ?.removeView(customView)

            customView =
                null

            binding.contentMain.webview.visibility =
                View.VISIBLE

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE

            customViewCallback
                ?.onCustomViewHidden()

            customViewCallback =
                null
        }
    }

    // =====================================  
    // HANDLE INCOMING INTENT  
    // =====================================  

    handleIncomingIntent()

// =====================================
// OPEN PAGE
// Safe WebView navigation
// =====================================

binding.contentMain.openBrowser.setOnClickListener {

// =====================================
// RESET DETECTION STATE
// =====================================

detectedStreams.clear()
detectedVideos.clear()
detectedImages.clear()
detectedAudio.clear()
detectedMasterStreams.clear()
detectedChannels.clear()
detectedM3uLists.clear()

pagePlaylistFound =
    false

pendingTsFallback.clear()

cloudflareChallengeActive =
    false

lastCloudflareChallengeTime =
    0L

streamScores.clear()
streamValidation.clear()
streamSources.clear()
streamHeaders.clear()
streamTokens.clear()
streamResolution.clear()
streamBandwidth.clear()
streamCodec.clear()
streamInfoSnapshots.clear()
streamHitCounter.clear()
hlsVerdicts.clear()

blobRelations.clear()
manifestRelations.clear()
liveHeartbeatMap.clear()

bestStreamUrl =
    ""

bestStreamScore =
    0

bestLiveUrl =
    ""

bestLiveScore =
    0

youtubeEmbedUrl =
    ""

youtubeWatchUrl =
    ""

youtubeDashVideoUrl =
    ""

youtubeDashAudioUrl =
    ""

youtubeDashVideoItag =
    ""

youtubeDashAudioItag =
    ""

bestVideoItag =
    0

bestAudioItag =
    0

liveLocked =
    false

lockedStreamId =
    ""

lastSelectedUrl =
    ""

lastDeepScanTime =
    0L

monitorRunning =
    false

protectedFallbackShown =
    false

refererRetryDone =
    false
    
lastChannelCandidateCount =
    0
    
autoScanRunning =
    false

autoScanIndex =
    0

autoScanCandidates.clear()

autoScanResults.clear()

autoScanKnownBefore.clear()

    // =====================================
    // READ CURRENT INPUT DIRECTLY
    // No cached / old search text
    // =====================================

    val enteredText =
        binding.contentMain.urlInput
            .text
            ?.toString()
            ?.trim()
            .orEmpty()

    val finalUrl =
        when {

            enteredText.isEmpty() -> {

                "https://www.google.com"
            }

            enteredText.startsWith(
                "http://",
                true
            ) ||
            enteredText.startsWith(
                "https://",
                true
            ) -> {

                enteredText
            }

            enteredText.contains(".") &&
                !enteredText.contains(" ") -> {

                "https://$enteredText"
            }

            else -> {

                val query =
                    java.net.URLEncoder.encode(
                        enteredText,
                        "UTF-8"
                    )

                "https://www.google.com/search?q=$query"
            }
        }

    liveUrlInputText =
        enteredText

    // =====================================
    // HIDE KEYBOARD / CLEAR FOCUS
    // =====================================

    try {

        val imm =
            getSystemService(
                INPUT_METHOD_SERVICE
            ) as android.view.inputmethod.InputMethodManager

        imm.hideSoftInputFromWindow(
            binding.contentMain.urlInput.windowToken,
            0
        )

    } catch (_: Throwable) {}

    binding.contentMain.urlInput.clearFocus()

    // =====================================
    // UI LOG
    // =====================================

    binding.contentMain.result.text =
        """
LOADING WITH ANALYZER:

INPUT:
$enteredText

FINAL URL:
$finalUrl

────────────────────

        """.trimIndent()

    // =====================================
    // SAFE WEBVIEW RESET + LOAD
    // =====================================

    try {

        binding.contentMain.webview.stopLoading()

        binding.contentMain.webview.loadUrl(
            "about:blank"
        )

    } catch (_: Throwable) {}

    binding.contentMain.webview.postDelayed({

        try {

            binding.contentMain.webview.stopLoading()

            binding.contentMain.webview.loadUrl(
                finalUrl,
                mapOf(
                    "Cache-Control" to "no-cache",
                    "Pragma" to "no-cache"
                )
            )

            Log.e(
                "GEL_LOAD_ANALYZER",
                "INPUT=$enteredText FINAL=$finalUrl"
            )

        } catch (t: Throwable) {

            Log.e(
                "GEL_LOAD_ANALYZER",
                "load failed",
                t
            )

            Toast.makeText(
                this,
                "Analyzer load failed",
                Toast.LENGTH_SHORT
            ).show()
        }

    }, 500)
}

    // =====================================  
    // OPEN CHROME  
    // =====================================  

    binding.contentMain.openChrome.setOnClickListener {  

        val enteredText =  
            binding.contentMain.urlInput  
                .text  
                .toString()  
                .trim()  

        val finalUrl =  
            when {  

                enteredText.isEmpty() -> {  
                    "https://www.google.com"  
                }  

                enteredText.startsWith("http") -> {  
                    enteredText  
                }  

                enteredText.contains(".") &&  
                !enteredText.contains(" ") -> {  
                    "https://$enteredText"  
                }  

                else -> {  

                    val query =  
                        enteredText.replace(  
                            " ",  
                            "+"  
                        )  

                    "https://www.google.com/search?q=$query"  
                }  
            }  

        val browserIntent =  
Intent(  
    Intent.ACTION_VIEW,  
    Uri.parse(finalUrl)  
)

startActivity(
Intent.createChooser(
browserIntent,
"Open With Browser"
)
)
}

// =====================================
// OPEN PLAYER
// =====================================

binding.contentMain.openPlayer.setOnClickListener {

    val sortedStreams =
        mutableListOf<String>()

    // =====================================
    // COLLECT ALL PLAYABLE CANDIDATES
    // =====================================

    sortedStreams.addAll(
        detectedStreams
    )

    sortedStreams.addAll(
        detectedVideos
    )

    sortedStreams.addAll(
        detectedAudio
    )

    sortedStreams.addAll(
        streamInfoSnapshots.keys
    )

    sortedStreams.addAll(
        streamSources.keys
    )

    sortedStreams.addAll(
        streamValidation.keys
    )

    sortedStreams.addAll(
        streamScores.keys
    )

    streamTokens.keys.forEach { key ->

        sortedStreams.add(
            key
        )
    }

    if (bestStreamUrl.isNotBlank()) {

        sortedStreams.add(
            bestStreamUrl
        )
    }

    if (bestLiveUrl.isNotBlank()) {

        sortedStreams.add(
            bestLiveUrl
        )
    }

    if (youtubeWatchUrl.isNotBlank()) {

        sortedStreams.add(
            youtubeWatchUrl
        )
    }

    if (youtubeDashVideoUrl.isNotBlank()) {

        sortedStreams.add(
            youtubeDashVideoUrl
        )
    }

    if (youtubeDashAudioUrl.isNotBlank()) {

        sortedStreams.add(
            youtubeDashAudioUrl
        )
    }

    // =====================================
    // EXTRACT URLS FROM RESULT TEXT
    // =====================================

    try {

        val resultText =
            binding.contentMain.result
                .text
                ?.toString()
                .orEmpty()

        val regex =
            "(https?://[^\\s\"'<>]+)"
                .toRegex()

        regex.findAll(resultText)
            .forEach { match ->

                val found =
                    match.value
                        .trim()
                        .trimEnd(',')
                        .trimEnd(';')
                        .trimEnd(')')
                        .trimEnd(']')
                        .trimEnd('}')

                if (found.isNotBlank()) {

                    sortedStreams.add(
                        found
                    )
                }
            }

    } catch (_: Throwable) {}

// =====================================
// PLAYABLE STREAM LIST
// NO IMAGES
// =====================================

val streamList =
    sortedStreams
        .map { url ->

            url
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim()
        }
        .filter { url ->

            url.isNotBlank() &&
                url.startsWith(
                    "http",
                    true
                ) &&
                isExportableStream(
                    url
                )
        }
        .map { url ->

            val key =
                try {

                    val lower =
                        url.lowercase()

                    if (
                        lower.contains("googlevideo.com") ||
                        lower.contains("videoplayback")
                    ) {

                        val uri =
                            Uri.parse(url)

                        val id =
                            uri.getQueryParameter("id")
                                ?.substringBefore(".")
                                .orEmpty()

                        val itag =
                            uri.getQueryParameter("itag")
                                .orEmpty()

                        val mime =
                            uri.getQueryParameter("mime")
                                .orEmpty()

                        val source =
                            uri.getQueryParameter("source")
                                .orEmpty()

                        "googlevideo://$id/$itag/$mime/$source"

                    } else if (
                        lower.contains("youtube.com/watch") ||
                        lower.contains("youtu.be/") ||
                        lower.contains("youtube.com/live") ||
                        lower.contains("youtube.com/c/")
                    ) {

                        val uri =
                            Uri.parse(url)

                        val v =
                            uri.getQueryParameter("v")
                                .orEmpty()

                        if (v.isNotBlank()) {

                            "youtube://watch/$v"

                        } else {

                            url
                                .substringBefore("#")
                                .trim()
                        }

                    } else {

                        url
                            .substringBefore("#")
                            .trim()
                    }

                } catch (_: Throwable) {

                    url
                }

            key to url
        }
        .distinctBy { pair ->

            pair.first
        }
        .map { pair ->

            pair.second
        }
        .toTypedArray()

if (streamList.isEmpty()) {

    Toast.makeText(
        this,
        "No playable streams found",
        Toast.LENGTH_SHORT
    ).show()

    return@setOnClickListener
}

androidx.appcompat.app.AlertDialog.Builder(this)
    .setTitle("Select Stream")

    .setItems(streamList) { _, which ->

        val url =
            streamList[which]

        try {

            val urlToOpen =
                url
                    .trim()
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace(" ", "")

// =====================================
// YOUTUBE WATCH URL
// Render disabled for YouTube
// Keep as reference only
// =====================================

if (
    urlToOpen.contains(
        "youtube.com/watch",
        true
    ) ||
    urlToOpen.contains(
        "youtu.be/",
        true
    ) ||
    urlToOpen.contains(
        "youtube.com/live",
        true
    ) ||
    urlToOpen.contains(
        "youtube.com/c/",
        true
    )
) {

    lastSelectedUrl =
        urlToOpen

    Toast.makeText(
        this,
        "YouTube requires local resolver / Termux",
        Toast.LENGTH_LONG
    ).show()

    val intent =
        Intent(
            Intent.ACTION_VIEW
        ).apply {

            data =
                Uri.parse(
                    urlToOpen
                )

            addCategory(
                Intent.CATEGORY_BROWSABLE
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

    try {

        startActivity(
            Intent.createChooser(
                intent,
                "Open YouTube Link"
            )
        )

    } catch (_: Throwable) {

        Toast.makeText(
            this,
            "Cannot open YouTube link",
            Toast.LENGTH_SHORT
        ).show()
    }

    return@setItems
}

            val lower =
                urlToOpen.lowercase()

            val mimeType =
                when {

                    // =========================
                    // AUDIO
                    // =========================

                    lower.contains(".mp3") ->
                        "audio/mpeg"

                    lower.contains(".m4a") ->
                        "audio/mp4"

                    lower.contains(".aac") ->
                        "audio/aac"

                    lower.contains(".opus") ->
                        "audio/opus"

                    lower.contains(".wav") ->
                        "audio/wav"

                    lower.contains(".ogg") ->
                        "audio/ogg"

                    lower.contains(".flac") ->
                        "audio/flac"

                    // =========================
                    // HLS / DASH
                    // =========================

                    lower.contains(".m3u8") ||
                        lower.contains("hls") ->
                        "application/vnd.apple.mpegurl"

                    lower.contains(".mpd") ||
                        lower.contains("dash") ->
                        "application/dash+xml"

                    // =========================
                    // VIDEO
                    // =========================

                    lower.contains(".mp4") ->
                        "video/mp4"

                    lower.contains(".webm") ->
                        "video/webm"

                    lower.contains(".mkv") ->
                        "video/*"

                    lower.contains(".mov") ->
                        "video/*"

                    lower.contains(".avi") ->
                        "video/*"

                    lower.contains(".3gp") ->
                        "video/*"

                    lower.endsWith(".ts") ||
                        lower.contains(".ts?") ->
                        "video/*"

                    lower.contains("googlevideo.com") ||
                        lower.contains("videoplayback") ->
                        "video/*"

                    // =========================
                    // FALLBACK
                    // =========================

                    else ->
                        "*/*"
                }

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        Uri.parse(urlToOpen),
                        mimeType
                    )

                    addCategory(
                        Intent.CATEGORY_BROWSABLE
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Open Stream With"
                )
            )

        } catch (t: Throwable) {

            Log.e(
                "PLAYER_OPEN",
                "failed",
                t
            )

            Toast.makeText(
                this,
                "Cannot open stream",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    .show()

} // END OPEN PLAYER

binding.contentMain.shareStreams.setOnClickListener {

    val sortedStreams =
        mutableListOf<String>()

    // =====================================
    // COPY / SHARE — PLAYABLE ONLY
    // Live streams + static videos + audio
    // No images, no ads, no trackers
    // =====================================

    sortedStreams.addAll(
        detectedStreams
    )

    sortedStreams.addAll(
        detectedVideos
    )

    sortedStreams.addAll(
        detectedAudio
    )

    sortedStreams.addAll(
        streamInfoSnapshots.keys
    )

    sortedStreams.addAll(
        streamSources.keys
    )

    sortedStreams.addAll(
        streamValidation.keys
    )

    sortedStreams.addAll(
        streamScores.keys
    )

    sortedStreams.addAll(
        streamTokens.keys
    )

    if (bestStreamUrl.isNotBlank()) {

        sortedStreams.add(
            bestStreamUrl
        )
    }

    if (bestLiveUrl.isNotBlank()) {

        sortedStreams.add(
            bestLiveUrl
        )
    }

    if (youtubeWatchUrl.isNotBlank()) {

        sortedStreams.add(
            youtubeWatchUrl
        )
    }

    if (youtubeDashVideoUrl.isNotBlank()) {

        sortedStreams.add(
            youtubeDashVideoUrl
        )
    }

    if (youtubeDashAudioUrl.isNotBlank()) {

        sortedStreams.add(
            youtubeDashAudioUrl
        )
    }

    // =====================================
    // EXTRACT URLS FROM RESULT TEXT
    // =====================================

    try {

        val resultText =
            binding.contentMain.result
                .text
                ?.toString()
                .orEmpty()

        val regex =
            "(https?://[^\\s\"'<>]+)"
                .toRegex()

        regex.findAll(resultText)
            .forEach { match ->

                val found =
                    match.value
                        .trim()
                        .trimEnd(',')
                        .trimEnd(';')
                        .trimEnd(')')
                        .trimEnd(']')
                        .trimEnd('}')

                if (found.isNotBlank()) {

                    sortedStreams.add(
                        found
                    )
                }
            }

    } catch (_: Throwable) {}

    // =====================================
    // FINAL COPY / SHARE LIST
    // SAME PLAYABLE LOGIC AS OPEN PLAYER / EXPORT
    // =====================================

    val streamList =
    sortedStreams
        .map { url ->

            url
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim()
        }
        .filter { url ->

            url.isNotBlank() &&
                url.startsWith(
                    "http",
                    true
                ) &&
                isExportableStream(
                    url
                )
        }
        .map { url ->

            val key =
                try {

                    val lower =
                        url.lowercase()

                    if (
                        lower.contains("googlevideo.com") ||
                        lower.contains("videoplayback")
                    ) {

                        val uri =
                            Uri.parse(url)

                        val id =
                            uri.getQueryParameter("id")
                                ?.substringBefore(".")
                                .orEmpty()

                        val itag =
                            uri.getQueryParameter("itag")
                                .orEmpty()

                        val mime =
                            uri.getQueryParameter("mime")
                                .orEmpty()

                        val source =
                            uri.getQueryParameter("source")
                                .orEmpty()

                        "googlevideo://$id/$itag/$mime/$source"

                    } else if (
                        lower.contains("youtube.com/watch")
                    ) {

                        val uri =
                            Uri.parse(url)

                        val v =
                            uri.getQueryParameter("v")
                                .orEmpty()

                        "youtube://watch/$v"

                    } else {

                        url
                            .substringBefore("#")
                            .trim()
                    }

                } catch (_: Throwable) {

                    url
                }

            key to url
        }
        .distinctBy { pair ->

            pair.first
        }
        .map { pair ->

            pair.second
        }
        .toTypedArray()

    if (streamList.isEmpty()) {

        Toast.makeText(
            this,
            "No playable streams found",
            Toast.LENGTH_SHORT
        ).show()

        return@setOnClickListener
    }

    val checkedItems =
        BooleanArray(
            streamList.size
        )

    val selected =
        mutableListOf<String>()

 // =====================================
// CUSTOM SHARE / COPY / SAVE DIALOG
// ScrollView version — stable buttons, scrollable list
// =====================================

val dialogHeight =
    (resources.displayMetrics.heightPixels * 0.90f).toInt()

val root =
    android.widget.LinearLayout(this).apply {

        orientation =
            android.widget.LinearLayout.VERTICAL

        setPadding(
            20,
            10,
            20,
            10
        )

        minimumHeight =
            dialogHeight
    }

val scrollView =
    android.widget.ScrollView(this).apply {

        isFillViewport =
            false

        isVerticalScrollBarEnabled =
            true

        overScrollMode =
            android.view.View.OVER_SCROLL_ALWAYS

        layoutParams =
            android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
    }

val listContainer =
    android.widget.LinearLayout(this).apply {

        orientation =
            android.widget.LinearLayout.VERTICAL
    }

scrollView.addView(
    listContainer,
    android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
    )
)

val checkBoxes =
    mutableListOf<android.widget.CheckBox>()

streamList.forEach { url ->

    val checkBox =
        android.widget.CheckBox(this).apply {

            text =
                url

            textSize =
                13f

            setPadding(
                0,
                8,
                0,
                8
            )

            setOnCheckedChangeListener { _, isChecked ->

                if (isChecked) {

                    if (!selected.contains(url)) {

                        selected.add(
                            url
                        )
                    }

                } else {

                    selected.remove(
                        url
                    )
                }
            }
        }

    checkBoxes.add(
        checkBox
    )

    listContainer.addView(
        checkBox
    )

    val line =
        android.view.View(this).apply {

            setBackgroundColor(
                0xFFE0E0E0.toInt()
            )

            layoutParams =
                android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
        }

    listContainer.addView(
        line
    )
}

root.addView(
    scrollView
)

// =====================================
// FIXED BUTTON ROW
// =====================================

val buttonRow =
    android.widget.LinearLayout(this).apply {

        orientation =
            android.widget.LinearLayout.HORIZONTAL

        setPadding(
            0,
            10,
            0,
            0
        )

        layoutParams =
            android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
    }

val selectAllButton =
    android.widget.Button(this).apply {

        text =
            "SELECT\nALL"

        textSize =
            11f

        layoutParams =
            android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
    }

val saveButton =
    android.widget.Button(this).apply {

        text =
            "SAVE"

        textSize =
            12f

        layoutParams =
            android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
    }

val shareButton =
    android.widget.Button(this).apply {

        text =
            "SHARE"

        textSize =
            12f

        layoutParams =
            android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
    }

val copyButton =
    android.widget.Button(this).apply {

        text =
            "COPY"

        textSize =
            12f

        layoutParams =
            android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
    }

buttonRow.addView(
    selectAllButton
)

buttonRow.addView(
    saveButton
)

buttonRow.addView(
    shareButton
)

buttonRow.addView(
    copyButton
)

root.addView(
    buttonRow
)

val dialog =
    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Select Streams To Share")
        .setView(root)
        .show()

try {

    dialog.window?.setLayout(
        (resources.displayMetrics.widthPixels * 0.92f).toInt(),
        dialogHeight
    )

} catch (_: Throwable) {}

dialog.setCanceledOnTouchOutside(true)

dialog.setCancelable(true)

// =====================================
// SELECT ALL
// =====================================

selectAllButton.setOnClickListener {

    selected.clear()

    checkBoxes.forEachIndexed { index, checkBox ->

        checkBox.isChecked =
            true

        val url =
            streamList[index]

        if (!selected.contains(url)) {

            selected.add(
                url
            )
        }
    }

    Toast.makeText(
        this,
        "All selected",
        Toast.LENGTH_SHORT
    ).show()
}

// =====================================
// SAVE
// =====================================

saveButton.setOnClickListener {

    if (selected.isEmpty()) {

        Toast.makeText(
            this,
            "No streams selected",
            Toast.LENGTH_SHORT
        ).show()

        return@setOnClickListener
    }

    var savedCount =
        0

    selected.forEach { url ->

        try {

            addSavedChannel(
                url
            )

            savedCount++

        } catch (_: Throwable) {}
    }

    Toast.makeText(
        this,
        "Saved $savedCount channel(s)",
        Toast.LENGTH_SHORT
    ).show()
}

// =====================================
// COPY
// =====================================

copyButton.setOnClickListener {

    if (selected.isEmpty()) {

        Toast.makeText(
            this,
            "No streams selected",
            Toast.LENGTH_SHORT
        ).show()

        return@setOnClickListener
    }

    try {

        val text =
            selected.joinToString(
                "\n\n"
            )

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "streams",
                text
            )
        )

        Toast.makeText(
            this,
            "Copied",
            Toast.LENGTH_SHORT
        ).show()

    } catch (t: Throwable) {

        Log.e(
            "COPY_STREAMS",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Copy failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHARE
// =====================================

shareButton.setOnClickListener {

    if (selected.isEmpty()) {

        Toast.makeText(
            this,
            "No streams selected",
            Toast.LENGTH_SHORT
        ).show()

        return@setOnClickListener
    }

    try {

        val allUrls =
            selected.joinToString(
                "\n\n"
            )

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {

                type =
                    "text/plain"

                putExtra(
                    Intent.EXTRA_TEXT,
                    allUrls
                )
            }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Share Streams With"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "SHARE_STREAM",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Share failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}
}

// =====================================
// EXPORT M3U
// =====================================

binding.contentMain.exportM3u.setOnClickListener {

    try {

        val exportUrls =
            mutableListOf<String>()

        // =====================================
        // COLLECT VIDEO / LIVE / AUDIO
        // =====================================

        exportUrls.addAll(
            detectedStreams
        )

        exportUrls.addAll(
            detectedVideos
        )

        exportUrls.addAll(
            detectedAudio
        )

        if (bestStreamUrl.isNotBlank()) {

            exportUrls.add(
                bestStreamUrl
            )
        }

        if (bestLiveUrl.isNotBlank()) {

            exportUrls.add(
                bestLiveUrl
            )
        }

        if (youtubeWatchUrl.isNotBlank()) {

            exportUrls.add(
                youtubeWatchUrl
            )
        }

        if (youtubeDashVideoUrl.isNotBlank()) {

            exportUrls.add(
                youtubeDashVideoUrl
            )
        }

        if (youtubeDashAudioUrl.isNotBlank()) {

            exportUrls.add(
                youtubeDashAudioUrl
            )
        }
        
// =====================================
// ADD SAVED CHANNELS / FAVORITES
// =====================================

try {

    loadSavedChannels()

    savedChannels.forEach { channel ->

        if (
            channel.url.isNotBlank()
        ) {

            exportUrls.add(
                channel.url
            )
        }
    }

} catch (_: Throwable) {}

        // =====================================
        // CLEAN + FILTER EXPORTABLE ONLY
        // NO IMAGES
        // =====================================

        val streamList =
    exportUrls
        .map { url ->

            url
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim()
        }
        .filter { url ->

            url.isNotBlank() &&
                isExportableStream(
                    url
                )
        }
        .map { url ->

            val key =
                try {

                    val lower =
                        url.lowercase()

                    if (
                        lower.contains("googlevideo.com") ||
                        lower.contains("videoplayback")
                    ) {

                        val uri =
                            Uri.parse(url)

                        val id =
                            uri.getQueryParameter("id")
                                ?.substringBefore(".")
                                .orEmpty()

                        val itag =
                            uri.getQueryParameter("itag")
                                .orEmpty()

                        val mime =
                            uri.getQueryParameter("mime")
                                .orEmpty()

                        val source =
                            uri.getQueryParameter("source")
                                .orEmpty()

                        "googlevideo://$id/$itag/$mime/$source"

                    } else if (
                        lower.contains("youtube.com/watch")
                    ) {

                        val uri =
                            Uri.parse(url)

                        val v =
                            uri.getQueryParameter("v")
                                .orEmpty()

                        "youtube://watch/$v"

                    } else {

                        url
                            .substringBefore("#")
                            .trim()
                    }

                } catch (_: Throwable) {

                    url
                }

            key to url
        }
        .distinctBy { pair ->

            pair.first
        }
        .map { pair ->

            pair.second
        }

        if (streamList.isEmpty()) {

            binding.contentMain.result.append(
                "\n\nNO EXPORTABLE STREAMS FOUND\n"
            )

            Toast.makeText(
                this,
                "No exportable streams found",
                Toast.LENGTH_SHORT
            ).show()

            return@setOnClickListener
        }

        val sb =
            StringBuilder()

        sb.append("#EXTM3U\n\n")

        streamList
            .forEach { url ->

val lower =
    url.lowercase()

val savedChannel =
    try {

        savedChannels.firstOrNull { channel ->

            channel.url == url
        }

    } catch (_: Throwable) {

        null
    }

val channelName =
    savedChannel
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: buildChannelName(
            url
        )

val logoUrl =
    savedChannel
        ?.logo
        ?.takeIf { it.isNotBlank() }
        ?: buildLogoUrl(
            url
        )

val groupTitle =
    savedChannel
        ?.group
        ?.takeIf { it.isNotBlank() }
        ?: when {

            lower.contains(".mp3") ||
                lower.contains(".m4a") ||
                lower.contains(".aac") ||
                lower.contains(".opus") ||
                lower.contains(".wav") ||
                lower.contains(".ogg") ||
                lower.contains(".flac") ->
                "Audio"

            lower.contains(".mp4") ||
                lower.contains(".webm") ||
                lower.contains(".mkv") ||
                lower.contains(".mov") ||
                lower.contains(".avi") ||
                lower.contains(".3gp") ->
                "Static Videos"

            else ->
                "Live Streams"
        }

                sb.append(
                    "#EXTINF:-1 tvg-id=\"$channelName\" tvg-name=\"$channelName\" tvg-logo=\"$logoUrl\" group-title=\"$groupTitle\",$channelName\n"
                )

                sb.append(
                    url
                )

                sb.append(
                    "\n\n"
                )
            }

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {

                type =
                    "text/plain"

                putExtra(
                    Intent.EXTRA_TEXT,
                    sb.toString()
                )
            }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Export M3U"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "EXPORT_M3U",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Export failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================  
    // FILTER BUTTONS  
    // =====================================  

    binding.contentMain.btnAll.setOnClickListener {  

        showAllMedia()  
    }  

    binding.contentMain.btnVideos.setOnClickListener {  

        showVideos()  
    }  

    binding.contentMain.btnAudio.setOnClickListener {  

        showAudio()  
    }  
    
// =====================================
// SAVED CHANNELS
// =====================================

binding.contentMain.btnSavedChannels.setOnClickListener {

    showSavedChannelsDialog()
}

// =====================================
// CLEAR RESULTS + RESET DETECTION STATE
// =====================================

binding.contentMain.btnClear.setOnClickListener {

    // =====================================
    // CLEAR KOTLIN DETECTION LISTS
    // =====================================

    detectedStreams.clear()
    detectedVideos.clear()
    detectedImages.clear()
    detectedAudio.clear()
    detectedMasterStreams.clear()
    detectedChannels.clear()

    streamScores.clear()
    streamValidation.clear()
    streamHeaders.clear()
    streamTokens.clear()
    streamResolution.clear()
    streamInfoSnapshots.clear()
    streamHitCounter.clear()
    blobRelations.clear()

    // =====================================
    // CLEAR BEST / YOUTUBE / DASH STATE
    // =====================================

    bestStreamUrl = ""
    bestStreamScore = 0

    bestLiveUrl = ""
    bestLiveScore = 0

    youtubeWatchUrl = ""
    youtubeEmbedUrl = ""

    youtubeDashVideoUrl = ""
    youtubeDashAudioUrl = ""
    youtubeDashVideoItag = ""
    youtubeDashAudioItag = ""

    bestVideoItag = 0
    bestAudioItag = 0

    dashVideoMap.clear()
    dashAudioMap.clear()

    liveLocked = false
    lockedStreamId = ""

    liveHeartbeatMap.clear()

    lastSelectedUrl = ""
    
    lastDeepScanTime =
    0L

    // =====================================
    // CLEAR UI
    // =====================================

    binding.contentMain.result.text = ""

    // =====================================
    // RESET WEBVIEW JS DETECTION MEMORY
    // =====================================

    try {

        binding.contentMain.webview.evaluateJavascript(
            """

(function() {

    try {

        window.__gelMediaResults =
            [];

        window.__gelLastResults =
            [];

        window.__gelDetectedUrls =
            {};

        window.__gelScanCounter =
            0;

        console.log(
            "GEL_DETECTION_STATE_CLEARED"
        );

    } catch(e) {}

})();

            """.trimIndent(),
            null
        )

    } catch (_: Throwable) {}

    Toast.makeText(
        this,
        "Cleared",
        Toast.LENGTH_SHORT
    ).show()
}

// =====================================
// RESULT CLICK = COPY SELECTED TEXT
// =====================================

binding.contentMain.result.setOnClickListener {

    val start =

    try {

        kotlin.math.max(
            0,
            binding.contentMain.result.selectionStart
        )

    } catch (_: Throwable) {

        0
    }

val end =

    try {

        kotlin.math.max(
            start,
            binding.contentMain.result.selectionEnd
        )

    } catch (_: Throwable) {

        start
    }

val fullText =
    binding.contentMain.result
        .text
        ?.toString()
        .orEmpty()

val selectedText =

    try {

        fullText
            .substring(start, end)
            .trim()

    } catch (_: Throwable) {

        ""
    }

    val textToCopy =

        if (selectedText.isNotBlank()) {
            selectedText
        } else {
            lastSelectedUrl
        }

    if (textToCopy.isBlank()) {
        return@setOnClickListener
    }

    val clipboard =

        getSystemService(
            CLIPBOARD_SERVICE
        ) as ClipboardManager

    clipboard.setPrimaryClip(

        ClipData.newPlainText(
            "stream",
            textToCopy
        )
    )

    Toast.makeText(
        this,
        "Copied",
        Toast.LENGTH_SHORT
    ).show()
}

// =====================================
// RESULT LONG PRESS MENU
// AlertDialog version — never goes off-screen
// =====================================

binding.contentMain.result.setOnLongClickListener { _ ->

    val textView =
        binding.contentMain.result

    val text =
        textView.text
            ?.toString()
            .orEmpty()

    if (text.isBlank()) {
        return@setOnLongClickListener true
    }

    val layout =
        textView.layout
            ?: return@setOnLongClickListener true

    val x =
        (
            lastTouchX -
                textView.totalPaddingLeft +
                textView.scrollX
        ).toInt()

    val y =
        (
            lastTouchY -
                textView.totalPaddingTop +
                textView.scrollY
        ).toInt()

    val line =
        layout.getLineForVertical(
            y
        )

    val offset =
        layout.getOffsetForHorizontal(
            line,
            x.toFloat()
        )

    val regex =
        "(https?://[^\\s\"'<>]+)"
            .toRegex()

    var selectedUrl =
        ""

    regex.findAll(text)
        .forEach { match ->

            if (
                offset >= match.range.first &&
                offset <= match.range.last
            ) {

                selectedUrl =
                    match.value
                        .trim()
                        .trimEnd(',')
                        .trimEnd(';')
                        .trimEnd(')')
                        .trimEnd(']')
                        .trimEnd('}')
            }
        }

    if (selectedUrl.isBlank()) {

        selectedUrl =
            regex.find(text)
                ?.value
                ?.trim()
                ?.trimEnd(',')
                ?.trimEnd(';')
                ?.trimEnd(')')
                ?.trimEnd(']')
                ?.trimEnd('}')
                ?: ""
    }

    selectedUrl =
        cleanDetectedUrl(
            selectedUrl
        )

    if (
        selectedUrl.isBlank() ||
        !isExportableStream(
            selectedUrl
        )
    ) {
        return@setOnLongClickListener true
    }

    lastSelectedUrl =
        selectedUrl

    val selectedSnapshot =
        streamInfoSnapshots[selectedUrl]
            ?: streamInfoSnapshots[
                selectedUrl.substringBefore("?")
            ]

    fun getMenuBestQualityStream(): String {

        return try {

            val selectedClean =
                cleanDetectedUrl(
                    selectedUrl
                )

            if (
                selectedClean.isNotBlank() &&
                isExportableStream(
                    selectedClean
                ) &&
                isBestQualityTrackStream(
                    selectedClean
                )
            ) {
                return selectedClean
            }

            val snapshotBest =
                cleanDetectedUrl(
                    selectedSnapshot?.bestStream.orEmpty()
                )

            if (
                snapshotBest.isNotBlank() &&
                isExportableStream(
                    snapshotBest
                ) &&
                isBestQualityTrackStream(
                    snapshotBest
                )
            ) {
                return snapshotBest
            }

            val globalBest =
                cleanDetectedUrl(
                    getCleanBestStreamUrl()
                )

            if (
                globalBest.isNotBlank() &&
                isExportableStream(
                    globalBest
                ) &&
                isBestQualityTrackStream(
                    globalBest
                )
            ) {
                return globalBest
            }

            ""

        } catch (_: Throwable) {

            ""
        }
    }

    fun getMenuBestStableStream(): String {

        return try {

            val selectedClean =
                cleanDetectedUrl(
                    selectedUrl
                )

            if (
                selectedClean.isNotBlank() &&
                isExportableStream(
                    selectedClean
                ) &&
                !isBestQualityTrackStream(
                    selectedClean
                ) &&
                (
                    isStablePlaylistStream(
                        selectedClean
                    ) ||
                        isLowerPriorityHlsVariant(
                            selectedClean
                        ) ||
                        selectedClean.contains(
                            ".m3u8",
                            true
                        )
                    )
            ) {
                return selectedClean
            }

            val snapshotBestLive =
                cleanDetectedUrl(
                    selectedSnapshot?.bestLive.orEmpty()
                )

            if (
                snapshotBestLive.isNotBlank() &&
                isExportableStream(
                    snapshotBestLive
                ) &&
                !isBestQualityTrackStream(
                    snapshotBestLive
                )
            ) {
                return snapshotBestLive
            }

            val snapshotBest =
                cleanDetectedUrl(
                    selectedSnapshot?.bestStream.orEmpty()
                )

            if (
                snapshotBest.isNotBlank() &&
                isExportableStream(
                    snapshotBest
                ) &&
                !isBestQualityTrackStream(
                    snapshotBest
                )
            ) {
                return snapshotBest
            }

            ""

        } catch (_: Throwable) {

            ""
        }
    }

    val menuBestQualityStream =
        getMenuBestQualityStream()

    val menuBestStableStream =
        getMenuBestStableStream()

    val actions =
        mutableListOf<String>()

    actions.add(
        "OPEN PLAYER"
    )

    actions.add(
        "SHARE URL"
    )

    actions.add(
        "COPY URL"
    )

    actions.add(
        "SAVE CHANNEL"
    )

    if (menuBestQualityStream.isNotBlank()) {

        actions.add(
            "OPEN BEST STREAM"
        )

        actions.add(
            "COPY BEST STREAM"
        )
    }

    if (menuBestStableStream.isNotBlank()) {

        actions.add(
            "OPEN BEST STABLE STREAM"
        )

        actions.add(
            "COPY BEST STABLE STREAM"
        )
    }

    if (youtubeWatchUrl.isNotBlank()) {

        actions.add(
            "OPEN YOUTUBE WATCH"
        )

        actions.add(
            "COPY YOUTUBE WATCH"
        )
    }

    if (
        youtubeDashVideoUrl.isNotBlank() &&
        youtubeDashAudioUrl.isNotBlank()
    ) {

        actions.add(
            "COPY DASH PAIR"
        )

        actions.add(
            "SHARE DASH PAIR"
        )
    }

    androidx.appcompat.app.AlertDialog.Builder(this)

        .setTitle(
            "Stream Actions"
        )

        .setItems(
            actions.toTypedArray()
        ) { dialogInterface, which ->

            val finalUrl =
                cleanDetectedUrl(
                    lastSelectedUrl
                )

            if (
                finalUrl.isBlank() ||
                !isExportableStream(
                    finalUrl
                )
            ) {
                return@setItems
            }

            when (
                actions[which]
            ) {

                "OPEN PLAYER" -> {

                    try {

                        val urlToOpen =
                            finalUrl.trim()

                        if (
                            urlToOpen.contains(
                                "youtube.com/watch",
                                true
                            ) ||
                            urlToOpen.contains(
                                "youtu.be/",
                                true
                            ) ||
                            urlToOpen.contains(
                                "youtube.com/live",
                                true
                            ) ||
                            urlToOpen.contains(
                                "youtube.com/c/",
                                true
                            )
                        ) {

                            Toast.makeText(
                                this,
                                "YouTube requires local resolver / Termux",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW
                                ).apply {

                                    data =
                                        Uri.parse(
                                            urlToOpen
                                        )

                                    addCategory(
                                        Intent.CATEGORY_BROWSABLE
                                    )

                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    )
                                }

                            startActivity(
                                Intent.createChooser(
                                    intent,
                                    "Open YouTube Link"
                                )
                            )

                            return@setItems
                        }

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW
                            ).apply {

                                setDataAndType(
                                    Uri.parse(
                                        urlToOpen
                                    ),
                                    "video/*"
                                )

                                addCategory(
                                    Intent.CATEGORY_BROWSABLE
                                )

                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                intent,
                                "Open With"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "No compatible player",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "SHARE URL" -> {

                    try {

                        val urlToShare =
                            finalUrl.trim()

                        if (urlToShare.isBlank()) {

                            Toast.makeText(
                                this,
                                "No URL selected",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val shareIntent =
                            Intent(
                                Intent.ACTION_SEND
                            ).apply {

                                type =
                                    "text/plain"

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    urlToShare
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share Stream"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Share failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "COPY URL" -> {

                    try {

                        val urlToCopy =
                            finalUrl.trim()

                        if (urlToCopy.isBlank()) {

                            Toast.makeText(
                                this,
                                "No URL selected",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val clipboard =
                            getSystemService(
                                CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "stream",
                                urlToCopy
                            )
                        )

                        Toast.makeText(
                            this,
                            "URL copied",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Copy failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "SAVE CHANNEL" -> {

                    try {

                        addSavedChannel(
                            finalUrl
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Save failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "OPEN BEST STREAM" -> {

                    try {

                        val bestQualityUrl =
                            getMenuBestQualityStream()

                        if (bestQualityUrl.isBlank()) {

                            Toast.makeText(
                                this,
                                "No best quality stream",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW
                            ).apply {

                                setDataAndType(
                                    Uri.parse(
                                        bestQualityUrl
                                    ),
                                    "video/*"
                                )

                                addCategory(
                                    Intent.CATEGORY_BROWSABLE
                                )

                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                intent,
                                "Open Best Stream"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Cannot open best stream",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "COPY BEST STREAM" -> {

                    try {

                        val bestQualityUrl =
                            getMenuBestQualityStream()

                        if (bestQualityUrl.isBlank()) {

                            Toast.makeText(
                                this,
                                "No best quality stream",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val clipboard =
                            getSystemService(
                                CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "best_quality_stream",
                                bestQualityUrl
                            )
                        )

                        Toast.makeText(
                            this,
                            "Best stream copied",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Copy failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "OPEN BEST STABLE STREAM" -> {

                    try {

                        val bestStableUrl =
                            getMenuBestStableStream()

                        if (bestStableUrl.isBlank()) {

                            Toast.makeText(
                                this,
                                "No best stable stream",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW
                            ).apply {

                                setDataAndType(
                                    Uri.parse(
                                        bestStableUrl
                                    ),
                                    "video/*"
                                )

                                addCategory(
                                    Intent.CATEGORY_BROWSABLE
                                )

                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                intent,
                                "Open Best Stable Stream"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Cannot open best stable stream",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "COPY BEST STABLE STREAM" -> {

                    try {

                        val bestStableUrl =
                            getMenuBestStableStream()

                        if (bestStableUrl.isBlank()) {

                            Toast.makeText(
                                this,
                                "No best stable stream",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val clipboard =
                            getSystemService(
                                CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "best_stable_stream",
                                bestStableUrl
                            )
                        )

                        Toast.makeText(
                            this,
                            "Best stable stream copied",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Copy failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "OPEN YOUTUBE WATCH" -> {

                    try {

                        val ytUrl =
                            cleanDetectedUrl(
                                youtubeWatchUrl
                            )

                        if (ytUrl.isBlank()) {

                            Toast.makeText(
                                this,
                                "No YouTube watch URL",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        Toast.makeText(
                            this,
                            "Opening YouTube watch URL",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW
                            ).apply {

                                data =
                                    Uri.parse(
                                        ytUrl
                                    )

                                addCategory(
                                    Intent.CATEGORY_BROWSABLE
                                )

                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                intent,
                                "Open YouTube Link"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Cannot open YouTube link",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "COPY YOUTUBE WATCH" -> {

                    try {

                        val ytUrl =
                            cleanDetectedUrl(
                                youtubeWatchUrl
                            )

                        if (ytUrl.isBlank()) {

                            Toast.makeText(
                                this,
                                "No YouTube watch URL",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setItems
                        }

                        val clipboard =
                            getSystemService(
                                CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "youtube_watch",
                                ytUrl
                            )
                        )

                        Toast.makeText(
                            this,
                            "YouTube link copied",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Copy failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "COPY DASH PAIR" -> {

                    try {

                        val dashPairText =
                            """
YOUTUBE DASH PAIR

VIDEO ITAG:
$youtubeDashVideoItag

VIDEO URL:
${cleanDetectedUrl(youtubeDashVideoUrl)}

AUDIO ITAG:
$youtubeDashAudioItag

AUDIO URL:
${cleanDetectedUrl(youtubeDashAudioUrl)}
                            """.trimIndent()

                        val clipboard =
                            getSystemService(
                                CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "youtube_dash_pair",
                                dashPairText
                            )
                        )

                        Toast.makeText(
                            this,
                            "DASH pair copied",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Copy failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                "SHARE DASH PAIR" -> {

                    try {

                        val dashPairText =
                            """
YOUTUBE DASH PAIR

VIDEO ITAG:
$youtubeDashVideoItag

VIDEO URL:
${cleanDetectedUrl(youtubeDashVideoUrl)}

AUDIO ITAG:
$youtubeDashAudioItag

AUDIO URL:
${cleanDetectedUrl(youtubeDashAudioUrl)}
                            """.trimIndent()

                        val shareIntent =
                            Intent(
                                Intent.ACTION_SEND
                            ).apply {

                                type =
                                    "text/plain"

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    dashPairText
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share DASH Pair"
                            )
                        )

                    } catch (_: Throwable) {

                        Toast.makeText(
                            this,
                            "Share failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            dialogInterface.dismiss()
        }

        .setNegativeButton(
            "CLOSE",
            null
        )

        .show()

    true

} // END result long press listener

} // END onCreate()

// =====================================
// DAILYMOTION CLEAN EXPORT HELPER
// Keeps clean Dailymotion streams / removes ad-session-gdpr junk
// =====================================

private fun cleanDailymotionUrlForExport(
    url: String
): String {

    try {

        val clean: String =
            cleanDetectedUrl(
                url
            ).trim()

        val lower: String =
            clean.lowercase()

        if (
            !lower.contains("dailymotion.com") &&
            !lower.contains("dmxleo.dailymotion.com") &&
            !lower.contains("cdndirector.dailymotion.com")
        ) {
            return clean
        }

        // CUT only temporary signed cdndirector links
        if (
            lower.contains("cdndirector.dailymotion.com") &&
            (
                lower.contains("sec=") ||
                lower.contains("dmts=") ||
                lower.contains("dmv1st=")
            )
        ) {
            return ""
        }

        // KEEP clean cdndirector live stream
        if (
            lower.contains("cdndirector.dailymotion.com") &&
            lower.contains("/cdn/live/video/") &&
            lower.contains(".m3u8")
        ) {
            return clean.substringBefore("?")
                .substringBefore("#")
                .trim()
        }

        // Hard ad/session/consent/error junk
        if (
            lower.contains("error=") ||
            lower.contains("reader_gdpr") ||
            lower.contains("gdpr_binary_consent") ||
            lower.contains("gdpr_comes_from_infopack") ||
            lower.contains("reader_us_privacy") ||
            lower.contains("cookie_sync") ||
            lower.contains("vmap") ||
            lower.contains("monetization") ||
            lower.contains("ciid=") ||
            lower.contains("cidx=") ||
            lower.contains("sidx=") ||
            lower.contains("vididx=") ||
            lower.contains("imal=") ||
            lower.contains("3pcb=") ||
            lower.contains("rap=") ||
            lower.contains("apo=") ||
            lower.contains("pdm=") ||
            lower.contains("pbm=") ||
            lower.contains("bs=") ||
            lower.contains("rid=")
        ) {

            val base: String =
                clean.substringBefore("?")
                    .substringBefore("#")
                    .trim()

            return if (
                base.contains(
                    "/cdn/manifest/video/",
                    true
                ) &&
                base.contains(
                    ".m3u8",
                    true
                )
            ) {
                base
            } else {
                ""
            }
        }

        // KEEP clean Dailymotion manifest base
        if (
            lower.contains("/cdn/manifest/video/") &&
            lower.contains(".m3u8")
        ) {
            return clean.substringBefore("?")
                .substringBefore("#")
                .trim()
        }

        return clean

    } catch (_: Throwable) {

        return ""
    }
}

// =====================================
// BUTTON OUTLINE STYLE
// Keeps gray button background + bold black text
// Adds only colored thick outline
// =====================================

private fun applyButtonOutline(
    button: Button,
    borderColor: Int
) {
    try {

        val strokeWidthPx =
            5

        val cornerRadiusPx =
            8f

        val drawable =
            android.graphics.drawable.GradientDrawable().apply {

                shape =
                    android.graphics.drawable.GradientDrawable.RECTANGLE

                // Keep button background gray
                setColor(
                    android.graphics.Color.rgb(
                        224,
                        224,
                        224
                    )
                )

                // Thicker outline
                setStroke(
                    strokeWidthPx,
                    borderColor
                )

                cornerRadius =
                    cornerRadiusPx
            }

        button.background =
            drawable

        // Keep text black + bold
        button.setTextColor(
            android.graphics.Color.BLACK
        )

        button.typeface =
            android.graphics.Typeface.DEFAULT_BOLD

    } catch (_: Throwable) {}
}

// =====================================
// FLOATING STOP AUTO SCAN BUTTON
// =====================================

private fun showAutoScanStopButton() {

    if (autoScanStopButton != null) {
        return
    }
    
    val button =
        Button(this).apply {

            text =
                "STOP AUTO SCAN"

            setBackgroundColor(
                android.graphics.Color.RED
            )

            setTextColor(
                android.graphics.Color.WHITE
            )

            textSize =
                13f

            setOnClickListener {
                stopAutoScanChannels()
            }
        }

    val params =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {

            gravity =
                Gravity.BOTTOM or Gravity.END

            setMargins(
                0,
                0,
                24,
                120
            )
        }

    val root =
        window.decorView as FrameLayout

    root.addView(
        button,
        params
    )

    autoScanStopButton =
        button
}

private fun hideAutoScanStopButton() {

    try {

        val root =
            window.decorView as FrameLayout

        autoScanStopButton?.let { button ->
            root.removeView(button)
        }

    } catch (_: Throwable) {}

    autoScanStopButton =
        null
}

// =====================================
// STOP AUTO CHANNEL SCAN
// =====================================

private fun stopAutoScanChannels() {

    autoScanRunning =
        false

    hideAutoScanStopButton()

    window.clearFlags(
        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )

    try {

        val activeWebView =
            popupWebView
                ?: binding.contentMain.webview

        activeWebView.stopLoading()

    } catch (_: Throwable) {}

    binding.contentMain.result.append(
        """

AUTO CHANNEL SCAN STOPPED

Scanned:
$autoScanIndex / ${autoScanCandidates.size}

Collected channels:
${autoScanResults.size}

Collected streams:
${autoScanResults.values.sumOf { it.size }}

────────────────────

        """.trimIndent()
    )

    Toast.makeText(
        this,
        "Auto scan stopped",
        Toast.LENGTH_SHORT
    ).show()
}

// =====================================
// SCAN CHANNEL CANDIDATES ONLY
// Safe mode: does NOT click anything
// Uses pagination pages when available
// =====================================

private fun scanChannelCandidatesOnly() {

    try {

        if (cloudflareChallengeActive) {

            Toast.makeText(
                this,
                "Wait for page verification first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        binding.contentMain.result.append(
            """

CHANNEL CANDIDATE SCAN:
Collecting pages...

────────────────────

            """.trimIndent()
        )

        collectCountryPaginationPages { pages ->

            runOnUiThread {

                val pageList =
                    if (pages.isNotEmpty()) {
                        pages
                    } else {
                        listOf(
                            AutoScanPage(
                                title = "CURRENT PAGE",
                                href = binding.contentMain.webview.url.orEmpty()
                            )
                        )
                    }

                binding.contentMain.result.append(
    """

CHANNEL CANDIDATE SCAN:
Pages found:
${pageList.size}

────────────────────

    """.trimIndent()
)

if (pageList.size > 10) {

    val estimatedChannels =
        pageList.size * 15

    lastChannelCandidateCount =
        estimatedChannels

    binding.contentMain.result.append(
        """

CHANNEL CANDIDATE SCAN:
Large paginated list detected.

Pages:
${pageList.size}

Estimated content:
About 15 channels per page

Estimated total channels:
~$estimatedChannels

Preview scan skipped to avoid freezing the app.

Use Auto Scan only when you want full collection.

────────────────────

        """.trimIndent()
    )

    Toast.makeText(
        this,
        "Pages: ${pageList.size} / estimated ~$estimatedChannels channels",
        Toast.LENGTH_LONG
    ).show()

    return@runOnUiThread
}

collectCandidatesPreviewFromPages(
    pageList
)
            }
        }

    } catch (t: Throwable) {

        Log.e(
            "CHANNEL_CANDIDATE_SCAN",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Candidate scan failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// COLLECT CANDIDATES PREVIEW FROM PAGES
// Opens pagination pages and lists only /channel/ candidates
// Does NOT click channel pages
// =====================================

private fun collectCandidatesPreviewFromPages(
    pages: List<AutoScanPage>
) {

    try {

        val activeWebView =
            popupWebView
                ?: binding.contentMain.webview

        val allCandidates =
            mutableListOf<AutoScanCandidate>()

        fun scanPageAt(
            index: Int
        ) {

            if (index >= pages.size) {

                val finalCandidates =
                    allCandidates
                        .distinctBy { item ->
                            item.href.lowercase()
                        }

                lastChannelCandidateCount =
                    finalCandidates.size

                val builder =
                    StringBuilder()

                builder.append(
                    "\nCHANNEL CANDIDATES FOUND:\n"
                )

                builder.append(
                    finalCandidates.size
                )

                builder.append(
                    "\n\n"
                )

                finalCandidates.forEachIndexed { i, item ->

                    builder.append(
                        i + 1
                    )

                    builder.append(
                        ". "
                    )

                    builder.append(
                        item.title.ifBlank {
                            "NO TITLE"
                        }
                    )

                    builder.append(
                        "\n"
                    )

                    builder.append(
                        item.href
                    )

                    builder.append(
                        "\n\n"
                    )
                }

                builder.append(
                    "────────────────────\n"
                )

                binding.contentMain.result.append(
                    builder.toString()
                )

                Toast.makeText(
                    this,
                    "Candidates: ${finalCandidates.size}",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val page =
                pages[index]

            try {

                activeWebView.stopLoading()

                activeWebView.loadUrl(
                    page.href,
                    mapOf(
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache"
                    )
                )

            } catch (_: Throwable) {

                scanPageAt(
                    index + 1
                )

                return
            }

            binding.contentMain.webview.postDelayed(
                {

                    collectAutoScanCandidates { candidates ->

                        runOnUiThread {

                            allCandidates.addAll(
                                candidates
                            )

                            binding.contentMain.result.append(
                                """

CANDIDATE PAGE:
${index + 1}/${pages.size}

Found:
${candidates.size}

Total:
${allCandidates.distinctBy { it.href.lowercase() }.size}

────────────────────

                                """.trimIndent()
                            )

                            scanPageAt(
                                index + 1
                            )
                        }
                    }
                },
                2500
            )
        }

        scanPageAt(
            0
        )

    } catch (t: Throwable) {

        Log.e(
            "CHANNEL_CANDIDATE_SCAN",
            "page preview failed",
            t
        )

        Toast.makeText(
            this,
            "Candidate scan failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// COLLECT COUNTRY / LIST PAGINATION PAGES
// Stable same-page pagination:
// 1) Query pagination: ?page=2
// 2) Hash pagination:  #page/2/filter/alphaaz
// Does NOT guess parent categories
// Does NOT scan other sections
// =====================================

private fun collectCountryPaginationPages(
    onReady: (List<AutoScanPage>) -> Unit
) {

    try {

        val activeWebView =
            popupWebView
                ?: binding.contentMain.webview

        activeWebView.evaluateJavascript(
            """

(function() {

    try {

        var pages =
            [];

        var seen =
            {};

        var currentUrl =
            window.location.href || "";

        var current =
            new URL(currentUrl);

        var currentHost =
            current.host || "";

        var currentPath =
            current.pathname || "";

        function cleanText(value) {

            try {

                return String(value || "")
                    .replace(/\s+/g, " ")
                    .trim();

            } catch(e) {

                return "";
            }
        }

        function addFinalPage(title, href) {

            try {

                if (!href) {
                    return;
                }

                var u =
                    new URL(href, currentUrl);

                if ((u.host || "") !== currentHost) {
                    return;
                }

                if ((u.pathname || "") !== currentPath) {
                    return;
                }

                var key =
                    u.href.toLowerCase();

                if (seen[key]) {
                    return;
                }

                seen[key] =
                    true;

                pages.push({
                    title: title || "PAGE",
                    href: u.href
                });

            } catch(e) {}
        }

        function addQueryPage(title, href) {

            try {

                var u =
                    new URL(href, currentUrl);

                if ((u.host || "") !== currentHost) {
                    return;
                }

                if ((u.pathname || "") !== currentPath) {
                    return;
                }

                var pageValue =
                    u.searchParams.get("page") || "";

                if (!pageValue) {
                    return;
                }

                if (!/^[0-9]+$/.test(pageValue)) {
                    return;
                }

                addFinalPage(
                    title || ("PAGE " + pageValue),
                    u.href
                );

            } catch(e) {}
        }

        function getHashPageNumber(hashValue) {

            try {

                var hash =
                    String(hashValue || "");

                var m =
                    hash.match(/#page\/([0-9]+)/i);

                if (!m || !m[1]) {
                    return "";
                }

                return m[1];

            } catch(e) {

                return "";
            }
        }

        function addHashPage(title, href) {

            try {

                var u =
                    new URL(href, currentUrl);

                if ((u.host || "") !== currentHost) {
                    return;
                }

                if ((u.pathname || "") !== currentPath) {
                    return;
                }

                var pageValue =
                    getHashPageNumber(
                        u.hash || ""
                    );

                if (!pageValue) {
                    return;
                }

                addFinalPage(
                    title || ("PAGE " + pageValue),
                    u.href
                );

            } catch(e) {}
        }

        // =====================================
        // COLLECT VISIBLE PAGINATION LINKS
        // =====================================

        document
            .querySelectorAll("a[href]")
            .forEach(function(a) {

                try {

                    var href =
                        a.href || "";

                    var text =
                        cleanText(
                            a.innerText ||
                            a.textContent ||
                            a.getAttribute("aria-label") ||
                            a.getAttribute("title") ||
                            a.getAttribute("rel") ||
                            ""
                        );

                    addQueryPage(
                        text,
                        href
                    );

                    addHashPage(
                        text,
                        href
                    );

                } catch(e) {}
            });

        // =====================================
        // CURRENT PAGE
        // =====================================

        addFinalPage(
            "CURRENT PAGE",
            currentUrl
        );

        // =====================================
        // EXPAND QUERY PAGINATION RANGE
        // Teleon-style: ?page=N
        // =====================================

        try {

            var maxQueryPage =
                1;

            document
                .querySelectorAll("a[href]")
                .forEach(function(a) {

                    try {

                        var u =
                            new URL(
                                a.href || "",
                                currentUrl
                            );

                        if ((u.host || "") !== currentHost) {
                            return;
                        }

                        if ((u.pathname || "") !== currentPath) {
                            return;
                        }

                        var pageValue =
                            parseInt(
                                u.searchParams.get("page") || "0",
                                10
                            );

                        if (
                            !isNaN(pageValue) &&
                            pageValue > maxQueryPage
                        ) {

                            maxQueryPage =
                                pageValue;
                        }

                    } catch(e) {}
                });

            if (maxQueryPage > 1) {

                for (
                    var p = 1;
                    p <= maxQueryPage && p <= 80;
                    p++
                ) {

                    try {

                        var pageUrl =
                            new URL(
                                currentUrl
                            );

                        pageUrl.searchParams.set(
                            "page",
                            String(p)
                        );

                        addFinalPage(
                            "PAGE " + p,
                            pageUrl.href
                        );

                    } catch(e) {}
                }
            }

        } catch(e) {}

// =====================================
        // EXPAND HASH PAGINATION RANGE
        // VivaLive-style: #page/N/filter/alphaaz
        // Supports visible pages + LAST button » / >> / last
        // =====================================

        try {

            var maxHashPage =
                1;

            var hashTemplate =
                "";

            document
                .querySelectorAll("a[href]")
                .forEach(function(a) {

                    try {

                        var href =
                            a.href || "";

                        var text =
                            cleanText(
                                a.innerText ||
                                a.textContent ||
                                a.getAttribute("aria-label") ||
                                a.getAttribute("title") ||
                                a.getAttribute("rel") ||
                                ""
                            ).toLowerCase();

                        var rel =
                            String(
                                a.getAttribute("rel") || ""
                            ).toLowerCase();

                        var u =
                            new URL(
                                href,
                                currentUrl
                            );

                        if ((u.host || "") !== currentHost) {
                            return;
                        }

                        if ((u.pathname || "") !== currentPath) {
                            return;
                        }

                        var pageValueRaw =
                            getHashPageNumber(
                                u.hash || ""
                            );

                        if (!pageValueRaw) {
                            return;
                        }

                        var pageValue =
                            parseInt(
                                pageValueRaw,
                                10
                            );

                        if (
                            isNaN(pageValue) ||
                            pageValue < 1
                        ) {
                            return;
                        }

                        var looksLast =
                            rel.indexOf("last") >= 0 ||
                            text.indexOf("last") >= 0 ||
                            text.indexOf("»") >= 0 ||
                            text.indexOf(">>") >= 0 ||
                            text.indexOf("last page") >= 0 ||
                            text.indexOf("final") >= 0;

                        if (
                            looksLast ||
                            pageValue > maxHashPage
                        ) {

                            maxHashPage =
                                pageValue;
                        }

                        if (!hashTemplate) {

                            hashTemplate =
                                u.hash || "";
                        }

                    } catch(e) {}
                });

            if (
                maxHashPage > 1 &&
                hashTemplate
            ) {

                for (
                    var hp = 1;
                    hp <= maxHashPage && hp <= 120;
                    hp++
                ) {

                    try {

                        var pageUrl2 =
                            new URL(
                                currentUrl
                            );

                        pageUrl2.search =
                            "";

                        pageUrl2.hash =
                            hashTemplate.replace(
                                /#page\/[0-9]+/i,
                                "#page/" + hp
                            );

                        addFinalPage(
                            "PAGE " + hp,
                            pageUrl2.href
                        );

                    } catch(e) {}
                }
            }

        } catch(e) {}

        return JSON.stringify(
            pages
        );

    } catch(e) {

        return JSON.stringify([]);
    }

})();

            """.trimIndent()
        ) { jsResult ->

            try {

                val cleaned =
                    jsResult
                        ?.removePrefix("\"")
                        ?.removeSuffix("\"")
                        ?.replace("\\\\", "\\")
                        ?.replace("\\\"", "\"")
                        ?.replace("\\n", "\n")
                        ?.trim()
                        .orEmpty()

                val array =
                    org.json.JSONArray(
                        cleaned
                    )

                val parsed =
                    mutableListOf<AutoScanPage>()

                for (i in 0 until array.length()) {

                    val obj =
                        array.optJSONObject(i)
                            ?: continue

                    val title =
                        obj.optString(
                            "title",
                            "PAGE ${i + 1}"
                        ).trim()

                    val href =
                        obj.optString(
                            "href",
                            ""
                        ).trim()

                    if (
                        href.isBlank() ||
                        !href.startsWith("http", true)
                    ) {
                        continue
                    }

                    parsed.add(
                        AutoScanPage(
                            title = title,
                            href = href
                        )
                    )
                }

                onReady(
                    parsed.distinctBy { page ->
                        page.href.lowercase()
                    }
                )

            } catch (t: Throwable) {

                Log.e(
                    "COUNTRY_PAGES",
                    "parse failed",
                    t
                )

                onReady(
                    emptyList()
                )
            }
        }

    } catch (t: Throwable) {

        Log.e(
            "COUNTRY_PAGES",
            "collect failed",
            t
        )

        onReady(
            emptyList()
        )
    }
}

// CLICK WATCH / PLAY BUTTON IF PRESENT
// Returns true only if a real button was clicked
// =====================================

private fun clickWatchOrPlayButtonIfPresent(
    view: WebView?,
    onDone: (Boolean) -> Unit
) {

    try {

        if (view == null) {
            onDone(false)
            return
        }

        view.evaluateJavascript(
            """
(function() {

    try {

        var words =
            /\b(play|watch|start|live|stream)\b/i;

        var badWords =
            /cookie|privacy|accept|reject|login|sign in|subscribe|share|facebook|twitter|instagram|youtube|menu|home|contact|terms|policy|close|back|next|previous|search|language|settings|download|app/i;

        function visible(el) {

            try {

                var s =
                    window.getComputedStyle(el);

                var r =
                    el.getBoundingClientRect();

                return (
                    s.display !== "none" &&
                    s.visibility !== "hidden" &&
                    s.opacity !== "0" &&
                    r.width > 30 &&
                    r.height > 15
                );

            } catch(e) {

                return false;
            }
        }

        function textOf(el) {

            try {

                return String(
                    (el.innerText || "") + " " +
                    (el.textContent || "") + " " +
                    (el.getAttribute("aria-label") || "") + " " +
                    (el.getAttribute("title") || "")
                )
                    .replace(/\s+/g, " ")
                    .trim();

            } catch(e) {

                return "";
            }
        }

        var nodes =
            Array.prototype.slice.call(
                document.querySelectorAll(
                    "button, a[href], [role='button'], [onclick]"
                )
            );

        for (var i = 0; i < nodes.length; i++) {

            var el =
                nodes[i];

            if (!visible(el)) {
                continue;
            }

            var txt =
                textOf(el);

            var lower =
                txt.toLowerCase();

            if (!words.test(lower)) {
                continue;
            }

            if (badWords.test(lower)) {
                continue;
            }

            try {

                el.scrollIntoView({
                    block: "center",
                    inline: "center"
                });

            } catch(e) {}

            try {

                el.click();

                return "CLICKED: " + txt.substring(0, 80);

            } catch(e) {}
        }

        return "NO_BUTTON";

    } catch(e) {

        return "ERROR: " + e;
    }

})();
            """.trimIndent()
        ) { result ->

            val clicked =
                result
                    ?.contains(
                        "CLICKED:",
                        true
                    ) == true

            Log.e(
                "AUTO_SCAN_PLAY_CLICK",
                result ?: ""
            )

            onDone(clicked)
        }

    } catch (t: Throwable) {

        Log.e(
            "AUTO_SCAN_PLAY_CLICK",
            "failed",
            t
        )

        onDone(false)
    }
}

// =====================================
// COLLECT CHANNEL CANDIDATES FOR AUTO SCAN
// Safe: collects channel links + media cards
// Generic DOM logic:
// CATEGORY / MENU / SIDEBAR / NAV = skip
// ARTICLE / POST / CARD / VIDEO / THUMBNAIL = candidate
// SORT / RATING / STAR blocks = skip
// =====================================

private fun collectAutoScanCandidates(
    onReady: (List<AutoScanCandidate>) -> Unit
) {

    try {

        val activeWebView =
            popupWebView
                ?: binding.contentMain.webview

        activeWebView.evaluateJavascript(
            """

(function() {

    try {

        var candidates =
            [];

        var seen =
            {};

        var badWords =
            /cookie|privacy|gdpr|consent|accept|reject|login|sign in|signin|subscribe|share|facebook|twitter|instagram|telegram|whatsapp|youtube|search|home|contact|terms|policy|advert|ads|close/i;

        var sortRatingWords =
            /sort by|comments|rating|ratings|views|newest|random|star|stars|vote|votes|average|out of 5/i;

        function cleanText(value) {

            try {

                return String(value || "")
                    .replace(/\s+/g, " ")
                    .trim();

            } catch(e) {

                return "";
            }
        }

        function normalizeTitle(value) {

            try {

                var text =
                    cleanText(value);

                // Fix duplicated titles like:
                // "Boca Chica Boca Chica"
                var parts =
                    text.split(" ");

                if (
                    parts.length % 2 === 0 &&
                    parts.length >= 2
                ) {

                    var half =
                        parts.length / 2;

                    var left =
                        parts.slice(0, half).join(" ");

                    var right =
                        parts.slice(half).join(" ");

                    if (
                        left.toLowerCase() === right.toLowerCase()
                    ) {
                        text =
                            left;
                    }
                }

                return cleanText(text);

            } catch(e) {

                return cleanText(value);
            }
        }

        function isBadTitle(title) {

            try {

                var t =
                    cleanText(title).toLowerCase();

                if (!t) {
                    return true;
                }

                if (t.length < 2) {
                    return true;
                }

                if (t.length > 180) {
                    return true;
                }

                if (sortRatingWords.test(t)) {
                    return true;
                }
                
if (
    t.indexOf("plan servidor") >= 0 ||
    t.indexOf("licencia") >= 0 ||
    t.indexOf("mensual") >= 0 ||
    t.indexOf("hosting") >= 0 ||
    t.indexOf("hostlagarto") >= 0 ||
    t === "videos" ||
    t === "youtube" ||
    t === "emisoras con video"
) {
    return true;
}

                if (
                    /^[0-9]+ star$/i.test(t) ||
                    /^[0-9]+ stars$/i.test(t)
                ) {
                    return true;
                }

                if (
                    t === "star" ||
                    t === "stars" ||
                    t === "rating" ||
                    t === "ratings" ||
                    t === "sort" ||
                    t === "sort by" ||
                    t === "comments" ||
                    t === "views" ||
                    t === "newest" ||
                    t === "random"
                ) {
                    return true;
                }

                return false;

            } catch(e) {

                return true;
            }
        }

        function isVisible(el) {

            try {

                if (!el) {
                    return false;
                }

                var style =
                    window.getComputedStyle(el);

                if (
                    style.display === "none" ||
                    style.visibility === "hidden" ||
                    style.opacity === "0"
                ) {
                    return false;
                }

                var rect =
                    el.getBoundingClientRect();

                if (
                    rect.width < 20 ||
                    rect.height < 12
                ) {
                    return false;
                }

                return true;

            } catch(e) {

                return false;
            }
        }

        function getText(el) {

            try {

                var text =
                    "";

                text += " " + (el.innerText || "");
                text += " " + (el.textContent || "");
                text += " " + (el.getAttribute("title") || "");
                text += " " + (el.getAttribute("aria-label") || "");
                text += " " + (el.getAttribute("data-title") || "");
                text += " " + (el.getAttribute("data-name") || "");
                text += " " + (el.getAttribute("alt") || "");

                try {

                    var img =
                        el.querySelector &&
                        el.querySelector("img");

                    if (img) {

                        var imgAlt =
                            cleanText(
                                img.getAttribute("alt") ||
                                img.getAttribute("title") ||
                                ""
                            );

                        if (!isBadTitle(imgAlt)) {

                            text += " " + imgAlt;
                        }
                    }

                } catch(e) {}

                return normalizeTitle(text);

            } catch(e) {

                return "";
            }
        }

        function getBestTitle(el) {

            try {

                var selectors =
                    [
                        "h1",
                        "h2",
                        "h3",
                        "h4",
                        ".title",
                        ".entry-title",
                        ".post-title",
                        ".video-title",
                        ".channel-title",
                        "[class*='title']",
                        "[class*='Title']"
                    ];

                for (
                    var i = 0;
                    i < selectors.length;
                    i++
                ) {

                    var t =
                        el.querySelector &&
                        el.querySelector(
                            selectors[i]
                        );

                    if (t) {

                        var tx =
                            normalizeTitle(
                                t.innerText ||
                                t.textContent ||
                                t.getAttribute("title") ||
                                ""
                            );

                        if (!isBadTitle(tx)) {
                            return tx;
                        }
                    }
                }

                var img =
                    el.querySelector &&
                    el.querySelector("img");

                if (img) {

                    var alt =
                        normalizeTitle(
                            img.getAttribute("alt") ||
                            img.getAttribute("title") ||
                            ""
                        );

                    if (!isBadTitle(alt)) {
                        return alt;
                    }
                }

                var own =
                    normalizeTitle(
                        getText(el)
                    );

                if (
                    own.length > 220
                ) {

                    own =
                        own.substring(
                            0,
                            220
                        );
                }

                if (isBadTitle(own)) {
                    return "";
                }

                return normalizeTitle(own);

            } catch(e) {

                return "";
            }
        }

        function getHref(el) {

            try {

                if (el.href) {
                    return String(el.href || "").trim();
                }

                var a =
                    el.closest &&
                    el.closest("a[href]");

                if (a && a.href) {
                    return String(a.href || "").trim();
                }

                var innerA =
                    el.querySelector &&
                    el.querySelector("a[href]");

                if (innerA && innerA.href) {
                    return String(innerA.href || "").trim();
                }

                var dataUrl =
                    el.getAttribute("data-url") ||
                    el.getAttribute("data-href") ||
                    el.getAttribute("data-src") ||
                    el.getAttribute("data-stream") ||
                    "";

                return String(dataUrl || "").trim();

            } catch(e) {

                return "";
            }
        }

        function getSignal(el) {

            try {

                if (!el) {
                    return "";
                }

                var signal =
                    "";

                signal += " " + String(el.tagName || "");
                signal += " " + String(el.className || "");
                signal += " " + String(el.id || "");
                signal += " " + String(el.getAttribute("role") || "");
                signal += " " + String(el.getAttribute("aria-label") || "");
                signal += " " + String(el.getAttribute("title") || "");

                return signal.toLowerCase();

            } catch(e) {

                return "";
            }
        }

        function getDeepSignal(el) {

            try {

                if (!el) {
                    return "";
                }

                var signal =
                    getSignal(el);

                signal += " " + String(el.innerText || "").toLowerCase();
                signal += " " + String(el.innerHTML || "").toLowerCase();

                return signal;

            } catch(e) {

                return "";
            }
        }

        function isSortOrRatingBlock(el) {

            try {

                var signal =
                    getDeepSignal(el);

                if (
                    signal.indexOf("sort by") >= 0 ||
                    signal.indexOf("comments") >= 0 && signal.indexOf("rating") >= 0 ||
                    signal.indexOf("newest") >= 0 && signal.indexOf("random") >= 0 ||
                    signal.indexOf("out of 5") >= 0 ||
                    signal.indexOf("average") >= 0 && signal.indexOf("votes") >= 0 ||
                    signal.indexOf("1 star") >= 0 ||
                    signal.indexOf("2 star") >= 0 ||
                    signal.indexOf("3 star") >= 0 ||
                    signal.indexOf("4 star") >= 0 ||
                    signal.indexOf("5 star") >= 0
                ) {
                    return true;
                }

                return false;

            } catch(e) {

                return false;
            }
        }

        function isCategoryOrNavigationBlock(el) {

            try {

                var current =
                    el;

                var depth =
                    0;

                while (
                    current &&
                    depth < 7 &&
                    current !== document.body
                ) {

                    var signal =
                        getSignal(current);

                    var deep =
                        getDeepSignal(current);

                    if (
                        signal.indexOf("cat-item") >= 0 ||
                        signal.indexOf("category-list") >= 0 ||
                        signal.indexOf("categories") >= 0 ||
                        signal.indexOf("categoria") >= 0 ||
                        signal.indexOf("categoría") >= 0 ||
                        signal.indexOf("taxonomy") >= 0 ||
                        signal.indexOf("archive") >= 0 ||
                        signal.indexOf("tagcloud") >= 0 ||
                        signal.indexOf("tag-cloud") >= 0 ||
                        signal.indexOf("sidebar") >= 0 ||
                        signal.indexOf("widget") >= 0 ||
                        signal.indexOf("breadcrumb") >= 0 ||
                        signal.indexOf("pagination") >= 0 ||
                        signal.indexOf("pager") >= 0 ||
                        signal.indexOf("page-numbers") >= 0 ||
                        signal.indexOf("nav-links") >= 0 ||
                        signal.indexOf("navbar") >= 0 ||
                        signal.indexOf("navigation") >= 0 ||
                        signal.indexOf("main-menu") >= 0 ||
                        signal.indexOf("menu-item") >= 0 ||
                        signal.indexOf("menu ") >= 0 ||
                        signal.indexOf(" menu") >= 0 ||
                        deep.indexOf("select category") >= 0 ||
                        deep.indexOf("selecciona categoria") >= 0
                    ) {
                        return true;
                    }

                    current =
                        current.parentElement;

                    depth++;
                }

                return false;

            } catch(e) {

                return false;
            }
        }

        function findMediaCard(el) {

            try {

                var current =
                    el;

                var depth =
                    0;

                while (
                    current &&
                    depth < 7 &&
                    current !== document.body
                ) {

                    if (isSortOrRatingBlock(current)) {

                        current =
                            current.parentElement;

                        depth++;

                        continue;
                    }

                    var signal =
                        getSignal(current);

                    var deep =
                        getDeepSignal(current);

                    var hasContainerSignal =
                        signal.indexOf("article") >= 0 ||
                        signal.indexOf("post") >= 0 ||
                        signal.indexOf("entry") >= 0 ||
                        signal.indexOf("card") >= 0 ||
                        signal.indexOf("channel") >= 0 ||
                        signal.indexOf("video") >= 0 ||
                        signal.indexOf("media") >= 0 ||
                        signal.indexOf("item") >= 0;

                    var hasImageOrPlay =
                        deep.indexOf("<img") >= 0 ||
                        deep.indexOf("thumbnail") >= 0 ||
                        deep.indexOf("thumb") >= 0 ||
                        deep.indexOf("poster") >= 0 ||
                        deep.indexOf("play") >= 0 ||
                        deep.indexOf("watch") >= 0 ||
                        deep.indexOf("video") >= 0 ||
                        deep.indexOf("player") >= 0;

                    var hasPostSignal =
                        deep.indexOf("posted by") >= 0 ||
                        deep.indexOf("published") >= 0;

                    var title =
                        getBestTitle(current);

                    if (
                        hasContainerSignal &&
                        title.length >= 2 &&
                        !isBadTitle(title) &&
                        (
                            hasImageOrPlay ||
                            hasPostSignal
                        )
                    ) {
                        return current;
                    }

                    current =
                        current.parentElement;

                    depth++;
                }

                return null;

            } catch(e) {

                return null;
            }
        }

        function makeRowHref(el) {

    try {

        var target =
            null;

        // Prefer real clickable/media target inside card
        try {

            target =
                el.querySelector(
                    "a[href], img, button, [role='button'], [onclick], .play, [class*='play'], [class*='Play']"
                );

        } catch(e) {}

        if (!target) {
            target =
                el;
        }

        var rect =
            target.getBoundingClientRect();

        if (
            rect.width < 10 ||
            rect.height < 10
        ) {

            rect =
                el.getBoundingClientRect();
        }

        var x =
            Math.floor(
                rect.left + rect.width / 2
            );

        var y =
            Math.floor(
                rect.top + rect.height / 2 + window.scrollY
            );

        return "gel-row://" + encodeURIComponent(window.location.href) +
            "?x=" + x +
            "&y=" + y;

    } catch(e) {

        return "";
    }
}

        function addCandidate(title, href) {

            try {

                title =
                    normalizeTitle(title);

                href =
                    String(href || "").trim();

                if (
                    isBadTitle(title) ||
                    href.length < 5
                ) {
                    return;
                }

                var lowerHref =
                    href.toLowerCase();

                if (
                    lowerHref.indexOf("facebook.com") >= 0 ||
                    lowerHref.indexOf("twitter.com") >= 0 ||
                    lowerHref.indexOf("instagram.com") >= 0 ||
                    lowerHref.indexOf("telegram") >= 0 ||
                    lowerHref.indexOf("whatsapp") >= 0 ||
                    lowerHref.indexOf("wa.me") >= 0 ||
                    lowerHref.indexOf("mailto:") >= 0 ||
                    lowerHref.indexOf("tel:") >= 0 ||
                    lowerHref.indexOf("?page=") >= 0 ||
                    lowerHref.indexOf("&page=") >= 0 ||
                    lowerHref.indexOf("#page/") >= 0
                ) {
                    return;
                }

                var key =
                    (title + "|" + href)
                        .toLowerCase();

                if (seen[key]) {
                    return;
                }

                seen[key] =
                    true;

                candidates.push({
                    title: title.substring(0, 120),
                    href: href.substring(0, 500)
                });

            } catch(e) {}
        }

        function looksLikeChannel(el, text, href) {

            try {

                text =
                    normalizeTitle(text);

                if (isBadTitle(text)) {
                    return false;
                }

                if (isSortOrRatingBlock(el)) {
                    return false;
                }

                var cls =
                    String(el.className || "").toLowerCase();

                var id =
                    String(el.id || "").toLowerCase();

                var role =
                    String(el.getAttribute("role") || "").toLowerCase();

                var combined =
                    (text + " " + href + " " + cls + " " + id + " " + role)
                        .toLowerCase();

                var mediaCard =
                    findMediaCard(el);

                var insideMediaCard =
                    !!mediaCard;

                var insideCategoryBlock =
                    isCategoryOrNavigationBlock(el);

                // =====================================
                // HARD REJECT — SOCIAL / CONTACT
                // =====================================

                if (
                    href.indexOf("facebook.com") >= 0 ||
                    href.indexOf("twitter.com") >= 0 ||
                    href.indexOf("instagram.com") >= 0 ||
                    href.indexOf("telegram") >= 0 ||
                    href.indexOf("whatsapp") >= 0 ||
                    href.indexOf("wa.me") >= 0 ||
                    href.indexOf("mailto:") >= 0 ||
                    href.indexOf("tel:") >= 0
                ) {
                    return false;
                }

                // =====================================
                // HARD REJECT — PAGINATION / ARCHIVE
                // =====================================

                if (
                    href.indexOf("/country/") >= 0 ||
                    href.indexOf("/countries/") >= 0 ||
                    href.indexOf("/category/") >= 0 ||
                    href.indexOf("/categories/") >= 0 ||
                    href.indexOf("?page=") >= 0 ||
                    href.indexOf("&page=") >= 0 ||
                    href.indexOf("#page/") >= 0 ||
                    href.indexOf("per-page=") >= 0 ||
                    combined.indexOf("last") >= 0 ||
                    combined.indexOf("next") >= 0 ||
                    combined.indexOf("previous") >= 0
                ) {
                    return false;
                }

                // =====================================
                // CATEGORY / MENU / SIDEBAR FILTER
                // =====================================

                if (
                    insideCategoryBlock &&
                    !insideMediaCard
                ) {
                    return false;
                }

                if (
                    badWords.test(combined) &&
                    !insideMediaCard
                ) {
                    return false;
                }

                // =====================================
                // DIRECT STREAM LINKS
                // =====================================

                if (
                    href &&
                    href !== "#" &&
                    (
                        href.indexOf(".m3u8") >= 0 ||
                        href.indexOf(".mpd") >= 0 ||
                        href.indexOf(".mp4") >= 0 ||
                        href.indexOf(".ts") >= 0
                    ) &&
                    text.length >= 2 &&
                    text.length <= 220
                ) {
                    return true;
                }

                // =====================================
                // CLEAR CHANNEL / WATCH / PLAYER LINKS
                // =====================================

                if (
                    href &&
                    href !== "#" &&
                    text.length >= 2 &&
                    text.length <= 180 &&
                    (
                        href.indexOf("/channel/") >= 0 ||
                        href.indexOf("/channels/") >= 0 ||
                        href.indexOf("/watch/") >= 0 ||
                        href.indexOf("/player/") >= 0 ||
                        href.indexOf("/live/") >= 0
                    )
                ) {
                    return true;
                }

                // =====================================
                // /TV/ LINKS
                // Accept only if inside real media card.
                // Prevents category folders from being scanned.
                // =====================================

                if (
                    href &&
                    href !== "#" &&
                    href.indexOf("/tv/") >= 0 &&
                    text.length >= 2 &&
                    text.length <= 180 &&
                    insideMediaCard
                ) {
                    return true;
                }

                // =====================================
                // MEDIA CARD ACCEPT
                // =====================================

                if (
                    insideMediaCard &&
                    text.length >= 2 &&
                    text.length <= 240
                ) {
                    return true;
                }

                // =====================================
                // GRID / EPG ROW ACCEPT
                // =====================================

                if (
                    text.length >= 2 &&
                    text.length <= 220 &&
                    (
                        combined.indexOf("live") >= 0 ||
                        combined.indexOf("channel") >= 0 ||
                        combined.indexOf("channels") >= 0 ||
                        combined.indexOf("epg") >= 0 ||
                        combined.indexOf("guide") >= 0 ||
                        combined.indexOf("program") >= 0 ||
                        combined.indexOf("station") >= 0 ||
                        combined.indexOf("tv") >= 0
                    ) &&
                    !insideCategoryBlock &&
                    !badWords.test(combined)
                ) {
                    return true;
                }

                // =====================================
                // BUTTON / CLICKABLE FALLBACKS
                // =====================================

                if (
                    role === "button" &&
                    text.length >= 2 &&
                    text.length <= 120 &&
                    !insideCategoryBlock
                ) {
                    return true;
                }

                if (
                    !href &&
                    el.onclick &&
                    text.length >= 2 &&
                    text.length <= 120 &&
                    !insideCategoryBlock
                ) {
                    return true;
                }

                return false;

            } catch(e) {

                return false;
            }
        }

        // =====================================
        // PASS 1 — MEDIA CARD COLLECTOR
        // Finds real cards even when category links exist everywhere.
        // =====================================

        var cardNodes =
    Array.prototype.slice.call(
        document.querySelectorAll(
            [
                "article",
                ".post",
                ".entry",
                ".card",
                ".item",
                ".box",
                ".grid",
                ".video",
                ".channel",
                ".tv-channel",
                ".media",
                ".thumb",
                ".thumbnail",
                ".image",

                "[class*='post']",
                "[class*='Post']",
                "[class*='entry']",
                "[class*='Entry']",
                "[class*='card']",
                "[class*='Card']",
                "[class*='item']",
                "[class*='Item']",
                "[class*='box']",
                "[class*='Box']",
                "[class*='grid']",
                "[class*='Grid']",
                "[class*='video']",
                "[class*='Video']",
                "[class*='channel']",
                "[class*='Channel']",
                "[class*='thumb']",
                "[class*='Thumb']",
                "[class*='thumbnail']",
                "[class*='Thumbnail']",
                "[class*='image']",
                "[class*='Image']"
            ].join(",")
        )
    );

        cardNodes.forEach(function(card) {

    try {

        if (!isVisible(card)) {
            return;
        }

        if (isSortOrRatingBlock(card)) {
            return;
        }

        var deep =
            getDeepSignal(card);

        var hasImage =
            deep.indexOf("<img") >= 0 ||
            deep.indexOf("thumbnail") >= 0 ||
            deep.indexOf("thumb") >= 0 ||
            deep.indexOf("poster") >= 0;

        var hasClickSignal =
            deep.indexOf("play") >= 0 ||
            deep.indexOf("watch") >= 0 ||
            deep.indexOf("video") >= 0 ||
            deep.indexOf("player") >= 0 ||
            deep.indexOf("posted by") >= 0 ||
            deep.indexOf("published") >= 0;

        if (
            !hasImage &&
            !hasClickSignal
        ) {
            return;
        }

        var title =
            getBestTitle(card);

        if (isBadTitle(title)) {

            try {

                var link =
                    card.querySelector &&
                    card.querySelector("a[href]");

                if (link) {

                    title =
                        normalizeTitle(
                            link.innerText ||
                            link.textContent ||
                            link.getAttribute("title") ||
                            link.getAttribute("aria-label") ||
                            ""
                        );
                }

            } catch(e) {}
        }

        if (isBadTitle(title)) {

            try {

                var img =
                    card.querySelector &&
                    card.querySelector("img");

                if (img) {

                    title =
                        normalizeTitle(
                            img.getAttribute("alt") ||
                            img.getAttribute("title") ||
                            ""
                        );
                }

            } catch(e) {}
        }

        if (isBadTitle(title)) {
            return;
        }

        var href =
            makeRowHref(card);

        addCandidate(
            title,
            href
        );

    } catch(e) {}
});

        // =====================================
        // PASS 2 — LINK / BUTTON COLLECTOR
        // =====================================

        var nodes =
            Array.prototype.slice.call(
                document.querySelectorAll(
                    [
                        "a[href]",
                        "button",
                        "[role='button']",
                        "[onclick]",
                        "[data-url]",
                        "[data-href]",
                        "[data-src]",
                        "[data-stream]",
                        "[data-channel]",
                        "[data-channel-id]",
                        "[data-name]",
                        "[data-title]",

                        ".channel-row",
                        ".channel-item",
                        ".channel-list-item",
                        ".epg-row",
                        ".guide-row",
                        ".program-row",
                        ".live-row",
                        ".grid-row",
                        ".list-row",

                        "[class*='channel']",
                        "[class*='Channel']",
                        "[class*='epg']",
                        "[class*='EPG']",
                        "[class*='guide']",
                        "[class*='Guide']",
                        "[class*='program']",
                        "[class*='Program']",
                        "[class*='station']",
                        "[class*='Station']",
                        "[class*='live']",
                        "[class*='Live']",
                        "[class*='row']",
                        "[class*='Row']"
                    ].join(",")
                )
            );

        nodes.forEach(function(el) {

            try {

                if (!isVisible(el)) {
                    return;
                }

                if (isSortOrRatingBlock(el)) {
                    return;
                }

                var text =
                    getText(el);

                var href =
                    getHref(el);

                if (
                    text.length < 2 &&
                    href.length < 5
                ) {
                    return;
                }

                if (!looksLikeChannel(el, text, href)) {
                    return;
                }

                if (
                    !href ||
                    href === "#"
                ) {

                    var mediaCard =
                        findMediaCard(el);

                    if (mediaCard) {
                        href =
                            makeRowHref(mediaCard);
                    }
                }

                addCandidate(
                    text,
                    href
                );

            } catch(e) {}
        });

        return JSON.stringify(
            candidates.slice(0, 250)
        );

    } catch(e) {

        return JSON.stringify([]);
    }

})();
            """.trimIndent()
        ) { jsResult ->

            try {

                val cleaned =
                    jsResult
                        ?.removePrefix("\"")
                        ?.removeSuffix("\"")
                        ?.replace("\\\\", "\\")
                        ?.replace("\\\"", "\"")
                        ?.replace("\\n", "\n")
                        ?.trim()
                        .orEmpty()

                val array =
                    org.json.JSONArray(
                        cleaned
                    )

                val parsed =
                    mutableListOf<AutoScanCandidate>()

                for (i in 0 until array.length()) {

                    val obj =
                        array.optJSONObject(i)
                            ?: continue

                    val title =
                        obj.optString(
                            "title",
                            ""
                        )
                            .trim()
                            .ifBlank {
                                "CHANNEL ${i + 1}"
                            }

                    val href =
                        obj.optString(
                            "href",
                            ""
                        ).trim()

                    if (
                        href.isBlank() ||
                        (
                            !href.startsWith("http", true) &&
                                !href.startsWith("gel-row://", true)
                            )
                    ) {
                        continue
                    }

                    val lowerTitle =
                        title.lowercase()

if (
    lowerTitle.contains("sort by") ||
    lowerTitle == "1 star" ||
    lowerTitle == "2 star" ||
    lowerTitle == "3 star" ||
    lowerTitle == "4 star" ||
    lowerTitle == "5 star" ||
    lowerTitle.contains("out of 5") ||
    lowerTitle.contains("average") ||
    lowerTitle.contains("votes") ||
    lowerTitle.contains("rating") ||
    lowerTitle == "videos" ||
    lowerTitle == "youtube" ||
    lowerTitle == "emisoras con video" ||
    lowerTitle.contains("plan servidor") ||
    lowerTitle.contains("licencia") ||
    lowerTitle.contains("mensual") ||
    lowerTitle.contains("hosting") ||
    lowerTitle.contains("hostlagarto")
) {
    continue
}

                    parsed.add(
                        AutoScanCandidate(
                            title = title,
                            href = href
                        )
                    )
                }

                val channelLike =
                    parsed.filter { item ->

                        val lower =
                            item.href.lowercase()

                        (
                            lower.startsWith("gel-row://") ||
                                lower.contains("/channel/") ||
                                lower.contains("/channels/") ||
                                lower.contains("/watch/") ||
                                lower.contains("/live/") ||                                
                                lower.contains("/player/") ||
                                lower.contains(".m3u8") ||
                                lower.contains(".mpd") ||
                                lower.contains(".mp4")
                            ) &&
                            !lower.contains("/country/") &&
                            !lower.contains("/countries/") &&
                            !lower.contains("/category/") &&
                            !lower.contains("/categories/") &&
                            !lower.contains("?page=") &&
                            !lower.contains("&page=") &&
                            !lower.contains("#page/")
                    }

                val finalList =
                    if (channelLike.isNotEmpty()) {
                        channelLike
                    } else {
                        parsed
                    }
                        .distinctBy { item ->

    if (
        item.href.startsWith(
            "gel-row://",
            true
        )
    ) {
        item.title
            .lowercase()
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    } else {
        item.href.lowercase()
    }
}
                        .take(
                            autoScanMaxCandidates
                        )

                onReady(
                    finalList
                )

            } catch (t: Throwable) {

                Log.e(
                    "AUTO_SCAN",
                    "candidate parse failed",
                    t
                )

                onReady(
                    emptyList()
                )
            }
        }

    } catch (t: Throwable) {

        Log.e(
            "AUTO_SCAN",
            "candidate collect failed",
            t
        )

        onReady(
            emptyList()
        )
    }
}

// =====================================
// COLLECT CURRENT PLAYABLE STREAMS
// For auto scan result comparison
// =====================================

private fun collectCurrentPlayableStreamsForAutoScan(): List<String> {

    val all =
        mutableListOf<String>()

    try {

        all.addAll(
            detectedStreams
        )

        all.addAll(
            detectedVideos
        )

        all.addAll(
            detectedAudio
        )

        all.addAll(
            detectedMasterStreams
        )

        all.addAll(
            streamInfoSnapshots.keys
        )

        all.addAll(
            streamSources.keys
        )

        all.addAll(
            streamValidation.keys
        )

        if (bestStreamUrl.isNotBlank()) {
            all.add(
                bestStreamUrl
            )
        }

        if (bestLiveUrl.isNotBlank()) {
            all.add(
                bestLiveUrl
            )
        }

        if (youtubeWatchUrl.isNotBlank()) {
            all.add(
                youtubeWatchUrl
            )
        }

        if (youtubeDashVideoUrl.isNotBlank()) {
            all.add(
                youtubeDashVideoUrl
            )
        }

        if (youtubeDashAudioUrl.isNotBlank()) {
            all.add(
                youtubeDashAudioUrl
            )
        }

    } catch (_: Throwable) {}

    return all
        .map { item ->
            cleanDetectedUrl(
                item
            )
        }
        .filter { item ->

            item.isNotBlank() &&
                item.startsWith(
                    "http",
                    true
                ) &&
                isExportableStream(
                    item
                )
        }
        .distinctBy { item ->
            item.lowercase()
        }
}

// =====================================
// START AUTO CHANNEL SCAN
// With country pagination support
// =====================================

private fun startAutoScanChannels() {

    try {

        if (autoScanRunning) {
            Toast.makeText(
                this,
                "Auto scan already running",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (cloudflareChallengeActive) {
            Toast.makeText(
                this,
                "Wait for verification first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        window.addFlags(
    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
)

showAutoScanStopButton()

        binding.contentMain.result.append(
            """

AUTO CHANNEL SCAN:
Collecting country pages...

────────────────────

            """.trimIndent()
        )

        collectCountryPaginationPages { pages ->

            runOnUiThread {

                val pageList =
                    if (pages.isNotEmpty()) {
                        pages
                    } else {
                        listOf(
                            AutoScanPage(
                                title = "CURRENT PAGE",
                                href = binding.contentMain.webview.url.orEmpty()
                            )
                        )
                    }

                binding.contentMain.result.append(
                    """

AUTO CHANNEL SCAN:
Country pages found:
${pageList.size}

${pageList.joinToString("\n") { it.href }}

────────────────────

                    """.trimIndent()
                )

                collectCandidatesFromCountryPages(
                    pageList
                )
            }
        }

    } catch (t: Throwable) {

        autoScanRunning =
            false
            
window.clearFlags(
    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
)

        Log.e(
            "AUTO_SCAN",
            "start failed",
            t
        )
    }
}

// =====================================
// COLLECT CANDIDATES FROM ALL COUNTRY PAGES
// Opens each pagination page only when needed.
// If current WebView is already on the target page,
// it scans the existing DOM without reloading.
// Retry once when candidates are 0.
// =====================================

private fun collectCandidatesFromCountryPages(
    pages: List<AutoScanPage>
) {

    try {

        val activeWebView =
            popupWebView
                ?: binding.contentMain.webview

        val allCandidates =
            mutableListOf<AutoScanCandidate>()

        fun normalizePageUrl(
            value: String
        ): String {

            return try {

                value
                    .trim()
                    .removeSuffix("/")
                    .lowercase()

            } catch (_: Throwable) {

                value.trim().lowercase()
            }
        }

        fun scanPageAt(
            index: Int
        ) {

            if (index >= pages.size) {

                val finalCandidates =
                    allCandidates
                        .distinctBy { item ->
                            item.href.lowercase()
                        }
                        .take(
                            autoScanMaxCandidates
                        )

                if (finalCandidates.isEmpty()) {

                    binding.contentMain.result.append(
                        """

AUTO CHANNEL SCAN:
No channel candidates found across country pages.

────────────────────

                        """.trimIndent()
                    )

                    Toast.makeText(
                        this,
                        "No channel candidates found",
                        Toast.LENGTH_SHORT
                    ).show()

                    return
                }

                autoScanCandidates.clear()

                autoScanCandidates.addAll(
                    finalCandidates
                )

                autoScanResults.clear()

                autoScanIndex =
                    0

                autoScanRunning =
                    true

                binding.contentMain.result.append(
                    """

AUTO CHANNEL SCAN STARTED:
Country pages:
${pages.size}

Channel candidates:
${autoScanCandidates.size}

────────────────────

                    """.trimIndent()
                )

                scanNextAutoChannel()

                return
            }

            val page =
                pages[index]

            binding.contentMain.result.append(
                """

SCAN COUNTRY PAGE:
${index + 1}/${pages.size}
${page.href}

────────────────────

                """.trimIndent()
            )

            fun collectCurrentPageCandidates(
                retry: Boolean
            ) {

                collectAutoScanCandidates { candidates ->

                    runOnUiThread {

                        if (
                            candidates.isEmpty() &&
                            retry
                        ) {

                            binding.contentMain.result.append(
                                """

COUNTRY PAGE CANDIDATES:
0

Retrying DOM scan once...

────────────────────

                                """.trimIndent()
                            )

                            binding.contentMain.webview.postDelayed(
                                {
                                    collectCurrentPageCandidates(
                                        false
                                    )
                                },
                                2200
                            )

                            return@runOnUiThread
                        }

                        allCandidates.addAll(
                            candidates
                        )

                        binding.contentMain.result.append(
                            """

COUNTRY PAGE CANDIDATES:
${candidates.size}

TOTAL SO FAR:
${allCandidates.distinctBy { it.href.lowercase() }.size}

────────────────────

                            """.trimIndent()
                        )

                        scanPageAt(
                            index + 1
                        )
                    }
                }
            }

            val currentUrl =
                activeWebView.url.orEmpty()

            val samePage =
                normalizePageUrl(
                    currentUrl
                ) == normalizePageUrl(
                    page.href
                )

            if (samePage) {

                binding.contentMain.result.append(
                    """

AUTO CHANNEL SCAN:
Current page already loaded.
Scanning existing DOM.

────────────────────

                    """.trimIndent()
                )

                binding.contentMain.webview.postDelayed(
                    {
                        collectCurrentPageCandidates(
                            true
                        )
                    },
                    700
                )

                return
            }

            try {

                activeWebView.stopLoading()

                activeWebView.loadUrl(
                    page.href,
                    mapOf(
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache"
                    )
                )

            } catch (_: Throwable) {

                scanPageAt(
                    index + 1
                )

                return
            }

            binding.contentMain.webview.postDelayed(
                {
                    collectCurrentPageCandidates(
                        true
                    )
                },
                5500
            )
        }

        scanPageAt(
            0
        )

    } catch (t: Throwable) {

        autoScanRunning =
            false

        Log.e(
            "AUTO_SCAN",
            "collect pages failed",
            t
        )
    }
}

// =====================================
// SCAN NEXT CHANNEL CANDIDATE
// Opens each candidate URL or clicks gel-row candidate
// =====================================

private fun scanNextAutoChannel() {

    try {

        if (!autoScanRunning) {
            return
        }

        if (autoScanIndex >= autoScanCandidates.size) {

            finishAutoScanChannels()
            return
        }

        val candidate =
            autoScanCandidates[autoScanIndex]
            
// =====================================
// RESET PER-CANDIDATE STREAM STATE
// Prevents previous channel best stream/logs leaking
// into the next scanned channel.
// =====================================

try {

    bestStreamUrl =
        ""

    bestLiveUrl =
        ""

    youtubeWatchUrl =
        ""

    youtubeDashVideoUrl =
        ""

    youtubeDashAudioUrl =
        ""

} catch (_: Throwable) {}      

        autoScanKnownBefore.clear()

        autoScanKnownBefore.addAll(
            collectCurrentPlayableStreamsForAutoScan()
                .map { item ->
                    item.lowercase()
                }
        )

        val activeWebView =
            popupWebView
                ?: binding.contentMain.webview

        binding.contentMain.result.append(
            """

AUTO SCAN:
${autoScanIndex + 1}/${autoScanCandidates.size}
${candidate.title}
${candidate.href}

────────────────────

            """.trimIndent()
        )

        // =====================================
        // GEL ROW CANDIDATE
        // gel-row://ENCODED_PAGE_URL?x=123&y=456
        // Do NOT load gel-row:// as URL.
        // Load original page if needed, then click x/y.
        // =====================================

        if (
            candidate.href.startsWith(
                "gel-row://",
                true
            )
        ) {

            try {

                val raw =
                    candidate.href.removePrefix(
                        "gel-row://"
                    )

                val qIndex =
                    raw.indexOf("?")

                val encodedPageUrl =
                    if (qIndex >= 0) {
                        raw.substring(
                            0,
                            qIndex
                        )
                    } else {
                        raw
                    }

                val pageUrl =
                    Uri.decode(
                        encodedPageUrl
                    )

                val rowUri =
                    Uri.parse(
                        candidate.href
                    )

                val x =
                    rowUri.getQueryParameter("x")
                        ?.toIntOrNull()
                        ?: -1

                val y =
                    rowUri.getQueryParameter("y")
                        ?.toIntOrNull()
                        ?: -1

                if (
                    pageUrl.isBlank() ||
                    !pageUrl.startsWith("http", true) ||
                    x < 0 ||
                    y < 0
                ) {

                    binding.contentMain.result.append(
                        """

AUTO SCAN:
Invalid gel-row candidate.
Skipping.

────────────────────

                        """.trimIndent()
                    )

                    autoScanIndex++

                    binding.contentMain.webview.postDelayed(
                        {
                            scanNextAutoChannel()
                        },
                        700
                    )

                    return
                }

                try {

                    binding.contentMain.urlInput.setText(
                        pageUrl
                    )

                    binding.contentMain.urlInput.setSelection(
                        0
                    )

                    liveUrlInputText =
                        pageUrl

                } catch (_: Throwable) {}

                val currentUrl =
                    activeWebView.url.orEmpty()

                val needsLoad =
                    !currentUrl.equals(
                        pageUrl,
                        true
                    )

                if (needsLoad) {

                    try {

                        activeWebView.stopLoading()

                        activeWebView.loadUrl(
                            pageUrl,
                            mapOf(
                                "Cache-Control" to "no-cache",
                                "Pragma" to "no-cache"
                            )
                        )

                    } catch (_: Throwable) {

                        autoScanIndex++

                        binding.contentMain.webview.postDelayed(
                            {
                                scanNextAutoChannel()
                            },
                            900
                        )

                        return
                    }
                }

                val loadDelay =
                    if (needsLoad) {
                        4200L
                    } else {
                        900L
                    }

                binding.contentMain.webview.postDelayed(
                    {

                        try {

                            if (
                                !autoScanRunning ||
                                cloudflareChallengeActive
                            ) {
                                return@postDelayed
                            }

                            val js =
                                """

(function() {

    try {

        var docX =
            $x;

        var docY =
            $y;

        var targetScrollY =
            Math.max(
                0,
                docY - Math.floor(window.innerHeight / 2)
            );

        window.scrollTo(
            0,
            targetScrollY
        );

        setTimeout(function() {

            try {

                var viewX =
                    docX;

                var viewY =
                    docY - window.scrollY;

                if (
                    viewY < 20 ||
                    viewY > window.innerHeight - 20
                ) {

                    viewY =
                        Math.floor(
                            window.innerHeight / 2
                        );
                }

                var el =
                    document.elementFromPoint(
                        viewX,
                        viewY
                    );

                if (!el) {
                    return;
                }

                var clickable =
                    el.closest(
                        "a[href], button, [role='button'], [onclick], article, .post, .entry, .card, .item, .video, .channel, .tv-channel, [class*='post'], [class*='entry'], [class*='card'], [class*='video'], [class*='channel']"
                    ) || el;

                try {

                    clickable.scrollIntoView({
                        block: "center",
                        inline: "center"
                    });

                } catch(e) {}

                setTimeout(function() {

                    try {

                        clickable.click();

                    } catch(e) {

                        try {

                            var evt =
                                new MouseEvent(
                                    "click",
                                    {
                                        bubbles: true,
                                        cancelable: true,
                                        view: window
                                    }
                                );

                            clickable.dispatchEvent(
                                evt
                            );

                        } catch(e2) {}
                    }

                }, 250);

            } catch(e) {}

        }, 450);

        return "OK";

    } catch(e) {

        return "ERR";
    }

})();

                                """.trimIndent()

                            activeWebView.evaluateJavascript(
                                js
                            ) { _ ->

                                runOnUiThread {

                                    binding.contentMain.result.append(
                                        """

AUTO SCAN:
Row/card clicked.
Waiting for stream...

────────────────────

                                        """.trimIndent()
                                    )

                                    runDeepMediaScan(
                                        activeWebView
                                    )

                                    binding.contentMain.webview.postDelayed(
                                        {

                                            runDeepMediaScan(
                                                activeWebView
                                            )

                                            finalizeCurrentAutoChannel(
                                                candidate
                                            )

                                            autoScanIndex++

                                            binding.contentMain.webview.postDelayed(
                                                {
                                                    scanNextAutoChannel()
                                                },
                                                900
                                            )
                                        },
                                        4500
                                    )
                                }
                            }

                        } catch (t: Throwable) {

                            Log.e(
                                "AUTO_SCAN",
                                "gel-row click failed",
                                t
                            )

                            finalizeCurrentAutoChannel(
                                candidate
                            )

                            autoScanIndex++

                            binding.contentMain.webview.postDelayed(
                                {
                                    scanNextAutoChannel()
                                },
                                900
                            )
                        }
                    },
                    loadDelay
                )

                return

            } catch (t: Throwable) {

                Log.e(
                    "AUTO_SCAN",
                    "gel-row handler failed",
                    t
                )

                autoScanIndex++

                binding.contentMain.webview.postDelayed(
                    {
                        scanNextAutoChannel()
                    },
                    900
                )

                return
            }
        }

        // =====================================
        // NORMAL URL CANDIDATE
        // =====================================

        try {

            binding.contentMain.urlInput.setText(
                candidate.href
            )

            binding.contentMain.urlInput.setSelection(
                0
            )

            liveUrlInputText =
                candidate.href

        } catch (_: Throwable) {}

        try {

            activeWebView.stopLoading()

            activeWebView.loadUrl(
                candidate.href,
                mapOf(
                    "Cache-Control" to "no-cache",
                    "Pragma" to "no-cache"
                )
            )

        } catch (_: Throwable) {

            autoScanIndex++

            binding.contentMain.webview.postDelayed(
                {
                    scanNextAutoChannel()
                },
                900
            )

            return
        }

        binding.contentMain.webview.postDelayed(
            {

                try {

                    if (
                        !autoScanRunning ||
                        cloudflareChallengeActive
                    ) {
                        return@postDelayed
                    }

                    val beforeClickStreams =
                        collectCurrentPlayableStreamsForAutoScan()

                    if (beforeClickStreams.isNotEmpty()) {

                        runDeepMediaScan(
                            activeWebView
                        )

                        binding.contentMain.webview.postDelayed(
                            {

                                finalizeCurrentAutoChannel(
                                    candidate
                                )

                                autoScanIndex++

                                binding.contentMain.webview.postDelayed(
                                    {
                                        scanNextAutoChannel()
                                    },
                                    900
                                )
                            },
                            2500
                        )

                        return@postDelayed
                    }

                    clickWatchOrPlayButtonIfPresent(
                        activeWebView
                    ) { clicked ->

                        runOnUiThread {

                            if (!clicked) {

                                binding.contentMain.result.append(
                                    """

AUTO SCAN:
No play button found.
Skipping wait.

────────────────────

                                    """.trimIndent()
                                )

                                finalizeCurrentAutoChannel(
                                    candidate
                                )

                                autoScanIndex++

                                binding.contentMain.webview.postDelayed(
                                    {
                                        scanNextAutoChannel()
                                    },
                                    700
                                )

                                return@runOnUiThread
                            }

                            binding.contentMain.result.append(
                                """

AUTO SCAN:
Play button clicked.
Waiting for stream...

────────────────────

                                """.trimIndent()
                            )

                            runDeepMediaScan(
                                activeWebView
                            )

                            binding.contentMain.webview.postDelayed(
                                {

                                    runDeepMediaScan(
                                        activeWebView
                                    )

                                    finalizeCurrentAutoChannel(
                                        candidate
                                    )

                                    autoScanIndex++

                                    binding.contentMain.webview.postDelayed(
                                        {
                                            scanNextAutoChannel()
                                        },
                                        900
                                    )
                                },
                                4500
                            )
                        }
                    }

                } catch (t: Throwable) {

                    Log.e(
                        "AUTO_SCAN",
                        "channel scan step failed",
                        t
                    )

                    autoScanIndex++

                    binding.contentMain.webview.postDelayed(
                        {
                            scanNextAutoChannel()
                        },
                        900
                    )
                }
            },
            4200
        )

    } catch (t: Throwable) {

        Log.e(
            "AUTO_SCAN",
            "scan next failed",
            t
        )

        autoScanIndex++

        binding.contentMain.webview.postDelayed(
            {
                scanNextAutoChannel()
            },
            1200
        )
    }
}

// =====================================
// FINALIZE ONE AUTO-SCANNED CHANNEL
// Stores all clean streams discovered after opening candidate.
// Marks:
// BEST QUALITY = tracks-v1a1/mono.m3u8
// BEST STABLE  = playlist.m3u8 / index.m3u8
// =====================================

private fun finalizeCurrentAutoChannel(
    candidate: AutoScanCandidate
) {

    try {

        val allNow =
            collectCurrentPlayableStreamsForAutoScan()

        val newStreams =
            allNow
                .map { stream ->
                    cleanDetectedUrl(
                        stream
                    )
                }
                .filter { stream ->

                    stream.isNotBlank() &&
                        isExportableStream(
                            stream
                        ) &&
                        !autoScanKnownBefore.contains(
                            stream.lowercase()
                        )
                }

        val finalStreams =
            newStreams
                .distinctBy { stream ->
                    stream.lowercase()
                }
                .sortedWith(
                    compareByDescending<String> { stream ->
                        streamRankScore(
                            stream
                        )
                    }.thenBy { stream ->
                        stream.lowercase()
                    }
                )

        if (finalStreams.isNotEmpty()) {

            val bestQualityStream =
                pickBestQualityStream(
                    finalStreams
                )

            val bestStableStream =
                pickBestStableStream(
                    finalStreams
                )

            val bestQualityNote =
                if (bestQualityStream.isNotBlank()) {

                    """

BEST QUALITY:
$bestQualityStream

                    """.trimIndent()

                } else {

                    ""
                }

            val bestStableNote =
                if (bestStableStream.isNotBlank()) {

                    """

BEST STABLE:
$bestStableStream

                    """.trimIndent()

                } else {

                    ""
                }

            val list =
                autoScanResults.getOrPut(
                    candidate.title
                ) {
                    mutableListOf()
                }

            finalStreams.forEach { stream ->

                if (
                    list.none { existing ->
                        existing.equals(
                            stream,
                            true
                        )
                    }
                ) {
                    list.add(
                        stream
                    )
                }
            }

            val labelLines =
                finalStreams.joinToString("\n") { stream ->

                    when {

                        stream.equals(
                            bestQualityStream,
                            true
                        ) ->
                            "⭐ BEST QUALITY → $stream"

                        stream.equals(
                            bestStableStream,
                            true
                        ) ->
                            "🛟 BEST STABLE → $stream"

                        isLowerPriorityHlsVariant(
                            stream
                        ) ->
                            "🧩 FALLBACK → $stream"

                        else ->
                            stream
                    }
                }

            binding.contentMain.result.append(
                """

AUTO SCAN FOUND:
${candidate.title}

$bestQualityNote
$bestStableNote
Streams:
${finalStreams.size}

$labelLines

────────────────────

                """.trimIndent()
            )

        } else {

            binding.contentMain.result.append(
                """

AUTO SCAN NO STREAM:
${candidate.title}

────────────────────

                """.trimIndent()
            )
        }

    } catch (t: Throwable) {

        Log.e(
            "AUTO_SCAN",
            "finalize failed",
            t
        )
    }
}

// =====================================
// FINISH AUTO CHANNEL SCAN
// Creates M3U from collected results
// =====================================

private fun finishAutoScanChannels() {

    try {

        autoScanRunning =
            false

        hideAutoScanStopButton()

        window.clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val totalStreams =
            autoScanResults.values.sumOf { list ->
                list.size
            }

        if (totalStreams <= 0) {

            binding.contentMain.result.append(
                """

AUTO CHANNEL SCAN DONE:
No streams collected.

────────────────────

                """.trimIndent()
            )

            Toast.makeText(
                this,
                "Auto scan done: no streams",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val pageHost =
            try {

                Uri.parse(
                    binding.contentMain.webview.url.orEmpty()
                )
                    .host
                    ?.replace(".", "_")
                    ?.uppercase()
                    ?: "PAGE"

            } catch (_: Throwable) {

                "PAGE"
            }

        val fileName =
            "GEL_AUTOSCAN_${pageHost}.m3u"

        val builder =
            StringBuilder()

        builder.append(
            "#EXTM3U\n"
        )

        autoScanResults.forEach { entry ->

            val channelName =
                entry.key
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()
                    .uppercase()
                    .ifBlank {
                        "UNKNOWN CHANNEL"
                    }

            entry.value.forEach { stream ->

                builder.append(
                    "#EXTINF:-1 tvg-id=\"\" tvg-logo=\"noimage.png\" group-title=\"AUTO SCAN\", AUTO: "
                )

                builder.append(
                    channelName
                )

                builder.append(
                    " autoscan\n"
                )

                builder.append(
                    stream
                )

                builder.append(
                    "\n\n"
                )
            }
        }

        val savedPath =
            saveM3uTextToDownloads(
                fileName,
                builder.toString()
            )

        binding.contentMain.result.append(
            """

AUTO CHANNEL SCAN DONE:
Channels:
${autoScanResults.size}

Streams:
$totalStreams

Saved:
$fileName

Path:
$savedPath

────────────────────

            """.trimIndent()
        )

        Toast.makeText(
            this,
            "Auto scan saved: $fileName",
            Toast.LENGTH_LONG
        ).show()

    } catch (t: Throwable) {

        autoScanRunning =
            false

        Log.e(
            "AUTO_SCAN",
            "finish failed",
            t
        )
    }
}

// =====================================
// M3U LIST DETECTION
// Detect country/channel playlist files
// Not HLS .m3u8 streams
// =====================================

private fun isM3uListUrl(
    url: String
): Boolean {

    val clean =
        cleanDetectedUrl(
            url
        )
            .substringBefore("?")
            .substringBefore("#")
            .lowercase()

    return (
        clean.endsWith(".m3u") &&
            !clean.endsWith(".m3u8")
    )
}

// =====================================
// BUILD SAFE M3U FILE NAME
// Example:
// https://site.com/GREECE.m3u
// -> GEL_DETECTED_GREECE.m3u
// =====================================

private fun buildDetectedM3uFileName(
    m3uUrl: String
): String {

    return try {

        val uri =
            Uri.parse(
                m3uUrl
            )

        val lastSegment =
            uri.lastPathSegment
                ?.substringBefore("?")
                ?.substringBefore("#")
                ?.removeSuffix(".m3u")
                ?.trim()
                .orEmpty()

        val hostFallback =
            uri.host
                ?.replace(".", "_")
                ?.trim()
                .orEmpty()

        val base =
            when {

                lastSegment.isNotBlank() ->
                    lastSegment

                hostFallback.isNotBlank() ->
                    hostFallback

                else ->
                    "M3U_LIST"
            }

        val cleanBase =
            base
                .uppercase()
                .replace(
                    Regex("[^A-Z0-9_ -]"),
                    "_"
                )
                .replace(
                    Regex("\\s+"),
                    "_"
                )
                .replace(
                    Regex("_+"),
                    "_"
                )
                .trim('_')
                .ifBlank {
                    "M3U_LIST"
                }

        "GEL_DETECTED_${cleanBase}.m3u"

    } catch (_: Throwable) {

        "GEL_DETECTED_M3U_LIST.m3u"
    }
}

// =====================================
// SAVE TEXT FILE TO DOWNLOADS
// Android 10+ uses MediaStore
// Older Android uses public Downloads folder
// =====================================

private fun saveM3uTextToDownloads(
    fileName: String,
    content: String
): String {

    return try {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            val values =
                android.content.ContentValues().apply {

                    put(
                        android.provider.MediaStore.Downloads.DISPLAY_NAME,
                        fileName
                    )

                    put(
                        android.provider.MediaStore.Downloads.MIME_TYPE,
                        "audio/x-mpegurl"
                    )

                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        "Download/GEL_DETECTED_M3U"
                    )

                    put(
                        android.provider.MediaStore.Downloads.IS_PENDING,
                        1
                    )
                }

            val resolver =
                contentResolver

            val uri =
                resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return ""

            resolver.openOutputStream(
                uri
            )?.use { output ->

                output.write(
                    content.toByteArray(
                        Charsets.UTF_8
                    )
                )

                output.flush()
            }

            values.clear()

            values.put(
                android.provider.MediaStore.Downloads.IS_PENDING,
                0
            )

            resolver.update(
                uri,
                values,
                null,
                null
            )

            "Downloads/GEL_DETECTED_M3U/$fileName"

        } else {

            val dir =
                java.io.File(
                    android.os.Environment
                        .getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        ),
                    "GEL_DETECTED_M3U"
                )

            if (!dir.exists()) {
                dir.mkdirs()
            }

            val file =
                java.io.File(
                    dir,
                    fileName
                )

            file.writeText(
                content,
                Charsets.UTF_8
            )

            file.absolutePath
        }

    } catch (t: Throwable) {

        Log.e(
            "M3U_SAVE",
            "save failed",
            t
        )

        ""
    }
}

// =====================================
// DOWNLOAD + SAVE DETECTED M3U LIST
// =====================================

private fun downloadAndSaveDetectedM3uList(
    m3uUrl: String
) {

    try {

        val cleanUrl =
            cleanDetectedUrl(
                m3uUrl
            )

        if (
            cleanUrl.isBlank() ||
            !isM3uListUrl(
                cleanUrl
            )
        ) {
            return
        }

        if (
            detectedM3uLists.contains(
                cleanUrl
            )
        ) {
            return
        }

        detectedM3uLists.add(
            cleanUrl
        )

        runOnUiThread {

            try {

                binding.contentMain.result.append(
                    """

M3U LIST FOUND:
$cleanUrl

Downloading...

────────────────────

                    """.trimIndent()
                )

            } catch (_: Throwable) {}
        }

        val request =
            Request.Builder()
                .url(
                    cleanUrl
                )
                .header(
                    "User-Agent",
                    binding.contentMain.webview.settings.userAgentString
                        ?: desktopUserAgent
                )
                .header(
                    "Accept",
                    "*/*"
                )
                .header(
                    "Referer",
                    binding.contentMain.webview.url
                        ?: cleanUrl
                )
                .build()

        OkHttpClient()
            .newCall(
                request
            )
            .enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        Log.e(
                            "M3U_DOWNLOAD",
                            "failed: $cleanUrl",
                            e
                        )

                        runOnUiThread {

                            try {

                                binding.contentMain.result.append(
                                    """

M3U DOWNLOAD FAILED:
$cleanUrl

${e.message ?: "Unknown error"}

────────────────────

                                    """.trimIndent()
                                )

                            } catch (_: Throwable) {}
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        try {

                            val body =
                                response.body
                                    ?.string()
                                    .orEmpty()

                            if (
                                !response.isSuccessful ||
                                body.isBlank()
                            ) {

                                runOnUiThread {

                                    binding.contentMain.result.append(
                                        """

M3U DOWNLOAD FAILED:
$cleanUrl

HTTP:
${response.code}

────────────────────

                                        """.trimIndent()
                                    )
                                }

                                return
                            }

                            val looksLikeM3u =
                                body.contains(
                                    "#EXTM3U",
                                    true
                                ) ||
                                    body.contains(
                                        "#EXTINF",
                                        true
                                    )

                            if (!looksLikeM3u) {

                                runOnUiThread {

                                    binding.contentMain.result.append(
                                        """

M3U REJECTED:
$cleanUrl

Reason:
Downloaded file is not valid M3U.

────────────────────

                                        """.trimIndent()
                                    )
                                }

                                return
                            }

                            val fileName =
                                buildDetectedM3uFileName(
                                    cleanUrl
                                )

                            val savedPath =
                                saveM3uTextToDownloads(
                                    fileName,
                                    body
                                )

                            runOnUiThread {

                                try {

                                    binding.contentMain.result.append(
                                        """

M3U LIST SAVED:
$fileName

SOURCE:
$cleanUrl

PATH:
$savedPath

────────────────────

                                        """.trimIndent()
                                    )

                                    Toast.makeText(
                                        this@MainActivity,
                                        "M3U saved: $fileName",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } catch (_: Throwable) {}
                            }

                        } catch (t: Throwable) {

                            Log.e(
                                "M3U_DOWNLOAD",
                                "response failed",
                                t
                            )
                        } finally {

                            try {
                                response.close()
                            } catch (_: Throwable) {}
                        }
                    }
                }
            )

    } catch (t: Throwable) {

        Log.e(
            "M3U_DOWNLOAD",
            "start failed",
            t
        )
    }
}

// =====================================
// CLOSE POPUP WEBVIEW
// =====================================

private fun closePopupWebView() {

    try {

        popupWebView?.stopLoading()

        (popupWebView?.parent as? ViewGroup)
            ?.removeView(
                popupWebView
            )

        popupWebView?.destroy()

        popupWebView =
            null

    } catch (_: Throwable) {}
}

// =====================================
// OPEN EXTERNAL BROWSER / CUSTOM TAB
// =====================================

private fun openWithExternalBrowser(
    url: String
) {

    try {

        val cleanUrl =
            url
                .trim()

        if (cleanUrl.isBlank()) {
            return
        }

        val customTabsIntent =
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

        customTabsIntent.launchUrl(
            this,
            Uri.parse(cleanUrl)
        )

    } catch (_: Throwable) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {

                    addCategory(
                        Intent.CATEGORY_BROWSABLE
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Open With Browser"
                )
            )

        } catch (_: Throwable) {

            Toast.makeText(
                this,
                "Cannot open browser",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// =====================================
// REFERER ORIGIN
// =====================================

private fun buildRefererOrigin(
    url: String
): String {

    return try {

        val uri =
            Uri.parse(url)

        val scheme =
            uri.scheme
                ?: "https"

        val host =
            uri.host
                ?: ""

        if (host.isBlank()) {
            "https://www.google.com/"
        } else {
            "$scheme://$host/"
        }

    } catch (_: Throwable) {

        "https://www.google.com/"
    }
}

// =====================================
// PROTECTED PAGE FALLBACK
// =====================================

private fun showProtectedPageFallback(
    url: String,
    reason: String
) {

    try {

        val cleanUrl =
            url.trim()

        // =====================================
        // IGNORE INTERNAL / SEARCH / EMPTY PAGES
        // =====================================

        if (
            cleanUrl.isBlank() ||
            cleanUrl.equals(
                "about:blank",
                true
            ) ||
            (
                !cleanUrl.startsWith(
                    "http://",
                    true
                ) &&
                !cleanUrl.startsWith(
                    "https://",
                    true
                )
            ) ||
            cleanUrl.contains(
                "google.com/search",
                true
            )
        ) {
            return
        }

        // =====================================
        // DO NOT SHOW BLOCKED WARNING
        // IF STREAMS WERE ALREADY DETECTED
        // =====================================

        val alreadyDetectedPlayable =
            detectedStreams.isNotEmpty() ||
                detectedVideos.isNotEmpty() ||
                detectedAudio.isNotEmpty() ||
                detectedMasterStreams.isNotEmpty() ||
                streamInfoSnapshots.isNotEmpty() ||
                bestStreamUrl.isNotBlank() ||
                bestLiveUrl.isNotBlank() ||
                youtubeWatchUrl.isNotBlank() ||
                youtubeDashVideoUrl.isNotBlank() ||
                youtubeDashAudioUrl.isNotBlank()
                lastChannelCandidateCount > 0

        if (alreadyDetectedPlayable) {

            Log.e(
                "PROTECTED_SKIPPED",
                "Streams already detected, fallback suppressed"
            )

            return
        }

        if (protectedFallbackShown) {
            return
        }

        protectedFallbackShown =
            true

        binding.contentMain.result.text =
            """

PROTECTED / BLOCKED PAGE

The page loaded, but returned no readable media DOM.

This source may block Android WebView, require protected playback, geo/session access, browser verification, or external browser cookies.

ACTION:
Open With Browser..

────────────────────

            """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Protected / Blocked Page")
            .setMessage(
                "This source does not expose readable media content inside the analyzer.\n\nOpen it with browser?"
            )
            .setPositiveButton("OPEN WITH BROWSER") { _, _ ->

                openWithExternalBrowser(
                    cleanUrl
                )
            }
            .setNegativeButton("STAY", null)
            .show()

    } catch (_: Throwable) {}
}

// =====================================
// REFERER-AWARE RETRY
// =====================================

private fun retryWithRefererOnce(
    url: String
): Boolean {

    if (refererRetryDone) {
        return false
    }

    refererRetryDone =
        true

    return try {

        val referer =
            buildRefererOrigin(
                url
            )

        binding.contentMain.webview.postDelayed(
            {

                try {

                    binding.contentMain.webview.stopLoading()

                    binding.contentMain.webview.loadUrl(
                        url,
                        mapOf(
                            "Referer" to referer,
                            "Origin" to referer.trimEnd('/'),
                            "Cache-Control" to "no-cache",
                            "Pragma" to "no-cache",
                            "Accept-Language" to "en-US,en;q=0.9"
                        )
                    )

                } catch (_: Throwable) {}
            },
            500
        )

        true

    } catch (_: Throwable) {

        false
    }
}

// =====================================
// RENDER YOUTUBE HLS RESOLVER
// YouTube watch URL -> fresh m3u8
// =====================================

private fun resolveYouTubeWithRender(
    sourceUrl: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {

    Thread {

        try {

            val encodedUrl =
                java.net.URLEncoder.encode(
                    sourceUrl,
                    "UTF-8"
                )

            val endpoint =
                "https://gel-youtube-hls-extractor.onrender.com/extract?url=$encodedUrl"

            val connection =
                java.net.URL(endpoint)
                    .openConnection() as java.net.HttpURLConnection

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                60000

            connection.readTimeout =
                120000

            val responseCode =
                connection.responseCode

            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val body =
                stream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

            val json =
                org.json.JSONObject(body)

            val ok =
                json.optBoolean(
                    "ok",
                    false
                )

            if (!ok) {

                val error =
                    json.optString(
                        "error",
                        "Render extraction failed"
                    )

                runOnUiThread {
                    onError(error)
                }

                return@Thread
            }

            val result =
                json.optJSONObject(
                    "result"
                )

            val m3u8Url =
                result
                    ?.optString(
                        "m3u8_url",
                        ""
                    )
                    ?.takeIf { it.isNotBlank() }
                    ?: result
                        ?.optString(
                            "url",
                            ""
                        )
                        .orEmpty()

            if (m3u8Url.isBlank()) {

                runOnUiThread {
                    onError("No m3u8 URL returned")
                }

                return@Thread
            }

            runOnUiThread {
                onSuccess(m3u8Url)
            }

        } catch (t: Throwable) {

            runOnUiThread {
                onError(
                    t.message
                        ?: "Render resolver error"
                )
            }
        }

    }.start()
}

// =====================================
// SAVE YOUTUBE WATCH FROM ANY URL
// =====================================

// =====================================
// SAVE YOUTUBE WATCH FROM ANY URL
// =====================================

private fun saveYouTubeWatchFromUrl(
    rawUrl: String
) {

    try {

        val cleaned =
            rawUrl
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim()

        if (cleaned.isBlank()) {
            return
        }

        val decoded =
            try {
                Uri.decode(cleaned)
            } catch (_: Throwable) {
                cleaned
            }

        val lower =
            decoded.lowercase()

        val uri =
            Uri.parse(decoded)

        var videoId =
            ""

        when {

            lower.contains("youtube.com/watch") -> {

                videoId =
                    uri.getQueryParameter("v")
                        .orEmpty()
            }

            lower.contains("youtube.com/embed/") -> {

                videoId =
                    uri.path
                        ?.substringAfterLast("/")
                        ?.substringBefore("?")
                        ?.substringBefore("&")
                        ?.trim()
                        .orEmpty()
            }

            lower.contains("youtube.com/live/") -> {

                videoId =
                    uri.path
                        ?.substringAfter("/live/")
                        ?.substringBefore("/")
                        ?.substringBefore("?")
                        ?.substringBefore("&")
                        ?.trim()
                        .orEmpty()
            }

            lower.contains("youtube.com/shorts/") -> {

                videoId =
                    uri.path
                        ?.substringAfter("/shorts/")
                        ?.substringBefore("/")
                        ?.substringBefore("?")
                        ?.substringBefore("&")
                        ?.trim()
                        .orEmpty()
            }

            lower.contains("youtu.be/") -> {

                videoId =
                    uri.path
                        ?.trimStart('/')
                        ?.substringBefore("/")
                        ?.substringBefore("?")
                        ?.substringBefore("&")
                        ?.trim()
                        .orEmpty()
            }
            
            else -> {

                videoId =
                    uri.getQueryParameter("video_id")
                        ?: uri.getQueryParameter("docid")
                        ?: ""
            }
        }

        // =====================================
        // STRICT YOUTUBE VIDEO ID CHECK
        // =====================================

        val isValidYouTubeId =
            videoId.matches(
                Regex("^[A-Za-z0-9_-]{11}$")
            )

        if (!isValidYouTubeId) {
            return
        }

        val watchUrl =
    "https://www.youtube.com/watch?v=$videoId"

youtubeWatchUrl =
    watchUrl

if (
    !detectedStreams.contains(
        watchUrl
    )
) {

    detectedStreams.add(
        watchUrl
    )

    detectedVideos.add(
        watchUrl
    )

    streamScores[watchUrl] =
        9999

    markStreamSource(
        watchUrl,
        "YOUTUBE_WATCH"
    )

    streamValidation[watchUrl] =
        "YOUTUBE WATCH"

    try {

        autoValidateStream(
            watchUrl
        )

    } catch (_: Throwable) {}

    // =====================================
    // TRY YOUTUBE HLS FALLBACK
    // Only for real 11-char YouTube IDs
    // =====================================

    try {

        if (
            videoId.matches(
                Regex("^[A-Za-z0-9_-]{11}$")
            )
        ) {

            tryYouTubeGetVideoInfoFallback(
                videoId
            )
        }

    } catch (_: Throwable) {}

    Log.e(
        "YOUTUBE_WATCH_SAVED",
        watchUrl
    )
}

} catch (_: Throwable) {}
}

// =====================================
// ENABLE PAGE TEXT SELECTION
// =====================================

private fun enablePageTextSelection(
    view: WebView?
) {

    try {

        val currentUrl =
            view?.url
                ?: ""

        // Do not inject global text-selection CSS on interactive
        // streaming/player pages. It can interfere with taps.
        if (
            currentUrl.contains(
                "rakuten.tv",
                true
            ) ||
            currentUrl.contains(
                "amagi.tv",
                true
            ) ||
            currentUrl.contains(
                "playouts.now",
                true
            )
        ) {
            return
        }

        view?.evaluateJavascript(
            """

(function() {

    try {

        const style =
            document.createElement("style");

        style.innerHTML =
            "* {" +
            "-webkit-user-select: text !important;" +
            "user-select: text !important;" +
            "-webkit-touch-callout: default !important;" +
            "}";

        document.documentElement.appendChild(
            style
        );

        document.onselectstart =
            null;

        document.oncontextmenu =
            null;

        document.body.onselectstart =
            null;

        document.body.oncontextmenu =
            null;

        console.log(
            "GEL_TEXT_SELECTION_ENABLED"
        );

    } catch(e) {}

})();

            """.trimIndent(),
            null
        )

    } catch (_: Throwable) {}
}

// =====================================
// CLEAN URL NORMALIZER
// =====================================

private fun cleanDetectedUrl(
    rawUrl: String
): String {

    return try {

        var value =
            rawUrl
                .replace("\\u0026", "&")
                .replace("\\u003d", "=")
                .replace("\\u003f", "?")
                .replace("\\u002f", "/")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim()

        if (value.isBlank()) {
            return ""
        }

        // =====================================
        // REMOVE WRAPPED / TRAILING JUNK
        // =====================================

        value =
            value
                .trim()
                .trim('"')
                .trim('\'')

        // =====================================
        // DROP KNOWN BAD QUERY VARIANTS COMPLETELY
        // =====================================

        val lower =
            value.lowercase()

        if (
            lower.contains("?error=") ||
            lower.contains("&error=") ||
            lower.contains("reader_gdpr") ||
            lower.contains("gdpr_binary_consent") ||
            lower.contains("gdpr_comes_from_infopack") ||
            lower.contains("reader_us_privacy") ||
            lower.contains("vmap") ||
            lower.contains("monetization") ||
            lower.contains("cookie_sync") ||
            lower.contains("ciid=") ||
            lower.contains("cidx=") ||
            lower.contains("sidx=") ||
            lower.contains("vididx=") ||
            lower.contains("imal=") ||
            lower.contains("3pcb=") ||
            lower.contains("rap=") ||
            lower.contains("apo=") ||
            lower.contains("pdm=") ||
            lower.contains("pbm=")
        ) {

            val base =
                value.substringBefore("?")
                    .trim()

            if (
                base.contains(".m3u8", true) ||
                base.contains(".mpd", true) ||
                base.contains(".mp4", true)
            ) {
                return base
            }

            return ""
        }

        // =====================================
        // CLEAN COMMON SAFE HLS QUERY NOISE
        // Keep URLs distinct when query is probably required.
        // Strip only obvious browser/ad/session junk.
        // =====================================

        if (
            value.contains(".m3u8", true) ||
            value.contains(".mpd", true)
        ) {

            val base =
                value.substringBefore("?")

            val query =
                value.substringAfter(
                    "?",
                    ""
                )

            if (query.isBlank()) {
                return value
            }

            val badQueryKeys =
                listOf(
                    "reader_gdpr_flag",
                    "reader_gdpr_consent",
                    "gdpr_binary_consent",
                    "gdpr_comes_from_infopack",
                    "reader_us_privacy",
                    "ciid",
                    "cidx",
                    "sidx",
                    "vidIdx",
                    "imal",
                    "3pcb",
                    "rap",
                    "apo",
                    "pdm",
                    "pbm",
                    "cookie_sync_ab_gk",
                    "error"
                )

            val kept =
                query
                    .split("&")
                    .filter { part ->

                        val key =
                            part.substringBefore("=")
                                .trim()

                        key.isNotBlank() &&
                            badQueryKeys.none { bad ->
                                key.equals(
                                    bad,
                                    true
                                )
                            }
                    }

            return if (kept.isEmpty()) {
                base
            } else {
                base + "?" + kept.joinToString("&")
            }
        }

        value

    } catch (_: Throwable) {

        rawUrl
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u003f", "?")
            .replace("\\u002f", "/")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .trim()
    }
}

// =====================================
// PLAYLIST DETECTION
// =====================================

private fun isPlaylistUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains("master.m3u8") ||
            lower.contains("playlist.m3u8") ||
            lower.contains("index.m3u8") ||
            lower.contains("live.m3u8") ||
            lower.contains("manifest/hls") ||
            lower.contains("hls_playlist") ||
            lower.contains("hlsmanifesturl") ||
            lower.contains("application/vnd.apple.mpegurl") ||
            lower.contains("application/x-mpegurl")
        )
}

// =====================================
// TS FALLBACK DETECTION
// Allowed only if no playlist was found
// =====================================

private fun isTsFallbackUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.endsWith(".ts") ||
            lower.contains(".ts?")
        )
}

// =====================================
// HARD NOISE FILTER
// Never useful as final stream
// =====================================

private fun isHardNoiseUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.endsWith(".gif") ||
            lower.contains(".gif?") ||
            lower.endsWith(".png") ||
            lower.contains(".png?") ||
            lower.endsWith(".jpg") ||
            lower.contains(".jpg?") ||
            lower.endsWith(".jpeg") ||
            lower.contains(".jpeg?") ||
            lower.endsWith(".webp") ||
            lower.contains(".webp?") ||
            lower.endsWith(".svg") ||
            lower.contains(".svg?") ||
            lower.endsWith(".ico") ||
            lower.contains(".ico?") ||
            lower.endsWith(".css") ||
            lower.contains(".css?") ||
            lower.endsWith(".js") ||
            lower.contains(".js?") ||
            lower.endsWith(".vtt") ||
            lower.contains(".vtt?") ||
            lower.endsWith(".m4s") ||
            lower.contains(".m4s?") ||
            lower.contains("doubleclick") ||
            lower.contains("googleads") ||
            lower.contains("googletag") ||
            lower.contains("analytics") ||
            lower.contains("/stats/") ||
            lower.contains("ptracking") ||
            lower.contains("api/stats") ||
            lower.contains("playback/stats") ||
            lower.contains("generate_204") ||
            lower.contains("pagead") ||
            lower.contains("collect?") ||
            lower.contains("favicon") ||
            lower.contains("/logo") ||
            lower.contains("banner") ||
            lower.contains("pixel") ||
            lower.contains("recaptcha")
        )
}

// =====================================
// EXPORTABLE NON-PLAYLIST MEDIA
// Keep only real media, not fragments
// =====================================

private fun isDirectMediaUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.contains(".mp4") ||
            lower.contains(".webm") ||
            lower.contains(".mkv") ||
            lower.contains(".mov") ||
            lower.contains(".avi") ||
            lower.contains(".3gp") ||
            lower.contains(".mp3") ||
            lower.contains(".m4a") ||
            lower.contains(".aac") ||
            lower.contains(".opus") ||
            lower.contains(".wav") ||
            lower.contains(".ogg") ||
            lower.contains(".flac") ||
            lower.contains("youtube.com/watch") ||
            lower.contains("youtu.be/") ||
            (
                lower.contains("googlevideo.com") &&
                    lower.contains("videoplayback")
            )
        )
}

// =====================================
// FINAL NETWORK CANDIDATE FILTER
// =====================================

private fun shouldAcceptDetectedNetworkUrl(
    rawUrl: String
): Boolean {

    val cleanUrl =
        cleanDetectedUrl(
            rawUrl
        )

    if (cleanUrl.isBlank()) {
        return false
    }

    if (
        !cleanUrl.startsWith("http://", true) &&
        !cleanUrl.startsWith("https://", true)
    ) {
        return false
    }

    if (isHardNoiseUrl(cleanUrl)) {
        return false
    }
    
    if (
    isM3uListUrl(
        cleanUrl
    )
) {
    return true
}

    if (isPlaylistUrl(cleanUrl)) {
        return true
    }

    if (isTsFallbackUrl(cleanUrl)) {

        // .ts is useful only before playlist discovery.
        return !pagePlaylistFound
    }

    if (isDirectMediaUrl(cleanUrl)) {
        return true
    }

    return false
}

// =====================================
// CLEAR TS FALLBACK WHEN PLAYLIST EXISTS
// =====================================

private fun clearTsFallbackBecausePlaylistFound() {

    try {

        if (pendingTsFallback.isEmpty()) {
            return
        }

        pendingTsFallback.forEach { tsUrl ->

            detectedStreams.remove(tsUrl)
            detectedVideos.remove(tsUrl)
            streamScores.remove(tsUrl)
            streamValidation.remove(tsUrl)
            streamSources.remove(tsUrl)
            streamHeaders.remove(tsUrl)
            streamTokens.remove(tsUrl)
            streamInfoSnapshots.remove(tsUrl)
        }

        pendingTsFallback.clear()

    } catch (_: Throwable) {}
}

// =====================================
// CLOUDFLARE / VERIFY PAGE DETECTOR
// =====================================

private fun isCloudflareChallengeRequestUrl(
    url: String?
): Boolean {

    val lower =
        url
            .orEmpty()
            .lowercase()

    return (
        lower.contains(
            "challenges.cloudflare.com"
        ) ||
        lower.contains(
            "/cdn-cgi/challenge-platform/"
        ) ||
        lower.contains(
            "/cdn-cgi/challenge/"
        ) ||
        lower.contains(
            "cf-chl-"
        ) ||
        lower.contains(
            "cf_turnstile"
        ) ||
        lower.contains(
            "turnstile"
        )
    )
}

private fun isCloudflareLikePage(
    url: String?,
    diagnosticText: String = ""
): Boolean {

    val lowerUrl =
        url
            .orEmpty()
            .lowercase()

    val lowerText =
        diagnosticText
            .lowercase()

    val explicitUrlMarker =
        isCloudflareChallengeRequestUrl(
            lowerUrl
        )

    val explicitTextMarker =
        lowerText.contains(
            "cloudflare"
        ) ||
        lowerText.contains(
            "cf-chl"
        ) ||
        lowerText.contains(
            "cf_clearance"
        ) ||
        lowerText.contains(
            "challenge-platform"
        ) ||
        lowerText.contains(
            "cf-turnstile"
        ) ||
        lowerText.contains(
            "challenges.cloudflare.com"
        )

    val humanChallenge =
        (
            lowerText.contains(
                "checking your browser"
            ) ||
            lowerText.contains(
                "verify you are human"
            ) ||
            lowerText.contains(
                "performing security verification"
            )
        ) &&
        explicitTextMarker

    val waitingPage =
        lowerText.contains(
            "just a moment"
        ) &&
        explicitTextMarker

    return (
        explicitUrlMarker ||
        explicitTextMarker ||
        humanChallenge ||
        waitingPage
    )
}

private fun setCloudflareChallengeMode(
    active: Boolean
) {

    cloudflareChallengeActive =
        active

    val activeWebView =
        popupWebView
            ?: binding.contentMain.webview

    activeWebView.removeCallbacks(
        cloudflareChallengeRecheckRunnable
    )

    if (active) {

        lastCloudflareChallengeTime =
            System.currentTimeMillis()

        // This flag belongs only to real touch events.
        // Cloudflare remains touchable and scrollable.
        activeWebView.postDelayed(
            cloudflareChallengeRecheckRunnable,
            1200L
        )

    } else {

        lastCloudflareChallengeTime =
            0L
    }
}

// =====================================
// HANDLE INTERCEPTED MEDIA URL
// =====================================

private fun handleInterceptedMediaUrl(
    url: String,
    request: WebResourceRequest?
) {

    try {

        if (
            cloudflareChallengeActive ||
            isCloudflareChallengeRequestUrl(
                url
            )
        ) {
            return
        }

        val lower =
            url.lowercase()

// =====================================
// EARLY WRAPPER / TRACKER CLEANUP
// Never log Google/Facebook/analytics wrappers as streams.
// Extract real playable URL first, then process only clean candidates.
// =====================================

try {

    if (
        isWrapperOrTrackerUrl(
            url
        )
    ) {

        val extractedStreams =
            expandDetectedStreamCandidate(
                url
            )
                .filter { candidate ->

                    candidate.isNotBlank() &&
                        candidate.startsWith(
                            "http",
                            true
                        ) &&
                        !isWrapperOrTrackerUrl(
                            candidate
                        ) &&
                        isProbablyPlayableMediaUrl(
                            candidate
                        )
                }
                .distinct()

        extractedStreams.forEach { candidate ->

            try {

                markStreamSource(
                    candidate,
                    "WRAPPER_EXTRACT"
                )

                detectAndSaveUrl(
                    candidate
                )

            } catch (_: Throwable) {}
        }

        return
    }

} catch (_: Throwable) {}
            
// =====================================
// EARLY YOUTUBE WATCH SAVE
// Do NOT convert googlevideo URLs to YouTube watch
// =====================================

if (
    lower.contains("youtube.com/watch") ||
    lower.contains("youtube.com/embed/") ||
    lower.contains("youtube.com/live/") ||
    lower.contains("youtube.com/shorts/") ||
    lower.contains("youtu.be/")
) {

    saveYouTubeWatchFromUrl(
        url
    )
}

// =====================================
// NETWORK MEDIA DETECTION — CLEAN MODE
// .m3u8 / .mpd first, .ts only fallback
// =====================================

if (
    shouldAcceptDetectedNetworkUrl(
        url
    )
) {

    Log.e(
        "NETWORK_MEDIA_CLEAN",
        url
    )

    markStreamSource(
        url,
        "INTERCEPT"
    )

    detectAndSaveUrl(
        url
    )
}

// =====================================
// API / PLAYER / JSON DETECTION — CLEAN MODE
// Do not save assets / ads / stats / images
// =====================================

if (
    !isHardNoiseUrl(url) &&
    (
        lower.contains("playlist") ||
            lower.contains("manifest") ||
            lower.contains("hls") ||
            lower.contains("dash") ||
            lower.contains("m3u8") ||
            lower.contains("mpd") ||
            lower.contains("videoplayback")
    )
) {

    Log.e(
        "API_MEDIA_HINT_CLEAN",
        url
    )

    detectAndSaveUrl(
        url
    )
}

        // =====================================
        // YOUTUBE LIVE DETECTOR
        // =====================================

        if (
            lower.contains("youtube.com/embed/") ||
            lower.contains("youtube.com/watch") ||
            lower.contains("yt_live_broadcast") ||
            lower.contains("googlevideo.com") ||
            lower.contains("youtu.be/")
        ) {

            var videoId =
                ""

            // =====================================
            // EMBED
            // =====================================

            try {

                val embedRegex =
                    "/embed/([a-zA-Z0-9_-]{6,})"
                        .toRegex()

                videoId =
                    embedRegex
                        .find(url)
                        ?.groupValues
                        ?.getOrNull(1)
                        .orEmpty()

            } catch (_: Throwable) {}

            // =====================================
            // WATCH
            // =====================================

            if (videoId.isBlank()) {

                try {

                    val uri =
                        Uri.parse(url)

                    videoId =
                        uri.getQueryParameter("v")
                            .orEmpty()

                } catch (_: Throwable) {}
            }

            // =====================================
            // SHORT URL
            // =====================================

            if (videoId.isBlank()) {

                try {

                    val shortRegex =
                        "youtu\\.be/([a-zA-Z0-9_-]{6,})"
                            .toRegex()

                    videoId =
                        shortRegex
                            .find(url)
                            ?.groupValues
                            ?.getOrNull(1)
                            .orEmpty()

                } catch (_: Throwable) {}
            }

// =====================================
// GOOGLEVIDEO IS DASH EVIDENCE
// Do NOT convert googlevideo id to YouTube watch URL
// =====================================

if (
    videoId.isBlank() &&
    lower.contains("googlevideo.com")
) {

    try {

        markStreamSource(
            url,
            "GOOGLEVIDEO_DASH"
        )

        Log.e(
            "GOOGLEVIDEO_DASH_EVIDENCE",
            url
        )

    } catch (_: Throwable) {}

    return
}

// =====================================
// FINAL WATCH URL
// Only real YouTube video IDs are allowed
// =====================================

val isRealYouTubeId =
    videoId.matches(
        Regex("^[A-Za-z0-9_-]{11}$")
    )

if (
    videoId.isNotBlank() &&
    isRealYouTubeId
) {

    youtubeWatchUrl =
        "https://www.youtube.com/watch?v=$videoId"

    Log.e(
        "YOUTUBE_LIVE_ID",
        videoId
    )

    Log.e(
        "YOUTUBE_LIVE_URL",
        youtubeWatchUrl
    )

    bestLiveUrl =
        youtubeWatchUrl

    bestLiveScore =
        9999

} else if (
    videoId.isNotBlank()
) {

    Log.e(
        "YOUTUBE_FAKE_ID_IGNORED",
        videoId
    )
}
}

        // =====================================
        // SAVE REQUEST HEADERS
        // =====================================

        try {

            if (request != null) {

                val headers =
                    mutableMapOf<String, String>()

                request.requestHeaders
                    ?.forEach { entry ->

                        headers[entry.key] =
                            entry.value
                    }

                streamHeaders[url] =
                    headers
            }

        } catch (_: Throwable) {}

    } catch (_: Throwable) {}
}

// =====================================
// DEEP MEDIA JS SCAN
// =====================================

private fun runDeepMediaScan(
    view: WebView?
) {

    try {

        if (view == null) {
            return
        }
        
        if (cloudflareChallengeActive) {
    return
}

try {

    val currentUrl =
        view.url
            ?: ""

    if (
        isCloudflareLikePage(
            currentUrl
        )
    ) {

        setCloudflareChallengeMode(
            true
        )

        return
    }

} catch (_: Throwable) {}

        // =====================================
        // INTERACTIVE PLAYER GUARD
        // Rakuten and similar FAST pages must remain touchable.
        // Once a best stream has been found, stop hammering the page
        // with JS rescans. The network interceptor will still catch
        // new media requests passively.
        // =====================================

        try {

            val currentUrl =
                view.url
                    ?: ""

            if (
                bestStreamUrl.isNotBlank() &&
                (
                    currentUrl.contains(
                        "rakuten.tv",
                        true
                    ) ||
                    currentUrl.contains(
                        "amagi.tv",
                        true
                    ) ||
                    currentUrl.contains(
                        "playouts.now",
                        true
                    )
                )
            ) {
                return
            }

        } catch (_: Throwable) {}

        // =====================================
        // ANR GUARD
        // Do not scan while user taps / scrolls / opens image previews
        // =====================================

        if (webUserInteracting) {
            return
        }

        val now =
            System.currentTimeMillis()

        if (
            now - lastDeepScanTime < 3500L
        ) {
            return
        }

        lastDeepScanTime =
            now

        val js =
            """

(function() {

let results = [];

if (!window.__gelMediaResults) {
    window.__gelMediaResults = [];
}

function gelPush(url) {

    try {

        if (!url) {
            return;
        }

        url =
            String(url)
                .trim();

        if (!url) {
            return;
        }

        results.push(url);
        window.__gelMediaResults.push(url);

    } catch(e) {}
}

// =====================================
// VIDEO / AUDIO / SOURCE / IMG / IFRAME
// =====================================

try {

    document
        .querySelectorAll("video, audio, source, iframe")
        .forEach(function(el) {

            try {

                if (el.src) {
                    gelPush(el.src);
                    console.log("GEL_ELEMENT_SRC:", el.src);
                }

                if (el.currentSrc) {
                    gelPush(el.currentSrc);
                    console.log("GEL_CURRENT_SRC:", el.currentSrc);
                }

                if (el.poster) {
                    gelPush(el.poster);
                    console.log("GEL_POSTER:", el.poster);
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// IFRAME DEEP MEDIA SCAN
// =====================================

try {

    document
        .querySelectorAll("iframe")
        .forEach(function(frame) {

            try {

                if (frame.src) {

                    gelPush(
                        frame.src
                    );

                    console.log(
                        "GEL_IFRAME_SRC:",
                        frame.src
                    );
                }

                const doc =
                    frame.contentDocument ||
                    frame.contentWindow?.document;

                if (!doc) {
                    return;
                }

                const iframeHtml =
                    doc.documentElement.outerHTML || "";

                if (!iframeHtml) {
                    return;
                }

                // =====================================
                // IFRAME HLS MANIFEST URL
                // =====================================

                try {

                    const hlsRegex =
                        /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                    let hlsMatch;

                    while (
                        (hlsMatch = hlsRegex.exec(iframeHtml)) !== null
                    ) {

                        try {

                            let clean =
                                String(hlsMatch[1])
                                    .replace(/\\u0026/g, "&")
                                    .replace(/\\u003d/g, "=")
                                    .replace(/\\u003f/g, "?")
                                    .replace(/\\u002f/g, "/")
                                    .replace(/\\\//g, "/")
                                    .replace(/&amp;/g, "&");

                            try {
                                clean =
                                    decodeURIComponent(clean);
                            } catch(e) {}

                            if (clean) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_IFRAME_HLS_MANIFEST:",
                                    clean
                                );
                            }

                        } catch(e) {}
                    }

                } catch(e) {}

                // =====================================
                // IFRAME DIRECT MEDIA URLS
                // =====================================

                try {

                    const mediaRegex =
                        /https?:\\?\/\\?\/[^"'\\s<>]+?(m3u8|mpd|mp4|m4s|ts|videoplayback)[^"'\\s<>]*/gi;

                    let match;

                    while (
                        (match = mediaRegex.exec(iframeHtml)) !== null
                    ) {

                        try {

                            let clean =
                                String(match[0])
                                    .replace(/\\u0026/g, "&")
                                    .replace(/\\u003d/g, "=")
                                    .replace(/\\u003f/g, "?")
                                    .replace(/\\u002f/g, "/")
                                    .replace(/\\\//g, "/")
                                    .replace(/&amp;/g, "&");

                            try {
                                clean =
                                    decodeURIComponent(clean);
                            } catch(e) {}

                            if (clean) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_IFRAME_MEDIA:",
                                    clean
                                );
                            }

                        } catch(e) {}
                    }

                } catch(e) {}

                // =====================================
                // IFRAME ELEMENT SOURCES
                // =====================================

                try {

                    doc
                        .querySelectorAll("video, audio, source, a")
                        .forEach(function(el) {

                            try {

                                if (el.src) {

                                    gelPush(
                                        el.src
                                    );

                                    console.log(
                                        "GEL_IFRAME_ELEMENT_SRC:",
                                        el.src
                                    );
                                }

                                if (el.href) {

                                    gelPush(
                                        el.href
                                    );

                                    console.log(
                                        "GEL_IFRAME_ELEMENT_HREF:",
                                        el.href
                                    );
                                }

                                if (el.currentSrc) {

                                    gelPush(
                                        el.currentSrc
                                    );

                                    console.log(
                                        "GEL_IFRAME_CURRENT_SRC:",
                                        el.currentSrc
                                    );
                                }

                            } catch(e) {}
                        });

                } catch(e) {}

            } catch(e) {

                console.log(
                    "GEL_IFRAME_CROSS_ORIGIN_LOCKED"
                );
            }
        });

} catch(e) {}

// =====================================
// YOUTUBE EMBED / WATCH DEEP SCAN
// =====================================

try {

    function gelExtractYoutubeId(u) {

        try {

            const url =
                String(u);

            if (
                url.indexOf("youtube.com/embed/") !== -1
            ) {

                return url
                    .split("youtube.com/embed/")[1]
                    .split("?")[0]
                    .split("&")[0]
                    .split("/")[0]
                    .trim();
            }

            if (
                url.indexOf("youtube.com/watch") !== -1
            ) {

                const parsed =
                    new URL(url);

                return (
                    parsed.searchParams.get("v") || ""
                ).trim();
            }

            if (
                url.indexOf("youtu.be/") !== -1
            ) {

                return url
                    .split("youtu.be/")[1]
                    .split("?")[0]
                    .split("&")[0]
                    .split("/")[0]
                    .trim();
            }

        } catch(e) {}

        return "";
    }

    document
        .querySelectorAll("iframe, a, script")
        .forEach(function(el) {

            try {

                const candidates =
                    [];

                if (el.src) {
                    candidates.push(el.src);
                }

                if (el.href) {
                    candidates.push(el.href);
                }

                if (el.textContent) {
                    candidates.push(el.textContent);
                }

                candidates.forEach(function(raw) {

                    try {

                        const txt =
                            String(raw);

                        const regex =
                            /(https?:\\?\/\\?\/)?(www\.)?(youtube\.com\/embed\/|youtube\.com\/watch\?v=|youtu\.be\/)[A-Za-z0-9_-]{6,}/gi;

                        let match;

                        while (
                            (match = regex.exec(txt)) !== null
                        ) {

                            let clean =
                                String(match[0])
                                    .replace(/\\u0026/g, "&")
                                    .replace(/\\u003d/g, "=")
                                    .replace(/\\u003f/g, "?")
                                    .replace(/\\u002f/g, "/")
                                    .replace(/\\\//g, "/")
                                    .replace(/&amp;/g, "&")
                                    .trim();

                            if (
                                clean.indexOf("http") !== 0
                            ) {
                                clean =
                                    "https://" + clean;
                            }

                            const videoId =
                                gelExtractYoutubeId(
                                    clean
                                );

                            if (videoId) {

                                const watchUrl =
                                    "https://www.youtube.com/watch?v=" + videoId;

                                gelPush(
                                    watchUrl
                                );

                                console.log(
                                    "GEL_YOUTUBE_WATCH_EXTRACTED:",
                                    watchUrl
                                );
                            }
                        }

                    } catch(e) {}
                });

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// HTML REGEX SCAN
// =====================================

try {

    const html =
        document.documentElement.outerHTML || "";

    // =====================================
    // HTML HLS MANIFEST URL SCAN
    // =====================================

    try {

        const hlsRegex =
            /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

        let hlsMatch;

        while (
            (hlsMatch = hlsRegex.exec(html)) !== null
        ) {

            try {

                let clean =
                    String(hlsMatch[1])
                        .replace(/\\u0026/g, "&")
                        .replace(/\\u003d/g, "=")
                        .replace(/\\u003f/g, "?")
                        .replace(/\\u002f/g, "/")
                        .replace(/\\\//g, "/")
                        .replace(/&amp;/g, "&");

                try {
                    clean =
                        decodeURIComponent(clean);
                } catch(e) {}

                if (
                    clean &&
                    (
                        clean.indexOf(".m3u8") !== -1 ||
                        clean.indexOf("manifest/hls") !== -1 ||
                        clean.indexOf("hls_playlist") !== -1
                    )
                ) {

                    gelPush(
                        clean
                    );

                    console.log(
                        "GEL_HTML_HLS_MANIFEST:",
                        clean
                    );
                }

            } catch(e) {}
        }

    } catch(e) {}

    // =====================================
    // STRONG HTML MEDIA REGEX
    // =====================================

    const regex =
        /(https?:\\?\/\\?\/[^"'\\s<>]+?(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s<>]*)?)/gi;

    let match;

    while (
        (match = regex.exec(html)) !== null
    ) {

        try {

            let clean =
                String(match[1])
                    .replace(/\\u0026/g, "&")
                    .replace(/\\u003d/g, "=")
                    .replace(/\\u003f/g, "?")
                    .replace(/\\u002f/g, "/")
                    .replace(/\\\//g, "/")
                    .replace(/&amp;/g, "&");

            try {
                clean =
                    decodeURIComponent(clean);
            } catch(e) {}

            gelPush(
                clean
            );

            console.log(
                "GEL_HTML_MEDIA:",
                clean
            );

        } catch(e) {}
    }

} catch(e) {}

// =====================================
// SCRIPT SCAN
// =====================================

try {

    document
    .querySelectorAll("script")
    .forEach(function(sc) {

        try {

            const txt =
                sc.innerHTML || "";

            const regex =
/(https?:\/\/[^"'\\s]+?\.(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s]*)?)/gi;

            let match;

            while (
                (match = regex.exec(txt)) !== null
            ) {

                gelPush(match[1]);

                console.log(
                    "GEL_SCRIPT_MEDIA:",
                    match[1]
                );
            }

        } catch(e) {}
    });

} catch(e) {}

// =====================================
// STRONG HLS / M3U8 HUNT
// =====================================

try {

    const html =
        document.documentElement.outerHTML || "";

    function gelCleanMediaUrl(u) {

        try {

            return String(u)
                .replace(/\\u0026/g, "&")
                .replace(/\\u003d/g, "=")
                .replace(/\\u003f/g, "?")
                .replace(/\\u002f/g, "/")
                .replace(/\\\//g, "/")
                .replace(/&amp;/g, "&")
                .trim();

        } catch(e) {

            return u;
        }
    }

    // =====================================
    // DIRECT M3U8
    // =====================================

    try {

        const directRegex =
            /https?:\\?\/\\?\/[^"'<>\\s]+?\.m3u8[^"'<>\\s]*/gi;

        let match;

        while (
            (match = directRegex.exec(html)) !== null
        ) {

            const clean =
                gelCleanMediaUrl(match[0]);

            if (
                clean &&
                clean.indexOf(".m3u8") !== -1
            ) {

                gelPush(clean);

                console.log(
                    "GEL_STRONG_M3U8:",
                    clean
                );
            }
        }

    } catch(e) {}

    // =====================================
    // HLS MANIFEST URL
    // =====================================

    try {

        const hlsRegex =
            /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

        let hls;

        while (
            (hls = hlsRegex.exec(html)) !== null
        ) {

            const clean =
                gelCleanMediaUrl(hls[1]);

            if (
                clean &&
                (
                    clean.indexOf(".m3u8") !== -1 ||
                    clean.indexOf("manifest/hls") !== -1 ||
                    clean.indexOf("hls_playlist") !== -1
                )
            ) {

                gelPush(clean);

                console.log(
                    "GEL_HLS_MANIFEST_URL:",
                    clean
                );
            }
        }

    } catch(e) {}

    // =====================================
    // PLAYER CONFIG KEYS
    // =====================================

    try {

        const playerRegex =
            /(file|src|source|stream|hls|url)["']?\s*[:=]\s*["']([^"']+?\.m3u8[^"']*)["']/gi;

        let player;

        while (
            (player = playerRegex.exec(html)) !== null
        ) {

            const clean =
                gelCleanMediaUrl(player[2]);

            if (
                clean &&
                clean.indexOf(".m3u8") !== -1
            ) {

                gelPush(clean);

                console.log(
                    "GEL_PLAYER_M3U8_KEY:",
                    clean
                );
            }
        }

    } catch(e) {}

} catch(e) {}

// =====================================
// INLINE JSON PLAYER SCAN
// =====================================

try {

    const html =
        document.documentElement
            .outerHTML || "";

    const jsonRegex =

/"(https?:\/\/[^"]+?\.(m3u8|mpd|mp4)(\?[^"]*)?)"/gi;

    let match;

    while (
        (match = jsonRegex.exec(html)) !== null
    ) {

        try {

            const found =
                match[1];

            gelPush(found);

            console.log(
                "GEL_JSON_MEDIA:",
                found
            );

        } catch(e) {}
    }

} catch(e) {}

// =====================================
// SCRIPT JSON DEEP MEDIA SCAN
// =====================================

try {

    document
        .querySelectorAll("script")
        .forEach(function(script) {

            try {

                const txt =
                    script.textContent || "";

                if (!txt) {
                    return;
                }

                const looksInteresting =
                    txt.indexOf(".m3u8") !== -1 ||
                    txt.indexOf("hlsManifestUrl") !== -1 ||
                    txt.indexOf("dashManifestUrl") !== -1 ||
                    txt.indexOf("manifest/hls") !== -1 ||
                    txt.indexOf("hls_playlist") !== -1 ||
                    txt.indexOf(".mpd") !== -1 ||
                    txt.indexOf("googlevideo.com") !== -1 ||
                    txt.indexOf("videoplayback") !== -1 ||
                    txt.indexOf("jwpsrv.com") !== -1;

                if (!looksInteresting) {
                    return;
                }

                console.log(
                    "GEL_SCRIPT_JSON_FOUND"
                );

                // =====================================
                // HLS MANIFEST URL
                // =====================================

                try {

                    const hlsRegex =
                        /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                    let hlsMatch;

                    while (
                        (hlsMatch = hlsRegex.exec(txt)) !== null
                    ) {

                        try {

                            let clean =
                                String(hlsMatch[1])
                                    .replace(/\\u0026/g, "&")
                                    .replace(/\\u003d/g, "=")
                                    .replace(/\\u003f/g, "?")
                                    .replace(/\\u002f/g, "/")
                                    .replace(/\\\//g, "/")
                                    .replace(/&amp;/g, "&");

                            try {
                                clean =
                                    decodeURIComponent(clean);
                            } catch(e) {}

                            if (clean) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_SCRIPT_HLS_MANIFEST:",
                                    clean
                                );
                            }

                        } catch(e) {}
                    }

                } catch(e) {}

                // =====================================
                // DASH MANIFEST URL
                // =====================================

                try {

                    const dashRegex =
                        /dashManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                    let dashMatch;

                    while (
                        (dashMatch = dashRegex.exec(txt)) !== null
                    ) {

                        try {

                            let clean =
                                String(dashMatch[1])
                                    .replace(/\\u0026/g, "&")
                                    .replace(/\\u003d/g, "=")
                                    .replace(/\\u003f/g, "?")
                                    .replace(/\\u002f/g, "/")
                                    .replace(/\\\//g, "/")
                                    .replace(/&amp;/g, "&");

                            try {
                                clean =
                                    decodeURIComponent(clean);
                            } catch(e) {}

                            if (clean) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_SCRIPT_DASH_MANIFEST:",
                                    clean
                                );
                            }

                        } catch(e) {}
                    }

                } catch(e) {}

                // =====================================
                // DIRECT MEDIA URLS
                // =====================================

                try {

                    const mediaRegex =
                        /https?:\\?\/\\?\/[^"'\\s<>]+?(m3u8|mpd|mp4|m4s|ts|videoplayback)[^"'\\s<>]*/gi;

                    let match;

                    while (
                        (match = mediaRegex.exec(txt)) !== null
                    ) {

                        try {

                            let clean =
                                String(match[0])
                                    .replace(/\\u0026/g, "&")
                                    .replace(/\\u003d/g, "=")
                                    .replace(/\\u003f/g, "?")
                                    .replace(/\\u002f/g, "/")
                                    .replace(/\\\//g, "/")
                                    .replace(/&amp;/g, "&");

                            try {
                                clean =
                                    decodeURIComponent(clean);
                            } catch(e) {}

                            if (clean) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_SCRIPT_MEDIA:",
                                    clean
                                );
                            }

                        } catch(e) {}
                    }

                } catch(e) {}

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// MUTATION OBSERVER DEEP MEDIA SCAN
// =====================================

try {

    if (!window.__gelMutationMediaHooked) {

        window.__gelMutationMediaHooked = true;

        function gelCleanMutationUrl(u) {

            try {

                let clean =
                    String(u)
                        .replace(/\\u0026/g, "&")
                        .replace(/\\u003d/g, "=")
                        .replace(/\\u003f/g, "?")
                        .replace(/\\u002f/g, "/")
                        .replace(/\\\//g, "/")
                        .replace(/&amp;/g, "&")
                        .trim();

                try {
                    clean =
                        decodeURIComponent(clean);
                } catch(e) {}

                return clean;

            } catch(e) {

                return "";
            }
        }

        function gelMutationLooksMedia(u) {

            try {

                const lower =
                    String(u).toLowerCase();

                return (
                    lower.indexOf(".m3u8") !== -1 ||
                    lower.indexOf("manifest/hls") !== -1 ||
                    lower.indexOf("hls_playlist") !== -1 ||
                    lower.indexOf(".mpd") !== -1 ||
                    lower.indexOf(".mp4") !== -1 ||
                    lower.indexOf(".m4s") !== -1 ||
                    lower.indexOf(".ts") !== -1 ||
                    lower.indexOf("videoplayback") !== -1 ||
                    lower.indexOf("googlevideo.com") !== -1 ||
                    lower.indexOf("jwpsrv.com") !== -1 ||
                    lower.indexOf("playlist") !== -1 ||
                    lower.indexOf("manifest") !== -1
                );

            } catch(e) {

                return false;
            }
        }

        function gelScanMutationNode(node) {

            try {

                if (!node) {
                    return;
                }

                if (node.src) {

                    const clean =
                        gelCleanMutationUrl(node.src);

                    if (
                        clean &&
                        gelMutationLooksMedia(clean)
                    ) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_MUTATION_SRC:",
                            clean
                        );
                    }
                }

                if (node.currentSrc) {

                    const clean =
                        gelCleanMutationUrl(node.currentSrc);

                    if (
                        clean &&
                        gelMutationLooksMedia(clean)
                    ) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_MUTATION_CURRENT_SRC:",
                            clean
                        );
                    }
                }

                if (node.href) {

                    const clean =
                        gelCleanMutationUrl(node.href);

                    if (
                        clean &&
                        gelMutationLooksMedia(clean)
                    ) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_MUTATION_HREF:",
                            clean
                        );
                    }
                }

                if (node.textContent) {

                    const txt =
                        String(node.textContent);

                    if (
                        gelMutationLooksMedia(txt)
                    ) {

                        const regex =
                            /https?:\\?\/\\?\/[^"'\\s<>]+?(m3u8|mpd|mp4|m4s|ts|videoplayback)[^"'\\s<>]*/gi;

                        let match;

                        while (
                            (match = regex.exec(txt)) !== null
                        ) {

                            const clean =
                                gelCleanMutationUrl(
                                    match[0]
                                );

                            if (clean) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_MUTATION_TEXT_MEDIA:",
                                    clean
                                );
                            }
                        }
                    }
                }

                if (node.querySelectorAll) {

                    node
                        .querySelectorAll("video, audio, source, iframe, script, a")
                        .forEach(function(child) {

                            try {

                                gelScanMutationNode(
                                    child
                                );

                            } catch(e) {}
                        });
                }

            } catch(e) {}
        }

        const observer =
            new MutationObserver(function(mutations) {

                try {

                    mutations.forEach(function(mutation) {

                        try {

                            mutation.addedNodes.forEach(function(node) {

                                try {

                                    gelScanMutationNode(
                                        node
                                    );

                                } catch(e) {}
                            });

                        } catch(e) {}
                    });

                } catch(e) {}
            });

        observer.observe(
            document.documentElement,
            {
                childList: true,
                subtree: true
            }
        );

        console.log(
            "GEL_MUTATION_OBSERVER_READY"
        );
    }

} catch(e) {}

// =====================================
// MEDIA ATTRIBUTE HOOK SCAN
// =====================================

try {

    if (!window.__gelMediaAttributeHooked) {

        window.__gelMediaAttributeHooked = true;

        function gelCleanAttributeUrl(u) {

            try {

                let clean =
                    String(u)
                        .replace(/\\u0026/g, "&")
                        .replace(/\\u003d/g, "=")
                        .replace(/\\u003f/g, "?")
                        .replace(/\\u002f/g, "/")
                        .replace(/\\\//g, "/")
                        .replace(/&amp;/g, "&")
                        .trim();

                try {
                    clean =
                        decodeURIComponent(clean);
                } catch(e) {}

                return clean;

            } catch(e) {

                return "";
            }
        }

        function gelAttributeLooksMedia(u) {

            try {

                const lower =
                    String(u).toLowerCase();

                return (
                    lower.indexOf(".m3u8") !== -1 ||
                    lower.indexOf("manifest/hls") !== -1 ||
                    lower.indexOf("hls_playlist") !== -1 ||
                    lower.indexOf(".mpd") !== -1 ||
                    lower.indexOf(".mp4") !== -1 ||
                    lower.indexOf(".m4s") !== -1 ||
                    lower.indexOf(".ts") !== -1 ||
                    lower.indexOf("videoplayback") !== -1 ||
                    lower.indexOf("googlevideo.com") !== -1 ||
                    lower.indexOf("jwpsrv.com") !== -1 ||
                    lower.indexOf("playlist") !== -1 ||
                    lower.indexOf("manifest") !== -1 ||
                    lower.indexOf("chunklist") !== -1 ||
                    lower.indexOf("live") !== -1
                );

            } catch(e) {

                return false;
            }
        }

        // =====================================
        // setAttribute HOOK
        // =====================================

        try {

            const originalSetAttribute =
                Element.prototype.setAttribute;

            Element.prototype.setAttribute =
                function(name, value) {

                    try {

                        const attr =
                            String(name || "")
                                .toLowerCase();

                        if (
                            attr === "src" ||
                            attr === "href" ||
                            attr === "data-src" ||
                            attr === "poster"
                        ) {

                            const clean =
                                gelCleanAttributeUrl(
                                    value
                                );

                            if (
                                clean &&
                                gelAttributeLooksMedia(clean)
                            ) {

                                gelPush(
                                    clean
                                );

                                console.log(
                                    "GEL_ATTRIBUTE_MEDIA:",
                                    attr,
                                    clean
                                );
                            }
                        }

                    } catch(e) {}

                    return originalSetAttribute.apply(
                        this,
                        arguments
                    );
                };

        } catch(e) {}

        // =====================================
        // VIDEO SRC PROPERTY HOOK
        // =====================================

        try {

            const videoDescriptor =
                Object.getOwnPropertyDescriptor(
                    HTMLMediaElement.prototype,
                    "src"
                );

            if (
                videoDescriptor &&
                videoDescriptor.set
            ) {

                Object.defineProperty(
                    HTMLMediaElement.prototype,
                    "src",
                    {
                        set: function(value) {

                            try {

                                const clean =
                                    gelCleanAttributeUrl(
                                        value
                                    );

                                if (
                                    clean &&
                                    gelAttributeLooksMedia(clean)
                                ) {

                                    gelPush(
                                        clean
                                    );

                                    console.log(
                                        "GEL_MEDIA_SRC_SET:",
                                        clean
                                    );
                                }

                            } catch(e) {}

                            return videoDescriptor.set.call(
                                this,
                                value
                            );
                        },

                        get: function() {

                            return videoDescriptor.get.call(
                                this
                            );
                        }
                    }
                );
            }

        } catch(e) {}

        console.log(
            "GEL_MEDIA_ATTRIBUTE_HOOK_READY"
        );
    }

} catch(e) {}

// =====================================
// FETCH HOOK
// =====================================

try {

if (!window.__gelFetchHooked) {

    window.__gelFetchHooked = true;

    const originalFetch =
        window.fetch;

    window.fetch =
        function() {

            try {

                let url =
                    arguments[0];

                if (
                    typeof url !== "string" &&
                    url?.url
                ) {

                    url = url.url;
                }

                if (url) {

                    gelPush(url);
                    
// =====================================
// MPD HUNT
// =====================================

try {

    const lower =
        String(url).toLowerCase();

    if (

        lower.includes(".mpd") ||
        lower.includes("dash") ||
        lower.includes("manifest")

    ) {

        console.log(
            "GEL_MPD_CANDIDATE:",
            url
        );

        gelPush(url);
    }

} catch(e) {}

                    console.log(
                        "GEL_FETCH:",
                        url
                    );
                }

            } catch(e) {}

            return originalFetch.apply(
                this,
                arguments
            );
        };
}

} catch(e) {}

// =====================================
// XHR RESPONSE BODY EXTRACTION
// =====================================

try {

    if (!window.__gelXHRResponseHook) {

        window.__gelXHRResponseHook = true;

        const originalOpen =
            XMLHttpRequest.prototype.open;

        const originalSend =
            XMLHttpRequest.prototype.send;

        XMLHttpRequest.prototype.open =
            function(method, url) {

                try {

                    this.__gelUrl =
                        url;

                    if (url) {

                        gelPush(
                            url
                        );

                        console.log(
                            "GEL_XHR_URL:",
                            url
                        );
                    }

                } catch(e) {}

                return originalOpen.apply(
                    this,
                    arguments
                );
            };

        XMLHttpRequest.prototype.send =
            function() {

                try {

                    this.addEventListener(
                        "readystatechange",
                        function() {

                            try {

                                if (
                                    this.readyState !== 4
                                ) {
                                    return;
                                }

                                const txt =
                                    this.responseText || "";

                                if (!txt) {
                                    return;
                                }
                                
// =====================================
// YOUTUBE FULL PLAYER FORENSICS
// =====================================

try {

    const hasYoutubePlayerData =
        txt.includes("ytInitialPlayerResponse") ||
        txt.includes("dashManifestUrl") ||
        txt.includes("hlsManifestUrl") ||
        txt.includes("adaptiveFormats") ||
        txt.includes("streamingData") ||
        txt.includes("googlevideo.com") ||
        txt.includes("videoplayback");

    if (hasYoutubePlayerData) {

        console.log(
            "GEL_YOUTUBE_PLAYER_DATA_FOUND"
        );

        // =====================================
        // CLEAN HELPER
        // =====================================

        function gelCleanYoutubeUrl(u) {

            try {

                u =
                    String(u)
                        .replace(/^"/, "")
                        .replace(/"$/, "")
                        .replace(/\\u0026/g, "&")
                        .replace(/\\u003d/g, "=")
                        .replace(/\\u003f/g, "?")
                        .replace(/\\u002f/g, "/")
                        .replace(/\\\//g, "/")
                        .replace(/&amp;/g, "&");

                try {
                    u =
                        decodeURIComponent(u);
                } catch(e) {}

                return u;

            } catch(e) {

                return "";
            }
        }

        // =====================================
        // hlsManifestUrl
        // =====================================

        try {

            const hlsManifestRegex =
                /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

            let hlsMatch;

            while (
                (hlsMatch = hlsManifestRegex.exec(txt)) !== null
            ) {

                try {

                    const clean =
                        gelCleanYoutubeUrl(
                            hlsMatch[1]
                        );

                    if (
                        clean &&
                        (
                            clean.indexOf(".m3u8") !== -1 ||
                            clean.indexOf("manifest/hls") !== -1 ||
                            clean.indexOf("hls_playlist") !== -1
                        )
                    ) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_YOUTUBE_HLS_MANIFEST_URL:",
                            clean
                        );
                    }

                } catch(e) {}
            }

        } catch(e) {}

        // =====================================
        // dashManifestUrl
        // =====================================

        try {

            const dashManifestRegex =
                /dashManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

            let dashMatch;

            while (
                (dashMatch = dashManifestRegex.exec(txt)) !== null
            ) {

                try {

                    const clean =
                        gelCleanYoutubeUrl(
                            dashMatch[1]
                        );

                    if (
                        clean &&
                        (
                            clean.indexOf(".mpd") !== -1 ||
                            clean.indexOf("dash") !== -1
                        )
                    ) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_YOUTUBE_DASH_MANIFEST_URL:",
                            clean
                        );
                    }

                } catch(e) {}
            }

        } catch(e) {}

        // =====================================
        // DIRECT YOUTUBE M3U8
        // =====================================

        try {

            const directHlsRegex =
                /https?:\\?\/\\?\/[^"'\\s<>]+?(\.m3u8|manifest\/hls|hls_playlist)[^"'\\s<>]*/gi;

            let hls;

            while (
                (hls = directHlsRegex.exec(txt)) !== null
            ) {

                try {

                    const clean =
                        gelCleanYoutubeUrl(
                            hls[0]
                        );

                    if (clean) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_YOUTUBE_DIRECT_HLS:",
                            clean
                        );
                    }

                } catch(e) {}
            }

        } catch(e) {}

        // =====================================
        // DIRECT YOUTUBE MPD
        // =====================================

        try {

            const directDashRegex =
                /https?:\\?\/\\?\/[^"'\\s<>]+?\.mpd[^"'\\s<>]*/gi;

            let dash;

            while (
                (dash = directDashRegex.exec(txt)) !== null
            ) {

                try {

                    const clean =
                        gelCleanYoutubeUrl(
                            dash[0]
                        );

                    if (clean) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_YOUTUBE_DIRECT_MPD:",
                            clean
                        );
                    }

                } catch(e) {}
            }

        } catch(e) {}

        // =====================================
        // GOOGLEVIDEO / VIDEOPLAYBACK
        // =====================================

        try {

            const videoPlaybackRegex =
                /https?:\\?\/\\?\/[^"'\\s<>]+?googlevideo\.com\/videoplayback[^"'\\s<>]*/gi;

            let vp;

            while (
                (vp = videoPlaybackRegex.exec(txt)) !== null
            ) {

                try {

                    const clean =
                        gelCleanYoutubeUrl(
                            vp[0]
                        );

                    if (clean) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_YOUTUBE_VIDEOPLAYBACK:",
                            clean
                        );
                    }

                } catch(e) {}
            }

        } catch(e) {}

        // =====================================
        // SIGNATURE CIPHER / URL PARAM
        // =====================================

        try {

            const cipherRegex =
                /"(url|signatureCipher|cipher)"\s*:\s*"([^"]+)"/gi;

            let cipher;

            while (
                (cipher = cipherRegex.exec(txt)) !== null
            ) {

                try {

                    const clean =
                        gelCleanYoutubeUrl(
                            cipher[2]
                        );

                    if (
                        clean &&
                        (
                            clean.indexOf("googlevideo.com") !== -1 ||
                            clean.indexOf("videoplayback") !== -1 ||
                            clean.indexOf(".m3u8") !== -1 ||
                            clean.indexOf(".mpd") !== -1
                        )
                    ) {

                        gelPush(
                            clean
                        );

                        console.log(
                            "GEL_YOUTUBE_CIPHER_URL:",
                            clean
                        );
                    }

                } catch(e) {}
            }

        } catch(e) {}
    }

} catch(e) {}                                

                                // =====================================
                                // YOUTUBE PLAYER RESPONSE DETECTION
                                // =====================================

                                try {

                                    if (
                                        txt.includes("dashManifestUrl") ||
                                        txt.includes("hlsManifestUrl") ||
                                        txt.includes("ytInitialPlayerResponse")
                                    ) {

                                    }

                                } catch(e) {}

                                // =====================================
                                // YOUTUBE DASH MANIFEST
                                // =====================================

                                try {

                                    const dashRegex =
                                        /"https:\\?\/\\?\/[^"]+?\.mpd[^"]*"/gi;

                                    let dash;

                                    while (
                                        (dash = dashRegex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(dash[0])
                                                    .replace(/^"/, "")
                                                    .replace(/"$/, "")
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            gelPush(
                                                clean
                                            );

                                            console.log(
                                                "GEL_YOUTUBE_DASH_MANIFEST:",
                                                clean
                                            );

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                                // =====================================
                                // YOUTUBE HLS MANIFEST
                                // =====================================

                                try {

                                    const hlsRegex =
                                        /"https:\\?\/\\?\/[^"]+?\.m3u8[^"]*"/gi;

                                    let hls;

                                    while (
                                        (hls = hlsRegex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(hls[0])
                                                    .replace(/^"/, "")
                                                    .replace(/"$/, "")
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            gelPush(
                                                clean
                                            );

                                            console.log(
                                                "GEL_YOUTUBE_HLS_MANIFEST:",
                                                clean
                                            );

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                                // =====================================
                                // XHR HLS MANIFEST URL SCAN
                                // =====================================

                                try {

                                    const hlsRegex =
                                        /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                                    let hlsMatch;

                                    while (
                                        (hlsMatch = hlsRegex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(hlsMatch[1])
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            if (
                                                clean &&
                                                (
                                                    clean.indexOf(".m3u8") !== -1 ||
                                                    clean.indexOf("manifest/hls") !== -1 ||
                                                    clean.indexOf("hls_playlist") !== -1
                                                )
                                            ) {

                                                gelPush(
                                                    clean
                                                );

                                                console.log(
                                                    "GEL_XHR_HLS_MANIFEST:",
                                                    clean
                                                );
                                            }

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                                // =====================================
                                // XHR DASH MANIFEST SCAN
                                // =====================================

                                try {

                                    const dashRegex =
                                        /https?:\\?\/\\?\/[^"'\\s<>]+?\.mpd[^"'\\s<>]*/gi;

                                    let dash;

                                    while (
                                        (dash = dashRegex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(dash[0])
                                                    .replace(/^"/, "")
                                                    .replace(/"$/, "")
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            gelPush(
                                                clean
                                            );

                                            console.log(
                                                "GEL_DASH_MANIFEST:",
                                                clean
                                            );

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                                // =====================================
                                // STRONG XHR MEDIA REGEX
                                // =====================================

                                try {

                                    const regex =
                                        /https?:\\?\/\\?\/[^"'\\s<>]+?(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s<>]*)?/gi;

                                    let match;

                                    while (
                                        (match = regex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(match[0])
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            gelPush(
                                                clean
                                            );

                                            console.log(
                                                "GEL_XHR_RESPONSE:",
                                                clean
                                            );

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                            } catch(e) {}
                        }
                    );

                } catch(e) {}

                return originalSend.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// PERFORMANCE API
// =====================================

try {

performance
.getEntriesByType("resource")
.forEach(function(r) {

    try {

        if (r.name) {

            gelPush(r.name);

            console.log(
                "GEL_PERF:",
                r.name
            );
        }

    } catch(e) {}
});

} catch(e) {}

// =====================================
// URL.createObjectURL HOOK
// =====================================

try {

    if (!window.__gelBlobHooked) {

        window.__gelBlobHooked = true;

        const originalCreateObjectURL =
            URL.createObjectURL;

        URL.createObjectURL =
            function(obj) {

                try {

                    const blobUrl =
                        originalCreateObjectURL.apply(
                            this,
                            arguments
                        );

                    gelPush(blobUrl);

                    console.log(
                        "GEL_CREATE_OBJECT_URL:",
                        blobUrl
                    );

                    if (
                        obj instanceof MediaSource
                    ) {

                        gelPush(
                            "mediasource://active"
                        );

                        console.log(
                            "GEL_MEDIASOURCE_OBJECT",
                            "ACTIVE"
                        );
                    }

                    return blobUrl;

                } catch(e) {

                    return originalCreateObjectURL.apply(
                        this,
                        arguments
                    );
                }
            };
    }

} catch(e) {}

// =====================================
// MSE HOOK
// =====================================

try {

if (!window.__gelMSEHooked) {

    window.__gelMSEHooked = true;

    const originalAddSourceBuffer =
        MediaSource.prototype.addSourceBuffer;

    MediaSource.prototype.addSourceBuffer =
        function(type) {

            try {

                gelPush(
                    "buffer://" + type
                );

                console.log(
                    "GEL_SOURCE_BUFFER:",
                    type
                );

            } catch(e) {}

            return originalAddSourceBuffer.apply(
                this,
                arguments
            );
        };
}

} catch(e) {}

// =====================================
// URL.CREATEOBJECTURL HOOK
// =====================================

if (!window.__gelBlobHooked) {

window.__gelBlobHooked = true;

try {

    const originalCreateObjectURL =
        URL.createObjectURL;

    URL.createObjectURL =
        function(obj) {

            try {

                const blobUrl =
                    originalCreateObjectURL.apply(
                        this,
                        arguments
                    );

                gelPush(blobUrl);

                console.log(
                    "GEL_BLOB_URL:",
                    blobUrl
                );

                // =====================================
                // TRY EXTRACT INTERNAL SOURCE
                // =====================================

                try {

                    if (obj) {

                        if (obj.type) {

                            gelPush(
                                "blob-type://" +
                                obj.type
                            );

                            console.log(
                                "GEL_BLOB_TYPE:",
                                obj.type
                            );
                        }
                    }

                } catch(e) {}

                return blobUrl;

            } catch(e) {

                return originalCreateObjectURL.apply(
                    this,
                    arguments
                );
            }
        };

} catch(e) {}

}

// =====================================
// VIDEO OBSERVER
// =====================================

try {

if (!window.__gelVideoObserver) {

    window.__gelVideoObserver = true;

    const observer =
        new MutationObserver(function() {

            document
            .querySelectorAll(
                "video, audio, source"
            )
            .forEach(function(el) {

                try {

                    if (el.src) {
                        gelPush(el.src);
                    }

                    if (el.currentSrc) {
                        gelPush(el.currentSrc);
                    }

                } catch(e) {}
            });
        });

    observer.observe(
        document.documentElement,
        {
            childList: true,
            subtree: true,
            attributes: true
        }
    );
}

} catch(e) {}

// =====================================
// AUTO PLAY
// =====================================

try {

document
.querySelectorAll("video")
.forEach(function(v) {

    try {

        v.muted = true;

        const p =
            v.play();

        if (p) {
            p.catch(function(){});
        }

    } catch(e) {}
});

} catch(e) {}

// =====================================
// JSON / WINDOW OBJECT SCAN
// =====================================

try {

    const scanText = function(txt) {

        try {

            if (!txt) return;

            const regex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s]*)?/gi;

            let match;

            while (
                (match = regex.exec(txt)) !== null
            ) {

                gelPush(match[0]);

                console.log(
                    "GEL_JSON_MEDIA:",
                    match[0]
                );
            }

        } catch(e) {}
    };

    // =================================
    // WINDOW KEYS
    // =================================

    Object.keys(window).forEach(function(k) {

        try {

            const value =
                window[k];

            if (
                typeof value === "object" ||
                typeof value === "string"
            ) {

                scanText(
                    JSON.stringify(value)
                );
            }

        } catch(e) {}
    });

    // =================================
    // SCRIPT JSON
    // =================================

    document
    .querySelectorAll(
        'script[type="application/json"], script[type="application/ld+json"]'
    )
    .forEach(function(sc) {

        try {

            scanText(
                sc.innerHTML
            );

        } catch(e) {}
    });

} catch(e) {}

// =====================================
// WORKER HOOK
// =====================================

try {

    if (!window.__gelWorkerHooked) {

        window.__gelWorkerHooked = true;

        const OriginalWorker =
            window.Worker;

        window.Worker =
            function(scriptURL, options) {

                try {

                    if (scriptURL) {

                        gelPush(scriptURL);

                        console.log(
                            "GEL_WORKER_SCRIPT:",
                            scriptURL
                        );
                    }

                } catch(e) {}

                return new OriginalWorker(
                    scriptURL,
                    options
                );
            };
    }

} catch(e) {}

// =====================================
// SERVICE WORKER SCAN
// =====================================

try {

    if (
        navigator.serviceWorker
    ) {

        navigator.serviceWorker
            .getRegistrations()
            .then(function(regs) {

                try {

                    regs.forEach(function(reg) {

                        try {

                            if (
                                reg.active &&
                                reg.active.scriptURL
                            ) {

                                gelPush(
                                    reg.active.scriptURL
                                );

                                console.log(
                                    "GEL_SERVICE_WORKER:",
                                    reg.active.scriptURL
                                );
                            }

                        } catch(e) {}
                    });

                } catch(e) {}
            });
    }

} catch(e) {}

// =====================================
// WORKER / SERVICE WORKER SCAN
// =====================================

try {

    // =====================================
    // SERVICE WORKERS
    // =====================================

    if (
        navigator.serviceWorker
    ) {

        navigator.serviceWorker
            .getRegistrations()
            .then(function(regs) {

                try {

                    regs.forEach(function(reg) {

                        try {

                            if (
                                reg.active &&
                                reg.active.scriptURL
                            ) {

                                gelPush(
                                    reg.active.scriptURL
                                );

                                console.log(
                                    "GEL_SW:",
                                    reg.active.scriptURL
                                );
                            }

                        } catch(e) {}
                    });

                } catch(e) {}
            });
    }

} catch(e) {}

// =====================================
// WORKER HOOK
// =====================================

try {

    if (!window.__gelWorkerHooked) {

        window.__gelWorkerHooked = true;

        const OriginalWorker =
            window.Worker;

        window.Worker =
            function(url, opts) {

                try {

                    if (url) {

                        gelPush(url);

                        console.log(
                            "GEL_WORKER:",
                            url
                        );
                    }

                } catch(e) {}

                return new OriginalWorker(
                    url,
                    opts
                );
            };
    }

} catch(e) {}

// =====================================
// HLS.JS LOADSOURCE HOOK
// =====================================

try {

    if (
        window.Hls &&
        window.Hls.prototype &&
        !window.__gelHlsHooked
    ) {

        window.__gelHlsHooked = true;

        const originalLoadSource =
            window.Hls.prototype.loadSource;

        window.Hls.prototype.loadSource =
            function(url) {

                try {

                    if (url) {

                        gelPush(url);

                        console.log(
                            "GEL_HLSJS_LOADSOURCE:",
                            url
                        );
                    }

                } catch(e) {}

                return originalLoadSource.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// VIDEO.JS SOURCE EXTRACTION
// =====================================

try {

    if (
        window.videojs
    ) {

        try {

            const players =
                window.videojs.getPlayers();

            Object.keys(players)
                .forEach(function(key) {

                    try {

                        const p =
                            players[key];

                        const current =

                            p.currentSource
                                ? p.currentSource()
                                : null;

                        if (
                            current &&
                            current.src
                        ) {

                            gelPush(
                                current.src
                            );

                            console.log(
                                "GEL_VIDEOJS_SOURCE:",
                                current.src
                            );
                        }

                    } catch(e) {}
                });

        } catch(e) {}
    }

} catch(e) {}

// =====================================
// DYNAMIC MEDIA WATCHER
// =====================================

try {

    if (!window.__gelDynamicWatcher) {

        window.__gelDynamicWatcher = true;

        setInterval(function() {

            try {

                // =========================
                // VIDEO TAGS
                // =========================

                document
                .querySelectorAll(
                    "video, source, audio"
                )
                .forEach(function(el) {

                    try {

                        if (el.src) {

                            gelPush(el.src);

                            console.log(
                                "GEL_DYNAMIC_MEDIA:",
                                el.src
                            );
                        }

                        if (el.currentSrc) {

                            gelPush(el.currentSrc);

                            console.log(
                                "GEL_DYNAMIC_CURRENT:",
                                el.currentSrc
                            );
                        }

                    } catch(e) {}
                });

                // =========================
                // VIDEOJS
                // =========================

                try {

                    if (window.videojs) {

                        const players =
                            window.videojs.getPlayers();

                        Object.keys(players)
                            .forEach(function(key) {

                                try {

                                    const p =
                                        players[key];

                                    const src =

                                        p.currentSource
                                            ? p.currentSource()
                                            : null;

                                    if (
                                        src &&
                                        src.src
                                    ) {

                                        gelPush(src.src);

                                        console.log(
                                            "GEL_DYNAMIC_VIDEOJS:",
                                            src.src
                                        );
                                    }

                                } catch(e) {}
                            });
                    }

                } catch(e) {}

            } catch(e) {}

        }, 2000);
    }

} catch(e) {}

// =====================================
// LIVE NETWORK RESOURCE FILTER
// =====================================

try {

    if (!window.__gelPerfWatcher) {

        window.__gelPerfWatcher = true;

        setInterval(function() {

            try {

                performance
                .getEntriesByType("resource")
                .forEach(function(r) {

                    try {

                        if (!r.name) {
                            return;
                        }

                        const u =
                            r.name.toLowerCase();

                        // =====================
                        // MEDIA FILTER
                        // =====================

                        if (

                            u.includes(".m3u8") ||
                            u.includes(".mpd") ||
                            u.includes(".ts") ||
                            u.includes(".m4s") ||
                            u.includes("manifest") ||
                            u.includes("playlist") ||
                            u.includes("chunklist") ||
                            u.includes("videoplayback")

                        ) {

                            gelPush(r.name);

                            console.log(
                                "GEL_PERF_MEDIA:",
                                r.name
                            );
                        }

                    } catch(e) {}
                });

            } catch(e) {}

        }, 1500);
    }

} catch(e) {}

// =====================================
// FETCH RESPONSE BODY EXTRACTION
// =====================================

try {

    if (!window.__gelFetchResponseHook) {

        window.__gelFetchResponseHook = true;

        const gelPreviousFetch =
            window.fetch;

        window.fetch =
            async function() {

                let response;

                try {

                    response =
                        await gelPreviousFetch.apply(
                            this,
                            arguments
                        );

                } catch(e) {

                    throw e;
                }

                try {

                    const cloned =
                        response.clone();

                    cloned.text()
                        .then(function(txt) {

                            try {

                                if (!txt) {
                                    return;
                                }

                                // =====================================
                                // FETCH YOUTUBE FULL PLAYER FORENSICS
                                // =====================================

                                try {

                                    const hasYoutubePlayerData =
                                        txt.includes("ytInitialPlayerResponse") ||
                                        txt.includes("dashManifestUrl") ||
                                        txt.includes("hlsManifestUrl") ||
                                        txt.includes("adaptiveFormats") ||
                                        txt.includes("streamingData") ||
                                        txt.includes("googlevideo.com") ||
                                        txt.includes("videoplayback");

                                    if (hasYoutubePlayerData) {

                                        console.log(
                                            "GEL_FETCH_YOUTUBE_PLAYER_DATA_FOUND"
                                        );

                                        try {

                                            console.log(
                                                "GEL_FETCH_RAW_PLAYER_RESPONSE:",
                                                txt.substring(
                                                    0,
                                                    5000
                                                )
                                            );

                                        } catch(e) {}

                                        function gelCleanFetchYoutubeUrl(u) {

                                            try {

                                                u =
                                                    String(u)
                                                        .replace(/^"/, "")
                                                        .replace(/"$/, "")
                                                        .replace(/\\u0026/g, "&")
                                                        .replace(/\\u003d/g, "=")
                                                        .replace(/\\u003f/g, "?")
                                                        .replace(/\\u002f/g, "/")
                                                        .replace(/\\\//g, "/")
                                                        .replace(/&amp;/g, "&");

                                                try {
                                                    u =
                                                        decodeURIComponent(u);
                                                } catch(e) {}

                                                return u;

                                            } catch(e) {

                                                return "";
                                            }
                                        }

                                        // =====================================
                                        // FETCH hlsManifestUrl
                                        // =====================================

                                        try {

                                            const hlsManifestRegex =
                                                /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                                            let hlsMatch;

                                            while (
                                                (hlsMatch = hlsManifestRegex.exec(txt)) !== null
                                            ) {

                                                try {

                                                    const clean =
                                                        gelCleanFetchYoutubeUrl(
                                                            hlsMatch[1]
                                                        );

                                                    if (
                                                        clean &&
                                                        (
                                                            clean.indexOf(".m3u8") !== -1 ||
                                                            clean.indexOf("manifest/hls") !== -1 ||
                                                            clean.indexOf("hls_playlist") !== -1
                                                        )
                                                    ) {

                                                        gelPush(
                                                            clean
                                                        );

                                                        console.log(
                                                            "GEL_FETCH_YOUTUBE_HLS_MANIFEST_URL:",
                                                            clean
                                                        );
                                                    }

                                                } catch(e) {}
                                            }

                                        } catch(e) {}

                                        // =====================================
                                        // FETCH dashManifestUrl
                                        // =====================================

                                        try {

                                            const dashManifestRegex =
                                                /dashManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                                            let dashMatch;

                                            while (
                                                (dashMatch = dashManifestRegex.exec(txt)) !== null
                                            ) {

                                                try {

                                                    const clean =
                                                        gelCleanFetchYoutubeUrl(
                                                            dashMatch[1]
                                                        );

                                                    if (
                                                        clean &&
                                                        (
                                                            clean.indexOf(".mpd") !== -1 ||
                                                            clean.indexOf("dash") !== -1
                                                        )
                                                    ) {

                                                        gelPush(
                                                            clean
                                                        );

                                                        console.log(
                                                            "GEL_FETCH_YOUTUBE_DASH_MANIFEST_URL:",
                                                            clean
                                                        );
                                                    }

                                                } catch(e) {}
                                            }

                                        } catch(e) {}

                                        // =====================================
                                        // FETCH DIRECT YOUTUBE HLS
                                        // =====================================

                                        try {

                                            const directHlsRegex =
                                                /https?:\\?\/\\?\/[^"'\\s<>]+?(\.m3u8|manifest\/hls|hls_playlist)[^"'\\s<>]*/gi;

                                            let hls;

                                            while (
                                                (hls = directHlsRegex.exec(txt)) !== null
                                            ) {

                                                try {

                                                    const clean =
                                                        gelCleanFetchYoutubeUrl(
                                                            hls[0]
                                                        );

                                                    if (clean) {

                                                        gelPush(
                                                            clean
                                                        );

                                                        console.log(
                                                            "GEL_FETCH_YOUTUBE_DIRECT_HLS:",
                                                            clean
                                                        );
                                                    }

                                                } catch(e) {}
                                            }

                                        } catch(e) {}

                                        // =====================================
                                        // FETCH DIRECT YOUTUBE MPD
                                        // =====================================

                                        try {

                                            const directDashRegex =
                                                /https?:\\?\/\\?\/[^"'\\s<>]+?\.mpd[^"'\\s<>]*/gi;

                                            let dash;

                                            while (
                                                (dash = directDashRegex.exec(txt)) !== null
                                            ) {

                                                try {

                                                    const clean =
                                                        gelCleanFetchYoutubeUrl(
                                                            dash[0]
                                                        );

                                                    if (clean) {

                                                        gelPush(
                                                            clean
                                                        );

                                                        console.log(
                                                            "GEL_FETCH_YOUTUBE_DIRECT_MPD:",
                                                            clean
                                                        );
                                                    }

                                                } catch(e) {}
                                            }

                                        } catch(e) {}

                                        // =====================================
                                        // FETCH GOOGLEVIDEO / VIDEOPLAYBACK
                                        // =====================================

                                        try {

                                            const videoPlaybackRegex =
                                                /https?:\\?\/\\?\/[^"'\\s<>]+?googlevideo\.com\/videoplayback[^"'\\s<>]*/gi;

                                            let vp;

                                            while (
                                                (vp = videoPlaybackRegex.exec(txt)) !== null
                                            ) {

                                                try {

                                                    const clean =
                                                        gelCleanFetchYoutubeUrl(
                                                            vp[0]
                                                        );

                                                    if (clean) {

                                                        gelPush(
                                                            clean
                                                        );

                                                        console.log(
                                                            "GEL_FETCH_YOUTUBE_VIDEOPLAYBACK:",
                                                            clean
                                                        );
                                                    }

                                                } catch(e) {}
                                            }

                                        } catch(e) {}

                                        // =====================================
                                        // FETCH SIGNATURE CIPHER / URL PARAM
                                        // =====================================

                                        try {

                                            const cipherRegex =
                                                /"(url|signatureCipher|cipher)"\s*:\s*"([^"]+)"/gi;

                                            let cipher;

                                            while (
                                                (cipher = cipherRegex.exec(txt)) !== null
                                            ) {

                                                try {

                                                    const clean =
                                                        gelCleanFetchYoutubeUrl(
                                                            cipher[2]
                                                        );

                                                    if (
                                                        clean &&
                                                        (
                                                            clean.indexOf("googlevideo.com") !== -1 ||
                                                            clean.indexOf("videoplayback") !== -1 ||
                                                            clean.indexOf(".m3u8") !== -1 ||
                                                            clean.indexOf(".mpd") !== -1
                                                        )
                                                    ) {

                                                        gelPush(
                                                            clean
                                                        );

                                                        console.log(
                                                            "GEL_FETCH_YOUTUBE_CIPHER_URL:",
                                                            clean
                                                        );
                                                    }

                                                } catch(e) {}
                                            }

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                                // =====================================
                                // FETCH HLS MANIFEST URL SCAN
                                // =====================================

                                try {

                                    const hlsRegex =
                                        /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/gi;

                                    let hlsMatch;

                                    while (
                                        (hlsMatch = hlsRegex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(hlsMatch[1])
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            if (
                                                clean &&
                                                (
                                                    clean.indexOf(".m3u8") !== -1 ||
                                                    clean.indexOf("manifest/hls") !== -1 ||
                                                    clean.indexOf("hls_playlist") !== -1
                                                )
                                            ) {

                                                gelPush(
                                                    clean
                                                );

                                                console.log(
                                                    "GEL_FETCH_HLS_MANIFEST:",
                                                    clean
                                                );
                                            }

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                                // =====================================
                                // STRONG FETCH MEDIA REGEX
                                // =====================================

                                try {

                                    const regex =
                                        /https?:\\?\/\\?\/[^"'\\s<>]+?(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s<>]*)?/gi;

                                    let match;

                                    while (
                                        (match = regex.exec(txt)) !== null
                                    ) {

                                        try {

                                            let clean =
                                                String(match[0])
                                                    .replace(/\\u0026/g, "&")
                                                    .replace(/\\u003d/g, "=")
                                                    .replace(/\\u003f/g, "?")
                                                    .replace(/\\u002f/g, "/")
                                                    .replace(/\\\//g, "/")
                                                    .replace(/&amp;/g, "&");

                                            try {
                                                clean =
                                                    decodeURIComponent(clean);
                                            } catch(e) {}

                                            gelPush(
                                                clean
                                            );

                                            console.log(
                                                "GEL_FETCH_RESPONSE:",
                                                clean
                                            );

                                        } catch(e) {}
                                    }

                                } catch(e) {}

                            } catch(e) {}
                        });

                } catch(e) {}

                return response;
            };
    }

} catch(e) {}

// =====================================
// JSON.PARSE HOOK
// =====================================

try {

    if (!window.__gelJsonHooked) {

        window.__gelJsonHooked = true;

        const originalParse =
            JSON.parse;

        JSON.parse =
            function(txt) {

                try {

                    if (
                        typeof txt === "string"
                    ) {

                        const regex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4)(\?[^"'\\s]*)?/gi;

                        let match;

                        while (
                            (match = regex.exec(txt)) !== null
                        ) {

                            gelPush(
                                match[0]
                            );

                            console.log(
                                "GEL_JSON_PARSE:",
                                match[0]
                            );
                        }
                    }

                } catch(e) {}

                return originalParse.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// EVAL / FUNCTION HOOK
// =====================================

try {

    if (!window.__gelEvalHooked) {

        window.__gelEvalHooked = true;

        // =============================
        // EVAL
        // =============================

        const originalEval =
            window.eval;

        window.eval =
            function(code) {

                try {

                    if (
                        typeof code === "string"
                    ) {

                        const regex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4)(\?[^"'\\s]*)?/gi;

                        let match;

                        while (
                            (match = regex.exec(code)) !== null
                        ) {

                            gelPush(
                                match[0]
                            );

                            console.log(
                                "GEL_EVAL_MEDIA:",
                                match[0]
                            );
                        }
                    }

                } catch(e) {}

                return originalEval.apply(
                    this,
                    arguments
                );
            };

        // =============================
        // FUNCTION
        // =============================

        const originalFunction =
            window.Function;

        window.Function =
            function() {

                try {

                    const joined =
                        Array.from(arguments)
                            .join(" ");

                    const regex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4)(\?[^"'\\s]*)?/gi;

                    let match;

                    while (
                        (match = regex.exec(joined)) !== null
                    ) {

                        gelPush(
                            match[0]
                        );

                        console.log(
                            "GEL_FUNCTION_MEDIA:",
                            match[0]
                        );
                    }

                } catch(e) {}

                return originalFunction.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// WEBSOCKET HOOK
// =====================================

try {

    if (!window.__gelWebSocketHooked) {

        window.__gelWebSocketHooked = true;

        const OriginalWebSocket =
            window.WebSocket;

        window.WebSocket =
            function(url, protocols) {

                try {

                    if (url) {

                        gelPush(url);

                        console.log(
                            "GEL_WS_CONNECT:",
                            url
                        );
                    }

                } catch(e) {}

                const ws =
                    protocols
                    ? new OriginalWebSocket(
                        url,
                        protocols
                    )
                    : new OriginalWebSocket(
                        url
                    );

                // =========================
                // MESSAGE HOOK
                // =========================

                ws.addEventListener(
                    "message",
                    function(event) {

                        try {

                            const txt =
                                String(
                                    event.data
                                );

                            const regex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4)(\?[^"'\\s]*)?/gi;

                            let match;

                            while (
                                (match = regex.exec(txt)) !== null
                            ) {

                                gelPush(
                                    match[0]
                                );

                                console.log(
                                    "GEL_WS_MEDIA:",
                                    match[0]
                                );
                            }

                        } catch(e) {}
                    }
                );

                return ws;
            };
    }

} catch(e) {}

// =====================================
// SOURCEBUFFER APPEND HOOK
// =====================================

try {

    if (!window.__gelSourceBufferHooked) {

        window.__gelSourceBufferHooked = true;

        const originalAppend =
            SourceBuffer.prototype.appendBuffer;

        SourceBuffer.prototype.appendBuffer =
            function(buffer) {

                try {

                    const size =
                        buffer
                            ? (
                                buffer.byteLength ||
                                buffer.length ||
                                0
                            )
                            : 0;

                    console.log(
                        "GEL_SOURCEBUFFER_APPEND:",
                        size
                    );

                    gelPush(
                        "sourcebuffer://" +
                        size
                    );

                } catch(e) {}

                return originalAppend.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// FINAL
// =====================================

results =
[...new Set(
    results.concat(
        window.__gelMediaResults
    )
)];

// =====================================
// PLAYER OBJECT SCAN
// =====================================

try {

    const html =
        document.documentElement
            .outerHTML;

    const playerRegex =

/(file|src|source|stream|hls|dash)["']?\s*[:=]\s*["'](https?:\/\/[^"' ]+)/gi;

    let match;

    while (
        (match = playerRegex.exec(html)) !== null
    ) {

        try {

            const found =
                match[2];

            gelPush(found);

            console.log(
                "GEL_PLAYER_OBJECT:",
                found
            );

        } catch(e) {}
    }

} catch(e) {}

// =====================================
// BASE64 PLAYER CONFIG
// =====================================

try {

    const html =
        document.documentElement
            .outerHTML;

    const regex =
/atob\(["']([^"']+)["']\)/gi;

    let match;

    while (
        (match = regex.exec(html)) !== null
    ) {

        try {

            const decoded =
                atob(match[1]);

            const mediaRegex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4)(\?[^"'\\s]*)?/gi;

            let media;

            while (
                (media = mediaRegex.exec(decoded)) !== null
            ) {

                gelPush(
                    media[0]
                );

                console.log(
                    "GEL_BASE64_MEDIA:",
                    media[0]
                );
            }

        } catch(e) {}
    }

} catch(e) {}

// =====================================
// HYDRATION DATA SCAN
// =====================================

try {

    const scanText = function(txt) {

        try {

            if (!txt) {
                return;
            }

            const regex =
/https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4)(\?[^"'\\s]*)?/gi;

            let match;

            while (
                (match = regex.exec(txt)) !== null
            ) {

                gelPush(
                    match[0]
                );

                console.log(
                    "GEL_HYDRATION_MEDIA:",
                    match[0]
                );
            }

        } catch(e) {}
    };

    // =====================================
    // NEXT DATA
    // =====================================

    try {

        const nextData =
            document.getElementById(
                "__NEXT_DATA__"
            );

        if (nextData?.textContent) {

            scanText(
                nextData.textContent
            );
        }

    } catch(e) {}

    // =====================================
    // WINDOW OBJECTS
    // =====================================

    Object.keys(window)
        .forEach(function(k) {

            try {

                const value =
                    window[k];

                if (

                    typeof value === "object" ||

                    typeof value === "string"

                ) {

                    scanText(
                        JSON.stringify(value)
                    );
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// SOURCE BUFFER HOOK
// =====================================

try {

    if (!window.__gelSourceBufferHook) {

        window.__gelSourceBufferHook = true;

        const originalAppendBuffer =
            SourceBuffer.prototype.appendBuffer;

        SourceBuffer.prototype.appendBuffer =
            function(buffer) {

                try {

                    const size =
                        buffer?.byteLength || 0;

                    console.log(
                        "GEL_BUFFER_APPEND:",
                        size
                    );

                    gelPush(
                        "buffer-append://" + size
                    );

                } catch(e) {}

                return originalAppendBuffer.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// DRM DETECTION
// =====================================

try {

    const html =
        document.documentElement
            .outerHTML
            .toLowerCase();

    const drmSignals = [

        "widevine",
        "fairplay",
        "playready",
        "clearkey",
        "drm",
        "license",
        "licenseurl",
        "keysystem",
        "eme",
        "encryptedmediaextensions"

    ];

    drmSignals.forEach(function(sig) {

        try {

            if (html.includes(sig)) {

                console.log(
                    "GEL_DRM_SIGNAL:",
                    sig
                );

                gelPush(
                    "drm://" + sig
                );
            }

        } catch(e) {}
    });

} catch(e) {}

// =====================================
// YOUTUBE HLS / M3U8 FORENSICS
// =====================================

try {

    var html =
        document.documentElement.innerHTML || "";

    function cleanYouTubeUrl(u) {

        if (!u) {
            return "";
        }

        try {

            u = u
                .replace(/\\u0026/g, "&")
                .replace(/\\\//g, "/")
                .replace(/&amp;/g, "&");

            u = decodeURIComponent(u);

        } catch (e) {}

        return u;
    }

    var hlsMatches =
        html.match(
            /hlsManifestUrl["']?\s*[:=]\s*["']([^"']+)["']/g
        );

    if (hlsMatches) {

        hlsMatches.forEach(function(m) {

            try {

                var found =
                    m.match(
                        /["']([^"']+)["']\s*$/
                    );

                if (found && found[1]) {

                    var hlsUrl =
                        cleanYouTubeUrl(found[1]);

                    if (
                        hlsUrl.indexOf(".m3u8") !== -1 ||
                        hlsUrl.indexOf("manifest/hls") !== -1 ||
                        hlsUrl.indexOf("hls_playlist") !== -1
                    ) {

                        results.push(hlsUrl);

                        console.log(
                            "GEL_YOUTUBE_HLS:",
                            hlsUrl
                        );
                    }
                }

            } catch (e) {}
        });
    }

    var directM3u8 =
        html.match(
            /https?:\\?\/\\?\/[^"'<> ]+?\.m3u8[^"'<> ]*/g
        );

    if (directM3u8) {

        directM3u8.forEach(function(u) {

            var clean =
                cleanYouTubeUrl(u);

            if (clean) {

                results.push(clean);

                console.log(
                    "GEL_DIRECT_M3U8:",
                    clean
                );
            }
        });
    }

    var manifestHls =
        html.match(
            /https?:\\?\/\\?\/[^"'<> ]+?manifest[^"'<> ]+?hls[^"'<> ]*/g
        );

    if (manifestHls) {

        manifestHls.forEach(function(u) {

            var clean =
                cleanYouTubeUrl(u);

            if (clean) {

                results.push(clean);

                console.log(
                    "GEL_MANIFEST_HLS:",
                    clean
                );
            }
        });
    }

} catch (e) {}

// =====================================
// PERFORMANCE RESOURCE HLS / M3U8 SCAN
// =====================================

try {

    function gelCleanUrl(u) {

        if (!u) {
            return "";
        }

        try {

            u = String(u)
                .replace(/\\u0026/g, "&")
                .replace(/\\\//g, "/")
                .replace(/&amp;/g, "&");

            u = decodeURIComponent(u);

        } catch (e) {}

        return u;
    }

    function gelPushIfStream(u, tag) {

        try {

            var clean =
                gelCleanUrl(u);

            var low =
                clean.toLowerCase();

            if (
                low.indexOf(".m3u8") !== -1 ||
                low.indexOf("hlsmanifesturl") !== -1 ||
                low.indexOf("manifest/hls") !== -1 ||
                low.indexOf("hls_playlist") !== -1 ||
                low.indexOf("/api/manifest/hls") !== -1
            ) {

                results.push(clean);

                console.log(
                    tag,
                    clean
                );
            }

        } catch (e) {}
    }

    var entries =
        performance.getEntriesByType("resource");

    if (entries && entries.length) {

        entries.forEach(function(entry) {

            try {

                gelPushIfStream(
                    entry.name,
                    "GEL_PERFORMANCE_HLS:"
                );

            } catch (e) {}
        });
    }

    if (performance.getEntriesByType("navigation")) {

        performance
            .getEntriesByType("navigation")
            .forEach(function(entry) {

                try {

                    gelPushIfStream(
                        entry.name,
                        "GEL_NAVIGATION_HLS:"
                    );

                } catch (e) {}
            });
    }

} catch (e) {}

return JSON.stringify(results);

})();

""".trimIndent()

        view?.evaluateJavascript(  
            js  
        ) { value ->  

            Log.e(  
                "JS_MEDIA_SCAN",  
                value  
            )  

            try {  

                val cleaned =  
                    value  
                        ?.replace("\\u003C", "<")  
                        ?.replace("\\/", "/")  
                        ?.replace("\"[", "[")  
                        ?.replace("]\"", "]")  
                        ?: return@evaluateJavascript  

                val jsonArray =  
                    org.json.JSONArray(cleaned)  

                for (i in 0 until jsonArray.length()) {

    val foundUrl =
    jsonArray.getString(i)

saveYouTubeWatchFromUrl(
    foundUrl
)

markStreamSource(
    foundUrl,
    "JS"
)

detectAndSaveUrl(
    foundUrl
)
}

            } catch (t: Throwable) {

                Log.e(
                    "JS_PARSE",
                    "failed",
                    t
                )
            }
        }

// =====================================
// AUTO MONITOR
// =====================================

try {

    val currentUrl =
        view?.url
            ?: ""

    if (
        autoRefreshEnabled &&
        !monitorRunning &&
        (
            currentUrl.contains(".m3u8", true) ||
                currentUrl.contains(".mpd", true) ||
                currentUrl.contains("youtube.com", true) ||
                currentUrl.contains("euronews", true)
            )
    ) {

        monitorRunning =
            true

        startStreamMonitor()
    }

} catch (_: Throwable) {}

    } catch (_: Throwable) {}
}

private fun extractUrlFromText(
    text: String
): String {

    val regex =
        "(https?://[^\\s\"'<>]+)"
            .toRegex()

    return regex.find(text)
        ?.value
        ?.trim()
        ?: ""
}

// =====================================
// CLEAN MEDIA URL
// =====================================

private fun cleanMediaUrl(
    raw: String
): String {

    return try {

        Uri.decode(
            raw
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("\\\\/", "/")
                .replace("&amp;", "&")
                .replace("\\u003d", "=")
                .replace("\\u003f", "?")
                .replace("\\u002f", "/")
                .trim()
        )

    } catch (_: Throwable) {

        raw
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("\\\\/", "/")
            .replace("&amp;", "&")
            .trim()
    }
}

// =====================================
// BRUTAL HLS EXTRACTOR
// YouTube / Euronews / escaped HTML-JSON finder
// =====================================

private fun extractBrutalHlsUrlsFromText(
    text: String
): List<String> {

    val results =
        linkedSetOf<String>()

    try {

        if (text.isBlank()) {
            return emptyList()
        }

        val normalized =
            text
                .replace("\\u0026", "&")
                .replace("\\u003d", "=")
                .replace("\\u003f", "?")
                .replace("\\u002f", "/")
                .replace("\\/", "/")
                .replace("\\\\/", "/")
                .replace("&amp;", "&")

        val targets =
            listOf(
                ".m3u8",
                "hls_playlist",
                "manifest/hls",
                "/api/manifest/hls"
            )

        targets.forEach { target ->

            try {

                var searchIndex =
                    0

                while (true) {

                    val hit =
                        normalized.indexOf(
                            target,
                            searchIndex,
                            ignoreCase = true
                        )

                    if (hit < 0) {
                        break
                    }

                    val start =
                        normalized.lastIndexOf(
                            "https://",
                            hit
                        )

                    if (start >= 0) {

                        var end =
                            hit + target.length

                        while (
                            end < normalized.length &&
                            !normalized[end].isWhitespace() &&
                            normalized[end] != '"' &&
                            normalized[end] != '\'' &&
                            normalized[end] != '<' &&
                            normalized[end] != '>' &&
                            normalized[end] != '\\'
                        ) {

                            end++
                        }

                        val raw =
                            normalized
                                .substring(
                                    start,
                                    end
                                )
                                .trim()
                                .trimEnd(',')
                                .trimEnd(';')
                                .trimEnd(')')
                                .trimEnd(']')
                                .trimEnd('}')

                        val clean =
                            try {

                                Uri.decode(raw)

                            } catch (_: Throwable) {

                                raw
                            }

                        if (
                            clean.startsWith(
                                "http",
                                true
                            ) &&
                            (
                                clean.contains(
                                    ".m3u8",
                                    true
                                ) ||
                                    clean.contains(
                                        "hls_playlist",
                                        true
                                    ) ||
                                    clean.contains(
                                        "manifest/hls",
                                        true
                                    ) ||
                                    clean.contains(
                                        "/api/manifest/hls",
                                        true
                                    )
                            )
                        ) {

                            results.add(
                                clean
                            )
                        }
                    }

                    searchIndex =
                        hit + target.length
                }

            } catch (_: Throwable) {}
        }

    } catch (_: Throwable) {}

    return results.toList()
}

// =====================================
// EXTRACT M3U8 URLS FROM TEXT
// =====================================

private fun extractM3u8UrlsFromText(
    text: String
): List<String> {

    val results =
        mutableSetOf<String>()

    try {

        val patterns =
            listOf(

                // Direct normal URLs
                "https?://[^\"'\\s<>]+?\\.m3u8[^\"'\\s<>]*",

                // Escaped JSON URLs
                "https?:\\\\/\\\\/[^\"'\\s<>]+?\\.m3u8[^\"'\\s<>]*",

                // YouTube / player hlsManifestUrl style
                "hlsManifestUrl[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",

                // Generic player keys
                "(file|src|source|stream|hls|url)[\"']?\\s*[:=]\\s*[\"']([^\"']+?\\.m3u8[^\"']*)[\"']"
            )

        patterns.forEach { pattern ->

            try {

                val regex =
                    pattern.toRegex(
                        setOf(
                            RegexOption.IGNORE_CASE,
                            RegexOption.MULTILINE
                        )
                    )

                regex.findAll(text)
                    .forEach { match ->

                        val found =
                            when {

                                match.groupValues.size >= 3 &&
                                    match.groupValues[2].contains(
                                        ".m3u8",
                                        true
                                    ) ->
                                    match.groupValues[2]

                                match.groupValues.size >= 2 &&
                                    match.groupValues[1].contains(
                                        ".m3u8",
                                        true
                                    ) ->
                                    match.groupValues[1]

                                else ->
                                    match.value
                            }

                        val clean =
                            cleanMediaUrl(found)

                        if (
                            clean.contains(".m3u8", true) &&
                            clean.startsWith("http", true)
                        ) {

                            results.add(clean)
                        }
                    }

            } catch (_: Throwable) {}
        }

    } catch (_: Throwable) {}

    return results.toList()
}

// =====================================  
// HANDLE NEW INTENTS  
// =====================================  

override fun onNewIntent(  
    intent: Intent?  
) {  

    super.onNewIntent(intent)  

    setIntent(intent)  

    handleIncomingIntent()  
}  

// =====================================  
// HANDLE SHARE / OPEN WITH  
// =====================================  

private fun handleIncomingIntent() {  

    try {  

        val action =  
            intent?.action  

        val type =  
            intent?.type  

        // =====================================  
        // SHARE TEXT  
        // =====================================  

        if (  
            Intent.ACTION_SEND == action  
        ) {  

            val sharedText =  
                intent.getStringExtra(  
                    Intent.EXTRA_TEXT  
                )  

            if (  
                !sharedText.isNullOrBlank()  
            ) {  

                binding.contentMain.urlInput  
                    .setText(sharedText)  

                detectAndSaveUrl(  
                    sharedText  
                )  

                if (

    sharedText.startsWith(
        "http",
        true
    )

) {

    binding.contentMain.webview
        .loadUrl(sharedText)
}

                return  
            }  
        }  

        // =====================================  
        // ACTION VIEW  
        // =====================================  

        if (  
            Intent.ACTION_VIEW == action  
        ) {  

            val data =  
                intent?.dataString  

          Log.e(  
"PLAYER_INTENT",  
"incoming stream -> $data"

)

if (
type?.contains("video") == true ||
type?.contains("audio") == true ||
data?.contains(".m3u8") == true ||
data?.contains(".mpd") == true ||
data?.contains(".mp4") == true ||
data?.contains(".ts") == true
) {

binding.contentMain.result.append(  
    "\n\nPLAYER MODE:\n$data\n"  
)

}

if (  
                !data.isNullOrBlank()  
            ) {  

                binding.contentMain.urlInput  
                    .setText(data)  

                detectAndSaveUrl(  
                    data  
                )  

                binding.contentMain.webview  
                    .loadUrl(data)  

                return  
            }  
        }  

// =====================================
// EXTRA STREAM
// =====================================

val streamUri =

    try {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            intent?.getParcelableExtra(
                Intent.EXTRA_STREAM,
                Uri::class.java
            )

        } else {

            @Suppress("DEPRECATION")

            intent?.getParcelableExtra<Uri>(
                Intent.EXTRA_STREAM
            )
        }

    } catch (_: Throwable) {

        null
    }

if (streamUri != null) {

    val url =
        streamUri.toString()

    binding.contentMain.urlInput
        .setText(url)

    detectAndSaveUrl(
        url
    )

    binding.contentMain.webview
        .loadUrl(url)
}

} catch (t: Throwable) {

    Log.e(
        "INTENT_HANDLER",
        "failed",
        t
    )
}
}

// =====================================
// TEST STREAM ENGINE
// =====================================

private fun testStream(
    url: String
) {

    val cleanedUrl =
        url
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .trim()

    binding.contentMain.result.append(
        """

🧪 TESTING STREAM

$cleanedUrl

────────────────────

""".trimIndent()
    )

    try {

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                data =
                    Uri.parse(cleanedUrl)

                addCategory(
                    Intent.CATEGORY_BROWSABLE
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Open Stream With"
            )
        )

    } catch (_: Throwable) {

        Toast.makeText(
            this,
            "Cannot open stream",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// FETCH HTML FALLBACK BODY
// YouTube / HLS raw body retry extractor
// =====================================

private fun fetchHtmlFallbackBody(
    targetUrl: String,
    userAgent: String,
    acceptHeader: String
): String {

    return try {

        val pageUrl =
            binding.contentMain.webview.url
                ?: targetUrl

        val originUrl =
            try {

                val pageUri =
                    Uri.parse(pageUrl)

                val scheme =
                    pageUri.scheme
                        ?: "https"

                val host =
                    pageUri.host
                        ?: ""

                if (host.isNotBlank()) {
                    "$scheme://$host"
                } else {
                    pageUrl
                }

            } catch (_: Throwable) {

                pageUrl
            }

        val builder =
            Request.Builder()
                .url(targetUrl)
                .get()

        builder.header(
            "User-Agent",
            userAgent
        )

        builder.header(
            "Accept",
            acceptHeader
        )

        builder.header(
            "Accept-Language",
            "en-US,en;q=0.9"
        )

        builder.header(
            "Referer",
            pageUrl
        )

        builder.header(
            "Origin",
            originUrl
        )

        builder.header(
            "Cache-Control",
            "no-cache"
        )

        builder.header(
            "Pragma",
            "no-cache"
        )

        builder.header(
            "Connection",
            "keep-alive"
        )

        try {

            val cookies =
                CookieManager
                    .getInstance()
                    .getCookie(targetUrl)

            if (!cookies.isNullOrBlank()) {

                builder.header(
                    "Cookie",
                    cookies
                )
            }

        } catch (_: Throwable) {}

        val request =
            builder.build()

        okHttpClient
            .newCall(request)
            .execute()
            .use { response ->

                response.body
                    ?.string()
                    .orEmpty()
            }

    } catch (t: Throwable) {

        Log.e(
            "HTML_FALLBACK_BODY",
            "failed",
            t
        )

        ""
    }
}

// =====================================
// YOUTUBE GET_VIDEO_INFO HLS FALLBACK
// Tries to extract hlsvp / hls_playlist / m3u8
// =====================================

private fun tryYouTubeGetVideoInfoFallback(
    videoId: String
) {

    try {

        if (
            !videoId.matches(
                Regex("^[A-Za-z0-9_-]{11}$")
            )
        ) {
            return
        }

        val infoUrls =
            listOf(
                "https://www.youtube.com/get_video_info?video_id=$videoId&el=detailpage",
                "https://www.youtube.com/get_video_info?video_id=$videoId&el=embedded",
                "https://www.youtube.com/get_video_info?video_id=$videoId&el=player"
            )

        infoUrls.forEach { infoUrl ->

            try {

                val request =
                    Request.Builder()
                        .url(infoUrl)
                        .get()
                        .header(
                            "User-Agent",
                            binding.contentMain.webview.settings.userAgentString
                                ?: "Mozilla/5.0"
                        )
                        .header(
                            "Accept",
                            "*/*"
                        )
                        .header(
                            "Referer",
                            "https://www.youtube.com/watch?v=$videoId"
                        )
                        .header(
                            "Connection",
                            "keep-alive"
                        )
                        .build()

                okHttpClient
                    .newCall(request)
                    .enqueue(

                        object : Callback {

                            override fun onFailure(
                                call: Call,
                                e: IOException
                            ) {

                                Log.e(
                                    "YT_HLS_FALLBACK",
                                    "failed",
                                    e
                                )
                            }

                            override fun onResponse(
                                call: Call,
                                response: Response
                            ) {

                                try {

                                    val body =
                                        response.body
                                            ?.string()
                                            .orEmpty()

                                    val decodedBody =
                                        try {
                                            Uri.decode(body)
                                        } catch (_: Throwable) {
                                            body
                                        }

                                    // =====================================
                                    // hlsvp direct parameter
                                    // =====================================

                                    try {

                                        val hlsvp =
                                            decodedBody
                                                .substringAfter(
                                                    "hlsvp=",
                                                    ""
                                                )
                                                .substringBefore("&")
                                                .trim()

                                        if (
                                            hlsvp.isNotBlank() &&
                                            (
                                                hlsvp.contains(".m3u8", true) ||
                                                    hlsvp.contains("hls_playlist", true) ||
                                                    hlsvp.contains("manifest/hls", true)
                                            )
                                        ) {

                                            val cleanHls =
                                                Uri.decode(hlsvp)
                                                    .replace("\\u0026", "&")
                                                    .replace("\\/", "/")
                                                    .replace("&amp;", "&")
                                                    .trim()

                                            markStreamSource(
                                                cleanHls,
                                                "YOUTUBE_HLSVP"
                                            )

                                            detectAndSaveUrl(
                                                cleanHls
                                            )

                                            streamValidation[cleanHls] =
                                                "📺 YOUTUBE HLSVP"

                                            Log.e(
                                                "YOUTUBE_HLSVP_FOUND",
                                                cleanHls
                                            )
                                        }

                                    } catch (_: Throwable) {}

                                    // =====================================
                                    // Strong m3u8 / hls playlist extraction
                                    // =====================================

                                    try {

                                        extractBrutalHlsUrlsFromText(
                                            decodedBody
                                        ).forEach { found ->

                                            markStreamSource(
                                                found,
                                                "YOUTUBE_GET_VIDEO_INFO_HLS"
                                            )

                                            detectAndSaveUrl(
                                                found
                                            )

                                            streamValidation[found] =
                                                "📺 YOUTUBE HLS FALLBACK"

                                            Log.e(
                                                "YOUTUBE_HLS_FALLBACK_FOUND",
                                                found
                                            )
                                        }

                                    } catch (_: Throwable) {}

                                } catch (_: Throwable) {

                                } finally {

                                    response.close()
                                }
                            }
                        }
                    )

            } catch (_: Throwable) {}
        }

    } catch (_: Throwable) {}
}
    
// =====================================
// AUTO VALIDATE STREAM
// =====================================

private fun autoValidateStream(
    url: String
) {

val existingValidation =
    streamValidation[url]
        .orEmpty()

if (
    existingValidation.isNotBlank() &&
    !existingValidation.contains("FOUND") &&
    !existingValidation.contains("AUTO TEST") &&
    !existingValidation.contains("ERROR") &&
    !existingValidation.contains("YOUTUBE WATCH")
) {
    return
}

// =====================================
// SILENT PREFETCH
// =====================================

try {

    if (

        url.contains(".m3u8") ||
        url.contains(".mpd")

    ) {

        runOnUiThread {

    try {

        val safeUrl =
            JSONObject.quote(url)

        binding.contentMain.webview
            .evaluateJavascript(

                """
fetch($safeUrl)
.then(function(r) {
    return r.text();
})
.then(function(t) {

    console.log(
        "GEL_PREFETCH_OK",
        $safeUrl
    );

})
.catch(function(e) {

    console.log(
        "GEL_PREFETCH_FAIL",
        $safeUrl
    );

});
"""
            ) { }

    } catch (_: Throwable) {}
        }
    }

} catch (_: Throwable) {}

    try {

        val cleanedUrl =
            url
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .trim()
                
// =====================================
// FORCE YOUTUBE WATCH HEADERS
// Needed for YouTube HTML / HLS discovery
// =====================================

val isYoutubeWatchValidation =
    cleanedUrl.contains(
        "youtube.com/watch",
        true
    ) ||
        cleanedUrl.contains(
            "youtu.be/",
            true
        )

        // =====================================
        // REQUEST
        // =====================================

        val pageUrl =
            binding.contentMain.webview.url
                ?: cleanedUrl

        val originUrl =
            try {

                val pageUri =
                    Uri.parse(pageUrl)

                val scheme =
                    pageUri.scheme
                        ?: "https"

                val host =
                    pageUri.host
                        ?: ""

                if (host.isNotBlank()) {
                    "$scheme://$host"
                } else {
                    pageUrl
                }

            } catch (_: Throwable) {

                pageUrl
            }

        val builder =
            Request.Builder()
                .url(cleanedUrl)
                .get()

        // =====================================
        // REPLAY HEADERS
        // =====================================

        streamHeaders[cleanedUrl]
            ?.toMap()
            ?.forEach { (k, v) ->

                try {

                    if (
                        k.isNotBlank() &&
                        v.isNotBlank() &&
                        !k.equals(
                            "Accept-Encoding",
                            true
                        )
                    ) {

                        builder.header(
                            k,
                            v
                        )
                    }

                } catch (_: Throwable) {}
            }

        // =====================================
        // FALLBACK HEADERS
        // =====================================

        builder.header(
    "User-Agent",
    if (isYoutubeWatchValidation) {
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/17.0 Mobile/15E148 Safari/604.1"
    } else {
        binding.contentMain.webview
            .settings
            .userAgentString
    }
)

        builder.header(
            "Referer",
            pageUrl
        )

        builder.header(
            "Origin",
            originUrl
        )

        builder.header(
    "Accept",
    if (isYoutubeWatchValidation) {
        "text/html,application/xhtml+xml,application/xml;q=0.9,application/vnd.apple.mpegurl,*/*;q=0.8"
    } else {
        "*/*"
    }
)

if (isYoutubeWatchValidation) {

    builder.header(
        "Accept-Language",
        "en-US,en;q=0.9"
    )

    builder.header(
        "Upgrade-Insecure-Requests",
        "1"
    )
}

        builder.header(
            "Connection",
            "keep-alive"
        )

        // =====================================
        // COOKIE REPLAY
        // =====================================

        try {

            val cookies =
                CookieManager
                    .getInstance()
                    .getCookie(cleanedUrl)

            if (
                !cookies.isNullOrBlank()
            ) {

                builder.header(
                    "Cookie",
                    cookies
                )

                Log.e(
                    "COOKIE_REPLAY",
                    cookies
                )
            }

        } catch (_: Throwable) {}

        val request =
            builder.build()

        okHttpClient
            .newCall(request)
            .enqueue(

                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

streamValidation[url] =
    "❌ DEAD"

streamValidation[
    url
        .replace("\\u0026", "&")
        .replace("\\/", "/")
        .trim()
] =
    "❌ DEAD"

                        uiHandler.removeCallbacks(
                            refreshRunnable
                        )

                        uiHandler.postDelayed(
                            refreshRunnable,
                            300
                        )
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        try {

                            val code =
                                response.code
                                
// =====================================
// FINAL URL AFTER REDIRECTS
// =====================================

try {

    val finalUrl =
        response.request.url.toString()

    if (
        finalUrl != url
    ) {

        markStreamSource(
            finalUrl,
            "REDIRECT"
        )

        detectAndSaveUrl(
            finalUrl
        )

        Log.e(
            "STREAM_REDIRECT",
            "$url -> $finalUrl"
        )
    }

} catch (_: Throwable) {}                                

                            val body =
                                response.body
                                    ?.string()
                                    .orEmpty()
                                    
// =====================================
// HTML FALLBACK RETRY
// Android equivalent of curl fallback
// =====================================

try {

    val hasHlsInBody =
        body.contains(
            ".m3u8",
            true
        ) ||
            body.contains(
                "hls_playlist",
                true
            ) ||
            body.contains(
                "manifest/hls",
                true
            ) ||
            body.contains(
                "/api/manifest/hls",
                true
            )

    if (
        isYoutubeWatchValidation &&
        !hasHlsInBody
    ) {

        val fallbackBodies =
            mutableListOf<String>()

        // =====================================
        // FALLBACK 1 — iPhone Safari
        // =====================================

        fallbackBodies.add(
            fetchHtmlFallbackBody(
                cleanedUrl,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                    "Version/17.0 Mobile/15E148 Safari/604.1",
                "text/html,application/xhtml+xml,application/xml;q=0.9,application/vnd.apple.mpegurl,*/*;q=0.8"
            )
        )

        // =====================================
        // FALLBACK 2 — Android Chrome
        // =====================================

        fallbackBodies.add(
            fetchHtmlFallbackBody(
                cleanedUrl,
                "Mozilla/5.0 (Linux; Android 13; Mobile) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            )
        )

        fallbackBodies.forEach { fallbackBody ->

            try {

                if (fallbackBody.isBlank()) {
                    return@forEach
                }

                val brutalUrls =
                    extractBrutalHlsUrlsFromText(
                        fallbackBody
                    )

                brutalUrls.forEach { found ->

                    try {

                        markStreamSource(
                            found,
                            "HTML_FALLBACK_HLS"
                        )

                        detectAndSaveUrl(
                            found
                        )

                        streamValidation[found] =
                            "📺 HLS FALLBACK"

                        Log.e(
                            "HTML_FALLBACK_HLS",
                            found
                        )

                    } catch (_: Throwable) {}
                }

            } catch (_: Throwable) {}
        }
    }

} catch (_: Throwable) {}                                   
                                    
// =====================================
// CONTENT-BASED MANIFEST DETECTION
// =====================================

try {

    val looksLikeHls =

        body.contains("#EXTM3U") ||
        body.contains("#EXTINF") ||
        body.contains("#EXT-X-TARGETDURATION") ||
        body.contains("#EXT-X-STREAM-INF")

    val looksLikeDash =

        body.contains("<MPD") ||
        body.contains("<AdaptationSet") ||
        body.contains("<Representation")

    if (

        looksLikeHls &&
        !url.contains(".m3u8")

    ) {

        markStreamSource(
            url,
            "HLS_BODY"
        )

        streamValidation[url] =
            "📺 HLS BODY"

        streamScores[url] =
            (streamScores[url] ?: 0) + 1200

        Log.e(
            "HLS_BODY_DETECTED",
            url
        )
    }

    if (

        looksLikeDash &&
        !url.contains(".mpd")

    ) {

        markStreamSource(
            url,
            "DASH_BODY"
        )

        streamValidation[url] =
            "📡 DASH BODY"

        streamScores[url] =
            (streamScores[url] ?: 0) + 1200

        Log.e(
            "DASH_BODY_DETECTED",
            url
        )
    }

} catch (_: Throwable) {}                                    
                                    
// =====================================
// MANIFEST BODY MINING
// BRUTAL HLS + DASH BODY DISCOVERY
// =====================================

try {

    val foundUrls =
        linkedSetOf<String>()

    // =====================================
    // OLD REGEX LAYER
    // =====================================

    try {

        val regex =
            Regex(
                "(https?:\\\\/\\\\/[^\\\"'\\s]+?\\.(m3u8|mpd)(\\?[^\\\"'\\s]*)?)",
                RegexOption.IGNORE_CASE
            )

        regex.findAll(body)
            .forEach { match ->

                try {

                    val found =
                        match.value
                            .replace("\\/", "/")
                            .replace("\\\\/", "/")
                            .trim()

                    if (found.isNotBlank()) {

                        foundUrls.add(
                            found
                        )
                    }

                } catch (_: Throwable) {}
            }

    } catch (_: Throwable) {}

    // =====================================
    // NEW BRUTAL HLS LAYER
    // YouTube / Euronews / escaped JSON
    // =====================================

    try {

        foundUrls.addAll(
            extractBrutalHlsUrlsFromText(
                body
            )
        )

    } catch (_: Throwable) {}

    // =====================================
    // SAVE + RECURSIVE VALIDATION
    // =====================================

    foundUrls.forEach { found ->

        try {

            markStreamSource(
                found,
                "BRUTAL_BODY"
            )

            detectAndSaveUrl(
                found
            )

            if (
                found != url &&
                (
                    found.contains(
                        ".m3u8",
                        true
                    ) ||
                        found.contains(
                            ".mpd",
                            true
                        ) ||
                        found.contains(
                            "manifest/hls",
                            true
                        ) ||
                        found.contains(
                            "hls_playlist",
                            true
                        )
                )
            ) {

                autoValidateStream(
                    found
                )
            }

            Log.e(
                "BRUTAL_BODY_MANIFEST",
                found
            )

        } catch (_: Throwable) {}
    }

} catch (_: Throwable) {}                          
                                    
// =====================================
// MASTER PLAYLIST DETECTION
// =====================================

try {

    if (

        body.contains("#EXT-X-STREAM-INF")

    ) {

        streamValidation[url] =
            "👑 MASTER PLAYLIST"

        streamScores[url] =
            (streamScores[url] ?: 0) + 3000

        Log.e(
            "MASTER_PLAYLIST",
            url
        )
    }

} catch (_: Throwable) {}                                    
                                    
// =====================================
// HEADER URL EXTRACTION
// =====================================

try {

    response.headers.names()
        .forEach { name ->

            try {

                val value =
                    response.header(name)
                        .orEmpty()

                val regex =
"(https?:\\\\/\\\\/[^\"'\\\\s]+?(m3u8|mpd|mp4|m4s|ts)(\\\\?[^\"'\\\\s]*)?)"
    .toRegex()

                regex.findAll(value)
                    .forEach {

                        try {

                            val found =
                                it.value
                                    .replace("\\\\/", "/")

                            Log.e(
                                "HEADER_MEDIA",
                                found
                            )

                            detectAndSaveUrl(
                                found
                            )

                        } catch (_: Throwable) {}
                    }

            } catch (_: Throwable) {}
        }

} catch (_: Throwable) {}

// =====================================
// FINAL RESULT
// =====================================

val result =
    when {

        // =====================================
        // MASTER PLAYLIST
        // =====================================

        streamValidation[url]
            ?.contains("MASTER PLAYLIST") == true ->

            "👑 MASTER PLAYLIST"

        // =====================================
        // DRM
        // =====================================

        streamValidation[url]
            ?.contains("DRM") == true ->

            "🔐 DRM"

        // =====================================
        // LIVE HLS
        // =====================================

        code in 200..299 &&
        body.contains("#EXTM3U") &&
        !body.contains("#EXT-X-ENDLIST") ->

            "🔴 LIVE HLS"

        // =====================================
        // LIVE DASH
        // =====================================

        code in 200..299 &&
        body.contains("<MPD") &&
        (
            body.contains("dynamic") ||
            body.contains("minimumUpdatePeriod")
        ) ->

            "🔴 LIVE DASH"

        // =====================================
        // VOD HLS
        // =====================================

        code in 200..299 &&
        body.contains("#EXTM3U") ->

            "🟢 HLS"

        // =====================================
        // VOD DASH
        // =====================================

        code in 200..299 &&
        body.contains("<MPD") ->

            "🟢 DASH"

        // =====================================
        // TOKEN
        // =====================================

        code == 401 ||
        code == 403 ->

            "🔒 TOKEN"

        // =====================================
        // DEAD
        // =====================================

        code == 404 ->

            "❌ DEAD"

        // =====================================
        // REDIRECT
        // =====================================

        code in 300..399 ->

            "↪ REDIRECT"

        // =====================================
        // PARTIAL
        // =====================================

        code in 200..299 ->

            "🟡 PARTIAL"

        else ->

            "⚠ UNKNOWN"
    }

streamValidation[url] =
    result

streamValidation[cleanedUrl] =
    result

                            uiHandler.removeCallbacks(
                                refreshRunnable
                            )

                            uiHandler.postDelayed(
                                refreshRunnable,
                                300
                            )

                        } catch (_: Throwable) {

streamValidation[url] =
    "⚠ ERROR"

streamValidation[
    url
        .replace("\\u0026", "&")
        .replace("\\/", "/")
        .trim()
] =
    "⚠ ERROR"

                            uiHandler.removeCallbacks(
                                refreshRunnable
                            )

                            uiHandler.postDelayed(
                                refreshRunnable,
                                300
                            )

                        } finally {

                            response.close()
                        }
                    }
                }
            )

    } catch (_: Throwable) {

        streamValidation[url] =
            "⚠ ERROR"

        uiHandler.removeCallbacks(
            refreshRunnable
        )

        uiHandler.postDelayed(
            refreshRunnable,
            300
        )
    }
}

// =====================================
// M3U8 PARSER ENGINE
// =====================================

private fun parseM3u8Variants(
    manifestUrl: String,
    body: String
): String {

    val sb =
        StringBuilder()

    val isMaster =
        body.contains("#EXT-X-STREAM-INF")

    val isLive =
        !body.contains("#EXT-X-ENDLIST")

    val playlistType =
        when {

            isMaster && isLive ->
                "📡 LIVE MASTER PLAYLIST"

            isMaster ->
                "📺 MASTER PLAYLIST"

            isLive ->
                "🔴 LIVE MEDIA PLAYLIST"

            else ->
                "🎞 MEDIA PLAYLIST"
        }

    sb.append("\n\n")
    sb.append(playlistType)
    sb.append("\n\n")

    val lines =
        body.lines()

    var lastInfo = ""

    // =====================================
    // VIDEO VARIANTS
    // =====================================

    lines.forEach { rawLine ->

        val line =
            rawLine.trim()

        when {

            line.startsWith("#EXT-X-STREAM-INF") -> {

                lastInfo =
                    line
            }

            lastInfo.isNotBlank() &&
            line.isNotBlank() &&
            !line.startsWith("#") -> {

                val resolution =
                    Regex("RESOLUTION=([0-9]+x[0-9]+)")
                        .find(lastInfo)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?: "AUTO"

                val bandwidth =
    Regex("BANDWIDTH=([0-9]+)")
        .find(lastInfo)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { "${it / 1000} kbps" }
        ?: "UNKNOWN"

                val codecs =
                    Regex("CODECS=\"([^\"]+)\"")
                        .find(lastInfo)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?: "UNKNOWN"

                val variantUrl =
                    resolveRelativeUrl(
                        manifestUrl,
                        line
                    )
                    
// =====================================
// RECURSIVE DISCOVERY
// =====================================

try {

    if (
        !detectedStreams.contains(
            variantUrl
        )
    ) {

        detectAndSaveUrl(
            variantUrl
        )
    }

} catch (_: Throwable) {}
                    
streamResolution[variantUrl] =
    resolution

streamBandwidth[variantUrl] =
    bandwidth

streamCodec[variantUrl] =
    codecs

                sb.append("QUALITY: ")
                sb.append(resolution)
                sb.append("\n")

                sb.append("BITRATE: ")
                sb.append(bandwidth)
                sb.append("\n")

                sb.append("CODECS: ")
                sb.append(codecs)
                sb.append("\n")

                sb.append("URL:\n")
                sb.append(variantUrl)

                sb.append(
                    "\n\n────────────────────\n\n"
                )

                lastInfo = ""
            }
        }
    }

    // =====================================
    // AUDIO / SUBTITLE TRACKS
    // =====================================

    lines.forEach { rawLine ->

        val line =
            rawLine.trim()

        if (
            line.startsWith("#EXT-X-MEDIA")
        ) {

            val type =
                Regex("TYPE=([^,]+)")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: "UNKNOWN"

            val name =
                Regex("NAME=\"([^\"]+)\"")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: "UNKNOWN"

            val language =
                Regex("LANGUAGE=\"([^\"]+)\"")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: "UNKNOWN"

            val uri =
                Regex("URI=\"([^\"]+)\"")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: ""

            sb.append("TRACK TYPE: ")
            sb.append(type)
            sb.append("\n")

            sb.append("NAME: ")
            sb.append(name)
            sb.append("\n")

            sb.append("LANGUAGE: ")
            sb.append(language)
            sb.append("\n")

            if (uri.isNotBlank()) {

    sb.append("URI:\n")

    val resolvedTrack =
        resolveRelativeUrl(
            manifestUrl,
            uri
        )

    try {

        detectAndSaveUrl(
            resolvedTrack
        )

    } catch (_: Throwable) {}

    sb.append(
        resolvedTrack
    )

    sb.append("\n")
}

sb.append(
    "\n────────────────────\n\n"
)
        }
    }

    return sb.toString()
}

// =====================================
// EXTRACT M3U8 VARIANT URLS
// =====================================

private fun extractVariantUrls(
    manifestUrl: String,
    body: String
): List<String> {

    val results =
        mutableListOf<String>()

    val lines =
        body.lines()

    var lastInfo = ""

    lines.forEach { rawLine ->

        val line =
            rawLine.trim()

        when {

            line.startsWith("#EXT-X-STREAM-INF") -> {

                lastInfo = line
            }

            lastInfo.isNotBlank() &&
            line.isNotBlank() &&
            !line.startsWith("#") -> {

                val finalUrl =
                    resolveRelativeUrl(
                        manifestUrl,
                        line
                    )

                results.add(finalUrl)
                
// =====================================
// PARENT MANIFEST
// =====================================

try {

    manifestRelations[finalUrl] =
        manifestUrl

    Log.e(
        "MANIFEST_PARENT",
        "$finalUrl <- $manifestUrl"
    )

} catch (_: Throwable) {}

                lastInfo = ""
            }
        }
    }

    return results
}

// =====================================
// RESOLVE RELATIVE PLAYLIST URL
// =====================================

private fun resolveRelativeUrl(
    baseUrl: String,
    childUrl: String
): String {

    if (
        childUrl.startsWith("http://") ||
        childUrl.startsWith("https://")
    ) {
        return childUrl
    }

    return try {

        val base =
            Uri.parse(baseUrl)

        val basePath =
            base.path ?: ""

        val folder =
            if (basePath.contains("/")) {
                basePath.substringBeforeLast("/")
            } else {
                ""
            }

        val finalPath =
            if (childUrl.startsWith("/")) {
                childUrl
            } else {
                "$folder/$childUrl"
            }

        base.buildUpon()
            .path(finalPath)
            .encodedQuery(null)
            .fragment(null)
            .build()
            .toString()

    } catch (_: Throwable) {

        childUrl
    }
}

// =====================================
// CLASSIFY HLS BODY
// =====================================

private fun classifyHlsBody(
    url: String,
    body: String
): String {

    return try {

        val lowerUrl =
            url.lowercase()

        val hasExtM3u =
            body.contains(
                "#EXTM3U",
                true
            )

        val hasEndList =
            body.contains(
                "#EXT-X-ENDLIST",
                true
            )

        val hasStreamInf =
            body.contains(
                "#EXT-X-STREAM-INF",
                true
            )

        val hasMedia =
            body.contains(
                "#EXT-X-MEDIA",
                true
            )

        val hasTargetDuration =
            body.contains(
                "#EXT-X-TARGETDURATION",
                true
            )

        val hasExtInf =
            body.contains(
                "#EXTINF",
                true
            )

        val hasSegments =
            hasTargetDuration ||
                hasExtInf

        when {

            !hasExtM3u ->
                "NOT_HLS"

            hasStreamInf ||
                hasMedia ->
                "HLS_MASTER"

            hasEndList ->
                "HLS_VOD"

            !hasEndList &&
                hasSegments ->
                "HLS_LIVE"

            lowerUrl.contains("live.m3u8") &&
                hasSegments ->
                "HLS_LIVE_CANDIDATE"

            else ->
                "HLS_UNKNOWN"
        }

    } catch (_: Throwable) {

        "HLS_UNKNOWN"
    }
}

// =====================================
// DETECT + SAVE URL
// =====================================

private fun detectAndSaveUrl(
    url: String
) {

    val cleanedUrl =
        cleanDetectedUrl(
            url
        )

    val filterLower =
        cleanedUrl.lowercase()

    if (cleanedUrl.isBlank()) {
        return
    }

    if (
        !cleanedUrl.startsWith("http://", true) &&
        !cleanedUrl.startsWith("https://", true)
    ) {
        return
    }

    // =====================================
    // HARD NOISE REJECT
    // Images / ads / analytics / fragments
    // =====================================

    if (isHardNoiseUrl(cleanedUrl)) {
        return
    }

    // =====================================
    // WRAPPER / TRACKER EARLY CLEANUP
    // Extract real playable stream if wrapper contains one
    // =====================================

    try {

        if (
            isWrapperOrTrackerUrl(
                cleanedUrl
            )
        ) {

            val extractedStreams =
                expandDetectedStreamCandidate(
                    cleanedUrl
                )
                    .filter { candidate ->

                        val cleanCandidate =
                            cleanDetectedUrl(
                                candidate
                            )

                        cleanCandidate.isNotBlank() &&
                            cleanCandidate.startsWith(
                                "http",
                                true
                            ) &&
                            !isWrapperOrTrackerUrl(
                                cleanCandidate
                            ) &&
                            shouldAcceptDetectedNetworkUrl(
                                cleanCandidate
                            )
                    }
                    .distinct()

            extractedStreams.forEach { candidate ->

                try {

                    detectAndSaveUrl(
                        candidate
                    )

                } catch (_: Throwable) {}
            }

            return
        }

    } catch (_: Throwable) {}

    val isPlaylist =
        isPlaylistUrl(
            cleanedUrl
        )
        
    val isM3uList =
    isM3uListUrl(
        cleanedUrl
    )

    val isTsFallback =
        isTsFallbackUrl(
            cleanedUrl
        )

    val isDirectMedia =
        isDirectMediaUrl(
            cleanedUrl
        )

    // =====================================
    // PLAYLIST FIRST
    // If .m3u8/.mpd appears, remove pending .ts
    // =====================================

    if (isPlaylist) {

        pagePlaylistFound =
            true

        clearTsFallbackBecausePlaylistFound()
    }

    // =====================================
    // TS FALLBACK ONLY
    // .ts appears only if no playlist exists
    // =====================================

    if (isTsFallback) {

        if (pagePlaylistFound) {
            return
        }

        pendingTsFallback.add(
            cleanedUrl
        )
    }
    
// =====================================
// M3U COUNTRY/CHANNEL LIST
// Download and save separately.
// Do not treat as live stream.
// =====================================

if (isM3uList) {

    Log.e(
        "M3U_LIST_DETECTED",
        cleanedUrl
    )

    downloadAndSaveDetectedM3uList(
        cleanedUrl
    )

    return
}

    // =====================================
    // FINAL ACCEPT CHECK
    // =====================================

    if (
    !isM3uList &&
    !isPlaylist &&
    !isTsFallback &&
    !isDirectMedia
) {
    return
}

if (isM3uList) {

    Log.e(
        "M3U_LIST_DETECTED",
        cleanedUrl
    )

    binding.contentMain.result.append(
        """

M3U LIST DETECTED:
$cleanedUrl

────────────────────

        """.trimIndent()
    )
}

    Log.e(
        "MEDIA_DETECT_CLEAN",
        cleanedUrl
    )
        
// =====================================
// EARLY YOUTUBE WATCH SAVE
// =====================================

saveYouTubeWatchFromUrl(
    cleanedUrl
)
        
// =====================================
// EARLY YOUTUBE WATCH URL EXTRACT
// =====================================

try {

    val earlyLower =
        cleanedUrl.lowercase()

    val earlyUri =
        Uri.parse(cleanedUrl)

    var videoId =
        ""

    when {

        earlyLower.contains("youtube.com/watch") -> {

            videoId =
                earlyUri.getQueryParameter("v")
                    .orEmpty()
        }

        earlyLower.contains("youtube.com/embed/") -> {

            videoId =
                earlyUri.path
                    ?.substringAfterLast("/")
                    ?.substringBefore("?")
                    ?.substringBefore("&")
                    ?.trim()
                    .orEmpty()
        }

        earlyLower.contains("youtu.be/") -> {

            videoId =
                earlyUri.path
                    ?.trimStart('/')
                    ?.substringBefore("?")
                    ?.substringBefore("&")
                    ?.trim()
                    .orEmpty()
        }

        earlyLower.contains("googlevideo.com/videoplayback") -> {

            videoId =
                earlyUri.getQueryParameter("id")
                    ?.substringBefore(".")
                    ?.trim()
                    .orEmpty()
        }
    }

    if (videoId.isNotBlank()) {

        val watch =
            "https://www.youtube.com/watch?v=$videoId"

        youtubeWatchUrl =
            watch

        if (!detectedStreams.contains(watch)) {

            detectedStreams.add(
                watch
            )

            detectedVideos.add(
                watch
            )

            streamScores[watch] =
                9999

            markStreamSource(
                watch,
                "YOUTUBE_WATCH"
            )

            streamValidation[watch] =
                "YOUTUBE WATCH"

            Log.e(
                "YOUTUBE_WATCH_EARLY_SAVED",
                watch
            )
        }
    }

} catch (_: Throwable) {}

// =====================================
// DISPLAY URL
// =====================================

val displayUrl =

    try {

        val uri =
            Uri.parse(cleanedUrl)

        val host =
            uri.host.orEmpty()

        val itag =
            uri.getQueryParameter("itag")
                .orEmpty()

        val mime =

            when {

                cleanedUrl.contains(
                    "mime=video",
                    true
                ) -> "VIDEO"

                cleanedUrl.contains(
                    "mime=audio",
                    true
                ) -> "AUDIO"

                else -> "STREAM"
            }

        when {

            cleanedUrl.contains(
                "yt_live_broadcast",
                true
            ) -> {

                "[YOUTUBE LIVE] " +
                "itag=$itag " +
                mime
            }

            cleanedUrl.contains(
                ".m3u8",
                true
            ) -> {

                "[HLS] $host"
            }

            cleanedUrl.contains(
                ".mpd",
                true
            ) -> {

                "[DASH] $host"
            }

            else -> {

                host
            }
        }

    } catch (_: Throwable) {

        cleanedUrl
    }
        
// =====================================
// TOKEN FORENSICS
// =====================================

try {

    val uri =
        Uri.parse(cleanedUrl)

    val names =
        uri.queryParameterNames

    val tokenParams =
        mutableListOf<String>()

    names.forEach { key ->

        try {

            val lower =
                key.lowercase()

            if (

                lower.contains("token") ||
                lower.contains("sig") ||
                lower.contains("signature") ||
                lower.contains("expire") ||
                lower.contains("expires") ||
                lower.contains("auth") ||
                lower.contains("key") ||
                lower.contains("hash") ||
                lower.contains("session")

            ) {

                tokenParams.add(key)
            }

        } catch (_: Throwable) {}
    }

    if (tokenParams.isNotEmpty()) {

        Log.e(
            "STREAM_TOKENS",
            tokenParams.joinToString()
        )
    }

} catch (_: Throwable) {}

// =====================================
// LIVE CONFIDENCE
// =====================================

var liveConfidence = 0

if (
    cleanedUrl.contains(
        "yt_live_broadcast",
        true
    )
) {
    liveConfidence += 50
}

if (
    cleanedUrl.contains(
        "live=1",
        true
    )
) {
    liveConfidence += 30
}

if (
    cleanedUrl.contains(
        "mime=video",
        true
    )
) {
    liveConfidence += 20
}

if (
    cleanedUrl.contains(
        "googlevideo",
        true
    )
) {
    liveConfidence += 20
}

if (
    cleanedUrl.contains(
        "noclen=1",
        true
    )
) {
    liveConfidence += 10
}

if (
    cleanedUrl.contains(
        "gir=yes",
        true
    )
) {
    liveConfidence += 10
}

if (liveConfidence >= 70) {

    Log.e(
        "LIVE_CONFIDENCE",
        "$liveConfidence -> $cleanedUrl"
    )
}

// =====================================
// LIVE HEARTBEAT
// =====================================

try {

    val uri =
        Uri.parse(cleanedUrl)

    val videoId =
        uri.getQueryParameter("id")
            ?.substringBefore(".")
            .orEmpty()

    if (videoId.isNotBlank()) {

        val now =
            System.currentTimeMillis()

        val previous =
            liveHeartbeatMap[videoId] ?: 0L

        val delta =
            now - previous

        liveHeartbeatMap[videoId] =
            now

        if (
            previous > 0 &&
            delta < 15000
        ) {

            Log.e(
                "LIVE_HEARTBEAT",
                "$videoId -> ACTIVE"
            )
        }
    }

} catch (_: Throwable) {}

// =====================================
// DASH SESSION ID
// =====================================

try {

    val uri =
        Uri.parse(cleanedUrl)

    val streamId =
        uri.getQueryParameter("id")
            .orEmpty()

    if (streamId.isNotBlank()) {

        Log.e(
            "DASH_SESSION",
            streamId
        )

        if (
            cleanedUrl.contains(
                "mime=video",
                true
            )
        ) {

            Log.e(
                "DASH_VIDEO",
                streamId
            )
        }

        if (
            cleanedUrl.contains(
                "mime=audio",
                true
            )
        ) {

            Log.e(
                "DASH_AUDIO",
                streamId
            )
        }
    }

} catch (_: Throwable) {}

// =====================================
// DASH AUDIO / VIDEO MERGE
// =====================================

try {

    val uri =
        Uri.parse(cleanedUrl)

    val streamId =
        uri.getQueryParameter("id")
            ?.substringBefore(".")
            .orEmpty()

    if (streamId.isNotBlank()) {

        // =====================================
        // ITAG PARSER
        // =====================================

        val itag =

            uri.getQueryParameter("itag")
                ?.toIntOrNull()
                ?: 0

        // =============================
        // SAVE VIDEO
        // =============================

        if (

            cleanedUrl.contains(
                "mime=video",
                true
            )

        ) {

            if (itag > bestVideoItag) {

                bestVideoItag =
                    itag

                dashVideoMap[streamId] =
                    cleanedUrl

                Log.e(
                    "BEST_VIDEO_ITAG",
                    "$itag -> $cleanedUrl"
                )
            }

            Log.e(
                "DASH_VIDEO_SAVED",
                streamId
            )
        }

        // =============================
        // SAVE AUDIO
        // =============================

        if (

            cleanedUrl.contains(
                "mime=audio",
                true
            )

        ) {

            if (itag > bestAudioItag) {

                bestAudioItag =
                    itag

                dashAudioMap[streamId] =
                    cleanedUrl

                Log.e(
                    "BEST_AUDIO_ITAG",
                    "$itag -> $cleanedUrl"
                )
            }

            Log.e(
                "DASH_AUDIO_SAVED",
                streamId
            )
        }

        // =============================
        // AUTO MERGE READY
        // =============================

        val video =
            dashVideoMap[streamId]

        val audio =
            dashAudioMap[streamId]

        if (

            !video.isNullOrBlank() &&
            !audio.isNullOrBlank()

        ) {

            Log.e(
                "DASH_READY",
                streamId
            )

            Log.e(
                "DASH_VIDEO_URL",
                video
            )

            Log.e(
                "DASH_AUDIO_URL",
                audio
            )

            if (!liveLocked) {

    bestLiveUrl =
        video

    bestLiveScore += 3000

    liveLocked = true

    lockedStreamId =
        streamId

    Log.e(
        "LIVE_LOCKED",
        "$lockedStreamId -> $bestLiveUrl"
    )
}
        }
    }

} catch (_: Throwable) {}

// =====================================
// YOUTUBE LIVE ID
// =====================================

try {

    val uri =
        Uri.parse(cleanedUrl)

    val videoId =
        uri.getQueryParameter("id")
            ?.substringBefore(".")
            .orEmpty()

    if (

        cleanedUrl.contains(
            "yt_live_broadcast",
            true
        ) &&

        videoId.isNotBlank()

    ) {

        Log.e(
            "YT_LIVE_ID",
            videoId
        )
    }

} catch (_: Throwable) {}
        
// =====================================
// BEST LIVE TRACKER
// =====================================

val score =
    calculateStreamScore(
        cleanedUrl
    )

if (score > bestLiveScore) {

    bestLiveScore =
        score

    bestLiveUrl =
        cleanedUrl

    Log.e(
        "BEST_LIVE_UPDATE",
        "$bestLiveScore -> $bestLiveUrl"
    )
}

// =====================================
// BEST DETECTED TRACKER
// =====================================

val detectedScore =
    calculateStreamScore(
        cleanedUrl
    )

if (detectedScore > bestLiveScore) {

    bestLiveScore =
        detectedScore

    bestLiveUrl =
        cleanedUrl

    Log.e(
        "BEST_DETECTED_UPDATE",
        "$bestLiveScore -> $bestLiveUrl"
    )
}
       
// =====================================
// DUPLICATE FILTER
// =====================================

// =====================================
// NORMALIZED URL
// =====================================

val normalizedUrl =

    when {

        // =====================================
        // KEEP YOUTUBE WATCH FULL
        // =====================================

        cleanedUrl.contains(
            "youtube.com/watch",
            true
        ) ->
            cleanedUrl
                .substringBefore("&")
                .substringBefore("#")
                .trim()

        // =====================================
        // GROUP GOOGLEVIDEO TEMP ENDPOINTS
        // =====================================

        cleanedUrl.contains(
            "googlevideo.com/videoplayback",
            true
        ) -> {

            try {

                val uri =
                    Uri.parse(cleanedUrl)

                val id =
                    uri.getQueryParameter("id")
                        ?.substringBefore(".")
                        .orEmpty()

                val itag =
                    uri.getQueryParameter("itag")
                        .orEmpty()

                val mime =
                    uri.getQueryParameter("mime")
                        .orEmpty()

                "googlevideo://$id/$itag/$mime"

            } catch (_: Throwable) {

                cleanedUrl
                    .substringBefore("?")
                    .substringBefore("#")
                    .trim()
            }
        }

        // =====================================
        // NORMAL STREAMS
        // =====================================

        else ->
            cleanedUrl
                .substringBefore("?")
                .substringBefore("#")
                .trim()
    }

// =====================================
// SAVED URL KEY
// =====================================

val savedUrl =
    normalizedUrl

// =====================================
// STREAM HIT TRACKING
// =====================================

val currentHits =
    (streamHitCounter[normalizedUrl] ?: 0) + 1

streamHitCounter[normalizedUrl] =
    currentHits

if (currentHits >= 3) {

    Log.e(
        "STREAM_STABLE",
        "$currentHits -> $normalizedUrl"
    )
}

if (
    detectedStreams.contains(savedUrl)
) {

    streamHitCounter[savedUrl] =
        (streamHitCounter[savedUrl] ?: 0) + 1

    lastSelectedUrl =
        cleanedUrl

    if (
        cleanedUrl.contains(".m3u8", true) ||
        cleanedUrl.contains(".mpd", true) ||
        cleanedUrl.contains("youtube.com/watch", true) ||
        cleanedUrl.contains("youtu.be/", true)
    ) {

        markStreamSource(
            savedUrl,
            "REVISIT"
        )

        markStreamSource(
            cleanedUrl,
            "REVISIT"
        )

        streamValidation[savedUrl] =
            streamValidation[savedUrl]
                ?: "REVISIT"

        streamValidation[cleanedUrl] =
            streamValidation[cleanedUrl]
                ?: "REVISIT"
    }

    return
}

// =====================================
// EURONEWS / YOUTUBE DASH / HLS FLAGS
// =====================================

val lower =
    cleanedUrl
        .lowercase()

val decodedLower =
    try {

        Uri.decode(cleanedUrl)
            .lowercase()

    } catch (_: Throwable) {

        lower
    }

val isEuronewsLivePage =
    lower.contains("gr.euronews.com/live") ||
    lower.contains("euronews.com/live")

val isEuronewsLiveApi =
    lower.contains("gr.euronews.com/api/live/data") ||
    lower.contains("euronews.com/api/live/data")

val isEuronewsGeoApi =
    lower.contains("gr.euronews.com/api/geoblocking/live") ||
    lower.contains("euronews.com/api/geoblocking/live")

val isEuronewsYoutubeEmbed =
    lower.contains("youtube.com/embed") &&
    (
        lower.contains("euronews") ||
        lower.contains("uwihv9gqclg")
    )

val isYoutubeLiveDash =
    lower.contains("googlevideo.com/videoplayback") &&
    (
        lower.contains("yt_live_broadcast") ||
        lower.contains("live=1")
    )

val isYoutubeDashVideoOnly =
    isYoutubeLiveDash &&
    (
        lower.contains("mime=video%2fmp4") ||
        decodedLower.contains("mime=video/mp4") ||
        lower.contains("itag=133") ||
        lower.contains("itag=134") ||
        lower.contains("itag=135") ||
        lower.contains("itag=136") ||
        lower.contains("itag=137") ||
        lower.contains("itag=160")
    )

val isYoutubeDashAudioOnly =
    isYoutubeLiveDash &&
    (
        lower.contains("mime=audio%2fmp4") ||
        decodedLower.contains("mime=audio/mp4") ||
        lower.contains("itag=140") ||
        lower.contains("itag=141") ||
        lower.contains("itag=251")
    )

val isYoutubeHlsManifest =
    (
        lower.contains(".m3u8") ||
        lower.contains("manifest/hls") ||
        lower.contains("hls_playlist")
    ) &&
    (
        lower.contains("googlevideo.com") ||
        lower.contains("youtube.com") ||
        lower.contains("yt_live_broadcast")
    )

val isPlayableHlsStream =
    lower.contains(".m3u8") ||
    lower.contains("application/x-mpegurl") ||
    lower.contains("application/vnd.apple.mpegurl")

// =====================================
// STREAM PRIORITY SCORE
// =====================================

var streamScore =
    calculateStreamScore(
        cleanedUrl
    )

if (isYoutubeHlsManifest) {
    streamScore += 500
}

if (isPlayableHlsStream) {
    streamScore += 300
}
    
// =====================================
// BEST STREAM
// =====================================

// =====================================
// JWPLAYER / HOSTED VOD
// =====================================

val isJwPlayerVod =
    lower.contains("jwpsrv.com") ||
        lower.contains("jwplayer") ||
        (
            lower.contains("/media/") &&
                lower.contains("/versions/")
        )

val isBadBestStream =
    isJwPlayerVod ||
        isYoutubeDashVideoOnly ||
        isYoutubeDashAudioOnly ||
        isEuronewsGeoApi ||
        isEuronewsLiveApi ||
        lower.endsWith(".ts") ||
        lower.contains(".ts?") ||
        lower.contains(".m4s") ||
        lower.contains("chunklist") ||
        lower.contains("index-a") ||
        lower.contains("index-v") ||
        lower.contains("audio_") ||
        lower.contains("-audio") ||
        lower.contains("manifest-audio") ||
        lower.contains(".jpg") ||
        lower.contains(".jpeg") ||
        lower.contains(".png") ||
        lower.contains(".webp") ||
        lower.contains(".gif")

val canBeBestStream =
    !isBadBestStream &&
        (
            isYoutubeHlsManifest ||
                isPlayableHlsStream ||
                lower.contains("master.m3u8") ||
                lower.contains(".mpd") ||
                (
                    lower.contains(".mp4") &&
                        !lower.contains("/vod/") &&
                        !lower.contains("original.mp4")
                )
        )

if (
    canBeBestStream &&
    streamScore > bestStreamScore
) {

    val bestCandidateUrl =
        try {

            expandDetectedStreamCandidate(
                cleanedUrl
            )
                .filter { candidate ->

                    candidate.isNotBlank() &&
                        candidate.startsWith(
                            "http",
                            true
                        ) &&
                        !isWrapperOrTrackerUrl(
                            candidate
                        ) &&
                        isExportableStream(
                            candidate
                        )
                }
                .maxByOrNull { candidate ->

                    calculateStreamScore(
                        candidate
                    )
                }
                ?: cleanDetectedPlayableUrl(
                    repeatedlyDecodeUrl(
                        cleanedUrl
                    )
                )

        } catch (_: Throwable) {

            cleanedUrl
        }

    if (
        bestCandidateUrl.isNotBlank() &&
        !isWrapperOrTrackerUrl(
            bestCandidateUrl
        )
    ) {

        bestStreamScore =
            streamScore

        bestStreamUrl =
            bestCandidateUrl

        Log.e(
            "BEST_STREAM",
            "$bestStreamScore -> $bestStreamUrl"
        )
    }
}

// =====================================
// TOKEN FORENSICS
// =====================================

try {

    val tokenParts =
        mutableListOf<String>()

    val uri =
        Uri.parse(cleanedUrl)

    uri.queryParameterNames
        .forEach { key ->

            if (

                key.contains("token", true) ||
                key.contains("auth", true) ||
                key.contains("sig", true) ||
                key.contains("expires", true) ||
                key.contains("policy", true) ||
                key.contains("hdnts", true)

            ) {

                val value =
                    uri.getQueryParameter(key)
                        ?: ""

                tokenParts.add(
                    "$key=$value"
                )
            }
        }

    if (tokenParts.isNotEmpty()) {

        streamTokens[savedUrl] =
    tokenParts

streamTokens[cleanedUrl] =
    tokenParts

        Log.e(
            "TOKEN_FORENSICS",
            tokenParts.joinToString(" | ")
        )
    }

} catch (_: Throwable) {}

// =====================================
// BLOB RELATION
// =====================================

try {

    if (

        lower.startsWith("blob:")

    ) {

        val latestManifest =

            detectedStreams
                .lastOrNull {

                    it.contains(".m3u8") ||
                    it.contains(".mpd")
                }

        if (
            latestManifest != null
        ) {

            blobRelations[cleanedUrl] =
                latestManifest

            Log.e(
                "BLOB_RELATION",
                "$cleanedUrl -> $latestManifest"
            )
        }
    }

} catch (_: Throwable) {}

// =====================================
// STREAM SCORE ENGINE
// =====================================

val streamPriority =
    when {

        // =========================
        // JWPLAYER / HOSTED VOD
        // =========================

        isJwPlayerVod ->
            80

// =========================
// LIVE MASTER HLS
// =========================

!isJwPlayerVod &&
    lower.contains("master.m3u8") &&
    !lower.contains("/vod/") &&
    (
        lower.contains("/live/") ||
            lower.contains("livestream") ||
            lower.contains("broadcast") ||
            lower.contains("linear") ||
            lower.contains("live.m3u8")
    ) ->
    1000

        // =========================
        // LIVE HLS
        // =========================

        !isJwPlayerVod &&
            lower.contains(".m3u8") &&
            (
                lower.contains("/live/") ||
                    lower.contains("livestream") ||
                    lower.contains("broadcast") ||
                    lower.contains("linear") ||
                    lower.contains("live.m3u8")
                ) ->

            when {

                lower.contains("master") ->
                    1200

                lower.contains("live") ->
                    1150

                lower.contains("broadcast") ->
                    1120

                lower.contains("playlist") ->
                    1100

                lower.contains("chunklist") ->
                    1080

                else ->
                    1000
            }

        // =========================
        // DASH LIVE
        // =========================

        !isJwPlayerVod &&
            lower.contains(".mpd") &&
            (
                lower.contains("/live/") ||
                    lower.contains("livestream") ||
                    lower.contains("broadcast") ||
                    lower.contains("linear")
                ) ->
            900

        // =========================
        // PLAYLISTS
        // =========================

        lower.contains("playlist") ->
            300

        lower.contains("chunklist") ->
            250

        // =========================
        // DIRECT VIDEO
        // =========================

        lower.contains(".mp4") &&
            !lower.contains("/vod/") &&
            !lower.contains("trailer") &&
            !lower.contains("intro") &&
            !lower.contains("original.mp4") ->
            500

        // =========================
        // SEGMENTS
        // =========================

        lower.contains(".m4s") ||
            lower.contains(".ts") ->
            120

        // =========================
        // AUDIO STREAMS
        // =========================

        lower.contains(".mp3") ||
            lower.contains(".aac") ->
            100

        // =========================
        // IMAGES
        // =========================

        lower.contains(".jpg") ||
            lower.contains(".png") ||
            lower.contains(".webp") ->
            50

        // =========================
        // STATIC VOD
        // =========================

        lower.contains("/vod/") ->
            20

        lower.contains("original.mp4") ->
            10

        else ->
            1
    }

// =====================================
// LOW PRIORITY FILTER — CLEAN MODE
// Do not cut accepted clean media types
// =====================================

if (
    streamPriority < 20 &&
    !isPlaylist &&
    !isM3uList &&
    !isTsFallback &&
    !isDirectMedia
) {
    return
}

// =====================================  
// MEDIA TYPES  
// =====================================  

val isVideo =

    lower.contains(".m3u") ||
    lower.contains(".m3u8") ||
    lower.contains(".mpd") ||
    lower.contains(".m4s") ||
    lower.endsWith(".ts") ||
    lower.contains(".ts?") ||
    lower.contains(".mp4") ||
    lower.contains(".webm") ||
    lower.contains(".mkv") ||

    // =====================================
    // STREAM KEYWORDS
    // =====================================

    lower.contains("playlist") ||
    lower.contains("chunklist") ||
    lower.contains("manifest") ||
    lower.contains("live") ||
    lower.contains("stream") ||
    lower.contains("media") ||
    lower.contains("hls") ||
    lower.contains("dash") ||

    // =====================================
    // PLAYER / API PATTERNS
    // =====================================

    lower.contains("jwplayer") ||
    lower.contains("bitmovin") ||
    lower.contains("videojs") ||
    lower.contains("playout") ||
    lower.contains("playback") ||
    lower.contains("videoplayer") ||
    lower.contains("master") ||
    lower.contains("index") ||
    lower.contains("source") ||
    lower.contains("cdn") ||

    // =====================================
    // EURONEWS STYLE
    // =====================================

    (
        lower.contains("euronews") &&
        (
            lower.contains("api") ||
            lower.contains("video")
        )
    )

// =====================================
// IMAGE TYPES
// =====================================

val isImage =
    lower.contains(".jpg") ||  
    lower.contains(".jpeg") ||  
    lower.contains(".png") ||  
    lower.contains(".webp") ||  
    lower.contains(".gif") ||  
    lower.contains(".bmp") ||  
    lower.contains(".svg") ||  
    lower.contains(".ico")  

val isAudio =  
    lower.contains(".mp3") ||  
    lower.contains(".m4a") ||  
    lower.contains(".aac") ||  
    lower.contains(".opus") ||  
    lower.contains(".wav") ||  
    lower.contains(".ogg") ||  
    lower.contains(".flac")  

// =====================================  
// SEGMENTS  
// =====================================  

val isSegmentTs =  
(  
    lower.endsWith(".ts") ||  
    lower.contains(".ts?") ||  
    lower.endsWith(".m4s") ||  
    lower.contains(".m4s?")  
) &&  
(  
    lower.contains("seg") ||  
    lower.contains("chunk") ||  
    lower.contains("frag") ||  
    lower.contains("segment")  
)  

// =====================================  
// MASTER STREAM  
// =====================================  

val isMasterStream =  
    lower.contains(".m3u8") ||  
    lower.contains(".mpd") ||  
    lower.contains("master")  

// =====================================  
// GARBAGE FILTER  
// =====================================  

val isGarbage =  
    lower.contains("doubleclick") ||  
    lower.contains("googleads") ||  
    lower.contains("analytics") ||  
    lower.contains("facebook") ||  
    lower.contains("tracker") ||  
    lower.contains("adsystem") ||  
    lower.contains(".css") ||  
    lower.contains(".js") ||  
    lower.contains("favicon") ||  
    lower.contains("logo") ||  
    lower.contains("banner") ||  
    lower.contains("recaptcha") ||  
    lower.contains("gstatic")  
    
// =====================================
// SEGMENT FILES — CLEAN RULE
// .m4s never shown.
// .ts only if no playlist exists.
// =====================================

if (
    lower.endsWith(".m4s") ||
    lower.contains(".m4s?")
) {
    return
}

if (
    (
        lower.endsWith(".ts") ||
            lower.contains(".ts?")
    ) &&
    pagePlaylistFound
) {
    return
}

// =====================================
// SMART DUPLICATE FILTER
// =====================================

val normalizedBase =

    normalizedUrl
        .substringBefore("?")
        .substringBefore("index.m3u8")
        .substringBefore("master.m3u8")
        .substringBefore("playlist.m3u8")
        .substringBefore("chunklist.m3u8")
        .substringBefore("live.m3u8")
        .trimEnd('/')

if (

    detectedStreams.any {

        val existing =

            it
                .substringBefore("?")
                .substringBefore("index.m3u8")
                .substringBefore("master.m3u8")
                .substringBefore("playlist.m3u8")
                .substringBefore("chunklist.m3u8")
                .substringBefore("live.m3u8")
                .trimEnd('/')

        existing == normalizedBase
    }

) {

    // =====================================
    // KEEP BEST VERSION
    // =====================================

    val existingBest =

        detectedStreams.firstOrNull {

            val existing =

                it
                    .substringBefore("?")
                    .substringBefore("index.m3u8")
                    .substringBefore("master.m3u8")
                    .substringBefore("playlist.m3u8")
                    .substringBefore("chunklist.m3u8")
                    .substringBefore("live.m3u8")
                    .trimEnd('/')

            existing == normalizedBase
        }

    if (

        existingBest != null &&
        streamPriority >
        (streamScores[existingBest] ?: 0)

    ) {

        detectedStreams.remove(existingBest)

        detectedVideos.remove(existingBest)

        detectedAudio.remove(existingBest)

        detectedImages.remove(existingBest)

    } else {

        return
    }
}

// =====================================
// SAVE STREAM
// =====================================

detectedStreams.add(savedUrl)

// =====================================
// FORENSIC SOURCE
// =====================================

try {

    val source =
        when {

            lower.contains(".m3u8") ->
                "HLS"

            lower.contains(".mpd") ->
                "DASH"

            lower.contains(".ts") ||
                lower.contains(".m4s") ->
                "SEGMENT"

            lower.contains("blob:") ->
                "BLOB"

            lower.contains("manifest") ->
                "MANIFEST"

            lower.contains("playlist") ->
                "PLAYLIST"

            lower.contains("chunklist") ->
                "CHUNKLIST"

            lower.contains("live") ->
                "LIVE"

            else ->
                "NETWORK"
        }

    markStreamSource(
        savedUrl,
        source
    )

    markStreamSource(
        cleanedUrl,
        source
    )

} catch (_: Throwable) {}

// =====================================
// STREAM PRIORITY SAVE
// =====================================

streamScores[savedUrl] =
    streamPriority

streamScores[cleanedUrl] =
    streamPriority

// =====================================
// MEDIA TYPES
// =====================================

if (isVideo) {
    detectedVideos.add(savedUrl)
}

if (isImage) {
    detectedImages.add(savedUrl)
}

if (isAudio) {
    detectedAudio.add(savedUrl)
}

if (isMasterStream) {
    detectedMasterStreams.add(savedUrl)
}

// =====================================
// AUTO VALIDATE STREAM
// =====================================

try {

    if (
        lower.contains(".m3u8") ||
        lower.contains(".mpd")
    ) {

        val currentValidation =
            streamValidation[cleanedUrl]
                .orEmpty()

        if (
            currentValidation.isBlank()
        ) {

            streamValidation[savedUrl] =
                "FOUND"

            streamValidation[cleanedUrl] =
                "FOUND"
        }

        val score =
            streamScores[savedUrl]
                ?: 0

        if (
            score >= 900 &&
            (
                currentValidation.isBlank() ||
                    currentValidation.contains("FOUND") ||
                    currentValidation.contains("AUTO TEST") ||
                    currentValidation.contains("ERROR")
            )
        ) {

            streamValidation[savedUrl] =
                "⏳ AUTO TEST"

            streamValidation[cleanedUrl] =
                "⏳ AUTO TEST"

            autoValidateStream(
                cleanedUrl
            )
        }
    }

} catch (_: Throwable) {}

// =====================================
// QUALITY
// =====================================

val streamQuality =
    streamResolution[cleanedUrl]
        ?: streamResolution[savedUrl]
        ?: when {

            lower.contains("2160p") ||
                lower.contains("4k") ->
                "4K"

            lower.contains("1440p") ->
                "1440p"

            lower.contains("1080p") ->
                "1080p"

            lower.contains("900p") ->
                "900p"

            lower.contains("720p") ->
                "720p"

            lower.contains("540p") ->
                "540p"

            lower.contains("480p") ->
                "480p"

            lower.contains("360p") ->
                "360p"

            lower.contains("240p") ->
                "240p"

            lower.contains("144p") ->
                "144p"

            lower.contains("hevc") ||
                lower.contains("h265") ->
                "HEVC"

            lower.contains("av1") ->
                "AV1"

            lower.contains("hdr") ->
                "HDR"

            lower.contains("aac") ->
                "AAC"

            lower.contains("ac3") ->
                "AC3"

            lower.contains("opus") ->
                "OPUS"

            lower.contains("/audio/") ->
                "AUDIO"

            lower.contains("chunklist") ->
                "ADAPTIVE"

            else ->
                "AUTO"
        }

// =====================================
// SAVE YOUTUBE WATCH AS EXPORTABLE STREAM
// =====================================

try {

    if (
        youtubeWatchUrl.isNotBlank() &&
        !detectedStreams.contains(youtubeWatchUrl)
    ) {

        detectedStreams.add(
            youtubeWatchUrl
        )

        detectedVideos.add(
            youtubeWatchUrl
        )

        streamScores[youtubeWatchUrl] =
            9999

        markStreamSource(
            youtubeWatchUrl,
            "YOUTUBE_WATCH"
        )

        streamValidation[youtubeWatchUrl] =
            "YOUTUBE WATCH"

        Log.e(
            "YOUTUBE_WATCH_SAVED",
            youtubeWatchUrl
        )
    }

} catch (_: Throwable) {}

// =====================================
// SAVE YOUTUBE DASH PAIRS
// =====================================

if (isYoutubeDashVideoOnly) {

    youtubeDashVideoUrl =
        cleanedUrl

    youtubeDashVideoItag =
        when {
            lower.contains("itag=137") -> "137"
            lower.contains("itag=136") -> "136"
            lower.contains("itag=135") -> "135"
            lower.contains("itag=134") -> "134"
            lower.contains("itag=133") -> "133"
            lower.contains("itag=160") -> "160"
            else -> ""
        }

    Log.e(
        "YOUTUBE_DASH_VIDEO",
        "$youtubeDashVideoItag -> $youtubeDashVideoUrl"
    )
}

if (isYoutubeDashAudioOnly) {

    youtubeDashAudioUrl =
        cleanedUrl

    youtubeDashAudioItag =
        when {
            lower.contains("itag=251") -> "251"
            lower.contains("itag=141") -> "141"
            lower.contains("itag=140") -> "140"
            else -> ""
        }

    Log.e(
        "YOUTUBE_DASH_AUDIO",
        "$youtubeDashAudioItag -> $youtubeDashAudioUrl"
    )
}

// =====================================
// BADGE
// =====================================

val streamBadge =
    when {

        isEuronewsLiveApi ->
            "🟡 EURONEWS LIVE API"

        isEuronewsGeoApi ->
            "🟡 EURONEWS GEO CHECK"

        isEuronewsYoutubeEmbed ->
            "🟡 EURONEWS YOUTUBE EMBED"

        isEuronewsLivePage ->
            "🟡 EURONEWS LIVE PAGE"

        lower.contains("googlevideo.com") &&
            lower.contains("source=yt_live_broadcast") &&
            lower.contains("live=1") &&
            lower.contains("mime=video") ->
            "🔴 YOUTUBE LIVE VIDEO"

        lower.contains("googlevideo.com") &&
            lower.contains("source=yt_live_broadcast") &&
            lower.contains("live=1") &&
            lower.contains("mime=audio") ->
            "🔴 YOUTUBE LIVE AUDIO"

        lower.contains("googlevideo.com") &&
            lower.contains("source=yt_live_broadcast") &&
            lower.contains("live=1") &&
            !lower.contains("mime=video") &&
            !lower.contains("mime=audio") ->
            "🔴 YOUTUBE LIVE ENDPOINT"

        isYoutubeDashVideoOnly ->
            "🟠 YOUTUBE DASH VIDEO ONLY"

        isYoutubeDashAudioOnly ->
            "🟠 YOUTUBE DASH AUDIO ONLY"

        lower.contains("youtube.com/watch") ||
            lower.contains("youtu.be/") ->
            "🔴 YOUTUBE WATCH"

        lower.contains("jwpsrv.com") ||
            lower.contains("jwplayer") ||
            (
                lower.contains("/media/") &&
                    lower.contains("/versions/")
            ) ->
            "🎬 JWPLAYER VOD"

        lower.contains(".m3u8") &&
            (
                lower.contains("token") ||
                    lower.contains("auth") ||
                    lower.contains("signature") ||
                    lower.contains("sig=") ||
                    lower.contains("expires") ||
                    lower.contains("expire=") ||
                    lower.contains("key=") ||
                    lower.contains("x-plex-token") ||
                    lower.contains("session")
            ) ->
            "📺 TOKENIZED HLS"

        lower.contains(".m3u8") ->
            "📺 HLS"

        lower.contains(".mpd") ->
            "📡 DASH"

        isVideo ->
            "🎬 VIDEO"

        isImage ->
            "🖼 IMAGE"

        isAudio ->
            "🎵 AUDIO"

        else ->
            "📦 MEDIA"
    }

val securityBadge =
    when {

        lower.contains("token") ||
            lower.contains("signature") ||
            lower.contains("sig=") ||
            lower.contains("expires") ||
            lower.contains("expire=") ||
            lower.contains("policy") ||
            lower.contains("hdnts") ->
            " 🔒 SIGNED"

        else ->
            ""
    }

val segmentBadge =
    when {

        isSegmentTs ->
            " 🧩 SEGMENT"

        lower.contains(".m4s") ->
            " 🧩 SEGMENT"

        lower.contains("chunklist") ->
            " 🧩 CHUNKLIST"

        lower.contains("index-v") ||
            lower.contains("index-a") ->
            " 🧩 VARIANT"

        else ->
            ""
    }

val cdnType =
    when {

        lower.contains("googlevideo.com") ->
            "YouTube CDN"

        lower.contains("jwpsrv.com") ->
            "JWPlayer CDN"

        lower.contains("cloudfront") ->
            "CloudFront"

        lower.contains("akamai") ->
            "Akamai"

        lower.contains("cloudflare") ->
            "Cloudflare"

        lower.contains("bunny") ->
            "Bunny"

        lower.contains("broadpeak") ->
            "Broadpeak"

        lower.contains("fastly") ->
            "Fastly"

        else ->
            "Generic CDN"
    }

// =====================================
// EXTRA PLAYABLE LINKS
// Show only clean/exportable playable links.
// During Auto Scan, hide BEST STREAM to avoid
// previous-channel best stream leaking into logs.
// =====================================

val extraPlayableLinks =
    buildString {

        val cleanYoutubeWatchUrl =
            cleanDetectedUrl(
                youtubeWatchUrl
            )

        if (
            cleanYoutubeWatchUrl.isNotBlank() &&
            isExportableStream(
                cleanYoutubeWatchUrl
            )
        ) {

            append("▶️ YOUTUBE WATCH PLAYABLE")
            append("\n")
            append(cleanYoutubeWatchUrl)
            append("\n\n")
        }

        if (!autoScanRunning) {

            val cleanBestStreamUrl =
                cleanDetectedUrl(
                    getCleanBestStreamUrl()
                )

            if (
                cleanBestStreamUrl.isNotBlank() &&
                isExportableStream(
                    cleanBestStreamUrl
                )
            ) {

                append("⭐ BEST STREAM")
                append("\n")
                append(cleanBestStreamUrl)
                append("\n\n")
            }
        }
    }

// =====================================
// FINAL CLEAN LOG URL
// Only playable / exportable streams are allowed
// in logs, snapshots and last selected URL.
// =====================================

val finalLogUrl =
    cleanDetectedUrl(
        cleanedUrl
    )

if (
    finalLogUrl.isBlank() ||
    !isExportableStream(
        finalLogUrl
    )
) {
    return
}

// =====================================
// YOUTUBE LIVE EXPORT NOTE
// =====================================

val cleanYoutubeWatchForNote =
    cleanDetectedUrl(
        youtubeWatchUrl
    )

val youtubeLiveExportNote =
    if (
        isYoutubeLiveDash &&
        cleanYoutubeWatchForNote.isNotBlank() &&
        isExportableStream(
            cleanYoutubeWatchForNote
        ) &&
        !extraPlayableLinks.contains(
            cleanYoutubeWatchForNote
        )
    ) {

        "\n▶️ PLAYABLE WATCH URL\n$cleanYoutubeWatchForNote"

    } else {

        ""
    }

// =====================================
// SAVE LAST URL
// =====================================

lastSelectedUrl =
    finalLogUrl

// =====================================
// FORENSIC NOTE
// =====================================

val hasYoutubeDashPair =
    youtubeDashVideoUrl.isNotBlank() &&
        youtubeDashAudioUrl.isNotBlank()

val dashPairNote =
    if (hasYoutubeDashPair) {

        "\n✅ DASH PAIR READY — Video itag: $youtubeDashVideoItag / Audio itag: $youtubeDashAudioItag"

    } else {

        ""
    }

val forensicNote =
    when {

        isYoutubeLiveDash &&
            !isYoutubeHlsManifest ->
            "\nℹ️ YouTube live detected — direct HLS .m3u8 not exposed. Use Watch URL / DASH evidence."

        isYoutubeDashVideoOnly ->
            "\n⚠️ VIDEO ONLY — YouTube DASH fragment, no audio$dashPairNote"

        isYoutubeDashAudioOnly ->
            "\n⚠️ AUDIO ONLY — YouTube DASH fragment, no video$dashPairNote"

        isEuronewsYoutubeEmbed ->
            "\nℹ️ Euronews uses YouTube embedded live player$dashPairNote"

        isEuronewsLiveApi ->
            "\nℹ️ Euronews live API detected — metadata/source endpoint$dashPairNote"

        isEuronewsGeoApi ->
            "\nℹ️ Euronews geoblocking check endpoint$dashPairNote"

        else ->
            dashPairNote
    }

// =====================================
// SAVE STREAM SNAPSHOT
// =====================================

try {

    val cleanBest =
        cleanDetectedUrl(
            getCleanBestStreamUrl()
        )

    val cleanBestLive =
        cleanDetectedUrl(
            bestLiveUrl
        )

    val snapshot =
        StreamInfoSnapshot(
            url = finalLogUrl,
            badge = streamBadge,
            quality = streamQuality,
            cdn = cdnType,
            security = securityBadge,
            segment = segmentBadge,
            forensic = forensicNote,
            youtubeWatch = cleanYoutubeWatchForNote,
            dashVideo = cleanDetectedUrl(
                youtubeDashVideoUrl
            ),
            dashAudio = cleanDetectedUrl(
                youtubeDashAudioUrl
            ),
            dashVideoItag = youtubeDashVideoItag,
            dashAudioItag = youtubeDashAudioItag,
            bestStream = if (
                cleanBest.isNotBlank() &&
                isExportableStream(
                    cleanBest
                )
            ) {
                cleanBest
            } else {
                ""
            },
            bestLive = if (
                cleanBestLive.isNotBlank() &&
                isExportableStream(
                    cleanBestLive
                )
            ) {
                cleanBestLive
            } else {
                ""
            }
        )

    val cleanSavedUrl =
        cleanDetectedUrl(
            savedUrl
        )

    if (cleanSavedUrl.isNotBlank()) {

        streamInfoSnapshots[cleanSavedUrl] =
            snapshot
    }

    streamInfoSnapshots[finalLogUrl] =
        snapshot

} catch (_: Throwable) {}

// =====================================
// UI OUTPUT
// =====================================

runOnUiThread {

    binding.contentMain.result.append(
        """

$extraPlayableLinks$streamBadge [$streamQuality] [$cdnType]$securityBadge$segmentBadge$forensicNote$youtubeLiveExportNote

$displayUrl

$finalLogUrl

────────────────────

""".trimIndent()
    )
}

} // END detectAndSaveUrl()

// =====================================
// BUILD MEDIA LABEL
// =====================================

private fun buildMediaLabel(
    url: String
): String {

val lower =  
    url.lowercase()  

val streamQuality =  
    when {  

        lower.contains("2160") ||  
        lower.contains("4k") ->  
            "4K"  

        lower.contains("1440") ->  
            "1440p"  

        lower.contains("1080") ->  
            "1080p"  

        lower.contains("900") ->  
            "900p"  

        lower.contains("720") ->  
            "720p"  

        lower.contains("540") ->  
            "540p"  

        lower.contains("480") ->  
            "480p"  

        lower.contains("360") ->  
            "360p"  

        lower.contains("240") ->  
            "240p"  

        lower.contains("144") ->  
            "144p"  

        lower.contains("hevc") ||  
        lower.contains("h265") ->  
            "HEVC"  

        lower.contains("av1") ->  
            "AV1"  

        lower.contains("/audio/") ->  
            "AUDIO"  

        else ->  
            "AUTO"  
    }  

val cdnType =  
    when {  

        lower.contains("cloudfront") ->  
            "CloudFront"  

        lower.contains("akamai") ->  
            "Akamai"  

        lower.contains("cloudflare") ->  
            "Cloudflare"  

        lower.contains("bunny") ->  
            "Bunny"  

        lower.contains("broadpeak") ->  
            "Broadpeak"  

        else ->  
            "Generic CDN"  
    }  

val badge =
    when {

        lower.contains(".m3u8") ->
            "📺 HLS"

        lower.contains(".mpd") ->
            "📡 DASH"

        lower.contains("/vod/") ->
            "📼 VOD"

        lower.contains("original.mp4") ->
            "📼 STATIC VIDEO"

        lower.contains(".jpg") ||
        lower.contains(".jpeg") ||
        lower.contains(".png") ||
        lower.contains(".webp") ||
        lower.contains(".gif") ->
            "🖼 IMAGE"

        lower.contains(".mp3") ||
        lower.contains(".aac") ||
        lower.contains(".m4a") ->
            "🎵 AUDIO"

        else ->
            "🎬 VIDEO"
    }

// =====================================
// VALIDATION DISPLAY
// Hide ERROR for playable URLs so user can test them
// =====================================

val rawValidation =
    streamValidation[url]
        ?: ""

val isPlayableForUserTest =
    lower.contains(".m3u8") ||
        lower.contains(".mpd") ||
        lower.contains(".mp4") ||
        lower.contains(".webm") ||
        lower.contains(".mkv") ||
        lower.contains(".mov") ||
        lower.contains(".avi") ||
        lower.contains(".3gp") ||
        lower.contains(".ts") ||
        lower.contains(".mp3") ||
        lower.contains(".m4a") ||
        lower.contains(".aac") ||
        lower.contains(".opus") ||
        lower.contains(".wav") ||
        lower.contains(".ogg") ||
        lower.contains(".flac") ||
        lower.contains("manifest/hls") ||
        lower.contains("hls_playlist") ||
        lower.contains("hlsmanifesturl") ||
        lower.contains("youtube.com/watch") ||
        lower.contains("youtu.be/") ||
        lower.contains("googlevideo.com") ||
        lower.contains("videoplayback")

val validation =
    when {

        rawValidation.contains("ERROR") &&
            isPlayableForUserTest ->
            "TRY IN PLAYER"

        else ->
            rawValidation
    }

// =====================================
// FORENSIC SOURCE
// =====================================

val sourceTag =
    streamSources[url]
        ?.let { " [$it]" }
        ?: ""

// =====================================
// BLOB RELATION
// =====================================

val blobTag =

    blobRelations[url]
        ?.let {

            val shortName =

                it.substringAfterLast("/")

            " [↳ $shortName]"
        }

        ?: ""
        
// =====================================
// MANIFEST PARENT
// =====================================

val parentTag =

    manifestRelations[url]
        ?.let {

            val shortName =

                it.substringAfterLast("/")

            " [P:$shortName]"
        }

        ?: ""

// =====================================
// TOKEN TAG
// =====================================

val tokenTag =

    streamTokens[url]
        ?.takeIf { it.isNotEmpty() }
        ?.let {

            val short =
                it.take(2)
                    .joinToString(",")

            " [🔑 $short]"
        }

        ?: ""

return "$badge [$streamQuality] [$cdnType]$sourceTag$blobTag$parentTag$tokenTag $validation"

}

// =====================================
// BUILD IPTV CHANNEL NAME
// =====================================

private fun buildChannelName(
    url: String
): String {

    val lower =
        url.lowercase()

    val quality =
        when {

            lower.contains("2160") ||
            lower.contains("4k") ->
                " 4K"

            lower.contains("1440") ->
                " 1440p"

            lower.contains("1080") ->
                " 1080p"

            lower.contains("720") ->
                " 720p"

            lower.contains("540") ->
                " 540p"

            lower.contains("480") ->
                " 480p"

            lower.contains("360") ->
                " 360p"

            else ->
                ""
        }

    // =====================================
    // CHANNEL NAME
    // =====================================

    val channel =
        when {

            // =====================================
            // PANELLADIKA
            // =====================================

            lower.contains("action24") ||
            lower.contains("actiontv") ->
                "ACTION 24"

            lower.contains("mega") &&
            lower.contains("news") ->
                "MEGA NEWS"

            lower.contains("mega") ->
                "MEGA"

            lower.contains("comedy") &&
            lower.contains("antennaplus") ->
                "ANT1 COMEDY"

            lower.contains("drama") &&
            lower.contains("antennaplus") ->
                "ANT1 DRAMA"

            lower.contains("ant1") ||
            lower.contains("antenna") ->
                "ANT1"

            lower.contains("skai") ->
                "SKAI"

            lower.contains("alpha") ->
                "ALPHA"

            lower.contains("star") &&
            lower.contains("international") ->
                "STAR INTERNATIONAL"

            lower.contains("/star/") ||
            lower.contains("star-channel") ||
            lower.contains("startv") ->
                "STAR"

            lower.contains("open") ->
                "OPEN"

            lower.contains("ertnews") ->
                "ERT NEWS"

            lower.contains("ertworld") ->
                "ERT WORLD"

            lower.contains("ertkids") ->
                "ERT KIDS"

            lower.contains("ert1") ->
                "ERT1"

            lower.contains("ert2") ->
                "ERT2"

            lower.contains("ert3") ->
                "ERT3"

            lower.contains("vouli") ->
                "VOULI TV"

            lower.contains("kontra") ->
                "KONTRA"

            lower.contains("mak") ||
            lower.contains("makedonia") ->
                "MAKEDONIA TV"

            lower.contains("onechannel") ||
            lower.contains("one/stream") ->
                "ONE CHANNEL"

            lower.contains("bluesky") ->
                "BLUE SKY"

            // =====================================
            // PERIFEREIAKA
            // =====================================

            lower.contains("faros1") ->
                "FAROS TV 1"

            lower.contains("faros2") ->
                "FAROS TV 2"

            lower.contains("acheloostv") ||
            lower.contains("acheloos") ->
                "ACHELOOS TV"

            lower.contains("mesogeiostv") ->
                "MESOGEIOS TV"

            lower.contains("messiniatv") ->
                "MESSINIA TV"

            lower.contains("hlektratv") ->
                "HLEKTRA TV"

            lower.contains("paniktv") ->
                "PANIK TV"

            lower.contains("nstv") ->
                "NSTV"

            lower.contains("hellenictv") ->
                "HELLENIC TV"

            lower.contains("eurotv") ->
                "EURO TV"

            lower.contains("madworld") ->
                "MAD WORLD"

            lower.contains("ena_channel") ||
            lower.contains("/ena/") ->
                "ENA CHANNEL"

            lower.contains("4e") ->
                "4E"

            lower.contains("aeolos") ->
                "AEOLOS TV"

            lower.contains("aigaiotv") ->
                "AIGAIO TV"

            lower.contains("arttv") ->
                "ART TV"

            lower.contains("astratv") ->
                "ASTRA TV"

            lower.contains("atticatv") ->
                "ATTICA TV"

            lower.contains("besttv") ->
                "BEST TV"

            lower.contains("centertv") ->
                "CENTER TV"

            lower.contains("corfu") ->
                "CORFU TV"

            lower.contains("deltatv") ->
                "DELTA TV"

            lower.contains("diontv") ->
                "DION TV"

            lower.contains("egnatiatv") ->
                "EGNATIA TV"

            lower.contains("epirus") ->
                "EPIRUS TV"

            lower.contains("epsilon") ->
                "EPSILON TV"

            lower.contains("extratv") ->
                "EXTRA TV"

            lower.contains("formedia") ->
                "FORMEDIA TV"

            lower.contains("hightv") ->
                "HIGH TV"

            lower.contains("ionian") ->
                "IONIAN TV"

            lower.contains("kriti1") ->
                "KRHTH 1"

            lower.contains("cretetv") ->
                "KRHTH TV"

            lower.contains("lepanto") ->
                "LEPANTO"

            lower.contains("lychnos") ->
                "LYCHNOS TV"

            lower.contains("naft") ->
                "NAFTEMPORIKI TV"

            lower.contains("neatv") ->
                "NEA TV"

            lower.contains("ort") ->
                "ORT TV"

            lower.contains("pellatv") ->
                "PELLA TV"

            lower.contains("plp") ->
                "PLP"

            lower.contains("samiaki") ->
                "SAMIAKI TV"

            lower.contains("starbe") ->
                "STAR B.E."

            lower.contains("starke") ->
                "STAR K.E."

            lower.contains("starttv") ->
                "START TV"

            lower.contains("stent") ->
                "STENT TV"

            lower.contains("syrostv1") ->
                "SYROS TV1"

            lower.contains("telekriti") ->
                "TELEKRITI"

            lower.contains("thraki") ->
                "THRAKI NET"

            lower.contains("topchannel") ->
                "TOP CHANNEL"

            lower.contains("trt") ->
                "TRT"

            lower.contains("tv100") ->
                "TV100"

            lower.contains("tvcreta") ->
                "TV CRETA"

            lower.contains("vergina") ->
                "VERGINA TV"

            lower.contains("zougla") ->
                "ZOUGLA TV"

            // =====================================
            // MUSIC / RADIO TV
            // =====================================

            lower.contains("kissfm") ->
                "KISS FM TV"

            lower.contains("tilemousiki") ->
                "THLEMOUSIKH"

            lower.contains("ellinikosfm") ->
                "ELLINIKOS FM"

            // =====================================
            // CYPRUS
            // =====================================

            lower.contains("alphacy") ->
                "ALPHA CYPRUS"

            lower.contains("ant1cy") ->
                "ANT1 CYPRUS"

            lower.contains("citychannel") ->
                "CITY CHANNEL"

            lower.contains("omega") ->
                "OMEGA CYPRUS"

            lower.contains("riksat") ->
                "RIK SAT"

            lower.contains("sigma") ->
                "SIGMA CYPRUS"

            // =====================================
            // CINEMA
            // =====================================

            lower.contains("extacy") ->
                "EXTACY TV"

            lower.contains("cinemaclassics") ->
                "CINEMA CLASSICS"

            lower.contains("groovy") ->
                "GROOVY CINEMA"

            lower.contains("lampsi") ->
                "LAMPSI TV"

            lower.contains("netmax") ->
                "NETMAX TV"

            lower.contains("village-world") ->
                "VILLAGE WORLD"

            // =====================================
            // FALLBACKS
            // =====================================

            lower.contains(".m3u8") ->
                "HLS"

            lower.contains(".mpd") ->
                "DASH"

            lower.contains(".mp4") ->
                "VIDEO"

            else ->
                "STREAM"
        }

    return "$channel$quality"
        .trim()
}

// =====================================
// BUILD STREAM TYPE
// =====================================

private fun buildStreamType(
    url: String
): String {

    val lower =
        url.lowercase()

    return when {

        lower.contains("drm") ||
        lower.contains("widevine") ||
        lower.contains("license") ->
            "DRM"

        lower.contains("/vod/") ||
        lower.contains("original.mp4") ->
            "VOD"

        lower.contains(".m3u8") ||
        lower.contains(".mpd") ||
        lower.contains("live") ->
            "LIVE"

        else ->
            "MEDIA"
    }
}

// =====================================
// REAL YOUTUBE WATCH URL CHECK
// =====================================

private fun isRealYouTubeWatchUrl(
    url: String
): Boolean {

    return try {

        val lower =
            url.lowercase()

        if (
            !lower.contains("youtube.com/watch") &&
            !lower.contains("youtu.be/")
        ) {
            return false
        }

        val videoId =
            if (lower.contains("youtube.com/watch")) {

                Uri.parse(url)
                    .getQueryParameter("v")
                    .orEmpty()

            } else {

                Uri.parse(url)
                    .path
                    ?.trimStart('/')
                    ?.substringBefore("/")
                    ?.substringBefore("?")
                    ?.substringBefore("&")
                    ?.trim()
                    .orEmpty()
            }

        videoId.matches(
            Regex("^[A-Za-z0-9_-]{11}$")
        )

    } catch (_: Throwable) {

        false
    }
}

// =====================================
// IS EXPORTABLE STREAM
// Clean export rules:
// - playlist always
// - .ts only fallback if no playlist found
// - no images / ads / fragments
// =====================================

private fun isExportableStream(
    url: String
): Boolean {

    val cleanUrl =
        cleanDetectedUrl(
            url
        )

    val lower =
        cleanUrl.lowercase()

    if (cleanUrl.isBlank()) {
        return false
    }

    if (
        !cleanUrl.startsWith("http://", true) &&
        !cleanUrl.startsWith("https://", true)
    ) {
        return false
    }

    // =====================================
    // HARD BAD / TEMP / AD / ERROR VARIANTS
    // =====================================

    if (
        lower.contains("?error=") ||
        lower.contains("&error=") ||
        lower.contains("error=1108") ||
        lower.contains("reader_gdpr") ||
        lower.contains("gdpr_binary_consent") ||
        lower.contains("gdpr_comes_from_infopack") ||
        lower.contains("reader_us_privacy") ||
        lower.contains("vmap") ||
        lower.contains("monetization") ||
        lower.contains("cookie_sync") ||
        lower.contains("cookie_sync_ab") ||
        lower.contains("ciid=") ||
        lower.contains("cidx=") ||
        lower.contains("sidx=") ||
        lower.contains("vididx=") ||
        lower.contains("imal=") ||
        lower.contains("3pcb=") ||
        lower.contains("rap=") ||
        lower.contains("apo=") ||
        lower.contains("pdm=") ||
        lower.contains("pbm=") ||
        lower.contains("reader_") ||
        lower.contains("gdpr_")
    ) {
        return false
    }

    // =====================================
    // FAKE YOUTUBE WATCH URL FILTER
    // =====================================

    if (
        (
            lower.contains("youtube.com/watch") ||
                lower.contains("youtu.be/")
            ) &&
        !isRealYouTubeWatchUrl(cleanUrl)
    ) {
        return false
    }

    // =====================================
    // GOOGLEVIDEO / TRACKING NOISE
    // =====================================

    if (
        lower.contains("generate_204") ||
        lower.contains("/ptracking") ||
        lower.contains("/api/stats") ||
        lower.contains("playback/stats")
    ) {
        return false
    }

    // =====================================
    // HARD NOISE
    // =====================================

    if (isHardNoiseUrl(cleanUrl)) {
        return false
    }

    // =====================================
    // VALIDATION
    // =====================================

    val validation =
        streamValidation[cleanUrl]
            ?: streamValidation[url]
            ?: ""

    if (
        validation.contains("DEAD", true)
    ) {
        return false
    }

    // =====================================
    // PLAYLISTS — BEST RESULT
    // =====================================

    if (
        isPlaylistUrl(
            cleanUrl
        )
    ) {
        return true
    }

    // =====================================
    // TS FALLBACK ONLY
    // .ts allowed only when no playlist exists
    // =====================================

    if (
        isTsFallbackUrl(
            cleanUrl
        )
    ) {

        return !pagePlaylistFound
    }

    // =====================================
    // STATIC VIDEO
    // =====================================

    if (
        lower.contains(".mp4") ||
            lower.contains(".webm") ||
            lower.contains(".mkv") ||
            lower.contains(".mov") ||
            lower.contains(".avi") ||
            lower.contains(".3gp")
    ) {
        return true
    }

    // =====================================
    // AUDIO
    // =====================================

    if (
        lower.contains(".mp3") ||
            lower.contains(".m4a") ||
            lower.contains(".aac") ||
            lower.contains(".opus") ||
            lower.contains(".wav") ||
            lower.contains(".ogg") ||
            lower.contains(".flac")
    ) {
        return true
    }

    // =====================================
    // YOUTUBE / GOOGLEVIDEO PLAYABLE
    // =====================================

    if (
        lower.contains("youtube.com/watch") ||
            lower.contains("youtu.be/") ||
            (
                lower.contains("googlevideo.com") &&
                    lower.contains("videoplayback")
                )
    ) {
        return true
    }

    return false
}

// =====================================
// STREAM QUALITY / STABILITY HELPERS
// =====================================

private fun isBestQualityTrackStream(
    url: String
): Boolean {

    val lower =
        cleanDetectedUrl(
            url
        ).lowercase()

    return lower.contains(
        "/tracks-v1a1/mono.m3u8"
    )
}

private fun isStablePlaylistStream(
    url: String
): Boolean {

    val lower =
        cleanDetectedUrl(
            url
        ).lowercase()

    if (
        !lower.contains(".m3u8")
    ) {
        return false
    }

    if (
        lower.contains("chunklist") ||
        lower.contains("/chunks.m3u8") ||
        lower.contains("/chunk.m3u8") ||
        lower.contains("/tracks-") ||
        lower.contains("mono.m3u8")
    ) {
        return false
    }

    return lower.endsWith("/playlist.m3u8") ||
        lower.endsWith("playlist.m3u8") ||
        lower.endsWith("/index.m3u8") ||
        lower.endsWith("index.m3u8")
}

private fun isLowerPriorityHlsVariant(
    url: String
): Boolean {

    val lower =
        cleanDetectedUrl(
            url
        ).lowercase()

    return lower.contains("chunklist") ||
        lower.contains("/chunks.m3u8") ||
        lower.contains("/chunk.m3u8")
}

private fun streamRankScore(
    url: String
): Int {

    val clean =
        cleanDetectedUrl(
            url
        )

    val lower =
        clean.lowercase()

    return when {

        // Best quality: combined video+audio track
        isBestQualityTrackStream(
            clean
        ) ->
            1000

        // Most stable fallback / master
        isStablePlaylistStream(
            clean
        ) ->
            900

        // Clean manifest from known HLS provider
        lower.contains("/manifest/video/") &&
            lower.contains(".m3u8") ->
            800

        // Other clean m3u8
        lower.contains(".m3u8") &&
            !isLowerPriorityHlsVariant(
                clean
            ) ->
            700

        // Chunklist / chunks fallback
        isLowerPriorityHlsVariant(
            clean
        ) ->
            500

        // Static video fallback
        lower.contains(".mp4") ||
            lower.contains(".webm") ->
            300

        else ->
            0
    }
}

private fun pickBestQualityStream(
    streams: List<String>
): String {

    return streams
        .map { item ->
            cleanDetectedUrl(
                item
            )
        }
        .filter { item ->
            item.isNotBlank() &&
                isExportableStream(
                    item
                )
        }
        .filter { item ->
            isBestQualityTrackStream(
                item
            )
        }
        .distinctBy { item ->
            item.lowercase()
        }
        .maxByOrNull { item ->
            streamRankScore(
                item
            )
        }
        .orEmpty()
}

private fun pickBestStableStream(
    streams: List<String>
): String {

    return streams
        .map { item ->
            cleanDetectedUrl(
                item
            )
        }
        .filter { item ->
            item.isNotBlank() &&
                isExportableStream(
                    item
                )
        }
        .filter { item ->
            isStablePlaylistStream(
                item
            )
        }
        .distinctBy { item ->
            item.lowercase()
        }
        .maxByOrNull { item ->
            streamRankScore(
                item
            )
        }
        .orEmpty()
}

// =====================================
// BUILD TVG LOGO
// =====================================

private fun buildLogoUrl(
    url: String
): String {

    val lower =
        url.lowercase()

    return when {

        // =====================================
        // PANELLADIKA
        // =====================================

        lower.contains("action24") ->
            "https://tr.static.cdns.cosmotetvott.gr/ote-prod/channel_logos/action24_new1-wide.png"

        lower.contains("alpha") ->
            "https://i.imgur.com/XlGD57K.png"

        lower.contains("ant1") ->
            "https://i.imgur.com/cr7ZEfC.png"

        lower.contains("bluesky") ||
        lower.contains("blue sky") ->
            "https://i.imgur.com/Z3OJ3Ew.png"

        lower.contains("ertnews") ->
            "https://files2.app.ertflix.gr/files/channels/ertnews-logo-800x300.png"

        lower.contains("ertworld") ->
            "https://tvonline.bg/wp-content/uploads/ERT-WORLD-tv.png"

        lower.contains("ert1") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/ERT1_logo_2020.svg/960px-ERT1_logo_2020.svg.png"

        lower.contains("ert2") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/ERT2_logo_2020.svg/1280px-ERT2_logo_2020.svg.png"

        lower.contains("ert3") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/ERT3_logo_2020.svg/1280px-ERT3_logo_2020.svg.png"

        lower.contains("kontra") ->
            "https://i.imgur.com/1xfxTiK.png"

        lower.contains("makedonia") ||
        lower.contains("maktv") ||
        lower.contains("/mak/") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/Makedonia_TV_2023.svg/1280px-Makedonia_TV_2023.svg.png"

        lower.contains("mega") &&
        lower.contains("news") ->
            "https://www.megatv.com/wp-content/themes/whsk_newmegatv_2025/assets/images/mega-news-hover.png"

        lower.contains("mega") ->
            "https://www.megatv.com/wp-content/themes/megatv/common/imgs/megawhite.png"

        lower.contains("onechannel") ||
        lower.contains("one/stream") ->
            "https://upload.wikimedia.org/wikipedia/commons/3/37/One_Channel_Logo_2019.png"

        lower.contains("open") ->
            "https://i.imgur.com/VtFNBrF.png"

        lower.contains("skai") ->
            "https://upload.wikimedia.org/wikipedia/el/e/eb/SkaiTV-Logo.png"

        lower.contains("/star/") ||
        lower.contains("star-channel") ||
        lower.contains("startv") ->
            "https://www.star.gr/tv/Content/Media/logo.png"

        lower.contains("vouli") ||
        lower.contains("parltv") ->
            "https://i.imgur.com/0mkw6VW.png"

        // =====================================
        // PERIFEREIAKA
        // =====================================

        lower.contains("4e") ->
            "https://upload.wikimedia.org/wikipedia/en/0/09/4E_TV_logo.png"

        lower.contains("aeolos") ->
            "https://i.imgur.com/7NK1C1v.png"

        lower.contains("aigaiotv") ->
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjcckzWBcMRMiHKQ-PePr2lbw2ifdmmcEpSgw2qYNkRumDZ_kNpsyXBGo8XbfUZPp2Kf3Wp6Xqpvh1BDSXk4FkaXYOifqtFr6iXm7z80kn3THaNegScafYOIGSYhvNb2xdJjHhM2qJA-8Q/s1600/AIGAIO.png"

        lower.contains("alfadramas") ||
        lower.contains("/alf/") ->
            "https://static.wixstatic.com/media/438597_c5e2019c2de745fd9229ccf0b116744a~mv2.png/v1/fill/w_98,h_78,al_c,q_85,usm_0.66_1.00_0.01/index2.webp"

        lower.contains("arttv") ->
            "https://arttv.gr/wp-content/uploads/2024/02/cropped-arttv-logo-site-icon.png"

        lower.contains("astratv") ->
            "https://www.astratv.gr/wp-content/uploads/2019/03/mainlogo.png"

        lower.contains("atticatv") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/ATTICA.png"

        lower.contains("axelwos") ->
            "https://i.imgur.com/kWxWjFU.png"

        lower.contains("besttv") ->
            "https://best-tv.gr/wp-content/uploads/2021/02/best_lg_footer.png"

        lower.contains("centertv") ->
            "https://centertv.gr/wp-content/uploads/2019/09/center-logo.png"

        lower.contains("corfu") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/CORFU.png"

        lower.contains("deltatv") ->
            "https://i.imgur.com/rbA1WRV.png"

        lower.contains("diontv") ->
            "https://diontv.gr/wp-content/uploads/2021/01/cropped-dionlogo.png"

        lower.contains("egnatiatv") ->
            "https://i.imgur.com/zuyYIca.png"

        lower.contains("ekkl") ||
        lower.contains("ecclessia") ->
            "https://i.imgur.com/QQSSMZx.png"

        lower.contains("epirus") ->
            "https://www.epirustv1.eu/img/logo.png"

        lower.contains("epsilon") ->
            "https://upload.wikimedia.org/wikipedia/el/2/20/EPSILON_TV_logo.png"

        lower.contains("extratv") ||
        lower.contains("/extra/") ->
            "https://4.bp.blogspot.com/-g7gOZOF1a58/Vo_rwvxDrmI/AAAAAAAAKVg/ThXRpP7uzk8/s640/extralogo2.png"

        lower.contains("formedia") ->
            "https://i.imgur.com/8FcJ2I9.png"

        lower.contains("hightv") ->
            "https://i.imgur.com/6am6zJU.png"

        lower.contains("ionian") ->
            "https://i.imgur.com/vbBFiOG.png"

        lower.contains("irida") ->
            "https://i.imgur.com/gn5sdCt.png"

        lower.contains("kostv") ->
            "https://www.kostv.gr/templates/reach/public/img/logo.png"

        lower.contains("kriti1") ->
            "https://www.kriti1.gr/wp-content/uploads/2024/01/KRHTH1_-e1706176098815-300x110.png"

        lower.contains("cretetv") ||
        lower.contains("krhthtv") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/%CE%9A%CF%81%CE%AE%CF%84%CE%B7_TV_%28logo%29.svg/1280px-%CE%9A%CF%81%CE%AE%CF%84%CE%B7_TV_%28logo%29.svg.png"

        lower.contains("lepanto") ->
            "https://i.imgur.com/0jiwkyS.png"

        lower.contains("lychnos") ->
            "https://i.imgur.com/QDg6c02.png"

        lower.contains("naft") ->
            "https://i.imgur.com/n7sroRH.png"

        lower.contains("neatv") ->
            "https://i.imgur.com/JL75YPO.png"

        lower.contains("notos") ->
            "https://cdn.e-radio.gr/logos/gr/big/notosnews978.png"

        lower.contains("ort") ->
            "https://upload.wikimedia.org/wikipedia/en/6/64/ORT_logo.png"

        lower.contains("pellatv") ->
            "https://pellatv.gr/wp-content/themes/pellatv/images/pellatv.jpg"

        lower.contains("plp") ->
            "https://i.imgur.com/qa3Lurc.png"

        lower.contains("samiaki") ->
            "https://i.imgur.com/S6Onzc0.png"

        lower.contains("starbe") ->
            "https://www.startvfm.gr/wp-content/uploads/2017/10/star-voreiou-ellados.png"

        lower.contains("starke") ->
            "https://digitalstar.gr/wp-content/uploads/2022/06/202206280031514287.png"

        lower.contains("starttv") ->
            "https://i.imgur.com/toTw8lx.png"

        lower.contains("stent") ->
            "https://stent.net.gr/wp-content/themes/whsk_stent/common/imgs/stentv_logo.png"

        lower.contains("syrostv1") ->
            "https://syrostv1.gr/wp-content/uploads/2021/12/site_logo-1.png"

        lower.contains("telekriti") ->
            "https://telekriti.com/wp-content/uploads/2023/08/NEW-LOGO-7c-300x50.png"

        lower.contains("thraki") ->
            "https://i.imgur.com/zkTohBm.png"

        lower.contains("topchannel") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/TOP.png"

        lower.contains("trt") ->
            "https://i.imgur.com/nAzfWx5.png"

        lower.contains("tv100") ->
            "https://i.imgur.com/D7YqJig.png"

        lower.contains("tvcreta") ->
            "https://i.imgur.com/1uMcCR6.png"

        lower.contains("tvfilopoli") ->
            "https://i.imgur.com/GdgekUL.png"

        lower.contains("tvkosmos") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/KOSMOS_TV.png"

        lower.contains("tvrodopi") ->
            "https://rodopiflix.gr/wp-content/uploads/2024/08/tvrodopi-logo.png"

        lower.contains("vergina") ->
            "https://i.imgur.com/JzuEEDi.png"

        lower.contains("zougla") ->
            "https://i.imgur.com/dW08jNe.png"

        // =====================================
        // CYPRUS
        // =====================================

        lower.contains("alphacy") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/ALPHA_CYPRUS.png"

        lower.contains("ant1cy") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/ANT1_CYPRUS.png"

        lower.contains("citychannel") ->
            "https://i.imgur.com/Jo6jIP1.jpg"

        lower.contains("omega") ->
            "https://www.omegatv.com.cy/assets/img/logo.png"

        lower.contains("riksat") ||
        lower.contains("/rik/") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Logo_RIK_Sat_2017.svg/1280px-Logo_RIK_Sat_2017.svg.png"

        lower.contains("sigma") ->
            "https://www.sigmatv.com/application/themes/default/img/redesign/sigma-logo-final.png"

        // =====================================
        // CINEMA
        // =====================================

        lower.contains("extacy") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/EXTASY_TV.png"

        lower.contains("cinemaclassics") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/CINEMA_CLASSICS.png"

        lower.contains("groovy") ->
            "https://i.imgur.com/0iklNh2.png"

        lower.contains("lampsi") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/LAMSI.png"

        lower.contains("netmax") ->
            "https://i.imgur.com/dLvRD4k.png"

        lower.contains("village-world") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/VILAGE_WORLD.png"

        // =====================================
        // DEFAULT
        // =====================================

        else ->
            ""
    }
}

// =====================================
// SHOW ALL
// =====================================

private fun showAllMedia() {

    val sb =
        StringBuilder()

val sorted =
    detectedStreams
        .filter { url ->

            isExportableStream(
                url
            )
        }
        .sortedByDescending { url ->

            val validation =
                streamValidation[url]
                    ?: ""

            val baseScore =
                streamScores[url]
                    ?: 0

            val lower =
                url.lowercase()
                
            val isJwPlayerVod =
    lower.contains("jwpsrv.com") ||
        lower.contains("jwplayer") ||
        (
            lower.contains("/media/") &&
                lower.contains("/versions/")
        )

            when {

                // =====================================
                // VERIFIED LIVE
                // =====================================

                validation.contains("LIVE") ->
                    baseScore + 5000

                // =====================================
                // VERIFIED STREAM
                // =====================================

                validation.contains("VERIFIED") ||
                validation.contains("HLS") ||
                validation.contains("DASH") ->
                    baseScore + 3000

                // =====================================
                // TOKEN
                // =====================================

                validation.contains("TOKEN") ->
                    baseScore + 1500

                // =====================================
                // DEAD
                // =====================================

                validation.contains("DEAD") ->
                    -1000

                // =====================================
                // MASTER
                // =====================================

                !isJwPlayerVod &&
    lower.contains("master.m3u8") &&
    !lower.contains("/vod/") ->
    1000

                !isJwPlayerVod &&
    lower.contains(".m3u8") &&
    (
        lower.contains("/live/") ||
            lower.contains("livestream") ||
            lower.contains("broadcast") ||
            lower.contains("linear") ||
            lower.contains("live.m3u8")
    ) ->
    950

                lower.contains(".mpd") ->
                    900

                lower.contains("playlist") ->
                    850

                lower.contains("chunklist") ->
                    800

                lower.contains(".mp4") &&
                !lower.contains("/vod/") &&
                !lower.contains("original.mp4") ->
                    700

                lower.contains(".m4s") ->
    -2000

lower.contains(".ts") ->
    if (pagePlaylistFound) {
        -2000
    } else {
        300
    }

                lower.contains(".mp3") ||
                lower.contains(".aac") ->
                    250

                lower.contains(".jpg") ||
lower.contains(".jpeg") ||
lower.contains(".png") ||
lower.contains(".webp") ||
lower.contains(".gif") ||
lower.contains(".svg") ||
lower.contains(".ico") ->
    -3000

                lower.contains("/vod/") ->
                    20

                lower.contains("original.mp4") ->
                     300

                else ->
                    baseScore
            }
        }

    sorted.forEach { url ->

        sb.append(
            buildMediaLabel(url)
        )

        sb.append("\n\n")

        sb.append(url)

        sb.append(
            "\n\n────────────────────\n\n"
        )
    }

    binding.contentMain.result.text =
        sb.toString()
}

// =====================================
// SHOW VIDEOS
// =====================================

private fun showVideos() {

    val sb =
        StringBuilder()

    detectedVideos
    .filter { url ->

        isExportableStream(
            url
        )
    }
    .sortedByDescending {

        streamScores[it] ?: 0
    }
        .forEach { url ->

            sb.append(
                buildMediaLabel(url)
            )

            sb.append("\n\n")

            sb.append(url)

            sb.append(
                "\n\n────────────────────\n\n"
            )
        }

    binding.contentMain.result.text =
        sb.toString()
}

// =====================================
// SHOW IMAGES
// =====================================

private fun showImages() {

    val sb =
        StringBuilder()

    detectedImages
        .sortedByDescending {

            streamScores[it] ?: 0
        }
        .forEach { url ->

            sb.append(
                buildMediaLabel(url)
            )

            sb.append("\n\n")

            sb.append(url)

            sb.append(
                "\n\n────────────────────\n\n"
            )
        }

    binding.contentMain.result.text =
        sb.toString()
}

// =====================================
// SHOW AUDIO
// =====================================

private fun showAudio() {

    val sb =
        StringBuilder()

    detectedAudio
        .sortedByDescending {

            streamScores[it] ?: 0
        }
        .forEach { url ->

            sb.append(
                buildMediaLabel(url)
            )

            sb.append("\n\n")

            sb.append(url)

            sb.append(
                "\n\n────────────────────\n\n"
            )
        }

    binding.contentMain.result.text =
        sb.toString()
}

// =====================================
// SHOW CHANNELS
// =====================================

private fun showChannels() {

val sb =  
    StringBuilder()  

detectedChannels  
    .forEach { (name, url) ->  

        sb.append(  
            "📺 $name\n\n"  
        )  

        sb.append(url)  

        sb.append(  
            "\n────────────────────\n\n"  
        )  
    }  

binding.contentMain.result.text =  
    sb.toString()

}

// =====================================
// LIVE STREAM MONITOR
// =====================================

private fun startStreamMonitor() {

    binding.contentMain.webview.postDelayed(

        object : Runnable {

            override fun run() {

    try {

        // =====================================
        // PAUSE SCANNER WHILE USER TOUCHES PAGE
        // =====================================

        if (webUserInteracting) {

            binding.contentMain.webview.postDelayed(
                this,
                3000
            )

            return
        }
        
// =====================================
// PAUSE SCANNER DURING CLOUDFLARE VERIFY
// =====================================

if (cloudflareChallengeActive) {

    binding.contentMain.webview.postDelayed(
        this,
        4000
    )

    return
}

                    val js =
                        """

(function() {

let results = [];

function gelPush(url) {

    try {

        if (!url) {
            return;
        }

        url =
            String(url);

        results.push(url);

    } catch(e) {}
}

// =====================================
// VIDEO TAGS
// =====================================

try {

    document
        .querySelectorAll("video")
        .forEach(function(v) {

            try {

                if (v.currentSrc) {

                    gelPush(v.currentSrc);

                    console.log(
                        "GEL_VIDEO_CURRENT:",
                        v.currentSrc
                    );
                }

                if (v.src) {

                    gelPush(v.src);

                    console.log(
                        "GEL_VIDEO_SRC:",
                        v.src
                    );
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// SOURCE TAGS
// =====================================

try {

    document
        .querySelectorAll("source")
        .forEach(function(s) {

            try {

                if (s.src) {

                    gelPush(s.src);

                    console.log(
                        "GEL_SOURCE_SRC:",
                        s.src
                    );
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// FETCH HOOK
// =====================================

try {

    if (!window.__gelFetchHooked) {

        window.__gelFetchHooked = true;

        const originalFetch =
            window.fetch;

        window.fetch =
            function() {

                try {

                    let reqUrl =
                        arguments[0];

                    if (
                        typeof reqUrl !== "string" &&
                        reqUrl &&
                        reqUrl.url
                    ) {

                        reqUrl =
                            reqUrl.url;
                    }

                    if (reqUrl) {

                        const lower =
                            String(reqUrl).toLowerCase();

                        if (
                            lower.includes(".m3u8") ||
                            lower.includes(".mpd") ||
                            lower.includes(".mp4") ||
                            lower.includes(".m4s") ||
                            lower.includes(".ts") ||
                            lower.includes("playlist") ||
                            lower.includes("manifest") ||
                            lower.includes("chunklist") ||
                            lower.includes("live") ||
                            lower.includes("videoplayback")
                        ) {

                            gelPush(reqUrl);

                            console.log(
                                "GEL_FETCH_URL:",
                                reqUrl
                            );
                        }
                    }

                } catch(e) {}

                return originalFetch
                    .apply(this, arguments)
                    .then(function(response) {

                        try {

                            const clone =
                                response.clone();

                            clone.text()
                                .then(function(text) {

                                    try {

                                        const regex =
                                            /https?:\/\/[^"'\\s]+?(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s]*)?/gi;

                                        let match;

                                        while (
                                            (match = regex.exec(text)) !== null
                                        ) {

                                            gelPush(match[0]);

                                            console.log(
                                                "GEL_FETCH_RESPONSE:",
                                                match[0]
                                            );
                                        }

                                    } catch(e) {}
                                });

                        } catch(e) {}

                        return response;
                    });
            };
    }

} catch(e) {}

// =====================================
// XHR HOOK
// =====================================

try {

    if (!window.__gelXHRHooked) {

        window.__gelXHRHooked = true;

        const originalOpen =
            XMLHttpRequest.prototype.open;

        XMLHttpRequest.prototype.open =
            function(method, url) {

                try {

                    if (
                        typeof url === "string"
                    ) {

                        const lower =
                            url.toLowerCase();

                        if (
                            lower.includes(".m3u8") ||
                            lower.includes(".mpd") ||
                            lower.includes(".mp4") ||
                            lower.includes(".m4s") ||
                            lower.includes(".ts") ||
                            lower.includes("playlist") ||
                            lower.includes("manifest") ||
                            lower.includes("chunklist") ||
                            lower.includes("live") ||
                            lower.includes("videoplayback")
                        ) {

                            gelPush(url);

                            console.log(
                                "GEL_MEDIA_XHR:",
                                url
                            );
                        }

                        if (
                            lower.includes(".mpd") ||
                            lower.includes("dash") ||
                            lower.includes("manifest")
                        ) {

                            gelPush(url);

                            console.log(
                                "GEL_MPD_CANDIDATE:",
                                url
                            );
                        }
                    }

                } catch(e) {}

                return originalOpen.apply(
                    this,
                    arguments
                );
            };
    }

} catch(e) {}

// =====================================
// PERFORMANCE API
// =====================================

try {

    performance
        .getEntriesByType("resource")
        .forEach(function(r) {

            try {

                const u =
                    r.name || "";

                if (!u) {
                    return;
                }

                const lower =
                    u.toLowerCase();

                if (
                    lower.includes(".m3u8") ||
                    lower.includes(".mpd") ||
                    lower.includes(".mp4") ||
                    lower.includes(".m4s") ||
                    lower.includes(".ts") ||
                    lower.includes("playlist") ||
                    lower.includes("manifest") ||
                    lower.includes("chunklist") ||
                    lower.includes("live") ||
                    lower.includes("videoplayback")
                ) {

                    gelPush(u);

                    console.log(
                        "GEL_PERFORMANCE:",
                        u
                    );
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// PERFORMANCE DEEP MEDIA SCAN
// =====================================

try {

    const entries =
        performance.getEntriesByType(
            "resource"
        );

    entries.forEach(function(entry) {

        try {

            const url =
                entry.name || "";

            if (!url) {
                return;
            }

            const lower =
                url.toLowerCase();

            const initiator =
                entry.initiatorType || "";

            const looksLikeMedia =
                lower.indexOf(".m3u8") !== -1 ||
                lower.indexOf(".mpd") !== -1 ||
                lower.indexOf(".mp4") !== -1 ||
                lower.indexOf(".m4s") !== -1 ||
                lower.indexOf(".ts") !== -1 ||
                lower.indexOf("manifest") !== -1 ||
                lower.indexOf("playlist") !== -1 ||
                lower.indexOf("chunklist") !== -1 ||
                lower.indexOf("videoplayback") !== -1 ||
                lower.indexOf("googlevideo.com") !== -1 ||
                lower.indexOf("jwpsrv.com") !== -1;

            const looksLikeLive =
                lower.indexOf("/live/") !== -1 ||
                lower.indexOf("live.m3u8") !== -1 ||
                lower.indexOf("livestream") !== -1 ||
                lower.indexOf("broadcast") !== -1 ||
                lower.indexOf("linear") !== -1 ||
                lower.indexOf("yt_live_broadcast") !== -1 ||
                lower.indexOf("live=1") !== -1;

            if (
                looksLikeMedia ||
                looksLikeLive ||
                initiator === "video" ||
                initiator === "audio" ||
                initiator === "xmlhttprequest" ||
                initiator === "fetch"
            ) {

                let clean =
                    String(url)
                        .replace(/\\u0026/g, "&")
                        .replace(/\\u003d/g, "=")
                        .replace(/\\u003f/g, "?")
                        .replace(/\\u002f/g, "/")
                        .replace(/\\\//g, "/")
                        .replace(/&amp;/g, "&")
                        .trim();

                try {
                    clean =
                        decodeURIComponent(clean);
                } catch(e) {}

                gelPush(
                    clean
                );

                console.log(
                    "GEL_PERFORMANCE_DEEP:",
                    initiator,
                    clean
                );
            }

        } catch(e) {}
    });

} catch(e) {}

// =====================================
// PERFORMANCE OBSERVER LIVE MEDIA SCAN
// =====================================

try {

    if (!window.__gelPerformanceObserverHooked) {

        window.__gelPerformanceObserverHooked = true;

        function gelCleanPerfObserverUrl(u) {

            try {

                let clean =
                    String(u)
                        .replace(/\\u0026/g, "&")
                        .replace(/\\u003d/g, "=")
                        .replace(/\\u003f/g, "?")
                        .replace(/\\u002f/g, "/")
                        .replace(/\\\//g, "/")
                        .replace(/&amp;/g, "&")
                        .trim();

                try {
                    clean =
                        decodeURIComponent(clean);
                } catch(e) {}

                return clean;

            } catch(e) {

                return "";
            }
        }

        function gelPerfObserverLooksMedia(u) {

            try {

                const lower =
                    String(u).toLowerCase();

                return (
                    lower.indexOf(".m3u8") !== -1 ||
                    lower.indexOf("manifest/hls") !== -1 ||
                    lower.indexOf("hls_playlist") !== -1 ||
                    lower.indexOf(".mpd") !== -1 ||
                    lower.indexOf(".mp4") !== -1 ||
                    lower.indexOf(".m4s") !== -1 ||
                    lower.indexOf(".ts") !== -1 ||
                    lower.indexOf("videoplayback") !== -1 ||
                    lower.indexOf("googlevideo.com") !== -1 ||
                    lower.indexOf("jwpsrv.com") !== -1 ||
                    lower.indexOf("playlist") !== -1 ||
                    lower.indexOf("chunklist") !== -1 ||
                    lower.indexOf("manifest") !== -1 ||
                    lower.indexOf("live") !== -1 ||
                    lower.indexOf("broadcast") !== -1 ||
                    lower.indexOf("linear") !== -1
                );

            } catch(e) {

                return false;
            }
        }

        const perfObserver =
            new PerformanceObserver(function(list) {

                try {

                    list
                        .getEntries()
                        .forEach(function(entry) {

                            try {

                                const url =
                                    entry.name || "";

                                if (!url) {
                                    return;
                                }

                                const initiator =
                                    entry.initiatorType || "";

                                const clean =
                                    gelCleanPerfObserverUrl(
                                        url
                                    );

                                if (
                                    clean &&
                                    (
                                        gelPerfObserverLooksMedia(clean) ||
                                        initiator === "video" ||
                                        initiator === "audio" ||
                                        initiator === "fetch" ||
                                        initiator === "xmlhttprequest"
                                    )
                                ) {

                                    gelPush(
                                        clean
                                    );

                                    console.log(
                                        "GEL_PERFORMANCE_OBSERVER:",
                                        initiator,
                                        clean
                                    );
                                }

                            } catch(e) {}
                        });

                } catch(e) {}
            });

        perfObserver.observe(
            {
                entryTypes: [
                    "resource"
                ]
            }
        );

        console.log(
            "GEL_PERFORMANCE_OBSERVER_READY"
        );
    }

} catch(e) {}

// =====================================
// BLOB / MSE
// =====================================

try {

    document
        .querySelectorAll("video")
        .forEach(function(v) {

            try {

                if (
                    v.src &&
                    v.src.startsWith("blob:")
                ) {

                    gelPush(v.src);

                    console.log(
                        "GEL_BLOB_VIDEO:",
                        v.src
                    );
                }

                if (
                    v.currentSrc &&
                    v.currentSrc.startsWith("blob:")
                ) {

                    gelPush(v.currentSrc);

                    console.log(
                        "GEL_BLOB_CURRENT:",
                        v.currentSrc
                    );
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// MEDIA SOURCE / SOURCEBUFFER HOOK SCAN
// =====================================

try {

    if (!window.__gelMseHooked) {

        window.__gelMseHooked = true;

        // =====================================
        // MEDIA SOURCE DETECTION
        // =====================================

        try {

            if (window.MediaSource) {

                console.log(
                    "GEL_MSE_AVAILABLE"
                );

                const originalAddSourceBuffer =
                    MediaSource.prototype.addSourceBuffer;

                MediaSource.prototype.addSourceBuffer =
                    function(mimeType) {

                        try {

                            console.log(
                                "GEL_MSE_SOURCE_BUFFER:",
                                mimeType
                            );

                            if (
                                mimeType &&
                                (
                                    String(mimeType).toLowerCase().indexOf("video") !== -1 ||
                                    String(mimeType).toLowerCase().indexOf("audio") !== -1 ||
                                    String(mimeType).toLowerCase().indexOf("mp4") !== -1
                                )
                            ) {

                                gelPush(
                                    "blob:mse-detected"
                                );
                            }

                        } catch(e) {}

                        return originalAddSourceBuffer.apply(
                            this,
                            arguments
                        );
                    };
            }

        } catch(e) {}

        // =====================================
        // SOURCEBUFFER APPEND DETECTION
        // =====================================

        try {

            if (window.SourceBuffer) {

                const originalAppendBuffer =
                    SourceBuffer.prototype.appendBuffer;

                SourceBuffer.prototype.appendBuffer =
                    function(buffer) {

                        try {

                            const size =
                                buffer
                                    ? buffer.byteLength || buffer.length || 0
                                    : 0;

                            console.log(
                                "GEL_MSE_APPEND_BUFFER:",
                                size
                            );

                            if (
                                size > 0
                            ) {

                                gelPush(
                                    "blob:mse-active-buffer"
                                );
                            }

                        } catch(e) {}

                        return originalAppendBuffer.apply(
                            this,
                            arguments
                        );
                    };
            }

        } catch(e) {}

        console.log(
            "GEL_MSE_HOOK_READY"
        );
    }

} catch(e) {}

return JSON.stringify(
    [...new Set(results)]
);

})();

""".trimIndent()

                    binding.contentMain.webview.evaluateJavascript(
                        js
                    ) { value ->

                        try {

                            val cleaned =
                                value
                                    ?.replace("\\u003C", "<")
                                    ?.replace("\\/", "/")
                                    ?.replace("\"[", "[")
                                    ?.replace("]\"", "]")
                                    ?: ""

                            val arr =
                                org.json.JSONArray(cleaned)

                            for (i in 0 until arr.length()) {

                                val found =
                                    arr.getString(i)

                                markStreamSource(
                                    found,
                                    "MONITOR"
                                )

                                detectAndSaveUrl(
                                    found
                                )
                            }

                        } catch (_: Throwable) {}

                        // =====================================
                        // SAFE PLAYBACK OBSERVER
                        // Do NOT auto-click page buttons.
                        // Auto-clicking breaks interactive players
                        // such as Rakuten TV and can freeze the WebView.
                        // =====================================

                        try {

                            binding.contentMain.webview.evaluateJavascript(

                                """
(function() {

try {

    document
        .querySelectorAll("video")
        .forEach(function(v) {

            try {

                v.muted = true;

            } catch(e) {}
        });

} catch(e) {}

})();
""".trimIndent()
                            ) {}

                        } catch (_: Throwable) {}

                        // =====================================
                        // LIVE STREAM MONITOR LOOP
                        // =====================================

                        if (monitorRunning) {

                            binding.contentMain.webview.postDelayed(
                                this,
                                4000
                            )
                        }
                    }

                } catch (_: Throwable) {

                    if (monitorRunning) {

                        binding.contentMain.webview.postDelayed(
                            this,
                            4000
                        )
                    }
                }
            }
        },

        4000
    )
}


// =====================================
// LOAD BROWSER HISTORY
// =====================================

private fun loadBrowserHistory() {

    try {

        browserHistory.clear()

        val prefs =
            getSharedPreferences(
                browserHistoryPrefsName,
                MODE_PRIVATE
            )

        val raw =
            prefs.getString(
                browserHistoryKey,
                "[]"
            )
                ?.trim()
                ?: "[]"

        if (raw.isBlank()) {
            return
        }

        val array =
            org.json.JSONArray(
                raw
            )

        for (i in 0 until array.length()) {

            try {

                val obj =
                    array.optJSONObject(
                        i
                    ) ?: continue

                val title =
                    obj.optString(
                        "title",
                        ""
                    ).trim()

                val url =
                    obj.optString(
                        "url",
                        ""
                    ).trim()

                val timestamp =
                    obj.optLong(
                        "timestamp",
                        0L
                    )

                if (
                    url.isBlank() ||
                    url.equals(
                        "about:blank",
                        true
                    )
                ) {
                    continue
                }

                browserHistory.add(
                    BrowserHistoryEntry(
                        title =
                            title.ifBlank {
                                url
                            },
                        url =
                            url,
                        timestamp =
                            timestamp
                    )
                )

            } catch (_: Throwable) {}
        }

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_HISTORY",
            "load failed",
            t
        )

        browserHistory.clear()
    }
}

// =====================================
// SAVE BROWSER HISTORY
// =====================================

private fun persistBrowserHistory() {

    try {

        val array =
            org.json.JSONArray()

        browserHistory
            .take(
                browserHistoryMaxItems
            )
            .forEach { entry ->

                try {

                    val obj =
                        org.json.JSONObject()

                    obj.put(
                        "title",
                        entry.title
                    )

                    obj.put(
                        "url",
                        entry.url
                    )

                    obj.put(
                        "timestamp",
                        entry.timestamp
                    )

                    array.put(
                        obj
                    )

                } catch (_: Throwable) {}
            }

        getSharedPreferences(
            browserHistoryPrefsName,
            MODE_PRIVATE
        )
            .edit()
            .putString(
                browserHistoryKey,
                array.toString()
            )
            .apply()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_HISTORY",
            "persist failed",
            t
        )
    }
}

// =====================================
// ADD BROWSER HISTORY ENTRY
// =====================================

private fun addBrowserHistory(
    url: String,
    title: String?
) {

    try {

        val cleanUrl =
            url.trim()

        if (
            cleanUrl.isBlank() ||
            cleanUrl.equals(
                "about:blank",
                true
            ) ||
            cleanUrl.startsWith(
                "javascript:",
                true
            ) ||
            cleanUrl.startsWith(
                "data:",
                true
            )
        ) {
            return
        }

        loadBrowserHistory()

        browserHistory.removeAll { entry ->

            entry.url.equals(
                cleanUrl,
                true
            )
        }

        browserHistory.add(
            0,
            BrowserHistoryEntry(
                title =
                    title
                        ?.trim()
                        ?.ifBlank {
                            cleanUrl
                        }
                        ?: cleanUrl,
                url =
                    cleanUrl,
                timestamp =
                    System.currentTimeMillis()
            )
        )

        while (
            browserHistory.size >
            browserHistoryMaxItems
        ) {

            browserHistory.removeAt(
                browserHistory.lastIndex
            )
        }

        persistBrowserHistory()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_HISTORY",
            "add failed",
            t
        )
    }
}

// =====================================
// OPEN HISTORY URL
// =====================================

private fun openHistoryUrl(
    url: String
) {

    try {

        if (url.isBlank()) {
            return
        }

        binding.contentMain.urlInput.setText(
            url
        )

        binding.contentMain.urlInput.setSelection(
            0
        )

        liveUrlInputText =
            url

        binding.contentMain.webview.loadUrl(
            url,
            mapOf(
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_HISTORY",
            "open failed",
            t
        )

        Toast.makeText(
            this,
            "Cannot open history item",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHOW BROWSER HISTORY DIALOG
// =====================================

private fun showBrowserHistoryDialog() {

    try {

        loadBrowserHistory()

        if (browserHistory.isEmpty()) {

            Toast.makeText(
                this,
                "No history",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val scrollView =
            android.widget.ScrollView(this)

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    20,
                    20,
                    20,
                    20
                )
            }

        scrollView.addView(container)

        val formatter =
            java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                java.util.Locale.getDefault()
            )

        browserHistory
            .toList()
            .forEach { entry ->

                val itemBox =
                    LinearLayout(this).apply {

                        orientation =
                            LinearLayout.VERTICAL

                        setPadding(
                            0,
                            10,
                            0,
                            16
                        )
                    }

                val dateText =
                    try {

                        formatter.format(
                            java.util.Date(
                                entry.timestamp
                            )
                        )

                    } catch (_: Throwable) {

                        ""
                    }

                val info =
                    TextView(this).apply {

                        text =
                            """
${entry.title}

${entry.url}

$dateText
                            """.trimIndent()

                        textSize =
                            13f

                        setTextIsSelectable(
                            true
                        )

                        setPadding(
                            0,
                            0,
                            0,
                            10
                        )
                    }

                val buttonRow =
                    LinearLayout(this).apply {

                        orientation =
                            LinearLayout.HORIZONTAL
                    }

                val openButton =
                    Button(this).apply {

                        text =
                            "OPEN"

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {

                                setMargins(
                                    0,
                                    0,
                                    6,
                                    0
                                )
                            }

                        setOnClickListener {

                            openHistoryUrl(
                                entry.url
                            )
                        }
                    }

                val copyButton =
                    Button(this).apply {

                        text =
                            "COPY"

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {

                                setMargins(
                                    6,
                                    0,
                                    6,
                                    0
                                )
                            }

                        setOnClickListener {

                            copyTextToClipboard(
                                "history_url",
                                entry.url,
                                "URL copied"
                            )
                        }
                    }

                val deleteButton =
                    Button(this).apply {

                        text =
                            "DELETE"

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {

                                setMargins(
                                    6,
                                    0,
                                    0,
                                    0
                                )
                            }

                        setOnClickListener {

                            try {

                                browserHistory.removeAll { saved ->

                                    saved.url == entry.url
                                }

                                persistBrowserHistory()

                                Toast.makeText(
                                    this@MainActivity,
                                    "History item deleted",
                                    Toast.LENGTH_SHORT
                                ).show()

                                showBrowserHistoryDialog()

                            } catch (_: Throwable) {}
                        }
                    }

                buttonRow.addView(openButton)
                buttonRow.addView(copyButton)
                buttonRow.addView(deleteButton)

                itemBox.addView(info)
                itemBox.addView(buttonRow)

                container.addView(itemBox)

                val line =
                    TextView(this).apply {

                        text =
                            "────────────────────────"

                        textSize =
                            12f
                    }

                container.addView(line)
            }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("History")
            .setView(scrollView)
            .setPositiveButton(
                "CLEAR ALL"
            ) { _, _ ->

                try {

                    browserHistory.clear()
                    persistBrowserHistory()

                    Toast.makeText(
                        this,
                        "History cleared",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (_: Throwable) {}
            }
            .setNegativeButton(
                "CLOSE",
                null
            )
            .show()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_HISTORY",
            "dialog failed",
            t
        )

        Toast.makeText(
            this,
            "History failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// LOAD BROWSER BOOKMARKS
// =====================================

private fun loadBrowserBookmarks() {

    try {

        browserBookmarks.clear()

        val prefs =
            getSharedPreferences(
                browserBookmarksPrefsName,
                MODE_PRIVATE
            )

        val raw =
            prefs.getString(
                browserBookmarksKey,
                "[]"
            )
                ?.trim()
                ?: "[]"

        if (raw.isBlank()) {
            return
        }

        val array =
            org.json.JSONArray(
                raw
            )

        for (i in 0 until array.length()) {

            try {

                val obj =
                    array.optJSONObject(
                        i
                    ) ?: continue

                val title =
                    obj.optString(
                        "title",
                        ""
                    ).trim()

                val url =
                    obj.optString(
                        "url",
                        ""
                    ).trim()

                val timestamp =
                    obj.optLong(
                        "timestamp",
                        0L
                    )

                if (
                    url.isBlank() ||
                    url.equals(
                        "about:blank",
                        true
                    )
                ) {
                    continue
                }

                browserBookmarks.add(
                    BrowserBookmarkEntry(
                        title =
                            title.ifBlank {
                                url
                            },
                        url =
                            url,
                        timestamp =
                            timestamp
                    )
                )

            } catch (_: Throwable) {}
        }

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_BOOKMARKS",
            "load failed",
            t
        )

        browserBookmarks.clear()
    }
}

// =====================================
// SAVE BROWSER BOOKMARKS
// =====================================

private fun persistBrowserBookmarks() {

    try {

        val array =
            org.json.JSONArray()

        browserBookmarks
            .take(
                browserBookmarksMaxItems
            )
            .forEach { entry ->

                try {

                    val obj =
                        org.json.JSONObject()

                    obj.put(
                        "title",
                        entry.title
                    )

                    obj.put(
                        "url",
                        entry.url
                    )

                    obj.put(
                        "timestamp",
                        entry.timestamp
                    )

                    array.put(
                        obj
                    )

                } catch (_: Throwable) {}
            }

        getSharedPreferences(
            browserBookmarksPrefsName,
            MODE_PRIVATE
        )
            .edit()
            .putString(
                browserBookmarksKey,
                array.toString()
            )
            .apply()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_BOOKMARKS",
            "persist failed",
            t
        )
    }
}

// =====================================
// GET CURRENT PAGE URL / TITLE
// =====================================

private fun getCurrentPageUrl(): String {

    return try {

        val webUrl =
            binding.contentMain.webview.url
                ?.trim()
                .orEmpty()

        val inputUrl =
            binding.contentMain.urlInput
                .text
                ?.toString()
                ?.trim()
                .orEmpty()

        when {

            webUrl.isNotBlank() &&
                !webUrl.equals(
                    "about:blank",
                    true
                ) -> webUrl

            inputUrl.isNotBlank() -> inputUrl

            liveUrlInputText.isNotBlank() -> liveUrlInputText

            else -> ""
        }

    } catch (_: Throwable) {

        ""
    }
}

private fun getCurrentPageTitle(): String {

    return try {

        binding.contentMain.webview.title
            ?.trim()
            ?.ifBlank {
                getCurrentPageUrl()
            }
            ?: getCurrentPageUrl()

    } catch (_: Throwable) {

        getCurrentPageUrl()
    }
}

// =====================================
// ADD CURRENT PAGE BOOKMARK
// =====================================

private fun addCurrentPageBookmark() {

    try {

        val url =
            getCurrentPageUrl()

        if (
            url.isBlank() ||
            url.equals(
                "about:blank",
                true
            )
        ) {

            Toast.makeText(
                this,
                "No page to bookmark",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        loadBrowserBookmarks()

        browserBookmarks.removeAll { entry ->

            entry.url.equals(
                url,
                true
            )
        }

        browserBookmarks.add(
            0,
            BrowserBookmarkEntry(
                title =
                    getCurrentPageTitle(),
                url =
                    url,
                timestamp =
                    System.currentTimeMillis()
            )
        )

        while (
            browserBookmarks.size >
            browserBookmarksMaxItems
        ) {

            browserBookmarks.removeAt(
                browserBookmarks.lastIndex
            )
        }

        persistBrowserBookmarks()

        Toast.makeText(
            this,
            "Bookmark saved",
            Toast.LENGTH_SHORT
        ).show()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_BOOKMARKS",
            "add current failed",
            t
        )

        Toast.makeText(
            this,
            "Bookmark failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// OPEN BOOKMARK URL
// =====================================

private fun openBookmarkUrl(
    url: String
) {

    try {

        if (url.isBlank()) {
            return
        }

        binding.contentMain.urlInput.setText(
            url
        )

        binding.contentMain.urlInput.setSelection(
            0
        )

        liveUrlInputText =
            url

        binding.contentMain.webview.loadUrl(
            url,
            mapOf(
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_BOOKMARKS",
            "open failed",
            t
        )

        Toast.makeText(
            this,
            "Cannot open bookmark",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHOW BOOKMARKS DIALOG
// =====================================

private fun showBrowserBookmarksDialog() {

    try {

        loadBrowserBookmarks()

        if (browserBookmarks.isEmpty()) {

            Toast.makeText(
                this,
                "No bookmarks",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val scrollView =
            android.widget.ScrollView(this)

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    20,
                    20,
                    20,
                    20
                )
            }

        scrollView.addView(container)

        val formatter =
            java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                java.util.Locale.getDefault()
            )

        browserBookmarks
            .toList()
            .forEach { entry ->

                val itemBox =
                    LinearLayout(this).apply {

                        orientation =
                            LinearLayout.VERTICAL

                        setPadding(
                            0,
                            10,
                            0,
                            16
                        )
                    }

                val dateText =
                    try {

                        formatter.format(
                            java.util.Date(
                                entry.timestamp
                            )
                        )

                    } catch (_: Throwable) {

                        ""
                    }

                val info =
                    TextView(this).apply {

                        text =
                            """
${entry.title}

${entry.url}

$dateText
                            """.trimIndent()

                        textSize =
                            13f

                        setTextIsSelectable(
                            true
                        )

                        setPadding(
                            0,
                            0,
                            0,
                            10
                        )
                    }

                val buttonRow =
                    LinearLayout(this).apply {

                        orientation =
                            LinearLayout.HORIZONTAL
                    }

                val openButton =
                    Button(this).apply {

                        text =
                            "OPEN"

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {

                                setMargins(
                                    0,
                                    0,
                                    6,
                                    0
                                )
                            }

                        setOnClickListener {

                            openBookmarkUrl(
                                entry.url
                            )
                        }
                    }

                val copyButton =
                    Button(this).apply {

                        text =
                            "COPY"

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {

                                setMargins(
                                    6,
                                    0,
                                    6,
                                    0
                                )
                            }

                        setOnClickListener {

                            copyTextToClipboard(
                                "bookmark_url",
                                entry.url,
                                "URL copied"
                            )
                        }
                    }

                val deleteButton =
                    Button(this).apply {

                        text =
                            "DELETE"

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {

                                setMargins(
                                    6,
                                    0,
                                    0,
                                    0
                                )
                            }

                        setOnClickListener {

                            try {

                                browserBookmarks.removeAll { saved ->

                                    saved.url == entry.url
                                }

                                persistBrowserBookmarks()

                                Toast.makeText(
                                    this@MainActivity,
                                    "Bookmark deleted",
                                    Toast.LENGTH_SHORT
                                ).show()

                                showBrowserBookmarksDialog()

                            } catch (_: Throwable) {}
                        }
                    }

                buttonRow.addView(openButton)
                buttonRow.addView(copyButton)
                buttonRow.addView(deleteButton)

                itemBox.addView(info)
                itemBox.addView(buttonRow)

                container.addView(itemBox)

                val line =
                    TextView(this).apply {

                        text =
                            "────────────────────────"

                        textSize =
                            12f
                    }

                container.addView(line)
            }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bookmarks")
            .setView(scrollView)
            .setPositiveButton(
                "CLEAR ALL"
            ) { _, _ ->

                try {

                    browserBookmarks.clear()
                    persistBrowserBookmarks()

                    Toast.makeText(
                        this,
                        "Bookmarks cleared",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (_: Throwable) {}
            }
            .setNegativeButton(
                "CLOSE",
                null
            )
            .show()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_BOOKMARKS",
            "dialog failed",
            t
        )

        Toast.makeText(
            this,
            "Bookmarks failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// COPY TEXT TO CLIPBOARD
// =====================================

private fun copyTextToClipboard(
    label: String,
    text: String,
    toast: String
) {

    try {

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                label,
                text
            )
        )

        Toast.makeText(
            this,
            toast,
            Toast.LENGTH_SHORT
        ).show()

    } catch (t: Throwable) {

        Log.e(
            "CLIPBOARD",
            "copy failed",
            t
        )

        Toast.makeText(
            this,
            "Copy failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHARE TEXT
// =====================================

private fun shareText(
    title: String,
    text: String
) {

    try {

        if (text.isBlank()) {

            Toast.makeText(
                this,
                "Nothing to share",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "text/plain"

                putExtra(
                    Intent.EXTRA_TEXT,
                    text
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                title
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "SHARE_TEXT",
            "share failed",
            t
        )

        Toast.makeText(
            this,
            "Share failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// CLEAN DETECTED STREAM CANDIDATES
// Keeps real playable media only.
// Extracts embedded media URLs from tracker / translation wrappers.
// =====================================

private fun repeatedlyDecodeUrl(
    rawUrl: String
): String {

    var current =
        rawUrl
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .trim()

    try {

        repeat(
            3
        ) {

            val decoded =
                java.net.URLDecoder.decode(
                    current,
                    "UTF-8"
                )
                    .replace("\\u0026", "&")
                    .replace("\\/", "/")
                    .replace("&amp;", "&")
                    .trim()

            if (decoded == current) {
                return current
            }

            current =
                decoded
        }

    } catch (_: Throwable) {}

    return current
}

private fun isWrapperOrTrackerUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.contains("facebook.com/tr") ||
            lower.contains("/tr/?") ||
            lower.contains("translate.goog") ||
            lower.contains("translate.google") ||
            lower.contains("google.com/translate") ||
            lower.contains("doubleclick") ||
            lower.contains("googleads") ||
            lower.contains("googlesyndication") ||
            lower.contains("google-analytics") ||
            lower.contains("analytics") ||
            lower.contains("pagead") ||
            lower.contains("collect?") ||
            lower.contains("/collect") ||
            lower.contains("/ajax?") ||
            lower.contains("/stats/") ||
            lower.contains("ptracking") ||
            lower.contains("moat") ||
            lower.contains("beacon") ||
            lower.contains("pixel") ||
            lower.contains("telemetry") ||
            lower.contains("metrics") ||
            lower.contains("favicon") ||
            lower.contains("logo") ||
            lower.contains("banner")
        )
}

private fun isProbablyPlayableMediaUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains(".mp4") ||
            lower.contains(".webm") ||
            lower.contains(".mkv") ||
            lower.contains(".mov") ||
            lower.contains(".avi") ||
            lower.contains(".3gp") ||
            lower.endsWith(".ts") ||
            lower.contains(".ts?") ||
            lower.contains(".mp3") ||
            lower.contains(".m4a") ||
            lower.contains(".aac") ||
            lower.contains(".opus") ||
            lower.contains(".wav") ||
            lower.contains(".ogg") ||
            lower.contains(".flac") ||
            lower.contains("manifest/hls") ||
            lower.contains("hls_playlist") ||
            lower.contains("hlsmanifesturl") ||
            lower.contains("googlevideo.com") ||
            lower.contains("videoplayback") ||
            lower.contains("youtube.com/watch") ||
            lower.contains("youtu.be/")
        )
}

private fun isTsMediaSegmentUrl(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    return (
        lower.endsWith(".ts") ||
            lower.contains(".ts?") ||
            Regex("/[^/?#]+\\.ts($|[?#])")
                .containsMatchIn(
                    lower
                )
        )
}

private fun cleanDetectedPlayableUrl(
    rawUrl: String
): String {

    var clean =
        rawUrl
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .trim()
            .trimEnd(',')
            .trimEnd(';')
            .trimEnd(')')
            .trimEnd(']')
            .trimEnd('}')
            .trimEnd('"')
            .trimEnd('\'')

    // Some tracker URLs contain a real stream followed by tracking fields.
    // Example: ...playlist.m3u8&cd[Program_Name]=...
    try {

        val lower =
            clean.lowercase()

        val cutMarkers =
            listOf(
                "&cd[",
                "&fbp=",
                "&fbc=",
                "&pmd[",
                "&expv",
                "&rqm=",
                "&dl=",
                "&ev=",
                "&rl=",
                "&if=",
                "&ts=",
                "&iw=",
                "&coo=",
                "&x_tr_"
            )

        if (
            lower.contains(".m3u8") ||
            lower.contains(".mpd")
        ) {

            cutMarkers.forEach { marker ->

                val index =
                    clean.indexOf(
                        marker,
                        ignoreCase = true
                    )

                if (index > 0) {
                    clean = clean.substring(0, index)
                }
            }
        }

    } catch (_: Throwable) {}

    return clean.trim()
}

private fun extractEmbeddedPlayableUrls(
    rawUrl: String
): List<String> {

    val out =
        mutableListOf<String>()

    fun addCandidate(
        value: String?
    ) {

        val candidate =
            value
                ?.trim()
                .orEmpty()

        if (candidate.isBlank()) {
            return
        }

        val decoded =
            repeatedlyDecodeUrl(
                candidate
            )

        val clean =
            cleanDetectedPlayableUrl(
                decoded
            )

        if (
            clean.startsWith(
                "http",
                true
            ) &&
            isProbablyPlayableMediaUrl(
                clean
            )
        ) {
            out.add(
                clean
            )
        }
    }

    try {

        val uri =
            Uri.parse(
                rawUrl
            )

        val keys =
            uri.queryParameterNames

        keys.forEach { key ->

            val lowerKey =
                key.lowercase()

            if (
                lowerKey == "url" ||
                lowerKey == "u" ||
                lowerKey == "src" ||
                lowerKey == "stream" ||
                lowerKey == "file" ||
                lowerKey == "media" ||
                lowerKey == "video" ||
                lowerKey == "dl" ||
                lowerKey == "cd[url]" ||
                lowerKey.contains("stream") ||
                lowerKey.contains("playlist") ||
                lowerKey.contains("manifest")
            ) {

                addCandidate(
                    uri.getQueryParameter(
                        key
                    )
                )
            }
        }

    } catch (_: Throwable) {}

    try {

        val decoded =
            repeatedlyDecodeUrl(
                rawUrl
            )

        val regex =
            "(https?://[^\\s\"'<>]+)"
                .toRegex()

        regex.findAll(
            decoded
        ).forEach { match ->

            addCandidate(
                match.value
            )
        }

    } catch (_: Throwable) {}

    return out
        .distinct()
}

private fun expandDetectedStreamCandidate(
    rawUrl: String
): List<String> {

    val normalized =
        cleanDetectedPlayableUrl(
            repeatedlyDecodeUrl(
                rawUrl
            )
        )

    val embedded =
        extractEmbeddedPlayableUrls(
            rawUrl
        )

    val result =
        mutableListOf<String>()

    result.addAll(
        embedded
    )

    if (
        normalized.startsWith(
            "http",
            true
        ) &&
        !isWrapperOrTrackerUrl(
            normalized
        ) &&
        isProbablyPlayableMediaUrl(
            normalized
        )
    ) {

        result.add(
            normalized
        )
    }

    return result
        .distinct()
}

// =====================================
// COLLECT PLAYABLE STREAM URLS FOR MENU
// Clean version:
// - removes trackers / translate / analytics wrappers
// - extracts embedded real streams from wrapper URLs
// - hides .ts segments when a matching .m3u8 exists
// =====================================

private fun collectPlayableStreamUrls(): List<String> {

    val rawStreams =
        mutableListOf<String>()

    try {

        rawStreams.addAll(
            detectedStreams
        )

        rawStreams.addAll(
            detectedVideos
        )

        rawStreams.addAll(
            detectedAudio
        )

        rawStreams.addAll(
            streamInfoSnapshots.keys
        )

        rawStreams.addAll(
            streamSources.keys
        )

        rawStreams.addAll(
            streamValidation.keys
        )

        rawStreams.addAll(
            streamScores.keys
        )

        rawStreams.addAll(
            streamTokens.keys
        )

        if (bestStreamUrl.isNotBlank()) {
            rawStreams.add(
                bestStreamUrl
            )
        }

        if (bestLiveUrl.isNotBlank()) {
            rawStreams.add(
                bestLiveUrl
            )
        }

        if (youtubeWatchUrl.isNotBlank()) {
            rawStreams.add(
                youtubeWatchUrl
            )
        }

        if (youtubeDashVideoUrl.isNotBlank()) {
            rawStreams.add(
                youtubeDashVideoUrl
            )
        }

        if (youtubeDashAudioUrl.isNotBlank()) {
            rawStreams.add(
                youtubeDashAudioUrl
            )
        }

        val resultText =
            binding.contentMain.result
                .text
                ?.toString()
                .orEmpty()

        val regex =
            "(https?://[^\\s\"'<>]+)"
                .toRegex()

        regex.findAll(
            resultText
        ).forEach { match ->

            rawStreams.add(
                match.value
            )
        }

    } catch (_: Throwable) {}

    val expandedStreams =
        mutableListOf<String>()

    rawStreams.forEach { raw ->

        try {

            expandedStreams.addAll(
                expandDetectedStreamCandidate(
                    raw
                )
            )

        } catch (_: Throwable) {}
    }

    val cleanedStreams =
        expandedStreams
            .mapNotNull { rawUrl ->

                try {

                    val decoded =
                        repeatedlyDecodeUrl(
                            rawUrl
                        )

                    val playableClean =
                        cleanDetectedPlayableUrl(
                            decoded
                        )

                    val dailymotionClean =
                        cleanDailymotionUrlForExport(
                            playableClean
                        )

                    val finalClean =
                        cleanDetectedUrl(
                            dailymotionClean
                        ).trim()

                    if (
                        finalClean.isBlank() ||
                        !finalClean.startsWith(
                            "http",
                            true
                        ) ||
                        isWrapperOrTrackerUrl(
                            finalClean
                        ) ||
                        !isExportableStream(
                            finalClean
                        )
                    ) {
                        null
                    } else {
                        finalClean
                    }

                } catch (_: Throwable) {

                    null
                }
            }

    val playable =
        cleanedStreams
            .map { url ->

                val key =
                    try {

                        val lower =
                            url.lowercase()

                        when {

                            lower.contains(
                                "googlevideo.com"
                            ) ||
                                lower.contains(
                                    "videoplayback"
                                ) -> {

                                val uri =
                                    Uri.parse(
                                        url
                                    )

                                val id =
                                    uri.getQueryParameter(
                                        "id"
                                    )
                                        ?.substringBefore(
                                            "."
                                        )
                                        .orEmpty()

                                val itag =
                                    uri.getQueryParameter(
                                        "itag"
                                    )
                                        .orEmpty()

                                val mime =
                                    uri.getQueryParameter(
                                        "mime"
                                    )
                                        .orEmpty()

                                val source =
                                    uri.getQueryParameter(
                                        "source"
                                    )
                                        .orEmpty()

                                "googlevideo://$id/$itag/$mime/$source"
                            }

                            lower.contains(
                                "youtube.com/watch"
                            ) ||
                                lower.contains(
                                    "youtu.be/"
                                ) ||
                                lower.contains(
                                    "youtube.com/live"
                                ) ||
                                lower.contains(
                                    "youtube.com/c/"
                                ) -> {

                                val uri =
                                    Uri.parse(
                                        url
                                    )

                                val v =
                                    uri.getQueryParameter(
                                        "v"
                                    )
                                        .orEmpty()

                                if (v.isNotBlank()) {
                                    "youtube://watch/$v"
                                } else {
                                    url.substringBefore(
                                        "#"
                                    ).trim()
                                }
                            }

                            lower.contains(
                                "dailymotion.com/cdn/manifest/video/"
                            ) &&
                                lower.contains(
                                    ".m3u8"
                                ) -> {

                                url.substringBefore(
                                    "?"
                                )
                                    .substringBefore(
                                        "#"
                                    )
                                    .trim()
                                    .lowercase()
                            }

                            else -> {

                                url.substringBefore(
                                    "#"
                                ).trim()
                            }
                        }

                    } catch (_: Throwable) {

                        url
                    }

                key to url
            }
            .distinctBy { pair ->

                pair.first
                    .lowercase()
            }
            .map { pair ->

                pair.second
            }

    val hasM3u8 =
        playable.any { url ->

            val lower =
                url.lowercase()

            lower.contains(
                ".m3u8"
            ) ||
                lower.contains(
                    "manifest/hls"
                ) ||
                lower.contains(
                    "hls_playlist"
                ) ||
                lower.contains(
                    "hlsmanifesturl"
                )
        }

    val noSegments =
        if (hasM3u8) {

            playable.filter { url ->

                !isTsMediaSegmentUrl(
                    url
                )
            }

        } else {

            playable
        }

    return noSegments
        .distinctBy { url ->

            url.lowercase()
        }
}

// =====================================
// CLEAN BEST STREAM URL
// Never expose tracker / analytics wrappers as Best Stream.
// Extracts embedded playable media URL when Best Stream was detected
// through Google/Facebook/analytics wrapper payloads.
// =====================================

private fun getCleanBestStreamUrl(): String {

    return try {

        val candidates =
            mutableListOf<String>()

        if (bestStreamUrl.isNotBlank()) {

            candidates.addAll(
                expandDetectedStreamCandidate(
                    bestStreamUrl
                )
            )
        }

        if (bestLiveUrl.isNotBlank()) {

            candidates.addAll(
                expandDetectedStreamCandidate(
                    bestLiveUrl
                )
            )
        }

        candidates.addAll(
            collectPlayableStreamUrls()
        )

        candidates
            .map { candidate ->

                cleanDetectedPlayableUrl(
                    repeatedlyDecodeUrl(
                        candidate
                    )
                )
            }
            .filter { candidate ->

                candidate.isNotBlank() &&
                    candidate.startsWith(
                        "http",
                        true
                    ) &&
                    !isWrapperOrTrackerUrl(
                        candidate
                    ) &&
                    isExportableStream(
                        candidate
                    )
            }
            .maxByOrNull { candidate ->

                calculateStreamScore(
                    candidate
                )
            }
            .orEmpty()

    } catch (_: Throwable) {

        ""
    }
}


// =====================================
// OPEN URL WITH PLAYER CHOOSER
// =====================================

private fun openUrlWithPlayerChooser(
    rawUrl: String
) {

    try {

        val urlToOpen =
            rawUrl
                .trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")

        if (urlToOpen.isBlank()) {
            return
        }

        lastSelectedUrl =
            urlToOpen

        val lower =
            urlToOpen.lowercase()

        if (
            lower.contains("youtube.com/watch") ||
            lower.contains("youtu.be/") ||
            lower.contains("youtube.com/live") ||
            lower.contains("youtube.com/c/")
        ) {

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    data =
                        Uri.parse(
                            urlToOpen
                        )

                    addCategory(
                        Intent.CATEGORY_BROWSABLE
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Open YouTube Link"
                )
            )

            return
        }

        val mimeType =
            when {

                lower.contains(".mp3") ->
                    "audio/mpeg"

                lower.contains(".m4a") ->
                    "audio/mp4"

                lower.contains(".aac") ->
                    "audio/aac"

                lower.contains(".opus") ->
                    "audio/opus"

                lower.contains(".wav") ->
                    "audio/wav"

                lower.contains(".ogg") ->
                    "audio/ogg"

                lower.contains(".flac") ->
                    "audio/flac"

                lower.contains(".m3u8") ||
                    lower.contains("hls") ->
                    "application/vnd.apple.mpegurl"

                lower.contains(".mpd") ||
                    lower.contains("dash") ->
                    "application/dash+xml"

                lower.contains(".mp4") ->
                    "video/mp4"

                lower.contains(".webm") ->
                    "video/webm"

                lower.contains(".mkv") ||
                    lower.contains(".mov") ||
                    lower.contains(".avi") ||
                    lower.contains(".3gp") ||
                    lower.endsWith(".ts") ||
                    lower.contains(".ts?") ||
                    lower.contains("googlevideo.com") ||
                    lower.contains("videoplayback") ->
                    "video/*"

                else ->
                    "*/*"
            }

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                setDataAndType(
                    Uri.parse(
                        urlToOpen
                    ),
                    mimeType
                )

                addCategory(
                    Intent.CATEGORY_BROWSABLE
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Open Stream With"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "PLAYER_OPEN",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Cannot open stream",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// OPEN SELECTED URLS
// If many selected, choose one from selected list
// =====================================

private fun openSelectedUrlsWithPlayer(
    selectedUrls: List<String>
) {

    try {

        val cleanList =
            selectedUrls
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        if (cleanList.isEmpty()) {

            Toast.makeText(
                this,
                "No streams selected",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (cleanList.size == 1) {

            openUrlWithPlayerChooser(
                cleanList.first()
            )

            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Stream To Open")
            .setItems(
                cleanList.toTypedArray()
            ) { _, which ->

                openUrlWithPlayerChooser(
                    cleanList[which]
                )
            }
            .setNegativeButton(
                "CLOSE",
                null
            )
            .show()

    } catch (_: Throwable) {}
}

// =====================================
// SELECTABLE URL DIALOG
// Shared by Detected Streams and Saved Channels
// =====================================

private fun showSelectableUrlsDialog(
    title: String,
    labels: List<String>,
    urls: List<String>,
    allowSave: Boolean,
    allowDelete: Boolean,
    onDeleteSelected: ((List<String>) -> Unit)?
) {

    try {

        if (urls.isEmpty()) {

            Toast.makeText(
                this,
                "No items",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val safeLabels =
            if (labels.size == urls.size) {
                labels
            } else {
                urls
            }

        val selected =
            mutableListOf<String>()

        val dialogHeight =
            (resources.displayMetrics.heightPixels * 0.90f).toInt()

        val root =
            android.widget.LinearLayout(this).apply {

                orientation =
                    android.widget.LinearLayout.VERTICAL

                setPadding(
                    20,
                    10,
                    20,
                    10
                )

                minimumHeight =
                    dialogHeight
            }

        val scrollView =
            android.widget.ScrollView(this).apply {

                isFillViewport =
                    false

                isVerticalScrollBarEnabled =
                    true

                overScrollMode =
                    android.view.View.OVER_SCROLL_ALWAYS

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
            }

        val listContainer =
            android.widget.LinearLayout(this).apply {

                orientation =
                    android.widget.LinearLayout.VERTICAL
            }

        scrollView.addView(
            listContainer,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val checkBoxes =
            mutableListOf<android.widget.CheckBox>()

        urls.forEachIndexed { index, url ->

            val checkBox =
                android.widget.CheckBox(this).apply {

                    text =
                        safeLabels[index]

                    textSize =
                        13f

                    setPadding(
                        0,
                        8,
                        0,
                        8
                    )

                    setOnCheckedChangeListener { _, isChecked ->

                        if (isChecked) {

                            if (!selected.contains(url)) {
                                selected.add(url)
                            }

                        } else {

                            selected.remove(url)
                        }
                    }
                }

            checkBoxes.add(
                checkBox
            )

            listContainer.addView(
                checkBox
            )

            val line =
                android.view.View(this).apply {

                    setBackgroundColor(
                        0xFFE0E0E0.toInt()
                    )

                    layoutParams =
                        android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                }

            listContainer.addView(
                line
            )
        }

        root.addView(
            scrollView
        )

        val buttonRow =
            android.widget.LinearLayout(this).apply {

                orientation =
                    android.widget.LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    10,
                    0,
                    0
                )
            }

        val selectAllButton =
            android.widget.Button(this).apply {

                text =
                    "SELECT\nALL"

                textSize =
                    10f

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val openButton =
            android.widget.Button(this).apply {

                text =
                    "OPEN"

                textSize =
                    11f

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val saveButton =
            android.widget.Button(this).apply {

                text =
                    "SAVE"

                textSize =
                    11f

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val copyButton =
            android.widget.Button(this).apply {

                text =
                    "COPY"

                textSize =
                    11f

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val shareButton =
            android.widget.Button(this).apply {

                text =
                    "SHARE"

                textSize =
                    11f

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val deleteButton =
            android.widget.Button(this).apply {

                text =
                    "DELETE"

                textSize =
                    11f

                layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        buttonRow.addView(selectAllButton)
        buttonRow.addView(openButton)

        if (allowSave) {
            buttonRow.addView(saveButton)
        }

        buttonRow.addView(copyButton)
        buttonRow.addView(shareButton)

        if (allowDelete) {
            buttonRow.addView(deleteButton)
        }

        root.addView(
            buttonRow
        )

        val dialog =
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(root)
                .setNegativeButton(
                    "CLOSE",
                    null
                )
                .show()

        try {

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                dialogHeight
            )

        } catch (_: Throwable) {}

        selectAllButton.setOnClickListener {

            selected.clear()

            checkBoxes.forEachIndexed { index, checkBox ->

                checkBox.isChecked =
                    true

                val url =
                    urls[index]

                if (!selected.contains(url)) {
                    selected.add(url)
                }
            }

            Toast.makeText(
                this,
                "All selected",
                Toast.LENGTH_SHORT
            ).show()
        }

        openButton.setOnClickListener {

            openSelectedUrlsWithPlayer(
                selected.toList()
            )
        }

        saveButton.setOnClickListener {

            if (selected.isEmpty()) {

                Toast.makeText(
                    this,
                    "No streams selected",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            var savedCount =
                0

            selected.forEach { url ->

                try {

                    addSavedChannel(
                        url
                    )

                    savedCount++

                } catch (_: Throwable) {}
            }

            Toast.makeText(
                this,
                "Saved $savedCount channel(s)",
                Toast.LENGTH_SHORT
            ).show()
        }

        copyButton.setOnClickListener {

            if (selected.isEmpty()) {

                Toast.makeText(
                    this,
                    "No streams selected",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            copyTextToClipboard(
                "selected_streams",
                selected.joinToString("\n\n"),
                "Selected streams copied"
            )
        }

        shareButton.setOnClickListener {

            if (selected.isEmpty()) {

                Toast.makeText(
                    this,
                    "No streams selected",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            shareText(
                title,
                selected.joinToString("\n\n")
            )
        }

        deleteButton.setOnClickListener {

            if (selected.isEmpty()) {

                Toast.makeText(
                    this,
                    "No streams selected",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete selected?")
                .setMessage("Delete ${selected.size} selected item(s)?")
                .setPositiveButton(
                    "DELETE"
                ) { _, _ ->

                    onDeleteSelected?.invoke(
                        selected.toList()
                    )

                    dialog.dismiss()
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .show()
        }

    } catch (t: Throwable) {

        Log.e(
            "SELECTABLE_URL_DIALOG",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Dialog failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHOW DETECTED STREAMS DIALOG
// Full selectable manager: SAVE / OPEN / COPY / SHARE
// =====================================

private fun showDetectedStreamsDialog() {

    try {

        val streamList =
            collectPlayableStreamUrls()
                .mapNotNull { rawUrl ->

                    val cleanUrl =
                        cleanDailymotionUrlForExport(
                            rawUrl
                        ).let { cleaned ->
                            cleanDetectedUrl(
                                cleaned
                            )
                        }.trim()

                    if (
                        cleanUrl.isBlank() ||
                        !isExportableStream(
                            cleanUrl
                        )
                    ) {
                        null
                    } else {
                        cleanUrl
                    }
                }
                .distinctBy { url ->
                    url.lowercase()
                }

        if (streamList.isEmpty()) {

            Toast.makeText(
                this,
                "No detected streams",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        showSelectableUrlsDialog(
            title = "Detected Streams",
            labels = streamList,
            urls = streamList,
            allowSave = true,
            allowDelete = false,
            onDeleteSelected = null
        )

    } catch (t: Throwable) {

        Log.e(
            "DETECTED_STREAMS_DIALOG",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Detected streams failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// COPY DETECTED STREAMS
// =====================================

private fun copyDetectedStreamsToClipboard() {

    val streamList =
        collectPlayableStreamUrls()

    if (streamList.isEmpty()) {

        Toast.makeText(
            this,
            "No detected streams",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    copyTextToClipboard(
        "detected_streams",
        streamList.joinToString("\n\n"),
        "Detected streams copied"
    )
}


// =====================================
// BROWSER TABS — SAVE CURRENT TAB
// =====================================

private fun saveCurrentBrowserTab() {

    try {

        val url =
            getCurrentPageUrl()

        if (
            url.isBlank() ||
            url.equals(
                "about:blank",
                true
            )
        ) {
            return
        }

        val entry =
            BrowserTabEntry(
                title = getCurrentPageTitle(),
                url = url,
                timestamp = System.currentTimeMillis()
            )

        if (
            currentBrowserTabIndex >= 0 &&
            currentBrowserTabIndex < browserTabs.size
        ) {

            browserTabs[currentBrowserTabIndex] =
                entry

        } else {

            browserTabs.add(entry)

            currentBrowserTabIndex =
                browserTabs.lastIndex
        }

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_TABS",
            "save current failed",
            t
        )
    }
}

// =====================================
// OPEN NEW TAB
// =====================================

private fun openNewBrowserTab() {

    try {

        saveCurrentBrowserTab()

        val newTabUrl =
            "https://www.google.com"

        browserTabs.add(
            BrowserTabEntry(
                title = "New Tab",
                url = newTabUrl,
                timestamp = System.currentTimeMillis()
            )
        )

        currentBrowserTabIndex =
            browserTabs.lastIndex

        binding.contentMain.urlInput.setText(newTabUrl)
        binding.contentMain.urlInput.setSelection(0)

        liveUrlInputText =
            newTabUrl

        binding.contentMain.webview.loadUrl(
            newTabUrl,
            mapOf(
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache"
            )
        )

        Toast.makeText(
            this,
            "New tab opened",
            Toast.LENGTH_SHORT
        ).show()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_TABS",
            "open new tab failed",
            t
        )

        Toast.makeText(
            this,
            "New tab failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// OPEN SAVED TAB
// =====================================

private fun openBrowserTab(
    entry: BrowserTabEntry,
    index: Int
) {

    try {

        saveCurrentBrowserTab()

        currentBrowserTabIndex =
            index

        binding.contentMain.urlInput.setText(entry.url)
        binding.contentMain.urlInput.setSelection(0)

        liveUrlInputText =
            entry.url

        binding.contentMain.webview.loadUrl(
            entry.url,
            mapOf(
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_TABS",
            "open tab failed",
            t
        )

        Toast.makeText(
            this,
            "Cannot open tab",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHOW BROWSER TABS
// =====================================

private fun showBrowserTabsDialog() {

    try {

        saveCurrentBrowserTab()

        if (browserTabs.isEmpty()) {

            Toast.makeText(
                this,
                "No tabs",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val scrollView =
            android.widget.ScrollView(this)

        val container =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
            }

        scrollView.addView(container)

        val formatter =
            java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                java.util.Locale.getDefault()
            )

        browserTabs.toList().forEachIndexed { index, entry ->

            val itemBox =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 10, 0, 16)
                }

            val dateText =
                try {
                    formatter.format(java.util.Date(entry.timestamp))
                } catch (_: Throwable) {
                    ""
                }

            val activeMark =
                if (index == currentBrowserTabIndex) {
                    "ACTIVE TAB\n\n"
                } else {
                    ""
                }

            val info =
                TextView(this).apply {
                    text =
                        """
$activeMark${entry.title}

${entry.url}

$dateText
                        """.trimIndent()
                    textSize = 13f
                    setTextIsSelectable(true)
                    setPadding(0, 0, 0, 10)
                }

            val buttonRow =
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

            val openButton =
                Button(this).apply {
                    text = "OPEN"
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(0, 0, 6, 0)
                    }
                    setOnClickListener {
                        openBrowserTab(entry, index)
                    }
                }

            val closeButton =
                Button(this).apply {
                    text = "CLOSE"
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(6, 0, 0, 0)
                    }
                    setOnClickListener {
                        try {
                            browserTabs.removeAt(index)
                            if (currentBrowserTabIndex >= browserTabs.size) {
                                currentBrowserTabIndex = browserTabs.lastIndex
                            }
                            Toast.makeText(
                                this@MainActivity,
                                "Tab closed",
                                Toast.LENGTH_SHORT
                            ).show()
                            showBrowserTabsDialog()
                        } catch (_: Throwable) {}
                    }
                }

            buttonRow.addView(openButton)
            buttonRow.addView(closeButton)
            itemBox.addView(info)
            itemBox.addView(buttonRow)
            container.addView(itemBox)

            val line = TextView(this).apply {
                text = "────────────────────────"
                textSize = 12f
            }
            container.addView(line)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tabs")
            .setView(scrollView)
            .setPositiveButton("NEW TAB") { _, _ ->
                openNewBrowserTab()
            }
            .setNegativeButton("CLOSE", null)
            .show()

    } catch (t: Throwable) {

        Log.e(
            "BROWSER_TABS",
            "dialog failed",
            t
        )

        Toast.makeText(
            this,
            "Tabs failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// TRANSLATE CURRENT PAGE
// =====================================

private fun translateCurrentPage() {

    try {

        val url =
            getCurrentPageUrl()

        if (
            url.isBlank() ||
            url.equals(
                "about:blank",
                true
            )
        ) {

            Toast.makeText(
                this,
                "No page to translate",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val options =
            arrayOf(
                "Translate to Greek",
                "Translate to English",
                "Translate to Portuguese",
                "Translate to Spanish"
            )

        val targets =
            arrayOf("el", "en", "pt", "es")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Translate Page")
            .setItems(options) { _, which ->

                try {

                    val encodedUrl =
                        java.net.URLEncoder.encode(
                            url,
                            "UTF-8"
                        )

                    val translateUrl =
                        "https://translate.google.com/translate?sl=auto&tl=${targets[which]}&u=$encodedUrl"

                    binding.contentMain.urlInput.setText(translateUrl)
                    binding.contentMain.urlInput.setSelection(0)

                    liveUrlInputText =
                        translateUrl

                    binding.contentMain.webview.loadUrl(
                        translateUrl,
                        mapOf(
                            "Cache-Control" to "no-cache",
                            "Pragma" to "no-cache"
                        )
                    )

                } catch (t: Throwable) {

                    Log.e(
                        "TRANSLATE_PAGE",
                        "load failed",
                        t
                    )

                    Toast.makeText(
                        this,
                        "Translate failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()

    } catch (t: Throwable) {

        Log.e(
            "TRANSLATE_PAGE",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Translate failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}


// =====================================
// ADD CURRENT PAGE TO HOME SCREEN
// =====================================

private fun addCurrentPageToHomeScreen() {

    try {

        val url =
            getCurrentPageUrl()

        if (
            url.isBlank() ||
            url.equals(
                "about:blank",
                true
            )
        ) {

            Toast.makeText(
                this,
                "No page to add",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pageTitle =
            try {

                binding.contentMain.webview.title
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: Uri.parse(url).host
                    ?: "GEL Page"

            } catch (_: Throwable) {

                "GEL Page"
            }

        val launchIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                action =
                    Intent.ACTION_VIEW

                data =
                    Uri.parse(url)

                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val shortcutManager =
                getSystemService(
                    android.content.pm.ShortcutManager::class.java
                )

            if (
                shortcutManager == null ||
                !shortcutManager.isRequestPinShortcutSupported
            ) {

                Toast.makeText(
                    this,
                    "Home screen shortcut not supported",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val shortcutId =
                "gel_page_" +
                    kotlin.math.abs(
                        url.hashCode()
                    )

            val shortcut =
                android.content.pm.ShortcutInfo.Builder(
                    this,
                    shortcutId
                )
                    .setShortLabel(
                        pageTitle.take(
                            20
                        )
                    )
                    .setLongLabel(
                        pageTitle.take(
                            40
                        )
                    )
                    .setIcon(
                        android.graphics.drawable.Icon.createWithResource(
                            this,
                            android.R.drawable.ic_menu_view
                        )
                    )
                    .setIntent(
                        launchIntent
                    )
                    .build()

            val callbackIntent =
                shortcutManager.createShortcutResultIntent(
                    shortcut
                )

            val flags =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_MUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }

            val successCallback =
                android.app.PendingIntent.getBroadcast(
                    this,
                    0,
                    callbackIntent,
                    flags
                )

            shortcutManager.requestPinShortcut(
                shortcut,
                successCallback.intentSender
            )

            Toast.makeText(
                this,
                "Add to Home Screen requested",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            @Suppress("DEPRECATION")
            val shortcutIntent =
                Intent(
                    "com.android.launcher.action.INSTALL_SHORTCUT"
                ).apply {

                    putExtra(
                        Intent.EXTRA_SHORTCUT_INTENT,
                        launchIntent
                    )

                    putExtra(
                        Intent.EXTRA_SHORTCUT_NAME,
                        pageTitle.take(
                            40
                        )
                    )

                    putExtra(
                        Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(
                            this@MainActivity,
                            android.R.drawable.ic_menu_view
                        )
                    )

                    putExtra(
                        "duplicate",
                        false
                    )
                }

            sendBroadcast(
                shortcutIntent
            )

            Toast.makeText(
                this,
                "Home screen shortcut requested",
                Toast.LENGTH_SHORT
            ).show()
        }

    } catch (t: Throwable) {

        Log.e(
            "ADD_HOME_SHORTCUT",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Add to Home Screen failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// SHARE / COPY PAGE URL
// =====================================

private fun shareCurrentPage() {

    val url =
        getCurrentPageUrl()

    if (url.isBlank()) {

        Toast.makeText(
            this,
            "No page URL",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    shareText(
        "Share Page",
        url
    )
}

private fun copyCurrentPageUrl() {

    val url =
        getCurrentPageUrl()

    if (url.isBlank()) {

        Toast.makeText(
            this,
            "No page URL",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    copyTextToClipboard(
        "page_url",
        url,
        "Page URL copied"
    )
}

// =====================================
// FIND IN PAGE
// =====================================

private fun showFindInPageDialog() {

    try {

        val input =
            android.widget.EditText(this).apply {

                hint =
                    "Find in page"

                setSingleLine(
                    true
                )
            }

        binding.contentMain.webview.setFindListener { _, numberOfMatches, isDoneCounting ->

            if (isDoneCounting) {

                Toast.makeText(
                    this,
                    "Matches: $numberOfMatches",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Find in Page")
            .setView(input)
            .setPositiveButton(
                "FIND"
            ) { _, _ ->

                try {

                    val query =
                        input.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                    if (query.isBlank()) {
                        return@setPositiveButton
                    }

                    binding.contentMain.webview.findAllAsync(
                        query
                    )

                } catch (_: Throwable) {}
            }
            .setNeutralButton(
                "NEXT"
            ) { _, _ ->

                try {

                    binding.contentMain.webview.findNext(
                        true
                    )

                } catch (_: Throwable) {}
            }
            .setNegativeButton(
                "CLEAR"
            ) { _, _ ->

                try {

                    binding.contentMain.webview.clearMatches()

                } catch (_: Throwable) {}
            }
            .show()

    } catch (t: Throwable) {

        Log.e(
            "FIND_IN_PAGE",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Find failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// OPEN CURRENT PAGE IN EXTERNAL BROWSER
// =====================================

private fun openCurrentPageInExternalBrowser() {

    try {

        val url =
            getCurrentPageUrl()

        if (url.isBlank()) {

            Toast.makeText(
                this,
                "No page URL",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            ).apply {

                addCategory(
                    Intent.CATEGORY_BROWSABLE
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Open With Browser"
            )
        )

    } catch (t: Throwable) {

        Log.e(
            "OPEN_EXTERNAL_BROWSER",
            "failed",
            t
        )

        Toast.makeText(
            this,
            "Cannot open browser",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// =====================================
// RELOAD / STOP / USER AGENT MODES
// =====================================

private fun reloadCurrentPage() {

    try {

        binding.contentMain.webview.reload()

    } catch (_: Throwable) {}
}

private fun stopCurrentPageLoading() {

    try {

        binding.contentMain.webview.stopLoading()

        Toast.makeText(
            this,
            "Loading stopped",
            Toast.LENGTH_SHORT
        ).show()

    } catch (_: Throwable) {}
}

private fun setWebViewUserAgentAndReload(
    desktop: Boolean
) {

    try {

        binding.contentMain.webview.settings.userAgentString =
            if (desktop) {
                desktopUserAgent
            } else {
                systemWebViewUserAgent
            }

        popupWebView?.settings?.userAgentString =
            binding.contentMain.webview.settings.userAgentString

        Toast.makeText(
            this,
            if (desktop) {
                "Desktop mode"
            } else {
                "Mobile mode"
            },
            Toast.LENGTH_SHORT
        ).show()

        binding.contentMain.webview.reload()

    } catch (t: Throwable) {

        Log.e(
            "USER_AGENT_MODE",
            "failed",
            t
        )
    }
}

// =====================================
// CLEAR BROWSER DATA
// =====================================

private fun clearBrowserData() {

    try {

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Browser Data")
            .setMessage("Clear WebView cache, cookies, form data and app browser history?")
            .setPositiveButton(
                "CLEAR"
            ) { _, _ ->

                try {

                    binding.contentMain.webview.clearCache(true)
                    binding.contentMain.webview.clearFormData()
                    binding.contentMain.webview.clearHistory()

                    popupWebView?.clearCache(true)
                    popupWebView?.clearFormData()
                    popupWebView?.clearHistory()

                    try {

                        CookieManager.getInstance()
                            .removeAllCookies(null)

                        CookieManager.getInstance()
                            .flush()

                    } catch (_: Throwable) {}

                    try {

                        android.webkit.WebStorage.getInstance()
                            .deleteAllData()

                    } catch (_: Throwable) {}

                    browserHistory.clear()
                    persistBrowserHistory()

                    Toast.makeText(
                        this,
                        "Browser data cleared",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (t: Throwable) {

                    Log.e(
                        "CLEAR_BROWSER_DATA",
                        "failed",
                        t
                    )

                    Toast.makeText(
                        this,
                        "Clear failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()

    } catch (_: Throwable) {}
}

// =====================================  
// MENU  
// =====================================

override fun onCreateOptionsMenu(
    menu: Menu
): Boolean {

    menuInflater.inflate(
        R.menu.menu_main,
        menu
    )

    try {

        // =====================================
        // TABS / HISTORY / BOOKMARKS
        // =====================================

        menu.add(Menu.NONE, menuOpenNewTabId, Menu.NONE, "Open New Tab")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuTabsId, Menu.NONE, "Tabs")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuHistoryId, Menu.NONE, "History")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuBookmarksId, Menu.NONE, "Bookmarks")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuAddBookmarkId, Menu.NONE, "Add Bookmark")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuAddToHomeScreenId, Menu.NONE, "Add to Home Screen")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        // =====================================
        // PAGE / URL TOOLS
        // =====================================

        menu.add(Menu.NONE, menuSharePageId, Menu.NONE, "Share Page")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuCopyPageUrlId, Menu.NONE, "Copy Page URL")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuFindInPageId, Menu.NONE, "Find in Page")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuTranslatePageId, Menu.NONE, "Translate Page")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuReloadId, Menu.NONE, "Reload")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            
        menu.add(Menu.NONE, menuGoForwardId, Menu.NONE, "Go Forward")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuStopLoadingId, Menu.NONE, "Stop Loading")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuOpenChromeId, Menu.NONE, "Open in Browser")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        // =====================================
        // STREAM / ANALYZER TOOLS
        // =====================================

        menu.add(Menu.NONE, menuSavedChannelsId, Menu.NONE, "Saved Channels")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuDetectedStreamsId, Menu.NONE, "Detected Streams")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuOpenPlayerId, Menu.NONE, "Open Player")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuExportM3uId, Menu.NONE, "Export M3U")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuScanChannelCandidatesId, Menu.NONE, "Scan Channel Candidates")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuAutoScanChannelsId, Menu.NONE, "Auto Scan Channels")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            

        // =====================================
        // VIEW / DATA
        // =====================================

        menu.add(Menu.NONE, menuDesktopModeId, Menu.NONE, "Desktop Mode")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuMobileModeId, Menu.NONE, "Mobile Mode")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, menuClearBrowserDataId, Menu.NONE, "Clear Browser Data/cache")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

    } catch (_: Throwable) {}

    return true
}
    
override fun onOptionsItemSelected(
    item: MenuItem
): Boolean {

    return when (item.itemId) {

        menuOpenNewTabId -> {
            openNewBrowserTab()
            true
        }

        menuTabsId -> {
            showBrowserTabsDialog()
            true
        }

        menuHistoryId -> {
            showBrowserHistoryDialog()
            true
        }

        menuBookmarksId -> {
            showBrowserBookmarksDialog()
            true
        }

        menuAddBookmarkId -> {
            addCurrentPageBookmark()
            true
        }

        menuAddToHomeScreenId -> {
            addCurrentPageToHomeScreen()
            true
        }

        menuScanChannelCandidatesId -> {
            scanChannelCandidatesOnly()
            true
        }

        menuAutoScanChannelsId -> {
            startAutoScanChannels()
            true
        }
menuSavedChannelsId -> {
            showSavedChannelsDialog()
            true
        }

        menuDetectedStreamsId -> {
            showDetectedStreamsDialog()
            true
        }

        menuOpenPlayerId -> {
            binding.contentMain.openPlayer.performClick()
            true
        }

        menuExportM3uId -> {
            binding.contentMain.exportM3u.performClick()
            true
        }

        menuSharePageId -> {
            shareCurrentPage()
            true
        }

        menuCopyPageUrlId -> {
            copyCurrentPageUrl()
            true
        }

        menuFindInPageId -> {
            showFindInPageDialog()
            true
        }

        menuTranslatePageId -> {
            translateCurrentPage()
            true
        }

        menuReloadId -> {
            reloadCurrentPage()
            true
        }

        menuGoForwardId -> {

            val activeWebView =
                popupWebView
                    ?: binding.contentMain.webview

            if (activeWebView.canGoForward()) {

                activeWebView.goForward()

            } else {

                Toast.makeText(
                    this,
                    "No forward page",
                    Toast.LENGTH_SHORT
                ).show()
            }

            true
        }

        menuStopLoadingId -> {
            stopCurrentPageLoading()
            true
        }

        menuOpenChromeId -> {
            openCurrentPageInExternalBrowser()
            true
        }

        menuDesktopModeId -> {
            setWebViewUserAgentAndReload(true)
            true
        }

        menuMobileModeId -> {
            setWebViewUserAgentAndReload(false)
            true
        }

        menuClearBrowserDataId -> {
            clearBrowserData()
            true
        }

        R.id.action_settings ->
            true

        else ->
            super.onOptionsItemSelected(item)
    }
}

// =====================================  
// TEST REQUESTS  
// =====================================  

fun fetchPosts(  
    view: View  
) {  

    val request =  
        Request.Builder()  
            .addHeader(  
                "Accept-Encoding",  
                "identity"  
            )  
            .addHeader(  
                "Content-type",  
                "application/json; charset=utf-8"  
            )  
            .url(  
                "https://reqres.in/api/users?page=1&per_page=12"  
            )  
            .build()  

    executeRequest(request)  
}  

fun fetchPostsFail(  
    view: View  
) {  

    val request =  
        Request.Builder()  
            .addHeader(  
                "Accept-Encoding",  
                "identity"  
            )  
            .addHeader(  
                "Content-type",  
                "application/json; charset=utf-8"  
            )  
            .url(  
                "https://reqres.in/apii/use?page=1&per_page=12"  
            )  
            .build()  

    executeRequest(request)  
}  

fun createPost(  
    view: View  
) {  

    val request =  
        Request.Builder()  
            .addHeader(  
                "Accept-Encoding",  
                "identity"  
            )  
            .addHeader(  
                "Content-type",  
                "application/json; charset=utf-8"  
            )  
            .url(  
                "https://reqres.in/api/users"  
            )  
            .post(  
                requestBody.toString()  
                    .toRequestBody(  
                        "application/json"  
                            .toMediaType()  
                    )  
            )  
            .build()  

    executeRequest(request)  
}  

// =====================================  
// EXECUTE REQUEST  
// =====================================  

private fun executeRequest(  
request: Request

) {

okHttpClient  
    .newCall(request)  
    .enqueue(  
        object : Callback {  

            override fun onFailure(  
                call: Call,  
                e: IOException  
            ) {  

                Log.e(  
                    "MAIN",  
                    "failure ${e.message}",  
                    e  
                )  
            }  

            override fun onResponse(  
                call: Call,  
                response: Response  
            ) {  

                try {  

                    if (
    response.code !in 200..399
) {
    return
}

val text =  
                        response.body  
                            ?.string()  
                            .orEmpty()  

                    runOnUiThread {

    if (
        !isFinishing &&
        !isDestroyed
    ) {

        binding
            .contentMain
            .result
            .setText(text)
    }
}

} catch (t: Throwable) {  

                    Log.e(  
                        "MAIN",  
                        "response parse error",  
                        t  
                    )  

                } finally {  

                    response.close()  
                }  
            }  
        }  
    )

}

// =====================================
// STREAM PRIORITY SCORE
// =====================================

private fun calculateStreamScore(
    url: String
): Int {

    val lower =
        url.lowercase()

    var score = 0
    
// =====================================
// JWPLAYER / HOSTED VOD DETECTION
// =====================================

val isJwPlayerVod =
    lower.contains("jwpsrv.com") ||
        lower.contains("jwplayer") ||
        (
            lower.contains("/media/") &&
                lower.contains("/versions/")
        )

if (isJwPlayerVod) {

    score -=
        1200
}

    // =====================================
    // MASTER PLAYLIST
    // =====================================

    if (lower.contains("master.m3u8")) {
        score += 100
    }

    // =====================================
    // PLAYLIST
    // =====================================

    if (lower.contains("playlist.m3u8")) {
        score += 80
    }

    // =====================================
    // CHUNKLIST
    // =====================================

    if (lower.contains("chunklist")) {
        score += 60
    }

    // =====================================
    // INDEX
    // =====================================

    if (
        lower.contains("index.m3u8") ||
        lower.contains("index-v")
    ) {
        score += 50
    }

    // =====================================
    // LIVE
    // =====================================

    if (
    !isJwPlayerVod &&
    (
        lower.contains("live") ||
            lower.contains("broadcast")
    )
) {
    score += 40
}

    // =====================================
    // HLS
    // =====================================

    if (lower.contains(".m3u8")) {
        score += 30
    }

    // =====================================
    // DASH
    // =====================================

    if (
        lower.contains(".mpd") ||
        lower.contains("manifest")
    ) {
        score += 20
    }

    // =====================================
    // VIDEO SEGMENTS
    // =====================================

    if (
        lower.contains(".ts") ||
        lower.contains(".m4s")
    ) {
        score -= 20
    }

    // =====================================
    // IMAGES / ADS
    // =====================================

    if (
        lower.contains(".jpg") ||
        lower.contains(".png") ||
        lower.contains("facebook.com/tr") ||
        lower.contains("/tr/?") ||
        lower.contains("translate.goog") ||
        lower.contains("translate.google") ||
        lower.contains("google.com/translate") ||
        lower.contains("doubleclick") ||
        lower.contains("googleads") ||
        lower.contains("googlesyndication") ||
        lower.contains("analytics") ||
        lower.contains("stats")
    ) {
        score -= 100
    }

    // =====================================
    // YOUTUBE DIRECT MEDIA
    // =====================================

// =====================================
// YOUTUBE LIVE DASH
// =====================================

if (

    lower.contains("googlevideo.com") &&
    lower.contains("videoplayback") &&
    lower.contains("yt_live_broadcast")

) {

    score += 1500
}

// =====================================
// YOUTUBE ITAG QUALITY
// =====================================

if (lower.contains("itag=137")) {
    score += 1000
}

if (lower.contains("itag=136")) {
    score += 800
}

if (lower.contains("itag=135")) {
    score += 600
}

if (lower.contains("itag=140")) {
    score += 100
}

// =====================================
// DASH AUDIO / VIDEO
// =====================================

if (

    lower.contains("mime=video") ||
    lower.contains("mime=audio")

) {

    score += 300
}

// =====================================
// LIVE FLAG
// =====================================

if (
    lower.contains("live=1")
) {
    score += 500
}

    // =====================================
    // LIVE SIGNALS
    // =====================================

    if (
    !isJwPlayerVod &&
    (
        lower.contains("/live/") ||
            lower.contains("live.m3u8") ||
            lower.contains("playlist_live") ||
            lower.contains("is_live") ||
            lower.contains("livestream") ||
            lower.contains("live-stream") ||
            lower.contains("broadcast") ||
            lower.contains("channel") ||
            lower.contains("linear")
    )
) {
    score += 300
}

    // =====================================
    // VOD PENALTY
    // =====================================

    if (
        lower.contains("/vod/") ||
        lower.contains("on-demand") ||
        lower.contains("archive") ||
        lower.contains("movie") ||
        lower.contains("series") ||
        lower.contains("episode")
    ) {
        score -= 200
    }

// =====================================
// ROOT LIVE MANIFEST BOOST
// =====================================

if (
    lower.contains("manifest.ism/live.m3u8") &&
    !lower.contains("jwpsrv.com") &&
    !lower.contains("/media/") &&
    !lower.contains("/versions/")
) {
    score += 1000
}

// =====================================
// CHILD PLAYLIST PENALTY
// =====================================

if (
    lower.contains("chunklist") ||
    lower.contains("index-v") ||
    lower.contains("index-a") ||
    lower.contains("audio_") ||
    lower.contains("-audio") ||
    lower.contains(".ts")
) {
    score -= 150
}

// =====================================
// LIVE VALIDATION BONUS
// =====================================

if (

    lower.contains(
        "source=yt_live_broadcast"
    )

) {

    score += 1000
}

if (

    lower.contains(
        "live=1"
    )

) {

    score += 500
}

if (

    lower.contains(
        "mime=video"
    )

) {

    score += 300
}

if (

    lower.contains(
        "mime=audio"
    )

) {

    score += 150
}

if (

    lower.contains(
        "noclen=1"
    )

) {

    score += 200
}

if (

    lower.contains(
        "gir=yes"
    )

) {

    score += 100
}

if (

    lower.contains(
        "googlevideo.com"
    )

) {

    score += 400
}

if (

    lower.contains(
        "expire="
    )

) {

    score += 100
}

// =====================================
// EXPIRE CHECK
// =====================================

try {

    val uri =
        Uri.parse(url)

    val expire =
        uri.getQueryParameter("expire")
            ?.toLongOrNull()
            ?: 0L

    if (expire > 0L) {

        val now =
            System.currentTimeMillis() / 1000L

        val remain =
            expire - now

        // =============================
        // DEAD STREAM
        // =============================

        if (remain <= 0L) {

            score -= 5000

            if (
                liveLocked &&
                url == bestLiveUrl
            ) {

                resetLiveDetection()

                Log.e(
                    "LIVE_UNLOCK",
                    "Expired stream"
                )
            }
        }

        // =============================
        // FRESH STREAM
        // =============================

        else {

            score += 200
        }
    }

} catch (_: Throwable) {}

return score
}

// =====================================
// LIVE RECOVERY
// =====================================

private fun resetLiveDetection() {

    try {

        liveLocked = false

        lockedStreamId = ""

        bestLiveUrl = ""

        bestLiveScore = 0

        bestVideoItag = 0

        bestAudioItag = 0

        dashVideoMap.clear()

        dashAudioMap.clear()

        Log.e(
            "LIVE_RESET",
            "Recovery started"
        )

    } catch (_: Throwable) {}
}

// =====================================
// WEBVIEW NETWORK STREAM OBSERVER
// =====================================

private fun observeWebViewRequestUrl(
    rawUrl: String?
) {

    if (rawUrl.isNullOrBlank()) {
        return
    }

    try {

        val url =
            rawUrl.trim()

        val lower =
            url.lowercase()

        val looksInteresting =
            lower.contains(".m3u8") ||
            lower.contains("m3u8") ||
            lower.contains("hlsmanifesturl") ||
            lower.contains("manifest/hls") ||
            lower.contains("hls_playlist") ||
            lower.contains("/api/manifest/hls") ||
            lower.contains("googlevideo.com") ||
            lower.contains("youtube.com/embed") ||
            lower.contains("youtube.com/watch") ||
            lower.contains("euronews.com/api/live") ||
            lower.contains("euronews.com/api/geoblocking")

        if (!looksInteresting) {
            return
        }

        Log.e(
            "WEBVIEW_STREAM_REQ",
            url
        )

        detectAndSaveUrl(
            url
        )

    } catch (_: Throwable) {}
}

}
