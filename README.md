# 🦖 Tivot — AI-Powered Code Learning for Kids

Tivot is a gamified programming tutor for children, running on Android tablets with **local AI inference** powered by llama.cpp and GGUF models. Designed for devices with as little as **3 GB of RAM**.

## 🏗️ Architecture

- **Language:** 100% Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVI
- **AI Engine:** Hybrid (Local llama.cpp via JNI + Cloud API fallback)
- **Target Model:** Qwen2.5-0.5B-Instruct-Q4_K_M.gguf (~491 MB)
- **Memory Budget:** Native model ~430 MB, JVM Heap <150 MB

## 📋 Prerequisites

- Android Studio Hedgehog+ (2024.1+)
- Android SDK: API 35 (compileSdk), API 29+ (minSdk)
- NDK r27+ (`27.0.12077973`)
- CMake 3.22.1 (install via SDK Manager)
- JDK 17

## 🔧 Build

```bash
# Clone the repository
git clone https://github.com/your-org/tivot.git
cd tivot

# (Optional) Add llama.cpp as submodule for real inference
git submodule add https://github.com/ggerganov/llama.cpp.git app/src/main/cpp/llama.cpp

# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumented tests (requires emulator or device)
./gradlew :app:connectedDebugAndroidTest
```

## 🧪 Stub Mode

Without the llama.cpp submodule, the app compiles in **stub mode**: all JNI calls return mock values. This allows UI and logic development without a real model.

## 📁 Project Structure

```
tivot/
├── SYSTEM_PROMPT_RULES.md     # Architecture rules for AI code assistants
├── MEMORY_STATE.md            # Development state and decisions tracker
├── app/src/main/
│   ├── cpp/                   # C++ JNI bridge + CMake
│   │   ├── CMakeLists.txt
│   │   ├── native-lib.cpp
│   │   └── llama.cpp/         # (git submodule, optional)
│   └── java/com/codecroco/tivot/
│       ├── core/jni/          # LlamaBridge, NativeInferenceEngine
│       ├── core/memory/       # MemoryMonitor (LMK), MemoryPressureLevel
│       ├── core/util/         # DispatcherProvider
│       ├── data/source/       # TivotModelLoader
│       ├── domain/model/      # ModelState, ModelConfig
│       └── presentation/      # MainActivity (minimal in Etapa 1)
└── gradle/libs.versions.toml  # Centralized dependency versions
```

## 🗺️ Development Roadmap

| Etapa | Description | Status |
|-------|-------------|--------|
| 1 | Native Foundations & Model Loading | ✅ Complete |
| 2 | Hybrid Inference Engine (Local + Cloud) | 🔲 Pending |
| 3 | Persistence & Lightweight RAG (Room + FTS5) | 🔲 Pending |
| 4 | Gamified UI in Jetpack Compose | 🔲 Pending |
| 5 | Socratic Pedagogical System | 🔲 Pending |
| 6 | Profiling, Optimization & Hardening | 🔲 Pending |

## 📄 License

Proprietary — CodeCroco © 2026