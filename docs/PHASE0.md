# Phase 0 진행 기록

최종 갱신 2026-11-25. 계획은 [ANDROID_PLAN.md](ANDROID_PLAN.md) 의 8장 Phase 0 을 따릅니다.

## 1. 한 일

| 항목 | 상태 | 내용 |
|---|---|---|
| 저장소 | 완료 | `git init`(main). 아직 커밋하지 않았습니다 |
| 빌드 구성 | 완료 | Gradle 9.7.1, AGP 9.5.1(Kotlin 내장), Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37 / targetSdk 36 / minSdk 28, Version Catalog |
| `:app` | 완료 | `com.joonsby.travelmate` 뼈대. `assembleDebug`, `lintDebug`, `testDebugUnitTest` 모두 통과 |
| `:spike` | 완료 | 실험 앱 `com.joonsby.travelmate.spike`. 스티커, 토스 링크, LiveKit 세 화면 |
| CI | 작성 | `.github/workflows/android.yml`. 원격 저장소가 없어 아직 돌려 보지 못했습니다 |

빌드하며 알게 된 것: Compose 1.12 부터 **compileSdk 37 이 필수**입니다(36 이면 AAR 메타데이터 검사에서 실패).

## 2. 실험 결과

### 2-1. HEIC 스티커 — ⚠ 그림은 나오지만 투명 배경이 검게 나옴

| 항목 | 결과 |
|---|---|
| 기기 | 에뮬레이터 Android 17 (API 37), HEVC 디코더는 HW 1개와 SW 2개 |
| 카탈로그 | fluent(heic) 3,145 · travi(heic) 16 · noto(lottie) 859 |
| HEIC 디코딩 (무작위 60개) | ImageDecoder 60/60 성공, BitmapFactory 60/60 성공 |
| **투명 배경 유지** | **ImageDecoder 0/60, BitmapFactory 0/60** |
| Coil 화면 표시 | 그림은 나오지만 사각형 배경이 검은색 ([캡처](phase0/heic-alpha-black-api37.png)) |
| Lottie (noto) | 정상 재생 |

**원인**
- 원본 HEIC 에는 알파가 있습니다. 알파는 별도 보조 이미지(`urn:mpeg:hevc:2015:auxid:1`)로 들어 있습니다(`fluent/grinning-face.heic` 로 확인).
- Android 플랫폼 HEIF 디코더는 이 보조 알파 이미지를 합치지 않습니다.
- 앱 코드나 Coil 설정으로는 고칠 수 없습니다. 디코더 두 가지가 모두 같은 결과였습니다.
- 실기기 제조사별 차이는 아직 확인하지 않았습니다. 다만 플랫폼 디코더 한계라서 대부분 같을 것으로 «추정»합니다.

→ **D3 은 A안(서버에 WebP 추가)으로 결정했습니다**(2026-11-25). 아래 2-1-1 과 3장을 보세요.

#### 2-1-1. WebP 변환과 검증

| 항목 | 결과 |
|---|---|
| 도구 | `tools/sticker-webp/convert.js`. libheif(wasm)로 RGBA 까지 디코딩한 뒤 sharp 로 WebP 를 만듭니다(quality 90, alpha 100) |
| 변환 | 3,161개 전부 성공(fluent 3,145 · travi 16). 각 파일을 다시 읽어 투명 영역이 98% 이상 남았는지 검사해 모두 통과 |
| 용량 | HEIC 22.2MB → WebP 23.8MB (+7%) |
| Android 확인 | 같은 에뮬레이터에서 60개를 검사해 ImageDecoder 60/60, BitmapFactory 60/60 모두 **투명 유지**. Coil 표시도 정상 ([캡처](phase0/webp-alpha-ok-api37.png)) |
| 결과물 위치 | `tools/sticker-webp/out/<묶음>/<이름>.webp` (git 에서 제외) |

### 2-2. 토스 송금 링크 — 실기기 필요 (미실시)

에뮬레이터에는 토스 앱이 없어 시험할 수 없습니다.

실험 앱 「2. 토스 송금 링크」 화면이 확인해 주는 것:
- 토스 설치 여부
- `supertoss` 스킴을 처리하는 앱이 있는지
- 링크를 열었을 때 송금 화면이 뜨는지

시험 방법: 토스가 깔린 Android 폰에서 **본인 계좌**로 시험합니다. 링크는 화면만 열고, 송금은 토스에서 직접 승인해야만 일어납니다.

확인할 점:
1. 송금 화면이 열리는가
2. 은행·계좌·금액·메모가 채워지는가

### 2-3. LiveKit 통화 — 실기기와 로그인 토큰 필요 (미실시)

