//
// Created by Clemens on 23.12.2017.
//

#include <memory>
#include <jni.h>
#include "midware/trace/trace.h"
#include "hmi/forms/form_main.hpp"
#include "hmi/hmi_thread.hpp"

namespace HMI
{
    FormMain::FormMain()
        : FormBase(), displayed_identifiers()
    {


        /* Constructor, create initial layout. */
        this->layout_data.rows = 4;
        this->layout_data.columns = 2;
        this->layout_data.show_hybrid_indicator = false;
    }


    void FormMain::update_data_from_dpool()
    {
        /* Just display all of the data */
        this->layout_data.obd_data.clear();


        for (int i = 0; i < DataPoolAccessor::get_obd_data_array_num_of_elements(); ++i)
        {
            DpoolOBDData data = DataPoolAccessor::get_obd_data_array_element(i);
            if (!is_identifier_displayed(data.identifier))
            {
                continue;
            }

            FormMainOBDDataLayout current_obd_data_layout = {0};
            current_obd_data_layout.obd_data = data;

            /* Todo all other values can currently remain zero, implement when free */
            this->layout_data.obd_data.push_back(current_obd_data_layout);
        }
    }

    bool FormMain::is_identifier_displayed(const unsigned char identifier) const
    {
        return displayed_identifiers[identifier];
    }


    void FormMain::create_view()
    {
        /* Update the current layout data from DPOOL */
        this->update_data_from_dpool();

        for (auto itr = this->layout_data.obd_data.begin(); itr != this->layout_data.obd_data.end(); ++itr)
        {
            TRACE_PRINTF("Printing OBDData " + itr->obd_data.description);
            std::shared_ptr<OBDValueBar> obd_value_bar(new OBDValueBar(
                    0,
                    itr->obd_data.min,
                    itr->obd_data.max,
                    itr->obd_data.zero,
                    itr->obd_data.value,
                    itr->obd_data.description,
                    itr->obd_data.unit
            ));
            obd_value_bar->update_view();


            this->add_obd_value_bar_to_android_gui(*obd_value_bar);
            this->obv_value_bars.push_back(obd_value_bar);
        }

    }

    void FormMain::update_view()
    {
        /* Update the current layout data from DPOOL */
        this->update_data_from_dpool();

        int index = 0;
        for (auto itr = this->layout_data.obd_data.begin(); itr != this->layout_data.obd_data.end(); ++itr)
        {
            this->obv_value_bars[index]->set_value(itr->obd_data.value);

            this->obv_value_bars[index]->update_view();

            index++;
        }
    }

    void FormMain::set_displayed_identifiers(const std::vector<unsigned int> &displayed_identifiers)
    {
        for (auto identifier : displayed_identifiers)
        {
            this->displayed_identifiers[identifier] = true;
        }

        DataPool::dpool_mutex.lock();
        DataPool::dpool_hmi_main_view_elements_of_interest = displayed_identifiers;
        DataPool::dpool_mutex.unlock();

        this->create_view();
    }

    void FormMain::add_obd_value_bar_to_android_gui(const OBDValueBar &obd_value_bar)
    {
        /* Update the current layout data from DPOOL */
        this->update_data_from_dpool();

        /* Request JAVA to update the form elements */

        //jclass cls = java_environment->GetObjectClass(java_jobject_main_view);
        jmethodID method_id = get_hmi_thread().get_java_env_thread_hmi()->GetMethodID(java_jobject_main_view, "addOBDValueBar", "(III)V");
        if (method_id == 0)
        {
            return;
        }
        get_hmi_thread().get_java_env_thread_hmi()->CallVoidMethod(java_jobject_main_view_instance, method_id, obd_value_bar.android_gui_id, this->layout_data.columns, this->layout_data.rows);
        //printf("In C, depth = %d, back from Java\n", depth);
    }

}