#include <jni.h>
#include "proot/src/build.h"
#include <sys/syscall.h>      /* Definition of SYS_* constants */
#include <unistd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_github_andock_proot_PRoot_getVersion(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(VERSION);
}