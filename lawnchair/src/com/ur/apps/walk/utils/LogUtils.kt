package com.ur.apps.walk.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.ur.apps.utils.URLog
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志工具类，用于收集应用日志并通过邮件发送
 */
object LogUtils {
    private const val TAG = "LogUtils"
    private const val LOG_FOLDER = "logs"

    /**
     * 收集应用日志并分享
     *
     * @param context 上下文
     * @param title 分享标题
     * @param message 分享内容
     */
    fun collectAndShareLogs(
        context: Context,
        title: String = context.getString(com.ur.apps.walk.R.string.log_share_title_default),
        message: String = context.getString(com.ur.apps.walk.R.string.log_select_app_prompt)
    ) {
        // 在后台协程中执行耗时操作
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 显示进度提示
                Toast.makeText(context, context.getString(com.ur.apps.walk.R.string.log_collecting_toast), Toast.LENGTH_SHORT).show()

                // 在IO线程执行文件操作
                val (logFile, zipFile, zipFileUri) = withContext(Dispatchers.IO) {
                    // 创建日志文件
                    val logFile = createLogFile(context)

                    // 收集设备信息和应用日志
                    collectDeviceInfo(context, logFile)
                    collectAppLogs(logFile)

                    // 压缩日志文件
                    val zipFile = compressLogFile(context, logFile)

                    // 获取压缩文件的URI
                    val zipFileUri = getFileUri(context, zipFile)

                    Triple(logFile, zipFile, zipFileUri)
                }

                // 在主线程分享文件
                shareFile(context, zipFileUri, title, message)

                // 清理临时文件（延迟清理，确保分享完成）
                cleanupTempFilesDelayed(logFile, zipFile)

            } catch (e: Exception) {
                URLog.e(TAG, context.getString(com.ur.apps.walk.R.string.log_collect_share_failed), e)
                Toast.makeText(context, context.getString(com.ur.apps.walk.R.string.log_collect_failed_toast_format, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 创建日志文件
     */
    private fun createLogFile(context: Context): File {
        // 创建日志文件夹 - 使用外部缓存目录
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val logDir = File(cacheDir, LOG_FOLDER)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        // 创建日志文件，使用时间戳命名
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logFile = File(logDir, "app_log_$timestamp.txt")

        return logFile
    }

    /**
     * 收集设备信息
     */
    private fun collectDeviceInfo(context: Context, logFile: File) {
        try {
            FileOutputStream(logFile).use { fos ->
                PrintWriter(fos).use { pw ->
                    pw.println(context.getString(com.ur.apps.walk.R.string.log_device_info_header))
                    pw.println(context.getString(com.ur.apps.walk.R.string.log_time_label, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())))
                    pw.println(context.getString(com.ur.apps.walk.R.string.log_device_label, Build.MANUFACTURER, Build.MODEL))
                    pw.println(context.getString(com.ur.apps.walk.R.string.log_android_version_label, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
                    pw.println(context.getString(com.ur.apps.walk.R.string.log_device_id_label, Build.FINGERPRINT))
                    pw.println(context.getString(com.ur.apps.walk.R.string.log_separator) + "\n")
                }
            }
        } catch (e: IOException) {
            URLog.e(TAG, context.getString(com.ur.apps.walk.R.string.error), e)
        }
    }

    /**
     * 收集应用日志
     */
    private fun collectAppLogs(logFile: File) {
        try {
            // 使用Runtime执行logcat命令获取应用日志
            val process = Runtime.getRuntime().exec("logcat -d")
            process.inputStream.use { inputStream ->
                FileOutputStream(logFile, true).use { fos ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } != -1) {
                        fos.write(buffer, 0, length)
                    }
                }
            }
        } catch (e: IOException) {
            URLog.e(TAG, "Failed to collect app logs", e)
        }
    }

    /**
     * 获取文件URI
     */
    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 压缩日志文件
     */
    private fun compressLogFile(context: Context, logFile: File): File {
        if (!logFile.exists()) {
            throw IOException(context.getString(com.ur.apps.walk.R.string.log_file_not_exist_format, logFile.absolutePath))
        }

        val zipFileName = logFile.nameWithoutExtension + ".zip"
        val zipFile = File(logFile.parent, zipFileName)

        try {
            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val entry = ZipEntry(logFile.name)
                    zos.putNextEntry(entry)

                    FileInputStream(logFile).use { fis ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (fis.read(buffer).also { length = it } > 0) {
                            zos.write(buffer, 0, length)
                        }
                    }
                    zos.closeEntry()
                }
            }

            URLog.d(TAG, context.getString(com.ur.apps.walk.R.string.log_zip_success_format, logFile.length(), zipFile.length()))
            return zipFile
        } catch (e: Exception) {
            URLog.e(TAG, context.getString(com.ur.apps.walk.R.string.error), e)
            throw e
        }
    }

    /**
     * 清理临时文件
     */
    private fun cleanupTempFiles(vararg files: File) {
        for (file in files) {
            try {
                if (file.exists()) {
                    file.delete()
                    URLog.d(TAG, "Deleted temp file: ${file.name}")
                }
            } catch (e: Exception) {
                URLog.w(TAG, "Failed to delete temp file: ${file.name}", e)
            }
        }
    }

    /**
     * 分享文件
     */
    private fun shareFile(
        context: Context,
        fileUri: Uri,
        title: String,
        message: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(Intent.createChooser(intent, context.getString(com.ur.apps.walk.R.string.log_share_chooser_title)))
        } catch (e: Exception) {
            URLog.e(TAG, context.getString(R.string.log_share_failed_toast), e)
            Toast.makeText(context, context.getString(R.string.log_share_failed_toast), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 延迟清理临时文件
     */
    private fun cleanupTempFilesDelayed(vararg files: File) {
        // 使用Handler延迟5秒后清理文件，确保分享操作完成
        android.os.Handler().postDelayed({
            cleanupTempFiles(*files)
        }, 5000)
    }
}
