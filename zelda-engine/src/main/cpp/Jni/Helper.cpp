
#include <jni.h>
#include <bits/sysconf.h>
#include <sys/mman.h>
#include <zconf.h>
#include "Helper.h"

ScopeUtfString::ScopeUtfString(jstring j_str) {
    _j_str = j_str;
    _c_str = getEnv()->GetStringUTFChars(j_str, nullptr);
}

ScopeUtfString::~ScopeUtfString() {
    getEnv()->ReleaseStringUTFChars(_j_str, _c_str);
}
