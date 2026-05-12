package com.github.jainsahab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import com.github.jainsahab.databinding.ActivityMainBinding
import com.prateekj.snooper.AndroidSnooper
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.okhttp.SnooperInterceptor
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

    private val okHttpClient:
            OkHttpClient

        get() {

            val builder =
                OkHttpClient.Builder()

            builder.networkInterceptors().add(
                SnooperInterceptor()
            )

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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        AndroidSnooper.init(application)

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

        setSupportActionBar(
            binding.toolbar
        )

        handleIncomingIntent()

// =====================================
// BROWSER SETTINGS
// =====================================

binding.contentMain.webview.settings.apply {

    javaScriptEnabled = true

    domStorageEnabled = true

    mediaPlaybackRequiresUserGesture = false

    loadsImagesAutomatically = true

    useWideViewPort = true

    loadWithOverviewMode = true

    builtInZoomControls = true

    displayZoomControls = false

    cacheMode =
        WebSettings.LOAD_DEFAULT
}


// =====================================
// HANDLE NEW INTENTS
// =====================================

override fun onNewIntent(intent: Intent?) {

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

                binding.contentMain.webview
                    .loadUrl(sharedText)

                detectAndSaveUrl(
                    sharedText
                )

                Log.e(
                    "SHARE_IMPORT",
                    sharedText
                )

                return
            }
        }

        // =====================================
        // OPEN STREAM / URL
        // =====================================

        if (
            Intent.ACTION_VIEW == action
        ) {

            val data =
                intent?.dataString

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

                Log.e(
                    "VIEW_IMPORT",
                    data
                )

                return
            }
        }

        // =====================================
        // FALLBACK EXTRA STREAM
        // =====================================

        val extraStream =
            intent?.getStringExtra(
                Intent.EXTRA_STREAM
            )

        if (
            !extraStream.isNullOrBlank()
        ) {

            binding.contentMain.urlInput
                .setText(extraStream)

            detectAndSaveUrl(
                extraStream
            )

            binding.contentMain.webview
                .loadUrl(extraStream)

            Log.e(
                "EXTRA_STREAM",
                extraStream
            )
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
        // WEBVIEW
        // =====================================

        binding.contentMain.webview.webViewClient =
            object : WebViewClient() {

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {

                    val url =
                        request?.url.toString()

                    detectAndSaveUrl(
                        url
                    )

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

                            // IMG

                            document
                                .querySelectorAll("img")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
                                    }
                                });

                            // VIDEO

                            document
                                .querySelectorAll("video")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
                                    }

                                    if (el.poster) {
                                        results.push(el.poster);
                                    }
                                });

                            // AUDIO

                            document
                                .querySelectorAll("audio")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
                                    }
                                });

                            // SOURCE

                            document
                                .querySelectorAll("source")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
                                    }
                                });

                            // CSS BACKGROUND IMAGE

                            document
                                .querySelectorAll("*")
                                .forEach(function(el) {

                                    let style =
                                        window.getComputedStyle(el);

                                    let bg =
                                        style.backgroundImage;

                                    if (
                                        bg &&
                                        bg.includes("url(")
                                    ) {

                                        results.push(bg);
                                    }
                                });

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
                    }
                }
            }

        binding.contentMain.webview.settings.javaScriptEnabled =
            true

        binding.contentMain.webview.settings.domStorageEnabled =
            true

        binding.contentMain.webview.settings.mediaPlaybackRequiresUserGesture =
            false

// =====================================
// OPEN PAGE / SEARCH
// =====================================

