//
// Created by Clemens on 23.12.2017.
//

#include <sstream>
#include <android/log.h>


#include "trace.h"


void print_trace(const char * format_str, ...)
{
    char print_buffer[1024];
    va_list args;

    va_start(args, format_str);
    snprintf(print_buffer, 1024, format_str, args);
    va_end(args);

    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", print_buffer);
}

void print_trace(const std::string &str)
{
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", str.c_str());
}

namespace helper
{
    std::string to_string(int i)
    {
        std::stringstream ss;
        ss << i;
        return ss.str();
    }
}

JNIEnv* java_env_thread_hmi = NULL;
JNIEnv* java_env_thread_ble = NULL;
jclass java_jobject_main_view;
jobject java_jobject_main_view_instance;
JavaVM* java_vm = NULL;