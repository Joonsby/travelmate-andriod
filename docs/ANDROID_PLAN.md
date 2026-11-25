# travelmate-android 분석 및 개발 계획

작성 2026-11-25. 분석 기준은 `travelmate-ios` main `28046f2` (1.0.1 빌드 8)과 `travelmate-api` main `f3de341` 입니다.
두 저장소는 읽기만 했고, 아무것도 수정하지 않았습니다.

---

## 0. 결론 먼저

1. **백엔드는 플랫폼을 가리지 않습니다.** User-Agent나 platform 값으로 분기하는 코드가 없습니다. 데이터는 전부 서버 DB(PostgreSQL)에 있고, 실시간 통신은 표준 WebSocket과 LiveKit 을 씁니다.
   그래서 Android 앱이 같은 API를 쓰기만 하면 iOS 사용자와 친구 추가·여행 공유·채팅·그룹 통화를 바로 함께 쓸 수 있습니다. 이를 위해 따로 만들 서버 기능은 없습니다.
2. **서버 코드를 바꾸지 않고도 출시할 수 있습니다.** 단, 운영 설정값 하나와 외부 콘솔 등록 몇 가지는 해야 합니다(5장 표의 ①②).
   - **Google 로그인**: 운영 환경변수 `GOOGLE_CLIENT_ID` 에 Web client ID 를 추가해야 합니다. 코드 변경은 아닙니다.
   - **카카오 로그인**: 카카오 개발자 콘솔에 Android 플랫폼과 키 해시를 등록해야 합니다.
3. **Apple 로그인은 지금 서버 구조로는 Android 에서 쓸 수 없습니다.** 서버가 iOS 번들 ID로만 토큰을 교환하기 때문입니다(`appleService.js:155,184`).
   그래서 **Apple 로만 가입한 iOS 사용자는 Android 에서 같은 계정으로 들어올 수 없습니다.** 이 문제는 결정이 필요합니다(7장 D1).
4. **푸시 알림은 1차 범위에 넣습니다(D2 확정).** 다만 지금은 iOS 에도 빠져 있습니다. 서버에 발송 코드가 0건이고, 기기 토큰 테이블 `push_devices` 와 알림 설정 컬럼(`users.notify_*`)만 만들어져 있습니다.
   그래서 대부분의 작업은 **서버에서** 해야 합니다(9장). Android 는 받는 쪽만 만들면 됩니다.
5. 기술적 위험은 두 가지입니다. Phase 0 에서 먼저 실험해 봅니다.
   - **HEIC 스티커**: 3,161개가 HEIC 형식이라 Android 에서 제대로 디코딩되는지 확인해야 합니다.
   - **토스 송금 링크**: `supertoss://` 는 비공식 스킴이라 Android 에서 열리는지 확인해야 합니다.

---

## 1. 전체 기능 및 화면 목록

iOS 는 SwiftUI 이고, 최소 iOS 26, iPhone 전용, UI 는 한국어만 지원합니다.
하단 탭은 5칸입니다: **내 여행 · 채팅 · (+) 새 여행 · 친구 · 내 정보**.
각 탭 오른쪽 위 톱니바퀴를 누르면 설정 화면으로 갑니다.

### 1-1. 시작·인증

| 화면 | 기능 | API |
|---|---|---|
| 스플래시 | 노을 하늘 배경과 앱 아이콘을 보여 주며 토큰 복원 | `GET /me`, `POST /auth/refresh` |
| 복원 실패 | 다시 시도하거나 다른 계정으로 로그인 | — |
| 로그인 | 카카오, Google, Apple 로그인. 약관 링크 | `POST /auth/{kakao,google,apple}` |
| 심사용 로그인 | 앱 마크를 1.5초 안에 5번 탭하면 시트가 열림. 이메일과 비밀번호로 로그인 | `POST /auth/review` |

### 1-2. 내 여행 탭

| 화면 | 기능 |
|---|---|
| 여행 목록 | 다가오는 여행은 큰 표지 카드, 지난 여행은 최근 3개. 길게 누르면 메뉴, 밀어서 삭제. 휴지통 토스트(4초)에서 되돌리기. 스켈레톤 로딩 |
| 지난 여행 전체 | 연도별 묶음과 검색 |
| 휴지통 | 30일 보관. 되살리기, 완전 삭제, 여러 개 선택, 비우기 |
| 여행 만들기·수정 (시트) | 제목, 여행지(지역 자동완성), 기간, 동행 고르기 |
| **여행 상세** | 표지 헤더와 세그먼트 **일정 / 준비 / 경비 / 동행**. 채팅 바로가기, 캘린더에 넣기, 수정, 삭제·나가기 |
| ├ 일정 | 요약 카드, 동선 지도 카드, AI 플래너 진입, 날짜별 타임라인, 선택해서 일괄 삭제, 공유 |
| ├ 일정 편집 (시트) | 날짜, 시간, 제목, 장소 검색, 메모. 같은 시간에 일정이 있으면 경고 |
| ├ 전체 화면 동선 지도 | 일차 칩, 번호 핀, 점선 동선, 장소 정보 시트, Google 지도 길찾기 |
| ├ 일정 요약 포스터 | 일정을 한 장 그림으로 렌더링해 이미지로 공유 |
| ├ AI 일정 플래너 | 채울 날과 취향 선택 → 초안 미리보기 → 고른 일정만 추가. 생성 중에는 뒤로 가기 막힘 |
| ├ AI 주변 추천 / 추천 지도 | Google 인기 장소를 먼저 보여 주고, AI 가 골라 줌(횟수 차감). 지도와 카드 캐러셀에서 바로 일정에 추가 |
| ├ 준비(체크리스트) | 준비물과 할 일, 진행률, 담당자, 밀어서 수정·삭제 |
| ├ 경비 | 정산 카드, 일차별 지출. 계좌가 없으면 계좌 등록부터 |
| ├ 경비 편집 | 금액, 통화와 환율, 분류, 결제자, 참여자, 고정·균등 분배, 영수증 OCR |
| ├ 경비 상세 / 정산 상세 | 사람별 부담액, 보낼 돈과 받을 돈, 토스 송금, 계좌 복사 |
| └ 동행 | 동행 목록, 방장 넘기기, 내보내기, 친구 초대, 프로필 |

