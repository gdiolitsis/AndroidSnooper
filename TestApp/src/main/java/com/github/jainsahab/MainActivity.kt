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

// =====================================
// STREAM META
// =====================================

private val streamResolution =
    linkedMapOf<String, String>()

private val streamBandwidth =
    linkedMapOf<String, String>()

private val streamCodec =
    linkedMapOf<String, String>()
    
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
// STREAM VALIDATION CACHE
// =====================================

private val streamValidation =
    linkedMapOf<String, String>()
  
private var monitorRunning =  
false  
  
private var autoRefreshEnabled =  
false  

private var lastTouchX = 0f
private var lastTouchY = 0f

private val streamScores =
    mutableMapOf<String, Int>()
      
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
            
// =====================================
// NETWORK MEDIA DETECTION
// =====================================

try {

    val lower =
        url.lowercase()

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
        lower.contains("hls")

    ) {

        Log.e(
            "NETWORK_MEDIA",
            url
        )

        detectAndSaveUrl(url)
    }

} catch (_: Throwable) {}

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

if (!window.__gelMediaResults) {
window.__gelMediaResults = [];
}

function gelPush(url) {

try {

    if (!url) return;

    url = String(url);

    results.push(url);

    window.__gelMediaResults.push(url);

} catch(e) {}

}

// =====================================
// IMG
// =====================================

document
.querySelectorAll("img")
.forEach(function(el) {

if (el.src) {
    gelPush(el.src);
}

});

// =====================================
// VIDEO / AUDIO / SOURCE
// =====================================

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

            if (el.poster) {
                gelPush(el.poster);
            }

        } catch(e) {}
    });

// =====================================
// IFRAME
// =====================================

document
    .querySelectorAll("iframe")
    .forEach(function(frame) {

        try {

            if (frame.src) {

                gelPush(frame.src);

                console.log(
                    "GEL_IFRAME_SRC:",
                    frame.src
                );
            }

        } catch(e) {}
    });

// =====================================
// HTML MANIFEST SCAN
// =====================================

try {

    const html =
        document.documentElement.outerHTML;

    const regex =
/(https?:\/\/[^"'\\s]+?\.(m3u8|mpd|mp4|m4s|ts)(\?[^"'\\s]*)?)/gi;

    let match;

    while (
        (match = regex.exec(html)) !== null
    ) {

        gelPush(match[1]);

        console.log(
            "GEL_HTML_MEDIA:",
            match[1]
        );
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
// FETCH HOOK
// =====================================

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

                gelPush(url);

                console.log(
                    "GEL_XHR:",
                    url
                );
            }

        } catch(e) {}

        return originalOpen.apply(
            this,
            arguments
        );
    };

}

// =====================================
// PERFORMANCE SCAN
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
// MSE HOOK
// =====================================

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

// =====================================
// VIDEO OBSERVER
// =====================================

if (!window.__gelVideoObserver) {

window.__gelVideoObserver = true;

try {

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

} catch(e) {}

}

// =====================================
// AUTO PLAY
// =====================================

try {

document
.querySelectorAll(
    "video"
)
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
// FINAL
// =====================================

results =
[...new Set(
results.concat(
window.__gelMediaResults
)
)];

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

sortedStreams.addAll(
    detectedImages
)

val allUrls =
    sortedStreams.joinToString(
        "\n\n"
    )

try {

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
            "Share Stream With"
        )
    )

} catch (t: Throwable) {

    Log.e(
        "SHARE_STREAM",
        "failed",
        t
    )
}

} // END shareStream listener

