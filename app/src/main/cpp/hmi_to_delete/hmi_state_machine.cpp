//
// Created by Clemens on 16.04.2018.
//

/* Foreign header */
#include "midware/data_pool/data_pool.hpp"

/* Own header */
#include "hmi/forms/form_main.hpp"
#include "hmi/forms/form_obd_value_list.hpp"
#include "hmi/hmi_state_machine.hpp"

using namespace HMI;


HMIStateMachine::HMIStateMachine()
        : m_current_state(HMI_STATE_MAIN_VIEW),
          m_p_form_main(nullptr),
          m_p_form_obd_value_list(nullptr)
{

    /* Create forms */
    m_p_form_main = std::shared_ptr<HMI::FormMain>(new HMI::FormMain());
    std::vector<unsigned char> displayed_ids = {0x03, 0x01, 0x02, 0x00, 0x04, 0x0E, 0x0A, 0x0C};
    //m_p_form_main->set_displayed_identifiers(displayed_ids);

    m_p_form_obd_value_list = std::shared_ptr<HMI::FormOBDValueList>(new HMI::FormOBDValueList());
    //m_p_form_obd_value_list->create_view();

}

int HMIStateMachine::trigger_transition(HMISMState new_state)
{
    switch(this->m_current_state)
    {
        case HMI_STATE_MAIN_VIEW:
            if(new_state == HMI_STATE_OBD_LIST_VIEW)
            {
                /* Change OBD Data reception so that all values are read */
                DataPool::bo_read_all_values = true;

                /* Transit from main view to the OBD List view */
                this->m_p_form_obd_value_list->create_view();
                this->m_current_state = new_state;
            }
            return 0;
        case HMI_STATE_OBD_LIST_VIEW:
            if(new_state == HMI_STATE_MAIN_VIEW)
            {
                /* Transit from main view to the OBD List view */
                this->m_current_state = new_state;
            }
            return 0;
        default:
            break;
    }

}

HMISMState HMIStateMachine::get_current_state() const
{
    return this->m_current_state;
}

int HMIStateMachine::process_5ms()
{
    switch (this->m_current_state)
    {
        case HMI_STATE_MAIN_VIEW:
            this->m_p_form_main->update_view();
            return 0;
        case HMI_STATE_OBD_LIST_VIEW:
            this->m_p_form_obd_value_list->update_view();
            return 0;
        default:
            break;
    }
    return -1;
}