binding.contentMain.openBrowser.setOnClickListener {

    detectedStreams.clear()

    detectedVideos.clear()

    detectedImages.clear()

    detectedAudio.clear()

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

    Log.e(
        "OPEN_PAGE",
        finalUrl
    )

    binding.contentMain.webview.loadUrl(
        finalUrl
    )
}

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

    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(finalUrl)
        )
    )
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

            try {

                SnooperRepo(this)
                    .deleteAll()

            } catch (t: Throwable) {

                Log.e(
                    "CLEAR",
                    "db clear failed",
                    t
                )
            }
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

        val lower =
            url.lowercase()

        // VIDEO

        val isVideo =
            lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains(".m4s") ||
            lower.contains(".ts") ||
            lower.contains(".mp4") ||
            lower.contains(".webm") ||
            lower.contains(".mkv") ||
            lower.contains("playlist") ||
            lower.contains("chunklist")

        // IMAGE

        val isImage =
            lower.contains(".jpg") ||
            lower.contains(".jpeg") ||
            lower.contains(".png") ||
            lower.contains(".webp") ||
            lower.contains(".gif") ||
            lower.contains(".bmp") ||
            lower.contains(".svg") ||
            lower.contains(".ico")

        // AUDIO

        val isAudio =
            lower.contains(".mp3") ||
            lower.contains(".m4a") ||
            lower.contains(".aac") ||
            lower.contains(".opus") ||
            lower.contains(".wav") ||
            lower.contains(".ogg") ||
            lower.contains(".flac")

        if (
            !isVideo &&
            !isImage &&
            !isAudio
        ) {
            return
        }

        // UNIQUE CACHE

        if (isVideo) {

            if (
                detectedVideos.contains(url)
            ) {
                return
            }

            detectedVideos.add(url)
        }

        if (isImage) {

            if (
                detectedImages.contains(url)
            ) {
                return
            }

            detectedImages.add(url)
        }

        if (isAudio) {

            if (
                detectedAudio.contains(url)
            ) {
                return
            }

            detectedAudio.add(url)
        }

        detectedStreams.add(url)

        val mediaType =
            when {

                isVideo -> "VIDEO"

                isImage -> "IMAGE"

                isAudio -> "AUDIO"

                else -> "MEDIA"
            }

        runOnUiThread {

            binding.contentMain.result.append(
                "\n\n$mediaType:\n$url\n"
            )
        }

        Log.e(
            "MEDIA_FOUND",
            "$mediaType -> $url"
        )

        // SAVE DB

        try {

            val repo =
                SnooperRepo(
                    this@MainActivity
                )

            repo.save(
                HttpCallRecord(
                    url = url,
                    method = "GET",
                    responseBody =
                        "$mediaType DETECTED",
                    statusCode = 200,
                    statusText = "OK",
                    date = Date()
                )
            )

        } catch (t: Throwable) {

            Log.e(
                "SNOOPER_DB",
                "save failed",
                t
            )
        }
    }

    // =====================================
    // SHOW ALL
    // =====================================

    private fun showAllMedia() {

        val sb = StringBuilder()

        detectedStreams.forEach {

            sb.append("\n\nMEDIA:\n")
            sb.append(it)
            sb.append("\n")
        }

        binding.contentMain.result.text =
            sb.toString()
    }

    // =====================================
    // SHOW VIDEOS
    // =====================================

    private fun showVideos() {

        val sb = StringBuilder()

        detectedVideos.forEach {

            sb.append("\n\nVIDEO:\n")
            sb.append(it)
            sb.append("\n")
        }

        binding.contentMain.result.text =
            sb.toString()
    }

    // =====================================
    // SHOW IMAGES
    // =====================================

    private fun showImages() {

        val sb = StringBuilder()

        detectedImages.forEach {

            sb.append("\n\nIMAGE:\n")
            sb.append(it)
            sb.append("\n")
        }

        binding.contentMain.result.text =
            sb.toString()
    }

    // =====================================
    // SHOW AUDIO
    // =====================================

    private fun showAudio() {

        val sb = StringBuilder()

        detectedAudio.forEach {

            sb.append("\n\nAUDIO:\n")
            sb.append(it)
            sb.append("\n")
        }

        binding.contentMain.result.text =
            sb.toString()
    }

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

        executeRequest(
            request
        )
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

        executeRequest(
            request
        )
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

        executeRequest(
            request
        )
    }

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

                            if (!response.isSuccessful) {
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

                                    binding.contentMain.result.text =
                                        text
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
