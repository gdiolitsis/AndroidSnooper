package com.github.jainsahab
 
import androidx.activity.addCallback
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManage
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
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
            ) ?: "[]"

        val array =
            org.json.JSONArray(raw)

        for (i in 0 until array.length()) {

            try {

                val obj =
                    array.getJSONObject(i)

                val channel =
                    SavedChannel(
                        name = obj.optString("name"),
                        url = obj.optString("url"),
                        logo = obj.optString("logo"),
                        group = obj.optString("group")
                    )

                if (
                    channel.url.isNotBlank() &&
                    isExportableStream(channel.url)
                ) {

                    savedChannels.add(
                        channel
                    )
                }

            } catch (_: Throwable) {}
        }

    } catch (_: Throwable) {}
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

        getSharedPreferences(
            savedChannelsPrefsName,
            MODE_PRIVATE
        )
            .edit()
            .putString(
                savedChannelsKey,
                array.toString()
            )
            .apply()

    } catch (_: Throwable) {}
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

    } catch (_: Throwable) {}
}

// =====================================
// SHOW SAVED CHANNELS DIALOG
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

        savedChannels
            .toList()
            .forEach { channel ->

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

                val info =
                    TextView(this).apply {

                        text =
                            """
${channel.name}

${channel.url}
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

                            try {

                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW
                                    ).apply {

                                        data =
                                            Uri.parse(
                                                channel.url
                                            )

                                        addCategory(
                                            Intent.CATEGORY_BROWSABLE
                                        )
                                    }

                                startActivity(
                                    Intent.createChooser(
                                        intent,
                                        "Open Channel"
                                    )
                                )

                            } catch (_: Throwable) {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Cannot open channel",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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

                                savedChannels.removeAll { saved ->

                                    saved.url == channel.url
                                }

                                persistSavedChannels()

                                Toast.makeText(
                                    this@MainActivity,
                                    "Channel deleted",
                                    Toast.LENGTH_SHORT
                                ).show()

                                showSavedChannelsDialog()

                            } catch (_: Throwable) {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Delete failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                buttonRow.addView(openButton)
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
            .setTitle("Saved Channels")
            .setView(scrollView)
            .setNegativeButton(
                "CLOSE",
                null
            )
            .show()

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

loadSavedChannels()

window.setSoftInputMode(
    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
)

binding.contentMain.result.isVerticalScrollBarEnabled = true

binding.contentMain.result.movementMethod =
android.text.method.ScrollingMovementMethod()

setSupportActionBar(  
        binding.toolbar  
    )

onBackPressedDispatcher.addCallback(this) {

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
// =====================================

binding.contentMain.webview.settings.apply {

    javaScriptEnabled = true

    domStorageEnabled = true

    databaseEnabled = true

    allowFileAccess = true

    allowContentAccess = true

    mediaPlaybackRequiresUserGesture = false

    loadsImagesAutomatically = true

    useWideViewPort = true

    loadWithOverviewMode = false

    setSupportZoom(true)

    builtInZoomControls = true

    displayZoomControls = false

    javaScriptCanOpenWindowsAutomatically = true

    setSupportMultipleWindows(false)

    mixedContentMode =
        WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

    userAgentString =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/137.0.0.0 Safari/537.36"

    cacheMode =
        WebSettings.LOAD_DEFAULT
}

// =====================================
// WEBVIEW SCROLL FIX
// =====================================

binding.contentMain.webview.isVerticalScrollBarEnabled =
    true

binding.contentMain.webview.isScrollbarFadingEnabled =
    false

binding.contentMain.webview.overScrollMode =
    View.OVER_SCROLL_ALWAYS

binding.contentMain.webview.setOnTouchListener { v, _ ->

    v.parent?.requestDisallowInterceptTouchEvent(
        true
    )

    false
}

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
// =====================================

binding.contentMain.webview.webViewClient =
    object : WebViewClient() {
    
// =====================================
// UPDATE URL BAR ON PAGE STARTED
// =====================================

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

            binding.contentMain.urlInput.setText(
                url
            )

            binding.contentMain.urlInput.setSelection(
                binding.contentMain.urlInput.text.length
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

                if (url.isNotBlank()) {

                    handleInterceptedMediaUrl(
                        url,
                        request
                    )

                    view?.loadUrl(url)

                    true

                } else {

                    false
                }

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

        if (url.isNotBlank()) {

            handleInterceptedMediaUrl(
                url,
                request
            )
        }

    } catch (_: Throwable) {}

    val response =
        try {

            super.shouldInterceptRequest(
                view,
                request
            )

        } catch (_: Throwable) {

            null
        }

    // =====================================
    // RESPONSE BODY SNIFFER
    // =====================================

    try {

        val mime =
            response
                ?.mimeType
                ?.lowercase()
                .orEmpty()

        val headers =
            response
                ?.responseHeaders
                ?: emptyMap()

        headers.forEach { entry ->

            try {

                val key =
                    entry.key.lowercase()

                val value =
                    entry.value.lowercase()

                if (
                    key.contains("content-type") ||
                    key.contains("server") ||
                    key.contains("cache") ||
                    key.contains("cdn") ||
                    key.contains("akamai") ||
                    key.contains("cloudfront") ||
                    key.contains("expires")
                ) {

                    Log.e(
                        "STREAM_HEADER",
                        "${entry.key} -> ${entry.value}"
                    )
                }

                if (
                    value.contains("akamai") ||
                    value.contains("cloudfront") ||
                    value.contains("fastly") ||
                    value.contains("edge")
                ) {

                    Log.e(
                        "LIVE_CDN",
                        "${entry.key} -> ${entry.value}"
                    )
                }

            } catch (_: Throwable) {}
        }

        if (
            mime.contains("json") ||
            mime.contains("javascript") ||
            mime.contains("xml") ||
            mime.contains("text") ||
            mime.contains("mpegurl") ||
            mime.contains("dash")
        ) {

            val rawBytes =
                response
                    ?.data
                    ?.readBytes()

            val body =
                rawBytes
                    ?.toString(Charsets.UTF_8)
                    .orEmpty()
                    
// =====================================
// STRONG / BRUTAL HLS EXTRACTION
// YouTube / Euronews / escaped HTML-JSON
// =====================================

try {

    val foundUrls =
        linkedSetOf<String>()

    foundUrls.addAll(
        extractM3u8UrlsFromText(
            body
        )
    )

    foundUrls.addAll(
        extractBrutalHlsUrlsFromText(
            body
        )
    )

    foundUrls.forEach { found ->

        try {

            markStreamSource(
                found,
                "BRUTAL_HLS_BODY"
            )

            detectAndSaveUrl(
                found
            )

            Log.e(
                "BRUTAL_HLS_BODY",
                found
            )

        } catch (_: Throwable) {}
    }

} catch (_: Throwable) {}

            // =====================================
            // BODY MEDIA EXTRACTION
            // =====================================

            val regex =
                "(https?:\\\\/\\\\/[^\"'\\\\s]+?(m3u8|mpd|mp4|m4s|ts)(\\\\?[^\"'\\\\s]*)?)"
                    .toRegex(
                        RegexOption.IGNORE_CASE
                    )

            regex.findAll(body)
                .forEach { match ->

                    try {

                        val found =
                            match.value
                                .replace("\\\\/", "/")
                                .replace("\\/", "/")
                                .trim()

                        if (found.isNotBlank()) {

                            markStreamSource(
                                found,
                                "BODY"
                            )

                            detectAndSaveUrl(
                                found
                            )

                            Log.e(
                                "BODY_MEDIA",
                                found
                            )
                        }

                    } catch (_: Throwable) {}
                }

            // =====================================
            // M3U8 BODY PARSER
            // =====================================

            if (
                url.contains(".m3u8", true) &&
                body.contains("#EXTM3U", true)
            ) {

                Log.e(
                    "M3U8_BODY",
                    body
                )

                val lines =
                    body.lines()

// =====================================
// LIVE / VOD HLS DETECTION
// =====================================

val hasEndList =
    body.contains(
        "#EXT-X-ENDLIST",
        true
    )

val hasSegments =
    body.contains(
        "#EXTINF",
        true
    ) ||
        body.contains(
            "#EXT-X-TARGETDURATION",
            true
        )

val isLiveStream =
    !hasEndList &&
        hasSegments

// =====================================
// HLS BODY VERDICT
// =====================================

val hlsVerdict =
    classifyHlsBody(
        url,
        body
    )

hlsVerdicts[url] =
    hlsVerdict

streamValidation[url] =
    hlsVerdict

Log.e(
    "HLS_VERDICT",
    "$hlsVerdict -> $url"
)

when (hlsVerdict) {

    "HLS_LIVE",
    "HLS_LIVE_CANDIDATE" -> {

        Log.e(
            "LIVE_HLS_CONFIRMED",
            url
        )

        bestLiveUrl =
            url

        bestLiveScore +=
            1000
    }

    "HLS_MASTER" -> {

        Log.e(
            "HLS_MASTER_CONFIRMED",
            url
        )
    }

    "HLS_VOD" -> {

        Log.e(
            "VOD_HLS_CONFIRMED",
            url
        )
    }

    else -> {

        Log.e(
            "HLS_UNKNOWN",
            url
        )
    }
}

                lines.forEachIndexed { index, line ->

                    try {

                        val current =
                            line.trim()

                        if (
                            current.contains(
                                "#EXT-X-STREAM-INF",
                                true
                            )
                        ) {

                            val next =
                                if (index + 1 < lines.size) {
                                    lines[index + 1].trim()
                                } else {
                                    ""
                                }

                            if (
                                next.endsWith(".m3u8") ||
                                next.contains(".m3u8?")
                            ) {

                                val absolute =
                                    if (next.startsWith("http")) {
                                        next
                                    } else {
                                        val base =
                                            url.substringBeforeLast("/")

                                        "$base/$next"
                                    }

                                markStreamSource(
                                    absolute,
                                    "HLS_VARIANT"
                                )

                                detectAndSaveUrl(
                                    absolute
                                )

                                Log.e(
                                    "HLS_VARIANT",
                                    absolute
                                )
                            }
                        }

                    } catch (_: Throwable) {}
                }
            }

            if (rawBytes != null) {

                return WebResourceResponse(
                    response.mimeType,
                    response.encoding,
                    response.statusCode,
                    response.reasonPhrase,
                    response.responseHeaders,
                    rawBytes.inputStream()
                )
            }
        }

    } catch (_: Throwable) {}

    return response
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

        if (
            !url.isNullOrBlank()
        ) {

            // =====================================
            // UPDATE URL BAR
            // =====================================

            binding.contentMain.urlInput.setText(
                url
            )

            binding.contentMain.urlInput.setSelection(
                binding.contentMain.urlInput.text.length
            )

            // =====================================
            // DETECT PAGE URL
            // =====================================

            handleInterceptedMediaUrl(
                url,
                null
            )

            detectAndSaveUrl(
                url
            )

            // =====================================
            // ENABLE TEXT SELECTION
            // =====================================

            enablePageTextSelection(
                view
            )

// =====================================
// DEEP MEDIA SCAN
// =====================================

runDeepMediaScan(
    view
)

// =====================================
// DELAYED LIGHT RESCAN FOR JS PLAYERS
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

                runDeepMediaScan(
                    view
                )
            }

        } catch (_: Throwable) {}

    },
    2500
)
            
binding.contentMain.webview.postDelayed(
    {
        try {

            runDeepMediaScan(
                view
            )

        } catch (_: Throwable) {}
    },
    2500
)

binding.contentMain.webview.postDelayed(
    {
        try {

            runDeepMediaScan(
                view
            )

        } catch (_: Throwable) {}
    },
    6000
)

        }

    } catch (_: Throwable) {}
}

} // END WebViewClient

// =====================================
// WEB CHROME CLIENT (FULLSCREEN)
// =====================================

binding.contentMain.webview.webChromeClient =
    object : WebChromeClient() {

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {

            return try {

                val transport =
                    resultMsg?.obj
                        as? WebView.WebViewTransport

                transport?.webView =
                    binding.contentMain.webview

                resultMsg?.sendToTarget()

                true

            } catch (_: Throwable) {

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
// =====================================

binding.contentMain.openBrowser.setOnClickListener {

    detectedStreams.clear()

    detectedVideos.clear()

    detectedImages.clear()

    detectedAudio.clear()

    detectedMasterStreams.clear()

    detectedChannels.clear()

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

    binding.contentMain.result.text =
        ""

    // =====================================
    // RESET WEBVIEW LIGHT STATE
    // =====================================

    try {

        binding.contentMain.webview.stopLoading()

        binding.contentMain.webview.clearMatches()

        CookieManager
            .getInstance()
            .flush()

    } catch (_: Throwable) {}

    // =====================================
    // RESET WEBVIEW JS MEMORY
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

        window.__gelFetchHooked =
            false;

        window.__gelFetchResponseHook =
            false;

        window.__gelXHRResponseHook =
            false;

        window.__gelPerformanceObserverHooked =
            false;

        window.__gelMutationMediaHooked =
            false;

        window.__gelMediaAttributeHooked =
            false;

        window.__gelBlobHooked =
            false;

        window.__gelMSEHooked =
            false;

        window.__gelSourceBufferHooked =
            false;

        window.__gelWorkerHooked =
            false;

        window.__gelWebSocketHooked =
            false;

        console.log(
            "GEL_FULL_DETECTION_RESET"
        );

    } catch(e) {}

})();

            """.trimIndent(),
            null
        )

    } catch (_: Throwable) {}

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

    // =====================================
    // DESKTOP MODE
    // =====================================

    val desktopMode =
        true

    if (desktopMode) {

        binding.contentMain.webview.settings.userAgentString =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"

    } else {

        binding.contentMain.webview.settings.userAgentString =
            null
    }

    // =====================================
    // HIDE KEYBOARD
    // =====================================

    try {

        val imm =
            getSystemService(
                INPUT_METHOD_SERVICE
            ) as android.view.inputmethod.InputMethodManager

        imm.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )

    } catch (_: Throwable) {}

    // =====================================
    // REMOVE INPUT FOCUS
    // =====================================

    binding.contentMain.urlInput.clearFocus()

    // =====================================
    // LOAD PAGE
    // =====================================

    binding.contentMain.webview.loadUrl(
        finalUrl,
        mapOf(
            "Cache-Control" to "no-cache"
        )
    )
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
// =====================================

