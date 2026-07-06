package com.aijobassistant.app.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijobassistant.app.data.jobs.JobsRepository
import com.aijobassistant.app.data.profile.ProfileRepository
import com.aijobassistant.app.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobDiscoveryState(
    val jobs: List<Job> = emptyList(),
    val savedJobs: List<Job> = emptyList(),
    val isSearching: Boolean = false,
    val searchProgress: String = "",
    val error: String? = null
)

class JobDiscoveryViewModel : ViewModel() {
    private val _state = MutableStateFlow(JobDiscoveryState())
    val state: StateFlow<JobDiscoveryState> = _state.asStateFlow()

    private val jobsRepository = JobsRepository()
    private val profileRepository = ProfileRepository()

    init {
        // Collect saved jobs
        viewModelScope.launch {
            try {
                jobsRepository.observeSavedJobs().collect { savedList ->
                    _state.update { it.copy(savedJobs = savedList) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Observer error: ${e.message}") }
            }
        }

        // Automatically fetch latest jobs for the user's desired role on initialization
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchProgress = "Fetching latest job roles...") }
            
            try {
                // Determine preferred role or default to Software Engineer
                val profileResult = profileRepository.getProfile()
                val role = if (profileResult.isSuccess) {
                    val profile = profileResult.getOrNull()
                    profile?.jobPreferences?.desiredRoles?.firstOrNull() ?: "Software Engineer"
                } else {
                    "Software Engineer"
                }

                // Fetch jobs using the fast search endpoint with a strict limit to speed up initial load
                val result = jobsRepository.searchJobs(keyword = role, location = "Remote", offset = 0, resultsWanted = 10)
                
                if (result.isSuccess) {
                    _state.update { 
                        it.copy(
                            jobs = result.getOrNull() ?: emptyList(),
                            isSearching = false,
                            searchProgress = ""
                        ) 
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isSearching = false,
                            error = result.exceptionOrNull()?.message,
                            searchProgress = ""
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isSearching = false, error = e.localizedMessage, searchProgress = "") 
                }
            }
        }
    }

    fun searchJobs(keyword: String, location: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchProgress = "Searching for '$keyword' in '$location'...", error = null) }
            
            val result = jobsRepository.searchJobs(keyword = keyword, location = location, offset = 0)
            
            if (result.isSuccess) {
                _state.update { 
                    it.copy(
                        jobs = result.getOrNull() ?: emptyList(),
                        isSearching = false,
                        searchProgress = ""
                    ) 
                }
            } else {
                _state.update { 
                    it.copy(
                        isSearching = false,
                        error = result.exceptionOrNull()?.message,
                        searchProgress = ""
                    ) 
                }
            }
        }
    }

    fun saveJob(job: Job) {
        viewModelScope.launch {
            val result = jobsRepository.saveJob(job)
            if (result.isFailure) {
                _state.update { it.copy(error = "Failed to save: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun unsaveJob(job: Job) {
        viewModelScope.launch {
            val result = jobsRepository.unsaveJob(job)
            if (result.isFailure) {
                _state.update { it.copy(error = "Failed to unsave: ${result.exceptionOrNull()?.message}") }
            }
        }
    }
}
