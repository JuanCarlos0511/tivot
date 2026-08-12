# MEMORY_STATE.md — Tivot Development State

## Last Updated
2026-08-11 — Etapa 1 Complete

## Current Stage
**Etapa 1: Cimientos Nativos, Estructura del Repositorio y Carga del Modelo Tivot** ✅

## Architectural Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Package `core.jni` instead of `core.native` | `native` is a Java/Kotlin reserved keyword. Using `core.jni` avoids JNI naming conflicts and IDE issues. |
| 2 | Conditional `#ifdef LLAMA_AVAILABLE` in C++ | Allows the project to compile and run in stub mode without the llama.cpp submodule, enabling UI/logic development in parallel. |
| 3 | `LlamaBridge` as Kotlin `object` (singleton) | JNI bridges should be singleton since they manage global native state. Thread safety handled via C++ `std::mutex`. |
| 4 | `NativeInferenceEngine` wraps all JNI calls | All native calls routed through coroutines (`Dispatchers.IO`). Never exposes raw JNI to upper layers. |
| 5 | `DispatcherProvider` interface for DI | Enables test substitution of dispatchers without `Dispatchers.setMain()` hacks. |
| 6 | `MemoryMonitor` uses `runBlocking` in `onTrimMemory` | LMK callbacks are synchronous and time-critical. The model MUST be freed before the callback returns, or Android kills the process. |
| 7 | `ModelState.Evicted` state added | Distinguishes "user uninstalled" (→ Idle) from "system evicted due to memory pressure" (→ Evicted) for UI messaging. |
| 8 | Model target: Qwen2.5-0.5B-Instruct-Q4_K_M.gguf | ~491 MB on disk, ~350 MB mmap'd. Superior Spanish comprehension vs TinyLlama at lower RAM footprint. |
| 9 | `largeHeap="false"` in AndroidManifest | Forces strict memory discipline. No JVM heap crutch. All model memory stays in native C++ via mmap. |
| 10 | Hilt DI from Etapa 1 | Avoids costly refactoring later. All singletons properly scoped from the start. |

## Files Created (Etapa 1)

### Build System (7 files)
- `settings.gradle.kts` — Plugin management, repository config
- `build.gradle.kts` — Root build file with plugin declarations
- `gradle/libs.versions.toml` — Centralized version catalog
- `gradle.properties` — JVM args, AndroidX config
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 8.11.1
- `app/build.gradle.kts` — App module with NDK/CMake/Compose/Hilt
- `app/proguard-rules.pro` — ProGuard rules for JNI + Hilt

### Android Entry Point (5 files)
- `app/src/main/AndroidManifest.xml` — Permissions, landscape, largeHeap=false
- `app/src/main/res/values/strings.xml` — App name
- `app/src/main/res/values/themes.xml` — Dark theme base
- `TivotApplication.kt` — @HiltAndroidApp, registers MemoryMonitor
- `presentation/main/MainActivity.kt` — Minimal Compose shell

### Core Native / JNI (5 files)
- `app/src/main/cpp/CMakeLists.txt` — Conditional llama.cpp inclusion
- `app/src/main/cpp/native-lib.cpp` — Full JNI bridge (stub + real modes)
- `core/jni/LlamaBridge.kt` — Kotlin external declarations
- `core/jni/NativeInferenceEngine.kt` — Coroutine-safe wrapper with StateFlow
- `core/jni/NativeModule.kt` — Hilt module placeholder

### Core Memory (3 files)
- `core/memory/MemoryPressureLevel.kt` — NORMAL/MODERATE/LOW/CRITICAL enum
- `core/memory/MemoryMonitor.kt` — ComponentCallbacks2 with SharedFlow
- `core/memory/MemoryModule.kt` — Hilt module placeholder

### Core Utility (2 files)
- `core/util/DispatcherProvider.kt` — Interface + DefaultDispatcherProvider
- `core/util/UtilModule.kt` — Hilt @Binds module

### Domain Models (2 files)
- `domain/model/ModelState.kt` — Sealed class (Idle/Downloading/Validating/Ready/Error/Evicted)
- `domain/model/ModelConfig.kt` — Data class with hardware-tuned defaults

### Data Source (1 file)
- `data/source/TivotModelLoader.kt` — Model lifecycle manager

### Tests (3 files)
- `androidTest/.../LlamaBridgeStubTest.kt` — JNI stub validation (6 tests)
- `androidTest/.../TivotModelLoaderTest.kt` — State machine validation (7 tests)
- `test/.../MemoryMonitorTest.kt` — Pressure level emission (5 tests)

### Documentation (3 files)
- `SYSTEM_PROMPT_RULES.md` — Architecture rules
- `MEMORY_STATE.md` — This file
- `README.md` — Project overview and build instructions

**Total: ~28 files, 18 tests**

## Open Technical Debts

| # | Debt | Target Etapa |
|---|------|-------------|
| 1 | llama.cpp submodule not yet cloned | Etapa 1.5 (manual step) |
| 2 | No real .gguf model for integration testing | Etapa 1.5 |
| 3 | `NativeModule` is empty (no interface bindings yet) | Etapa 2 (AiRepository) |
| 4 | `MemoryModule` is empty (constructor injection suffices) | N/A |
| 5 | Greedy sampling in native-lib.cpp (no temperature/top-p) | Etapa 2 |
| 6 | No streaming token callback from JNI | Etapa 2 |
| 7 | MainActivity is minimal placeholder | Etapa 4 |
| 8 | No Room database yet | Etapa 3 |
| 9 | No Cloud API fallback | Etapa 2 |

## Memory Budget Estimation (Etapa 1)

| Component | Estimated RAM |
|-----------|--------------|
| Android OS + System UI | ~800 MB |
| JVM Heap (app) | ~50-80 MB (idle) |
| Qwen2.5-0.5B Q4_K_M (mmap'd) | ~350 MB |
| KV Cache (n_ctx=512) | ~32 MB |
| Native working memory | ~50 MB |
| **Total** | **~1.3 GB** |
| **Remaining from 3 GB** | **~1.7 GB headroom** |

## Next Step
**Etapa 2:** Motor de Inferencia Híbrido (Local GGUF + Remote API Fallback)
- Implement `AiRepository` interface in domain layer
- Implement `HybridAiRepositoryImpl` in data layer
- Add Retrofit/Ktor for Gemini/OpenAI API
- Add streaming token callback via JNI
- Add settings DataStore for API_KEY management