### 1-3. 채팅 탭

| 화면 | 기능 |
|---|---|
| 채팅 목록 | 여행 단체방과 1:1 방. 고정한 방이 위. 밀거나 길게 눌러 고정·알림 끄기·나가기. 여러 개 선택해 읽음 처리. 지난 여행 방은 따로 모음. 탭 배지에 안 읽은 수 |
| 채팅방 | 글, 답장, 수정, 삭제, 복사, 텍스트 선택, 긴 글 전체 보기, 읽음 수, 공지 배너, 검색, 통화 버튼 |
| 첨부(+ 메뉴) | 앨범(최대 100장), 카메라, 파일, 링크, 게시글, 송금 요청, 정산 공유 |
| 스티커 패널 | 최근 / Fluent(HEIC) / Noto(Lottie) / 트래비. 피부색 5가지, 크게 미리 보기 |
| 사진·동영상 뷰어 | 좌우 넘기기, 확대, 아래로 밀어 닫기, 저장, 공유 |
| 서랍(오른쪽) | 미디어·파일·링크 모아 보기, 공지·게시판, 알림 끄기, 대화상대, 초대, 방 배경, 나가기 |
| 1:1 초안 방 | 첫 메시지를 보내거나 통화를 걸 때 방을 만듦 |
| 그룹 음성 통화 | 참여자 타일(말하는 사람 강조), 마이크, 스피커, 나가기. 최소화하면 떠 있는 버블. 앱 전체에서 통화는 하나만 |

### 1-4. 친구 탭 · 내 정보 탭 · 설정

| 화면 | 기능 |
|---|---|
| 친구 | 내 친구 코드(12자리)와 복사·공유, 코드로 친구 추가, 받은 요청과 보낸 요청, 즐겨찾기 |
| 친구 프로필 | 1:1 채팅·통화, 즐겨찾기, 삭제, 신고, 차단 |
| 내 프로필 | 사진(원형 자르기), 닉네임, 상태 메시지, 이메일, 연결된 로그인, 정산 계좌 |
| 정산 계좌 | 은행·증권 60곳 중 선택(글자 배지), 계좌번호, 예금주 |
| 설정 | 채팅 글자 크기·배경·사진 화질·동영상 자동 재생 / 캐시 / 화면 모드 / 차단 목록·AI 전송 동의 / 고객 지원·약관·오픈소스 라이선스 / 로그아웃·회원 탈퇴 |
| 신고 | 사유 8가지와 상세 내용, 함께 차단할지 선택 |
| AI 전송 동의 | AI 기능을 처음 쓸 때 한 번 동의 받음(기기에 저장) |

### 1-5. 공통 동작

- **오프라인 캐시**: 성공한 GET 응답을 디스크에 저장해 둡니다. 오프라인이거나 502·503·504 오류면 저장본을 보여 주고 "오프라인" 배지를 띄웁니다.
- **실시간 반영**: 다른 동행이 여행을 바꾸면 WebSocket 으로 `tripChanged` 신호가 옵니다. 보이는 화면은 바로 조용히 다시 불러오고, 안 보이는 화면은 다시 보일 때 불러옵니다.
- **포그라운드 복귀**: 앱으로 돌아오면 WebSocket 을 다시 연결하고 데이터를 다시 불러옵니다.

---

## 2. 기존 API 명세 및 인증 방식

### 2-1. 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | 운영 `https://api-travelmate.fit-plate.com/api`, 개발 `http://localhost:3000/api` (Android 에뮬레이터에서는 `10.0.2.2`) |
| 서버 스택 | Express 5, Node 24, PostgreSQL 17, `ws`, LiveKit |
| 요청 | JSON 본문, 최대 1MB |
| 성공 응답 | 감싸지 않은 JSON. 삭제류는 204 |
| 오류 응답 | `{ "error": { "message", "details"? } }`. **`code` 필드가 없으므로** HTTP 상태와 message 로만 분기 |
| 상태 코드 | 400, 401, 403, 404, 409(충돌·중복), 413, **428(계좌 미등록)**, 429(요청 횟수 제한·AI 한도), 502, 503 |
| 키·값 | camelCase. **id 는 문자열**. 날짜는 `YYYY-MM-DD`, 시각은 ISO 8601(밀리초가 있을 수도, 없을 수도 있음) |
| 파일 주소 | 응답의 `/files/...`, `/places/photo?...` 는 `/api` 기준 상대 경로라 base URL 을 앞에 붙여야 함 |
| 요청 횟수 제한 | 전체 1분 300회, 인증 15분 30회, 친구 요청 15분 30회 |

### 2-2. 엔드포인트 묶음

