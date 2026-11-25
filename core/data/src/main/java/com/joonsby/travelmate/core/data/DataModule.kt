package com.joonsby.travelmate.core.data

import android.content.Context
import com.joonsby.travelmate.core.network.ApiConfig
import com.joonsby.travelmate.core.network.AuthApi
import com.joonsby.travelmate.core.network.NetworkFactory
import com.joonsby.travelmate.core.network.OfflineStatus
import com.joonsby.travelmate.core.network.ResponseCache
import com.joonsby.travelmate.core.network.TokenRefresher
import com.joonsby.travelmate.core.network.TokenStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

const val BASE_CLIENT = "base"
const val API_CLIENT = "api"
const val APP_SCOPE = "appScope"

/** ApiConfig 는 앱 모듈이 BuildConfig 로 만들어 넣는다. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton @Named(APP_SCOPE)
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton @Named(BASE_CLIENT)
    fun baseClient(): OkHttpClient = NetworkFactory.baseClient()

    @Provides @Singleton
    fun responseCache(@ApplicationContext context: Context) = ResponseCache(File(context.cacheDir, "api-responses"))

    @Provides @Singleton
    fun offlineStatus() = OfflineStatus()

    @Provides @Singleton
    fun tokenRefresher(storage: TokenStorage, config: ApiConfig, @Named(BASE_CLIENT) base: OkHttpClient) =
        TokenRefresher(storage, NetworkFactory.refreshCall(config, base))

    @Provides @Singleton @Named(API_CLIENT)
    fun apiClient(
        @Named(BASE_CLIENT) base: OkHttpClient,
        storage: TokenStorage,
        refresher: TokenRefresher,
        cache: ResponseCache,
        offlineStatus: OfflineStatus,
    ): OkHttpClient = NetworkFactory.apiClient(base, storage, refresher, cache, offlineStatus)

    @Provides @Singleton
    fun retrofit(config: ApiConfig, @Named(API_CLIENT) client: OkHttpClient): Retrofit = NetworkFactory.retrofit(config, client)

    @Provides @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    abstract fun tokenStorage(impl: KeystoreTokenStorage): TokenStorage
}
