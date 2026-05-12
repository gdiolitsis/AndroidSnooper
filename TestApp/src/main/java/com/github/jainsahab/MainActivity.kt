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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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

    // =====================================
    // ON CREATE
    // =====================================

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

        // =====================================
        // WEBVIEW SETTINGS
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
        // WEBVIEW CLIENT
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

                            document
                                .querySelectorAll("img")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
                                    }
                                });

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

                            document
                                .querySelectorAll("audio")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
                                    }
                                });

                            document
                                .querySelectorAll("source")
                                .forEach(function(el) {

                                    if (el.src) {
                                        results.push(el.src);
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

    val streamList =
    detectedStreams.toTypedArray()

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
    "*/*"

        lower.contains(".mpd") ->
    "*/*"

        lower.contains(".mp4") ->
            "video/mp4"

        lower.contains(".mkv") ->
            "video/x-matroska"

        lower.contains(".webm") ->
            "video/webm"

        lower.contains(".ts") ->
    "*/*"

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

            data = Uri.parse(cleanUrl)

            type = mimeType

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

    val streamList =
    detectedStreams.toTypedArray()

    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Select Stream To Share")

        .setItems(streamList) { _, which ->

            val url =
                streamList[which]

            try {

                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            url
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
        }

        .show()
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

        if (
            !isVideo &&
            !isImage &&
            !isAudio
        ) {
            return
        }

        if (
            detectedStreams.contains(url)
        ) {
            return
        }

        detectedStreams.add(url)

        if (isVideo) {
            detectedVideos.add(url)
        }

        if (isImage) {
            detectedImages.add(url)
        }

        if (isAudio) {
            detectedAudio.add(url)
        }

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

        try {

            SnooperRepo(this)
                .save(
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
