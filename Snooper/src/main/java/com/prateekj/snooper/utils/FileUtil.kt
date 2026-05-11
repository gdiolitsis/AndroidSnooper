package com.prateekj.snooper.utils

import android.content.Context
import java.io.File
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

            val safeFileName =
                sanitizeFileName(fileName)

            val file =
                File(
                    context.cacheDir,
                    safeFileName
                )

            if (
                file.exists() &&
                !file.delete()
            ) {

                Logger.e(
                    TAG,
                    "Failed to delete existing file"
                )

                return ""
            }

            file.outputStream().use { fos ->

                OutputStreamWriter(fos).use { writer ->

                    writer.write(
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

    private fun sanitizeFileName(
        fileName: String
    ): String {

        return fileName.replace(
            Regex("[^a-zA-Z0-9._-]"),
            "_"
        )
    }

    companion object {

        private val TAG =
            FileUtil::class.java.simpleName
    }
}
