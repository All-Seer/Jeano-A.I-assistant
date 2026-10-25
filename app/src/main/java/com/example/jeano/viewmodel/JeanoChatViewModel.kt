package com.example.jeano.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jeano.model.BreiahMessageModel
import com.example.jeano.model.LeeMessageModel
import com.example.jeano.model.MessageModel
import com.example.jeano.model.repository.MessageRepository // Ensure correct repository import
import com.google.ai.client.generativeai.Chat // Import Chat if you want persistent chat object
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content // Import Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// import kotlinx.coroutines.flow.WhileSubscribed // Not needed directly for SharingStarted.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


private val API_KEY = com.example.jeano.BuildConfig.GEMINI_API_KEY

class JeanoChatViewModel(private val messageRepository: MessageRepository) : ViewModel() {

    val allMessages: StateFlow<List<MessageModel>> = messageRepository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )
    val allBreaihMessages: StateFlow<List<BreiahMessageModel>> = messageRepository.allBreiahMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )
    val allLeeMessages : StateFlow<List<LeeMessageModel>> = messageRepository.allLeeMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _breiahIsTyping = MutableStateFlow(false)
    val breiahIsTyping: StateFlow<Boolean> = _breiahIsTyping.asStateFlow()

    private val _leeIsTyping = MutableStateFlow(false)
    val leeIsTyping: StateFlow<Boolean> = _leeIsTyping.asStateFlow()

    val queryContext = "The Context is You are a Professional Teacher named as Jeano its a roleplay character, always reply in manners. Your scope is General Knowledge and you speak a conyo way of english tagalog like in bgc."
    val breiahQueryContext = "The Context is You are a Professional Teacher named as Breiah. its a roleplay character, always reply in manners. Your scope is All Work Related Knowledge and you speak a conyo way of english tagalog like in bgc."
    val leeQueryContext = "The Context is You are a Professional Teacher named as Kenley. its a roleplay character, always reply in manners. Your scope is All Programming related knowledge and you speak a conyo way of english tagalog like in bgc."

    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-3.8-flash",
        apiKey = API_KEY,
    )

    private fun insertMessage(message: MessageModel) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.insertMessage(message)
        }
    }
    private fun insertBreiahMessage(message: BreiahMessageModel) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.insertBreiahMessage(message)
        }
    }
    private fun insertLeeMessage(message: LeeMessageModel) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.insertLeeMessage(message)
        }
    }
    fun sendLeeQuestion(question: String) {
        val userMessageContent = question.trim()
        insertLeeMessage(LeeMessageModel(0, userMessageContent, "user"))

        _leeIsTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentHistory = allLeeMessages.value.map {
                    content(it.role) { text(it.message) }
                }
                val chat = generativeModel.startChat(history = currentHistory)

                val response = chat.sendMessage("context is: $leeQueryContext, question is: $userMessageContent")
                Log.d("ViewModelAI", "API call finished.")
                _leeIsTyping.value = false

                val modelResponseText = response.text?.trim()

                if (!modelResponseText.isNullOrBlank()) {
                    insertLeeMessage(LeeMessageModel(0, modelResponseText, "model"))
                }

            } catch (e: Exception) {
                Log.e("ViewModelAI", "API Error: ${e.message}", e)
                _leeIsTyping.value = false
                insertLeeMessage(LeeMessageModel(0, "Error: ${e.message}", "model"))
            }
        }
    }
    fun sendBreaihQuestion(question: String) {
        val userMessageContent = question.trim()
        insertBreiahMessage(BreiahMessageModel(0, userMessageContent, "user"))

        _breiahIsTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentHistory = allBreaihMessages.value.map {
                    content(it.role) { text(it.message) }
                }
                val chat = generativeModel.startChat(history = currentHistory)

                val response = chat.sendMessage("context is: $breiahQueryContext, question is: $userMessageContent")
                Log.d("ViewModelAI", "API call finished.")
                _breiahIsTyping.value = false

                val modelResponseText = response.text?.trim()

                if (!modelResponseText.isNullOrBlank()) {
                    insertBreiahMessage(BreiahMessageModel(0, modelResponseText, "model"))
                }

            } catch (e: Exception) {
                Log.e("ViewModelAI", "API Error: ${e.message}", e)
                _breiahIsTyping.value = false
                insertBreiahMessage(BreiahMessageModel(0, "Error: ${e.message}", "model"))
            }
        }
    }

    fun sendQuestion(question: String) {
        val userMessageContent = question.trim()
        insertMessage(MessageModel(0, userMessageContent, "user"))

        _isTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentHistory = allMessages.value.map {
                    content(it.role) { text(it.message) }
                }
                val chat = generativeModel.startChat(history = currentHistory)

                val response = chat.sendMessage("context is: $queryContext, question is: $userMessageContent")
                Log.d("ViewModelAI", "API call finished.")
                _isTyping.value = false

                val modelResponseText = response.text?.trim()

                if (!modelResponseText.isNullOrBlank()) {
                    insertMessage(MessageModel(0, modelResponseText, "model"))
                }

            } catch (e: Exception) {
                Log.e("ViewModelAI", "API Error: ${e.message}", e)
                _isTyping.value = false
                insertMessage(MessageModel(0, "Error: ${e.message}", "model"))
            }
        }
    }
}


class JeanoChatViewModelFactory(private val messageRepository: MessageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JeanoChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JeanoChatViewModel(messageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}