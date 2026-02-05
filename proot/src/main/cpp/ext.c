#include <jni.h>
#include "proot/src/build.h"

//
// Created by 罗庭辉 on 2026/2/5.
//

JNIEXPORT jstring JNICALL
Java_com_github_andock_proot_PRoot_getVersion(JNIEnv *env, jclass thiz) {
    return (*env)->NewStringUTF(env, VERSION);
}