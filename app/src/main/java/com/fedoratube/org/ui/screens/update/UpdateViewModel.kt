package com.fedoratube.org.ui.screens.update

import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.data.update.AppRelease
import com.fedoratube.org.data.update.GitHubRelease
import com.fedoratube.org.data.update.ReleaseNotes
import com.fedoratube.org.data.update.UpdateFailure
import com.fedoratube.org.data.update.UpdateRepository
import com.fedoratube.org.data.update.parseReleaseNotes
import com.fedoratube.org.updater.AppUpdateInstaller
import com.fedoratube.org.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@Immutable
data class UpdateUiState(
    val release: AppRelease? = null,
    val notes: ReleaseNotes? = null,
    val stage: UpdateStage = UpdateStage.Available,
    val loading: Boolean = false,
    val isChecking: Boolean = false,
    val fetchFailed: Boolean = false,
    val allReleases: List<GitHubRelease> = emptyList(),
    val currentVersion: String = BuildConfig.VERSION_NAME,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        private val updates: UpdateRepository,
        private val installer: AppUpdateInstaller,
    ) : ViewModel() {
        private val canInstall = MutableStateFlow(installer.canInstall())
        private val installing = MutableStateFlow(false)
        private val isChecking = MutableStateFlow(false)
        private val fetchFailed = MutableStateFlow(false)

        val state: StateFlow<UpdateUiState> =
            combine(
                updates.latest.flatMapLatest { release ->
                    if (release == null) {
                        flowOf(Pair(null, UpdateStage.Available))
                    } else {
                        val notes = parseReleaseNotes(release.notes)
                        combine(
                            installer.state(release.version),
                            canInstall,
                            installing,
                            installer.installFailed,
                        ) { download, allowed, busy, failed ->
                            Pair(notes, updateStage(download, allowed, busy, failed))
                        }
                    }
                },
                updates.latest,
                updates.allReleases,
                isChecking,
                fetchFailed,
            ) { (notes, stage), release, allReleases, checking, isFailed ->
                UpdateUiState(
                    release = release,
                    notes = notes,
                    stage = stage,
                    loading = checking && release == null && allReleases.isEmpty(),
                    isChecking = checking,
                    fetchFailed = isFailed && release == null && allReleases.isEmpty(),
                    allReleases = allReleases,
                    currentVersion = BuildConfig.VERSION_NAME,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                UpdateUiState(
                    allReleases = updates.allReleases.value,
                    release = updates.latest.value,
                    loading = updates.allReleases.value.isEmpty() && updates.latest.value == null,
                    isChecking = false,
                ),
            )

        private val _toastEvent = MutableSharedFlow<Int>()
        val toastEvent = _toastEvent.asSharedFlow()

        init {
            if (updates.allReleases.value.isEmpty()) {
                retryFetch(isManual = false)
            }
        }

        fun retryFetch(isManual: Boolean = true) {
            if (isChecking.value) return
            isChecking.value = true
            fetchFailed.value = false
            viewModelScope.launch {
                try {
                    val result = runCatching { updates.fetch() }
                    fetchFailed.value = result.isFailure
                    if (result.isSuccess) {
                        if (isManual && updates.latest.value == null) {
                            _toastEvent.emit(R.string.flow_is_up_to_date)
                        }
                    } else if (isManual) {
                        _toastEvent.emit(R.string.update_check_failed)
                    }
                } finally {
                    isChecking.value = false
                }
            }
        }

        /** Runs the action the current stage offers; the permission stage is handled by the screen. */
        fun primaryAction() {
            val current = state.value
            val release = current.release ?: return
            when (val stage = current.stage) {
                UpdateStage.Available -> installer.download(release)
                is UpdateStage.Failed -> if (stage.reason == UpdateFailure.INSTALL) install(release) else installer.download(release)
                UpdateStage.Ready -> install(release)
                else -> Unit
            }
        }

        fun cancelDownload() = installer.cancel()

        fun installPermissionIntent(): Intent = installer.installPermissionIntent()

        fun skip() {
            val release = state.value.release ?: return
            viewModelScope.launch { updates.skip(release.version) }
            installer.cancel()
        }

        /** Re-reads what may have changed while the user was in system settings or the installer. */
        fun onResume() {
            canInstall.value = installer.canInstall()
            installing.value = false
        }

        private fun install(release: AppRelease) {
            installing.value = true
            viewModelScope.launch { if (!installer.install(release.version)) installing.value = false }
        }
    }