| 영역 | 경로 |
|---|---|
| 인증 | `POST /auth/kakao {accessToken}` · `/auth/google {idToken}` · `/auth/apple {identityToken,authorizationCode,nonce,name?}` · `/auth/review {email,password}` · `/auth/refresh {refreshToken}` · `/auth/logout` |
| 내 정보 | `GET/PATCH/DELETE /me` · `GET /me/ai-usage` · `GET/PUT /me/payout` · `PUT/DELETE /me/profile-image`(raw 본문, jpeg·png, 5MB) · `GET /me/blocks` · `PUT/DELETE /me/blocks/:userId` |
| 친구 | `GET /friends` → `{code, friends, incoming, outgoing}` · `POST /friends/requests {code}` · `.../:id/accept` · `DELETE /friends/requests/:id` · `PUT/DELETE /friends/:id/favorite` · `DELETE /friends/:id` |
| 여행 | `GET/POST /trips` · `GET/PUT/DELETE /trips/:id` · 휴지통 `/trips/trash[/:id[/restore]]` · 동행 `POST /trips/:id/members`, `PUT .../owner`, `DELETE .../members/:userId` |
| 일정 | `/trips/:id/itinerary[/:itemId]` · `/itinerary/batch` · `/itinerary/batch-delete` · `.../:itemId/{photos,nearby,recommendations}` |
| AI | `POST /trips/:id/ai/itinerary`(100초 timeout) · `GET/DELETE .../latest` · `.../usage` · `.../places` · `POST /trips/:id/places/resolve` |
| 체크리스트 | `/trips/:id/checklist[/:itemId]` · `/checklist/batch` |
| 경비 | `/trips/:id/expenses[/:eid]`(PUT 에 `updatedAt` 을 보내 충돌 검사) · `/settlement` · `/payout-methods` · `GET /exchange-rates?from=` · `/exchange-rates/currencies` |
| 채팅 | `GET /chats` · `GET/POST /chats/direct` · `PUT /chats/:tripId/pinned` · `GET/POST /trips/:id/messages` · `PATCH/DELETE .../messages/:mid` · `.../messages/{media,search,reads,read}` · `/chat` · `/chat/{notices,mute,pin}` |
| 첨부 | `POST /trips/:id/messages/attachments?kind=` · `POST /trips/:id/files?kind=`. **raw 본문**에 헤더 `Content-Type`, `X-File-Name`(퍼센트 인코딩), `X-Client-Id`, `X-Width`, `X-Height`, `X-Duration` |
| 통화 | `GET /trips/:id/call` · `POST .../call/join` → `{url, token, call}` · `POST .../call/leave` |
| 장소 | `GET /places/autocomplete` · `/places/area` · `/places/details/:id` · `/places/photo`(공개, 서명 URL) |
| 기타 | `POST /reports` · `GET /stickers/catalog.json`(공개) · `GET /legal/{privacy,terms,support}` |

메시지 `kind` 는 text, post, sticker, album, itinerary, expense, settlement, transfer, photo, video, file, call, system 입니다.
첨부 크기 제한은 사진 10MB(jpeg, png, heic, gif, webp), 동영상 50MB(mp4, mov), 파일 20MB 입니다.

### 2-3. 인증 방식

- **토큰**
  - 액세스 토큰: HS256 JWT, 1시간.
  - 리프레시 토큰: 불투명 문자열, 30일. **쓸 때마다 새 토큰으로 바뀌고(회전), 이미 쓴 토큰을 다시 쓰면 401** 입니다.
  - 헤더: `Authorization: Bearer <access>`.
- **클라이언트 갱신 규칙**(iOS 와 똑같이)
  1. 요청이 401 이면 refresh 를 **한 번만** 시도하고, 원래 요청을 다시 보냅니다.
  2. refresh 도 401 이면 로그아웃합니다.
  3. **refresh 는 동시에 하나만 돌게 해야 합니다**(Mutex). 토큰이 회전되므로 두 요청이 동시에 갱신하면 하나가 401 을 받고 로그아웃됩니다.
  4. 앱 시작 때 생기는 네트워크 오류는 로그아웃 사유가 아닙니다. "복원 실패" 화면으로 보냅니다.
- **소셜 로그인별 Android 대응**

| 공급자 | 클라이언트가 보내는 값 | Android 대응 | 서버 변경 |
|---|---|---|---|
| 카카오 | 카카오 SDK accessToken | Kakao Android SDK v2 `UserApiClient` | 없음. 콘솔에 Android 플랫폼과 키 해시만 등록 |
| Google | idToken | Credential Manager + `GetGoogleIdOption(serverClientId = Web client ID)` | **코드 변경 없음, 설정만.** `GOOGLE_CLIENT_ID` 에 Web client ID 추가(쉼표로 구분, `env.js:40`). Android OAuth 클라이언트(패키지명 + SHA-1)도 만들어야 함 |
| Apple | identityToken 등 | Android 용 SDK 가 없어 웹 흐름(Services ID)이 필요 | **코드 변경 필요.** 지금은 aud 와 code 교환이 iOS 번들 ID 로만 됨. redirect 를 받을 엔드포인트도 없음 → 7장 D1 |

- 심사용 로그인 `/auth/review` 는 App Store 전용 코드가 아닙니다. **Play 심사에 그대로 쓸 수 있습니다.**
- 계정은 소셜 계정 단위로 나뉩니다. 이메일이 같아도 자동으로 합치지 않습니다.

### 2-4. 실시간(WebSocket)

- 주소: `wss://api-travelmate.fit-plate.com/api/chat/ws`. 라이브러리는 순수 `ws` 이고 socket.io 가 아닙니다. 받은 프레임은 최대 16KB 입니다.
- 인증: 연결한 뒤 10초 안에 `{"type":"auth","token":"<access>"}` 를 보내야 합니다. 성공하면 `{"type":"ready"}` 가 오고, 실패하면 close 4001 입니다.
- **받기 전용**입니다. 메시지는 REST 로 보냅니다.
- 서버가 보내는 타입: `message`, `messageUpdated`, `pin`, `read`, `call`, `tripChanged{tripId, scope, by}`.
  - `tripChanged` 는 신호만 옵니다. 바뀐 내용은 다시 불러와야 합니다.
- 서버가 30초마다 ping 을 보냅니다. OkHttp 는 pong 을 자동으로 답합니다.
- 재연결: 1초에서 시작해 두 배씩 늘리고 최대 30초. `ready` 를 받으면 놓친 메시지와 화면 데이터를 다시 불러옵니다.
- 그룹 통화: 서버가 LiveKit 토큰(room `trip-<id>`)을 발급합니다. Android 는 **LiveKit Android SDK** 로 같은 방에 들어가므로 iOS 와 함께 통화할 수 있습니다.

