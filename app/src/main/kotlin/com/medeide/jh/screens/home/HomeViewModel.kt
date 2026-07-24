package com.medeide.jh.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medeide.jh.core.data.repository.UserPreferencesRepository
import com.medeide.jh.screens.home.domain.usecase.GetThemeModeUseCase
import com.medeide.jh.screens.home.landscape.topbar.audioplayer.AudioPlaybackState
import com.medeide.jh.screens.home.landscape.workspace.audioplayer.AudioPlaybackState as WorkspaceAudioPlaybackState
import com.medeide.jh.core.model.VideoPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val enabledPlugins: StateFlow<Set<String>> = preferencesRepository.enabledPlugins.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    /** 顶栏音乐播放状态 */
    val audioPlaybackState = AudioPlaybackState()

    /** 工作区视频播放状态（跨标签保持活跃） */
    val workspaceVideoState = VideoPlaybackState()

    /** 工作区音频播放状态（跨标签保持活跃） */
    val workspaceAudioState = WorkspaceAudioPlaybackState()

    init {
        viewModelScope.launch {
            getThemeModeUseCase().collect { themeMode ->
                _state.value = _state.value.copy(themeMode = themeMode)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackState.release()
        workspaceVideoState.release()
        workspaceAudioState.release()
    }
}
