#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_texelography_hybridinsight_MainView_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT jstring
JNICALL
Java_com_texelography_hybridinsight_MainView_stringFromJNI2(
        JNIEnv *env,
jobject /* this */) {
std::string hello = "Hello from C++";
return env->NewStringUTF(hello.c_str());
}