---

## 3. Android 프로젝트 아키텍처 및 기술 스택

### 3-1. 기본 정보

| 항목 | 값 |
|---|---|
| 프로젝트 | `travelmate-android` (별도 git 저장소) |
| applicationId | `com.joonsby.travelmate` (D5 확정. iOS 번들 ID 와 같음. Play 에 한 번 등록하면 바꿀 수 없음) |
| 언어·빌드 | Kotlin 2.x, AGP 8.x, Gradle Version Catalog, JDK 17 |
| minSdk / target | **minSdk 28 (Android 9)** 권장. HEIC 스티커 디코딩(ImageDecoder)이 이유. targetSdk 는 Play 최신 요구치 |
| UI | Jetpack Compose (BOM) + Material 3 위에 자체 디자인 시스템 |
| 지원 | 휴대폰 세로 화면 전용, 한국어 전용 (iOS 와 같음) |

### 3-2. 라이브러리

| 용도 | 선택 | iOS 대응 |
|---|---|---|
| DI | Hilt | 수동 조립 |
| 화면 전환 | Navigation Compose (type-safe routes), 탭마다 백스택 | NavigationStack ×5 |
| 상태 | ViewModel + StateFlow, Coroutines | ObservableObject |
| HTTP | Retrofit + OkHttp + kotlinx.serialization | URLSession |
| 인증 | OkHttp `Authenticator`(401 → Mutex 로 refresh 한 번) + Interceptor | `AuthStore.withAccessToken` |
| WebSocket | OkHttp WebSocket | URLSessionWebSocketTask |
| 오프라인 캐시 | OkHttp Interceptor 로 GET 응답을 `cacheDir/api-responses/{sha256(url)}.json` 에 저장하고 실패 시 돌려줌 | APIResponseCache |
| 토큰 저장 | DataStore + Android Keystore(AES-GCM) 로 암호화 | Keychain |
| 설정값 | DataStore Preferences (`tm.appearance`, `tm.chatTextSize` 등 iOS 와 같은 키 이름) | UserDefaults |
| 이미지 | Coil 3 (HEIC 는 ImageDecoder, 서명 URL 캐시 키 고정) | CachedAsyncImage |
| Lottie 스티커 | lottie-compose | lottie-spm |
| 지도 | Maps Compose (Google Maps SDK for Android). **Android 전용 API 키를 새로 발급** | GoogleMaps iOS |
| 통화 | LiveKit Android SDK (+ 마이크 타입 포그라운드 서비스) | LiveKit Swift |
| 카카오 | Kakao SDK v2 (user) | KakaoSDK |
| Google | Credential Manager + googleid | GoogleSignIn |
| OCR | ML Kit Text Recognition v2 (Korean 모델 + Latin, 기기 안에서 처리) | Vision |
| 동영상 | Media3 ExoPlayer | AVKit |
| 사진 선택 | Photo Picker (`PickMultipleVisualMedia`), 카메라 `TakePicture`·`CaptureVideo` | PhotosPicker |
| 테스트 | JUnit, Turbine, MockWebServer, Compose UI test, Roborazzi(스크린샷) | XCTest |

### 3-3. 모듈 구조

```
app/                    진입점, 탭 셸, 내비게이션 그래프, Hilt 설정
core/model              DTO (서버 JSON 과 1:1, id 는 String)
core/network            Retrofit API, OkHttp 설정, Authenticator, 오프라인 캐시, WebSocket(ChatSocket)
core/data               Repository, TokenStore, 설정값, TripChange 이벤트 버스(SharedFlow)
core/designsystem       TM 토큰, 테마, 공통 컴포넌트, SkyBackground, 커스텀 탭바·시트·스와이프·새로고침
core/common             날짜 형식, 금액 분배(ExpenseSplit), 영수증 파서 같은 순수 로직
feature/auth            로그인, 심사용 로그인
feature/trips           목록, 상세(일정·준비·경비·동행), 편집기, 지도, 휴지통, 정산, 캘린더
feature/ai              AI 플래너, 추천, 동의 게이트
feature/chat            목록, 채팅방, 첨부, 스티커, 서랍, 뷰어
feature/call            LiveKit 통화 화면과 버블, 포그라운드 서비스
feature/friends         친구, 프로필
feature/profile         내 정보, 설정, 계좌, 신고·차단, 탈퇴
```

- 단방향 데이터 흐름: `Screen(Compose) ← UiState ← ViewModel ← Repository ← Api/Socket`
- iOS 의 `reloadOnTripChange` 는 `TripChangeBus: SharedFlow<TripChange>` 로 옮깁니다. 화면은 `LifecycleResumeEffect` 와 묶어서 구독합니다. 보일 때만 다시 불러오고, 안 보이면 다시 보일 때 한 번 불러옵니다.
- 순수 로직은 iOS 의 테스트 케이스를 그대로 옮겨 같은 결과가 나오는지 확인합니다. 대상은 금액 분배, 영수증 파서, 토스 링크 생성, 캘린더 변환입니다.

---

## 4. 플랫폼별로 별도 구현이 필요한 기능

