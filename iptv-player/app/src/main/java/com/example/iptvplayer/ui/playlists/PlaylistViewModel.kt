package com.example.iptvplayer.ui.playlists

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val activeUser: StateFlow<User?> = userRepository.activeUser

    val playlists: StateFlow<List<Playlist>> = activeUser.flatMapLatest { user ->
        if (user != null) {
            playlistRepository.getPlaylists(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0)
    val importProgress: StateFlow<Int> = _importProgress.asStateFlow()

    private val _importStatusMessage = MutableStateFlow("")
    val importStatusMessage: StateFlow<String> = _importStatusMessage.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }

    fun importLocalPlaylist(
        name: String,
        uri: Uri,
        contentResolver: ContentResolver,
        onSuccess: () -> Unit = {}
    ) {
        val user = activeUser.value ?: run {
            _errorMessage.value = "Nenhum utilizador ativo selecionado."
            return
        }

        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = 0
            _importStatusMessage.value = "A abrir ficheiro M3U..."

            try {
                // Get filename if possible
                var fileName = "Ficheiro local"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }

                val finalName = if (name.isNotBlank()) name.trim() else fileName.removeSuffix(".m3u").removeSuffix(".m3u8")

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _errorMessage.value = "Não foi possível abrir o ficheiro selecionado."
                    _isImporting.value = false
                    return@launch
                }

                _importStatusMessage.value = "A analisar canais do ficheiro..."

                inputStream.use { stream ->
                    val result = playlistRepository.importLocalPlaylist(
                        userId = user.id,
                        name = finalName,
                        inputStream = stream,
                        sourceDescription = fileName,
                        onProgress = { count ->
                            _importProgress.value = count
                            _importStatusMessage.value = "A carregar $count canais..."
                        }
                    )

                    result.onSuccess { playlist ->
                        _feedbackMessage.value = "Playlist \"${playlist.name}\" importada com sucesso (${playlist.channelCount} canais)."
                        onSuccess()
                    }.onFailure { error ->
                        _errorMessage.value = error.message ?: "Falha ao importar ficheiro M3U."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao ler ficheiro selecionado."
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importRemotePlaylist(
        name: String,
        url: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = activeUser.value ?: run {
            _errorMessage.value = "Nenhum utilizador ativo selecionado."
            return
        }

        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            _errorMessage.value = "O endereço URL não pode estar vazio."
            return
        }

        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = 0
            _importStatusMessage.value = "A estabelecer ligação com o servidor..."

            val finalName = if (name.isNotBlank()) name.trim() else "Playlist Remota"

            val result = playlistRepository.importRemotePlaylist(
                userId = user.id,
                name = finalName,
                url = cleanUrl,
                onProgress = { count ->
                    _importProgress.value = count
                    _importStatusMessage.value = "A descarregar $count canais..."
                }
            )

            result.onSuccess { playlist ->
                _feedbackMessage.value = "Playlist \"${playlist.name}\" importada com sucesso (${playlist.channelCount} canais)."
                onSuccess()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Falha ao descarregar a playlist remota."
            }

            _isImporting.value = false
        }
    }

    fun renamePlaylist(id: String, newName: String, onSuccess: () -> Unit = {}) {
        if (newName.isBlank()) {
            _errorMessage.value = "O nome da playlist não pode ser vazio."
            return
        }

        viewModelScope.launch {
            playlistRepository.renamePlaylist(id, newName.trim())
                .onSuccess {
                    _feedbackMessage.value = "Playlist renomeada com sucesso."
                    onSuccess()
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Falha ao renomear a playlist."
                }
        }
    }

    fun deletePlaylist(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
                .onSuccess {
                    _feedbackMessage.value = "Playlist eliminada com sucesso."
                    onSuccess()
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Falha ao eliminar a playlist."
                }
        }
    }

    fun duplicatePlaylist(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "A duplicar playlist..."

            playlistRepository.duplicatePlaylist(id)
                .onSuccess {
                    _feedbackMessage.value = "Playlist duplicada com sucesso."
                    onSuccess()
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Falha ao duplicar a playlist."
                }

            _isImporting.value = false
        }
    }

    fun reorderPlaylist(playlistId: String, moveUp: Boolean) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            playlistRepository.reorderPlaylist(playlistId, moveUp, user.id)
        }
    }

    fun refreshRemotePlaylist(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = 0
            _importStatusMessage.value = "A atualizar lista de canais remota..."

            playlistRepository.refreshRemotePlaylist(id) { count ->
                _importProgress.value = count
                _importStatusMessage.value = "A atualizar $count canais..."
            }.onSuccess { playlist ->
                _feedbackMessage.value = "Playlist \"${playlist.name}\" sincronizada com sucesso (${playlist.channelCount} canais)."
                onSuccess()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Falha ao atualizar a playlist remota."
            }

            _isImporting.value = false
        }
    }

    companion object {
        fun provideFactory(
            playlistRepository: PlaylistRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaylistViewModel(playlistRepository, userRepository) as T
            }
        }
    }
}
