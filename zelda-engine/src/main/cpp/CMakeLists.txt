cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_STANDARD 14)


message("Building ${LIB_NAME} for Android(${CMAKE_SYSTEM_PROCESSOR})...")

ENABLE_LANGUAGE(ASM)

set(LIBRARY_NAME ${LIB_NAME})

set(ZELDA_NATIVE_SOURCE
        Jni/VAJni.cpp
        Jni/Helper.cpp
        Foundation/IOUniformer.cpp
        Foundation/SymbolFinder.cpp
        Foundation/Path.cpp
        Foundation/SandboxFs.cpp
        Substrate/hde64.c
        Substrate/SubstrateDebug.cpp
        Substrate/SubstrateHook.cpp
        Substrate/SubstratePosixMemory.cpp
        )
include_directories(Foundation)
include_directories(Jni)
include_directories(./)

# add_definitions(-std=c++11)

add_definitions(-DCORE_SO_NAME="${LIBRARY_NAME}")
# NATIVE_TAG_NAME
add_definitions(-DNATIVE_TAG_NAME="${NATIVE_TAG_NAME}")

set(CXX_APPEND_FLAGS "-funwind-tables -g")

IF (CMAKE_BUILD_TYPE STREQUAL Release)
    set(CXX_APPEND_FLAGS "${CXX_APPEND_FLAGS} -fvisibility=hidden ")
    add_definitions("-DZELDA_BUILD_TYPE_RELEASE")
ELSE ()
    add_definitions("-DZELDA_BUILD_TYPE_DEBUG")
ENDIF ()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${CXX_APPEND_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++0x ${CXX_APPEND_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -s -Wl,--gc-sections")

set(ZELDA_SOURCE ${ZELDA_NATIVE_SOURCE})

add_library(${LIBRARY_NAME} SHARED ${ZELDA_SOURCE})

target_link_libraries(${LIBRARY_NAME} log)