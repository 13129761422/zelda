#include <elf.h>//
// VirtualApp Native Project
//
#include <Foundation/IOUniformer.h>
#include "VAJni.h"


char *origin_package_name;
char *now_package_name;
size_t now_package_name_length;
size_t origin_package_name_length;

extern "C"
void JNICALL
Java_com_virjar_zelda_engine_NativeEngine_nativeEnableIORedirect(JNIEnv *env, jclass,
                                                                 jstring selfSoPath,
                                                                 jint apiLevel,
                                                                 jint preview_api_level,
                                                                 jstring originPkg, jstring nowPkg
) {
    origin_package_name = strdup(ScopeUtfString(originPkg).c_str());
    now_package_name = strdup(ScopeUtfString(nowPkg).c_str());

    origin_package_name_length = strlen(origin_package_name);
    now_package_name_length = strlen(now_package_name);

    ScopeUtfString so_path(selfSoPath);
    IOUniformer::startUniformer(so_path.c_str(), apiLevel, preview_api_level);
}

extern "C"
void JNICALL
Java_com_virjar_zelda_engine_NativeEngine_nativeIOWhitelist(JNIEnv *env, jclass jclazz,
                                                            jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::whitelist(path.c_str());
}

extern "C"
void JNICALL
Java_com_virjar_zelda_engine_NativeEngine_nativeIOForbid(JNIEnv *env, jclass jclazz,
                                                         jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::forbid(path.c_str());
}

extern "C"
void JNICALL
Java_com_virjar_zelda_engine_NativeEngine_nativeIORedirect(JNIEnv *env, jclass jclazz,
                                                           jstring origPath,
                                                           jstring newPath) {
    ScopeUtfString orig_path(origPath);
    ScopeUtfString new_path(newPath);
    IOUniformer::redirect(orig_path.c_str(), new_path.c_str());

}

extern "C"
jstring JNICALL
Java_com_virjar_zelda_engine_NativeEngine_nativeGetRedirectedPath(JNIEnv *env, jclass jclazz,
                                                                  jstring origPath) {
    ScopeUtfString orig_path(origPath);
    const char *redirected_path = IOUniformer::query(orig_path.c_str());
    if (redirected_path != nullptr) {
        return env->NewStringUTF(redirected_path);
    }
    return nullptr;
}

extern "C"
jstring JNICALL
Java_com_virjar_zelda_engine_NativeEngine_nativeReverseRedirectedPath(JNIEnv *env, jclass jclazz,
                                                                      jstring redirectedPath) {
    ScopeUtfString redirected_path(redirectedPath);
    const char *orig_path = IOUniformer::reverse(redirected_path.c_str());
    return env->NewStringUTF(orig_path);
}


jclass nativeEngineClass;
JavaVM *vm;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *_vm, void *) {
    vm = _vm;
    JNIEnv *env;
    _vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);

    nativeEngineClass = (jclass) env->NewGlobalRef(env->FindClass(JNI_CLASS_NAME));
    static JNINativeMethod methods[] = {
            {"nativeReverseRedirectedPath", "(Ljava/lang/String;)Ljava/lang/String;",                      (void *) Java_com_virjar_zelda_engine_NativeEngine_nativeReverseRedirectedPath},
            {"nativeGetRedirectedPath",     "(Ljava/lang/String;)Ljava/lang/String;",                      (void *) Java_com_virjar_zelda_engine_NativeEngine_nativeGetRedirectedPath},
            {"nativeIORedirect",            "(Ljava/lang/String;Ljava/lang/String;)V",                     (void *) Java_com_virjar_zelda_engine_NativeEngine_nativeIORedirect},
            {"nativeIOWhitelist",           "(Ljava/lang/String;)V",                                       (void *) Java_com_virjar_zelda_engine_NativeEngine_nativeIOWhitelist},
            {"nativeIOForbid",              "(Ljava/lang/String;)V",                                       (void *) Java_com_virjar_zelda_engine_NativeEngine_nativeIOForbid},
            {"nativeEnableIORedirect",      "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", (void *) Java_com_virjar_zelda_engine_NativeEngine_nativeEnableIORedirect},
    };

    if (env->RegisterNatives(nativeEngineClass, methods, 6) < 0) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

JNIEnv *getEnv() {
    JNIEnv *env;
    vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *ensureEnvCreated() {
    JNIEnv *env = getEnv();
    if (env == nullptr) {
        vm->AttachCurrentThread(&env, nullptr);
    }
    return env;
}

void DetachCurrentThread() {
    vm->DetachCurrentThread();
}

extern "C" __attribute__((constructor)) void _init(void) {
    IOUniformer::init_env_before_all();
}


