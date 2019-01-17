//
// Created by Clemens on 16.04.2018.
//

#ifndef ANDROID_FORM_BASE_H
#define ANDROID_FORM_BASE_H

#include <jni.h>
#include "hmi/hmi_thread.hpp"

namespace HMI
{
    class FormBase
    {
    public:
        FormBase()
                : m_p_java_form_class(nullptr), m_p_java_form_object(nullptr)
        {}

        virtual ~FormBase() {}

        virtual void update_view() = 0;

        virtual void create_view() = 0;

        virtual int process_event(const HMI::HMIEvent& event);

        /* Android specific */
        void set_android_object_references(jclass java_class, jobject java_object);

        jclass get_java_form_class() const;
        jobject get_java_form_object() const;


    protected:
        /* Android specific */
        jclass m_p_java_form_class;
        jobject m_p_java_form_object;

    };
}

#endif //ANDROID_FORM_BASE_H