binding.contentMain.shareStreams.setOnClickListener {

val sortedStreams =
    mutableListOf<String>()

sortedStreams.addAll(
    detectedVideos
)

sortedStreams.addAll(
    detectedAudio
)

sortedStreams.addAll(
    detectedImages
)

// =====================================
// SHARE STREAMS DIALOG
// =====================================

val streamList =
    sortedStreams.toTypedArray()

val checkedItems =
    BooleanArray(streamList.size)

val selected =
    mutableListOf<String>()

val dialog =

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

        // =====================================
        // SELECT ALL
        // =====================================

        .setNeutralButton(
            "SELECT ALL",
            null
        )

        // =====================================
        // COPY SELECTED
        // =====================================

        .setPositiveButton(
            "COPY",
            null
        )

        // =====================================
        // SHARE SELECTED
        // =====================================

        .setNegativeButton(
            "SHARE",
            null
        )

        .show()

dialog.setCanceledOnTouchOutside(true)

dialog.setCancelable(true)

// =====================================
// SELECT ALL
// =====================================

dialog.getButton(
    androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL
).setOnClickListener {

    selected.clear()

    for (i in streamList.indices) {

        checkedItems[i] = true

        dialog.listView.setItemChecked(
            i,
            true
        )

        selected.add(
            streamList[i]
        )
    }

    Toast.makeText(
        this,
        "All selected",
        Toast.LENGTH_SHORT
    ).show()
}

// =====================================
// COPY
// =====================================

dialog.getButton(
    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
).setOnClickListener {

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

dialog.getButton(
    androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
).setOnClickListener {

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

        Toast.makeText(
            this,
            "Share failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}
}

// =====================================
// TEST STREAM BUTTON
// =====================================

binding.contentMain.testStream.setOnClickListener {

    if (detectedStreams.isEmpty()) {

        binding.contentMain.result.append(
            "\n\nNO STREAMS DETECTED\n"
        )

        return@setOnClickListener
    }

    val sortedStreams =
        mutableListOf<String>()

    sortedStreams.addAll(
        detectedVideos
    )

    sortedStreams.addAll(
        detectedAudio
    )

    sortedStreams.addAll(
        detectedImages
    )

    val streamList =
        sortedStreams.toTypedArray()

    androidx.appcompat.app.AlertDialog.Builder(this)

        .setTitle("Select Stream To Test")

        .setItems(streamList) { _, which ->

            val selectedUrl =
                streamList[which]

            testStream(selectedUrl)
        }

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

        detectedStreams
            .toList()
            .forEachIndexed { index, url ->

                if (!isExportableStream(url)) {
                    return@forEachIndexed
                }

                val label =
                    buildMediaLabel(url)
                        .replace("🟢", "")
                        .replace("🟡", "")
                        .replace("🔒", "")
                        .replace("❌", "")
                        .replace("⚠", "")
                        .trim()

                val streamType =
                    buildStreamType(url)

                val channelName =
                    buildChannelName(url)

                val name =
                    "$channelName [$streamType] - $label"

                val groupTitle =
                    "Live Streams"

                val logoUrl =
                    buildLogoUrl(url)

                sb.append(
                    "#EXTINF:-1 tvg-name=\"$name\" tvg-logo=\"$logoUrl\" group-title=\"$groupTitle\",$name\n"
                )

                sb.append(url)

                sb.append("\n\n")
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

    streamScores.clear()

    streamValidation.clear()

    binding.contentMain.result.text = ""
}

// =====================================
// RESULT CLICK = COPY SELECTED TEXT
// =====================================

binding.contentMain.result.setOnClickListener {

    val start =
    kotlin.math.max(
        0,
        binding.contentMain.result.selectionStart
    )

val end =
    kotlin.math.max(
        start,
        binding.contentMain.result.selectionEnd
    )

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

binding.contentMain.result.setOnLongClickListener { v ->

    val textView =
        binding.contentMain.result

    val text =
        textView.text.toString()

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
        layout.getLineForVertical(y)

    val offset =
        layout.getOffsetForHorizontal(
            line,
            x.toFloat()
        )

    val regex =
        "(https?://[^\\s]+)".toRegex()

    var selectedUrl = ""

    regex.findAll(text).forEach {

        if (
            offset >= it.range.first &&
            offset <= it.range.last
        ) {

            selectedUrl =
                it.value
        }
    }

    if (selectedUrl.isBlank()) {

        selectedUrl =
            regex.find(text)
                ?.value
                ?: ""
    }

    if (selectedUrl.isBlank()) {
        return@setOnLongClickListener true
    }

    lastSelectedUrl =
        selectedUrl

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
    "(https?://[^\\s\"'<>]+)"
        .toRegex()

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

                if (
    sharedText.startsWith("http")
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
    ?.toMap()
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

                    val responseBody =
                        response.body
                            ?.string()
                            .orEmpty()

                    val m3u8Variants =
    if (

        cleanedUrl.contains(".m3u8") &&
        responseBody.contains("#EXTM3U")

    ) {

        val extractedVariants =
            extractVariantUrls(
                cleanedUrl,
                responseBody
            )

        extractedVariants.forEach {

            detectAndSaveUrl(it)
        }

        parseM3u8Variants(
            cleanedUrl,
            responseBody
        )

    } else {

        ""
    }

                    val finalStatus =
    when {

        // =========================
        // PERFECT
        // =========================

        code in 200..299 &&
        (
            contentType.contains("mpegurl", true) ||
            contentType.contains("dash", true) ||
            contentType.contains("video", true)
        ) ->
            "🟢 PLAYABLE STREAM"

        // =========================
        // HLS MANIFEST
        // =========================

        code in 200..299 &&
        responseBody.contains("#EXTM3U") ->
            "🟢 VALID HLS MANIFEST"

        // =========================
        // DASH MANIFEST
        // =========================

        code in 200..299 &&
        responseBody.contains("<MPD") ->
            "🟢 VALID DASH MANIFEST"

        // =========================
        // TOKEN
        // =========================

        code == 401 ||
        code == 403 ->
            "🔒 TOKEN / AUTH REQUIRED"

        // =========================
        // GEO
        // =========================

        responseBody.contains("geoblock", true) ||
        responseBody.contains("geo blocked", true) ->
            "🌍 GEO BLOCKED"

        // =========================
        // DEAD
        // =========================

        code == 404 ->
            "❌ DEAD STREAM"

        // =========================
        // REDIRECT
        // =========================

        code in 300..399 ->
            "↪ REDIRECT"

        // =========================
        // PARTIAL
        // =========================

        code in 200..299 ->
            "🟡 RESPONSE RECEIVED"

        else ->
            "⚠ UNKNOWN RESPONSE"
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

$m3u8Variants

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
// AUTO VALIDATE STREAM
// =====================================

private fun autoValidateStream(
    url: String
) {

    if (
        streamValidation.containsKey(url)
    ) {
        return
    }

    streamValidation[url] =
        "⏳ TESTING"

    try {

        val request =
            Request.Builder()
                .url(url)
                .get()
                .build()

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

                            val body =
                                response.body
                                    ?.string()
                                    .orEmpty()

                            val result =
                                when {

                                    code in 200..299 &&
                                    body.contains("#EXTM3U") ->
                                        "🟢 VERIFIED"

                                    code in 200..299 &&
                                    body.contains("<MPD") ->
                                        "🟢 VERIFIED"

                                    code == 401 ||
                                    code == 403 ->
                                        "🔒 TOKEN"

                                    code == 404 ->
                                        "❌ DEAD"

                                    else ->
                                        "🟡 PARTIAL"
                                }

                            streamValidation[url] =
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
                        ?.toIntOrNull()
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

                sb.append(
                    resolveRelativeUrl(
                        manifestUrl,
                        uri
                    )
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
        
// =====================================
// NORMALIZED URL
// =====================================

val normalizedUrl =
    cleanedUrl
        .substringBefore("?token=")
        .substringBefore("&token=")
        .substringBefore("?expires=")
        .substringBefore("&expires=")
        .substringBefore("?signature=")
        .substringBefore("&signature=")
        .substringBefore("?policy=")
        .substringBefore("&policy=")
        .substringBefore("?hdnts=")
        .substringBefore("&hdnts=")
        .substringBefore("?auth=")
        .substringBefore("&auth=")

val lower =  
    cleanedUrl.lowercase()  
    
// =====================================
// STREAM SCORE ENGINE
// =====================================

val streamPriority =
    when {

        // =========================
        // LIVE MASTER HLS
        // =========================

        lower.contains("master.m3u8") &&
        !lower.contains("/vod/") ->
            1000

        // =========================
        // LIVE HLS
        // =========================

        lower.contains(".m3u8") &&
        (
            lower.contains("live") ||
            lower.contains("channel") ||
            lower.contains("index")
        ) ->
            950

        // =========================
        // DASH LIVE
        // =========================

        lower.contains(".mpd") &&
        (
            lower.contains("live") ||
            lower.contains("manifest")
        ) ->
            900

        // =========================
        // PLAYLISTS
        // =========================

        lower.contains("playlist") ->
            850

        lower.contains("chunklist") ->
            800

        // =========================
        // DIRECT VIDEO
        // =========================

        lower.contains(".mp4") &&
        !lower.contains("/vod/") &&
        !lower.contains("trailer") &&
        !lower.contains("intro") &&
        !lower.contains("original.mp4") ->
            700

        // =========================
        // SEGMENTS
        // =========================

        lower.contains(".m4s") ||
        lower.contains(".ts") ->
            300

        // =========================
        // AUDIO STREAMS
        // =========================

        lower.contains(".mp3") ||
        lower.contains(".aac") ->
            250

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

    detectedStreams.any {

        it.substringBefore("?") ==
        normalizedUrl.substringBefore("?")
    }

) {
    return
}

detectedStreams.add(cleanedUrl)

// =====================================
// STREAM PRIORITY SAVE
// =====================================

streamScores[cleanedUrl] =
    streamPriority

// =====================================
// AUTO VALIDATE STREAM
// =====================================

if (

    lower.contains(".m3u8") ||
    lower.contains(".mpd")

) {

    autoValidateStream(
        cleanedUrl
    )
}

// =====================================
// MEDIA TYPES
// =====================================

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

    streamResolution[cleanedUrl]

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

    binding.contentMain.result.append(
        """

$streamBadge [$streamQuality] [$cdnType]$securityBadge$segmentBadge

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

val validation =
    streamValidation[url]
        ?: ""

return "$badge [$streamQuality] [$cdnType] $validation"

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

        lower.contains("action24") ->
            "ACTION 24"

        lower.contains("mega") &&
        lower.contains("news") ->
            "MEGA NEWS"

        lower.contains("mega") ->
            "MEGA"

        lower.contains("ant1") &&
        lower.contains("comedy") ->
            "ANT1 COMEDY"

        lower.contains("ant1") &&
        lower.contains("drama") ->
            "ANT1 DRAMA"

        lower.contains("ant1") ->
            "ANT1"

        lower.contains("skai") ->
            "SKAI"

        lower.contains("alpha") ->
            "ALPHA"

        lower.contains("star") &&
        lower.contains("international") ->
            "STAR INTERNATIONAL"

        lower.contains("star") ->
            "STAR"

        lower.contains("open") ->
            "OPEN"

        lower.contains("ertnews") ->
            "ERT NEWS"

        lower.contains("ertworld") ->
            "ERT WORLD"

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

        lower.contains("onechannel") ->
            "ONE CHANNEL"

        lower.contains("bluesky") ->
            "BLUE SKY"

        // =====================================
        // PERIFEREIAKA
        // =====================================

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
// IS EXPORTABLE STREAM
// =====================================

private fun isExportableStream(
    url: String
): Boolean {

    val lower =
        url.lowercase()

    val validation =
        streamValidation[url]
            ?: ""

    // =====================================
    // DEAD
    // =====================================

    if (
        validation.contains("DEAD")
    ) {
        return false
    }

    // =====================================
    // SEGMENTS
    // =====================================

    if (

        lower.endsWith(".ts") ||
        lower.endsWith(".m4s")

    ) {
        return false
    }

    // =====================================
    // STATIC / VOD PLACEHOLDERS
    // =====================================

    if (

        lower.contains("original.mp4") ||
        lower.contains("/vod/")

    ) {
        return false
    }

    // =====================================
    // IMAGES
    // =====================================

    if (

        lower.contains(".jpg") ||
        lower.contains(".jpeg") ||
        lower.contains(".png") ||
        lower.contains(".webp") ||
        lower.contains(".gif")

    ) {
        return false
    }

    // =====================================
    // GOOD STREAMS
    // =====================================

    return (

        lower.contains(".m3u8") ||
        lower.contains(".mpd") ||
        lower.contains(".mp4")

    )
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

        lower.contains("star") ->
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
            "http://i.imgur.com/nAzfWx5.png"

        lower.contains("tv100") ->
            "https://i.imgur.com/D7YqJig.png"

        lower.contains("tvcreta") ->
            "https://i.imgur.com/1uMcCR6.png"

        lower.contains("tvfilopoli") ->
            "http://i.imgur.com/GdgekUL.png"

        lower.contains("tvkosmos") ->
            "https://raw.githubusercontent.com/manolischania/manalab-iptv/refs/heads/main/logo/KOSMOS_TV.png"

        lower.contains("tvrodopi") ->
            "https://rodopiflix.gr/wp-content/uploads/2024/08/tvrodopi-logo.png"

        lower.contains("vergina") ->
            "https://i.imgur.com/JzuEEDi.png"

        lower.contains("zougla") ->
            "http://i.imgur.com/dW08jNe.png"

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
        .sortedByDescending { url ->

            val lower =
                url.lowercase()

            when {

                lower.contains("master.m3u8") &&
                !lower.contains("/vod/") ->
                    1000

                lower.contains(".m3u8") &&
                (
                    lower.contains("live") ||
                    lower.contains("channel") ||
                    lower.contains("index")
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
                    10

                else ->
                    1
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

                    val js = """

(function() {

let results = [];

// =====================================
// VIDEO TAGS
// =====================================

document
    .querySelectorAll("video")
    .forEach(function(v) {

        try {

            if (v.currentSrc) {

                results.push(v.currentSrc);

                console.log(
                    "GEL_VIDEO_CURRENT:",
                    v.currentSrc
                );
            }

            if (v.src) {

                results.push(v.src);

                console.log(
                    "GEL_VIDEO_SRC:",
                    v.src
                );
            }

        } catch(e) {}
    });

// =====================================
// SOURCE TAGS
// =====================================

document
    .querySelectorAll("source")
    .forEach(function(s) {

        try {

            if (s.src) {

                results.push(s.src);

                console.log(
                    "GEL_SOURCE_SRC:",
                    s.src
                );
            }

        } catch(e) {}
    });

// =====================================
// FETCH HOOK
// =====================================

try {

    if (!window.__gelFetchHooked) {

        window.__gelFetchHooked = true;

        const originalFetch =
            window.fetch;

        window.fetch =
            async function() {

                try {

                    const url =
                        arguments[0];

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
    lower.includes("live")

) {

    results.push(url);

    console.log(
        "GEL_MEDIA_URL:",
        url
    );
}
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
                            lower.includes("live")

                        ) {

                            results.push(url);

                            console.log(
                                "GEL_MEDIA_XHR:",
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

    const perf =
        performance.getEntriesByType(
            "resource"
        );

    perf.forEach(function(r) {

        try {

            const u =
                r.name || "";

            if (u) {

                results.push(u);

                console.log(
                    "GEL_PERFORMANCE:",
                    u
                );
            }

        } catch(e) {}
    });

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

                    results.push(v.src);

                    console.log(
                        "GEL_BLOB_VIDEO:",
                        v.src
                    )
                }

                if (
                    v.currentSrc &&
                    v.currentSrc.startsWith("blob:")
                ) {

                    results.push(v.currentSrc)

                    console.log(
                        "GEL_BLOB_CURRENT:",
                        v.currentSrc
                    )
                }

            } catch(e) {}
        })

} catch(e) {}

return JSON.stringify(
    [...new Set(results)]
)

})()

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

            detectAndSaveUrl(
                arr.getString(i)
            )
        }

    } catch (_: Throwable) {
    }
}

            } catch (_: Throwable) {
            }

            binding.contentMain.webview.postDelayed(
                this,
                4000
            )
        }
    }

}, 4000)

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

}
