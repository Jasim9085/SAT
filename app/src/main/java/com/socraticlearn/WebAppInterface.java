package com.socraticlearn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class WebAppInterface {
    private static final String LOG_TAG = "WebAppInterface";
    private final Context context;
    private final WebView webView;
    private final DatabaseHelper dbHelper;

    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.dbHelper = new DatabaseHelper(context);
    }

    @JavascriptInterface
    public void saveSession(String sessionData) {
        Log.d(LOG_TAG, "saveSession called");
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            JSONObject json = new JSONObject(sessionData);
            String sessionId = json.getString("id");
            long updatedAt = json.getLong("updatedAt");
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_SESSION_ID, sessionId);
            values.put(DatabaseHelper.COLUMN_UPDATED_AT, updatedAt);
            values.put(DatabaseHelper.COLUMN_SESSION_JSON_DATA, sessionData);
            db.replace(DatabaseHelper.TABLE_SESSIONS, null, values);
            copyDatabaseToExternalStorage();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error saving session", e);
        }
    }

    @JavascriptInterface
    public String loadAllSessions() {
        Log.d(LOG_TAG, "loadAllSessions called");
        JSONArray sessionsArray = new JSONArray();
        String query = "SELECT " + DatabaseHelper.COLUMN_SESSION_JSON_DATA + " FROM " + DatabaseHelper.TABLE_SESSIONS + " ORDER BY " + DatabaseHelper.COLUMN_UPDATED_AT + " DESC";

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String jsonData = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SESSION_JSON_DATA));
                    sessionsArray.put(jsonData);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error loading sessions", e);
        }
        return sessionsArray.toString();
    }

    @JavascriptInterface
    public void deleteSession(String sessionId) {
        Log.d(LOG_TAG, "deleteSession called for ID: " + sessionId);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.delete(DatabaseHelper.TABLE_SESSIONS, DatabaseHelper.COLUMN_SESSION_ID + " = ?", new String[]{sessionId});
            copyDatabaseToExternalStorage();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error deleting session", e);
        }
    }

    @JavascriptInterface
    public void clearAllSessions() {
        Log.d(LOG_TAG, "clearAllSessions called");
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.delete(DatabaseHelper.TABLE_SESSIONS, null, null);
            copyDatabaseToExternalStorage();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error clearing all sessions", e);
        }
    }

    @JavascriptInterface
    public void saveSettings(String data) {
        Log.d(LOG_TAG, "saveSettings called");
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_SETTINGS_ID, 1);
            values.put(DatabaseHelper.COLUMN_SETTINGS_JSON_DATA, data);
            db.replace(DatabaseHelper.TABLE_SETTINGS, null, values);
            copyDatabaseToExternalStorage();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error saving settings", e);
        }
    }

    @JavascriptInterface
    public String loadSettings() {
        Log.d(LOG_TAG, "loadSettings called");
        String settingsJson = "";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query(DatabaseHelper.TABLE_SETTINGS, new String[]{DatabaseHelper.COLUMN_SETTINGS_JSON_DATA},
                     DatabaseHelper.COLUMN_SETTINGS_ID + " = ?", new String[]{"1"}, null, null, null)) {
            if (cursor.moveToFirst()) {
                settingsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SETTINGS_JSON_DATA));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error loading settings", e);
        }
        return settingsJson;
    }

    @JavascriptInterface
    public void reloadApp() {
        new Handler(Looper.getMainLooper()).post(() -> webView.reload());
    }

    @JavascriptInterface
    public void saveFile(String fileName, String mimeType, String content) {
        Log.d(LOG_TAG, "saveFile called for: " + fileName);
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        }
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                    showToast("File saved to Downloads: " + fileName);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error saving file", e);
                showToast("Failed to save file.");
            }
        } else {
            showToast("Failed to create file entry.");
        }
    }

    private void copyDatabaseToExternalStorage() {
        Log.d(LOG_TAG, "Attempting to copy database to external storage.");
        try {
            File internalDbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
            File externalDir = new File(Environment.getExternalStorageDirectory(), "SocLearn");
            if (!externalDir.exists()) {
                if (!externalDir.mkdirs()) {
                    Log.e(LOG_TAG, "Failed to create external directory.");
                    return;
                }
            }
            File externalDbFile = new File(externalDir, DatabaseHelper.DATABASE_NAME);
            if (internalDbFile.exists()) {
                try (FileChannel src = new FileInputStream(internalDbFile).getChannel();
                     FileChannel dst = new FileOutputStream(externalDbFile).getChannel()) {
                    dst.transferFrom(src, 0, src.size());
                    Log.d(LOG_TAG, "Database copied successfully to " + externalDbFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error copying database to external storage", e);
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}