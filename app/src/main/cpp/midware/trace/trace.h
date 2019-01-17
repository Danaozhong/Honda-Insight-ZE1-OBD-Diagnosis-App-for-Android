//
// Created by Clemens on 23.12.2017.
//

#ifndef _OS_SERVICES_H
#define _OS_SERVICES_H

#include <string>
#include <sstream>
#include <jni.h>

//void debug_log(const std::string &print);

void print_trace(const char *, ...);

/** C++ overload */
void print_trace(const std::string &str);

namespace helper
{
    std::string to_string(int i);

    template<typename T>
    std::string to_string(T _obj)
    {
        std::stringstream ss;
        ss << _obj;
        return ss.str();
    }
}


//#define TRACE_PRINTF(arg) debug_log(arg)
#define TRACE_PRINTF(...) (print_trace(__VA_ARGS__))

extern jclass java_jobject_main_view;
extern jobject java_jobject_main_view_instance;
extern JavaVM* java_vm;

#endif /* _OS_SERVICES_H */
