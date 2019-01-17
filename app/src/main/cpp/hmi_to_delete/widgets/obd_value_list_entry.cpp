#include <sstream>
#include <jni.h>
#include <complex>

#include "midware/trace/trace.h"

#include "hmi/widgets/obd_value_list_entry.hpp"

namespace HMI
{
    OBDValueListEntry::OBDValueListEntry(FormBase &owner, int identifier)
            : m_owner(owner), m_index(0u), m_identifier(identifier),
              m_min(0.0f), m_max(1.0f), m_zero(0.0f), m_value(0.5f),
              description("Test"), unit("cm"), m_bo_checked(false),
              m_bo_value_changed(false)
    {
        create_android_gui_element();
    }

    OBDValueListEntry::OBDValueListEntry(FormBase &owner, int identifier,
                                         float min, float max, float zero,
                                         float value, const std::string &description,
                                         const std::string &unit, bool checked)
            : m_owner(owner), m_index(0u), m_identifier(identifier),
              m_min(min), m_max(max), m_zero(zero), m_value(value),
              description(description), unit(unit), m_bo_checked(checked),
              m_bo_value_changed(false)
    {
        create_android_gui_element();
    }

    void OBDValueListEntry::set_min(float min)
    {
        this->m_min = min;
    }

    void OBDValueListEntry::set_max(float max)
    {
        this->m_max = max;
    }

    void OBDValueListEntry::set_zero(float zero)
    {
        this->m_zero = zero;
    }

    void OBDValueListEntry::set_value(float value)
    {
        if (std::abs(this->m_value - value) > 0.0001f)
        {

            this->m_value = value;
            this->m_bo_value_changed = true;
        }
        //this->update_view();
    }

    void OBDValueListEntry::set_description(const std::string &description)
    {
        this->description = description;
    }

    void OBDValueListEntry::set_checked(bool bo_checked)
    {
        this->m_bo_checked = bo_checked;
    }

    bool OBDValueListEntry::get_checked() const
    {
        /* Directly get the value from JAVA */

    }

    int OBDValueListEntry::get_identifier() const { return this->m_identifier; }

    void OBDValueListEntry::create_android_gui_element()
    {
        /* Request JAVA to update the form elements */


        jmethodID method_id =  HMI::get_hmi_thread().get_java_env_thread_hmi()->GetMethodID(m_owner.get_java_form_class(),
                                                               "createOBDValueListEntry",
                                                               "(FFFFLjava/lang/String;Ljava/lang/String;Z)I");

        if (method_id == 0)
        {
            return;
        }

        jstring jstrBuf =  HMI::get_hmi_thread().get_java_env_thread_hmi()->NewStringUTF(this->description.c_str());
        jstring jstrUnitBuf =  HMI::get_hmi_thread().get_java_env_thread_hmi()->NewStringUTF(this->unit.c_str());

        jint android_gui_id_ret_value =  HMI::get_hmi_thread().get_java_env_thread_hmi()->CallIntMethod(
                m_owner.get_java_form_object(), method_id,
                this->m_min, this->m_max, this->m_zero, this->m_value,
                jstrBuf, jstrUnitBuf, this->m_bo_checked
        );

        this->android_gui_id = static_cast<int>(android_gui_id_ret_value);
    }

    void OBDValueListEntry::update_view() const
    {
        if (m_bo_value_changed == false)
        {
            return;
        }
        m_bo_value_changed = false;

        jmethodID method_id = java_env_thread_hmi->GetMethodID(m_owner.get_java_form_class(), "setOBDValueListEntry", "(IF)V");
        if (method_id == 0)
        {
            return;
        }
        java_env_thread_hmi->CallVoidMethod(m_owner.get_java_form_object(), method_id,
                                            this->android_gui_id, this->m_value);

    }

}