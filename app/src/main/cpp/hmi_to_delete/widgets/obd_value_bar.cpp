#include <sstream>
#include <jni.h>

#include "midware/trace/trace.h"

#include "obd_value_bar.hpp"

namespace HMI
{
    OBDValueBar::OBDValueBar(int identifier, float min, float max, float zero, float value, const std::string &description, const std::string &unit)
        : m_identifier(identifier), m_min(min), m_max(max), m_zero(zero), m_value(value), description(description), unit(unit)
    {
        this->create_android_gui_element();
    }

    void OBDValueBar::set_min(float min)
    {
        this->m_min = min;
    }

    void OBDValueBar::set_max(float max)
    {
        this->m_max = max;
    }

    void OBDValueBar::set_zero(float zero)
    {
        this->m_zero = zero;
    }

    void OBDValueBar::set_value(float value)
    {
        this->m_value = value;
        //this->update_view();
    }

    void OBDValueBar::set_description(const std::string &description)
    {
        this->description = description;
    }

    int OBDValueBar::get_identifier() const { return this->m_identifier; }

    void OBDValueBar::create_android_gui_element()
    {
#if 0
        /* Request JAVA to update the form elements */
        jmethodID method_id = 0; //java_env_thread_hmi->GetMethodID(java_jobject_main_view, "createOBDValueBar", "(FFFFLjava/lang/String;Ljava/lang/String;)I");
        if (method_id == 0)
        {
            return;
        }
        //float min, float max, float zero, float value, String description
        //java_environment->CallVoidMethod(java_jobject_main_view, method_id,
        jstring jstrBuf = java_env_thread_hmi->NewStringUTF(this->description.c_str());
        jstring jstrUnitBuf = java_env_thread_hmi->NewStringUTF(this->unit.c_str());

        jint android_gui_id_ret_value = java_env_thread_hmi->CallIntMethod(
                java_jobject_main_view_instance, method_id,
                this->m_min, this->m_max, this->m_zero, this->m_value,
                jstrBuf, jstrUnitBuf
        );

        //jclass integer_class = java_env_thread_hmi->FindClass("java/lang/Integer");
        //if(integer_class == NULL){
            //outFile<<"cannot find FindClass(java/lang/Integer)"<<endl;
       // }
        //jmethodID getVal = java_env_thread_hmi->GetMethodID(integer_class, "intValue", "()I");
        //if(getVal == NULL){
            //outFile<<"Couldnot find Int getValue()"<<endl;
        //}
        //this->android_gui_id = java_env_thread_hmi->CallIntMethod(android_gui_id_ret_value, getVal);
        this->android_gui_id = static_cast<int>(android_gui_id_ret_value);
        TRACE_PRINTF("Android GUI ID is: " + helper::to_string(this->android_gui_id));
        //printf("In C, depth = %d, back from Java\n", depth);
#endif
    }

    void OBDValueBar::update_android_gui_element() const
    {
#if 0
        //jclass cls = java_environment->GetObjectClass(java_jobject_main_view);
        jmethodID method_id = java_env_thread_hmi->GetMethodID(java_jobject_main_view, "setOBDValueVarValue", "(IFFFF)V");
        if (method_id == 0)
        {
            return;
        }
        java_env_thread_hmi->CallVoidMethod(java_jobject_main_view_instance, method_id, this->android_gui_id, this->m_min, this->m_max, this->m_zero, this->m_value);
#endif
    }


    void OBDValueBar::update_view() const
    {
        this->update_android_gui_element();
    }
}