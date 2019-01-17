//
// Created by Clemens on 16.04.2018.
//

#ifndef ANDROID_HMI_STATE_MACHINE_H
#define ANDROID_HMI_STATE_MACHINE_H



#include <memory>
namespace HMI
{
    class FormMain;
    class FormOBDValueList;

    enum HMISMState
    {
        HMI_STATE_MAIN_VIEW = 0,
        HMI_STATE_OBD_LIST_VIEW = 1,
        HMI_STATE_DTC_VIEW = 2,
        HMI_STATE_READINESS_VIEW = 3
    };

    class HMIStateMachine
    {
    public:
        HMIStateMachine();

        int trigger_transition(HMISMState new_state);

        HMISMState get_current_state() const;

        int process_5ms();

        /* All the different forms */
        std::shared_ptr<HMI::FormMain> m_p_form_main;
        std::shared_ptr<HMI::FormOBDValueList> m_p_form_obd_value_list;

    private:
        HMISMState m_current_state;
    };


}

#endif //ANDROID_HMI_STATE_MACHINE_H
