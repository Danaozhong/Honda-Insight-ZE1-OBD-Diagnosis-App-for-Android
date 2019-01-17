//
// Created by Clemens on 16.04.2018.
//

#include "form_base.hpp"


using namespace HMI;

int FormBase::process_event(const HMI::HMIEvent& event)
{
    return 0;
}

void FormBase::set_android_object_references(jclass java_class, jobject java_object)
{
    this->m_p_java_form_class = java_class;
    this->m_p_java_form_object = java_object;
}

jclass FormBase::get_java_form_class() const
{
    return this->m_p_java_form_class;
}

jobject FormBase::get_java_form_object() const
{
    return this->m_p_java_form_object;
}