| 기능 | iOS 구현 | Android 구현 | 주의 |
|---|---|---|---|
| 카카오 로그인 | KakaoSDK, URL scheme | Kakao SDK, `AuthCodeHandlerActivity`, 키 해시 | 디버그와 릴리스 키 해시를 둘 다 등록 |
| Google 로그인 | GoogleSignIn | Credential Manager | Web client ID 를 서버 설정에 추가해야 함 |
| Apple 로그인 | AuthenticationServices | **1차에서는 버튼 숨김(결정됨, D1)** | Apple 전용 가입자는 Android 에 들어올 수 없음 |
| 토큰 보관 | Keychain | Keystore + DataStore | 백업에서 제외(`dataExtractionRules`) |
| 지도·동선 | GoogleMaps iOS | Maps Compose (Polyline 점선, 번호 마커) | API 키는 패키지명과 SHA-1 로 제한 |
| 그룹 통화 | LiveKit Swift, 백그라운드 audio | LiveKit Android, **포그라운드 서비스(microphone)** + 진행 중 알림 | Android 14 이상은 `FOREGROUND_SERVICE_MICROPHONE` 권한 필요. 오디오 라우팅은 AudioManager 로 |
| 영수증 OCR | Vision (ko/en/ja/zh) | ML Kit Korean + Latin (+ Japanese/Chinese 선택) | 파서는 공통 로직으로 옮겨 iOS 테스트로 검증 |
| 캘린더 넣기 | EventKit 쓰기 전용 + 알림 | CalendarContract (Events + Reminders), `WRITE_CALENDAR` | 일정 여러 개와 알림을 넣어야 하므로 Intent 방식으로는 부족함 |
| 사진 선택·촬영 | PhotosPicker, UIImagePicker | Photo Picker(권한 불필요), ActivityResult 카메라 | iOS 는 최대 100장. Android Photo Picker 는 기기 상한이 있음 → `MediaStore.getPickImagesMaxLimit()` |
| 사진 저장 | PhotoLibraryAdd | MediaStore `Pictures/여행짜자` | API 29 이상은 권한 불필요 |
| 파일 첨부·미리보기 | fileImporter, QuickLook | `OpenDocument`, 내려받은 뒤 `ACTION_VIEW` (FileProvider) | |
| 동영상 | AVKit, 포스터 생성 | Media3, `MediaMetadataRetriever` 로 포스터 | iOS 가 보낸 `.mov` 재생 확인(HEVC 는 기기마다 다름) |
| 이미지 압축 | 긴 변 2048/0.82 또는 4096/0.92 JPEG | 같은 수치로 Bitmap 축소 후 JPEG | EXIF 회전 보정 |
| 링크 미리보기 | LinkPresentation | 직접 구현(OkHttp 로 OG 태그 파싱) 또는 도메인과 제목만 | 서버 기능이 없어 클라이언트에서 처리 |
| 공유 | ShareLink, UIActivity | `Intent.ACTION_SEND`, 이미지는 FileProvider | 요약 포스터는 `GraphicsLayer.toImageBitmap()` |
| 토스 송금 | `supertoss://send?...` | 같은 URI 로 `ACTION_VIEW`, 실패하면 계좌 복사 | `<queries>` 에 토스 패키지 선언. **동작 실험 필요** |
| 친구 초대 문구 | 코드 + App Store 링크 | 코드 + **Play 스토어 링크** | iOS 문구는 그대로 둠 |
| HEIC 스티커 | 기본 지원 | Coil + ImageDecoder (API 28 이상) | **Phase 0 에서 실험.** 안 되면 D3 |
| 탭 + 버튼 가로채기 | UITabBarController 해킹 | 커스텀 하단 바이므로 그냥 onClick | 오히려 더 간단 |
| 뒤로 가기 막기 | BackSwipeLock | `BackHandler` + 예측형 뒤로 가기 대응 | 하드웨어 뒤로 버튼도 막아야 함 |
| 시트 끌어서 닫기 | SheetHeaderDrag 직접 구현 | `ModalBottomSheet`(70% 높이) + `confirmValueChange` 로 확인 대화상자 | |
| 키보드 | 빈 곳 탭하면 내림 | `imePadding` + 포커스 해제 Modifier | edge-to-edge |
| 화면 모드 | UserDefaults → colorScheme | `AppCompatDelegate.setApplicationNightMode` 또는 테마 상태 | |
| 푸시 | APNs 직접 발송 (이미 있음) | FCM, 알림 채널 4개(채팅·통화·여행 초대·친구), `POST_NOTIFICATIONS` 권한(Android 13 이상), 알림을 누르면 해당 채팅방·여행·친구 탭으로 이동 | 9장 참고. 서버에 FCM 갈래(9-2)가 먼저 배포돼야 함 |

---

## 5. 서버·운영 쪽 필요 사항 (이번에는 아무것도 하지 않음)

요청에 따라 운영 중인 iOS 와 백엔드는 건드리지 않았습니다. 아래는 **승인을 받은 뒤에만** 할 일입니다.

| # | 항목 | 성격 | 필수 여부 | 기존 iOS 영향 |
|---|---|---|---|---|
| ① | `GOOGLE_CLIENT_ID` 에 Web client ID 추가 (GitHub Secret과 서버 env) | 설정 | **Google 로그인에 필수** | 없음. 허용할 aud 가 하나 늘어날 뿐 |
| ② | 카카오 콘솔에 Android 플랫폼 등록, GCP 에 Android OAuth 클라이언트와 Maps Android 키 발급 | 외부 콘솔 | 필수 | 없음 |
| ③ | Apple 로그인 Services ID 지원(aud 에 따라 client_id 선택, 웹 흐름 redirect 엔드포인트) | 코드 | D1 에 따라 결정 | 기존 흐름을 유지하도록 만들면 영향 없음. 회귀 테스트 필요 |
| ④ | 스티커 WebP 변환본 3,161개(23.8MB)를 정적 파일로 추가. 변환은 끝났음(`tools/sticker-webp/out`) | 파일 추가 | **필수 (D3 확정)** | 없음. iOS 는 계속 .heic 를 받음 |
| ⑤ | 업로드 크기 초과가 500 으로 나가는 문제를 413 으로 고침 | 버그 수정 | 선택 | 오류 문구만 정확해짐 |
| ⑥ | 푸시: 기기 등록에 플랫폼 구분 추가, Android 용 FCM 발송기 — 9장 | 기능 | **필수 (D2·D2-1 확정)** | 없음. 플랫폼을 보내지 않는 지금 iOS 요청은 그대로 받고, iOS 는 계속 APNs 로 보냅니다 |