binding.contentMain.result.setOnTouchListener { _, event ->

    lastTouchX = event.x
    lastTouchY = event.y

    false
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

    if (selectedUrl.isBlank()) {
        return@setOnLongClickListener true
    }

    lastSelectedUrl =
        selectedUrl

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

    if (bestStreamUrl.isNotBlank()) {

        actions.add(
            "OPEN BEST STREAM"
        )

        actions.add(
            "COPY BEST STREAM"
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
            lastSelectedUrl
                .trim()

        if (finalUrl.isBlank()) {
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
                                Uri.parse(urlToOpen),
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

                    val intent =
                        Intent(
                            Intent.ACTION_VIEW
                        ).apply {

                            setDataAndType(
                                Uri.parse(bestStreamUrl),
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

                    val clipboard =
                        getSystemService(
                            CLIPBOARD_SERVICE
                        ) as ClipboardManager

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "best_stream",
                            bestStreamUrl
                        )
                    )

                    Toast.makeText(
                        this,
                        "Best stream copied",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (_: Throwable) {}
            }

            "OPEN YOUTUBE WATCH" -> {

                try {

                    val ytUrl =
                        youtubeWatchUrl
                            .trim()

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
                        youtubeWatchUrl
                            .trim()

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
$youtubeDashVideoUrl

AUDIO ITAG:
$youtubeDashAudioItag

AUDIO URL:
$youtubeDashAudioUrl
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

                } catch (_: Throwable) {}
            }

            "SHARE DASH PAIR" -> {

                try {

                    val dashPairText =
                        """
YOUTUBE DASH PAIR

VIDEO ITAG:
$youtubeDashVideoItag

VIDEO URL:
$youtubeDashVideoUrl

AUDIO ITAG:
$youtubeDashAudioItag

AUDIO URL:
$youtubeDashAudioUrl
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

                } catch (_: Throwable) {}
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
// HANDLE INTERCEPTED MEDIA URL
// =====================================

private fun handleInterceptedMediaUrl(
    url: String,
    request: WebResourceRequest?
) {

    try {

        val lower =
            url.lowercase()
            
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
        // NETWORK MEDIA DETECTION
        // =====================================

        if (
            lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains(".ts") ||
            lower.contains(".m4s") ||
            lower.contains(".mp4") ||
            lower.contains("manifest") ||
            lower.contains("playlist") ||
            lower.contains("chunklist") ||
            lower.contains("live") ||
            lower.contains("dash") ||
            lower.contains("hls") ||
            lower.contains("videoplayback")
        ) {

            Log.e(
                "NETWORK_MEDIA",
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
        // API / PLAYER / JSON DETECTION
        // =====================================

        if (
            lower.contains("api") ||
            lower.contains("player") ||
            lower.contains("media") ||
            lower.contains("stream") ||
            lower.contains("video") ||
            lower.contains("playback") ||
            lower.contains("config") ||
            lower.contains("playlist") ||
            lower.contains("manifest") ||
            lower.contains("token") ||
            lower.contains("license") ||
            lower.contains("session")
        ) {

            Log.e(
                "API_MEDIA_HINT",
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
        .querySelectorAll("video, audio, source, img, iframe")
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
                        .querySelectorAll("video, audio, source, img, script, a")
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
        // RAW PLAYER RESPONSE DEBUG
        // =====================================

        try {

            console.log(
                "GEL_RAW_PLAYER_RESPONSE:",
                txt.substring(
                    0,
                    5000
                )
            );

        } catch(e) {}

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

                                        console.log(
                                            "GEL_PLAYER_RESPONSE_FOUND"
                                        );

                                        try {

                                            console.log(
                                                "GEL_RAW_PLAYER_RESPONSE:",
                                                txt.substring(
                                                    0,
                                                    5000
                                                )
                                            );

                                        } catch(e) {}
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

        Log.e(  
            "INTENT_DEBUG",  
            "action=$action type=$type data=${intent?.dataString}"  
        )  

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

// =====================================
// URL FILTER — KEEP ALL PLAYABLE MEDIA
// =====================================

val filterLower =
    url.lowercase()

// =====================================
// PLAYABLE MEDIA CANDIDATES
// Do NOT cut these, even if low quality
// =====================================

val isPlayableMediaCandidate =
    filterLower.contains(".m3u8") ||
        filterLower.contains(".mpd") ||
        filterLower.contains(".mp4") ||
        filterLower.contains(".webm") ||
        filterLower.contains(".mkv") ||
        filterLower.contains(".ts") ||
        filterLower.contains(".m4s") ||
        filterLower.contains(".mp3") ||
        filterLower.contains(".m4a") ||
        filterLower.contains(".aac") ||
        filterLower.contains(".opus") ||
        filterLower.contains(".wav") ||
        filterLower.contains(".ogg") ||
        filterLower.contains(".flac") ||
        filterLower.contains("manifest/hls") ||
        filterLower.contains("hls_playlist") ||
        filterLower.contains("hlsmanifesturl") ||
        filterLower.contains("playlist") ||
        filterLower.contains("chunklist") ||
        filterLower.contains("live.m3u8") ||
        filterLower.contains("master.m3u8") ||
        filterLower.contains("videoplayback") ||
        filterLower.contains("googlevideo.com") ||
        filterLower.contains("youtube.com/watch") ||
        filterLower.contains("youtu.be/")

// =====================================
// IMAGE CANDIDATES — NOT NEEDED
// =====================================

val isImageCandidate =
    filterLower.contains(".jpg") ||
        filterLower.contains(".jpeg") ||
        filterLower.contains(".png") ||
        filterLower.contains(".webp") ||
        filterLower.contains(".gif") ||
        filterLower.contains(".svg") ||
        filterLower.contains(".ico")

if (
    isImageCandidate &&
    !isPlayableMediaCandidate
) {
    return
}

// =====================================
// GARBAGE / ADS / TRACKING
// Only cut if NOT playable media
// =====================================

if (
    !isPlayableMediaCandidate &&
    (
        filterLower.contains("doubleclick") ||
            filterLower.contains("googleads") ||
            filterLower.contains("analytics") ||
            filterLower.contains("/stats/") ||
            filterLower.contains("ptracking") ||
            filterLower.contains("api/stats") ||
            filterLower.contains("pagead") ||
            filterLower.contains("collect?") ||
            filterLower.contains("html-load.com") ||
            filterLower.contains("ad-delivery") ||
            filterLower.contains("moat") ||
            filterLower.contains("feed/iu1") ||
            filterLower.contains("favicon") ||
            filterLower.contains("logo") ||
            filterLower.contains("banner")
    )
) {
    return
}

Log.e(
    "MEDIA_DETECT",
    url
)

val cleanedUrl =
    url
        .replace("\\u0026", "&")
        .replace("\\/", "/")
        .trim()
        
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

    bestStreamScore =
        streamScore

    bestStreamUrl =
        cleanedUrl

    Log.e(
        "BEST_STREAM",
        "$bestStreamScore -> $bestStreamUrl"
    )
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
// LOW PRIORITY FILTER
// Do NOT cut playable audio/video/live streams
// =====================================

if (
    streamPriority < 20 &&
    !isPlayableMediaCandidate
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
// SEGMENT FILES
// Keep possible playable TS/M4S, ignore only obvious tiny chunks
// =====================================

if (
    isSegmentTs &&
    !lower.contains(".m3u8") &&
    !lower.contains(".mpd") &&
    !lower.contains("playlist") &&
    !lower.contains("manifest") &&
    !lower.contains("live")
) {

    Log.e(
        "SEGMENT_LOW_PRIORITY",
        cleanedUrl
    )

    // Do not return.
    // We keep it as evidence/playable candidate.
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
// =====================================

val extraPlayableLinks =
    buildString {

        if (youtubeWatchUrl.isNotBlank()) {

            append("▶️ YOUTUBE WATCH PLAYABLE")
            append("\n")
            append(youtubeWatchUrl)
            append("\n\n")
        }

        if (bestStreamUrl.isNotBlank()) {

            append("⭐ BEST STREAM")
            append("\n")
            append(bestStreamUrl)
            append("\n\n")
        }
    }

// =====================================
// YOUTUBE LIVE EXPORT NOTE
// =====================================

val youtubeLiveExportNote =
    if (
        isYoutubeLiveDash &&
        youtubeWatchUrl.isNotBlank() &&
        !extraPlayableLinks.contains(youtubeWatchUrl)
    ) {

        "\n▶️ PLAYABLE WATCH URL\n$youtubeWatchUrl"

    } else {

        ""
    }

// =====================================
// SAVE LAST URL
// =====================================

lastSelectedUrl =
    cleanedUrl

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

    val snapshot =
        StreamInfoSnapshot(
            url = cleanedUrl,
            badge = streamBadge,
            quality = streamQuality,
            cdn = cdnType,
            security = securityBadge,
            segment = segmentBadge,
            forensic = forensicNote,
            youtubeWatch = youtubeWatchUrl,
            dashVideo = youtubeDashVideoUrl,
            dashAudio = youtubeDashAudioUrl,
            dashVideoItag = youtubeDashVideoItag,
            dashAudioItag = youtubeDashAudioItag,
            bestStream = bestStreamUrl,
            bestLive = bestLiveUrl
        )

    streamInfoSnapshots[savedUrl] =
        snapshot

    streamInfoSnapshots[cleanedUrl] =
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

$cleanedUrl

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
// =====================================

private fun isExportableStream(
    url: String
): Boolean {

val lower =
    url.lowercase()
    
// =====================================
// FAKE YOUTUBE WATCH URL FILTER
// Blocks googlevideo temporary IDs converted to watch?v=...
// =====================================

if (
    (
        lower.contains("youtube.com/watch") ||
            lower.contains("youtu.be/")
    ) &&
    !isRealYouTubeWatchUrl(url)
) {
    return false
}

val validation =
    streamValidation[url]
        ?: ""

// =====================================
// VALIDATION ERROR FILTER
// Keep real playable media even if validator failed
// =====================================

val isDirectPlayableUrl =
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

if (
    validation.contains("ERROR") &&
    !isDirectPlayableUrl
) {
    return false
}

    // =====================================
    // DEAD
    // =====================================

    if (
        validation.contains("DEAD")
    ) {
        return false
    }

    // =====================================
    // IMAGES — NOT NEEDED
    // =====================================

    if (
        lower.contains(".jpg") ||
        lower.contains(".jpeg") ||
        lower.contains(".png") ||
        lower.contains(".webp") ||
        lower.contains(".gif") ||
        lower.contains(".bmp") ||
        lower.contains(".svg") ||
        lower.contains(".ico")
    ) {
        return false
    }

    // =====================================
    // ADS / TRACKERS — NOT PLAYABLE MEDIA
    // =====================================

    if (
        lower.contains("doubleclick") ||
        lower.contains("googleads") ||
        lower.contains("analytics") ||
        lower.contains("/stats/") ||
        lower.contains("ptracking") ||
        lower.contains("api/stats") ||
        lower.contains("pagead") ||
        lower.contains("collect?") ||
        lower.contains("html-load.com") ||
        lower.contains("ad-delivery") ||
        lower.contains("moat") ||
        lower.contains("feed/iu1") ||
        lower.contains("favicon") ||
        lower.contains("logo") ||
        lower.contains("banner")
    ) {
        return false
    }

    // =====================================
    // OBVIOUS FRAGMENTS / NON-STANDALONE CHUNKS
    // =====================================

    if (
        (
            lower.endsWith(".m4s") ||
            lower.contains(".m4s?")
        ) &&
        !lower.contains("googlevideo.com")
    ) {
        return false
    }

    if (
        (
            lower.endsWith(".ts") ||
            lower.contains(".ts?")
        ) &&
        (
            lower.contains("segment") ||
            lower.contains("segments") ||
            lower.contains("frag") ||
            lower.contains("fragments") ||
            lower.contains("sq=") ||
            lower.contains("range=")
        )
    ) {
        return false
    }

    // =====================================
    // LIVE STREAMS
    // =====================================

    if (
        lower.contains(".m3u8") ||
        lower.contains(".mpd") ||
        lower.contains("manifest/hls") ||
        lower.contains("hls_playlist") ||
        lower.contains("hlsmanifesturl") ||
        lower.contains("master.m3u8") ||
        lower.contains("live.m3u8") ||
        lower.contains("playlist.m3u8") ||
        lower.contains("chunklist.m3u8")
    ) {
        return true
    }

    // =====================================
    // STATIC VIDEOS
    // =====================================

    if (
        lower.contains(".mp4") ||
        lower.contains(".webm") ||
        lower.contains(".mkv") ||
        lower.contains(".mov") ||
        lower.contains(".avi") ||
        lower.contains(".3gp") ||
        lower.endsWith(".ts") ||
        lower.contains(".ts?")
    ) {
        return true
    }

    // =====================================
    // AUDIO / SONGS
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
// GOOGLEVIDEO PING / TRACKING — NOT PLAYABLE
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
    // YOUTUBE / GOOGLEVIDEO PLAYABLE EVIDENCE
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

                lower.contains(".m4s") ||
                lower.contains(".ts") ->
                    300

                lower.contains(".mp3") ||
                lower.contains(".aac") ->
                    250

                lower.contains(".jpg") ||
                lower.contains(".png") ||
                lower.contains(".webp") ->
                    50

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
                        // FORCE PLAYBACK / PLAYER WAKEUP
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

                const p =
                    v.play();

                if (p) {
                    p.catch(function(){});
                }

            } catch(e) {}
        });

    document
        .querySelectorAll(
            "button, .play, .play-button, .vjs-big-play-button"
        )
        .forEach(function(el) {

            try {

                el.click();

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
// MENU  
// =====================================

override fun onCreateOptionsMenu(  
    menu: Menu  
): Boolean {  

    menuInflater.inflate(  
        R.menu.menu_main,  
        menu  
    )  

    return true  
}  

override fun onOptionsItemSelected(  
    item: MenuItem  
): Boolean {  

    return when (item.itemId) {  

        R.id.action_settings ->  
            true  

        else ->  
            super.onOptionsItemSelected(  
                item  
            )  
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
        lower.contains("doubleclick") ||
        lower.contains("googleads") ||
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
