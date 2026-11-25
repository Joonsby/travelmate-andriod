package com.joonsby.travelmate.core.network

/** 접속할 서버. baseUrl 은 `/api` 까지 포함한다 (예: https://api-travelmate.fit-plate.com/api). */
data class ApiConfig(val baseUrl: String) {
    private val trimmed = baseUrl.trimEnd('/')

    /** Retrofit 용 (끝에 / 가 있어야 한다) */
    val retrofitBaseUrl: String get() = "$trimmed/"

    /** 실시간 연결 주소 (/api/chat/ws) */
    val chatSocketUrl: String
        get() = trimmed.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/chat/ws"

    /** 응답의 `/files/...`, `/places/photo?...` 같은 상대 경로를 절대 주소로 바꾼다. 이미 절대 주소면 그대로. */
    fun resolve(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return trimmed + "/" + path.trimStart('/')
    }

    /** 이용약관·개인정보 처리방침 (서버가 HTML 로 보여 준다) */
    val termsUrl: String get() = "$trimmed/legal/terms"
    val privacyUrl: String get() = "$trimmed/legal/privacy"
}