---

## 6. iOS 와 같은 디자인·UX 를 유지하는 방법

1. **토큰을 1:1 로 옮깁니다.** `Tokens.swift` 의 색(라이트와 다크 14종), coverGradient 팔레트 6쌍과 해시 공식, 간격(4~48, 화면 좌우 여백 20), 라운드(10/14/20/28), 그림자를 `core/designsystem` 의 `TMColors`, `TMSpacing`, `TMRadius` 로 그대로 옮깁니다.
   - pt 는 dp 로 같은 숫자를 씁니다.
   - 일차 색 팔레트, 채팅 배경 5색, 카카오 버튼 노랑 `#FEE500`, `CreateTabIcon` 의 `#1A8CF0` 도 같은 hex 로 씁니다.
2. **글꼴 기준표를 만듭니다.** iOS 는 Dynamic Type 을 쓰고 제목은 SF Rounded 입니다. Android 에는 SF 가 없으므로 다음 기준을 둡니다.
   - 크기는 iOS Large 기본값에 맞춥니다: display 34 / title 22 / titleSmall 20 / headline·body 17 / callout 16 / subheadline 15 / footnote 13 / caption 12 (sp, 시스템 글자 크기에 맞춰 커짐).
   - 서체는 **Pretendard 하나로 통일합니다(D4 확정).** 제목도 Pretendard 를 쓰고, iOS 의 둥근 제목(SF Rounded)은 굵기만 맞춥니다(display·title 은 Bold, titleSmall 은 SemiBold). 앱에 직접 넣어 쓰므로 기기마다 달라 보이지 않습니다. 라이선스는 SIL OFL 이라 앱에 넣을 수 있고, 오픈소스 라이선스 화면에 표기합니다. 용량을 줄이려면 쓰는 굵기(Regular·Medium·SemiBold·Bold)만 넣습니다.
3. **Material 기본 모양을 쓰지 않습니다.** Material 3 는 테마와 접근성 기반으로만 쓰고, 버튼·카드·텍스트필드·칩·배너·빈 상태·아바타는 iOS 의 `TM*` 컴포넌트 이름 그대로 직접 만듭니다. 예: 버튼 높이 54, 누르면 0.98배, 비활성 0.45.
4. **고유 비주얼은 Canvas 로 다시 그립니다.** SkyBackground(그라데이션, 해, 능선 2겹), DestinationCover(그라데이션과 비행기 아이콘, Google 사진 출처 표기), 큰 제목 헤더와 44dp 원형 아이콘 버튼, 카카오톡식 스와이프 액션, 인스타그램식 당겨서 새로고침(80dp 에서 진동, 72dp 여백)이 대상입니다.
5. **아이콘**: SF Symbols 는 쓸 수 없습니다. Material Symbols Rounded 에서 가장 가까운 것을 골라 대응표(`docs/icon-map.md`)를 만들고, 맞는 게 없으면 벡터로 그립니다.
6. **Android 관례는 지킵니다.** 시스템 뒤로 가기와 예측형 뒤로 가기, edge-to-edge, 시스템 공유 시트, 권한 요청 흐름은 Android 방식대로 둡니다. "같은 디자인" 은 브랜드, 정보 구조, 화면 구성, 문구, 상호작용 결과까지를 뜻하고, OS 고유 동작까지 흉내 내는 것은 아닙니다.
7. **문구는 그대로 씁니다.** iOS 한국어 문자열을 `strings.xml` 로 옮깁니다(사람이 직접 맞춤). 빈 상태, 오류, 확인 대화상자 문구가 같아야 같은 앱으로 느껴집니다.
8. **눈으로 비교해 검증합니다.**
   - 기준 이미지: iOS 의 `design/appstore-screenshots`(10장)와 iOS `DesignPreview` 모드로 뽑은 화면.
   - Android 쪽: 같은 목데이터로 Compose Preview 와 Roborazzi 스크린샷을 찍어 나란히 놓고 비교합니다.
   - 목데이터는 iOS `InMemoryTripBackend` 를 옮긴 FakeRepository 를 씁니다.

---

## 7. 결정이 필요한 사항

| # | 질문 | 권장 |
|---|---|---|
| D1 | Apple 로그인을 Android 에서 지원하나요? | **결정(2026-11-25): 1차에서는 숨깁니다.** Apple 로만 가입한 iOS 사용자는 Android 에 들어올 수 없습니다. 계정 연결이나 서버 ③ 작업은 2차에서 다시 검토합니다 |
| D2 | 푸시 알림을 이번 범위에 넣나요? | **결정(2026-11-25): 1차 범위에 넣습니다.** iOS 에도 넣을 예정이었는데 빠졌던 기능입니다. 서버 작업은 9장 |
| D2-1 | 푸시 발송 경로 | **결정(2026-11-25, 재결정): A안. iOS 는 지금의 APNs 직접 발송을 그대로 두고, 서버에 Android 용 FCM 발송 경로만 추가합니다.** 처음엔 «FCM 통일» 이었으나 그사이 iOS·서버에 APNs 로 푸시가 들어가 있어 바꿨습니다. iOS 앱은 고치지 않습니다 — 9장 |
| D3 | HEIC 스티커가 일부 기기에서 안 열리면? | **결정(2026-11-25): A안, 서버에 WebP 변환본 추가(④).** Phase 0 결과 HEIC 는 투명 배경이 검게 나오고, WebP 는 정상입니다. Android 는 정지 스티커를 `.webp` 로 요청합니다. [PHASE0.md](PHASE0.md) 2-1-1, 3장 |
| D4 | 글꼴 | **결정(2026-11-25): Pretendard 로 통일.** 제목에도 같은 서체를 쓰고 굵기로 구분합니다 |
| D5 | applicationId | **결정(2026-11-25): `com.joonsby.travelmate`** (iOS 번들 ID 와 같음) |

