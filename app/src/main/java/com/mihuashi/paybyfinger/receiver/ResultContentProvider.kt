package com.mihuashi.paybyfinger.receiver

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class ResultContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.mihuashi.paybyfinger.provider"
        private const val RESULTS = 1
        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "results", RESULTS)
        }

        // 用于模拟存储结果数据的变量
        private var resultData: Boolean? = null
        private var errorMessage: String? = null
        private var timestamp: Long? = null // 时间戳，表示认证的时间

        // 定义列名
        private const val COLUMN_RESULT = "result"
        private const val COLUMN_ERROR_MESSAGE = "error_message"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        when (URI_MATCHER.match(uri)) {
            RESULTS -> {
                // 提取值并保存
                resultData = values?.getAsBoolean(COLUMN_RESULT)
                errorMessage = values?.getAsString(COLUMN_ERROR_MESSAGE)
                timestamp = values?.getAsLong(COLUMN_TIMESTAMP)

                context?.contentResolver?.notifyChange(uri, null)
                return uri
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        return when (URI_MATCHER.match(uri)) {
            RESULTS -> {
                // 使用 MatrixCursor 返回查询结果
                val cursor = MatrixCursor(arrayOf(COLUMN_RESULT, COLUMN_ERROR_MESSAGE, COLUMN_TIMESTAMP))
                cursor.addRow(arrayOf(resultData, errorMessage, timestamp))
                cursor
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Not supported")
    }
}

