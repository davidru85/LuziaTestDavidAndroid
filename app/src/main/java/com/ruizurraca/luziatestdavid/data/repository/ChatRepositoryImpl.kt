package com.ruizurraca.luziatestdavid.data.repository

import com.ruizurraca.luziatestdavid.data.local.dao.ChatMessageDao
import com.ruizurraca.luziatestdavid.data.local.mapper.toDomain
import com.ruizurraca.luziatestdavid.data.local.mapper.toEntity
import com.ruizurraca.luziatestdavid.data.remote.api.L1ApiClient
import com.ruizurraca.luziatestdavid.data.remote.mapper.ChatMapper
import com.ruizurraca.luziatestdavid.data.remote.sse.SseEvent
import com.ruizurraca.luziatestdavid.data.remote.sse.SseParser
import com.ruizurraca.luziatestdavid.di.qualifier.IoDispatcher
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiClient: L1ApiClient,
    private val sseParser: SseParser,
    private val chatMapper: ChatMapper,
    private val dao: ChatMessageDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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
            .map { it.toResource() }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(Resource.Error(e.message ?: "Stream failed", e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveMessage(message: ChatMessage) {
        withContext(ioDispatcher) {
            dao.insert(message.toEntity())
        }
    }

    override fun observeConversation(): Flow<List<ChatMessage>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun clearConversation() {
        withContext(ioDispatcher) {
            dao.deleteAll()
        }
    }
}

private fun SseEvent.toResource(): Resource<String> = when (this) {
    is SseEvent.Token -> Resource.Success(text)
    is SseEvent.Error -> Resource.Error("$code: $message")
}
