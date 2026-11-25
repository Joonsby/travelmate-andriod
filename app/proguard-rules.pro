# kotlinx.serialization 은 자체 규칙을 라이브러리에 담고 있다.
# 카카오 SDK (공식 안내 규칙)
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep interface com.kakao.sdk.**.*Api
# Credential Manager (Google 로그인)
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
