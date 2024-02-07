package com.emretaskesen.tpost.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME , null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "notifications.db"
        private const val TABLE_NOTIFICATIONS = "notifications"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_IMAGE = "image"
        private const val KEY_RECEIVED_AT = "received_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createNotificationsTable = ("CREATE TABLE " + TABLE_NOTIFICATIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_BODY + " TEXT,"
                + KEY_IMAGE + " TEXT," // Buradaki eksik boşluğu düzelttim
                + KEY_RECEIVED_AT + " INTEGER" + ")")
        db.execSQL(createNotificationsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Yükseltme senaryosu
    }

    fun addNotification(title: String, body: String, imageUrl: String?) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(KEY_TITLE , title)
                put(KEY_BODY , body)
                put(KEY_IMAGE , imageUrl ?: "")
                put(KEY_RECEIVED_AT , System.currentTimeMillis())
            }
            db.insert(TABLE_NOTIFICATIONS , null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding notification", e)
        } finally {
            db.endTransaction()
        }
        db.close()
    }


    fun getAllNotifications(): Cursor {
        val db = this.readableDatabase
        return db.query(TABLE_NOTIFICATIONS , null, null, null, null, null, "$KEY_RECEIVED_AT DESC")
    }
}