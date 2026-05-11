package com.prateekj.snooper.networksnooper.activity

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.prateekj.snooper.R
import com.prateekj.snooper.databinding.ActivityHttpCallDetailBinding
import com.prateekj.snooper.formatter.ResponseFormatterFactory
import com.prateekj.snooper.infra.AppPermissionChecker
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.fragment.HttpCallFragment
import com.prateekj.snooper.networksnooper.fragment.HttpHeadersFragment
import com.prateekj.snooper.networksnooper.helper.DataCopyHelper
import com.prateekj.snooper.networksnooper.helper.HttpCallRenderer
import com.prateekj.snooper.networksnooper.presenter.HttpCallPresenter
import com.prateekj.snooper.networksnooper.views.HttpCallView
import com.prateekj.snooper.utils.FileUtil
import java.io.File

class HttpCallActivity :
    SnooperBaseActivity(),
    HttpCallView {

    private lateinit var binding:
            ActivityHttpCallDetailBinding

    private var httpCallPresenter:
            HttpCallPresenter? = null

    private var httpCallRenderer:
            HttpCallRenderer? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityHttpCallDetailBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        setSupportActionBar(
            binding.toolbar
        )

        supportActionBar
            ?.setDisplayHomeAsUpEnabled(true)

        val httpCallId =
            intent.getLongExtra(
                HTTP_CALL_ID,
                0L
            )

        if (httpCallId <= 0L) {

            finish()

            return
        }

        val repo =
            SnooperRepo(this)

        val httpCall =
            repo.findById(httpCallId)

        val fileUtil =
            FileUtil()

        val backgroundTaskExecutor =
            BackgroundTaskExecutor(this)

        val dataCopyHelper =
            DataCopyHelper(
                this,
                httpCall,
                ResponseFormatterFactory(),
                resources
            )

        httpCallPresenter =
            HttpCallPresenter(
                dataCopyHelper,
                httpCall,
                this,
                fileUtil,
                backgroundTaskExecutor
            )

        val hasError =
            !httpCall.error.isNullOrBlank()

        httpCallRenderer =
            HttpCallRenderer(
                this,
                hasError
            )

        setupUi()
    }

    private fun setupUi() {

        val renderer =
            httpCallRenderer
                ?: return

        for (tab in renderer.getTabs()) {

            binding.tabLayout.addTab(
                binding.tabLayout
                    .newTab()
                    .setText(tab.tabTitle)
            )
        }

        binding.tabLayout.tabGravity =
            TabLayout.GRAVITY_FILL

        val adapter =
            HttpCallPagerAdapter(
                supportFragmentManager
            )

        binding.pager.adapter =
            adapter

        binding.pager.addOnPageChangeListener(
            TabLayout.TabLayoutOnPageChangeListener(
                binding.tabLayout
            )
        )

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(
                    tab: Tab
                ) {

                    binding.pager.currentItem =
                        tab.position
                }

                override fun onTabUnselected(
                    tab: Tab
                ) {
                }

                override fun onTabReselected(
                    tab: Tab
                ) {
                }
            }
        )
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {

        menuInflater.inflate(
            R.menu.http_call_menu,
            menu
        )

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        when (item.itemId) {

            R.id.copy_menu -> {

                val renderer =
                    httpCallRenderer
                        ?: return true

                val currentTab =
                    renderer.getTabs()[
                        binding.pager.currentItem
                    ]

                httpCallPresenter
                    ?.copyHttpCallBody(currentTab)

                return true
            }

            R.id.share_menu -> {

                shareHttpCallData()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun copyToClipboard(
        data: String
    ) {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "Copied",
                data
            )

        clipboard.setPrimaryClip(clip)
    }

    override fun shareData(
        logFilePath: String
    ) {

        val file =
            File(logFilePath)

        val fileUri =
            FileProvider.getUriForFile(
                this,
                "$packageName.snooper.provider",
                file
            )

        val intent =
            Intent(ACTION_SEND).apply {

                setDataAndType(
                    fileUri,
                    LOGFILE_MIME_TYPE
                )

                putExtra(
                    EXTRA_SUBJECT,
                    getString(
                        R.string.mail_subject_share_logs
                    )
                )

                putExtra(
                    EXTRA_STREAM,
                    fileUri
                )

                addFlags(
                    FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                getString(
                    R.string.chooser_title_share_logs
                )
            )
        )
    }

    override fun showMessageShareNotAvailable() {

        Toast.makeText(
            this,
            R.string.permission_not_granted,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareHttpCallData() {

        appPermissionChecker.handlePermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE,
            object :
                AppPermissionChecker.PermissionRequestCallBack {

                override fun permissionGranted() {

                    httpCallPresenter
                        ?.shareHttpCallBody()
                }

                override fun permissionDenied() {

                    httpCallPresenter
                        ?.onPermissionDenied()
                }
            }
        )
    }

    override fun getResponseBodyFragment():
            Fragment {

        return HttpCallFragment().apply {

            arguments =
                Bundle(intent.extras ?: Bundle()).apply {

                    putInt(
                        HTTP_CALL_MODE,
                        RESPONSE_MODE
                    )
                }
        }
    }

    override fun getRequestBodyFragment():
            Fragment {

        return HttpCallFragment().apply {

            arguments =
                Bundle(intent.extras ?: Bundle()).apply {

                    putInt(
                        HTTP_CALL_MODE,
                        REQUEST_MODE
                    )
                }
        }
    }

    override fun getHeadersFragment():
            Fragment {

        return HttpHeadersFragment().apply {

            arguments =
                Bundle(intent.extras ?: Bundle())
        }
    }

    override fun getExceptionFragment():
            Fragment {

        return HttpCallFragment().apply {

            arguments =
                Bundle(intent.extras ?: Bundle()).apply {

                    putInt(
                        HTTP_CALL_MODE,
                        ERROR_MODE
                    )
                }
        }
    }

    private inner class HttpCallPagerAdapter(
        fm: FragmentManager
    ) : FragmentStatePagerAdapter(
        fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {

        override fun getItem(
            position: Int
        ): Fragment {

            return httpCallRenderer
                ?.getFragment(position)
                ?: Fragment()
        }

        override fun getCount(): Int {

            return httpCallRenderer
                ?.getTabs()
                ?.size
                ?: 0
        }
    }

    companion object {

        const val HTTP_CALL_ID =
            "HTTP_CALL_ID"

        const val HTTP_CALL_MODE =
            "HTTP_CALL_MODE"

        const val REQUEST_MODE =
            1

        const val RESPONSE_MODE =
            2

        const val ERROR_MODE =
            3

        const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE =
            1

        private const val LOGFILE_MIME_TYPE =
            "*/*"
    }
}
