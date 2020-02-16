//
// VirtualApp Native Project
//

#ifndef NDK_LOG_H
#define NDK_LOG_H

#include "VAJni.h"


#define NATIVE_METHOD(func_ptr, func_name, signature) { func_name, signature, reinterpret_cast<void*>(func_ptr) }

class ScopeUtfString {
public:
    ScopeUtfString(jstring j_str);


    const char *c_str() {
        return _c_str;
    }

    ~ScopeUtfString();

private:
    jstring _j_str;
    const char *_c_str;
};


#endif //NDK_LOG_H