---

## 8. 단계별 개발 TODO

### Phase 0. 준비와 위험 확인 (약 1주)
- [ ] `travelmate-android` 저장소 만들기, Version Catalog, CI(빌드·린트·단위 테스트)
- [ ] 외부 콘솔: 카카오 Android 플랫폼, GCP Android OAuth 와 Web client ID, Maps Android 키, Play Console 앱 생성(D5)
- [ ] 서버 ① 설정 승인받기 (운영 반영은 사용자가 결정)
- [ ] **실험**: Coil + ImageDecoder 로 Fluent HEIC 스티커 표시 (API 28·31·34 기기)
- [ ] **실험**: `supertoss://send` 가 Android 토스에서 열리는지
- [ ] **실험**: LiveKit Android 로 iOS 와 같은 방에 들어가 통화하기
- [ ] **실험**: Google idToken 이 운영 서버 검증을 통과하는지 (설정 ① 반영 뒤)

### Phase 1. 기반 (약 2주) — 진행 기록은 [PHASE1.md](PHASE1.md)
- [x] core/designsystem: 토큰, 테마(라이트·다크·시스템), TM 컴포넌트, SkyBackground, 커스텀 하단 탭(가운데 +)
- [ ] core/designsystem 나머지: 시트, 스와이프 행, 새로고침 → 처음 쓰는 화면(Phase 2)에서 만든다
- [x] core/network: 오류 파싱(`error.message/details`), Authenticator(Mutex refresh), 오프라인 캐시, 상대 경로를 절대 URL 로 바꾸기
- [ ] core/network DTO: 인증·내 정보만 했다. 나머지는 각 Phase 에서 그 화면과 함께 만든다
- [x] core/data: TokenStore(Keystore), 설정값 DataStore, ChatSocket(인증, ready, 재연결 1→30초, 포그라운드 복귀 시 재연결), TripChangeBus
- [x] 스플래시, 복원 실패, 로그인(카카오·Google), 심사용 로그인, 로그아웃 (실제 계정 로그인 성공은 실기기에서 확인 필요)

### Phase 2. 여행 핵심 (약 3주)
- [ ] 여행 목록, 지난 여행, 휴지통, 여행 만들기·수정(지역 자동완성, 동행 고르기)
- [ ] 여행 상세 세그먼트, 일정 타임라인, 일정 편집(장소 검색), 일괄 삭제
- [ ] 동선 지도(Maps Compose), 장소 정보 시트, 길찾기 링크
- [ ] 체크리스트
- [ ] 동행(초대, 방장 넘기기, 내보내기, 나가기)
- [ ] `tripChanged` 실시간 반영 연결

### Phase 3. 경비·정산 (약 2주)
- [ ] 경비 목록과 편집(외화, 분배, 428 이면 계좌 등록으로 유도, 409 충돌 처리)
- [ ] 정산 카드와 상세, 토스 송금, 계좌 복사
- [ ] 계좌 설정(은행 60곳, 글자 배지)
- [ ] 영수증 OCR (ML Kit + 공통 파서)

### Phase 4. 채팅 (약 3~4주)
- [ ] 채팅 목록(고정, 알림 끄기, 선택 모드, 지난 여행 방, 탭 배지)
- [ ] 채팅방: 페이지 나눠 불러오기(before/after), clientId 로 낙관적 전송, 답장·수정·삭제, 읽음, 공지 배너, 검색
- [ ] 첨부: 사진·동영상·파일 업로드(raw 본문, 진행률), 앨범, 링크, 송금 요청, 공유 카드
- [ ] 스티커 패널(HEIC + Lottie, 피부색, 최근)
- [ ] 뷰어(사진 확대, 동영상, 저장, 공유), 서랍, 공지·게시판, 방 배경
- [ ] 1:1 채팅(초안 방)

### Phase 5. 친구·프로필·안전 (약 1.5주)
- [ ] 친구 코드, 친구 요청·수락, 즐겨찾기, 친구 프로필
- [ ] 내 정보, 프로필 사진 자르기(JPEG 업로드), 닉네임·이메일·상태 메시지 편집
- [ ] 설정(채팅 설정값, 캐시, 화면 모드, 약관), 회원 탈퇴
- [ ] 신고·차단과 차단 목록, 차단 목록이 바뀌면 화면 새로고침

### Phase 6. 통화·AI·부가 기능 (약 2주)
- [ ] 그룹 음성 통화(LiveKit, 포그라운드 서비스, 버블, 스피커 전환, 통화 하나만 유지)
- [ ] AI 동의 게이트, AI 일정 플래너(초안 이어 보기, 사용량), AI 주변 추천과 추천 지도
- [ ] 일정 요약 포스터 공유, 캘린더 넣기(CalendarContract)

### Phase 6-1. 푸시 알림 — Android 쪽 (약 1주, 서버 9-2 가 배포돼야 함)
- [ ] Firebase 프로젝트에 Android 앱 등록, `google-services.json`
- [ ] FCM 토큰 등록: 로그인 직후, 토큰이 바뀔 때(`onNewToken`) → `POST /me/devices {token, platform: "android"}`. 해제: 로그아웃, 탈퇴 → `DELETE /me/devices/:token`
- [ ] 알림 채널 4개(채팅, 통화, 여행 초대, 친구 — 서버 알림 종류와 같음)와 `POST_NOTIFICATIONS` 권한 요청(Android 13 이상)
- [ ] 알림을 누르면 채팅·통화 → 그 채팅방, 초대 → 그 여행, 친구 → 친구 탭 (iOS PushManager 와 같은 규칙, 앱이 꺼져 있을 때도). 지금 보고 있는 채팅방의 알림은 띄우지 않기
- [ ] 설정 화면 알림 항목: `GET/PUT /me/notifications {chat, call, invite, friend}`
- [ ] iOS(APNs)와 Android(FCM)가 같은 채팅방 알림을 함께 받는지 검증

