package com.github.jainsahab

import androidx.activity.addCallback
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
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
    linkedSetOf<String>()  

private val detectedVideos =  
    linkedSetOf<String>()  

private val detectedImages =  
    linkedSetOf<String>()  

private val detectedAudio =  
    linkedSetOf<String>()  
      
private val detectedMasterStreams =  
linkedSetOf<String>()  
  
private val detectedChannels =  
linkedMapOf<String, String>()  
  
private var lastSelectedUrl =  
""  
  
private val streamHeaders =  
linkedMapOf<String, MutableMap<String, String>>()  
  
private var monitorRunning =  
false  
  
private var autoRefreshEnabled =  
false  
      
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

private val okHttpClient:  
        OkHttpClient  

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

                    val body =  
                        response.body  
                            ?.string()  
                            .orEmpty()  

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

loadWithOverviewMode = true  

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
// WEBVIEW CLIENT
// =====================================

binding.contentMain.webview.webViewClient =
object : WebViewClient() {

override fun shouldOverrideUrlLoading(  
        view: WebView?,  
        request: WebResourceRequest?  
    ): Boolean {  

        val url =  
            request?.url.toString()  

        return try {  

            view?.loadUrl(url)  

            true  

        } catch (_: Throwable) {  

            false  
        }  
    }  

    override fun shouldInterceptRequest(  
        view: WebView?,  
        request: WebResourceRequest?  
    ): WebResourceResponse? {  

        val url =  
            request?.url.toString()  

        try {  

            val headers =  
                mutableMapOf<String, String>()  

            request?.requestHeaders  
                ?.forEach { (k, v) ->  

                    headers[k] = v  
                }  

            streamHeaders[url] =  
                headers  

        } catch (_: Throwable) {}  

        detectAndSaveUrl(url)  

        return super.shouldInterceptRequest(  
            view,  
            request  
        )  
    }  

