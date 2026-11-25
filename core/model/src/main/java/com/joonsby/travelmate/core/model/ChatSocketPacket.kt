package com.joonsby.travelmate.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 실시간 연결(/api/chat/ws)로 받는 패킷.
 * message·call 은 채팅 단계에서 모델을 붙이기 전까지 JSON 그대로 둔다.
 */
@Serializable
data class ChatSocketPacket(
    val type: String,
    val message: JsonElement? = null,
    val tripId: String? = null,
    val userId: String? = null,
    val lastReadMessageId: String? = null,
    val call: JsonElement? = null,
    /** "tripChanged": 바뀐 부분 (trip | itinerary | expenses | checklist | members) */
    val scope: String? = null,
    /** "tripChanged": 바꾼 회원 */
    val by: String? = null,
)
