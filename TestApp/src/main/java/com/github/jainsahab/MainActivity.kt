package com.github.jainsahab

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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

                    Log.e(
                        "M3U8_DETECT",
                        url
                    )

                    val isStream =
                        url.contains(".m3u8") ||
                        url.contains(".mpd") ||
                        url.contains(".m4s") ||
                        url.contains(".ts") ||
                        url.contains("playlist") ||
                        url.contains("chunklist")

                    if (
                        isStream &&
                        !detectedStreams.contains(url)
                    ) {

                        detectedStreams.add(url)

                        runOnUiThread {

                            binding.contentMain.result.append(
                                "\n\nSTREAM:\n$url\n"
                            )
                        }

                        Log.e(
                            "STREAM_FOUND",
                            url
                        )

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
                                        "STREAM DETECTED",
                                    statusCode = 200,
                                    statusText = "OK",
                                    date = Date()
                                )
                            )

                            Log.e(
                                "SNOOPER_DB",
                                "saved -> $url"
                            )

                        } catch (t: Throwable) {

                            Log.e(
                                "SNOOPER_DB",
                                "save failed",
                                t
                            )
                        }
                    }

                    return super.shouldInterceptRequest(
                        view,
                        request
                    )
                }
            }

        binding.contentMain.webview.settings.javaScriptEnabled =
            true

        binding.contentMain.webview.settings.domStorageEnabled =
            true

        binding.contentMain.webview.settings.mediaPlaybackRequiresUserGesture =
            false

        binding.contentMain.openBrowser.setOnClickListener {

            detectedStreams.clear()

            val enteredUrl =
                binding.contentMain.urlInput
                    .text
                    .toString()
                    .trim()

            val finalUrl =
                if (enteredUrl.isEmpty()) {

                    "https://hlsjs.video-dev.org/demo/"

                } else {

                    enteredUrl
                }

            binding.contentMain.result.text = ""

            binding.contentMain.webview.loadUrl(
                finalUrl
            )
        }
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
