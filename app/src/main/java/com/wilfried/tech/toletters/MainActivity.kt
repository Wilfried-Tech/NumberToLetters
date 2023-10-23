package com.wilfried.tech.toletters


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wilfried.tech.toletters.databinding.ActivityMainBinding
import com.wilfried.tech.toletters.tools.MAX_VALUE_LENGTH
import com.wilfried.tech.toletters.tools.toLetters
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appContext: Context
    private lateinit var speaker: TextToSpeech
    private var lastNumber: String? = null
    private var ttsError: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appContext = applicationContext

        speaker = TextToSpeech(appContext, OnInitListener { status: Int ->
            if (status == TextToSpeech.ERROR_NOT_INSTALLED_YET || status == TextToSpeech.ERROR_SERVICE) {
                ttsError = getString(R.string.tts_missing)
                return@OnInitListener
            }
            val langStatus: Int = speaker.setLanguage(Locale.FRANCE)
            if (langStatus == TextToSpeech.ERROR_NOT_INSTALLED_YET || langStatus == TextToSpeech.ERROR_SERVICE) {
                ttsError = getString(R.string.lang_not_avaible)
            }
        })

        binding.prompt.filters = arrayOf<InputFilter>(LengthFilter(MAX_VALUE_LENGTH))
        binding.inputLayout.counterMaxLength = MAX_VALUE_LENGTH

        listenEvents()
    }

    private fun listenEvents() {
        binding.btnConvert.setOnClickListener {
            if (binding.prompt.text.toString().isEmpty()) {
                Toast.makeText(
                    appContext,
                    resources.getString(R.string.empty_msg, MAX_VALUE_LENGTH),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lastNumber = toLetters(binding.prompt.text.toString())
                binding.output.text = String.format(
                    "\t%s = %s\n\n",
                    binding.prompt.text.toString(),
                    lastNumber
                )
                binding.prompt.text?.clear()
            }
        }

        binding.prompt.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || keyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                binding.btnConvert.performClick()
                return@setOnKeyListener true
            }
            false
        }

        binding.copyOption.setOnClickListener {
            if (outputEmpty()) {
                Toast.makeText(appContext, R.string.nothing_to_copy, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val clipboardManager: ClipboardManager =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        getString(R.string.app_name),
                        binding.output.text.toString().trim()
                    )
                )
                Toast.makeText(appContext, R.string.result_copied, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    appContext,
                    getString(R.string.copy_error) + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.shareOption.setOnClickListener {
            if (outputEmpty()) {
                Toast.makeText(appContext, R.string.nothing_to_share, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, binding.output.text.toString().trim())
            startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
        }

        binding.speakOption.setOnClickListener {
            if (ttsError != null) {
                Toast.makeText(appContext, ttsError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lastNumber == null) {
                Toast.makeText(appContext, R.string.nothing_to_read, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                speaker.speak(lastNumber, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(appContext, R.string.tts_low_sdk_version, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun outputEmpty(): Boolean {
        return binding.output.text.toString().trim().isEmpty()
    }

    override fun onPause() {
        speaker.stop()
        super.onPause()
    }
}