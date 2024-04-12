package com.aydinkasimoglu.sesliyonlendirme

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class VoiceToTextState(
    val spokenText: String = "",
    val isSpeaking: Boolean = false,
    val error: String? = null,
)

class VoiceToText(private val app: Application): RecognitionListener {
    private val _state = MutableStateFlow(VoiceToTextState())
    val state = _state.asStateFlow()

    private val recognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(app)

    fun startListening() {
        _state.update { VoiceToTextState() }

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update { it.copy(error = "Recognition isn't available") }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        }

        recognizer.setRecognitionListener(this)
        recognizer.startListening(intent)

        _state.update {
            it.copy(isSpeaking = true)
        }
    }

    fun stopListening() {
        _state.update { it.copy(isSpeaking = false) }
        recognizer.stopListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null) }
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        _state.update { it.copy(isSpeaking = false) }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_CLIENT) {
            return
        }

        _state.update { it.copy(error = error.toString()) }
    }

    override fun onResults(results: Bundle?) {
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { result ->
                _state.update {
                    it.copy(
                        spokenText = result
                    )
                }
            }
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

@Composable
fun Speech(voiceToText: VoiceToText) {
    var canRecord by remember {
        mutableStateOf(false)
    }

    val recordAudioLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            canRecord = isGranted
        }

    LaunchedEffect(key1 = recordAudioLauncher) {
        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val state by voiceToText.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            if (state.isSpeaking) {
                voiceToText.stopListening()
            } else {
                voiceToText.startListening()
            }
        }) {
            AnimatedContent(targetState = state.isSpeaking) { isSpeaking ->
                if (isSpeaking) {
                    Icon(painter = painterResource(id = R.drawable.round_stop_24), contentDescription = "Stop recording audio")
                } else {
                    Icon(painter = painterResource(id = R.drawable.round_mic_24), contentDescription = "Record audio")
                }
            }
        }

        AnimatedContent(targetState = state.isSpeaking) { isSpeaking ->
            Text(text =
                if (isSpeaking)
                    "Konuşuluyor"
                else
                    state.spokenText.ifEmpty { "Mikrofona basın" }
            )
        }
    }
}