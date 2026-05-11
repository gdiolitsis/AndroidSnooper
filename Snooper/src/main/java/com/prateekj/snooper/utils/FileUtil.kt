package com.prateekj.snooper.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

class FileUtil(
    private val context: Context
) {

    fun createLogFile(
        content: StringBuilder,
        fileName: String
    ): String {

        return try {

            val file =
                File(
                    context.cacheDir,
                    fileName
                )

            if (file.exists()) {
                file.delete()
            }

            file.createNewFile()

            FileOutputStream(file).use { fos ->

                OutputStreamWriter(fos).use { writer ->

                    writer.append(
                        content.toString()
                    )

                    writer.flush()
                }
            }

            file.absolutePath

        } catch (e: IOException) {

            Logger.e(
                TAG,
                "Error while creating log file",
                e
            )

            ""
        }
    }

    companion object {

        private val TAG =
            FileUtil::class.java.simpleName
    }
}
