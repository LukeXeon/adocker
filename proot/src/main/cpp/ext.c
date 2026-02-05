#include <jni.h>
#include "proot/src/build.h"

JNIEXPORT jstring JNICALL
Java_com_github_andock_proot_PRoot_getVersion(JNIEnv *env, jclass thiz) {
    return (*env)->NewStringUTF(env, VERSION);
}