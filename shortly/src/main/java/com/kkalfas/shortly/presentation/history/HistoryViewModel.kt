package com.kkalfas.shortly.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkalfas.shortly.AppCoroutineDispatchers
import com.kkalfas.shortly.R
import com.kkalfas.shortly.data.ShrtcoApiError
import com.kkalfas.shortly.domain.history.usecase.GetLinkHistoryUseCase
import com.kkalfas.shortly.domain.history.usecase.ShortenUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STATE_KEY_INPUT_VALUE = "input_value"
private const val STATE_KEY_INPUT_ERROR = "input_error"
private const val STATE_KEY_ERROR_MESSAGE = "error_message"

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
    private val savedStateHandle: SavedStateHandle,
    private val shortenUrlUseCase: ShortenUrlUseCase,
    private val linkHistoryUseCase: GetLinkHistoryUseCase
) : ViewModel() {

    private val _inputValue = savedStateHandle.getStateFlow(STATE_KEY_INPUT_VALUE, "")
    private val _isInputError = savedStateHandle.getStateFlow(STATE_KEY_INPUT_ERROR, false)
    private val _errorMessage = savedStateHandle.getStateFlow(STATE_KEY_ERROR_MESSAGE, -1)
    private val _isLoading = MutableStateFlow(false)
    private val _history = linkHistoryUseCase.invoke()

    val state: StateFlow<HistoryUiState> = combine(
        _inputValue, _isInputError, _errorMessage, _isLoading, _history,
    ) { input, isInputError, errorMessage, isLoading, history ->
        HistoryUiState(
            isLoading = isLoading,
            isInputError = isInputError,
            errorMessage = errorMessage,
            urlInput = input,
            history = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onUrlChanged(text: String) {
        savedStateHandle[STATE_KEY_INPUT_VALUE] = text
        savedStateHandle[STATE_KEY_INPUT_ERROR] = false
    }

    fun onShortenUrl() {
        viewModelScope.launch(appCoroutineDispatchers.io) {
            try {
                shortenUrlUseCase.invoke(state.value.urlInput)
                linkHistoryUseCase.invoke()
            } catch (ex: Exception) {
                ex.printStackTrace() // send error cause somewhere or add Timber/logger
                when {
                    ex is ShrtcoApiError && ex.errorCode == 1 -> savedStateHandle[STATE_KEY_INPUT_ERROR] = true
                    else -> savedStateHandle[STATE_KEY_ERROR_MESSAGE] = R.string.error_generic_message
                }
            }
        }
    }

    fun clearError() {
        savedStateHandle[STATE_KEY_ERROR_MESSAGE] = -1
    }
}
