package com.aijobassistant.app.ui.resume

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijobassistant.app.data.network.AnalyzeResumeRequest
import com.aijobassistant.app.data.network.ApiClient
import com.aijobassistant.app.data.network.GenerateCoverLetterPdfRequest
import com.aijobassistant.app.data.network.GeneratePdfRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

data class ResumeTailorState(
    val isAnalyzing: Boolean = false,
    val isGenerating: Boolean = false,
    val matchRate: Int = 0,
    val keywordsToInclude: List<String> = emptyList(),
    val missingKeywords: List<String> = emptyList(),
    val tailoredData: Map<String, Any?>? = null,
    val coverLetterText: String = "",
    val error: String? = null
)

class ResumeTailorViewModel : ViewModel() {
    private val jobsRepository = com.aijobassistant.app.data.jobs.JobsRepository()
    
    private val _state = MutableStateFlow(ResumeTailorState())
    val state: StateFlow<ResumeTailorState> = _state.asStateFlow()

    fun analyzeResume(jobDescription: String, uid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, error = null) }
            try {
                val result = jobsRepository.analyzeJobDescription(jobDescription)
                if (result.isSuccess) {
                    val data = result.getOrNull()!!
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            matchRate = data["matchRate"] as Int,
                            keywordsToInclude = data["keywordsToInclude"] as List<String>,
                            missingKeywords = data["missingKeywords"] as List<String>,
                            tailoredData = data["tailoredData"] as Map<String, Any?>,
                            coverLetterText = data["coverLetterText"] as String
                        )
                    }
                } else {
                    _state.update { it.copy(isAnalyzing = false, error = result.exceptionOrNull()?.message ?: "Failed to analyze resume.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isAnalyzing = false, error = e.localizedMessage) }
            }
        }
    }

    fun updateTailoredData(newData: Map<String, Any?>) {
        _state.update { it.copy(tailoredData = newData) }
    }

    fun updateCoverLetter(newText: String) {
        _state.update { it.copy(coverLetterText = newText) }
    }

    fun generateResumePdf(context: Context, data: Map<String, Any?>, applicantName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }
            try {
                val result = jobsRepository.generateResumePdf(data)
                if (result.isSuccess) {
                    savePdfAndOpen(context, result.getOrNull()!!, "${applicantName}_Resume.pdf")
                    _state.update { it.copy(isGenerating = false) }
                } else {
                    _state.update { it.copy(isGenerating = false, error = result.exceptionOrNull()?.message ?: "Failed to generate PDF") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = e.localizedMessage) }
            }
        }
    }

    fun generateCoverLetterPdf(context: Context, text: String, applicantName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }
            try {
                val request = GenerateCoverLetterPdfRequest(cover_letter_text = text)
                val responseBody = ApiClient.apiService.generateCoverLetterPdf(request)
                savePdfAndOpen(context, responseBody, "${applicantName}_CoverLetter.pdf")
                _state.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = e.localizedMessage) }
            }
        }
    }

    private fun savePdfAndOpen(context: Context, body: ResponseBody, fileName: String) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                try {
                    inputStream = body.byteStream()
                    outputStream = contentResolver.openOutputStream(uri)
                    
                    val buffer = ByteArray(4096)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream?.write(buffer, 0, read)
                    }
                    outputStream?.flush()
                    
                    // Try to open it
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } else {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
