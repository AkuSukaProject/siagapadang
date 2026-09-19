package com.akusukaproject.siagapadang.data.remote

/**
 * Respons non-2xx dari backend. Kode status dibawa agar pemanggil dapat membedakan
 * permintaan yang ditolak (4xx, tidak perlu diulang) dari gangguan server (5xx, dapat diulang).
 */
class ApiHttpException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message) {
    val isRejectedByServer: Boolean
        get() = statusCode in 400..499
}
