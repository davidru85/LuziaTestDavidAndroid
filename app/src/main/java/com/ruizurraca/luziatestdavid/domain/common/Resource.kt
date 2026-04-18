package com.ruizurraca.luziatestdavid.domain.common

sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val error: AppError? = null
    ) : Resource<Nothing>
}
