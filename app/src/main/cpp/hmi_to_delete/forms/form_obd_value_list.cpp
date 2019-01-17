//
// Created by Clemens on 23.12.2017.
//

/* Libraries */
#include <memory>
#include <jni.h>

/* Foreign header */
#include "midware/trace/trace.h"
#include "midware/data_pool/data_pool.hpp"


/* Own header */
#include "hmi/forms/form_obd_value_list.hpp"
#include "hmi/hmi_state_machine.hpp"
#include "hmi/hmi_thread.hpp"

namespace HMI
{
    using namespace DataPool;

    static FormOBDValueList* po_form_obd_value_list = nullptr;

    FormOBDValueList::FormOBDValueList()
    {}


    void FormOBDValueList::update_data_from_dpool()
    {
        dpool_mutex.lock();
        for (size_t i = 0; i != m_ao_obd_value_list_entries.size(); ++i)
        {
            if (i >= dpool_obd_data.size())
            {
                TRACE_PRINTF("Data storeage does not have a OBD data for Index " + helper::to_string(i));
            }
            else
            {
                m_ao_obd_value_list_entries[i]->set_value(dpool_obd_data[i].value);
                m_ao_obd_value_list_entries[i]->set_checked(false);
            }

        }

        for (auto itr = dpool_hmi_main_view_elements_of_interest.begin();
             itr != dpool_hmi_main_view_elements_of_interest.end(); ++itr)
        {
            if (*itr >= m_ao_obd_value_list_entries.size())
            {
                TRACE_PRINTF("OBD_VALUE_LIST_INDEX_OUT_OF_RANGE " + helper::to_string(*itr));
            }
            else
            {
                m_ao_obd_value_list_entries[*itr]->set_checked(true);
            }
        }
        dpool_mutex.unlock();
    }


    void FormOBDValueList::create_view()
    {
        /* Initially create the OBD data values */
        dpool_mutex.lock();
        this->m_ao_obd_value_list_entries.reserve(dpool_obd_data.size());

        TRACE_PRINTF("Creating OBD Value List!" + helper::to_string(dpool_obd_data.size()) + " elements!");
        for (auto itr = dpool_obd_data.begin(); itr != dpool_obd_data.begin() + 30; ++itr) //dpool_obd_data.end(); ++itr)
        {
            auto obd_value_list_entry = std::shared_ptr<OBDValueListEntry>(
                    new OBDValueListEntry(*this, itr->identifier,
                                          itr->min, itr->max, itr->zero, itr->value,
                                          itr->description, itr->unit, false));
            m_ao_obd_value_list_entries.push_back(obd_value_list_entry);
        }

        dpool_mutex.unlock();

        /* Update the current layout data from DPOOL */
        this->update_data_from_dpool();

        for (auto itr = this->m_ao_obd_value_list_entries.begin();
             itr != this->m_ao_obd_value_list_entries.end(); ++itr)
        {
            this->add_obd_value_list_entry_to_android_gui(**itr);
        }
    }

    void FormOBDValueList::update_view()
    {
        /* Update the current layout data from DPOOL */
        this->update_data_from_dpool();

        for(size_t i = 0u; i != m_ao_obd_value_list_entries.size(); ++i)
        {
            this->m_ao_obd_value_list_entries[i]->update_view();
        }
    }

    int FormOBDValueList::process_event_button_click(int i32_button_id)
    {
        switch (i32_button_id)
        {
            case FORM_OBD_VALUE_LIST_BUTTON_CONFIRM_SETTINGS:
                /* User tries to close the form, check settings */
                break;
        }
    }

    int FormOBDValueList::process_event(const HMI::HMIEvent& event)
    {
        switch(event.m_event_type)
        {
            case HMIEvent_PressButton:
                return this->process_event_button_click(event.m_button_id);
                break;

        }
        return 0;
    }

    void FormOBDValueList::confirm_selected_elements(const std::vector<unsigned int> &cvi32_selected_elements, int i32_cols, int i32_rows)
    {
        /* Save the new selection in data pool */
        DataPool::dpool_mutex.lock();
        DataPool::dpool_hmi_main_view_elements_of_interest = cvi32_selected_elements;
        DataPool::dpool_mutex.unlock();

        /* And return to the main view */
        HMIEvent o_event_confirm_selected_elements;
        o_event_confirm_selected_elements.m_event_type = HMI::
        HMI::get_event_queue().enqueue_event();

    }

    void FormOBDValueList::add_obd_value_list_entry_to_android_gui(const OBDValueListEntry &obd_value_list_entry)

    {
        /* Update the current layout data from DPOOL */
        //this->update_data_from_dpool();

        /* Request JAVA to update the form elements */

        jmethodID method_id = java_env_thread_hmi->GetMethodID( this->get_java_form_class(), "addOBDValueListEntry", "(I)V");
        if (method_id == 0)
        {
            return;
        }
        java_env_thread_hmi->CallVoidMethod(this->get_java_form_object(), method_id, obd_value_list_entry.android_gui_id);
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_FormOBDValueList_android_1confirm_1_selection1(JNIEnv *env,
                                                                          jobject obj,
              jintArray selected_field, jint num_of_selected_fields, jint num_of_cols, jint num_of_rows)
{
    /* Convert the selected fields from JAVA to C++ data types */
    int* ai32_selected_fields_array = new int[num_of_selected_fields];
    env->GetIntArrayRegion (selected_field, 0, num_of_selected_fields, ai32_selected_fields_array);
    std::vector<unsigned int> vi32_selected_elements(ai32_selected_fields_array, ai32_selected_fields_array + num_of_selected_fields);
    delete[](ai32_selected_fields_array);

    if (HMI::po_form_obd_value_list == nullptr)
    {
        return;
    }
    HMI::po_form_obd_value_list->confirm_selected_elements(vi32_selected_elements, num_of_cols, num_of_rows);
}


