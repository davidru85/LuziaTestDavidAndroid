package com.ruizurraca.luziatestdavid.data.repository

import com.ruizurraca.luziatestdavid.data.remote.api.L1ApiClient
import com.ruizurraca.luziatestdavid.data.remote.mapper.ChatMapper
import com.ruizurraca.luziatestdavid.data.remote.sse.SseEvent
import com.ruizurraca.luziatestdavid.data.remote.sse.SseParser
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class ChatRepositoryImpl(
    private val apiClient: L1ApiClient,
    private val sseParser: SseParser,
    private val chatMapper: ChatMapper,
    private val ioDispatcher: CoroutineDispatcher
) : ChatRepository {

    override suspend fun transcribeAudio(audio: File): Resource<String> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.transcribe(
                    audio = audio.readBytes(),
                    filename = audio.name
                )
                Resource.Success(response.text)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Throwable) {
                Resource.Error(e.message ?: "Transcription failed", e)
            }
        }

    override fun streamChat(history: List<ChatMessage>): Flow<Resource<String>> {
        val requestDto = chatMapper.toRequestDto(history)
        return sseParser
            .parse(apiClient.streamChat(requestDto))
            .map<SseEvent, Resource<String>> { event ->
                when (event) {
                    is SseEvent.Token -> Resource.Success(event.text)
                    is SseEvent.Error -> Resource.Error("${event.code}: ${event.message}")
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(Resource.Error(e.message ?: "Stream failed", e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveMessage(message: ChatMessage) {
        TODO("Room persistence implemented in 4.1.d.2")
    }

    override fun observeConversation(): Flow<List<ChatMessage>> {
        TODO("Room persistence implemented in 4.1.d.2")
    }

    override suspend fun clearConversation() {
        TODO("Room persistence implemented in 4.1.d.2")
    }
}
