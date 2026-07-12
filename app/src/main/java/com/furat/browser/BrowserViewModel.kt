package com.furat.browser

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("furat_settings", Context.MODE_PRIVATE)
    private val db = BrowserDatabase.getInstance(application)

    // LiveData for UI state
    private val _currentUrl = MutableLiveData<String>("https://www.google.com")
    val currentUrl: LiveData<String> = _currentUrl

    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> = _progress

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _canGoBack = MutableLiveData<Boolean>(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData<Boolean>(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val _isSecure = MutableLiveData<Boolean>(false)
    val isSecure: LiveData<Boolean> = _isSecure

    // Settings
    var isAdBlockerEnabled: Boolean
        get() = prefs.getBoolean("ad_blocker", true)
        set(value) = prefs.edit().putBoolean("ad_blocker", value).apply()

    var isReadingModeEnabled: Boolean
        get() = prefs.getBoolean("reading_mode", false)
        set(value) = prefs.edit().putBoolean("reading_mode", value).apply()

    var isDnsEncrypted: Boolean
        get() = prefs.getBoolean("dns_encrypted", false)
        set(value) = prefs.edit().putBoolean("dns_encrypted", value).apply()

    var downloadImages: Boolean
        get() = prefs.getBoolean("download_images", true)
        set(value) = prefs.edit().putBoolean("download_images", value).apply()

    var fontSizePercent: Int
        get() = prefs.getInt("font_size", 100)
        set(value) = prefs.edit().putInt("font_size", value).apply()

    var searchEngine: String
        get() = prefs.getString("search_engine", "https://www.google.com/search?q=") ?: "https://www.google.com/search?q="
        set(value) = prefs.edit().putString("search_engine", value).apply()

    fun updateUrl(url: String) {
        _currentUrl.value = url
        _isSecure.value = url.startsWith("https://")
    }

    fun updateProgress(progress: Int) {
        _progress.value = progress
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun updateNavigationState(canBack: Boolean, canForward: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
    }

    // Bookmarks
    fun addBookmark(title: String, url: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = db.bookmarkDao().exists(url)
            if (!exists) {
                db.bookmarkDao().insert(Bookmark(title = title, url = url))
                withContext(Dispatchers.Main) { callback(true) }
            } else {
                withContext(Dispatchers.Main) { callback(false) }
            }
        }
    }

    fun getBookmarks(): List<Bookmark> {
        return db.bookmarkDao().getAll()
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            db.bookmarkDao().delete(bookmark)
        }
    }

    fun clearBookmarks() {
        viewModelScope.launch {
            db.bookmarkDao().deleteAll()
        }
    }

    // History
    fun addHistory(title: String, url: String) {
        viewModelScope.launch {
            db.historyDao().insert(HistoryItem(title = title, url = url))
        }
    }

    fun getHistory(): List<HistoryItem> {
        return db.historyDao().getAll()
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            db.historyDao().delete(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.historyDao().deleteAll()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            db.bookmarkDao().deleteAll()
            db.historyDao().deleteAll()
            prefs.edit().clear().apply()
        }
    }
}
