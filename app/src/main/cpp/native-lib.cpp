#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_vaultguard_app_di_NetworkModule_getApiKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string api_key = "e8f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6";
    return env->NewStringUTF(api_key.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_vaultguard_app_di_NetworkModule_getHmacSecret(
        JNIEnv* env,
        jobject /* this */) {
    std::string hmac_secret = "c1d2e3f4g5h6i7j8k9l0m1n2o3p4q5r6s7t8u9v0w1x2y3z4a5b6c7";
    return env->NewStringUTF(hmac_secret.c_str());
}
