//
// VirtualApp Native Project
//

#ifndef NDK_CORE_H
#define NDK_CORE_H

#include <jni.h>
#include <stdlib.h>


#include "Helper.h"
#include "Foundation/IOUniformer.h"

//com.virjar.zelda.engine.NativeEngine
#define JNI_CLASS_NAME "com/virjar/zelda/engine/NativeEngine"

extern jclass nativeEngineClass;
extern JavaVM *vm;

extern char *origin_package_name;
extern char *now_package_name;
extern size_t now_package_name_length;
extern size_t origin_package_name_length;


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved);

JNIEnv *getEnv();

JNIEnv *ensureEnvCreated();

void DetachCurrentThread();

#define FREE(ptr, org_ptr) { if ((void*) ptr != NULL && (void*) ptr != (void*) org_ptr) { free((void*) ptr); } }

#endif //NDK_CORE_H
