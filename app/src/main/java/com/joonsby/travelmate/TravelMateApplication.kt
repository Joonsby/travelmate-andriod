package com.joonsby.travelmate

import android.app.Application
import com.joonsby.travelmate.core.data.AppLifecycle
import com.joonsby.travelmate.core.network.ApiConfig
import com.joonsby.travelmate.feature.auth.AuthConfig
import com.kakao.sdk.common.KakaoSdk
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class TravelMateApplication : Application() {
    @Inject lateinit var appLifecycle: AppLifecycle

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 로그인 상태·앱 앞/뒤 전환에 맞춰 실시간 연결을 켜고 끈다.
        appLifecycle.install()
    }
}

/** BuildConfig 값으로 만드는 설정 (서버 주소, 로그인 클라이언트 ID) */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun apiConfig() = ApiConfig(BuildConfig.API_BASE_URL)

    @Provides @Singleton
    fun authConfig() = AuthConfig(googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
}