실험 앱 「3. LiveKit 그룹 통화」 화면은 iOS 와 같은 경로를 씁니다. 앱 access token 과 여행 id 를 넣으면 서버 `/trips/:id/call/join` 을 불러 LiveKit 에 들어갑니다.

시험 방법:
1. iOS 기기로 어떤 여행의 통화에 먼저 들어갑니다.
2. Android 실기기에서 같은 여행으로 참여합니다.
3. 서로 들리는지, 말하는 사람 표시(🔊)가 맞는지 봅니다.

토큰은 화면에만 두고 저장하지 않습니다. 토큰을 구하는 방법:
- 개발 서버에서 로그인해 발급합니다.
- 운영에서 시험한다면 iOS 앱 토큰을 쓸 수 없으니 심사용 로그인 `/auth/review` 응답을 씁니다.

### 2-4. Google idToken 서버 검증 — 선행 작업 대기

GCP Web client ID 발급과 서버 설정 ①(`GOOGLE_CLIENT_ID` 추가)이 끝나야 시험할 수 있습니다.

## 3. D3 스티커 — A안 결정, 서버 반영 대기

서버에 반영하는 방법:
1. `travelmate-api/public/stickers/{fluent,travi}/` 에 같은 이름의 `.webp` 3,161개(23.8MB)를 넣습니다.
2. `express.static` 이 그대로 서빙합니다. Dockerfile 이 `public` 을 복사하므로 **코드 변경은 없습니다.** catalog 형식도 그대로이고, iOS 는 계속 `.heic` 를 받습니다.
3. main 에 push 하면 자동 배포됩니다.

배포 뒤에는 `GET /api/stickers/fluent/grinning-face.webp` 가 200, `image/webp` 로 오는지 확인합니다.
새 스티커를 추가할 때는 이 도구로 webp 도 함께 만들어야 합니다.

검토했던 선택지:

| 안 | 내용 | 서버 영향 | 장단점 |
|---|---|---|---|
| **A. 서버에 WebP 추가 (권장)** | `public/stickers/fluent/*.webp`, `travi/*.webp` 를 같은 이름으로 추가합니다. Android 는 `.webp` 를 요청합니다 | 정적 파일만 추가합니다. 코드와 catalog 형식은 그대로이고 iOS 영향은 없습니다. 서버 배포는 필요합니다 | 새 스티커를 추가할 때 webp 도 함께 만들어야 합니다 |
| B. Android 앱에 WebP 내장 | 스티커 3,161개를 WebP 로 바꿔 앱 assets 에 넣습니다 | 없음 | 앱 용량이 몇십 MB «추정» 늘어납니다. 서버에 새 스티커가 생기면 앱을 업데이트해야 합니다 |
| C. 앱에 libheif 를 넣어 직접 디코딩 | 네이티브 라이브러리를 넣습니다 | 없음 | 용량이 늘고 HEVC 특허·라이선스 검토가 필요합니다. 권장하지 않습니다 |

A 와 B 모두 변환은 PC 에서 한 번 하면 됩니다(Node 와 libheif 의 wasm 판 등). 알파 보존은 변환 뒤 다시 검사합니다.

## 4. 사용자가 할 일 (콘솔·계정)

| # | 할 일 | 필요한 곳 |
|---|---|---|
| 1 | 카카오 개발자 콘솔에서 기존 앱에 Android 플랫폼을 추가합니다. 패키지 `com.joonsby.travelmate` 와 키 해시(디버그·릴리스)를 등록합니다 | 카카오 로그인 |
| 2 | GCP(iOS 와 같은 프로젝트)에서 OAuth 클라이언트 2개를 만듭니다. 하나는 Android(패키지와 SHA-1), 하나는 Web 입니다 | Google 로그인 |
| 3 | 2번에서 만든 Web client ID 를 서버 `GOOGLE_CLIENT_ID` 에 쉼표로 추가합니다. GitHub Secret 과 서버 env 모두입니다(설정 ①) | Google 로그인 |
| 4 | GCP 에서 Maps SDK for Android 키를 발급합니다. 패키지와 SHA-1 로 제한합니다 | 지도 |
| 5 | Firebase 프로젝트를 만들고 Android 앱을 등록합니다(`google-services.json`) | 푸시 (Phase 6-1) |
| 6 | Play Console 에 앱을 만들어 `com.joonsby.travelmate` 를 선점합니다 | 출시 |
| 7 | 원격 git 저장소를 만들고 첫 커밋을 올립니다 | CI |

디버그 키 해시와 SHA-1 은 이 PC 의 `~/.android/debug.keystore` 에서 뽑을 수 있습니다. 필요하면 알려 드리겠습니다.