    override fun onPageFinished(  
        view: WebView?,  
        url: String?  
    ) {  

        super.onPageFinished(  
            view,  
            url  
        )  

        val js = """

(function() {

let results = [];  

// =====================================  
// IMG  
// =====================================  

document  
    .querySelectorAll("img")  
    .forEach(function(el) {  

        if (el.src) {  
            results.push(el.src);  
        }  
    });  

// =====================================  
// VIDEO  
// =====================================  

document  
    .querySelectorAll("video")  
    .forEach(function(el) {  

        if (el.src) {  
            results.push(el.src);  
        }  

        if (el.currentSrc) {  
            results.push(el.currentSrc);  
        }  

        if (el.poster) {  
            results.push(el.poster);  
        }  
    });  

// =====================================  
// AUDIO  
// =====================================  

document  
    .querySelectorAll("audio")  
    .forEach(function(el) {  

        if (el.src) {  
            results.push(el.src);  
        }  
    });  

// =====================================  
// SOURCE  
// =====================================  

document  
    .querySelectorAll("source")  
    .forEach(function(el) {  

        if (el.src) {  
            results.push(el.src);  
        }  
    });

// =====================================
// IFRAME DETECTION
// =====================================

document
.querySelectorAll("iframe")
.forEach(function(el) {

try {  

        if (el.src) {  

            results.push(el.src);  
        }  

    } catch(e) {}  
});  

// =====================================  
// LINKS  
// =====================================  

document  
    .querySelectorAll("a")  
    .forEach(function(el) {  

        if (el.href) {  

            if (  
el.href.includes(".m3u8") ||  
el.href.includes(".mpd") ||  
el.href.includes(".mp4") ||  
el.href.includes("master.m3u8") ||  
el.href.includes("playlist.m3u8") ||  
el.href.includes("index.m3u8") ||  
el.href.includes("chunklist")

) {

results.push(el.href);

}
}
});

// =====================================  
// SCRIPT CONTENT  
// =====================================  

document  
    .querySelectorAll("script")  
    .forEach(function(el) {  

        const txt =  
            el.innerHTML;  

        const regex =

/(https?://[^"' ]+.(m3u8|mpd|mp4)(?[^"' ]*)?)/gi;

const found =  
            txt.match(regex);  

        if (found) {  

            found.forEach(function(x) {  

                results.push(x);  
            });  
        }  
    });  

// =====================================  
// JWPLAYER  
// =====================================  

if (window.jwplayer) {  

    try {  

        const players =  
            jwplayer().getPlaylist();  

        if (players) {  

            players.forEach(function(p) {  

                if (p.file) {  
                    results.push(p.file);  
                }  
            });  
        }  

    } catch(e) {}  
}

// =====================================
// AUTO PLAY TRIGGER
// =====================================

try {

document  
    .querySelectorAll(  
        "button, .play, .vjs-big-play-button, .jw-icon-playback, .ytp-large-play-button"  
    )  
    .forEach(function(el) {  

        try {  
            el.click();  
        } catch(e) {}  
    });

} catch(e) {}

// =====================================
// VIDEO AUTO PLAY
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
// REMOVE DUPLICATES  
// =====================================  

results =  
    [...new Set(results)];

// =====================================
// FETCH HOOK
// =====================================

if (!window.__gelFetchHooked) {

window.__gelFetchHooked = true;  

const originalFetch =  
    window.fetch;  

window.fetch =  
    function() {  

        try {  

            const url =  
                arguments[0];  

            if (typeof url === "string") {  

                results.push(url);  
            }  

        } catch(e) {}  

        return originalFetch.apply(  
            this,  
            arguments  
        );  
    };

}

// =====================================
// XHR HOOK
// =====================================

if (!window.__gelXHRHooked) {

window.__gelXHRHooked = true;  

const originalOpen =  
    XMLHttpRequest.prototype.open;  

XMLHttpRequest.prototype.open =  
    function(method, url) {  

        try {  

            if (url) {  
                results.push(url);  
            }  

        } catch(e) {}  

        return originalOpen.apply(  
            this,  
            arguments  
        );  
    };

}

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

if (
autoRefreshEnabled &&
!monitorRunning &&
(
url?.contains(".m3u8") == true ||
url?.contains(".mpd") == true
)
) {

monitorRunning = true  

startStreamMonitor()

}

}  
}

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

        customView = view  

        customViewCallback = callback  

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

        customView = null  

        binding.contentMain.webview.visibility =  
            View.VISIBLE  

        window.decorView.systemUiVisibility =  
            View.SYSTEM_UI_FLAG_VISIBLE  

        customViewCallback  
            ?.onCustomViewHidden()  
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

binding.contentMain.result.text = ""  

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

val desktopMode = true  

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
    finalUrl  
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

if (detectedStreams.isEmpty()) {  

    binding.contentMain.result.append(  
        "\n\nNO STREAMS DETECTED\n"  
    )  

    return@setOnClickListener  
}  

val sortedStreams =  
    mutableListOf<String>()  

// =====================================  
// VIDEOS  
// =====================================  

sortedStreams.addAll(  
    detectedVideos  
)  

// =====================================  
// AUDIO  
// =====================================  

sortedStreams.addAll(  
    detectedAudio  
)  

// =====================================  
// IMAGES  
// =====================================  

sortedStreams.addAll(  
    detectedImages  
)  

val streamList =  
    sortedStreams.toTypedArray()  

androidx.appcompat.app.AlertDialog.Builder(this)  
    .setTitle("Select Stream")  

    .setItems(streamList) { _, which ->  

        val url =  
            streamList[which]  

        try {  

            val lower =  
                url.lowercase()  

            val mimeType =  
                when {  

                    // =========================  
                    // STREAMS / VIDEO  
                    // =========================  

                    lower.contains(".m3u8") ->  
                        "video/*"  

                    lower.contains(".mpd") ->  
                        "video/*"  

                    lower.contains(".mp4") ->  
                        "video/*"  

                    lower.contains(".mkv") ->  
                        "video/*"  

                    lower.contains(".webm") ->  
                        "video/*"  

                    lower.contains(".ts") ->  
                        "video/*"  

                    // =========================  
                    // AUDIO  
                    // =========================  

                    lower.contains(".mp3") ->  
                        "audio/mpeg"  

                    lower.contains(".aac") ->  
                        "audio/aac"  

                    lower.contains(".wav") ->  
                        "audio/wav"  

                    lower.contains(".ogg") ->  
                        "audio/ogg"  

                    lower.contains(".flac") ->  
                        "audio/flac"  

                    // =========================  
                    // IMAGES  
                    // =========================  

                    lower.contains(".jpg") ||  
                    lower.contains(".jpeg") ->  
                        "image/jpeg"  

                    lower.contains(".png") ->  
                        "image/png"  

                    lower.contains(".webp") ->  
                        "image/webp"  

                    lower.contains(".gif") ->  
                        "image/gif"  

                    // =========================  
                    // FALLBACK  
                    // =========================  

                    else ->  
                        "*/*"  
                }  

            val cleanUrl =  
                url  
                    .trim()  
                    .replace("\n", "")  
                    .replace("\r", "")  
                    .replace(" ", "")  

            val intent =  
                Intent(Intent.ACTION_VIEW).apply {  

                    setDataAndType(  
                        Uri.parse(cleanUrl),  
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
        }  
    }  

    .show()

}

// =====================================
// SHARE SELECTED STREAM
// =====================================

binding.contentMain.shareStream.setOnClickListener {

if (detectedStreams.isEmpty()) {  

    binding.contentMain.result.append(  
        "\n\nNO STREAMS DETECTED\n"  
    )  

    return@setOnClickListener  
}  

val sortedStreams =  
    mutableListOf<String>()  

// =====================================  
// VIDEOS  
// =====================================  

sortedStreams.addAll(  
    detectedVideos  
)  

// =====================================  
// AUDIO  
// =====================================  

sortedStreams.addAll(  
    detectedAudio  
)  

// =====================================  
// IMAGES  
// =====================================  

sortedStreams.addAll(  
    detectedImages  
)  

val streamList =  
    sortedStreams.toTypedArray()  

val checkedItems =  
    BooleanArray(streamList.size)  

val selected =  
    mutableListOf<String>()  

androidx.appcompat.app.AlertDialog.Builder(this)  

    .setTitle("Select Streams To Share")  

    .setMultiChoiceItems(  
        streamList,  
        checkedItems  
    ) { _, which, isChecked ->  

        val url =  
            streamList[which]  

        if (isChecked) {  

            if (!selected.contains(url)) {  
                selected.add(url)  
            }  

        } else {  

            selected.remove(url)  
        }  
    }  

    .setPositiveButton("SHARE") { _, _ ->  

        if (selected.isEmpty()) {  
            return@setPositiveButton  
        }  

        try {  

            val allUrls =  
                selected.joinToString("\n\n")  

            val shareIntent =  
                Intent(Intent.ACTION_SEND).apply {  

                    type = "text/plain"  

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
        }  
    }  

    .setNegativeButton("CANCEL", null)  

    .show()

}

// =====================================
// EXPORT M3U
// =====================================

binding.contentMain.exportM3u.setOnClickListener {

if (detectedStreams.isEmpty()) {  

    binding.contentMain.result.append(  
        "\n\nNO STREAMS DETECTED\n"  
    )  

    return@setOnClickListener  
}  

try {  

    val sb =  
        StringBuilder()  

    sb.append("#EXTM3U\n\n")  

    detectedChannels  
.forEach { (name, url) ->  

    sb.append(  
        "#EXTINF:-1,$name\n"  
    )  

    sb.append("$url\n\n")  
}  

    val shareIntent =  
        Intent(Intent.ACTION_SEND).apply {  

            type = "text/plain"  

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

    binding.contentMain.btnImages.setOnClickListener {  

        showImages()  
    }  

    binding.contentMain.btnAudio.setOnClickListener {  

        showAudio()  
    }  

    binding.contentMain.btnClear.setOnClickListener {  

detectedStreams.clear()  

detectedVideos.clear()  

detectedImages.clear()  

detectedAudio.clear()  

binding.contentMain.result.text = ""

}

// =====================================
// RESULT CLICK = COPY
// =====================================

binding.contentMain.result.setOnClickListener {

if (lastSelectedUrl.isBlank()) {  
    return@setOnClickListener  
}  

val clipboard =  
    getSystemService(  
        CLIPBOARD_SERVICE  
    ) as ClipboardManager  

clipboard.setPrimaryClip(  
    ClipData.newPlainText(  
        "stream",  
        lastSelectedUrl  
    )  
)  

Toast.makeText(  
    this,  
    "URL copied",  
    Toast.LENGTH_SHORT  
).show()

}

// =====================================
// RESULT LONG PRESS MENU
// =====================================

binding.contentMain.result.setOnLongClickListener { v ->

val selectedUrl =  
    extractUrlFromText(  
        binding.contentMain.result.text.toString()  
    )  

if (selectedUrl.isBlank()) {  
    return@setOnLongClickListener true  
}  

val popup =  
    PopupMenu(this, v)  

popup.menu.add("OPEN PLAYER")  

popup.menu.add("TEST STREAM")  

popup.menu.add("SHARE URL")  

popup.menu.add("COPY URL")  

popup.setOnMenuItemClickListener {  

    when (it.title.toString()) {  

        "OPEN PLAYER" -> {  

            val intent =  
                Intent(Intent.ACTION_VIEW).apply {  

                    setDataAndType(  
                        Uri.parse(selectedUrl),  
                        "video/*"  
                    )  
                }  

            startActivity(  
                Intent.createChooser(  
                    intent,  
                    "Open With"  
                )  
            )  
        }  

        "TEST STREAM" -> {  

            testStream(selectedUrl)  
        }  

        "SHARE URL" -> {  

            val shareIntent =  
                Intent(Intent.ACTION_SEND).apply {  

                    type = "text/plain"  

                    putExtra(  
                        Intent.EXTRA_TEXT,  
                        selectedUrl  
                    )  
                }  

            startActivity(  
                Intent.createChooser(  
                    shareIntent,  
                    "Share URL"  
                )  
            )  
        }  

        "COPY URL" -> {  

            val clipboard =  
                getSystemService(  
                    CLIPBOARD_SERVICE  
                ) as ClipboardManager  

            clipboard.setPrimaryClip(  
                ClipData.newPlainText(  
                    "stream",  
                    selectedUrl  
                )  
            )  

            Toast.makeText(  
                this,  
                "Copied",  
                Toast.LENGTH_SHORT  
            ).show()  
        }  
    }  

    true  
}  

popup.show()  

true

}

} // END onCreate()

private fun extractUrlFromText(
text: String
): String {

val regex =  
    "(https?://[^\\s]+)".toRegex()  

return regex.find(text)  
    ?.value  
    ?.trim()  
    ?: ""

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

                binding.contentMain.webview  
                    .loadUrl(sharedText)  

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
            intent?.getParcelableExtra<Uri>(  
                Intent.EXTRA_STREAM  
            )  

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

// =====================================  
// REQUEST  
// =====================================  

val builder =  
    Request.Builder()  
        .url(cleanedUrl)  
        .get()  

streamHeaders[cleanedUrl]  
    ?.forEach { (k, v) ->  

        builder.header(k, v)  
    }  

builder.header(  
    "User-Agent",  
    binding.contentMain.webview  
        .settings  
        .userAgentString  
)  

builder.header(  
    "Referer",  
    binding.contentMain.webview.url  
        ?: cleanedUrl  
)  

builder.header(  
    "Origin",  
    binding.contentMain.webview.url  
        ?: cleanedUrl  
)  

builder.header(  
    "Accept",  
    "*/*"  
)  

builder.header(  
    "Connection",  
    "keep-alive"  
)  

val request =  
    builder.build()  

// =====================================  
// EXECUTE  
// =====================================  

okHttpClient  
    .newCall(request)  
    .enqueue(  

        object : Callback {  

            override fun onFailure(  
                call: Call,  
                e: IOException  
            ) {  

                runOnUiThread {  

                    binding.contentMain.result.append(  

                        """

❌ TEST FAILED

URL:
$cleanedUrl

ERROR:
${e.message}

────────────────────

""".trimIndent()
)
}
}

override fun onResponse(  
                call: Call,  
                response: Response  
            ) {  

                try {  

                    val code =  
                        response.code  

                    val contentType =  
                        response.header(  
                            "Content-Type"  
                        ).orEmpty()  

                    val contentLength =  
                        response.header(  
                            "Content-Length"  
                        ).orEmpty()  

                    val location =  
                        response.header(  
                            "Location"  
                        ).orEmpty()  

                    val server =  
                        response.header(  
                            "Server"  
                        ).orEmpty()  

                    val finalStatus =  
                        when {  

                            code in 200..299 ->  
                                "✅ STREAM OK"  

                            code in 300..399 ->  
                                "↪ REDIRECT"  

                            code == 403 ->  
                                "🔒 FORBIDDEN"  

                            code == 404 ->  
                                "❌ NOT FOUND"  

                            else ->  
                                "⚠ RESPONSE RECEIVED"  
                        }  

                    runOnUiThread {  

                        binding.contentMain.result.append(  

                            """

$finalStatus

HTTP:
$code

TYPE:
$contentType

SIZE:
$contentLength

SERVER:
$server

REDIRECT:
$location

HEADERS:
${streamHeaders[cleanedUrl]}

────────────────────

""".trimIndent()
)
}

} catch (t: Throwable) {  

                    Log.e(  
                        "STREAM_TEST",  
                        "parse failed",  
                        t  
                    )  

                    runOnUiThread {  

                        binding.contentMain.result.append(  

                            """

❌ PARSE FAILED

${t.message}

────────────────────

""".trimIndent()
)
}

} finally {  

                    response.close()  
                }  
            }  
        }  
    )

}

// =====================================
// DETECT + SAVE URL
// =====================================

private fun detectAndSaveUrl(
url: String
) {

Log.e(  
    "MEDIA_DETECT",  
    url  
)  

val cleanedUrl =  
    url  
        .replace("\\u0026", "&")  
        .replace("\\/", "/")  
        .trim()  

val lower =  
    cleanedUrl.lowercase()  

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
    lower.contains("playlist") ||  
    lower.contains("chunklist")  

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

if (isSegmentTs) {  
    return  
}  

if (isGarbage) {  
    return  
}  

if (  
    !isVideo &&  
    !isImage &&  
    !isAudio  
) {  
    return  
}  

// =====================================  
// DUPLICATES  
// =====================================  

if (  
    detectedStreams.contains(cleanedUrl)  
) {  
    return  
}  

detectedStreams.add(cleanedUrl)  

if (isVideo) {  
    detectedVideos.add(cleanedUrl)  
}  

if (isImage) {  
    detectedImages.add(cleanedUrl)  
}  

if (isAudio) {  
    detectedAudio.add(cleanedUrl)  
}  

if (isMasterStream) {  
    detectedMasterStreams.add(cleanedUrl)  
}  

// =====================================  
// QUALITY  
// =====================================  

val streamQuality =  
    when {  

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
// BADGES  
// =====================================  

val streamBadge =  
    when {  

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
        lower.contains("expires") ||  
        lower.contains("policy") ->  
            " 🔒 SIGNED"  

        else ->  
            ""  
    }  

val segmentBadge =  
    if (isSegmentTs)  
        " 🧩 SEGMENT"  
    else  
        ""  

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

// =====================================
// SAVE LAST URL
// =====================================

lastSelectedUrl =
cleanedUrl

// =====================================
// UI OUTPUT
// =====================================

runOnUiThread {

// NO AUTO REFRESH

}

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

return "$badge [$streamQuality] [$cdnType]"

}

// =====================================
// SHOW ALL
// =====================================

private fun showAllMedia() {

val sb = StringBuilder()  

// =====================================  
// VIDEOS  
// =====================================  

detectedVideos.forEach { url ->  

    sb.append(  
        buildMediaLabel(url)  
    )  

    sb.append("\n\n")  

    sb.append(url)  

    sb.append(  
        "\n\n────────────────────\n\n"  
    )  
}  

// =====================================  
// AUDIO  
// =====================================  

detectedAudio.forEach { url ->  

    sb.append(  
        buildMediaLabel(url)  
    )  

    sb.append("\n\n")  

    sb.append(url)  

    sb.append(  
        "\n\n────────────────────\n\n"  
    )  
}  

// =====================================  
// IMAGES  
// =====================================  

detectedImages.forEach { url ->  

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

val sb = StringBuilder()  

detectedVideos.forEach { url ->  

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

val sb = StringBuilder()  

detectedImages.forEach { url ->  

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

val sb = StringBuilder()  

detectedAudio.forEach { url ->  

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

                val js = """

(function() {

let results = [];  

document  
    .querySelectorAll("video")  
    .forEach(function(v) {  

        if (v.currentSrc) {  
            results.push(v.currentSrc);  
        }  

        if (v.src) {  
            results.push(v.src);  
        }  
    });  

return JSON.stringify(  
    [...new Set(results)]  
);

})();

""".trimIndent()  

                binding.contentMain.webview  
                    .evaluateJavascript(js) {  

                        value ->  

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

                                detectAndSaveUrl(  
                                    arr.getString(i)  
                                )  
                            }  

                        } catch (_: Throwable) {}  
                    }  

            } catch (_: Throwable) {}  

            binding.contentMain.webview  
.postDelayed(  
    this,  
    10000  
)  
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
!response.isSuccessful &&  
response.code !in 200..399

) {
return
}

val text =  
                        response.body  
                            ?.string()  
                            .orEmpty()  

                    this@MainActivity.runOnUiThread {  

if (  
    !this@MainActivity.isFinishing &&  
    !this@MainActivity.isDestroyed  
) {  

    this@MainActivity.binding  
        .contentMain  
        .result  
        .text = text  
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

}
