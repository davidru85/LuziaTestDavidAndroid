package com.ruizurraca.luziatestdavid.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of a backend error response body, per
 * `TECHNICAL_SPEC.md §3 Global Error Schema`:
 *
 *   { "error": { "code": "...", "message": "..." } }
 */
@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorDetail
)

@Serializable
data class ApiErrorDetail(
    val code: String,
    val message: String
)
