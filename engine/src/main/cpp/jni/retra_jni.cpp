// Retra engine - JNI bridge between app.retra.engine.GbaCore and the C++ core.
// The core is built with -fno-exceptions; nothing here throws C++ exceptions.
#include <jni.h>

#include <new>
#include <string>
#include <vector>

#include "gba.h"

using retra::Gba;

namespace {

inline Gba* fromHandle(jlong h) { return reinterpret_cast<Gba*>(static_cast<intptr_t>(h)); }

jbyteArray toByteArray(JNIEnv* env, const std::vector<retra::u8>& v) {
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(v.size()));
    if (!arr) return nullptr;
    if (!v.empty()) env->SetByteArrayRegion(arr, 0, static_cast<jsize>(v.size()), reinterpret_cast<const jbyte*>(v.data()));
    return arr;
}

std::vector<retra::u8> fromByteArray(JNIEnv* env, jbyteArray arr) {
    std::vector<retra::u8> v;
    if (!arr) return v;
    const jsize n = env->GetArrayLength(arr);
    v.resize(static_cast<size_t>(n));
    if (n > 0) env->GetByteArrayRegion(arr, 0, n, reinterpret_cast<jbyte*>(v.data()));
    return v;
}

int saveTypeOrdinal(const Gba& g) {
    // Matches app.retra.engine.SaveType ordinal order.
    return static_cast<int>(g.save.kind);
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_app_retra_engine_GbaCore_nativeCreate(JNIEnv*, jclass) {
    Gba* g = new (std::nothrow) Gba();
    return static_cast<jlong>(reinterpret_cast<intptr_t>(g));
}

JNIEXPORT void JNICALL Java_app_retra_engine_GbaCore_nativeDestroy(JNIEnv*, jclass, jlong h) {
    delete fromHandle(h);
}

// Returns null on success, otherwise an error message.
JNIEXPORT jstring JNICALL Java_app_retra_engine_GbaCore_nativeLoadRom(JNIEnv* env, jclass, jlong h, jbyteArray rom,
                                                                       jbyteArray bios) {
    Gba* g = fromHandle(h);
    if (!g) return env->NewStringUTF("Emulator core is not available");
    if (!rom) return env->NewStringUTF("ROM data is missing");
    std::vector<retra::u8> romData = fromByteArray(env, rom);
    std::vector<retra::u8> biosData = fromByteArray(env, bios);
    std::string err;
    const bool ok = g->loadRom(romData.data(), romData.size(), biosData.empty() ? nullptr : biosData.data(),
                               biosData.size(), err);
    if (ok) return nullptr;
    return env->NewStringUTF(err.empty() ? "Failed to load ROM" : err.c_str());
}

// [title, gameCode, makerCode, version, saveTypeOrdinal, hasRtc(0/1)]
JNIEXPORT jobjectArray JNICALL Java_app_retra_engine_GbaCore_nativeRomInfo(JNIEnv* env, jclass, jlong h) {
    Gba* g = fromHandle(h);
    jclass strCls = env->FindClass("java/lang/String");
    if (!strCls) return nullptr;
    jobjectArray arr = env->NewObjectArray(6, strCls, nullptr);
    if (!arr || !g) return arr;
    const auto& info = g->info();
    bool eepromUnknown = false;
    (void)eepromUnknown;
    const std::string fields[6] = {info.title, info.gameCode, info.makerCode, std::to_string(info.version),
                                   std::to_string(saveTypeOrdinal(*g)), info.hasRtc ? "1" : "0"};
    for (int i = 0; i < 6; i++) {
        jstring s = env->NewStringUTF(fields[i].c_str());
        env->SetObjectArrayElement(arr, i, s);
        env->DeleteLocalRef(s);
    }
    return arr;
}

JNIEXPORT jboolean JNICALL Java_app_retra_engine_GbaCore_nativeEepromSizeUnknown(JNIEnv*, jclass, jlong h) {
    Gba* g = fromHandle(h);
    return (g && g->save.isEeprom() && g->eepromSizePending()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_app_retra_engine_GbaCore_nativeReset(JNIEnv*, jclass, jlong h) {
    if (Gba* g = fromHandle(h)) g->reset();
}

JNIEXPORT void JNICALL Java_app_retra_engine_GbaCore_nativeSetKeys(JNIEnv*, jclass, jlong h, jint mask) {
    if (Gba* g = fromHandle(h)) g->setKeys(static_cast<retra::u16>(mask & 0x3FF));
}

JNIEXPORT jint JNICALL Java_app_retra_engine_GbaCore_nativeRunFrame(JNIEnv* env, jclass, jlong h, jintArray video,
                                                                    jshortArray audio) {
    Gba* g = fromHandle(h);
    if (!g || !g->loaded()) return 0;
    const jsize vlen = video ? env->GetArrayLength(video) : 0;
    const jsize alen = audio ? env->GetArrayLength(audio) : 0;
    const int maxAudio = alen / 2;
    void* vp = (vlen >= 240 * 160) ? env->GetPrimitiveArrayCritical(video, nullptr) : nullptr;
    void* ap = (maxAudio > 0) ? env->GetPrimitiveArrayCritical(audio, nullptr) : nullptr;
    const int n = g->runFrame(static_cast<retra::u32*>(vp), static_cast<retra::s16*>(ap), ap ? maxAudio : 0);
    if (ap) env->ReleasePrimitiveArrayCritical(audio, ap, 0);
    if (vp) env->ReleasePrimitiveArrayCritical(video, vp, 0);
    return n;
}

JNIEXPORT jbyteArray JNICALL Java_app_retra_engine_GbaCore_nativeBatterySave(JNIEnv* env, jclass, jlong h) {
    Gba* g = fromHandle(h);
    if (!g || !g->hasBattery()) return nullptr;
    return toByteArray(env, g->batterySave());
}

JNIEXPORT jboolean JNICALL Java_app_retra_engine_GbaCore_nativeLoadBatterySave(JNIEnv* env, jclass, jlong h,
                                                                               jbyteArray data) {
    Gba* g = fromHandle(h);
    if (!g || !data) return JNI_FALSE;
    std::vector<retra::u8> d = fromByteArray(env, data);
    return g->loadBatterySave(d.data(), d.size()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_app_retra_engine_GbaCore_nativeConsumeSaveDirty(JNIEnv*, jclass, jlong h) {
    Gba* g = fromHandle(h);
    return (g && g->consumeSaveDirty()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_app_retra_engine_GbaCore_nativeSaveState(JNIEnv* env, jclass, jlong h) {
    Gba* g = fromHandle(h);
    if (!g || !g->loaded()) return env->NewByteArray(0);
    return toByteArray(env, g->saveState());
}

JNIEXPORT jboolean JNICALL Java_app_retra_engine_GbaCore_nativeLoadState(JNIEnv* env, jclass, jlong h,
                                                                         jbyteArray data) {
    Gba* g = fromHandle(h);
    if (!g || !data) return JNI_FALSE;
    std::vector<retra::u8> d = fromByteArray(env, data);
    return g->loadState(d.data(), d.size()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_app_retra_engine_GbaCore_nativeSetCheats(JNIEnv* env, jclass, jlong h,
                                                                     jobjectArray codes) {
    Gba* g = fromHandle(h);
    if (!g) return 0;
    std::vector<std::string> list;
    const jsize n = codes ? env->GetArrayLength(codes) : 0;
    for (jsize i = 0; i < n; i++) {
        jstring s = static_cast<jstring>(env->GetObjectArrayElement(codes, i));
        if (!s) continue;
        const char* c = env->GetStringUTFChars(s, nullptr);
        if (c) {
            list.emplace_back(c);
            env->ReleaseStringUTFChars(s, c);
        }
        env->DeleteLocalRef(s);
    }
    return g->setCheats(list);
}

JNIEXPORT void JNICALL Java_app_retra_engine_GbaCore_nativeSetColorCorrection(JNIEnv*, jclass, jlong h,
                                                                              jboolean enabled) {
    if (Gba* g = fromHandle(h)) g->setColorCorrection(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_app_retra_engine_GbaCore_nativeSetRtcOffsetSeconds(JNIEnv*, jclass, jlong h,
                                                                               jlong offset) {
    if (Gba* g = fromHandle(h)) g->setRtcOffsetSeconds(static_cast<retra::s64>(offset));
}

}  // extern "C"
