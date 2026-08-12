# ARCHITECTURE & SYSTEM RULES: TIVOT CODE-LEARNING TABLET APP

## 1. PROJECT IDENTITY & HARDWARE CONSTRAINTS
- **Target Platform:** Android Tablet (Landscape 16:10), Android 10+ (API 29+).
- **Hardware Profile:** 3 GB Total RAM, 1024 MB JVM Heap limit, eMMC 5.1 Storage.
- **Core Engine:** Hybrid Edge AI (Local GGUF via llama.cpp NDK/JNI + Cloud API Fallback).
- **Target Model:** Qwen2.5-0.5B-Instruct-Q4_K_M.gguf (~491 MB) or Qwen3-0.6B-Q4_K_M.gguf (~397 MB).
- **Language & UI:** 100% Native Kotlin, Jetpack Compose, Material 3, Clean Architecture + MVI.

## 2. VIBECODER & BIG-TECH WORKFLOW RULES
Every task executed by the AI must strictly follow this protocol:
1. **Spec-Driven Development:** Before writing code, briefly state:
   - What component is being modified.
   - What architectural layer it belongs to (Domain, Data, Presentation).
   - Memory impact estimation (RAM / Thread allocation).
2. **Atomic Context Maintenance:** Maintain a `MEMORY_STATE.md` file in the root. Update it at the end of every prompt with current progress, architectural decisions, and open technical debts.
3. **Incremental Implementation:** Write fully implemented, production-grade code. Never use `// TODO: Implement this later` or stub functions in critical logic.

## 3. ARCHITECTURAL PATTERNS & STRUCTURE
Use Clean Architecture with MVI (Model-View-Intent) for UI state management.

```
app/src/main/java/com/codecroco/tivot/
├── core/
│   ├── memory/       # LowMemoryKiller (LMK) hooks, MemoryMonitor, MemoryTrimListener
│   ├── jni/          # JNI Bindings for llama.cpp (C++ bridge), NativeInferenceEngine
│   └── util/         # Resource wrappers, Dispatcher providers
├── data/
│   ├── local/        # Room Database, FTS5 Entities, DAOs, File Storage
│   ├── remote/       # Cloud LLM API DTOs & Services (Gemini/OpenAI)
│   ├── repository/   # HybridAiRepositoryImpl, ChatRepositoryImpl, LessonRepositoryImpl
│   └── source/       # LocalModelLoader ("Tivot Installer"), SharedPrefs/DataStore
├── domain/
│   ├── model/        # Pure Kotlin Data Classes (Message, Lesson, ModelConfig)
│   ├── repository/   # Repository Interfaces
│   └── usecase/      # GenerateSocraticFeedbackUseCase, ProcessPromptUseCase, TrimMemoryUseCase
└── presentation/
    ├── main/         # MainActivity, Root Navigation
    ├── chat/         # ChatScreen, ChatViewModel, ChatContract (State, Intent, Effect)
    ├── components/   # TivotCanvas, SpeechBubble, CodeDiagram, Sidebar
    └── theme/        # Color.kt (#0F0F0F, #202020, #5DD62C, #337418, #F8F8F8), Type.kt
```

## 4. HYBRID AI ENGINE SPECIFICATION
- **Local Engine ("Tivot Model"):** Loaded natively via JNI calling `llama.cpp`. The GGUF file resides in `context.filesDir`. Allocation must happen in Native C++ Memory (`mmap`), NOT in the Java Heap.
- **Model Installer Metaphor:** Downloading or setting up "Tivot" in the UI triggers the allocation and validation of the local GGUF model file.
- **Cloud Fallback:** `AiRepository` automatically switches to the Cloud API if:
  1. The local model is not downloaded/initialized.
  2. The system triggers `TRIM_MEMORY_RUNNING_CRITICAL`.
  3. An `API_KEY` is provided in settings AND configured to override local inference.

## 5. BANNED PATTERNS & ANTI-PATTERNS (STRICT)
❌ **NO Main Thread I/O or Inference:** Never execute database, file reading, or JNI `llama.cpp` calls on `Dispatchers.Main`. Always use custom, isolated coroutine contexts (`Dispatchers.IO` or a single-threaded execution context).
❌ **NO Heavy Java Reflection:** Do not use libraries that rely heavily on runtime reflection. Use Kotlinx.Serialization and compile-time code generation (KSP / Room / Hilt).
❌ **NO Memory Leak Invocations:** Do not pass `Context` references to ViewModels or Native C++ wrappers. Use `ApplicationContext` or dependency injection wrappers.
❌ **NO Ignoring LMK Events:** Every native wrapper MUST register an explicit memory release handle (`llama_free`) connected to `ComponentCallbacks2.onTrimMemory`.
❌ **NO Standard Linear Chat UI:** Do not build standard list-item messaging threads. UI must render spatial floating elements over a central canvas.

## 6. MEMORY MANAGEMENT MANDATE
- `n_ctx` default: 512 (Max 1024).
- `n_batch` default: 32.
- `threads` default: 2 (Never consume all 4 vCPUs).
- Memory listening: Handle `ON_TRIM_MEMORY_RUNNING_LOW` and `ON_TRIM_MEMORY_CRITICAL` immediately by flushing the KV Cache via JNI.
