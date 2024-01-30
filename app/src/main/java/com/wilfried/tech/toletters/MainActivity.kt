package com.wilfried.tech.toletters


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.text.InputFilter
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.wilfried.tech.toletters.databinding.ActivityMainBinding
import com.wilfried.tech.toletters.databinding.BottomSheetDialogBinding
import com.wilfried.tech.toletters.databinding.ConvertInputIncludeBinding
import com.wilfried.tech.toletters.databinding.ConvertResultIncludeBinding
import com.wilfried.tech.toletters.tools.MAX_VALUE_LENGTH
import com.wilfried.tech.toletters.tools.toLetters
import com.wilfried.tech.toletters.utils.HistoryAdapter
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var convertInputBinding: ConvertInputIncludeBinding
    private lateinit var convertResultBinding: ConvertResultIncludeBinding
    private lateinit var bottomSheetDialogBinding: BottomSheetDialogBinding
    private lateinit var speaker: TextToSpeech
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var convertHistory: HistoryAdapter
    private var lastNumber: String? = null
    private var ttsError: String? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_history -> {
                if (convertHistory.isEmpty())
                    showMessage(R.string.no_history)
                else
                    bottomSheetDialog.show()
            }

//            R.id.menu_settings -> {
//                startActivity(Intent(applicationContext, SettingsActivity::class.java))
//            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityMainBinding.inflate(layoutInflater).also {
            convertInputBinding = it.convertInput
            convertResultBinding = it.convertResult
            setContentView(it.root)
        }

        bottomSheetDialogBinding = BottomSheetDialogBinding.inflate(layoutInflater)

        speaker = initializeTextToSpeech()

        convertInputBinding.apply {
            prompt.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_VALUE_LENGTH))
            inputLayout.counterMaxLength = MAX_VALUE_LENGTH

            btnConvert.setOnClickListener { convert() }

            prompt.setOnKeyListener { _, _, keyEvent ->
                if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || keyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    btnConvert.performClick()
                    return@setOnKeyListener true
                }
                false
            }
        }

        convertResultBinding.apply {
            speakOption.setOnClickListener { readResult() }
            copyOption.setOnClickListener { copyResult() }
            shareOption.setOnClickListener { shareResult() }
        }

        applyPreferences()
        configureBottomSheet()
    }

    private fun initializeTextToSpeech() =
        TextToSpeech(applicationContext, OnInitListener { status: Int ->
            if (status == TextToSpeech.ERROR_NOT_INSTALLED_YET || status == TextToSpeech.ERROR_SERVICE) {
                ttsError = getString(R.string.tts_missing)
                return@OnInitListener
            }
            val langStatus: Int = speaker.setLanguage(Locale.FRANCE)
            if (langStatus == TextToSpeech.ERROR_NOT_INSTALLED_YET || langStatus == TextToSpeech.ERROR_SERVICE) {
                ttsError = getString(R.string.lang_not_available)
            }
        })

    private fun applyPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun configureBottomSheet() {
        bottomSheetDialog = BottomSheetDialog(this)
        convertHistory = HistoryAdapter(this)

        bottomSheetDialogBinding.apply {
            historyList.layoutManager = LinearLayoutManager(applicationContext)
            historyList.adapter = convertHistory
        }
        bottomSheetDialog.setContentView(bottomSheetDialogBinding.root)

    }

    private fun convert() {
        val value = convertInputBinding.prompt.text.toString()
        if (value.isEmpty()) {
            showMessage(resources.getString(R.string.empty_msg, MAX_VALUE_LENGTH))
        } else {
            lastNumber = toLetters(value)
            convertResultBinding.output.text = String.format(
                "\t%s = %s\n\n",
                value,
                lastNumber
            )
            lastNumber?.let {
                convertHistory.add(it)
            }
            convertInputBinding.prompt.text?.clear()
        }
    }


    private fun readResult() {
        if (ttsError != null) {
            showMessage(ttsError!!)
            return
        }
        if (lastNumber == null) {
            showMessage(R.string.nothing_to_read)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speaker.speak(lastNumber, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            showMessage(R.string.tts_low_sdk_version)
        }
    }

    private fun copyResult() {
        if (isOutputEmpty()) {
            showMessage(R.string.nothing_to_copy)
            return
        }
        try {
            val clipboardManager: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.app_name),
                    convertResultBinding.output.text.toString().trim()
                )
            )
            showMessage(R.string.result_copied)
        } catch (e: Exception) {
            showMessage(getString(R.string.copy_error) + e.message)
        }
    }

    private fun shareResult() {
        if (isOutputEmpty()) {
            showMessage(R.string.nothing_to_share)
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, convertResultBinding.output.text.toString().trim())
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
    }

    private fun isOutputEmpty(): Boolean {
        return convertResultBinding.output.text.toString().trim().isEmpty()
    }

    override fun onPause() {
        speaker.stop()
        convertHistory.save()
        super.onPause()
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }

    private fun showMessage(message: Any) {
        if (message is Int)
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
        if (message is String)
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}