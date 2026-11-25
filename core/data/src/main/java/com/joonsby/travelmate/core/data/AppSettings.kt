package com.joonsby.travelmate.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 설정의 화면 모드. 저장 키와 값은 iOS 와 같다 (tm.appearance = system | light | dark). */
enum class Appearance(val storageValue: String, val title: String) {
    System("system", "시스템 설정"),
    Light("light", "라이트"),
    Dark("dark", "다크");

    companion object {
        fun from(value: String?): Appearance = entries.firstOrNull { it.storageValue == value } ?: System
    }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 이 기기에만 저장하는 앱 설정값 */
@Singleton
class AppSettings @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.settingsStore

    val appearance: Flow<Appearance> = store.data.map { Appearance.from(it[APPEARANCE]) }

    suspend fun setAppearance(value: Appearance) {
        store.edit { it[APPEARANCE] = value.storageValue }
    }

    private companion object {
        val APPEARANCE = stringPreferencesKey("tm.appearance")
    }
}
