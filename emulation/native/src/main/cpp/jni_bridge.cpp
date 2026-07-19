#include <jni.h>

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "libretro_mgba_engine.h"
#include "reference_engine.h"

namespace {
retra::ReferenceEngine* fromHandle(jlong handle) {
    return reinterpret_cast<retra::ReferenceEngine*>(handle);
}

retra::LibretroMgbaEngine* fromMgbaHandle(jlong handle) {
    return reinterpret_cast<retra::LibretroMgbaEngine*>(handle);
}

std::vector<std::uint8_t> toVector(JNIEnv* env, jbyteArray source) {
    const auto length = env->GetArrayLength(source);
    std::vector<std::uint8_t> output(static_cast<std::size_t>(length));
    env->GetByteArrayRegion(source, 0, length, reinterpret_cast<jbyte*>(output.data()));
    return output;
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new retra::ReferenceEngine());
}

extern "C" JNIEXPORT void JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    delete fromHandle(handle);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeLoadRom(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray rom,
    jstring sha256
) {
    if (handle == 0 || rom == nullptr || sha256 == nullptr) return JNI_FALSE;
    const char* hashChars = env->GetStringUTFChars(sha256, nullptr);
    const std::string hash(hashChars == nullptr ? "" : hashChars);
    if (hashChars != nullptr) env->ReleaseStringUTFChars(sha256, hashChars);
    return fromHandle(handle)->loadRom(toVector(env, rom), hash) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeStep(
    JNIEnv* env,
    jobject,
    jlong handle,
    jint inputMask,
    jfloat speedMultiplier
) {
    if (handle == 0) return nullptr;
    const auto pixels = fromHandle(handle)->step(static_cast<std::uint32_t>(inputMask), speedMultiplier);
    auto result = env->NewIntArray(static_cast<jsize>(pixels.size()));
    if (result != nullptr) env->SetIntArrayRegion(result, 0, static_cast<jsize>(pixels.size()), pixels.data());
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeSerialize(JNIEnv* env, jobject, jlong handle) {
    if (handle == 0) return nullptr;
    const auto state = fromHandle(handle)->serialize();
    auto result = env->NewByteArray(static_cast<jsize>(state.size()));
    if (result != nullptr) env->SetByteArrayRegion(result, 0, static_cast<jsize>(state.size()), reinterpret_cast<const jbyte*>(state.data()));
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeDeserialize(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray state
) {
    if (handle == 0 || state == nullptr) return JNI_FALSE;
    return fromHandle(handle)->deserialize(toVector(env, state)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_app_retra_emulation_nativecore_NativeBridge_nativeReset(JNIEnv*, jobject, jlong handle) {
    if (handle != 0) fromHandle(handle)->reset();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeIsAvailable(JNIEnv*, jobject) {
    return retra::LibretroMgbaEngine::canOpen() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeCreate(JNIEnv*, jobject) {
    auto engine = std::make_unique<retra::LibretroMgbaEngine>();
    if (!engine->available()) return 0;
    return reinterpret_cast<jlong>(engine.release());
}

extern "C" JNIEXPORT void JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    delete fromMgbaHandle(handle);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeLoadRom(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray rom
) {
    if (handle == 0 || rom == nullptr) return JNI_FALSE;
    return fromMgbaHandle(handle)->loadRom(toVector(env, rom)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeStep(
    JNIEnv* env,
    jobject,
    jlong handle,
    jint inputMask,
    jfloat speedMultiplier
) {
    if (handle == 0) return nullptr;
    const auto pixels = fromMgbaHandle(handle)->step(static_cast<std::uint32_t>(inputMask), speedMultiplier);
    auto result = env->NewIntArray(static_cast<jsize>(pixels.size()));
    if (result != nullptr && !pixels.empty()) {
        env->SetIntArrayRegion(result, 0, static_cast<jsize>(pixels.size()), pixels.data());
    }
    return result;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeDrainAudio(JNIEnv* env, jobject, jlong handle) {
    if (handle == 0) return nullptr;
    const auto samples = fromMgbaHandle(handle)->drainAudio();
    auto result = env->NewShortArray(static_cast<jsize>(samples.size()));
    if (result != nullptr && !samples.empty()) {
        env->SetShortArrayRegion(result, 0, static_cast<jsize>(samples.size()), samples.data());
    }
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeSerialize(JNIEnv* env, jobject, jlong handle) {
    if (handle == 0) return nullptr;
    const auto state = fromMgbaHandle(handle)->serialize();
    auto result = env->NewByteArray(static_cast<jsize>(state.size()));
    if (result != nullptr && !state.empty()) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(state.size()), reinterpret_cast<const jbyte*>(state.data()));
    }
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeDeserialize(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray state
) {
    if (handle == 0 || state == nullptr) return JNI_FALSE;
    return fromMgbaHandle(handle)->deserialize(toVector(env, state)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeBatterySave(JNIEnv* env, jobject, jlong handle) {
    if (handle == 0) return nullptr;
    const auto save = fromMgbaHandle(handle)->batterySave();
    auto result = env->NewByteArray(static_cast<jsize>(save.size()));
    if (result != nullptr && !save.empty()) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(save.size()), reinterpret_cast<const jbyte*>(save.data()));
    }
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeRestoreBattery(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray save
) {
    if (handle == 0 || save == nullptr) return JNI_FALSE;
    return fromMgbaHandle(handle)->restoreBattery(toVector(env, save)) ? JNI_TRUE : JNI_FALSE;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeSetCheats(
    JNIEnv* env,
    jobject,
    jlong handle,
    jobjectArray codes
) {
    if (handle == 0 || codes == nullptr) return JNI_FALSE;
    const jsize count = env->GetArrayLength(codes);
    if (count < 0 || count > 512) return JNI_FALSE;
    std::vector<std::string> values;
    values.reserve(static_cast<std::size_t>(count));
    for (jsize index = 0; index < count; ++index) {
        auto value = static_cast<jstring>(env->GetObjectArrayElement(codes, index));
        if (value == nullptr) return JNI_FALSE;
        const char* chars = env->GetStringUTFChars(value, nullptr);
        if (chars == nullptr) {
            env->DeleteLocalRef(value);
            return JNI_FALSE;
        }
        values.emplace_back(chars);
        env->ReleaseStringUTFChars(value, chars);
        env->DeleteLocalRef(value);
    }
    return fromMgbaHandle(handle)->setCheats(values) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeClearCheats(JNIEnv*, jobject, jlong handle) {
    if (handle != 0) fromMgbaHandle(handle)->clearCheats();
}

extern "C" JNIEXPORT void JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeReset(JNIEnv*, jobject, jlong handle) {
    if (handle != 0) fromMgbaHandle(handle)->reset();
}

extern "C" JNIEXPORT jint JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeWidth(JNIEnv*, jobject, jlong handle) {
    return handle != 0 ? fromMgbaHandle(handle)->width() : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeHeight(JNIEnv*, jobject, jlong handle) {
    return handle != 0 ? fromMgbaHandle(handle)->height() : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_app_retra_emulation_nativecore_MgbaBridge_nativeSampleRate(JNIEnv*, jobject, jlong handle) {
    return handle != 0 ? fromMgbaHandle(handle)->sampleRate() : 0;
}
