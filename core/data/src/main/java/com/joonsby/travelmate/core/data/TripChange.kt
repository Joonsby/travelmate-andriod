package com.joonsby.travelmate.core.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버가 실시간 연결로 알려 주는 "이 여행의 무엇이 바뀌었다" 신호. 바뀐 내용은 오지 않으므로 받은 화면이 다시 불러온다.
 * tripId·scope 가 null 이면 모두 (연결이 다시 이어져 그 사이 신호를 놓쳤을 수 있을 때).
 */
data class TripChange(val tripId: String?, val scope: Scope?) {
    enum class Scope(val serverName: String) {
        /** 여행 정보(제목·기간)·휴지통으로 옮김·되살림 */
        Trip("trip"),
        Itinerary("itinerary"),
        Expenses("expenses"),
        Checklist("checklist"),
        Members("members");

        companion object {
            fun fromServerName(name: String?): Scope? = entries.firstOrNull { it.serverName == name }
        }
    }

    /** 이 신호가 그 화면과 관계있는지. screenTripId 가 null 인 화면은 모든 여행을 본다(내 여행 목록). */
    fun affects(screenTripId: String?, scopes: Set<Scope>): Boolean {
        val tripMatches = tripId == null || screenTripId == null || tripId == screenTripId
        val scopeMatches = scope == null || scope in scopes
        return tripMatches && scopeMatches
    }

    companion object {
        val Everything = TripChange(null, null)
    }
}

/** 여행 변경 신호를 화면들에 나눠 준다. 화면은 보일 때만 다시 불러오고, 안 보이면 다시 보일 때 한 번 불러온다. */
@Singleton
class TripChangeBus @Inject constructor() {
    private val flow = MutableSharedFlow<TripChange>(extraBufferCapacity = 64)
    val changes: SharedFlow<TripChange> = flow.asSharedFlow()

    fun post(change: TripChange) {
        flow.tryEmit(change)
    }
}
