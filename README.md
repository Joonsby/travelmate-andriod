# travelmate-android

여행짜자(TravelMate)의 Android 앱입니다. Kotlin과 Jetpack Compose로 만듭니다.
백엔드는 iOS 앱과 같은 `travelmate-api` 를 그대로 씁니다.

- 계획과 결정 사항: [docs/ANDROID_PLAN.md](docs/ANDROID_PLAN.md)
- Phase 0 실험 결과: [docs/PHASE0.md](docs/PHASE0.md)

## 구성

| 모듈 | 내용 |
|---|---|
| `:app` | 본 앱 (`com.joonsby.travelmate`). 지금은 뼈대만 있습니다 |
| `:spike` | Phase 0 실험 앱 (`com.joonsby.travelmate.spike`). 스티커, 토스 링크, LiveKit 실험이 들어 있습니다. 결론이 나면 지웁니다 |

## 빌드

- 필요한 것: JDK 21, Android SDK 37
- `local.properties` 에 `sdk.dir` 를 적어 둡니다

```
./gradlew :app:assembleDebug
./gradlew :spike:installDebug    # 실험 앱을 연결된 기기에 설치
```
