package com.joonsby.travelmate.spike

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** 운영 API. 스티커·약관은 인증 없이 열린다. 에뮬레이터에서 로컬 서버를 쓰려면 http://10.0.2.2:3000/api */
const val DEFAULT_API_BASE = "https://api-travelmate.fit-plate.com/api"

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
