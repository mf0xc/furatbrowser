package com.furat.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var ivSecure: ImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var noInternetView: LinearLayout
    private lateinit var btnRetry: MaterialButton
    private lateinit var splashContainer: FrameLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var navExplore: LinearLayout
    private lateinit var navFavorites: LinearLayout
    private lateinit var navHistory: LinearLayout
    private lateinit var navAccount: LinearLayout

    private lateinit var viewModel: BrowserViewModel
    private val adBlocker = AdBlocker()
    private var isLoadingPage = false
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]
        initViews()
        setupWebView()
        setupListeners()
        setupObservers()
        showSplash()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnMenu = findViewById(R.id.btnMenu)
        btnClear = findViewById(R.id.btnClear)
        ivSecure = findViewById(R.id.ivSecure)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        noInternetView = findViewById(R.id.noInternetView)
        btnRetry = findViewById(R.id.btnRetry)
        splashContainer = findViewById(R.id.splashContainer)
        mainContent = findViewById(R.id.mainContent)
        navExplore = findViewById(R.id.navExplore)
        navFavorites = findViewById(R.id.navFavorites)
        navHistory = findViewById(R.id.navHistory)
        navAccount = findViewById(R.id.navAccount)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadsImagesAutomatically = viewModel.downloadImages
            textZoom = viewModel.fontSizePercent
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 FuratBrowser/1.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.let { url ->
                    val urlString = url.toString()
                    when {
                        urlString.startsWith("http://") || urlString.startsWith("https://") -> {
                            view?.loadUrl(urlString)
                            return true
                        }
                        urlString.startsWith("intent://") -> {
                            try {
                                val intent = Intent.parseUri(urlString, Intent.URI_INTENT_SCHEME)
                                startActivity(intent)
                            } catch (e: Exception) {
                                showToast("لا يمكن فتح هذا الرابط")
                            }
                            return true
                        }
                        else -> {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, url)
                                startActivity(Intent.createChooser(intent, getString(R.string.open_in)))
                            } catch (e: Exception) {
                                showToast("لا يمكن فتح هذا الرابط")
                            }
                            return true
                        }
                    }
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (viewModel.isAdBlockerEnabled) {
                    request?.url?.toString()?.let { url ->
                        if (adBlocker.isAd(url)) {
                            return adBlocker.createEmptyResponse()
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    viewModel.updateUrl(it)
                    etUrl.setText(it)
                    viewModel.addHistory(view?.title ?: "", it)
                    updateNavigationButtons()
                }
                swipeRefresh.isRefreshing = false
                isLoadingPage = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (!isInternetAvailable()) showNoInternet()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                viewModel.updateProgress(newProgress)
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                progressBar.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { viewModel.addHistory(it, view?.url ?: "") }
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                val intent = fileChooserParams?.createIntent()
                try {
                    startActivityForResult(intent, 100)
                    this@MainActivity.filePathCallback = filePathCallback
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                }
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            showToast(getString(R.string.download_started))
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("جاري التحميل...")
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2 != null) {
                    val diffX = e2.x - e1.x
                    if (kotlin.math.abs(diffX) > 100 && kotlin.math.abs(diffX) > kotlin.math.abs(e2.y - e1.y)) {
                        if (diffX > 0 && e1.x < 50) {
                            if (webView.canGoBack()) webView.goBack()
                        } else if (diffX < 0 && e1.x > resources.displayMetrics.widthPixels - 50) {
                            if (webView.canGoForward()) webView.goForward()
                        }
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupListeners() {
        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrl(etUrl.text.toString())
                hideKeyboard()
                true
            } else false
        }

        etUrl.setOnFocusChangeListener { _, hasFocus ->
            etUrl.setBackgroundResource(if (hasFocus) R.drawable.url_bar_focused else R.drawable.url_bar_bg)
            if (hasFocus) etUrl.selectAll()
        }

        btnClear.setOnClickListener { etUrl.setText(""); etUrl.requestFocus() }

        etUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClear.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        btnRefresh.setOnClickListener {
            if (isLoadingPage) {
                webView.stopLoading()
                btnRefresh.setImageResource(R.drawable.ic_refresh)
            } else {
                webView.reload()
                btnRefresh.setImageResource(R.drawable.ic_stop)
            }
        }

        btnMenu.setOnClickListener { showPopupMenu() }
        swipeRefresh.setOnRefreshListener { webView.reload() }
        swipeRefresh.setColorSchemeResources(R.color.accent)

        btnRetry.setOnClickListener {
            if (isInternetAvailable()) {
                hideNoInternet()
                webView.reload()
            } else {
                showToast(getString(R.string.no_internet))
            }
        }

        navExplore.setOnClickListener { updateNavSelection(0); hideNoInternet() }
        navFavorites.setOnClickListener { showBookmarksSheet() }
        navHistory.setOnClickListener { showHistorySheet() }
        navAccount.setOnClickListener { showAccountSheet() }
    }

    private fun setupObservers() {
        viewModel.canGoBack.observe(this) { btnBack.isEnabled = it }
        viewModel.canGoForward.observe(this) { btnForward.isEnabled = it }
        viewModel.isSecure.observe(this) { ivSecure.visibility = if (it) View.VISIBLE else View.GONE }
    }

    private fun showSplash() {
        Handler(Looper.getMainLooper()).postDelayed({
            splashContainer.animate().alpha(0f).setDuration(500).withEndAction {
                splashContainer.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
                mainContent.alpha = 0f
                mainContent.animate().alpha(1f).setDuration(300).start()
                if (isInternetAvailable()) loadUrl("https://www.google.com") else showNoInternet()
            }.start()
        }, 1500)
    }

    private fun loadUrl(input: String) {
        if (!isInternetAvailable()) { showNoInternet(); return }
        hideNoInternet()
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> viewModel.searchEngine + input.replace(" ", "+")
        }
        webView.loadUrl(url)
        isLoadingPage = true
    }

    private fun updateNavigationButtons() {
        viewModel.updateNavigationState(webView.canGoBack(), webView.canGoForward())
    }

    private fun showPopupMenu() {
        val popup = PopupMenu(this, btnMenu)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_share -> shareUrl()
                R.id.menu_copy -> copyUrl()
                R.id.menu_open_external -> openExternal()
                R.id.menu_add_favorite -> addToFavorites()
                R.id.menu_settings -> showSettings()
                R.id.menu_about -> showAbout()
            }
            true
        }
        popup.show()
    }

    private fun shareUrl() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, webView.url)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun copyUrl() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("URL", webView.url))
        showToast(getString(R.string.url_copied))
    }

    private fun openExternal() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView.url))
        startActivity(Intent.createChooser(intent, getString(R.string.open_in)))
    }

    private fun addToFavorites() {
        viewModel.addBookmark(webView.title ?: "", webView.url ?: "") { added ->
            showToast(if (added) getString(R.string.bookmark_added) else getString(R.string.bookmark_exists))
        }
    }

    private fun showBookmarksSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        val title = view.findViewById<TextView>(R.id.sheetTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAdd = view.findViewById<ImageButton>(R.id.btnAdd)
        val btnClear = view.findViewById<MaterialButton>(R.id.btnClear)
        val emptyView = view.findViewById<TextView>(R.id.emptyView)

        title.text = getString(R.string.favorites)
        btnAdd.visibility = View.VISIBLE
        val bookmarks = viewModel.getBookmarks()

        if (bookmarks.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = BookmarkAdapter(bookmarks, { bookmark ->
                loadUrl(bookmark.url); dialog.dismiss()
            }, { bookmark ->
                viewModel.deleteBookmark(bookmark)
                dialog.dismiss()
                showBookmarksSheet()
            })
        }

        btnAdd.setOnClickListener { addToFavorites(); dialog.dismiss() }
        btnClear.setOnClickListener {
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("مسح المفضلة").setMessage("هل أنت متأكد؟")
                .setPositiveButton("نعم") { _, _ -> viewModel.clearBookmarks(); dialog.dismiss() }
                .setNegativeButton("لا", null).show()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showHistorySheet() {
        val dialog = BottomSheetDialog(this, R.style.AlertDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        val title = view.findViewById<TextView>(R.id.sheetTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAdd = view.findViewById<ImageButton>(R.id.btnAdd)
        val btnClear = view.findViewById<MaterialButton>(R.id.btnClear)
        val emptyView = view.findViewById<TextView>(R.id.emptyView)

        title.text = getString(R.string.history)
        btnAdd.visibility = View.GONE
        val history = viewModel.getHistory()

        if (history.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = HistoryAdapter(history, { item ->
                loadUrl(item.url); dialog.dismiss()
            }, { item ->
                viewModel.deleteHistoryItem(item)
                dialog.dismiss()
                showHistorySheet()
            })
        }

        btnClear.text = getString(R.string.clear_history)
        btnClear.setOnClickListener {
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("مسح التاريخ").setMessage("هل أنت متأكد؟")
                .setPositiveButton("نعم") { _, _ -> viewModel.clearHistory(); dialog.dismiss() }
                .setNegativeButton("لا", null).show()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showAccountSheet() {
        val dialog = BottomSheetDialog(this, R.style.AlertDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_account, null)
        view.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showSettings() {
        val dialog = BottomSheetDialog(this, R.style.AlertDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_settings, null)
        val switchReading = view.findViewById<SwitchMaterial>(R.id.switchReading)
        val switchAdBlock = view.findViewById<SwitchMaterial>(R.id.switchAdBlock)
        val switchDns = view.findViewById<SwitchMaterial>(R.id.switchDns)
        val switchImages = view.findViewById<SwitchMaterial>(R.id.switchImages)
        val sliderFont = view.findViewById<Slider>(R.id.sliderFont)
        val spinnerSearch = view.findViewById<Spinner>(R.id.spinnerSearch)
        val btnClearData = view.findViewById<MaterialButton>(R.id.btnClearData)

        switchReading.isChecked = viewModel.isReadingModeEnabled
        switchAdBlock.isChecked = viewModel.isAdBlockerEnabled
        switchDns.isChecked = viewModel.isDnsEncrypted
        switchImages.isChecked = viewModel.downloadImages
        sliderFont.value = viewModel.fontSizePercent.toFloat()

        val searchEngines = arrayOf("Google", "Bing", "DuckDuckGo")
        val searchUrls = arrayOf(
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q="
        )
        spinnerSearch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, searchEngines).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerSearch.setSelection(searchUrls.indexOf(viewModel.searchEngine).coerceAtLeast(0))

        switchReading.setOnCheckedChangeListener { _, checked ->
            viewModel.isReadingModeEnabled = checked
            if (checked) webView.evaluateJavascript(
                "document.body.style.maxWidth='800px';document.body.style.margin='0 auto';document.body.style.padding='20px';document.body.style.fontSize='18px';document.body.style.lineHeight='1.8';", null)
        }

        switchAdBlock.setOnCheckedChangeListener { _, checked ->
            viewModel.isAdBlockerEnabled = checked
            webView.reload()
        }

        switchDns.setOnCheckedChangeListener { _, checked ->
            viewModel.isDnsEncrypted = checked
            showToast("DNS ${if (checked) "مفعّل" else "معطّل"}")
        }

        switchImages.setOnCheckedChangeListener { _, checked ->
            viewModel.downloadImages = checked
            webView.settings.loadsImagesAutomatically = checked
            webView.reload()
        }

        sliderFont.addOnChangeListener { _, value, _ ->
            viewModel.fontSizePercent = value.toInt()
            webView.settings.textZoom = value.toInt()
        }

        spinnerSearch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.searchEngine = searchUrls[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnClearData.setOnClickListener {
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("مسح البيانات").setMessage("هل أنت متأكد من مسح كل البيانات؟")
                .setPositiveButton("نعم") { _, _ ->
                    viewModel.clearAllData()
                    webView.clearCache(true)
                    webView.clearHistory()
                    CookieManager.getInstance().removeAllCookies(null)
                    dialog.dismiss()
                    showToast("تم مسح البيانات")
                }
                .setNegativeButton("لا", null).show()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Furat Browser")
            .setMessage("${getString(R.string.version)}
${getString(R.string.developer)}")
            .setPositiveButton("حسناً", null).show()
    }

    private fun updateNavSelection(index: Int) {
        val navs = listOf(navExplore, navFavorites, navHistory, navAccount)
        navs.forEachIndexed { i, nav ->
            val icon = nav.getChildAt(0) as ImageView
            val text = nav.getChildAt(1) as TextView
            val color = if (i == index) ContextCompat.getColor(this, R.color.accent) else ContextCompat.getColor(this, R.color.text_secondary)
            icon.setColorFilter(color)
            text.setTextColor(color)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternet() {
        noInternetView.visibility = View.VISIBLE
        webView.visibility = View.GONE
    }

    private fun hideNoInternet() {
        noInternetView.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etUrl.windowToken, 0)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            filePathCallback?.onReceiveValue(
                if (data == null || resultCode != RESULT_OK) null else
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            filePathCallback = null
        }
    }
}

class BookmarkAdapter(
    private val items: List<Bookmark>,
    private val onClick: (Bookmark) -> Unit,
    private val onDelete: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemTitle)
        val url: TextView = view.findViewById(R.id.itemUrl)
        val delete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.url.text = item.url
        holder.itemView.setOnClickListener { onClick(item) }
        holder.delete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}

class HistoryAdapter(
    private val items: List<HistoryItem>,
    private val onClick: (HistoryItem) -> Unit,
    private val onDelete: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemTitle)
        val url: TextView = view.findViewById(R.id.itemUrl)
        val time: TextView = view.findViewById(R.id.itemTime)
        val delete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.url.text = item.url
        holder.time.text = dateFormat.format(Date(item.visitTime))
        holder.itemView.setOnClickListener { onClick(item) }
        holder.delete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}
