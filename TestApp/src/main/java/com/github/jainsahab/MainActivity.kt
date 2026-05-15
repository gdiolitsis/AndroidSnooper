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
// GLOBAL MEDIA CACHE
// =====================================

if (!window.__gelMediaResults) {

    window.__gelMediaResults = [];
}

// =====================================
// SAFE PUSH
// =====================================

function gelPush(url) {

    try {

        if (!url) {
            return;
        }

        window.__gelMediaResults.push(url);

        results.push(url);

    } catch(e) {}
}

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
// FETCH LOGGER
// =====================================

if (!window.__gelFetchHooked) {

    window.__gelFetchHooked = true;

    const originalFetch =
        window.fetch;

    window.fetch =
        function() {

            try {

                const req =
                    arguments[0];

                let url = "";

                if (typeof req === "string") {

                    url = req;

                } else if (req && req.url) {

                    url = req.url;
                }

                if (url) {

                    results.push(url);

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
// XHR LOGGER
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

                    console.log(
                        "GEL_XHR:",
                        method,
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
// BLOB URL HOOK
// =====================================

if (!window.__gelBlobHooked) {

    window.__gelBlobHooked = true;

    const originalCreateObjectURL =
        URL.createObjectURL;

    URL.createObjectURL =
        function(obj) {

            const blobUrl =
                originalCreateObjectURL(obj);

            try {

                results.push(blobUrl);

                console.log(
                    "GEL_BLOB:",
                    blobUrl
                );

            } catch(e) {}

            return blobUrl;
        };
}

// =====================================
// MEDIA SOURCE HOOK
// =====================================

if (!window.__gelMediaSourceHooked) {

    window.__gelMediaSourceHooked = true;

    const originalAddSourceBuffer =
        MediaSource.prototype.addSourceBuffer;

    MediaSource.prototype.addSourceBuffer =
        function(type) {

            try {

                console.log(
                    "GEL_MEDIA_SOURCE:",
                    type
                );

                results.push(
                    "mediasource://" + type
                );

            } catch(e) {}

            return originalAddSourceBuffer.apply(
                this,
                arguments
            );
        };
}

// =====================================
// VIDEO SRC WATCHER
// =====================================

try {

    document
        .querySelectorAll("video")
        .forEach(function(v) {

            try {

                if (v.src) {

                    results.push(v.src);

                    console.log(
                        "GEL_VIDEO_SRC:",
                        v.src
                    );
                }

                if (v.currentSrc) {

                    results.push(v.currentSrc);

                    console.log(
                        "GEL_VIDEO_CURRENT:",
                        v.currentSrc
                    );
                }

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// DELAYED RESCAN QUEUE
// =====================================

try {

    setTimeout(function() {

        try {

            document
                .querySelectorAll("video, audio, source, iframe")
                .forEach(function(el) {

                    try {

                        if (el.src) {
                            results.push(el.src);
                        }

                        if (el.currentSrc) {
                            results.push(el.currentSrc);
                        }

                    } catch(e) {}
                });

        } catch(e) {}

    }, 1500);

    setTimeout(function() {

        try {

            document
                .querySelectorAll("video, audio, source, iframe")
                .forEach(function(el) {

                    try {

                        if (el.src) {
                            results.push(el.src);
                        }

                        if (el.currentSrc) {
                            results.push(el.currentSrc);
                        }

                    } catch(e) {}
                });

        } catch(e) {}

    }, 4000);

    setTimeout(function() {

        try {

            document
                .querySelectorAll("video, audio, source, iframe")
                .forEach(function(el) {

                    try {

                        if (el.src) {
                            results.push(el.src);
                        }

                        if (el.currentSrc) {
                            results.push(el.currentSrc);
                        }

                    } catch(e) {}
                });

        } catch(e) {}

    }, 8000);

} catch(e) {}

// =====================================
// IFRAME DEEP SCAN
// =====================================

try {

    document
        .querySelectorAll("iframe")
        .forEach(function(frame) {

            try {

                const src =
                    frame.src;

                if (src) {

                    results.push(src);

                    console.log(
                        "GEL_IFRAME:",
                        src
                    );
                }

                const doc =
                    frame.contentDocument ||
                    frame.contentWindow?.document;

                if (!doc) {
                    return;
                }

                // =========================
                // VIDEO
                // =========================

                doc
                    .querySelectorAll(
                        "video, source, audio"
                    )
                    .forEach(function(el) {

                        try {

                            if (el.src) {

                                results.push(el.src);

                                console.log(
                                    "GEL_IFRAME_MEDIA:",
                                    el.src
                                );
                            }

                            if (el.currentSrc) {

                                results.push(
                                    el.currentSrc
                                );

                                console.log(
                                    "GEL_IFRAME_CURRENT:",
                                    el.currentSrc
                                );
                            }

                        } catch(e) {}
                    });

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// PERFORMANCE NETWORK SCAN
// =====================================

try {

    const perfEntries =
        performance.getEntriesByType(
            "resource"
        );

    perfEntries.forEach(function(entry) {

        try {

            const url =
                entry.name;

            if (!url) {
                return;
            }

            const lower =
                url.toLowerCase();

            if (

                lower.includes(".m3u8") ||
                lower.includes(".mpd") ||
                lower.includes(".mp4") ||
                lower.includes(".m4s") ||
                lower.includes(".ts") ||
                lower.includes("playlist") ||
                lower.includes("chunklist") ||
                lower.includes("manifest") ||
                lower.includes("video") ||
                lower.includes("live")

            ) {

                results.push(url);

                console.log(
                    "GEL_PERF:",
                    url
                );
            }

        } catch(e) {}

    });

} catch(e) {}

// =====================================
// LIVE VIDEO OBSERVER
// =====================================

if (!window.__gelVideoObserver) {

    window.__gelVideoObserver = true;

    try {

        const observeVideo =
            function(video) {

                try {

                    // =====================
                    // INITIAL SRC
                    // =====================

                    if (video.src) {

                        results.push(video.src);

                        console.log(
                            "GEL_VIDEO_INIT:",
                            video.src
                        );
                    }

                    if (video.currentSrc) {

                        results.push(
                            video.currentSrc
                        );

                        console.log(
                            "GEL_VIDEO_CURRENT:",
                            video.currentSrc
                        );
                    }

                    // =====================
                    // ATTRIBUTE WATCH
                    // =====================

                    const observer =
                        new MutationObserver(
                            function() {

                                try {

                                    if (video.src) {

                                        results.push(
                                            video.src
                                        );

                                        console.log(
                                            "GEL_VIDEO_SRC_CHANGE:",
                                            video.src
                                        );
                                    }

                                    if (video.currentSrc) {

                                        results.push(
                                            video.currentSrc
                                        );

                                        console.log(
                                            "GEL_VIDEO_CURRENT_CHANGE:",
                                            video.currentSrc
                                        );
                                    }

                                } catch(e) {}
                            }
                        );

                    observer.observe(
                        video,
                        {
                            attributes: true,
                            attributeFilter: [
                                "src"
                            ]
                        }
                    );

                    // =====================
                    // PLAY EVENT
                    // =====================

                    video.addEventListener(
                        "play",
                        function() {

                            try {

                                if (video.currentSrc) {

                                    results.push(
                                        video.currentSrc
                                    );

                                    console.log(
                                        "GEL_VIDEO_PLAY:",
                                        video.currentSrc
                                    );
                                }

                            } catch(e) {}
                        }
                    );

                } catch(e) {}
            };

        // =========================
        // EXISTING VIDEOS
        // =========================

        document
            .querySelectorAll("video")
            .forEach(function(v) {

                observeVideo(v);
            });

        // =========================
        // NEW VIDEOS
        // =========================

        const pageObserver =
            new MutationObserver(
                function(mutations) {

                    mutations.forEach(
                        function(m) {

                            m.addedNodes
                                .forEach(function(node) {

                                    try {

                                        if (
                                            node.tagName === "VIDEO"
                                        ) {

                                            observeVideo(node);
                                        }

                                    } catch(e) {}
                                });
                        });
                }
            );

        pageObserver.observe(
            document.documentElement,
            {
                childList: true,
                subtree: true
            }
        );

    } catch(e) {}
}

// =====================================
// WEBSOCKET HOOK
// =====================================

if (!window.__gelWebSocketHooked) {

    window.__gelWebSocketHooked = true;

    const OriginalWebSocket =
        window.WebSocket;

    window.WebSocket =
        function(url, protocols) {

            try {

                if (url) {

                    results.push(url);

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
            // INCOMING MESSAGES
            // =========================

            ws.addEventListener(
                "message",
                function(event) {

                    try {

                        const data =
                            String(event.data);

                        console.log(
                            "GEL_WS_MESSAGE:",
                            data
                        );

                        const regex =
/https?:\/\/[^"' ]+/gi;

                        const found =
                            data.match(regex);

                        if (found) {

                            found.forEach(
                                function(x) {

                                    results.push(x);
                                }
                            );
                        }

                    } catch(e) {}
                }
            );

            return ws;
        };
}

// =====================================
// FETCH RESPONSE PARSER
// =====================================

if (!window.__gelFetchResponseHooked) {

    window.__gelFetchResponseHooked = true;

    const originalFetch =
        window.fetch;

    window.fetch =
        async function() {

            const response =
                await originalFetch.apply(
                    this,
                    arguments
                );

            try {

                const clone =
                    response.clone();

                clone.text()
                    .then(function(txt) {

                        try {

                            const regex =
/https?:\/\/[^"'\\ ]+/gi;

                            const found =
                                txt.match(regex);

                            if (found) {

                                found.forEach(
                                    function(url) {

                                        const lower =
                                            url.toLowerCase();

                                        if (

                                            lower.includes(".m3u8") ||
                                            lower.includes(".mpd") ||
                                            lower.includes(".mp4") ||
                                            lower.includes(".m4s") ||
                                            lower.includes(".ts") ||
                                            lower.includes("manifest") ||
                                            lower.includes("playlist") ||
                                            lower.includes("chunklist") ||
                                            lower.includes("live")

                                        ) {

                                            results.push(url);

                                            console.log(
                                                "GEL_FETCH_RESPONSE:",
                                                url
                                            );
                                        }
                                    }
                                );
                            }

                        } catch(e) {}
                    });

            } catch(e) {}

            return response;
        };
}

// =====================================
// SERVICE WORKER HOOK
// =====================================

if (

    'serviceWorker' in navigator &&
    !window.__gelServiceWorkerHooked

) {

    window.__gelServiceWorkerHooked = true;

    try {

        navigator.serviceWorker
            .getRegistrations()
            .then(function(registrations) {

                registrations.forEach(
                    function(reg) {

                        try {

                            const scope =
                                reg.scope;

                            if (scope) {

                                results.push(scope);

                                console.log(
                                    "GEL_SW_SCOPE:",
                                    scope
                                );
                            }

                            // =====================
                            // ACTIVE WORKER
                            // =====================

                            const worker =
                                reg.active;

                            if (worker) {

                                console.log(
                                    "GEL_SW_ACTIVE:",
                                    worker.scriptURL
                                );

                                results.push(
                                    worker.scriptURL
                                );
                            }

                        } catch(e) {}
                    }
                );
            });

    } catch(e) {}
}

// =====================================
// DRM / EME HOOK
// =====================================

if (!window.__gelEMEHooked) {

    window.__gelEMEHooked = true;

    try {

        const originalRequestMediaKeySystemAccess =
            navigator.requestMediaKeySystemAccess;

        navigator.requestMediaKeySystemAccess =
            function(keySystem, configs) {

                try {

                    console.log(
                        "GEL_DRM:",
                        keySystem
                    );

                    results.push(
                        "drm://" + keySystem
                    );

                } catch(e) {}

                return originalRequestMediaKeySystemAccess.apply(
                    this,
                    arguments
                );
            };

    } catch(e) {}
}

// =====================================
// ENCRYPTED EVENT WATCHER
// =====================================

try {

    document
        .querySelectorAll("video")
        .forEach(function(video) {

            try {

                video.addEventListener(
                    "encrypted",
                    function(event) {

                        try {

                            console.log(
                                "GEL_ENCRYPTED_MEDIA"
                            );

                            results.push(
                                "encrypted://media"
                            );

                        } catch(e) {}
                    }
                );

            } catch(e) {}
        });

} catch(e) {}

// =====================================
// JSON.parse HOOK
// =====================================

if (!window.__gelJSONHooked) {

    window.__gelJSONHooked = true;

    const originalJSONParse =
        JSON.parse;

    JSON.parse =
        function(text) {

            try {

                if (typeof text === "string") {

                    const regex =
/https?:\/\/[^"'\\ ]+/gi;

                    const found =
                        text.match(regex);

                    if (found) {

                        found.forEach(
                            function(url) {

                                results.push(url);

                                console.log(
                                    "GEL_JSON_URL:",
                                    url
                                );
                            }
                        );
                    }
                }

            } catch(e) {}

            return originalJSONParse.apply(
                this,
                arguments
            );
        };
}

// =====================================
// atob HOOK
// =====================================

if (!window.__gelAtobHooked) {

    window.__gelAtobHooked = true;

    const originalAtob =
        window.atob;

    window.atob =
        function(str) {

            const decoded =
                originalAtob(str);

            try {

                const regex =
/https?:\/\/[^"'\\ ]+/gi;

                const found =
                    decoded.match(regex);

                if (found) {

                    found.forEach(
                        function(url) {

                            results.push(url);

                            console.log(
                                "GEL_ATOB_URL:",
                                url
                            );
                        }
                    );
                }

            } catch(e) {}

            return decoded;
        };
}

// =====================================
// eval HOOK
// =====================================

if (!window.__gelEvalHooked) {

    window.__gelEvalHooked = true;

    const originalEval =
        window.eval;

    window.eval =
        function(code) {

            try {

                if (typeof code === "string") {

                    const regex =
/https?:\/\/[^"'\\ ]+/gi;

                    const found =
                        code.match(regex);

                    if (found) {

                        found.forEach(
                            function(url) {

                                results.push(url);

                                console.log(
                                    "GEL_EVAL_URL:",
                                    url
                                );
                            }
                        );
                    }
                }

            } catch(e) {}

            return originalEval.apply(
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
                buildGroupTitle(url)

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
    lastTouchX.toInt()

    val y =
    lastTouchY.toInt()

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

                        runOnUiThread {

                            try {

                                showAllMedia()

                            } catch (_: Throwable) {}
                        }
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

                            runOnUiThread {

                                try {

                                    showAllMedia()

                                } catch (_: Throwable) {}
                            }

                        } catch (_: Throwable) {

                            streamValidation[url] =
                                "⚠ ERROR"

                            runOnUiThread {

                                try {

                                    showAllMedia()

                                } catch (_: Throwable) {}
                            }

                        } finally {

                            response.close()
                        }
                    }
                }
            )

    } catch (_: Throwable) {

        streamValidation[url] =
            "⚠ ERROR"
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
    
val streamsSnapshot =
    detectedStreams.toList()

val headersSnapshot =
    streamHeaders.toMap()

val channelsSnapshot =
    detectedChannels.toMap()
    
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

        it.startsWith(normalizedUrl)
    }
) {
    return
}

detectedStreams.add(cleanedUrl)  

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

    // NO AUTO REFRESH
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
                "4K"

            lower.contains("1440") ->
                "1440p"

            lower.contains("1080") ->
                "1080p"

            lower.contains("720") ->
                "720p"

            lower.contains("540") ->
                "540p"

            lower.contains("480") ->
                "480p"

            else ->
                "AUTO"
        }

    val channel =
        when {

            // =====================================
            // GREEK TV
            // =====================================

            lower.contains("megatv") ||
            lower.contains("mega") ->
                "MEGA"

            lower.contains("ant1") ->
                "ANT1"

            lower.contains("skai") ->
                "SKAI"

            lower.contains("alphatv") ||
            lower.contains("alpha") ->
                "ALPHA"

            lower.contains("star") ->
                "STAR"

            lower.contains("open") ->
                "OPEN"

            lower.contains("ert") ->
                "ERT"

            lower.contains("mad") ->
                "MAD TV"

            // =====================================
            // SPORTS
            // =====================================

            lower.contains("sport") ->
                "SPORT"

            lower.contains("nova") ->
                "NOVA"

            lower.contains("cosmote") ->
                "COSMOTE"

            // =====================================
            // FALLBACKS
            // =====================================

            lower.contains(".m3u8") ->
                "HLS STREAM"

            lower.contains(".mpd") ->
                "DASH STREAM"

            lower.contains(".mp4") ->
                "VIDEO STREAM"

            else ->
                "LIVE STREAM"
        }

    val liveTag =
        when {

            lower.contains("live") ->
                " LIVE"

            lower.contains("/vod/") ->
                " VOD"

            else ->
                ""
        }

    return "$channel $quality$liveTag"
        .replace("  ", " ")
        .trim()
}

// =====================================
// BUILD IPTV GROUP
// =====================================

private fun buildGroupTitle(
    url: String
): String {

    val lower =
        url.lowercase()

    return when {

        // =====================================
        // GREEK TV
        // =====================================

        lower.contains("mega") ||
        lower.contains("ant1") ||
        lower.contains("skai") ||
        lower.contains("alpha") ||
        lower.contains("star") ||
        lower.contains("open") ||
        lower.contains("ert") ->
            "Greek TV"

        // =====================================
        // SPORTS
        // =====================================

        lower.contains("sport") ||
        lower.contains("nova") ||
        lower.contains("cosmote") ->
            "Sports"

        // =====================================
        // AUDIO
        // =====================================

        lower.contains(".mp3") ||
        lower.contains(".aac") ||
        lower.contains(".m4a") ->
            "Radio"

        // =====================================
        // VOD
        // =====================================

        lower.contains("/vod/") ||
        lower.contains("original.mp4") ->
            "VOD"

        // =====================================
        // LIVE
        // =====================================

        lower.contains(".m3u8") ->
            "HLS Live"

        lower.contains(".mpd") ->
            "DASH Live"

        // =====================================
        // FALLBACK
        // =====================================

        else ->
            "Live Streams"
    }
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

        lower.contains("mega") ->
            "https://www.megatv.com/wp-content/uploads/2021/11/mega-logo.png"

        lower.contains("ant1") ->
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/57/ANT1_logo_%282020%29.png/512px-ANT1_logo_%282020%29.png"

        lower.contains("skai") ->
            "https://upload.wikimedia.org/wikipedia/commons/2/24/Skai_TV_logo_2018.png"

        lower.contains("alpha") ->
            "https://upload.wikimedia.org/wikipedia/commons/8/89/Alpha_TV_logo.png"

        lower.contains("star") ->
            "https://upload.wikimedia.org/wikipedia/commons/7/7a/Star_Channel_logo.png"

        lower.contains("open") ->
            "https://upload.wikimedia.org/wikipedia/commons/5/50/Open_Beyond_logo.png"

        lower.contains("ert") ->
            "https://upload.wikimedia.org/wikipedia/commons/6/61/ERT_logo_2022.png"

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
    [
        ...new Set(
            window.__gelMediaResults || results
        )
    ]
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
