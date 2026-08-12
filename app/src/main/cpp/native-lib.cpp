#include <jni.h>
#include <string>
#include <mutex>
#include <vector>
#include <android/log.h>

#define LOG_TAG "TivotNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef LLAMA_AVAILABLE
#include "llama.h"
#endif

std::mutex g_mutex;

#ifdef LLAMA_AVAILABLE
llama_model* g_model = nullptr;
llama_context* g_ctx = nullptr;
#endif

extern "C" JNIEXPORT jboolean JNICALL
Java_com_codecroco_tivot_core_jni_LlamaBridge_initModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jint nCtx,
        jint nThreads) {

    std::lock_guard<std::mutex> lock(g_mutex);
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    
#ifdef LLAMA_AVAILABLE
    LOGI("Loading model from %s", path);
    llama_backend_init();
    
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;
    
    g_model = llama_model_load_from_file(path, model_params);
    if (g_model == nullptr) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_threads = nThreads;
    
    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (g_ctx == nullptr) {
        LOGE("Failed to create context with model");
        llama_model_free(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }
    
    LOGI("Model loaded successfully");
    env->ReleaseStringUTFChars(modelPath, path);
    return JNI_TRUE;
#else
    LOGI("Stub mode: initModel called with path %s", path);
    env->ReleaseStringUTFChars(modelPath, path);
    return JNI_TRUE;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_codecroco_tivot_core_jni_LlamaBridge_completion(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt,
        jint nBatch) {
        
    std::lock_guard<std::mutex> lock(g_mutex);
    
    const char* c_prompt = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str(c_prompt);
    env->ReleaseStringUTFChars(prompt, c_prompt);
    
#ifdef LLAMA_AVAILABLE
    if (!g_model || !g_ctx) {
        return env->NewStringUTF("[Error] Model not initialized");
    }
    
    std::vector<llama_token> tokens_list(prompt_str.length() + 2);
    int n_tokens = llama_tokenize(g_model, prompt_str.c_str(), prompt_str.length(), tokens_list.data(), tokens_list.size(), true, false);
    
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(g_model, prompt_str.c_str(), prompt_str.length(), tokens_list.data(), tokens_list.size(), true, false);
    }
    tokens_list.resize(n_tokens);
    
    llama_batch batch = llama_batch_get_one(tokens_list.data(), n_tokens, 0, 0);
    
    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("llama_decode() failed");
        return env->NewStringUTF("[Error] Decode failed");
    }
    
    std::string result = "";
    int n_cur = n_tokens;
    int max_tokens = 256;
    
    while (n_cur <= n_tokens + max_tokens) {
        auto* logits = llama_get_logits(g_ctx);
        auto n_vocab = llama_n_vocab(g_model);
        
        std::vector<llama_token_data> candidates;
        candidates.reserve(n_vocab);
        for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
            candidates.emplace_back(llama_token_data{token_id, logits[token_id], 0.0f});
        }
        
        llama_token_data_array candidates_p = {candidates.data(), candidates.size(), false};
        
        // Simple greedy sampling
        llama_token new_token_id = candidates_p.data[0].id;
        for (size_t i = 1; i < candidates_p.size; i++) {
            if (candidates_p.data[i].logit > candidates_p.data[0].logit) {
                new_token_id = candidates_p.data[i].id;
                candidates_p.data[0] = candidates_p.data[i];
            }
        }
        
        if (new_token_id == llama_token_eos(g_model)) {
            break;
        }
        
        std::vector<char> buf(128, 0);
        int piece_len = llama_token_to_piece(g_model, new_token_id, buf.data(), buf.size(), 0, false);
        if (piece_len > 0) {
            result += std::string(buf.data(), piece_len);
        }
        
        batch = llama_batch_get_one(&new_token_id, 1, n_cur, 0);
        if (llama_decode(g_ctx, batch) != 0) {
            break;
        }
        n_cur += 1;
    }
    
    return env->NewStringUTF(result.c_str());
#else
    std::string stub_res = "[Stub] Tivot response to: " + prompt_str;
    return env->NewStringUTF(stub_res.c_str());
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_com_codecroco_tivot_core_jni_LlamaBridge_clearKVCache(
        JNIEnv* env,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef LLAMA_AVAILABLE
    if (g_ctx) {
        llama_kv_cache_clear(g_ctx);
        LOGI("KV cache cleared");
    }
#else
    LOGI("Stub: KV cache cleared");
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_com_codecroco_tivot_core_jni_LlamaBridge_freeModel(
        JNIEnv* env,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef LLAMA_AVAILABLE
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    llama_backend_free();
    LOGI("Model freed");
#else
    LOGI("Stub: model freed");
#endif
}
