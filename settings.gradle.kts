pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LiveKit 의 일부 하위 의존성(audioswitch)이 jitpack 에 있음
        maven("https://jitpack.io")
    }
}

rootProject.name = "travelmate-android"
include(":app")
// Phase 0 실험 전용. 결론이 나면 모듈째 지운다
include(":spike")
