//
// Created by Clemens on 20.12.2017.
//

#ifndef ANDROID_FORM_OBD_VALUE_LIST_HPP_
#define ANDROID_FORM_OBD_VALUE_LIST_HPP_


/* Libraries */
#include <memory>

/* Commonly used */
#include "data_pool.hpp"

/* Own header */
#include "hmi/forms/form_base.hpp"
#include "hmi/widgets/obd_value_list_entry.hpp"

namespace HMI
{
    class FormOBDValueList : public FormBase
    {
    public:
        FormOBDValueList();
        virtual ~FormOBDValueList() {}
        virtual void create_view();
        virtual void update_view();

        int process_event_button_click(int i32_button_id);
        virtual int process_event(const HMI::HMIEvent& event);

        void confirm_selected_elements(const std::vector<unsigned int> &cvi32_selected_elements, int i32_cols, int i32_rows);

        const int FORM_OBD_VALUE_LIST_BUTTON_CONFIRM_SETTINGS = 1;
    private:
        void update_data_from_dpool();


        /* Platform specific */
        void add_obd_value_list_entry_to_android_gui(const HMI::OBDValueListEntry &obd_value_list_entry);

        std::vector<std::shared_ptr<HMI::OBDValueListEntry>> m_ao_obd_value_list_entries;
    };
}
#endif /* ANDROID_FORM_OBD_VALUE_LIST_HPP_ */