### Phase 7. 함께 쓰기 검증과 출시 (약 2주)
- [ ] **iOS 와 Android 를 섞어 시나리오 검증**: 친구 코드 교환, 여행 초대, 일정 동시 편집 반영, 채팅(글·사진·동영상·스티커·앨범), 읽음 수, 공지, 1:1, 그룹 통화, 경비와 정산이 양쪽에서 같은 금액인지, 신고·차단
- [ ] iOS 와 디자인 나란히 비교(스크린샷 10장 기준)
- [ ] 접근성(TalkBack, 큰 글꼴), 다크 모드, 저사양 기기 성능
- [ ] Play 출시 준비: 데이터 보안 양식, 개인정보 처리방침 URL(기존 것 재사용), 콘텐츠 등급(사용자 간 소통), 심사용 계정 안내, 내부 테스트 → 비공개 테스트(Play 신규 개인 계정은 테스터 요건 확인) → 출시
- [ ] 릴리스 서명, R8 규칙(kotlinx.serialization, LiveKit, Kakao)

예상 기간은 1인 기준 약 17~19주입니다(서버 푸시 작업은 따로). 실험 결과와 남은 결정에 따라 달라질 수 있습니다.

---

## 9. 푸시 알림 — 서버에 필요한 작업 (승인 전에는 손대지 않음)

**D2-1 재결정(2026-11-25): A안.** iOS 는 이미 들어가 있는 APNs 직접 발송을 그대로 두고, 서버에 **Android 용 FCM 발송 경로만 추가**합니다.
처음에는 «FCM 으로 통일» 이었으나, 그사이 iOS(`5ca9c22 푸시 알림`)와 서버에 APNs 방식으로 푸시가 들어가 있어 바꿨습니다.
iOS 앱은 고치지 않습니다.

### 9-1. 지금 있는 것 (2026-11-25 확인)

| 위치 | 내용 |
|---|---|
| `src/services/pushService.js` | APNs 직접 발송(`node:http2`, .p8 키). 발송부는 `setPushTransport` 로 갈아 끼울 수 있고, 알림 내용(`buildPayload`)과 받을 기기 고르기는 플랫폼과 무관한 공통 코드 |
| 알림 종류 | `chat`(새 메시지, 방 알림 끄기 반영), `call`(통화 시작), `invite`(여행 초대), `friend`(친구 요청·수락). 보낸 본인과 차단한 사람은 제외 |
| `push_devices` (migration 018·032) | `token PK, user_id, environment('sandbox'\|'production')`. 플랫폼 구분 없음 |
| `POST /me/devices {token, environment}` · `DELETE /me/devices/:token` | 토큰을 16진수(`^[0-9a-f]{32,200}$`)로만 받음 → **FCM 토큰은 등록되지 않음** |
| `GET/PUT /me/notifications {chat, call, invite, friend}` | 알림 종류별 켜기·끄기. Android 도 그대로 씀 |
| 토큰 정리 | APNs 가 410·`BadDeviceToken` 등을 돌려주면 그 토큰을 지움 |

### 9-2. 서버에 추가할 것

1. **마이그레이션**: `push_devices` 에 `platform` 컬럼(`'ios'` 기본값 \| `'android'`)을 추가하고, `environment` 는 NULL 을 허용합니다(FCM 은 쓰지 않음). 기존 행은 모두 `ios` 가 됩니다.
2. **기기 등록 API 확장**: `POST /me/devices` 가 `{ token, platform: "android" }` 도 받게 합니다.
   - `platform` 이 없으면 지금처럼 iOS 로 보고 16진수·`environment` 검사를 그대로 합니다 → **지금 iOS 앱은 그대로 동작**
   - `android` 면 FCM 토큰 형식(영숫자와 `:` `-` `_`, 길이 제한)으로 검사합니다
   - `DELETE /me/devices/:token` 은 그대로 씁니다
3. **FCM 발송기**: FCM HTTP v1 API. 서비스 계정 키는 환경변수(GitHub Secret)로 받고, 없으면 APNs 처럼 조용히 건너뜁니다.
   - 받을 기기를 `platform` 으로 나눠 iOS 는 기존 APNs 로, Android 는 FCM 으로 보냅니다
   - FCM 이 `UNREGISTERED`·`INVALID_ARGUMENT`(토큰) 를 돌려주면 그 토큰을 지웁니다
   - 메시지는 `data` 위주로 보냅니다(`type`, `tripId?`, `userId?`, 제목·본문). 앱이 채널을 골라 알림을 띄우고, 지금 보고 있는 채팅방이면 띄우지 않기 위해서입니다
4. 알림 종류·대상 고르기·설정 API 는 **바꾸지 않습니다.**

### 9-3. 사용자가 할 일

- Firebase 프로젝트 생성, Android 앱 등록(`com.joonsby.travelmate`, SHA-1·SHA-256) → `google-services.json`
- 서버용 서비스 계정 키 발급 → GitHub Secret 과 서버 env 에 등록 (배포 워크플로의 optional 목록에 추가 필요)

**순서**: 서버 9-2 를 먼저 배포합니다. 바뀌는 것은 등록 API 가 받는 값이 늘고 발송 갈래가 하나 느는 것뿐이라 지금의 iOS 앱은 영향을 받지 않습니다. 그다음 Android Phase 6-1 을 붙입니다.
