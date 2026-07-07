package com.aijobassistant.app.ui.resume

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijobassistant.app.data.jobs.JobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ResumeTailorState(
    val matchRate: Int? = null,
    val keywordsToInclude: List<String> = emptyList(),
    val missingKeywords: List<String> = emptyList(),
    val tailoredData: Map<String, Any?>? = null,
    val coverLetterText: String? = null,
    val isAnalyzing: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class ResumeTailorViewModel : ViewModel() {
    private val _state = MutableStateFlow(ResumeTailorState())
    val state: StateFlow<ResumeTailorState> = _state.asStateFlow()

    private val repository = JobsRepository()

    fun analyzeResume(jobDescription: String, uid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, error = null) }
            val result = repository.analyzeJobDescription(jobDescription)
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            matchRate = (data["matchRate"] as? Number)?.toInt(),
                            keywordsToInclude = (data["keywordsToInclude"] as? List<*>)?.map { k -> k.toString() } ?: emptyList(),
                            missingKeywords = (data["missingKeywords"] as? List<*>)?.map { k -> k.toString() } ?: emptyList(),
                            tailoredData = data["tailoredData"] as? Map<String, Any?>,
                            coverLetterText = data["coverLetterText"] as? String
                        )
                    }
                }
            } else {
                _state.update { 
                    it.copy(isAnalyzing = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun generateResumePdf(context: Context, tailoredData: Map<String, Any?>, userName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true) }
            val result = repository.generateResumePdf(tailoredData)
            if (result.isSuccess) {
                val bytes = result.getOrNull()?.bytes()
                if (bytes != null) {
                    openPdf(context, bytes, "${userName}_Tailored_Resume.pdf")
                }
            } else {
                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
            _state.update { it.copy(isGenerating = false) }
        }
    }

    fun generateCoverLetterPdf(context: Context, text: String, userName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true) }
            val result = repository.generateCoverLetterPdf(text)
            if (result.isSuccess) {
                val bytes = result.getOrNull()?.bytes()
                if (bytes != null) {
                    openPdf(context, bytes, "${userName}_Cover_Letter.pdf")
                }
            } else {
                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
            _state.update { it.copy(isGenerating = false) }
        }
    }

    private fun openPdf(context: Context, bytes: ByteArray, filename: String) {
        try {
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open PDF viewer", Toast.LENGTH_SHORT).show()
        }
    }
}
