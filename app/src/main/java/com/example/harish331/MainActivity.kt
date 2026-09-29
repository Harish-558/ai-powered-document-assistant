package com.example.harish331

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.harish331.data.model.Document
import com.example.harish331.data.model.HistoryItem
import com.example.harish331.data.model.SearchResult
import com.example.harish331.viewmodel.DocumentViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val documentViewModel = DocumentViewModel()

    private lateinit var screenContainer: FrameLayout
    private lateinit var bottomNavDivider: View
    private lateinit var bottomNavContainer: LinearLayout
    private lateinit var navItemHome: LinearLayout
    private lateinit var navItemDocuments: LinearLayout
    private lateinit var navItemHistory: LinearLayout
    private lateinit var navIconHome: ImageView
    private lateinit var navIconDocuments: ImageView
    private lateinit var navIconHistory: ImageView
    private lateinit var navTextHome: TextView
    private lateinit var navTextDocuments: TextView
    private lateinit var navTextHistory: TextView

    private var currentScreenTag: String = "splash"

    // Dynamic State
    private var activeDocument: Document? = null
    private val uploadedDocuments = mutableListOf<Document>()
    private val historyList = mutableListOf<HistoryItem>()
    private var currentSearchResult: SearchResult? = null
    private var currentSearchError: String? = null
    private var lastQueryText: String = ""

    private val openDocPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        handlePickedFileUri(uri)
    }

    private val getContentPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handlePickedFileUri(uri)
    }

    private fun handlePickedFileUri(uri: Uri?) {
        if (uri != null) {
            val (name, size) = getFileNameAndSizeFromUri(uri)
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
            val doc = Document(
                id = "doc_${System.currentTimeMillis()}",
                filename = name,
                filesize = size,
                uploadDate = dateStr,
                uriString = uri.toString(),
                status = "Uploading..."
            )
            activeDocument = doc
            if (uploadedDocuments.none { it.filename == name }) {
                uploadedDocuments.add(0, doc)
            }

            Toast.makeText(
                this,
                "Uploading ${doc.filename} to server...",
                Toast.LENGTH_SHORT
            ).show()

            documentViewModel.resetUploadState()
            documentViewModel.uploadDocument(
                this,
                uri,
                doc.filename
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        screenContainer = findViewById(R.id.screenContainer)
        bottomNavDivider = findViewById(R.id.bottomNavDivider)
        bottomNavContainer = findViewById(R.id.bottomNavContainer)
        navItemHome = findViewById(R.id.navItemHome)
        navItemDocuments = findViewById(R.id.navItemDocuments)
        navItemHistory = findViewById(R.id.navItemHistory)
        navIconHome = findViewById(R.id.navIconHome)
        navIconDocuments = findViewById(R.id.navIconDocuments)
        navIconHistory = findViewById(R.id.navIconHistory)
        navTextHome = findViewById(R.id.navTextHome)
        navTextDocuments = findViewById(R.id.navTextDocuments)
        navTextHistory = findViewById(R.id.navTextHistory)

        setupBottomNav()
        showScreen("splash")

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentViewModel.uploadResult.collect { result ->
                    if (result != null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Upload successful: ${result.filename}",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (activeDocument != null) {
                            activeDocument = activeDocument!!.copy(status = "Active")
                        }
                        showScreen("upload_success")
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentViewModel.uploadError.collect { error ->
                    if (error != null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Upload failed: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentViewModel.searchResult.collect { result ->
                    if (result != null) {
                        currentSearchResult = result
                        currentSearchError = null
                        if (currentScreenTag == "searching" || currentScreenTag == "search_results") {
                            showScreen("search_results")
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentViewModel.searchError.collect { error ->
                    if (error != null) {
                        currentSearchError = error
                        currentSearchResult = null
                        if (currentScreenTag == "searching" || currentScreenTag == "search_results") {
                            showScreen("search_results")
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentScreenTag) {
                    "home", "splash" -> finish()
                    "upload_document" -> showScreen("home")
                    "upload_success" -> showScreen("upload_document")
                    "ask_question" -> showScreen("upload_success")
                    "searching" -> showScreen("ask_question")
                    "search_results" -> showScreen("ask_question")
                    "document_details" -> showScreen("search_results")
                    else -> showScreen("home")
                }
            }
        })
    }

    private fun launchFilePicker() {
        try {
            openDocPdfLauncher.launch(arrayOf("application/pdf", "*/*"))
        } catch (e: Exception) {
            try {
                getContentPdfLauncher.launch("application/pdf")
            } catch (ex1: Exception) {
                try {
                    getContentPdfLauncher.launch("*/*")
                } catch (ex2: Exception) {
                    Toast.makeText(this, "File picker unavailable", Toast.LENGTH_SHORT).show()
                    showScreen("upload_success")
                }
            }
        }
    }

    private fun getFileNameAndSizeFromUri(uri: Uri): Pair<String, String> {
        var name = "Document.pdf"
        var sizeStr = "PDF - 1.2MB"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val fetchedName = cursor.getString(nameIndex)
                        if (!fetchedName.isNullOrEmpty()) {
                            name = fetchedName
                        }
                    }
                    if (sizeIndex != -1) {
                        val bytes = cursor.getLong(sizeIndex)
                        if (bytes > 0) {
                            val mb = String.format(Locale.US, "%.1fMB", bytes / (1024.0 * 1024.0))
                            sizeStr = "PDF - $mb"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, sizeStr)
    }

    private fun setupBottomNav() {
        navItemHome.setOnClickListener {
            updateBottomNavSelection("home")
            showScreen("home")
        }
        navItemDocuments.setOnClickListener {
            updateBottomNavSelection("documents")
            showScreen("upload_document")
        }
        navItemHistory.setOnClickListener {
            updateBottomNavSelection("history")
            showScreen("history")
        }
    }

    private fun updateBottomNavSelection(activeTag: String) {
        val activeColor = ContextCompat.getColor(this, R.color.primary_violet)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        navTextHome.setTextColor(if (activeTag == "home") activeColor else inactiveColor)
        navTextDocuments.setTextColor(if (activeTag == "documents") activeColor else inactiveColor)
        navTextHistory.setTextColor(if (activeTag == "history") activeColor else inactiveColor)

        navIconHome.setColorFilter(if (activeTag == "home") activeColor else inactiveColor)
        navIconDocuments.setColorFilter(if (activeTag == "documents") activeColor else inactiveColor)
        navIconHistory.setColorFilter(if (activeTag == "history") activeColor else inactiveColor)
    }

    private fun setBottomNavVisibility(visible: Boolean) {
        val vis = if (visible) View.VISIBLE else View.GONE
        bottomNavContainer.visibility = vis
        bottomNavDivider.visibility = vis
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    fun showScreen(tag: String) {
        currentScreenTag = tag
        screenContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        when (tag) {
            "splash" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_splash, screenContainer, false)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (currentScreenTag == "splash") {
                        showScreen("home")
                    }
                }, 1600)
                screenContainer.addView(view)
            }

            "home" -> {
                setBottomNavVisibility(true)
                updateBottomNavSelection("home")
                val view = inflater.inflate(R.layout.screen_home, screenContainer, false)

                val tvHomeDocName = view.findViewById<TextView>(R.id.tvHomeDocName)
                val tvHomeDocMeta = view.findViewById<TextView>(R.id.tvHomeDocMeta)
                val tvHomeDocStatus = view.findViewById<TextView>(R.id.tvHomeDocStatus)

                if (activeDocument != null) {
                    tvHomeDocName?.text = activeDocument!!.filename
                    tvHomeDocMeta?.text = "${activeDocument!!.filesize} • Ready for search"
                    tvHomeDocStatus?.text = activeDocument!!.status
                } else {
                    tvHomeDocName?.text = "No document selected"
                    tvHomeDocMeta?.text = "Upload a PDF to start searching"
                    tvHomeDocStatus?.text = "None"
                }

                view.findViewById<View>(R.id.btnCardUpload).setOnClickListener {
                    showScreen("upload_document")
                }
                view.findViewById<View>(R.id.btnCardAsk).setOnClickListener {
                    showScreen("ask_question")
                }
                view.findViewById<View>(R.id.btnSettingsHeader).setOnClickListener {
                    showScreen("settings")
                }
                view.findViewById<View>(R.id.btnActiveDocCard).setOnClickListener {
                    if (activeDocument != null) {
                        showScreen("document_details")
                    } else {
                        Toast.makeText(this, "Please upload a document first", Toast.LENGTH_SHORT).show()
                        showScreen("upload_document")
                    }
                }
                screenContainer.addView(view)
            }

            "upload_document" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_upload_document, screenContainer, false)

                view.findViewById<View>(R.id.btnBackUpload).setOnClickListener {
                    showScreen("home")
                }
                view.findViewById<View>(R.id.btnTopUploadIcon).setOnClickListener {
                    launchFilePicker()
                }
                view.findViewById<View>(R.id.dropAreaContainer).setOnClickListener {
                    launchFilePicker()
                }
                view.findViewById<View>(R.id.btnChooseFile).setOnClickListener {
                    launchFilePicker()
                }

                val container = view.findViewById<LinearLayout>(R.id.llUploadedDocumentsList)
                if (uploadedDocuments.isNotEmpty()) {
                    container?.removeAllViews()
                    val headerTv = TextView(this).apply {
                        text = "Uploaded Documents"
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(context, R.color.text_main))
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setPadding(0, 8.toPx(), 0, 12.toPx())
                    }
                    container?.addView(headerTv)
                    for (doc in uploadedDocuments) {
                        val cardView = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 8.toPx()) }
                            background = ContextCompat.getDrawable(context, R.drawable.bg_card_white)
                            setPadding(14.toPx(), 14.toPx(), 14.toPx(), 14.toPx())
                            isClickable = true
                            isFocusable = true

                            val icon = ImageView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(28.toPx(), 34.toPx())
                                setImageResource(R.drawable.ic_pdf_card_icon)
                            }
                            val textLayout = LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                    marginStart = 12.toPx()
                                }
                                val nameTv = TextView(context).apply {
                                    text = doc.filename
                                    textSize = 14f
                                    setTextColor(ContextCompat.getColor(context, R.color.text_main))
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                val sizeTv = TextView(context).apply {
                                    text = "${doc.filesize} • ${doc.uploadDate}"
                                    textSize = 12f
                                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                                }
                                addView(nameTv)
                                addView(sizeTv)
                            }
                            addView(icon)
                            addView(textLayout)

                            if (doc == activeDocument) {
                                val activeTv = TextView(context).apply {
                                    text = "Active"
                                    textSize = 12f
                                    setTextColor(ContextCompat.getColor(context, R.color.primary_violet))
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    background = ContextCompat.getDrawable(context, R.drawable.bg_score_chip)
                                    setPadding(8.toPx(), 4.toPx(), 8.toPx(), 4.toPx())
                                }
                                addView(activeTv)
                            }

                            setOnClickListener {
                                activeDocument = doc
                                Toast.makeText(context, "Selected: ${doc.filename}", Toast.LENGTH_SHORT).show()
                                showScreen("upload_document")
                            }
                        }
                        container?.addView(cardView)
                    }
                } else {
                    container?.removeAllViews()
                }

                screenContainer.addView(view)
            }

            "upload_success" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_upload_success, screenContainer, false)

                val tvFilename = view.findViewById<TextView>(R.id.tvSuccessFilename)
                val tvFilesize = view.findViewById<TextView>(R.id.tvSuccessFilesize)
                tvFilename.text = activeDocument?.filename ?: "No document selected"
                tvFilesize.text = activeDocument?.filesize ?: ""

                view.findViewById<View>(R.id.btnStartChat).setOnClickListener {
                    showScreen("ask_question")
                }
                view.findViewById<View>(R.id.btnViewDetails).setOnClickListener {
                    showScreen("document_details")
                }
                screenContainer.addView(view)
            }

            "ask_question" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_ask_question, screenContainer, false)

                val tvFilename = view.findViewById<TextView>(R.id.tvAskFilename)
                val tvMeta = view.findViewById<TextView>(R.id.tvAskMeta)
                val etInput = view.findViewById<EditText>(R.id.etQuestionInput)

                tvFilename.text = activeDocument?.filename ?: "No document selected"
                tvMeta.text = activeDocument?.filesize ?: ""

                view.findViewById<View>(R.id.btnBackAsk).setOnClickListener {
                    showScreen("upload_success")
                }

                fun submitQuestion(query: String) {
                    if (query.isNotEmpty()) {
                        lastQueryText = query
                        val timeStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
                        val historyItem = HistoryItem(
                            id = UUID.randomUUID().toString(),
                            question = query,
                            timestamp = timeStr,
                            documentFilename = activeDocument?.filename ?: "Document.pdf"
                        )
                        historyList.add(0, historyItem)
                        documentViewModel.resetSearchState()
                        showScreen("searching")
                        documentViewModel.searchDocument(query)
                    } else {
                        Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
                    }
                }

                view.findViewById<View>(R.id.btnSubmitSearch).setOnClickListener {
                    val text = etInput.text.toString().trim()
                    submitQuestion(text)
                }

                view.findViewById<View>(R.id.exQuestion1).setOnClickListener {
                    val q = "Whiz summary of document...?"
                    etInput.setText(q)
                    submitQuestion(q)
                }
                view.findViewById<View>(R.id.exQuestion2).setOnClickListener {
                    val q = "What are the key dates and total amounts?"
                    etInput.setText(q)
                    submitQuestion(q)
                }

                screenContainer.addView(view)
            }

            "searching" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_searching, screenContainer, false)

                view.findViewById<View>(R.id.btnBackSearching).setOnClickListener {
                    showScreen("ask_question")
                }

                screenContainer.addView(view)
            }

            "search_results" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_search_results, screenContainer, false)

                val tvQuery = view.findViewById<TextView>(R.id.tvResultsQueryText)
                tvQuery.text = if (lastQueryText.isNotEmpty()) lastQueryText else "No query entered"

                val container = view.findViewById<LinearLayout>(R.id.llSearchResultsContainer)
                val tvNoResults = view.findViewById<TextView>(R.id.tvNoSearchResultsMsg)

                if (currentSearchError != null) {
                    tvNoResults?.apply {
                        visibility = View.VISIBLE
                        text = "Search error: $currentSearchError"
                        setTextColor(ContextCompat.getColor(context, R.color.pdf_red))
                    }
                    container?.removeAllViews()
                    if (tvNoResults != null) container?.addView(tvNoResults)
                } else if (currentSearchResult != null && currentSearchResult!!.results.isNotEmpty()) {
                    tvNoResults?.visibility = View.GONE
                    container?.removeAllViews()
                    for ((index, item) in currentSearchResult!!.results.withIndex()) {
                        val cardView = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 14.toPx()) }
                            background = ContextCompat.getDrawable(context, R.drawable.bg_card_white)
                            setPadding(16.toPx(), 16.toPx(), 16.toPx(), 16.toPx())

                            val headerLayout = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { setMargins(0, 0, 0, 8.toPx()) }

                                val titleTv = TextView(context).apply {
                                    text = "Result #${index + 1}"
                                    textSize = 15f
                                    setTextColor(ContextCompat.getColor(context, R.color.primary_violet))
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                }

                                val distanceTv = TextView(context).apply {
                                    text = String.format(Locale.US, "Distance: %.4f", item.distance)
                                    textSize = 12f
                                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                                    background = ContextCompat.getDrawable(context, R.drawable.bg_score_chip)
                                    setPadding(8.toPx(), 4.toPx(), 8.toPx(), 4.toPx())
                                }

                                addView(titleTv)
                                addView(distanceTv)
                            }

                            val docContentTv = TextView(context).apply {
                                text = item.document
                                textSize = 14f
                                setTextColor(ContextCompat.getColor(context, R.color.text_main))
                            }

                            addView(headerLayout)
                            addView(docContentTv)
                        }
                        container?.addView(cardView)
                    }
                } else {
                    tvNoResults?.apply {
                        visibility = View.VISIBLE
                        text = "No results found"
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    }
                    container?.removeAllViews()
                    if (tvNoResults != null) container?.addView(tvNoResults)
                }

                view.findViewById<View>(R.id.btnBackResults).setOnClickListener {
                    showScreen("ask_question")
                }
                view.findViewById<View>(R.id.cardResultContainer).setOnClickListener {
                    showScreen("document_details")
                }

                screenContainer.addView(view)
            }

            "document_details" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_document_details, screenContainer, false)

                val tvFilename = view.findViewById<TextView>(R.id.tvDetailsFilename)
                val tvMeta = view.findViewById<TextView>(R.id.tvDetailsMeta)
                tvFilename.text = activeDocument?.filename ?: "No document selected"
                if (activeDocument != null) {
                    tvMeta.text = "${activeDocument!!.filesize}\nDate - ${activeDocument!!.uploadDate}"
                }

                view.findViewById<View>(R.id.btnBackDetails).setOnClickListener {
                    showScreen("search_results")
                }
                view.findViewById<View>(R.id.btnDetailsAnalyze).setOnClickListener {
                    showScreen("ask_question")
                }
                view.findViewById<View>(R.id.btnDetailsExport).setOnClickListener {
                    val docName = activeDocument?.filename ?: "Document.pdf"
                    Toast.makeText(this, "Exporting $docName as TXT...", Toast.LENGTH_SHORT).show()
                }
                view.findViewById<View>(R.id.btnDetailsShare).setOnClickListener {
                    val docName = activeDocument?.filename ?: "Document.pdf"
                    Toast.makeText(this, "Sharing $docName...", Toast.LENGTH_SHORT).show()
                }

                screenContainer.addView(view)
            }

            "history" -> {
                setBottomNavVisibility(true)
                updateBottomNavSelection("history")
                val view = inflater.inflate(R.layout.screen_history, screenContainer, false)

                view.findViewById<View>(R.id.btnBackHistory).setOnClickListener {
                    showScreen("home")
                }
                view.findViewById<View>(R.id.btnFilterBy).setOnClickListener {
                    Toast.makeText(this, "Filter options opened", Toast.LENGTH_SHORT).show()
                }
                view.findViewById<View>(R.id.btnFilterDateType).setOnClickListener {
                    Toast.makeText(this, "Date/Type filter selected", Toast.LENGTH_SHORT).show()
                }

                val container = view.findViewById<LinearLayout>(R.id.llHistoryItemsContainer)
                val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyHistoryMsg)

                if (historyList.isEmpty()) {
                    tvEmpty?.visibility = View.VISIBLE
                    container?.removeAllViews()
                    container?.addView(tvEmpty)
                } else {
                    tvEmpty?.visibility = View.GONE
                    container?.removeAllViews()
                    for (item in historyList) {
                        val cardView = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 12.toPx()) }
                            background = ContextCompat.getDrawable(context, R.drawable.bg_card_white)
                            setPadding(16.toPx(), 16.toPx(), 16.toPx(), 16.toPx())
                            isClickable = true
                            isFocusable = true

                            val icon = ImageView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(32.toPx(), 40.toPx())
                                setImageResource(R.drawable.ic_pdf_card_icon)
                            }
                            val textLayout = LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                    marginStart = 14.toPx()
                                }
                                val qTv = TextView(context).apply {
                                    text = item.question
                                    textSize = 15f
                                    setTextColor(ContextCompat.getColor(context, R.color.text_main))
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                val metaTv = TextView(context).apply {
                                    text = "${item.documentFilename} • ${item.timestamp}"
                                    textSize = 12f
                                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                                    setPadding(0, 2.toPx(), 0, 0)
                                }
                                addView(qTv)
                                addView(metaTv)
                            }
                            val deleteBtn = ImageView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(24.toPx(), 24.toPx())
                                setImageResource(R.drawable.ic_close)
                                setOnClickListener {
                                    historyList.remove(item)
                                    showScreen("history")
                                }
                            }
                            addView(icon)
                            addView(textLayout)
                            addView(deleteBtn)

                            setOnClickListener {
                                lastQueryText = item.question
                                documentViewModel.resetSearchState()
                                showScreen("searching")
                                documentViewModel.searchDocument(item.question)
                            }
                        }
                        container?.addView(cardView)
                    }
                }

                screenContainer.addView(view)
            }

            "settings" -> {
                setBottomNavVisibility(false)
                val view = inflater.inflate(R.layout.screen_settings, screenContainer, false)

                view.findViewById<View>(R.id.btnBackSettings).setOnClickListener {
                    showScreen("home")
                }
                view.findViewById<View>(R.id.btnSaveSettings).setOnClickListener {
                    Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                    showScreen("home")
                }
                view.findViewById<View>(R.id.btnLogout).setOnClickListener {
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    showScreen("splash")
                }

                screenContainer.addView(view)
            }
        }
    }
}