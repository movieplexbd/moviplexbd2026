package com.ottapp.moviestream.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.repository.AuthRepository
import com.ottapp.moviestream.data.repository.DownloadRepository
import com.ottapp.moviestream.data.repository.UserRepository
import com.ottapp.moviestream.util.toReadableSize
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val authRepo = try { AuthRepository(app) } catch (e: Exception) { null }
    private val userRepo = UserRepository()
    private val dlRepo   = DownloadRepository(app)

    private val _user        = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _storageUsed        = MutableLiveData<String>()
    val storageUsed: LiveData<String> = _storageUsed

    private val _signedOut        = MutableLiveData(false)
    val signedOut: LiveData<Boolean> = _signedOut

    init { observeUser(); loadStorage() }

    private fun observeUser() = viewModelScope.launch {
        try {
            userRepo.getCurrentUserFlow()
                .catch {
                    Log.e(TAG, "User flow error: ${it.message}")
                    emit(null)
                }
                .collect { _user.value = it }
        } catch (e: Exception) {
            Log.e(TAG, "observeUser error: ${e.message}")
            _user.value = null
        }
    }

    private fun loadStorage() {
        try {
            _storageUsed.value = dlRepo.getTotalStorageUsed().toReadableSize()
        } catch (e: Exception) {
            _storageUsed.value = "0 B"
        }
    }

    fun signOut() = viewModelScope.launch {
        try {
            authRepo?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
        _signedOut.value = true
    }
}
