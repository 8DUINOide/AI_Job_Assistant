package com.aijobassistant.app.ui.resume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijobassistant.app.data.profile.ProfileRepository
import com.aijobassistant.app.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResumeEditorState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

class ResumeEditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(ResumeEditorState())
    val state: StateFlow<ResumeEditorState> = _state.asStateFlow()
    
    private val profileRepository = ProfileRepository()
    
    fun loadProfile(profile: UserProfile?) {
        if (profile != null) {
            _state.update { it.copy(profile = profile, isLoading = false) }
        } else {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, error = null) }
                val result = profileRepository.getProfile()
                if (result.isSuccess) {
                    val loadedProfile = result.getOrNull() ?: UserProfile()
                    _state.update { it.copy(profile = loadedProfile, isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                }
            }
        }
    }
    
    fun updateProfile(newProfile: UserProfile) {
        _state.update { it.copy(profile = newProfile) }
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            val result = profileRepository.saveProfile(state.value.profile)
            if (result.isSuccess) {
                _state.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                _state.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun resetSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }
}
